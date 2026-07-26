package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.time.Instant;

/** Persistence-owned, transactional retention boundary; it is never a scheduler or external API. */
public interface FeedbackRetentionPort {

  /** Executes one locked, bounded cleanup batch with the supplied deterministic cutoff. */
  FeedbackRetentionResult cleanup(FeedbackRetentionCommand command, Instant cutoff);
}
