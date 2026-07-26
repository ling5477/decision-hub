package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;

/** Internal use-case service that enforces disabled-by-default retention before any JDBC access. */
public final class FeedbackRetentionService {

  private final FeedbackRetentionPort retentionPort;
  private final Clock clock;

  /** Creates a service whose cutoff is reproducible through an injected clock. */
  public FeedbackRetentionService(final FeedbackRetentionPort retentionPort, final Clock clock) {
    this.retentionPort = Objects.requireNonNull(retentionPort, "retentionPort");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Runs exactly one explicitly enabled cleanup batch, otherwise returns a no-write disabled result. */
  public FeedbackRetentionResult cleanup(final FeedbackRetentionCommand command) {
    final FeedbackRetentionCommand checked = Objects.requireNonNull(command, "command");
    final Instant cutoff = clock.instant().minus(checked.retentionAge());
    if (!checked.enabled()) {
      return FeedbackRetentionResult.disabled(checked, cutoff);
    }
    return retentionPort.cleanup(checked, cutoff);
  }
}
