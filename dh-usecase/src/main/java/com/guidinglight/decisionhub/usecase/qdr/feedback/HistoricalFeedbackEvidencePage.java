package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Immutable bounded keyset page for internal historical evidence reads. */
public record HistoricalFeedbackEvidencePage(
    List<HistoricalFeedbackEvidenceView> items,
    boolean hasNext,
    Optional<HistoricalFeedbackEvidenceQuery.Cursor> nextCursor) {

  /** Enforces that a continuation cursor exists exactly when there is another page. */
  public HistoricalFeedbackEvidencePage {
    items = List.copyOf(Objects.requireNonNull(items, "items"));
    nextCursor = Objects.requireNonNull(nextCursor, "nextCursor");
    if (hasNext != nextCursor.isPresent()) {
      throw new IllegalArgumentException("hasNext and nextCursor must agree");
    }
  }

  /** Creates a terminal empty or final page. */
  public static HistoricalFeedbackEvidencePage terminal(final List<HistoricalFeedbackEvidenceView> items) {
    return new HistoricalFeedbackEvidencePage(items, false, Optional.empty());
  }
}
