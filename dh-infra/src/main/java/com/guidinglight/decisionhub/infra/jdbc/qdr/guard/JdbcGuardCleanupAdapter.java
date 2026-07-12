package com.guidinglight.decisionhub.infra.jdbc.qdr.guard;

import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.GuardCleanupPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import java.time.Duration;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL bounded cleanup；rate物理删除受DB safety grace保护，idempotency只保留EXPIRED tombstone。 */
public final class JdbcGuardCleanupAdapter implements GuardCleanupPort {
  private static final String RATE_SQL =
      "with candidates as (select environment,endpoint,source,tenant_id,window_start"
          + " from dh_qdr7_rate_limit_bucket where environment=? and endpoint=? and source=?"
          + " and window_end < transaction_timestamp()-(? * interval '1 millisecond')"
          + " order by window_end,tenant_id,window_start limit ? for update skip locked)"
          + " delete from dh_qdr7_rate_limit_bucket target using candidates where"
          + " target.environment=candidates.environment and target.endpoint=candidates.endpoint"
          + " and target.source=candidates.source and target.tenant_id=candidates.tenant_id"
          + " and target.window_start=candidates.window_start";
  private static final String IDEMPOTENCY_SQL =
      "with candidates as (select guard_id,state,state_version from dh_qdr7_idempotency_guard"
          + " where environment=? and endpoint=? and source=? and state<>'EXPIRED'"
          + " and (lease_expires_at is null or lease_expires_at < transaction_timestamp())"
          + " and ((state in ('COMPLETED','FAILED') and retention_until < transaction_timestamp()-(? * interval '1 millisecond'))"
          + " or (state in ('RECEIVED','IN_PROGRESS') and expires_at < transaction_timestamp()-(? * interval '1 millisecond')))"
          + " order by least(expires_at,retention_until),guard_id limit ? for update skip locked)"
          + " update dh_qdr7_idempotency_guard target set state='EXPIRED',state_version=target.state_version+1,"
          + " lease_owner=null,lease_token=null,lease_expires_at=null,result_type=null,result_id=null,"
          + " result_checksum=null,stable_error_code=null,completed_at=null,failed_at=null,"
          + " expired_at=transaction_timestamp(),updated_at=transaction_timestamp() from candidates"
          + " where target.guard_id=candidates.guard_id and target.state=candidates.state"
          + " and target.state_version=candidates.state_version";

  private final JdbcTemplate jdbc;

  /** @param jdbcTemplate DH-owned PostgreSQL JdbcTemplate。 */
  public JdbcGuardCleanupAdapter(final JdbcTemplate jdbcTemplate) {
    this.jdbc = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public int cleanupExpiredRateBuckets(final GuardCleanupCommand command) {
    final var c = Objects.requireNonNull(command, "command");
    return bounded(c, () -> jdbc.update(RATE_SQL, c.environment(), c.endpoint(), c.source(), millis(c.safetyGrace()), c.batchSize()));
  }

  @Override
  public int cleanupRetainedIdempotency(final GuardCleanupCommand command) {
    final var c = Objects.requireNonNull(command, "command");
    return bounded(c, () -> jdbc.update(IDEMPOTENCY_SQL, c.environment(), c.endpoint(), c.source(),
        millis(c.safetyGrace()), millis(c.safetyGrace()), c.batchSize()));
  }

  private int bounded(final GuardCleanupCommand command, final java.util.function.IntSupplier action) {
    try {
      final int affected = action.getAsInt();
      if (affected < 0 || affected > command.batchSize())
        throw new PersistentGuardStoreException("cleanup affected-row bound violated", null);
      return affected;
    } catch (final DataAccessException error) {
      throw new PersistentGuardStoreException("guard cleanup store unavailable", error);
    }
  }

  private static long millis(final Duration duration) {
    return duration.toMillis();
  }
}
