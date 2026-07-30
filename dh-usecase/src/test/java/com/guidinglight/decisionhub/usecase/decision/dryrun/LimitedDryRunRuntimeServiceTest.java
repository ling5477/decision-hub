package com.guidinglight.decisionhub.usecase.decision.dryrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

/** B3 bounded executor、deadline、backpressure 与 no-side-effect 回归。 */
class LimitedDryRunRuntimeServiceTest {

  @Test
  void disabledRuntimeRejectsBeforeDelegateExecution() {
    final AtomicInteger executions = new AtomicInteger();
    final RecordingService delegate =
        new RecordingService(command -> {
          executions.incrementAndGet();
          return success("NO_TRADE");
        });
    final LimitedDryRunRuntimePolicy disabled = policy(false, 1000, 1, 0, 0);

    try (LimitedDryRunRuntimeService service =
        new LimitedDryRunRuntimeService(delegate, disabled)) {
      final DecisionDryRunResult result = service.execute(command("disabled"));

      assertFalse(result.success());
      assertEquals(DecisionDryRunErrorCode.POLICY_DENIED, result.errorCode());
      assertTrue(result.message().endsWith(RuntimeFailureClassification.RUNTIME_DISABLED.name()));
      assertEquals(0, executions.get());
    }
  }

  @Test
  void deadlineCancelsCooperativeTaskWithoutLateBusinessEffect() throws Exception {
    final CountDownLatch interrupted = new CountDownLatch(1);
    final AtomicInteger lateBusinessEffects = new AtomicInteger();
    final RecordingService delegate =
        new RecordingService(command -> {
          try {
            Thread.sleep(5000L);
            lateBusinessEffects.incrementAndGet();
          } catch (final InterruptedException expected) {
            interrupted.countDown();
            Thread.currentThread().interrupt();
          }
          return success("NO_TRADE");
        });

    try (LimitedDryRunRuntimeService service =
        new LimitedDryRunRuntimeService(delegate, policy(true, 50, 1, 0, 0))) {
      final DecisionDryRunResult result = service.execute(command("deadline"));

      assertFalse(result.success());
      assertEquals(DecisionDryRunErrorCode.PROVIDER_TIMEOUT, result.errorCode());
      assertTrue(result.message().endsWith(RuntimeFailureClassification.DEADLINE_EXCEEDED.name()));
      assertTrue(interrupted.await(1, TimeUnit.SECONDS));
      assertEquals(0, lateBusinessEffects.get());
    }
  }

  @Test
  void concurrencyAndQueueStayBoundedAndSaturationRejects() throws Exception {
    final CountDownLatch firstEntered = new CountDownLatch(1);
    final CountDownLatch release = new CountDownLatch(1);
    final AtomicInteger active = new AtomicInteger();
    final AtomicInteger maximumActive = new AtomicInteger();
    final RecordingService delegate =
        new RecordingService(command -> {
          final int current = active.incrementAndGet();
          maximumActive.accumulateAndGet(current, Math::max);
          firstEntered.countDown();
          try {
            release.await(2, TimeUnit.SECONDS);
            return success("OBSERVE");
          } catch (final InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            return DecisionDryRunResult.rejected(
                500,
                DecisionDryRunErrorCode.UNKNOWN_ERROR,
                "interrupted",
                command.requestId(),
                command.traceId(),
                null);
          } finally {
            active.decrementAndGet();
          }
        });
    final ExecutorService callers = Executors.newFixedThreadPool(2);

    try (LimitedDryRunRuntimeService service =
        new LimitedDryRunRuntimeService(delegate, policy(true, 2000, 1, 1, 500))) {
      final Future<DecisionDryRunResult> first =
          callers.submit(() -> service.execute(command("first")));
      assertTrue(firstEntered.await(1, TimeUnit.SECONDS));
      final Future<DecisionDryRunResult> queued =
          callers.submit(() -> service.execute(command("queued")));
      assertTrue(awaitQueueSize(service, 1));

      final DecisionDryRunResult saturated = service.execute(command("saturated"));
      assertFalse(saturated.success());
      assertTrue(
          saturated.message().endsWith(RuntimeFailureClassification.CAPACITY_REJECTED.name()));
      assertEquals(1, service.maxConcurrency());
      assertEquals(1, service.queueCapacity());
      assertTrue(service.queuedTaskCount() <= 1);

      release.countDown();
      assertTrue(first.get(1, TimeUnit.SECONDS).success());
      assertTrue(queued.get(1, TimeUnit.SECONDS).success());
      assertEquals(1, maximumActive.get());
    } finally {
      release.countDown();
      callers.shutdownNow();
      callers.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  @Test
  void forbiddenActionIsConvertedToStructuredNoSideEffectRejection() {
    final RecordingService delegate = new RecordingService(command -> success("BUY"));

    try (LimitedDryRunRuntimeService service =
        new LimitedDryRunRuntimeService(delegate, policy(true, 1000, 1, 0, 0))) {
      final DecisionDryRunResult result = service.execute(command("forbidden-action"));

      assertFalse(result.success());
      assertEquals(DecisionDryRunErrorCode.POLICY_DENIED, result.errorCode());
      assertTrue(
          result.message().endsWith(RuntimeFailureClassification.NO_SIDE_EFFECT_VIOLATION.name()));
    }
  }

  @Test
  void closeStopsFurtherAdmissionAndMarksExecutorClosed() {
    final LimitedDryRunRuntimeService service =
        new LimitedDryRunRuntimeService(
            new RecordingService(command -> success("NO_TRADE")), policy(true, 1000, 1, 0, 0));

    service.close();
    final DecisionDryRunResult result = service.execute(command("closed"));

    assertTrue(service.isClosed());
    assertFalse(result.success());
    assertTrue(
        result.message().endsWith(RuntimeFailureClassification.INTERNAL_RUNTIME_FAILURE.name()));
  }

  private static boolean awaitQueueSize(
      final LimitedDryRunRuntimeService service, final int expected) {
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
    while (System.nanoTime() < deadline) {
      if (service.queuedTaskCount() == expected) {
        return true;
      }
      Thread.onSpinWait();
    }
    return false;
  }

  private static LimitedDryRunRuntimePolicy policy(
      final boolean enabled,
      final long deadlineMillis,
      final int concurrency,
      final int queueCapacity,
      final long queueWaitMillis) {
    return new LimitedDryRunRuntimePolicy(
        new RuntimeFeatureFlag(enabled),
        new RuntimeEnvironmentPolicy(Set.of("test"), false),
        new RuntimeKillSwitch(RuntimeKillSwitch.State.ALLOW),
        new RuntimeDeadlinePolicy(
            Duration.ofMillis(deadlineMillis), Duration.ofMillis(queueWaitMillis)),
        new RuntimeConcurrencyPolicy(concurrency, queueCapacity),
        LimitedDryRunRuntimePolicy.MOCK_PROVIDER_KIND,
        0,
        new NoSideEffectDecisionContract());
  }

  private static DecisionDryRunCommand command(final String suffix) {
    return new DecisionDryRunCommand(
        "request-" + suffix,
        "trace-" + suffix,
        "tenant-a",
        "NQ_DRYRUN",
        "TEST",
        Instant.parse("2026-07-20T00:00:00Z").toString(),
        "nonce-" + suffix,
        "1.0",
        true,
        Set.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE"),
        new DecisionDryRunContext(
            "BTC-USDT",
            "CRYPTO",
            "1h",
            "strategy:1",
            "research:1",
            "context:1",
            "snapshot:1",
            Instant.parse("2026-07-20T00:00:00Z"),
            List.of("evidence:1"),
            1024),
        false,
        new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.TEST));
  }

  private static DecisionDryRunResult success(final String action) {
    return DecisionDryRunResult.success(
        new DecisionDryRunSnapshot(
            "decision-1",
            true,
            action,
            BigDecimal.ONE,
            "LOW",
            List.of("READ_ONLY"),
            List.of("runtime-policy=allow"),
            "replay:1",
            "audit:1",
            "1.0"));
  }

  private static final class RecordingService implements DecisionDryRunService {

    private final Function<DecisionDryRunCommand, DecisionDryRunResult> execution;

    private RecordingService(
        final Function<DecisionDryRunCommand, DecisionDryRunResult> execution) {
      this.execution = execution;
    }

    @Override
    public DecisionDryRunResult execute(final DecisionDryRunCommand command) {
      return execution.apply(command);
    }

    @Override
    public DecisionDryRunResult reject(
        final DecisionDryRunCommand command,
        final int status,
        final DecisionDryRunErrorCode errorCode,
        final String message) {
      return DecisionDryRunResult.rejected(
          status,
          errorCode,
          message,
          command == null ? null : command.requestId(),
          command == null ? null : command.traceId(),
          "audit:reject");
    }
  }
}
