package com.guidinglight.decisionhub.infra.jdbc;

import com.guidinglight.decisionhub.security.nq.NonceReplayGuard;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1：PostgreSQL-backed {@link NonceReplayGuard} 实现。
 *
 * <p>职责：以原子 {@code INSERT ... ON CONFLICT (replay_key) DO NOTHING} 登记 replay_key，靠影响行数判定是否重放，
 * 取代仅单实例、无界的 {@code InMemoryNonceReplayGuard}，使多实例部署与进程重启后窗口内重放仍被拒（持久化）。
 *
 * <p>关键约束：
 *
 * <ol>
 *   <li><b>fail-closed</b>：存储不可用（{@link DataAccessException}）时返回 false（= 重放命中 -> 认证层映射为拒绝）， 绝不
 *       fail-open 放行。
 *   <li><b>不泄露敏感信息</b>：不记录 replay_key 明文、不记录 nonce、签名原材料或 payload；异常日志只记类型，不带值。
 *   <li><b>有界增长</b>：开启 {@code cleanupEnabled} 时在登记前惰性清理 {@code expires_at < now()} 的过期行（依赖 {@code
 *       idx_dh_nq_replay_nonce_expires}）。高吞吐场景可改为定时 sweep。
 * </ol>
 *
 * <p>本实现不调用任何外部服务、不发起 HTTP、不处理订单/成交/仓位。
 */
public final class JdbcNonceReplayGuard implements NonceReplayGuard {

  private static final Logger log = LoggerFactory.getLogger(JdbcNonceReplayGuard.class);

  /** 原子登记：首次插入影响 1 行；冲突（已存在 = 重放）DO NOTHING 影响 0 行。 */
  private static final String MARK_SQL =
      "insert into dh_nq_replay_nonce (replay_key, expires_at, created_at)"
          + " values (?, ?, now())"
          + " on conflict (replay_key) do nothing";

  /** 惰性清理过期行，保证存储有界增长；过期前的行不会被删除，不破坏防重放窗口。 */
  private static final String CLEANUP_SQL =
      "delete from dh_nq_replay_nonce where expires_at < now()";

  private final JdbcTemplate jdbcTemplate;
  private final boolean cleanupEnabled;

  /**
   * 构造。
   *
   * @param jdbcTemplate 指向 DH 自身库的 JdbcTemplate；不可为 null。
   * @param cleanupEnabled 是否在每次登记前惰性清理过期行（有界增长）。
   */
  public JdbcNonceReplayGuard(final JdbcTemplate jdbcTemplate, final boolean cleanupEnabled) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.cleanupEnabled = cleanupEnabled;
  }

  /**
   * 原子登记 replay key。
   *
   * @param replayKey source::nonce::requestId 组合 key；不可为 null。
   * @param expiresAt 该 key 过期时间（NOT NULL 列）；不可为 null。
   * @return true 表示首次出现；false 表示重放或存储不可用（fail-closed）。
   */
  @Override
  public boolean markIfAbsent(final String replayKey, final Instant expiresAt) {
    Objects.requireNonNull(replayKey, "replayKey");
    Objects.requireNonNull(expiresAt, "expiresAt");
    try {
      if (cleanupEnabled) {
        // 惰性清理：单条 set-based DELETE，非 N+1；失败不应放行，故与登记同在 try 内统一 fail-closed。
        jdbcTemplate.update(CLEANUP_SQL);
      }
      final int affected = jdbcTemplate.update(MARK_SQL, replayKey, Timestamp.from(expiresAt));
      return affected == 1;
    } catch (final DataAccessException ex) {
      // fail-closed：存储不可用时拒绝（返回 false -> 认证层 409 REPLAY_DETECTED），不降级为放行。
      // 只记异常类型，不记 replay_key / nonce / payload，避免敏感信息进日志。
      log.error(
          "nonce replay store unavailable, failing closed (request rejected). cause={}",
          ex.getClass().getName());
      return false;
    }
  }

  /**
   * 显式清理过期行，供定时 sweep 或测试调用。
   *
   * @return 删除的行数。
   * @throws DataAccessException 存储不可用时由调用方处理。
   */
  public int cleanupExpired() {
    return jdbcTemplate.update(CLEANUP_SQL);
  }
}
