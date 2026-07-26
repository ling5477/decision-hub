package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.time.Instant;

/** Read-only, bounded integrity inspection port for the feedback aggregate. */
public interface FeedbackIntegrityPort {

  /** Inspects at most one command-sized tenant/environment candidate set without deleting data. */
  FeedbackIntegrityReport inspect(FeedbackRetentionCommand command, Instant cutoff);
}
