package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionImpact;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidencePage;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery.Cursor;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceView;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/** B3 read-only PostgreSQL keyset adapter; every query is tenant/environment scoped. */
public final class JdbcHistoricalFeedbackEvidenceQueryAdapter
    implements HistoricalFeedbackEvidenceQueryPort {

  private static final String PARENT_SELECT =
      "select a.attribution_id,a.observed_at from qdr_feedback_attribution a"
          + " join qdr_feedback_outcome_observation o on o.tenant_id=a.tenant_id"
          + " and o.environment=a.environment and o.observation_id=a.observation_id"
          + " and o.decision_id=a.decision_id and o.trace_id=a.trace_id and o.observed_at=a.observed_at"
          + " where a.tenant_id=? and a.environment=? and a.observed_at>=? and a.observed_at<=?";
  private static final String PARENT_FIELDS =
      "select a.tenant_id,a.environment,a.decision_id,a.trace_id,a.observation_id,a.attribution_id,"
          + "a.observed_at,a.policy_id,a.policy_version,a.attribution_status,a.confidence,"
          + "o.outcome_source,o.outcome_status,o.evaluation_time from qdr_feedback_attribution a"
          + " join qdr_feedback_outcome_observation o on o.tenant_id=a.tenant_id"
          + " and o.environment=a.environment and o.observation_id=a.observation_id"
          + " and o.decision_id=a.decision_id and o.trace_id=a.trace_id and o.observed_at=a.observed_at";
  private static final String CONTRIBUTIONS =
      "select attribution_id,dimension,measurement,contribution,impact,confidence,reason_code,evidence_ref,sort_order"
          + " from qdr_feedback_attribution_contribution where tenant_id=? and environment=?";
  private static final String REFERENCES =
      "select attribution_id,reference_type,reference_value,reference_status"
          + " from qdr_feedback_attribution_reference where tenant_id=? and environment=?";

  private final JdbcTemplate jdbc;
  private final TransactionTemplate readOnlyTransaction;

  /** Creates an adapter with an explicit read-only, READ_COMMITTED transaction boundary. */
  public JdbcHistoricalFeedbackEvidenceQueryAdapter(
      final JdbcTemplate jdbc, final PlatformTransactionManager transactionManager) {
    this.jdbc = Objects.requireNonNull(jdbc, "jdbc");
    this.readOnlyTransaction = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
    this.readOnlyTransaction.setReadOnly(true);
    this.readOnlyTransaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
  }

  @Override
  public HistoricalFeedbackEvidencePage query(final HistoricalFeedbackEvidenceQuery query) {
    try {
      return Objects.requireNonNull(
          readOnlyTransaction.execute(status -> read(Objects.requireNonNull(query, "query"))),
          "read-only query result");
    } catch (final FeedbackPersistenceException error) {
      throw error;
    } catch (final DataAccessException error) {
      throw failure(FeedbackPersistenceErrorCode.QUERY_FAILURE, "historical evidence query failed", error);
    } catch (final IllegalArgumentException error) {
      throw failure(FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE, "historical evidence aggregate is incomplete", error);
    }
  }

  private HistoricalFeedbackEvidencePage read(final HistoricalFeedbackEvidenceQuery query) {
    final List<Object> parameters = new ArrayList<>();
    final StringBuilder sql = new StringBuilder(PARENT_SELECT);
    parameters.add(query.tenantId());
    parameters.add(query.environment().name());
    parameters.add(timestamp(query.fromObservedAt()));
    parameters.add(timestamp(query.toObservedAt()));
    addFilters(sql, parameters, query);
    if (query.cursor() != null) {
      sql.append(" and (a.observed_at<? or (a.observed_at=? and a.attribution_id<?))");
      parameters.add(timestamp(query.cursor().lastObservedAt()));
      parameters.add(timestamp(query.cursor().lastObservedAt()));
      parameters.add(query.cursor().lastAttributionId());
    }
    sql.append(" order by a.observed_at desc,a.attribution_id desc limit ?");
    parameters.add(query.effectivePageSize() + 1);
    final List<ParentKey> candidates = jdbc.query(sql.toString(), this::parentKey, parameters.toArray());
    final boolean hasNext = candidates.size() > query.effectivePageSize();
    final List<ParentKey> keys = candidates.subList(0, Math.min(candidates.size(), query.effectivePageSize()));
    if (keys.isEmpty()) {
      return HistoricalFeedbackEvidencePage.terminal(List.of());
    }
    final Map<String, Parent> parents = loadParents(query, keys);
    final Map<String, List<HistoricalFeedbackEvidenceView.Contribution>> contributions = loadContributions(query, keys);
    final Map<String, List<HistoricalFeedbackEvidenceView.Reference>> references = loadReferences(query, keys);
    final List<HistoricalFeedbackEvidenceView> items = new ArrayList<>();
    for (ParentKey key : keys) {
      final Parent parent = Optional.ofNullable(parents.get(key.attributionId())).orElseThrow(() -> incomplete());
      if (!parent.observedAt().equals(key.observedAt())) {
        throw incomplete();
      }
      final List<HistoricalFeedbackEvidenceView.Contribution> childContributions =
          Optional.ofNullable(contributions.get(key.attributionId())).orElseThrow(() -> incomplete());
      final List<HistoricalFeedbackEvidenceView.Reference> childReferences =
          Optional.ofNullable(references.get(key.attributionId())).orElseThrow(() -> incomplete());
      validateContributionOrder(childContributions);
      items.add(parent.view(childContributions, childReferences));
    }
    final ParentKey last = keys.getLast();
    final Optional<Cursor> next =
        hasNext
            ? Optional.of(new Cursor(1, query.tenantId(), query.environment(), query.filterFingerprint(), last.observedAt(), last.attributionId()))
            : Optional.empty();
    return new HistoricalFeedbackEvidencePage(items, hasNext, next);
  }

  private void addFilters(final StringBuilder sql, final List<Object> parameters, final HistoricalFeedbackEvidenceQuery q) {
    filter(sql, parameters, "a.decision_id", q.decisionId());
    filter(sql, parameters, "a.trace_id", q.traceId());
    filter(sql, parameters, "a.policy_id", q.policyId());
    filter(sql, parameters, "a.policy_version", q.policyVersion());
    filter(sql, parameters, "a.attribution_status", q.attributionStatus() == null ? null : q.attributionStatus().name());
    filter(sql, parameters, "o.outcome_status", q.outcomeStatus() == null ? null : q.outcomeStatus().name());
    if (q.referenceType() != null) {
      sql.append(" and exists (select 1 from qdr_feedback_attribution_reference r where r.tenant_id=a.tenant_id and r.environment=a.environment and r.attribution_id=a.attribution_id and r.reference_type=?)");
      parameters.add(q.referenceType().name());
    }
  }

  private Map<String, Parent> loadParents(final HistoricalFeedbackEvidenceQuery q, final List<ParentKey> keys) {
    final String sql = PARENT_FIELDS + " where a.tenant_id=? and a.environment=? and a.attribution_id in (" + marks(keys.size()) + ")";
    final List<Object> values = new ArrayList<>(List.of(q.tenantId(), q.environment().name()));
    keys.forEach(key -> values.add(key.attributionId()));
    final Map<String, Parent> result = new HashMap<>();
    jdbc.query(sql, this::parent, values.toArray()).forEach(parent -> {
      if (result.put(parent.attributionId(), parent) != null) { throw incomplete(); }
    });
    return result;
  }

  private Map<String, List<HistoricalFeedbackEvidenceView.Contribution>> loadContributions(final HistoricalFeedbackEvidenceQuery q, final List<ParentKey> keys) {
    final String sql = CONTRIBUTIONS + " and attribution_id in (" + marks(keys.size()) + ") order by attribution_id asc,sort_order asc";
    return jdbc.query(sql, this::contribution, values(q, keys).toArray()).stream()
        .collect(Collectors.groupingBy(ChildContribution::attributionId, Collectors.mapping(ChildContribution::view, Collectors.toList())));
  }

  private Map<String, List<HistoricalFeedbackEvidenceView.Reference>> loadReferences(final HistoricalFeedbackEvidenceQuery q, final List<ParentKey> keys) {
    final String sql = REFERENCES + " and attribution_id in (" + marks(keys.size()) + ") order by attribution_id asc,case reference_type when 'AUDIT' then 0 when 'REPLAY' then 1 when 'EVALUATION' then 2 else 3 end,reference_value asc";
    return jdbc.query(sql, this::reference, values(q, keys).toArray()).stream()
        .collect(Collectors.groupingBy(ChildReference::attributionId, Collectors.mapping(ChildReference::view, Collectors.toList())));
  }

  private static List<Object> values(final HistoricalFeedbackEvidenceQuery q, final List<ParentKey> keys) {
    final List<Object> result = new ArrayList<>(List.of(q.tenantId(), q.environment().name()));
    keys.forEach(key -> result.add(key.attributionId()));
    return result;
  }

  private ParentKey parentKey(final ResultSet row, final int ignored) throws SQLException { return new ParentKey(row.getString("attribution_id"), instant(row, "observed_at")); }
  private Parent parent(final ResultSet r, final int ignored) throws SQLException { return new Parent(r.getString("tenant_id"), FeedbackEnvironment.valueOf(r.getString("environment")), r.getString("decision_id"), r.getString("trace_id"), r.getString("observation_id"), r.getString("attribution_id"), instant(r, "observed_at"), OutcomeSource.valueOf(r.getString("outcome_source")), ObservedDecisionOutcome.valueOf(r.getString("outcome_status")), instant(r, "evaluation_time"), r.getString("policy_id"), r.getString("policy_version"), FeedbackStatus.valueOf(r.getString("attribution_status")), r.getBigDecimal("confidence")); }
  private ChildContribution contribution(final ResultSet r, final int ignored) throws SQLException { return new ChildContribution(r.getString("attribution_id"), new HistoricalFeedbackEvidenceView.Contribution(AttributionDimension.valueOf(r.getString("dimension")), r.getBigDecimal("measurement"), r.getBigDecimal("contribution"), AttributionImpact.valueOf(r.getString("impact")), r.getBigDecimal("confidence"), r.getString("reason_code"), r.getString("evidence_ref"), r.getInt("sort_order"))); }
  private ChildReference reference(final ResultSet r, final int ignored) throws SQLException { return new ChildReference(r.getString("attribution_id"), new HistoricalFeedbackEvidenceView.Reference(ReferenceType.valueOf(r.getString("reference_type")), r.getString("reference_value"), ReferenceStatus.valueOf(r.getString("reference_status")))); }
  private static void filter(final StringBuilder sql, final List<Object> values, final String column, final String value) { if (value != null) { sql.append(" and ").append(column).append("=?"); values.add(value); } }
  private static String marks(final int count) { return String.join(",", java.util.Collections.nCopies(count, "?")); }
  private static Instant instant(final ResultSet row, final String column) throws SQLException { final Timestamp value = row.getTimestamp(column); if (value == null) { throw new SQLException("required timestamp is null"); } return value.toInstant(); }
  private static Timestamp timestamp(final Instant value) { return Timestamp.from(value); }
  private static void validateContributionOrder(final List<HistoricalFeedbackEvidenceView.Contribution> values) { if (values.isEmpty()) { throw incomplete(); } values.sort(Comparator.comparingInt(HistoricalFeedbackEvidenceView.Contribution::sortOrder)); for (int i = 0; i < values.size(); i++) { if (values.get(i).sortOrder() != i) { throw incomplete(); } } }
  private static FeedbackPersistenceException incomplete() { return failure(FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE, "historical evidence aggregate is incomplete", null); }
  private static FeedbackPersistenceException failure(final FeedbackPersistenceErrorCode code, final String message, final Throwable cause) { return cause == null ? new FeedbackPersistenceException(code, message) : new FeedbackPersistenceException(code, message, cause); }

  private record ParentKey(String attributionId, Instant observedAt) {}
  private record Parent(String tenantId, FeedbackEnvironment environment, String decisionId, String traceId, String observationId, String attributionId, Instant observedAt, OutcomeSource outcomeSource, ObservedDecisionOutcome outcomeStatus, Instant evaluationTime, String policyId, String policyVersion, FeedbackStatus attributionStatus, BigDecimal confidence) { HistoricalFeedbackEvidenceView view(final List<HistoricalFeedbackEvidenceView.Contribution> contributions, final List<HistoricalFeedbackEvidenceView.Reference> references) { return new HistoricalFeedbackEvidenceView(tenantId, environment, decisionId, traceId, observationId, attributionId, observedAt, outcomeSource, outcomeStatus, evaluationTime, policyId, policyVersion, attributionStatus, confidence, contributions, references); } }
  private record ChildContribution(String attributionId, HistoricalFeedbackEvidenceView.Contribution view) {}
  private record ChildReference(String attributionId, HistoricalFeedbackEvidenceView.Reference view) {}
}
