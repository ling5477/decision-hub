package com.guidinglight.decisionhub.usecase.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

/** Unit tests for retention policy validation, the disabled default, and injected-clock cutoffs. */
class FeedbackRetentionServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-26T12:00:00Z");

  @Test
  void defaultsAreDisabledAndDoNotInvokeThePersistencePort() {
    final RecordingPort port = new RecordingPort();
    final FeedbackRetentionService service = service(port);

    final FeedbackRetentionResult result =
        service.cleanup(FeedbackRetentionCommand.defaults("tenant-a", FeedbackEnvironment.DEV));

    assertEquals(FeedbackRetentionResult.Status.DISABLED, result.status());
    assertEquals(NOW.minus(Duration.ofDays(365)), result.cutoff());
    assertEquals(100, result.requestedBatchSize());
    assertEquals(0, port.calls.get());
  }

  @Test
  void enabledCommandUsesInjectedClockAndInvokesExactlyOneBoundedCleanup() {
    final RecordingPort port = new RecordingPort();
    final FeedbackRetentionService service = service(port);
    final FeedbackRetentionCommand command =
        new FeedbackRetentionCommand(
            "tenant-a", FeedbackEnvironment.TEST, true, Duration.ofDays(30), 1);

    final FeedbackRetentionResult result = service.cleanup(command);

    assertEquals(1, port.calls.get());
    assertEquals(NOW.minus(Duration.ofDays(30)), port.cutoff);
    assertEquals(FeedbackRetentionResult.Status.COMPLETED, result.status());
    assertEquals(1, result.deletedAggregateCount());
  }

  @Test
  void invalidScopeAgeAndBatchAreClassifiedBeforePersistence() {
    final FeedbackPersistenceException invalidScope =
        assertThrows(
            FeedbackPersistenceException.class,
            () ->
                new FeedbackRetentionCommand(
                    " ", FeedbackEnvironment.DEV, true, Duration.ofDays(1), 1));
    assertEquals(FeedbackPersistenceErrorCode.INVALID_RETENTION_SCOPE, invalidScope.errorCode());
    final FeedbackPersistenceException invalidAge =
        assertThrows(
            FeedbackPersistenceException.class,
            () ->
                new FeedbackRetentionCommand(
                    "tenant-a", FeedbackEnvironment.DEV, true, Duration.ZERO, 1));
    assertEquals(FeedbackPersistenceErrorCode.INVALID_RETENTION_AGE, invalidAge.errorCode());
    final FeedbackPersistenceException invalidBatch =
        assertThrows(
            FeedbackPersistenceException.class,
            () ->
                new FeedbackRetentionCommand(
                    "tenant-a", FeedbackEnvironment.DEV, true, Duration.ofDays(1), 101));
    assertEquals(FeedbackPersistenceErrorCode.INVALID_BATCH_SIZE, invalidBatch.errorCode());
  }

  @Test
  void timeoutResultIsReturnedWithoutAnAutomaticRetry() {
    final AtomicInteger calls = new AtomicInteger();
    final FeedbackRetentionPort timeoutPort =
        (command, cutoff) -> {
          calls.incrementAndGet();
          return FeedbackRetentionResult.failed(
              command, cutoff, 0, 0, 0, FeedbackPersistenceErrorCode.RETENTION_TIMEOUT);
        };

    final FeedbackRetentionResult result =
        service(timeoutPort)
            .cleanup(
                new FeedbackRetentionCommand(
                    "tenant-a", FeedbackEnvironment.DEV, true, Duration.ofDays(1), 1));

    assertEquals(FeedbackRetentionResult.Status.FAILED, result.status());
    assertEquals(FeedbackPersistenceErrorCode.RETENTION_TIMEOUT, result.failureCode());
    assertEquals(1, calls.get());
  }

  private static FeedbackRetentionService service(final FeedbackRetentionPort port) {
    return new FeedbackRetentionService(port, Clock.fixed(NOW, ZoneOffset.UTC));
  }

  private static final class RecordingPort implements FeedbackRetentionPort {
    private final AtomicInteger calls = new AtomicInteger();
    private Instant cutoff;

    @Override
    public FeedbackRetentionResult cleanup(
        final FeedbackRetentionCommand command, final Instant value) {
      calls.incrementAndGet();
      cutoff = value;
      return new FeedbackRetentionResult(
          command.tenantId(),
          command.environment(),
          value,
          command.batchSize(),
          1,
          1,
          0,
          0,
          1,
          false,
          FeedbackRetentionResult.Status.COMPLETED,
          null);
    }
  }
}
