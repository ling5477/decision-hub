package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.util.Objects;

/** Internal use-case boundary that classifies invalid queries before any JDBC call. */
public final class HistoricalFeedbackEvidenceReadService {

  private final HistoricalFeedbackEvidenceQueryPort queryPort;

  /** Creates the internal-only read service. */
  public HistoricalFeedbackEvidenceReadService(final HistoricalFeedbackEvidenceQueryPort queryPort) {
    this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
  }

  /** Returns one bounded keyset page; validation failures are stable and fail closed. */
  public HistoricalFeedbackEvidencePage read(final HistoricalFeedbackEvidenceQuery query) {
    try {
      return queryPort.query(Objects.requireNonNull(query, "query"));
    } catch (final HistoricalFeedbackEvidenceQuery.ValidationException error) {
      throw new FeedbackPersistenceException(
          FeedbackPersistenceErrorCode.QUERY_VALIDATION_FAILED, error.code().name(), error);
    }
  }
}
