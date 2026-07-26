package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceRecords.ReferenceType;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionCommand;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionPort;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionResult;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * PostgreSQL implementation of one bounded feedback cleanup batch.
 *
 * <p>The adapter locks candidate observations with {@code FOR UPDATE SKIP LOCKED}, sets a local five-second
 * PostgreSQL statement timeout, validates every reference before deleting anything, writes a bounded safe audit
 * event, and deletes the four V15 relations atomically. It has no scheduler, retry loop, HTTP, or fallback store.
 */
public final class JdbcFeedbackRetentionAdapter implements FeedbackRetentionPort {

  private static final int TIMEOUT_SECONDS = 5;
  private static final int MAX_REFERENCE_ROWS =
      FeedbackRetentionCommand.MAX_BATCH_SIZE * 34 + 1;
  private static final String LOCK_CANDIDATES =
      "select o.observation_id,o.decision_id,o.trace_id,o.observed_at"
          + " from qdr_feedback_outcome_observation o"
          + " where o.tenant_id=? and o.environment=? and o.observed_at<?"
          + " order by o.observed_at asc,o.observation_id asc limit ?"
          + " for update skip locked";
  private static final String LOCK_ATTRIBUTIONS =
      "select a.observation_id,a.attribution_id from qdr_feedback_attribution a"
          + " where a.tenant_id=? and a.environment=? and a.observation_id in (%s) for update";
  private static final String CONTRIBUTION_INTEGRITY =
      "select attribution_id,count(*),min(sort_order),max(sort_order),count(distinct dimension),"
          + " count(distinct sort_order) from qdr_feedback_attribution_contribution"
          + " where tenant_id=? and environment=? and attribution_id in (%s) group by attribution_id";
  private static final String LOCK_REFERENCES =
      "select attribution_id,reference_type,reference_value,reference_status"
          + " from qdr_feedback_attribution_reference where tenant_id=? and environment=?"
          + " and attribution_id in (%s) order by attribution_id,reference_type,reference_value"
          + " limit ? for update";
  private static final String MORE_CANDIDATES =
      "select exists (select 1 from qdr_feedback_outcome_observation o"
          + " where o.tenant_id=? and o.environment=? and o.observed_at<?)";
  private static final String DELETE_REFERENCES =
      "delete from qdr_feedback_attribution_reference"
          + " where tenant_id=? and environment=? and attribution_id in (%s)";
  private static final String DELETE_CONTRIBUTIONS =
      "delete from qdr_feedback_attribution_contribution"
          + " where tenant_id=? and environment=? and attribution_id in (%s)";
  private static final String DELETE_ATTRIBUTIONS =
      "delete from qdr_feedback_attribution"
          + " where tenant_id=? and environment=? and attribution_id in (%s)";
  private static final String DELETE_OBSERVATIONS =
      "delete from qdr_feedback_outcome_observation"
          + " where tenant_id=? and environment=? and observation_id in (%s)";

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transaction;

  /** Creates a required/repeatable-read transaction boundary with a fixed five-second deadline. */
  public JdbcFeedbackRetentionAdapter(
      final JdbcTemplate jdbcTemplate, final PlatformTransactionManager transactionManager) {
    this.jdbc = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.transaction = new TransactionTemplate(Objects.requireNonNull(transactionManager, "transactionManager"));
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    this.transaction.setTimeout(TIMEOUT_SECONDS);
  }

  @Override
  public FeedbackRetentionResult cleanup(
      final FeedbackRetentionCommand command, final Instant cutoff) {
    final FeedbackRetentionCommand checked = Objects.requireNonNull(command, "command");
    final Instant checkedCutoff = Objects.requireNonNull(cutoff, "cutoff");
    try {
      return Objects.requireNonNull(
          transaction.execute(status -> cleanupLocked(checked, checkedCutoff)), "retention transaction result");
    } catch (final QueryTimeoutException | TransactionTimedOutException error) {
      return FeedbackRetentionResult.failed(checked, checkedCutoff, 0, 0, 0,
          FeedbackPersistenceErrorCode.RETENTION_TIMEOUT);
    } catch (final CannotCreateTransactionException error) {
      return FeedbackRetentionResult.failed(checked, checkedCutoff, 0, 0, 0,
          FeedbackPersistenceErrorCode.RETENTION_FAILURE);
    } catch (final DataAccessException error) {
      return FeedbackRetentionResult.failed(checked, checkedCutoff, 0, 0, 0,
          FeedbackPersistenceErrorCode.RETENTION_FAILURE);
    } catch (final FeedbackPersistenceException error) {
      return FeedbackRetentionResult.failed(checked, checkedCutoff, 0, 0, 0, error.errorCode());
    }
  }

  private FeedbackRetentionResult cleanupLocked(
      final FeedbackRetentionCommand command, final Instant cutoff) {
    jdbc.execute("set local statement_timeout = '5000ms'");
    final List<Candidate> candidates = lockCandidates(command, cutoff);
    if (candidates.isEmpty()) {
      return completed(command, cutoff, 0, 0, 0, 0, false);
    }
    final Map<String, List<String>> attributions = lockAttributions(command, candidates);
    final List<Aggregate> complete = new ArrayList<>();
    int blockedIntegrity = 0;
    for (Candidate candidate : candidates) {
      final List<String> matches = attributions.getOrDefault(candidate.observationId(), List.of());
      if (matches.size() != 1) {
        blockedIntegrity++;
      } else {
        complete.add(new Aggregate(candidate, matches.getFirst()));
      }
    }
    if (complete.isEmpty()) {
      return completed(command, cutoff, candidates.size(), 0, blockedIntegrity, 0, hasMore(command, cutoff));
    }
    final Map<String, ContributionIntegrity> contributionIntegrity =
        contributionIntegrity(command, attributionIds(complete));
    final List<Reference> references = lockReferences(command, attributionIds(complete));
    if (references.size() >= MAX_REFERENCE_ROWS) {
      return FeedbackRetentionResult.failed(command, cutoff, candidates.size(), 0, blockedIntegrity,
          FeedbackPersistenceErrorCode.REFERENCE_STATUS_UNKNOWN);
    }
    final Map<String, List<Reference>> referencesByAttribution = groupReferences(references);
    final List<Aggregate> integrityComplete = new ArrayList<>();
    for (Aggregate aggregate : complete) {
      if (!contributionIntegrity
              .getOrDefault(aggregate.attributionId(), ContributionIntegrity.incomplete())
              .complete()
          || referencesByAttribution.getOrDefault(aggregate.attributionId(), List.of()).isEmpty()) {
        blockedIntegrity++;
      } else {
        integrityComplete.add(aggregate);
      }
    }
    final Liveness liveness = validateReferenceLiveness(command, integrityComplete, referencesByAttribution);
    if (liveness.unknown()) {
      return FeedbackRetentionResult.failed(command, cutoff, candidates.size(), liveness.blocked(), blockedIntegrity,
          FeedbackPersistenceErrorCode.REFERENCE_STATUS_UNKNOWN);
    }
    if (liveness.invalid()) {
      return FeedbackRetentionResult.failed(command, cutoff, candidates.size(), liveness.blocked(), blockedIntegrity,
          FeedbackPersistenceErrorCode.RETENTION_BLOCKED);
    }
    final List<Aggregate> deletable =
        integrityComplete.stream().filter(aggregate -> !liveness.blockedAttributions().contains(aggregate.attributionId()))
            .toList();
    if (deletable.isEmpty()) {
      return completed(command, cutoff, candidates.size(), 0, blockedIntegrity, liveness.blocked(),
          hasMore(command, cutoff));
    }
    deleteAggregates(command, cutoff, deletable, referencesByAttribution);
    return completed(command, cutoff, candidates.size(), deletable.size(), blockedIntegrity, liveness.blocked(),
        hasMore(command, cutoff));
  }

  private List<Candidate> lockCandidates(
      final FeedbackRetentionCommand command, final Instant cutoff) {
    return jdbc.query(
        LOCK_CANDIDATES,
        (row, ignored) ->
            new Candidate(
                row.getString("observation_id"),
                row.getString("decision_id"),
                row.getString("trace_id"),
                row.getTimestamp("observed_at").toInstant()),
        command.tenantId(),
        command.environment().name(),
        Timestamp.from(cutoff),
        command.batchSize());
  }

  private Map<String, List<String>> lockAttributions(
      final FeedbackRetentionCommand command, final List<Candidate> candidates) {
    final List<String> observationIds = candidates.stream().map(Candidate::observationId).toList();
    final Map<String, List<String>> result = new HashMap<>();
    jdbc.query(
            LOCK_ATTRIBUTIONS.formatted(marks(observationIds.size())),
            (row, ignored) -> new Attribution(row.getString(1), row.getString(2)),
            scopedValues(command, observationIds).toArray())
        .forEach(value -> result.computeIfAbsent(value.observationId(), ignored -> new ArrayList<>()).add(value.attributionId()));
    return result;
  }

  private Map<String, ContributionIntegrity> contributionIntegrity(
      final FeedbackRetentionCommand command, final List<String> attributionIds) {
    final Map<String, ContributionIntegrity> result = new HashMap<>();
    jdbc.query(
            CONTRIBUTION_INTEGRITY.formatted(marks(attributionIds.size())),
            (row, ignored) ->
                Map.entry(
                    row.getString(1),
                    new ContributionIntegrity(
                        row.getLong(2), row.getInt(3), row.getInt(4), row.getLong(5), row.getLong(6))),
            scopedValues(command, attributionIds).toArray())
        .forEach(value -> result.put(value.getKey(), value.getValue()));
    return result;
  }

  private List<Reference> lockReferences(
      final FeedbackRetentionCommand command, final List<String> attributionIds) {
    final List<Object> values = scopedValues(command, attributionIds);
    values.add(MAX_REFERENCE_ROWS);
    return jdbc.query(
        LOCK_REFERENCES.formatted(marks(attributionIds.size())),
        (row, ignored) ->
            new Reference(
                row.getString(1), row.getString(2), row.getString(3), row.getString(4)),
        values.toArray());
  }

  private Liveness validateReferenceLiveness(
      final FeedbackRetentionCommand command,
      final List<Aggregate> aggregates,
      final Map<String, List<Reference>> referencesByAttribution) {
    final Set<String> blocked = new HashSet<>();
    final List<Target> targets = new ArrayList<>();
    boolean invalid = false;
    for (Aggregate aggregate : aggregates) {
      for (Reference reference : referencesByAttribution.getOrDefault(aggregate.attributionId(), List.of())) {
        final ReferenceType type;
        final ReferenceStatus status;
        try {
          type = ReferenceType.valueOf(reference.type());
          status = ReferenceStatus.valueOf(reference.status());
        } catch (final IllegalArgumentException error) {
          return Liveness.unknown(blocked);
        }
        if (type == ReferenceType.EVIDENCE) {
          if (status == ReferenceStatus.INVALID) {
            blocked.add(aggregate.attributionId());
            invalid = true;
          }
          continue;
        }
        final Target target = Target.parse(type, reference.value(), aggregate);
        if (target == null) {
          return Liveness.unknown(blocked);
        }
        targets.add(target);
        if (status == ReferenceStatus.INVALID) {
          invalid = true;
          blocked.add(aggregate.attributionId());
        } else if (status != ReferenceStatus.RELEASED) {
          blocked.add(aggregate.attributionId());
        }
      }
    }
    if (!targetsExist(command, targets)) {
      return Liveness.unknown(blocked);
    }
    return Liveness.known(blocked, invalid);
  }

  private boolean targetsExist(final FeedbackRetentionCommand command, final List<Target> targets) {
    return auditTargetsExist(command, targets)
        && replayTargetsExist(command, targets, "replay-case:", "select id::text from qdr_replay_case where tenant_id=? and id::text in (%s)")
        && replayTargetsExist(command, targets, "canonical-snapshot:", "select id::text from qdr_canonical_replay_snapshot where tenant_id=? and id::text in (%s)")
        && replayTargetsExist(command, targets, "replay:", "select case_id from qdr_replay_case where tenant_id=? and case_id in (%s)")
        && replayTargetsExist(command, targets, "evaluation:", "select id::text from qdr_evaluation_case where tenant_id=? and id::text in (%s)");
  }

  private boolean auditTargetsExist(final FeedbackRetentionCommand command, final List<Target> targets) {
    final List<Target> audit = targets.stream().filter(value -> value.prefix().equals("audit:")).toList();
    if (audit.isEmpty()) {
      return true;
    }
    final List<Object> values = new ArrayList<>(List.of(command.tenantId()));
    values.addAll(audit.stream().map(Target::id).toList());
    final Map<String, AuditTarget> found = new HashMap<>();
    jdbc.query(
            "select id,decision_id,trace_id from dh_decision_audit_event where tenant_id=? and id in (%s)"
                .formatted(marks(audit.size())),
            (row, ignored) -> new AuditTarget(row.getString(1), row.getString(2), row.getString(3)),
            values.toArray())
        .forEach(value -> found.put(value.id(), value));
    return audit.stream()
        .allMatch(
            target -> {
              final AuditTarget actual = found.get(target.id());
              return actual != null
                  && target.decisionId().equals(actual.decisionId())
                  && target.traceId().equals(actual.traceId());
            });
  }

  private boolean replayTargetsExist(
      final FeedbackRetentionCommand command,
      final List<Target> targets,
      final String prefix,
      final String sql) {
    final List<Target> selected = targets.stream().filter(value -> value.prefix().equals(prefix)).toList();
    if (selected.isEmpty()) {
      return true;
    }
    final List<Object> values = new ArrayList<>(List.of(command.tenantId()));
    values.addAll(selected.stream().map(Target::id).toList());
    final Set<String> found =
        new HashSet<>(
            jdbc.queryForList(sql.formatted(marks(selected.size())), String.class, values.toArray()));
    return selected.stream().allMatch(value -> found.contains(value.id()));
  }

  private void deleteAggregates(
      final FeedbackRetentionCommand command,
      final Instant cutoff,
      final List<Aggregate> aggregates,
      final Map<String, List<Reference>> referencesByAttribution) {
    final List<String> attributionIds = attributionIds(aggregates);
    final List<String> observationIds = aggregates.stream().map(value -> value.candidate().observationId()).toList();
    final int expectedReferences =
        aggregates.stream().mapToInt(value -> referencesByAttribution.get(value.attributionId()).size()).sum();
    final int expectedContributions = contributionTotal(command, attributionIds);
    final int audited = writeDeletionAudit(command, cutoff, attributionIds);
    final int references = update(DELETE_REFERENCES, command, attributionIds);
    final int contributions = update(DELETE_CONTRIBUTIONS, command, attributionIds);
    final int attributions = update(DELETE_ATTRIBUTIONS, command, attributionIds);
    final int observations = update(DELETE_OBSERVATIONS, command, observationIds);
    if (audited != aggregates.size()
        || references != expectedReferences
        || contributions != expectedContributions
        || attributions != aggregates.size()
        || observations != aggregates.size()) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE,
          "feedback retention affected row counts are inconsistent");
    }
  }

  private int contributionTotal(final FeedbackRetentionCommand command, final List<String> attributionIds) {
    return contributionIntegrity(command, attributionIds).values().stream()
        .mapToInt(value -> Math.toIntExact(value.count()))
        .sum();
  }

  private int writeDeletionAudit(
      final FeedbackRetentionCommand command, final Instant cutoff, final List<String> attributionIds) {
    final String sql =
        "insert into dh_decision_audit_event"
            + " (id,decision_id,tenant_id,trace_id,event_type,event_status,event_json,error_code,created_at)"
            + " select concat('qdr9-retention:',attribution_id),decision_id,tenant_id,trace_id,"
            + " 'QDR9_RETENTION_DELETE','SUCCESS',jsonb_build_object("
            + " 'schemaVersion','DH-QDR9-RETENTION-1','environment',?,'cutoff',?,"
            + " 'reasonCode','RETENTION_AGE_EXPIRED','aggregateCount',1,'observationCount',1,"
            + " 'attributionCount',1,'aggregateHash',attribution_id),null,transaction_timestamp()"
            + " from qdr_feedback_attribution where tenant_id=? and environment=? and attribution_id in (%s)";
    final List<Object> values = new ArrayList<>();
    values.add(command.environment().name());
    values.add(cutoff.toString());
    values.addAll(scopedValues(command, attributionIds));
    return jdbc.update(sql.formatted(marks(attributionIds.size())), values.toArray());
  }

  private int update(
      final String statement, final FeedbackRetentionCommand command, final List<String> identities) {
    return jdbc.update(statement.formatted(marks(identities.size())), scopedValues(command, identities).toArray());
  }

  private boolean hasMore(final FeedbackRetentionCommand command, final Instant cutoff) {
    final Boolean result =
        jdbc.queryForObject(
            MORE_CANDIDATES,
            Boolean.class,
            command.tenantId(), command.environment().name(), Timestamp.from(cutoff));
    return Boolean.TRUE.equals(result);
  }

  private static FeedbackRetentionResult completed(
      final FeedbackRetentionCommand command,
      final Instant cutoff,
      final int scanned,
      final int eligible,
      final int integrityBlocked,
      final int referenceBlocked,
      final boolean hasMore) {
    return new FeedbackRetentionResult(
        command.tenantId(), command.environment(), cutoff, command.batchSize(), scanned, eligible,
        referenceBlocked, integrityBlocked, eligible, hasMore, FeedbackRetentionResult.Status.COMPLETED, null);
  }

  private static Map<String, List<Reference>> groupReferences(final List<Reference> references) {
    final Map<String, List<Reference>> result = new HashMap<>();
    references.forEach(value -> result.computeIfAbsent(value.attributionId(), ignored -> new ArrayList<>()).add(value));
    return result;
  }

  private static List<String> attributionIds(final Collection<Aggregate> aggregates) {
    return aggregates.stream().map(Aggregate::attributionId).toList();
  }

  private static List<Object> scopedValues(
      final FeedbackRetentionCommand command, final List<String> identities) {
    final List<Object> values = new ArrayList<>(List.of(command.tenantId(), command.environment().name()));
    values.addAll(identities);
    return values;
  }

  private static String marks(final int count) {
    return String.join(",", java.util.Collections.nCopies(count, "?"));
  }

  private record Candidate(String observationId, String decisionId, String traceId, Instant observedAt) {}

  private record Attribution(String observationId, String attributionId) {}

  private record Aggregate(Candidate candidate, String attributionId) {}

  private record Reference(String attributionId, String type, String value, String status) {}

  private record ContributionIntegrity(
      long count, int minimumSortOrder, int maximumSortOrder, long distinctDimensions, long distinctSortOrders) {

    private static ContributionIntegrity incomplete() {
      return new ContributionIntegrity(0, 0, 0, 0, 0);
    }

    private boolean complete() {
      return count > 0
          && minimumSortOrder == 0
          && maximumSortOrder == count - 1
          && distinctDimensions == count
          && distinctSortOrders == count;
    }
  }

  private record AuditTarget(String id, String decisionId, String traceId) {}

  private record Target(String prefix, String id, String decisionId, String traceId) {

    private static Target parse(
        final ReferenceType type, final String value, final Aggregate aggregate) {
      final String prefix =
          switch (type) {
            case AUDIT -> "audit:";
            case REPLAY -> replayPrefix(value);
            case EVALUATION -> "evaluation:";
            case EVIDENCE -> throw new IllegalArgumentException("evidence has no liveness target");
          };
      if (prefix == null || !value.startsWith(prefix) || value.length() == prefix.length()) {
        return null;
      }
      final String id = value.substring(prefix.length());
      if ((prefix.equals("replay-case:") || prefix.equals("canonical-snapshot:") || prefix.equals("evaluation:"))
          && !uuid(id)) {
        return null;
      }
      return new Target(prefix, id, aggregate.candidate().decisionId(), aggregate.candidate().traceId());
    }

    private static String replayPrefix(final String value) {
      if (value.startsWith("replay-case:")) {
        return "replay-case:";
      }
      if (value.startsWith("canonical-snapshot:")) {
        return "canonical-snapshot:";
      }
      return value.startsWith("replay:") ? "replay:" : null;
    }

    private static boolean uuid(final String value) {
      try {
        UUID.fromString(value);
        return true;
      } catch (final IllegalArgumentException error) {
        return false;
      }
    }
  }

  private record Liveness(Set<String> blockedAttributions, boolean unknown, boolean invalid) {

    private static Liveness known(final Set<String> blocked, final boolean invalid) {
      return new Liveness(Set.copyOf(blocked), false, invalid);
    }

    private static Liveness unknown(final Set<String> blocked) {
      return new Liveness(Set.copyOf(blocked), true, false);
    }

    private int blocked() {
      return blockedAttributions.size();
    }
  }
}
