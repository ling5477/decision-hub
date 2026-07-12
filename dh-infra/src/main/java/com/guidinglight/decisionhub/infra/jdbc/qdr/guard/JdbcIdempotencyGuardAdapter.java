package com.guidinglight.decisionhub.infra.jdbc.qdr.guard;

import com.guidinglight.decisionhub.usecase.qdr.guard.*;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL exact-identity幂等状态机；所有持久化生命周期绝对时间均来自当前数据库事务。 */
public final class JdbcIdempotencyGuardAdapter implements IdempotencyGuardPort {
  /** COMPLETED唯一允许的既有结果类型。 */
  public static final String RESULT_TYPE_DECISION_OUTPUT = "DH_DECISION_OUTPUT";

  private static final String INSERT_SQL =
      "insert into dh_qdr7_idempotency_guard"
          + " (guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,hash_version,"
          + "state,state_version,created_at,updated_at,expires_at,retention_until)"
          + " values (?,?,?,?,?,?,?,?,'RECEIVED',0,transaction_timestamp(),transaction_timestamp(),"
          + "transaction_timestamp()+(? * interval '1 millisecond'),"
          + "transaction_timestamp()+(? * interval '1 millisecond'))"
          + " on conflict (environment,endpoint,source,tenant_id,request_id) do nothing";
  private static final String SELECT_SQL =
      "select guard_id,environment,endpoint,source,tenant_id,request_id,request_hash,state,state_version,"
          + "lease_owner,lease_token,lease_expires_at,result_type,result_id,result_checksum,stable_error_code,"
          + "expires_at,retention_until from dh_qdr7_idempotency_guard"
          + " where environment=? and endpoint=? and source=? and tenant_id=? and request_id=?";
  private static final String UPDATE_SQL =
      "update dh_qdr7_idempotency_guard set state=?,state_version=state_version+1,"
          + "lease_owner=?,lease_token=?,lease_expires_at=case when cast(? as bigint) is null then null else "
          + "transaction_timestamp()+(? * interval '1 millisecond') end,result_type=?,result_id=?,"
          + "result_checksum=?,stable_error_code=?,updated_at=transaction_timestamp(),"
          + "completed_at=case when ?='COMPLETED' then transaction_timestamp() else null end,"
          + "failed_at=case when ?='FAILED' then transaction_timestamp() else null end,"
          + "expired_at=case when ?='EXPIRED' then transaction_timestamp() else null end"
          + " where environment=? and endpoint=? and source=? and tenant_id=? and request_id=?"
          + " and request_hash=? and state=? and state_version=?"
          + " and ((cast(? as varchar) is null and lease_owner is null) or lease_owner=cast(? as varchar))"
          + " and ((cast(? as uuid) is null and lease_token is null) or lease_token=cast(? as uuid))";
  private static final String RECOVER_SQL =
      "update dh_qdr7_idempotency_guard set state_version=state_version+1,lease_owner=?,lease_token=?,"
          + "lease_expires_at=transaction_timestamp()+(? * interval '1 millisecond'),"
          + "updated_at=transaction_timestamp() where environment=? and endpoint=? and source=?"
          + " and tenant_id=? and request_id=? and request_hash=? and state='IN_PROGRESS'"
          + " and state_version=? and lease_expires_at < transaction_timestamp()";

  private final JdbcTemplate jdbc;

  /** @param jdbcTemplate DH-owned PostgreSQL JdbcTemplate。 */
  public JdbcIdempotencyGuardAdapter(final JdbcTemplate jdbcTemplate) {
    this.jdbc = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public IdempotencyAdmissionResult admit(final IdempotencyAdmissionCommand command) {
    final var checked = Objects.requireNonNull(command, "command");
    try {
      final int inserted = jdbc.update(INSERT_SQL, UUID.randomUUID(), checked.identity().environment(),
          checked.identity().endpoint(), checked.identity().source(), checked.identity().tenantId(),
          checked.requestId(), checked.requestHash(), checked.hashVersion(), millis(checked.timeToLive()),
          millis(checked.retentionPeriod()));
      final IdempotencyRecordView record = selectExact(checked.identity(), checked.requestId());
      if (record == null) return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.STATE_INVALID, null);
      if (!record.requestHash().equals(checked.requestHash())) return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.CONFLICT, record);
      if (inserted == 1) return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.ADMITTED, record);
      if (inserted != 0) return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.STATE_INVALID, record);
      return new IdempotencyAdmissionResult(status(record.state()), record);
    } catch (final DataAccessException error) {
      return IdempotencyAdmissionResult.storeUnavailable();
    }
  }

  @Override
  public IdempotencyRecordView transition(final IdempotencyTransitionCommand command) {
    final var checked = Objects.requireNonNull(command, "command");
    validateTransition(checked);
    try {
      final int affected = isRecovery(checked) ? recover(checked) : update(checked);
      if (affected != 1) throw new PersistentGuardStateException("idempotency CAS transition lost or invalid");
      final var result = selectExact(checked.identity(), checked.requestId());
      if (result == null || !result.requestHash().equals(checked.requestHash()))
        throw new PersistentGuardStateException("idempotency row missing after transition");
      return result;
    } catch (final DataAccessException error) {
      throw new PersistentGuardStoreException("idempotency transition store unavailable", error);
    }
  }

  @Override
  public IdempotencyRecordView findExact(final PersistentGuardIdentity identity, final String requestId, final String requestHash) {
    try {
      final var record = selectExact(identity, requestId);
      if (record == null || !record.requestHash().equals(requestHash))
        throw new PersistentGuardStateException("idempotency exact record unavailable");
      return record;
    } catch (final DataAccessException error) {
      throw new PersistentGuardStoreException("idempotency query store unavailable", error);
    }
  }

  private int update(final IdempotencyTransitionCommand c) {
    final Long leaseMillis = c.leaseDuration() == null ? null : millis(c.leaseDuration());
    return jdbc.update(UPDATE_SQL, c.targetState().name(), c.newLeaseOwner(), c.newLeaseToken(),
        leaseMillis, leaseMillis, c.resultType(), c.resultId(), c.resultChecksum(), c.stableErrorCode(),
        c.targetState().name(), c.targetState().name(), c.targetState().name(), c.identity().environment(),
        c.identity().endpoint(), c.identity().source(), c.identity().tenantId(), c.requestId(), c.requestHash(),
        c.expectedState().name(), c.expectedVersion(), c.expectedLeaseOwner(), c.expectedLeaseOwner(),
        c.expectedLeaseToken(), c.expectedLeaseToken());
  }

  private int recover(final IdempotencyTransitionCommand c) {
    return jdbc.update(RECOVER_SQL, c.newLeaseOwner(), c.newLeaseToken(), millis(c.leaseDuration()),
        c.identity().environment(), c.identity().endpoint(), c.identity().source(), c.identity().tenantId(),
        c.requestId(), c.requestHash(), c.expectedVersion());
  }

  private IdempotencyRecordView selectExact(final PersistentGuardIdentity identity, final String requestId) {
    final List<Map<String,Object>> rows = jdbc.queryForList(SELECT_SQL, identity.environment(), identity.endpoint(),
        identity.source(), identity.tenantId(), requestId);
    if (rows.isEmpty()) return null;
    if (rows.size() != 1) throw new PersistentGuardStateException("idempotency exact identity returned multiple rows");
    final var r = rows.get(0);
    return new IdempotencyRecordView(uuid(r.get("guard_id")), new PersistentGuardIdentity(text(r,"environment"),
        text(r,"endpoint"), text(r,"source"), text(r,"tenant_id")), text(r,"request_id"), text(r,"request_hash"),
        IdempotencyState.valueOf(text(r,"state")), number(r,"state_version").longValue(), nullable(r.get("lease_owner")),
        uuidOrNull(r.get("lease_token")), instantOrNull(r.get("lease_expires_at")), nullable(r.get("result_type")),
        nullable(r.get("result_id")), nullable(r.get("result_checksum")), nullable(r.get("stable_error_code")),
        instant(r.get("expires_at")), instant(r.get("retention_until")));
  }

  private static void validateTransition(final IdempotencyTransitionCommand c) {
    final boolean legal = switch (c.expectedState()) {
      case RECEIVED -> c.targetState()==IdempotencyState.IN_PROGRESS || c.targetState()==IdempotencyState.EXPIRED;
      case IN_PROGRESS -> c.targetState()==IdempotencyState.IN_PROGRESS || c.targetState()==IdempotencyState.COMPLETED
          || c.targetState()==IdempotencyState.FAILED || c.targetState()==IdempotencyState.EXPIRED;
      case COMPLETED, FAILED -> c.targetState()==IdempotencyState.EXPIRED;
      case EXPIRED -> false;
    };
    if (!legal) throw new PersistentGuardStateException("illegal idempotency state transition");
    if (c.targetState()==IdempotencyState.IN_PROGRESS && (blank(c.newLeaseOwner()) || c.newLeaseToken()==null || c.leaseDuration()==null))
      throw new PersistentGuardStateException("IN_PROGRESS requires owner, token and duration");
    if (c.targetState()==IdempotencyState.COMPLETED && (!RESULT_TYPE_DECISION_OUTPUT.equals(c.resultType()) || blank(c.resultId()) || blank(c.resultChecksum())))
      throw new PersistentGuardStateException("COMPLETED requires typed safe result reference");
    if (c.targetState()==IdempotencyState.FAILED && blank(c.stableErrorCode()))
      throw new PersistentGuardStateException("FAILED requires stable error code");
  }

  private static boolean isRecovery(final IdempotencyTransitionCommand c) {
    return c.expectedState()==IdempotencyState.IN_PROGRESS && c.targetState()==IdempotencyState.IN_PROGRESS
        && c.expectedLeaseOwner()==null && c.expectedLeaseToken()==null;
  }
  private static IdempotencyAdmissionStatus status(final IdempotencyState s) { return switch(s) {
    case RECEIVED, IN_PROGRESS -> IdempotencyAdmissionStatus.IN_PROGRESS; case COMPLETED -> IdempotencyAdmissionStatus.COMPLETED;
    case FAILED -> IdempotencyAdmissionStatus.FAILED; case EXPIRED -> IdempotencyAdmissionStatus.EXPIRED; }; }
  private static long millis(final Duration value) { try { return value.toMillis(); } catch (ArithmeticException e) { throw new IllegalArgumentException("duration overflow",e); } }
  private static boolean blank(final String v) { return v==null || v.isBlank(); }
  private static String text(final Map<String,Object> r,String k) { final String v=nullable(r.get(k)); if(v==null) throw new PersistentGuardStateException("required idempotency field missing"); return v; }
  private static String nullable(Object v) { return v==null || v.toString().isBlank()?null:v.toString(); }
  private static Number number(Map<String,Object> r,String k) { if(r.get(k) instanceof Number n)return n; throw new PersistentGuardStateException("idempotency version is invalid"); }
  private static UUID uuid(Object v) { final UUID u=uuidOrNull(v); if(u==null)throw new PersistentGuardStateException("required UUID missing"); return u; }
  private static UUID uuidOrNull(Object v) { return v==null?null:(v instanceof UUID u?u:UUID.fromString(v.toString())); }
  private static Instant instant(Object v) { final Instant i=instantOrNull(v); if(i==null)throw new PersistentGuardStateException("required timestamp missing"); return i; }
  private static Instant instantOrNull(Object v) { if(v==null)return null; if(v instanceof Timestamp t)return t.toInstant(); if(v instanceof Instant i)return i; throw new PersistentGuardStateException("invalid timestamp"); }
}
