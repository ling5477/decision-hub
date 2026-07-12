package com.guidinglight.decisionhub.infra.jdbc.qdr.guard;

import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import java.sql.Timestamp;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL bounded cleanup adapter；每次只删除indexed、locked且不超过batchSize的过期记录。 */
public final class JdbcGuardCleanupAdapter implements GuardCleanupPort {

  private static final String RATE_SQL =
      "with candidates as (select environment, endpoint, source, tenant_id, window_start"
          + " from dh_qdr7_rate_limit_bucket where environment = ? and endpoint = ? and source = ?"
          + " and window_end < ? order by window_end, tenant_id, window_start limit ?"
          + " for update skip locked) delete from dh_qdr7_rate_limit_bucket target using candidates"
          + " where target.environment = candidates.environment and target.endpoint = candidates.endpoint"
          + " and target.source = candidates.source and target.tenant_id = candidates.tenant_id"
          + " and target.window_start = candidates.window_start";

  private static final String IDEMPOTENCY_SQL =
      "with candidates as (select guard_id from dh_qdr7_idempotency_guard"
          + " where environment = ? and endpoint = ? and source = ?"
          + " and state in ('COMPLETED', 'FAILED', 'EXPIRED') and retention_until < ?"
          + " order by retention_until, tenant_id, request_id limit ? for update skip locked)"
          + " delete from dh_qdr7_idempotency_guard target using candidates"
          + " where target.guard_id = candidates.guard_id"
          + " and target.state in ('COMPLETED', 'FAILED', 'EXPIRED')";

  private final JdbcTemplate jdbcTemplate;

  /** @param jdbcTemplate DH-owned PostgreSQL JdbcTemplate。 */
  public JdbcGuardCleanupAdapter(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public int cleanupExpiredRateBuckets(final GuardCleanupCommand command) {
    return update(RATE_SQL, command);
  }

  @Override
  public int cleanupRetainedIdempotency(final GuardCleanupCommand command) {
    return update(IDEMPOTENCY_SQL, command);
  }

  private int update(final String sql, final GuardCleanupCommand command) {
    final GuardCleanupCommand checked = Objects.requireNonNull(command, "command");
    try {
      final int affected =
          jdbcTemplate.update(
              sql,
              checked.environment(),
              checked.endpoint(),
              checked.source(),
              Timestamp.from(checked.cutoffExclusive()),
              checked.batchSize());
      if (affected < 0 || affected > checked.batchSize()) {
        throw new PersistentGuardStoreException("cleanup affected-row bound violated", null);
      }
      return affected;
    } catch (final DataAccessException error) {
      throw new PersistentGuardStoreException("guard cleanup store unavailable", error);
    }
  }
}
