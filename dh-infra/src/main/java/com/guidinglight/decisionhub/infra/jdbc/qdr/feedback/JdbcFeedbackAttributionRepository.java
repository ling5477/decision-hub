package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionImpact;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionRepository;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackAttributionErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionContributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.AttributionReferenceRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackAttributionRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.FeedbackPersistenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.OutcomeObservationRecord;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * V15 四关系 feedback aggregate 的 JDBC adapter。
 *
 * <p>所有 lookup 都要求 tenant/environment，所有写入由 usecase 的 single transaction 编排。该 adapter
 * 不提供列表 read model、delete、retention 或外部连接。
 */
public final class JdbcFeedbackAttributionRepository implements FeedbackAttributionRepository {

  private static final String SELECT_OBSERVATION_BY_KEY =
      "select id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,"
          + "outcome_source,outcome_status,observed_at,evaluation_time,canonical_hash"
          + " from qdr_feedback_outcome_observation"
          + " where tenant_id=? and environment=? and idempotency_key=?";
  private static final String SELECT_OBSERVATION_BY_PROJECTION =
      "select id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,"
          + "outcome_source,outcome_status,observed_at,evaluation_time,canonical_hash"
          + " from qdr_feedback_outcome_observation"
          + " where tenant_id=? and environment=? and observation_id=? and decision_id=?"
          + " and trace_id=? and observed_at=?";
  private static final String SELECT_ATTRIBUTION_BY_OBSERVATION =
      "select id,tenant_id,environment,observation_id,decision_id,trace_id,observed_at,"
          + "attribution_id,policy_id,policy_version,attribution_status,confidence,canonical_hash,"
          + "error_code from qdr_feedback_attribution"
          + " where tenant_id=? and environment=? and observation_id=? and decision_id=?"
          + " and trace_id=? and observed_at=?";
  private static final String SELECT_ATTRIBUTION_BY_ID =
      "select id,tenant_id,environment,observation_id,decision_id,trace_id,observed_at,"
          + "attribution_id,policy_id,policy_version,attribution_status,confidence,canonical_hash,"
          + "error_code from qdr_feedback_attribution"
          + " where tenant_id=? and environment=? and attribution_id=?";
  private static final String SELECT_CONTRIBUTIONS =
      "select id,tenant_id,environment,attribution_id,dimension,measurement,contribution,impact,"
          + "confidence,reason_code,evidence_ref,sort_order"
          + " from qdr_feedback_attribution_contribution"
          + " where tenant_id=? and environment=? and attribution_id=? order by sort_order asc";
  private static final String SELECT_REFERENCES =
      "select id,tenant_id,environment,attribution_id,reference_type,reference_value,"
          + "reference_status from qdr_feedback_attribution_reference"
          + " where tenant_id=? and environment=? and attribution_id=?"
          + " order by case reference_type when 'AUDIT' then 0 when 'REPLAY' then 1"
          + " when 'EVALUATION' then 2 when 'EVIDENCE' then 3 end, reference_value";
  private static final String INSERT_OBSERVATION =
      "insert into qdr_feedback_outcome_observation"
          + " (id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,"
          + "outcome_source,outcome_status,observed_at,evaluation_time,canonical_hash)"
          + " values (?,?,?,?,?,?,?,?,?,?,?,?)";
  private static final String INSERT_ATTRIBUTION =
      "insert into qdr_feedback_attribution"
          + " (id,tenant_id,environment,observation_id,decision_id,trace_id,observed_at,"
          + "attribution_id,policy_id,policy_version,attribution_status,confidence,canonical_hash,"
          + "error_code) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
  private static final String INSERT_CONTRIBUTION =
      "insert into qdr_feedback_attribution_contribution"
          + " (id,tenant_id,environment,attribution_id,dimension,measurement,contribution,impact,"
          + "confidence,reason_code,evidence_ref,sort_order) values (?,?,?,?,?,?,?,?,?,?,?,?)";
  private static final String INSERT_REFERENCE =
      "insert into qdr_feedback_attribution_reference"
          + " (id,tenant_id,environment,attribution_id,reference_type,reference_value,"
          + "reference_status) values (?,?,?,?,?,?,?)";

  private final JdbcTemplate jdbcTemplate;

  /** 创建只访问 DH V15 feedback tables 的 adapter。 */
  public JdbcFeedbackAttributionRepository(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public Optional<FeedbackPersistenceAggregate> findByIdempotencyKey(
      final String tenantId,
      final FeedbackEnvironment environment,
      final String idempotencyKey) {
    return load(
        selectOne(
            SELECT_OBSERVATION_BY_KEY,
            this::mapObservation,
            tenantId,
            environment.name(),
            idempotencyKey));
  }

  @Override
  public Optional<FeedbackPersistenceAggregate> findByAttributionId(
      final String tenantId,
      final FeedbackEnvironment environment,
      final String attributionId) {
    try {
      final Optional<FeedbackAttributionRecord> attribution =
          selectOne(
              SELECT_ATTRIBUTION_BY_ID,
              this::mapAttribution,
              tenantId,
              environment.name(),
              attributionId);
      if (attribution.isEmpty()) {
        return Optional.empty();
      }
      final FeedbackAttributionRecord checked = attribution.orElseThrow();
      return load(
          selectOne(
              SELECT_OBSERVATION_BY_PROJECTION,
              this::mapObservation,
              checked.tenantId(),
              checked.environment().name(),
              checked.observationId(),
              checked.decisionId(),
              checked.traceId(),
              timestamp(checked.observedAt())));
    } catch (final DataAccessException error) {
      throw persistenceFailure("feedback attribution lookup failed", error);
    }
  }

  @Override
  public void insertObservation(final OutcomeObservationRecord observation) {
    final OutcomeObservationRecord checked = Objects.requireNonNull(observation, "observation");
    update(
        INSERT_OBSERVATION,
        checked.id(),
        checked.tenantId(),
        checked.environment().name(),
        checked.decisionId(),
        checked.traceId(),
        checked.observationId(),
        checked.idempotencyKey(),
        checked.outcomeSource().name(),
        checked.outcomeStatus().name(),
        timestamp(checked.observedAt()),
        timestamp(checked.evaluationTime()),
        checked.canonicalHash());
  }

  @Override
  public void insertAttribution(final FeedbackAttributionRecord attribution) {
    final FeedbackAttributionRecord checked = Objects.requireNonNull(attribution, "attribution");
    update(
        INSERT_ATTRIBUTION,
        checked.id(),
        checked.tenantId(),
        checked.environment().name(),
        checked.observationId(),
        checked.decisionId(),
        checked.traceId(),
        timestamp(checked.observedAt()),
        checked.attributionId(),
        checked.policyId(),
        checked.policyVersion(),
        checked.attributionStatus().name(),
        checked.confidence(),
        checked.canonicalHash(),
        checked.errorCode().name());
  }

  @Override
  public void insertContributions(final List<AttributionContributionRecord> contributions) {
    for (AttributionContributionRecord contribution :
        List.copyOf(Objects.requireNonNull(contributions, "contributions"))) {
      update(
          INSERT_CONTRIBUTION,
          contribution.id(),
          contribution.tenantId(),
          contribution.environment().name(),
          contribution.attributionId(),
          contribution.dimension().name(),
          contribution.measurement(),
          contribution.contribution(),
          contribution.impact().name(),
          contribution.confidence(),
          contribution.reasonCode(),
          contribution.evidenceRef(),
          contribution.sortOrder());
    }
  }

  @Override
  public void insertReferences(final List<AttributionReferenceRecord> references) {
    for (AttributionReferenceRecord reference :
        List.copyOf(Objects.requireNonNull(references, "references"))) {
      update(
          INSERT_REFERENCE,
          reference.id(),
          reference.tenantId(),
          reference.environment().name(),
          reference.attributionId(),
          reference.referenceType().name(),
          reference.referenceValue(),
          reference.referenceStatus().name());
    }
  }

  private Optional<FeedbackPersistenceAggregate> load(
      final Optional<OutcomeObservationRecord> observation) {
    try {
      if (observation.isEmpty()) {
        return Optional.empty();
      }
      final OutcomeObservationRecord checkedObservation = observation.orElseThrow();
      final FeedbackAttributionRecord attribution =
          selectOne(
                  SELECT_ATTRIBUTION_BY_OBSERVATION,
                  this::mapAttribution,
                  checkedObservation.tenantId(),
                  checkedObservation.environment().name(),
                  checkedObservation.observationId(),
                  checkedObservation.decisionId(),
                  checkedObservation.traceId(),
                  timestamp(checkedObservation.observedAt()))
              .orElseThrow(
                  () ->
                      failure(
                          FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE,
                          "feedback observation has no attribution"));
      final List<AttributionContributionRecord> contributions =
          jdbcTemplate.query(
              SELECT_CONTRIBUTIONS,
              this::mapContribution,
              attribution.tenantId(),
              attribution.environment().name(),
              attribution.attributionId());
      final List<AttributionReferenceRecord> references =
          jdbcTemplate.query(
              SELECT_REFERENCES,
              this::mapReference,
              attribution.tenantId(),
              attribution.environment().name(),
              attribution.attributionId());
      return Optional.of(
          new FeedbackPersistenceAggregate(checkedObservation, attribution, contributions, references));
    } catch (final FeedbackPersistenceException error) {
      throw error;
    } catch (final DataAccessException error) {
      throw persistenceFailure("feedback aggregate lookup failed", error);
    } catch (final IllegalArgumentException error) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE,
          "feedback aggregate contains invalid persisted values",
          error);
    }
  }

  private <T> Optional<T> selectOne(
      final String sql, final RowMapper<T> mapper, final Object... parameters) {
    final List<T> rows = jdbcTemplate.query(sql, mapper, parameters);
    if (rows.isEmpty()) {
      return Optional.empty();
    }
    if (rows.size() != 1) {
      throw failure(
          FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE,
          "feedback aggregate scoped lookup returned multiple rows");
    }
    return Optional.of(rows.getFirst());
  }

  private OutcomeObservationRecord mapObservation(final ResultSet row, final int ignored)
      throws SQLException {
    return new OutcomeObservationRecord(
        uuid(row, "id"),
        row.getString("tenant_id"),
        FeedbackEnvironment.valueOf(row.getString("environment")),
        row.getString("decision_id"),
        row.getString("trace_id"),
        row.getString("observation_id"),
        row.getString("idempotency_key"),
        OutcomeSource.valueOf(row.getString("outcome_source")),
        ObservedDecisionOutcome.valueOf(row.getString("outcome_status")),
        instant(row, "observed_at"),
        instant(row, "evaluation_time"),
        row.getString("canonical_hash"));
  }

  private FeedbackAttributionRecord mapAttribution(final ResultSet row, final int ignored)
      throws SQLException {
    return new FeedbackAttributionRecord(
        uuid(row, "id"),
        row.getString("tenant_id"),
        FeedbackEnvironment.valueOf(row.getString("environment")),
        row.getString("observation_id"),
        row.getString("decision_id"),
        row.getString("trace_id"),
        instant(row, "observed_at"),
        row.getString("attribution_id"),
        row.getString("policy_id"),
        row.getString("policy_version"),
        FeedbackStatus.valueOf(row.getString("attribution_status")),
        row.getBigDecimal("confidence"),
        row.getString("canonical_hash"),
        FeedbackAttributionErrorCode.valueOf(row.getString("error_code")));
  }

  private AttributionContributionRecord mapContribution(final ResultSet row, final int ignored)
      throws SQLException {
    return new AttributionContributionRecord(
        uuid(row, "id"),
        row.getString("tenant_id"),
        FeedbackEnvironment.valueOf(row.getString("environment")),
        row.getString("attribution_id"),
        AttributionDimension.valueOf(row.getString("dimension")),
        row.getBigDecimal("measurement"),
        row.getBigDecimal("contribution"),
        AttributionImpact.valueOf(row.getString("impact")),
        row.getBigDecimal("confidence"),
        row.getString("reason_code"),
        row.getString("evidence_ref"),
        row.getInt("sort_order"));
  }

  private AttributionReferenceRecord mapReference(final ResultSet row, final int ignored)
      throws SQLException {
    return new AttributionReferenceRecord(
        uuid(row, "id"),
        row.getString("tenant_id"),
        FeedbackEnvironment.valueOf(row.getString("environment")),
        row.getString("attribution_id"),
        ReferenceType.valueOf(row.getString("reference_type")),
        row.getString("reference_value"),
        ReferenceStatus.valueOf(row.getString("reference_status")));
  }

  private void update(final String sql, final Object... parameters) {
    try {
      jdbcTemplate.update(sql, parameters);
    } catch (final DuplicateKeyException error) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.DUPLICATE_KEY,
          "feedback aggregate write encountered a duplicate key",
          error);
    } catch (final DataAccessException error) {
      throw persistenceFailure("feedback aggregate write failed", error);
    }
  }

  private static UUID uuid(final ResultSet row, final String column) throws SQLException {
    final Object value = row.getObject(column);
    if (value instanceof UUID uuid) {
      return uuid;
    }
    if (value != null) {
      return UUID.fromString(value.toString());
    }
    throw new SQLException("required UUID is null");
  }

  private static Instant instant(final ResultSet row, final String column) throws SQLException {
    final Timestamp value = row.getTimestamp(column);
    if (value == null) {
      throw new SQLException("required timestamp is null");
    }
    return value.toInstant();
  }

  private static Timestamp timestamp(final Instant value) {
    return Timestamp.from(Objects.requireNonNull(value, "value"));
  }

  private static FeedbackPersistenceException failure(
      final FeedbackPersistenceErrorCode code, final String message) {
    return new FeedbackPersistenceException(code, message);
  }

  private static FeedbackPersistenceException persistenceFailure(
      final String message, final DataAccessException error) {
    return new FeedbackPersistenceException(
        FeedbackPersistenceErrorCode.PERSISTENCE_FAILURE, message, error);
  }
}
