package com.guidinglight.decisionhub.infra.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1：JdbcNonceReplayGuard 持久化/重启语义集成测试。
 *
 * <p>需要 Docker；无 Docker 时由 {@code disabledWithoutDocker=true} 整类跳过（CI 必须有 Docker 才能验证 持久化 nonce
 * restart 语义）。本测试用真实 Postgres 验证：重启（重建 guard 实例、复用同一持久化存储）后窗口内 重放仍被拒；不同
 * source/tenant/requestId/nonce 组合互不误判。
 *
 * <p>全部 test-only：固定假 replay key；不读取 .env、不接 RealClient、不发真实 NQ HTTP、不接交易所。
 */
@Testcontainers(disabledWithoutDocker = true)
class JdbcNonceReplayGuardPersistenceTest {

  /** 表 DDL 镜像 V4__nq_feedback_replay_nonce.sql，仅用于本集成测试建表。 */
  private static final String CREATE_TABLE_SQL =
      "create table if not exists dh_nq_replay_nonce ("
          + " replay_key varchar(512) primary key,"
          + " expires_at timestamptz not null,"
          + " created_at timestamptz not null default now())";

  private static final String CREATE_INDEX_SQL =
      "create index if not exists idx_dh_nq_replay_nonce_expires on dh_nq_replay_nonce(expires_at)";

  @Container
  static final PostgreSQLContainer<?> pg =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("decision_hub")
          .withUsername("decision_hub")
          .withPassword("decision_hub");

  private static JdbcTemplate jdbcTemplate;

  @BeforeAll
  static void initSchema() {
    final DriverManagerDataSource ds = new DriverManagerDataSource();
    ds.setUrl(pg.getJdbcUrl());
    ds.setUsername(pg.getUsername());
    ds.setPassword(pg.getPassword());
    ds.setDriverClassName("org.postgresql.Driver");
    jdbcTemplate = new JdbcTemplate(ds);
    jdbcTemplate.execute(CREATE_TABLE_SQL);
    jdbcTemplate.execute(CREATE_INDEX_SQL);
  }

  private Instant futureExpiry() {
    return Instant.now().plusSeconds(600);
  }

  @Test
  void persistent_nonce_rejects_replay_after_restart_simulation() {
    final String replayKey = "nexus-quant::nonce-restart::req-restart";
    final Instant expiresAt = futureExpiry();

    // 第一个 guard 实例登记成功。
    final JdbcNonceReplayGuard guard1 = new JdbcNonceReplayGuard(jdbcTemplate, false);
    assertThat(guard1.markIfAbsent(replayKey, expiresAt)).isTrue();

    // 模拟进程重启：丢弃 guard1，新建 guard2（复用持久化存储），同 key 必须被拒。
    final JdbcNonceReplayGuard guard2 = new JdbcNonceReplayGuard(jdbcTemplate, false);
    assertThat(guard2.markIfAbsent(replayKey, expiresAt)).isFalse();
  }

  @Test
  void persistent_nonce_is_source_tenant_request_scoped() {
    final Instant expiresAt = futureExpiry();
    final JdbcNonceReplayGuard guard = new JdbcNonceReplayGuard(jdbcTemplate, false);

    // 不同 source/requestId/nonce 组合 -> 不同 replay_key -> 互不影响，均首次通过。
    assertThat(guard.markIfAbsent("nexus-quant::n-a::req-a", expiresAt)).isTrue();
    assertThat(guard.markIfAbsent("nexus-quant::n-a::req-b", expiresAt)).isTrue();
    assertThat(guard.markIfAbsent("other-source::n-a::req-a", expiresAt)).isTrue();
    assertThat(guard.markIfAbsent("nexus-quant::n-b::req-a", expiresAt)).isTrue();

    // 相同 replay_key 第二次 -> 重放，拒绝。
    assertThat(guard.markIfAbsent("nexus-quant::n-a::req-a", expiresAt)).isFalse();
  }

  @Test
  void cleanup_removes_expired_rows_only() {
    final JdbcNonceReplayGuard guard = new JdbcNonceReplayGuard(jdbcTemplate, false);
    final String expiredKey = "nexus-quant::n-expired::req-x";
    final String liveKey = "nexus-quant::n-live::req-x";

    // 已过期行 + 仍有效行各一。
    assertThat(guard.markIfAbsent(expiredKey, Instant.now().minusSeconds(1))).isTrue();
    assertThat(guard.markIfAbsent(liveKey, futureExpiry())).isTrue();

    final int removed = guard.cleanupExpired();
    assertThat(removed).isGreaterThanOrEqualTo(1);

    // 过期 key 清理后可再次登记（视为新）；有效 key 仍被防重放保护。
    assertThat(guard.markIfAbsent(expiredKey, futureExpiry())).isTrue();
    assertThat(guard.markIfAbsent(liveKey, futureExpiry())).isFalse();
  }
}
