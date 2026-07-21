package com.guidinglight.decisionhub.usecase.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class FeedbackAttributionConcurrencyTest {

  @Test
  void concurrentIdenticalInputProducesOneResultAndOneAuditWithoutSleep() throws Exception {
    final FeedbackAttributionTestSupport.BoundedIdempotencyPort idempotency =
        new FeedbackAttributionTestSupport.BoundedIdempotencyPort(32);
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(idempotency, audit);
    final int workers = 16;
    final CountDownLatch ready = new CountDownLatch(workers);
    final CountDownLatch start = new CountDownLatch(1);
    final ExecutorService executor = Executors.newFixedThreadPool(workers);
    try {
      final List<Future<FeedbackAttributionResult>> futures = new ArrayList<>();
      for (int index = 0; index < workers; index++) {
        futures.add(
            executor.submit(
                synchronizedCall(
                    ready, start, service, FeedbackAttributionTestSupport.validCommand())));
      }
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();
      final Set<FeedbackAttributionResult> results = new HashSet<>();
      for (Future<FeedbackAttributionResult> future : futures) {
        results.add(future.get(10, TimeUnit.SECONDS));
      }

      assertEquals(1, results.size());
      assertEquals(FeedbackStatus.ATTRIBUTED, results.iterator().next().status());
      assertEquals(1, audit.writeCount());
      assertEquals(1, idempotency.size());
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  @Test
  void concurrentConflictingInputAcceptsOneAndRejectsOneWithoutProbabilisticTiming()
      throws Exception {
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(
            new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8), audit);
    final FeedbackAttributionCommand baseline = FeedbackAttributionTestSupport.validCommand();
    final FeedbackAttributionCommand conflict =
        FeedbackAttributionTestSupport.command(
            "tenant-a",
            FeedbackEnvironment.DEV,
            "decision-1",
            "trace-1",
            "observation-1",
            OutcomeSource.DRY_RUN_RESULT,
            ObservedDecisionOutcome.SUCCEEDED,
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.9"),
                AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.6")),
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1",
                AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1"));
    final CountDownLatch ready = new CountDownLatch(2);
    final CountDownLatch start = new CountDownLatch(1);
    final ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      final Future<FeedbackAttributionResult> left =
          executor.submit(synchronizedCall(ready, start, service, baseline));
      final Future<FeedbackAttributionResult> right =
          executor.submit(synchronizedCall(ready, start, service, conflict));
      assertTrue(ready.await(10, TimeUnit.SECONDS));
      start.countDown();

      final List<FeedbackAttributionResult> results =
          List.of(left.get(10, TimeUnit.SECONDS), right.get(10, TimeUnit.SECONDS));
      assertEquals(
          1,
          results.stream().filter(value -> value.status() == FeedbackStatus.ATTRIBUTED).count());
      assertEquals(
          1,
          results.stream()
              .filter(
                  value ->
                      value.errorCode() == FeedbackAttributionErrorCode.IDEMPOTENCY_CONFLICT)
              .count());
      assertEquals(1, audit.writeCount());
    } finally {
      executor.shutdownNow();
      assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
    }
  }

  @Test
  void sameObservationIdIsIsolatedAcrossTenantAndEnvironment() {
    final FeedbackAttributionTestSupport.BoundedIdempotencyPort idempotency =
        new FeedbackAttributionTestSupport.BoundedIdempotencyPort(8);
    final FeedbackAttributionTestSupport.RecordingAuditPort audit =
        new FeedbackAttributionTestSupport.RecordingAuditPort();
    final DefaultFeedbackAttributionService service =
        FeedbackAttributionTestSupport.service(idempotency, audit);

    final FeedbackAttributionResult tenantA =
        service.attribute(FeedbackAttributionTestSupport.validCommand());
    final FeedbackAttributionResult tenantB =
        service.attribute(commandForScope("tenant-b", FeedbackEnvironment.DEV));
    final FeedbackAttributionResult testEnvironment =
        service.attribute(commandForScope("tenant-a", FeedbackEnvironment.TEST));

    assertEquals(FeedbackStatus.ATTRIBUTED, tenantA.status());
    assertEquals(FeedbackStatus.ATTRIBUTED, tenantB.status());
    assertEquals(FeedbackStatus.ATTRIBUTED, testEnvironment.status());
    assertEquals(3, idempotency.size());
    assertEquals(3, audit.writeCount());
  }

  private static Callable<FeedbackAttributionResult> synchronizedCall(
      final CountDownLatch ready,
      final CountDownLatch start,
      final DefaultFeedbackAttributionService service,
      final FeedbackAttributionCommand command) {
    return () -> {
      ready.countDown();
      if (!start.await(10, TimeUnit.SECONDS)) {
        throw new IllegalStateException("test start latch timed out");
      }
      return service.attribute(command);
    };
  }

  private static FeedbackAttributionCommand commandForScope(
      final String tenantId, final FeedbackEnvironment environment) {
    return FeedbackAttributionTestSupport.command(
        tenantId,
        environment,
        "decision-1",
        "trace-1",
        "observation-1",
        OutcomeSource.DRY_RUN_RESULT,
        ObservedDecisionOutcome.SUCCEEDED,
        Map.of(
            AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.8"),
            AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.6")),
        Map.of(
            AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1",
            AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1"));
  }
}
