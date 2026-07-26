package com.guidinglight.decisionhub.usecase.qdr.feedback;

/** Bounded internal integrity summary; it intentionally contains no aggregate or reference identities. */
public record FeedbackIntegrityReport(int inspectedCount, int completeCount, int blockedCount) {

  /** Ensures the report cannot claim more aggregates than it inspected. */
  public FeedbackIntegrityReport {
    if (inspectedCount < 0
        || inspectedCount > FeedbackRetentionCommand.MAX_BATCH_SIZE
        || completeCount < 0
        || blockedCount < 0
        || completeCount + blockedCount != inspectedCount) {
      throw new IllegalArgumentException("retention integrity report counts are inconsistent");
    }
  }
}
