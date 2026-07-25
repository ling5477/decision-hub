package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.OutcomeObservationRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackReferenceValidationPort;
import java.util.Objects;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 既有 DH audit/replay/evaluation identity 的 tenant-bound JDBC validator。
 *
 * <p>旧表没有 environment，故 validator 始终先保留来自 V15 row 的 tenant/environment scope，再以
 * tenant-bound lookup 证明 target 存在；找不到或无法解析时 fail-closed。
 */
public final class JdbcFeedbackReferenceValidationAdapter
    implements FeedbackReferenceValidationPort {

  private static final String AUDIT_EXISTS =
      "select count(*) from dh_decision_audit_event"
          + " where id=? and tenant_id=? and decision_id=? and trace_id=?";
  private static final String REPLAY_CASE_EXISTS =
      "select count(*) from qdr_replay_case where id=? and tenant_id=?";
  private static final String REPLAY_CASE_ID_EXISTS =
      "select count(*) from qdr_replay_case where case_id=? and tenant_id=?";
  private static final String CANONICAL_SNAPSHOT_EXISTS =
      "select count(*) from qdr_canonical_replay_snapshot where id=? and tenant_id=?";
  private static final String EVALUATION_EXISTS =
      "select count(*) from qdr_evaluation_case where id=? and tenant_id=?";

  private final JdbcTemplate jdbcTemplate;

  /** 创建不会执行外部 IO 的 reference validator。 */
  public JdbcFeedbackReferenceValidationAdapter(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public void confirm(
      final AttributionReferenceRecord reference,
      final OutcomeObservationRecord observation) {
    final AttributionReferenceRecord checkedReference =
        Objects.requireNonNull(reference, "reference");
    final OutcomeObservationRecord checkedObservation =
        Objects.requireNonNull(observation, "observation");
    if (!checkedReference.tenantId().equals(checkedObservation.tenantId())) {
      throw failure(
          FeedbackPersistenceErrorCode.TENANT_SCOPE_MISMATCH,
          "feedback reference tenant does not match observation");
    }
    if (checkedReference.environment() != checkedObservation.environment()) {
      throw failure(
          FeedbackPersistenceErrorCode.ENVIRONMENT_SCOPE_MISMATCH,
          "feedback reference environment does not match observation");
    }
    try {
      final boolean valid =
          switch (checkedReference.referenceType()) {
            case AUDIT -> auditExists(checkedReference.referenceValue(), checkedObservation);
            case REPLAY -> replayExists(checkedReference.referenceValue(), checkedObservation.tenantId());
            case EVALUATION ->
                uuidTargetExists(
                    EVALUATION_EXISTS,
                    requiredSuffix(checkedReference.referenceValue(), "evaluation:"),
                    checkedObservation.tenantId());
            case EVIDENCE -> true;
          };
      if (!valid) {
        throw failure(
            FeedbackPersistenceErrorCode.REFERENCE_INVALID,
            "feedback reference target is unavailable in the current tenant scope");
      }
    } catch (final FeedbackPersistenceException error) {
      throw error;
    } catch (final DataAccessException error) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.PERSISTENCE_FAILURE,
          "feedback reference validation failed",
          error);
    }
  }

  private boolean auditExists(
      final String referenceValue, final OutcomeObservationRecord observation) {
    final String id = requiredSuffix(referenceValue, "audit:");
    return count(AUDIT_EXISTS, id, observation.tenantId(), observation.decisionId(), observation.traceId())
        == 1;
  }

  private boolean replayExists(final String referenceValue, final String tenantId) {
    if (referenceValue.startsWith("replay-case:")) {
      return uuidTargetExists(
          REPLAY_CASE_EXISTS, requiredSuffix(referenceValue, "replay-case:"), tenantId);
    }
    if (referenceValue.startsWith("canonical-snapshot:")) {
      return uuidTargetExists(
          CANONICAL_SNAPSHOT_EXISTS,
          requiredSuffix(referenceValue, "canonical-snapshot:"),
          tenantId);
    }
    if (referenceValue.startsWith("replay:")) {
      return count(REPLAY_CASE_ID_EXISTS, requiredSuffix(referenceValue, "replay:"), tenantId) == 1;
    }
    return false;
  }

  private boolean uuidTargetExists(
      final String sql, final String rawId, final String tenantId) {
    try {
      return count(sql, UUID.fromString(rawId), tenantId) == 1;
    } catch (final IllegalArgumentException error) {
      return false;
    }
  }

  private int count(final String sql, final Object... values) {
    final Integer result = jdbcTemplate.queryForObject(sql, Integer.class, values);
    if (result == null || result < 0 || result > 1) {
      throw failure(
          FeedbackPersistenceErrorCode.REFERENCE_INVALID,
          "feedback reference target lookup is not unique");
    }
    return result;
  }

  private static String requiredSuffix(final String value, final String prefix) {
    if (!value.startsWith(prefix) || value.length() == prefix.length()) {
      throw failure(
          FeedbackPersistenceErrorCode.REFERENCE_INVALID,
          "feedback reference has an invalid identity");
    }
    return value.substring(prefix.length());
  }

  private static FeedbackPersistenceException failure(
      final FeedbackPersistenceErrorCode code, final String message) {
    return new FeedbackPersistenceException(code, message);
  }
}
