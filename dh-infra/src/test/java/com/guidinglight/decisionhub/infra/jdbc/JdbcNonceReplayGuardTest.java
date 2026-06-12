package com.guidinglight.decisionhub.infra.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1：JdbcNonceReplayGuard 单元测试（无 Docker 可跑）。
 *
 * <p>本测试只校验 SQL 片段 + 影响行数语义 + fail-closed + cleanup 行为；真实持久化/重启语义由 {@link
 * JdbcNonceReplayGuardPersistenceIT}（Testcontainers，需 Docker）覆盖。
 */
@ExtendWith(MockitoExtension.class)
class JdbcNonceReplayGuardTest {

  private static final Instant EXPIRES = Instant.parse("2026-06-12T10:10:00Z");

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  void markIfAbsent_returns_true_when_insert_affects_one_row_and_uses_on_conflict_do_nothing() {
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);
    final JdbcNonceReplayGuard guard = new JdbcNonceReplayGuard(jdbcTemplate, false);

    final boolean first = guard.markIfAbsent("src::nonce-1::req-1", EXPIRES);

    assertThat(first).isTrue();
    final String sql = captureParamUpdateSql();
    assertThat(sql)
        .contains("insert into dh_nq_replay_nonce")
        .contains("on conflict (replay_key) do nothing");
  }

  @Test
  void markIfAbsent_returns_false_when_insert_affects_zero_rows_replay_detected() {
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);
    final JdbcNonceReplayGuard guard = new JdbcNonceReplayGuard(jdbcTemplate, false);

    final boolean replay = guard.markIfAbsent("src::nonce-1::req-1", EXPIRES);

    assertThat(replay).isFalse();
  }

  @Test
  void replay_store_unavailable_fails_closed() {
    when(jdbcTemplate.update(anyString(), any(Object[].class)))
        .thenThrow(new DataAccessResourceFailureException("db down"));
    final JdbcNonceReplayGuard guard = new JdbcNonceReplayGuard(jdbcTemplate, false);

    // 存储不可用必须 fail-closed：返回 false（认证层映射为拒绝），绝不放行；不抛未捕获异常导致放行。
    final boolean result = guard.markIfAbsent("src::nonce-secret::req-1", EXPIRES);

    assertThat(result).isFalse();
    // 不泄露：捕获的 update 调用参数里不得把 secret/payload 作为 SQL 文本拼接（只走参数绑定）。
    final String sql = captureParamUpdateSql();
    assertThat(sql).doesNotContain("nonce-secret");
  }

  @Test
  void cleanup_runs_before_mark_when_enabled() {
    // 不 stub 返回值（mark 走默认 0），只验证 cleanup 被调用，避免 varargs/单参重载的严格 stub 冲突。
    final JdbcNonceReplayGuard guard = new JdbcNonceReplayGuard(jdbcTemplate, true);

    guard.markIfAbsent("src::nonce-1::req-1", EXPIRES);

    // cleanup 走 update(String) 无参重载，删除过期行保证有界增长。
    verify(jdbcTemplate).update(eq("delete from dh_nq_replay_nonce where expires_at < now()"));
  }

  @Test
  void cleanup_skipped_when_disabled() {
    final JdbcNonceReplayGuard guard = new JdbcNonceReplayGuard(jdbcTemplate, false);

    guard.markIfAbsent("src::nonce-1::req-1", EXPIRES);

    verify(jdbcTemplate, never())
        .update(eq("delete from dh_nq_replay_nonce where expires_at < now()"));
  }

  /** 取参数化 update(sql, args...) 重载的 SQL 文本（mark insert）。 */
  private String captureParamUpdateSql() {
    return mockingDetails(jdbcTemplate).getInvocations().stream()
        .filter(inv -> "update".equals(inv.getMethod().getName()))
        .filter(inv -> inv.getArguments().length > 1)
        .map(Invocation::getArguments)
        .map(args -> (String) args[0])
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected parameterized JdbcTemplate.update(...)"));
  }
}
