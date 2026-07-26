package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackIntegrityPort;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackIntegrityReport;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionCommand;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/** Bounded PostgreSQL integrity inspector; it never repairs or deletes malformed feedback rows. */
public final class JdbcFeedbackIntegrityAdapter implements FeedbackIntegrityPort {

  private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
  private static final String CANDIDATES =
      "select o.observation_id,o.canonical_hash from qdr_feedback_outcome_observation o"
          + " where o.tenant_id=? and o.environment=? and o.observed_at<?"
          + " order by o.observed_at asc,o.observation_id asc limit ?";
  private static final String ATTRIBUTIONS =
      "select a.observation_id,a.attribution_id,a.canonical_hash from qdr_feedback_attribution a"
          + " where a.tenant_id=? and a.environment=? and a.observation_id in (%s)";
  private static final String CONTRIBUTIONS =
      "select attribution_id,count(*),min(sort_order),max(sort_order),count(distinct dimension),"
          + " count(distinct sort_order) from qdr_feedback_attribution_contribution"
          + " where tenant_id=? and environment=? and attribution_id in (%s) group by attribution_id";
  private static final String REFERENCES =
      "select attribution_id,count(*),"
          + " sum(case when reference_type not in ('AUDIT','REPLAY','EVALUATION','EVIDENCE')"
          + " or reference_status not in ('ACTIVE','RELEASED','INVALID')"
          + " or lower(reference_value) like '%%raw_prompt%%'"
          + " or lower(reference_value) like '%%raw-provider%%'"
          + " or lower(reference_value) like '%%payload%%'"
          + " or lower(reference_value) like '%%credential%%'"
          + " then 1 else 0 end),"
          + " sum(case when reference_type in ('AUDIT','REPLAY','EVALUATION')"
          + " and reference_status='ACTIVE' then 1 else 0 end)"
          + " from qdr_feedback_attribution_reference"
          + " where tenant_id=? and environment=? and attribution_id in (%s) group by attribution_id";

  private final JdbcTemplate jdbc;

  /** Creates an internal-only integrity adapter against the DH PostgreSQL datasource. */
  public JdbcFeedbackIntegrityAdapter(final JdbcTemplate jdbcTemplate) {
    this.jdbc = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  /**
   * Inspects at most one tenant/environment-bound retention batch.
   *
   * <p>A report never repairs inconsistencies. A candidate is complete only when it has exactly one matching
   * attribution, identical valid canonical hashes, contiguous contribution ordering, valid bounded references,
   * and no active retention hold.
   */
  @Override
  public FeedbackIntegrityReport inspect(
      final FeedbackRetentionCommand command, final Instant cutoff) {
    final FeedbackRetentionCommand checked = Objects.requireNonNull(command, "command");
    final Instant checkedCutoff = Objects.requireNonNull(cutoff, "cutoff");
    try {
      final List<Candidate> candidates = candidates(checked, checkedCutoff);
      if (candidates.isEmpty()) {
        return new FeedbackIntegrityReport(0, 0, 0);
      }
      final Map<String, List<Attribution>> attributions = attributions(checked, candidates);
      final List<String> attributionIds =
          attributions.values().stream().flatMap(List::stream).map(Attribution::attributionId).toList();
      final Map<String, ContributionFacts> contributions = contributions(checked, attributionIds);
      final Map<String, ReferenceFacts> references = references(checked, attributionIds);
      int complete = 0;
      for (Candidate candidate : candidates) {
        final List<Attribution> matches = attributions.getOrDefault(candidate.observationId(), List.of());
        if (matches.size() != 1) {
          continue;
        }
        final Attribution attribution = matches.getFirst();
        if (!validHash(candidate.canonicalHash())
            || !candidate.canonicalHash().equals(attribution.canonicalHash())
            || !validHash(attribution.attributionId())
            || !contributions
                .getOrDefault(attribution.attributionId(), ContributionFacts.incomplete())
                .complete()
            || !references
                .getOrDefault(attribution.attributionId(), ReferenceFacts.incomplete())
                .complete()) {
          continue;
        }
        complete++;
      }
      return new FeedbackIntegrityReport(candidates.size(), complete, candidates.size() - complete);
    } catch (final DataAccessException error) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.RETENTION_FAILURE,
          "feedback retention integrity inspection failed",
          error);
    }
  }

  private List<Candidate> candidates(
      final FeedbackRetentionCommand command, final Instant cutoff) {
    return jdbc.query(
        CANDIDATES,
        (row, ignored) -> new Candidate(row.getString(1), row.getString(2)),
        command.tenantId(),
        command.environment().name(),
        Timestamp.from(cutoff),
        command.batchSize());
  }

  private Map<String, List<Attribution>> attributions(
      final FeedbackRetentionCommand command, final List<Candidate> candidates) {
    final Map<String, List<Attribution>> result = new HashMap<>();
    final List<String> observationIds = candidates.stream().map(Candidate::observationId).toList();
    jdbc.query(
            ATTRIBUTIONS.formatted(marks(observationIds.size())),
            (row, ignored) -> new Attribution(row.getString(1), row.getString(2), row.getString(3)),
            values(command, observationIds).toArray())
        .forEach(
            attribution ->
                result
                    .computeIfAbsent(attribution.observationId(), ignored -> new ArrayList<>())
                    .add(attribution));
    return result;
  }

  private Map<String, ContributionFacts> contributions(
      final FeedbackRetentionCommand command, final List<String> attributionIds) {
    final Map<String, ContributionFacts> result = new HashMap<>();
    if (attributionIds.isEmpty()) {
      return result;
    }
    jdbc.query(
            CONTRIBUTIONS.formatted(marks(attributionIds.size())),
            (row, ignored) ->
                Map.entry(
                    row.getString(1),
                    new ContributionFacts(
                        row.getLong(2), row.getInt(3), row.getInt(4), row.getLong(5), row.getLong(6))),
            values(command, attributionIds).toArray())
        .forEach(value -> result.put(value.getKey(), value.getValue()));
    return result;
  }

  private Map<String, ReferenceFacts> references(
      final FeedbackRetentionCommand command, final List<String> attributionIds) {
    final Map<String, ReferenceFacts> result = new HashMap<>();
    if (attributionIds.isEmpty()) {
      return result;
    }
    jdbc.query(
            REFERENCES.formatted(marks(attributionIds.size())),
            (row, ignored) -> Map.entry(row.getString(1), new ReferenceFacts(row.getLong(2), row.getLong(3), row.getLong(4))),
            values(command, attributionIds).toArray())
        .forEach(value -> result.put(value.getKey(), value.getValue()));
    return result;
  }

  private static boolean validHash(final String value) {
    return value != null && SHA_256.matcher(value).matches();
  }

  private static List<Object> values(
      final FeedbackRetentionCommand command, final List<String> identities) {
    final List<Object> result = new ArrayList<>(List.of(command.tenantId(), command.environment().name()));
    result.addAll(identities);
    return result;
  }

  private static String marks(final int count) {
    return String.join(",", java.util.Collections.nCopies(count, "?"));
  }

  private record Candidate(String observationId, String canonicalHash) {}

  private record Attribution(String observationId, String attributionId, String canonicalHash) {}

  private record ContributionFacts(
      long count, int minimumSortOrder, int maximumSortOrder, long distinctDimensions, long distinctSortOrders) {

    private static ContributionFacts incomplete() {
      return new ContributionFacts(0, 0, 0, 0, 0);
    }

    private boolean complete() {
      return count > 0
          && minimumSortOrder == 0
          && maximumSortOrder == count - 1
          && distinctDimensions == count
          && distinctSortOrders == count;
    }
  }

  private record ReferenceFacts(long count, long invalidCount, long activeHoldCount) {

    private static ReferenceFacts incomplete() {
      return new ReferenceFacts(0, 1, 0);
    }

    private boolean complete() {
      return count > 0 && invalidCount == 0 && activeHoldCount == 0;
    }
  }
}
