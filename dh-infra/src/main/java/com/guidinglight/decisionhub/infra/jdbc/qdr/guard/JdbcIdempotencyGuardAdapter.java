package com.guidinglight.decisionhub.infra.jdbc.qdr.guard;

import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionResult;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionStatus;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyGuardPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyRecordView;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyState;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyTransitionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStateException;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardStoreException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL exact-identity idempotency state machine adapter；只暴露atomic admission和CAS transition。 */
public final class JdbcIdempotencyGuardAdapter implements IdempotencyGuardPort {

  private static final String INSERT_SQL =
      "insert into dh_qdr7_idempotency_guard"
          + " (guard_id, environment, endpoint, source, tenant_id, request_id, request_hash,"
          + " hash_version, state, state_version, created_at, updated_at, expires_at, retention_until)"
          + " values (?, ?, ?, ?, ?, ?, ?, ?, 'RECEIVED', 0, transaction_timestamp(),"
          + " transaction_timestamp(), ?, ?)"
          + " on conflict (environment, endpoint, source, tenant_id, request_id) do nothing";

  private static final String SELECT_SQL =
      "select guard_id, environment, endpoint, source, tenant_id, request_id, request_hash,"
          + " state, state_version, lease_token, lease_expires_at, result_id, result_checksum,"
          + " stable_error_code, expires_at, retention_until"
          + " from dh_qdr7_idempotency_guard"
          + " where environment = ? and endpoint = ? and source = ? and tenant_id = ?"
          + " and request_id = ?";

  private static final String UPDATE_SQL =
      "update dh_qdr7_idempotency_guard set state = ?, state_version = state_version + 1,"
          + " lease_token = ?, lease_expires_at = ?, result_id = ?, result_checksum = ?,"
          + " stable_error_code = ?, updated_at = transaction_timestamp(),"
          + " completed_at = case when ? in ('COMPLETED', 'FAILED', 'EXPIRED')"
          + " then transaction_timestamp() else completed_at end"
          + " where environment = ? and endpoint = ? and source = ? and tenant_id = ?"
          + " and request_id = ? and request_hash = ? and state = ? and state_version = ?"
          + " and ((cast(? as uuid) is null and lease_token is null)"
          + " or lease_token = cast(? as uuid))"
          + " and (? <> 'IN_PROGRESS' or cast(? as uuid) is not null)";

  private static final String RECOVER_SQL =
      "update dh_qdr7_idempotency_guard set state_version = state_version + 1, lease_token = ?,"
          + " lease_expires_at = ?, updated_at = transaction_timestamp()"
          + " where environment = ? and endpoint = ? and source = ? and tenant_id = ?"
          + " and request_id = ? and request_hash = ? and state = 'IN_PROGRESS'"
          + " and state_version = ? and lease_expires_at < transaction_timestamp()";

  private final JdbcTemplate jdbcTemplate;

  /** @param jdbcTemplate DH-owned PostgreSQL JdbcTemplate。 */
  public JdbcIdempotencyGuardAdapter(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public IdempotencyAdmissionResult admit(final IdempotencyAdmissionCommand command) {
    final IdempotencyAdmissionCommand checked = Objects.requireNonNull(command, "command");
    try {
      final int inserted =
          jdbcTemplate.update(
              INSERT_SQL,
              UUID.randomUUID(),
              checked.identity().environment(),
              checked.identity().endpoint(),
              checked.identity().source(),
              checked.identity().tenantId(),
              checked.requestId(),
              checked.requestHash(),
              checked.hashVersion(),
              Timestamp.from(checked.expiresAt()),
              Timestamp.from(checked.retentionUntil()));
      final IdempotencyRecordView record = selectExact(checked.identity(), checked.requestId());
      if (record == null) {
        return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.STATE_INVALID, null);
      }
      if (!record.requestHash().equals(checked.requestHash())) {
        return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.CONFLICT, record);
      }
      if (inserted == 1) {
        return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.ADMITTED, record);
      }
      if (inserted != 0) {
        return new IdempotencyAdmissionResult(IdempotencyAdmissionStatus.STATE_INVALID, record);
      }
      return new IdempotencyAdmissionResult(status(record.state()), record);
    } catch (final DataAccessException error) {
      return IdempotencyAdmissionResult.storeUnavailable();
    }
  }

  @Override
  public IdempotencyRecordView transition(final IdempotencyTransitionCommand command) {
    final IdempotencyTransitionCommand checked = Objects.requireNonNull(command, "command");
    validateTransition(checked);
    try {
      final int affected;
      if (isExpiredLeaseRecovery(checked)) {
        affected = recover(checked);
      } else {
        affected = update(checked);
      }
      if (affected != 1) {
        throw new PersistentGuardStateException("idempotency CAS transition lost or invalid");
      }
      final IdempotencyRecordView result =
          selectExact(checked.identity(), checked.requestId());
      if (result == null || !result.requestHash().equals(checked.requestHash())) {
        throw new PersistentGuardStateException("idempotency row missing after transition");
      }
      return result;
    } catch (final DataAccessException error) {
      throw new PersistentGuardStoreException("idempotency transition store unavailable", error);
    }
  }

  @Override
  public IdempotencyRecordView findExact(
      final PersistentGuardIdentity identity,
      final String requestId,
      final String requestHash) {
    try {
      final IdempotencyRecordView record = selectExact(identity, requestId);
      if (record == null || !record.requestHash().equals(requestHash)) {
        throw new PersistentGuardStateException("idempotency exact record unavailable");
      }
      return record;
    } catch (final DataAccessException error) {
      throw new PersistentGuardStoreException("idempotency query store unavailable", error);
    }
  }

  private int update(final IdempotencyTransitionCommand command) {
    return jdbcTemplate.update(
        UPDATE_SQL,
        command.targetState().name(),
        command.newLeaseToken(),
        timestampOrNull(command.leaseExpiresAt()),
        command.resultId(),
        command.resultChecksum(),
        command.stableErrorCode(),
        command.targetState().name(),
        command.identity().environment(),
        command.identity().endpoint(),
        command.identity().source(),
        command.identity().tenantId(),
        command.requestId(),
        command.requestHash(),
        command.expectedState().name(),
        command.expectedVersion(),
        command.expectedLeaseToken(),
        command.expectedLeaseToken(),
        command.targetState().name(),
        command.newLeaseToken());
  }

  private int recover(final IdempotencyTransitionCommand command) {
    return jdbcTemplate.update(
        RECOVER_SQL,
        command.newLeaseToken(),
        Timestamp.from(command.leaseExpiresAt()),
        command.identity().environment(),
        command.identity().endpoint(),
        command.identity().source(),
        command.identity().tenantId(),
        command.requestId(),
        command.requestHash(),
        command.expectedVersion());
  }

  private IdempotencyRecordView selectExact(
      final PersistentGuardIdentity identity, final String requestId) {
    final List<Map<String, Object>> rows =
        jdbcTemplate.queryForList(
            SELECT_SQL,
            identity.environment(),
            identity.endpoint(),
            identity.source(),
            identity.tenantId(),
            requestId);
    if (rows.isEmpty()) {
      return null;
    }
    if (rows.size() != 1) {
      throw new PersistentGuardStateException("idempotency exact identity returned multiple rows");
    }
    return map(rows.get(0));
  }

  private static IdempotencyRecordView map(final Map<String, Object> row) {
    return new IdempotencyRecordView(
        uuid(row.get("guard_id")),
        new PersistentGuardIdentity(
            text(row, "environment"),
            text(row, "endpoint"),
            text(row, "source"),
            text(row, "tenant_id")),
        text(row, "request_id"),
        text(row, "request_hash"),
        IdempotencyState.valueOf(text(row, "state")),
        number(row, "state_version").longValue(),
        uuidOrNull(row.get("lease_token")),
        instantOrNull(row.get("lease_expires_at")),
        nullableText(row.get("result_id")),
        nullableText(row.get("result_checksum")),
        nullableText(row.get("stable_error_code")),
        instant(row.get("expires_at")),
        instant(row.get("retention_until")));
  }

  private static IdempotencyAdmissionStatus status(final IdempotencyState state) {
    return switch (state) {
      case RECEIVED, IN_PROGRESS -> IdempotencyAdmissionStatus.IN_PROGRESS;
      case COMPLETED -> IdempotencyAdmissionStatus.COMPLETED;
      case FAILED -> IdempotencyAdmissionStatus.FAILED;
      case EXPIRED -> IdempotencyAdmissionStatus.EXPIRED;
    };
  }

  private static void validateTransition(final IdempotencyTransitionCommand command) {
    final boolean legal =
        switch (command.expectedState()) {
          case RECEIVED ->
              command.targetState() == IdempotencyState.IN_PROGRESS
                  || command.targetState() == IdempotencyState.EXPIRED;
          case IN_PROGRESS ->
              command.targetState() == IdempotencyState.IN_PROGRESS
                  || command.targetState() == IdempotencyState.COMPLETED
                  || command.targetState() == IdempotencyState.FAILED
                  || command.targetState() == IdempotencyState.EXPIRED;
          case COMPLETED, FAILED, EXPIRED -> false;
        };
    if (!legal) {
      throw new PersistentGuardStateException("illegal idempotency state transition");
    }
    if (command.targetState() == IdempotencyState.IN_PROGRESS
        && (command.newLeaseToken() == null || command.leaseExpiresAt() == null)) {
      throw new PersistentGuardStateException("IN_PROGRESS requires opaque lease");
    }
    if (command.targetState() == IdempotencyState.COMPLETED
        && (isBlank(command.resultId()) || isBlank(command.resultChecksum()))) {
      throw new PersistentGuardStateException("COMPLETED requires safe result reference");
    }
    if (command.targetState() == IdempotencyState.FAILED
        && isBlank(command.stableErrorCode())) {
      throw new PersistentGuardStateException("FAILED requires stable error code");
    }
  }

  private static boolean isExpiredLeaseRecovery(final IdempotencyTransitionCommand command) {
    return command.expectedState() == IdempotencyState.IN_PROGRESS
        && command.targetState() == IdempotencyState.IN_PROGRESS
        && command.expectedLeaseToken() == null;
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }

  private static String text(final Map<String, Object> row, final String key) {
    final String value = nullableText(row.get(key));
    if (value == null) {
      throw new PersistentGuardStateException("required idempotency field missing");
    }
    return value;
  }

  private static String nullableText(final Object value) {
    return value == null || value.toString().isBlank() ? null : value.toString();
  }

  private static Number number(final Map<String, Object> row, final String key) {
    if (row.get(key) instanceof Number number) {
      return number;
    }
    throw new PersistentGuardStateException("idempotency version is invalid");
  }

  private static UUID uuid(final Object value) {
    final UUID result = uuidOrNull(value);
    if (result == null) {
      throw new PersistentGuardStateException("required idempotency UUID missing");
    }
    return result;
  }

  private static UUID uuidOrNull(final Object value) {
    if (value == null) {
      return null;
    }
    return value instanceof UUID uuid ? uuid : UUID.fromString(value.toString());
  }

  private static Instant instant(final Object value) {
    final Instant result = instantOrNull(value);
    if (result == null) {
      throw new PersistentGuardStateException("required idempotency timestamp missing");
    }
    return result;
  }

  private static Instant instantOrNull(final Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    throw new PersistentGuardStateException("idempotency timestamp is invalid");
  }

  private static Timestamp timestampOrNull(final Instant value) {
    return value == null ? null : Timestamp.from(value);
  }
}
