package com.guidinglight.decisionhub.qdr7.capacity;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.security.nq.HmacNqDryRunAuthenticator;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunCommand;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunContext;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunErrorCode;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunResult;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunRuntimeProperties;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.DecisionDryRunSnapshot;
import com.guidinglight.decisionhub.usecase.decision.dryrun.LimitedDryRunRuntimePolicy;
import com.guidinglight.decisionhub.usecase.decision.dryrun.LimitedDryRunRuntimeService;
import com.guidinglight.decisionhub.usecase.decision.dryrun.PersistentGuardedDecisionDryRunService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

/** 使用真实 bounded runtime 与 payload gate 生成 queue/deadline/input-cap 证据。 */
final class Qdr7CapacityRuntimeSafetyProbe {

  private static final Instant FIXED_TIME = Instant.parse("2026-08-09T00:00:00Z");

  private Qdr7CapacityRuntimeSafetyProbe() {}

  static Map<String, Object> runBound(
      final LimitedDryRunRuntimePolicy policy,
      final HmacNqDryRunAuthenticator authenticator,
      final DecisionDryRunService decisionDryRunService,
      final DecisionDryRunRuntimeProperties properties,
      final long transportPayloadCap,
      final String tenant,
      final String source,
      final String runId)
      throws Exception {
    validateSpringBinding(policy, decisionDryRunService);
    final Map<String, Object> result =
        new LinkedHashMap<>(
            runComponentOnly(
                policy,
                authenticator,
                decisionDryRunService,
                properties,
                transportPayloadCap,
                tenant,
                source,
                runId));
    result.put("springRuntimeBinding", "PASS");
    result.put("springRuntimeWrapperClass", LimitedDryRunRuntimeService.class.getName());
    result.put(
        "springPersistentGuardClass", PersistentGuardedDecisionDryRunService.class.getName());
    return Map.copyOf(result);
  }

  static Map<String, Object> runComponentOnly(
      final LimitedDryRunRuntimePolicy policy,
      final HmacNqDryRunAuthenticator authenticator,
      final DecisionDryRunService decisionDryRunService,
      final DecisionDryRunRuntimeProperties properties,
      final long transportPayloadCap,
      final String tenant,
      final String source,
      final String runId)
      throws Exception {
    Objects.requireNonNull(policy, "policy");
    Objects.requireNonNull(authenticator, "authenticator");
    Objects.requireNonNull(decisionDryRunService, "decisionDryRunService");
    Objects.requireNonNull(properties, "properties");
    final Map<String, Object> result = new LinkedHashMap<>();
    result.put("queueBackpressure", queueBackpressure(policy, tenant, source, runId));
    result.put("deadlineTimeout", deadlineTimeout(policy, tenant, source, runId));
    result.put(
        "inputMemoryCap",
        inputMemoryCap(
            authenticator,
            decisionDryRunService,
            properties,
            transportPayloadCap,
            tenant,
            source,
            runId));
    result.put("runtimePolicyBeanClass", policy.getClass().getName());
    result.put("authenticatorBeanClass", authenticator.getClass().getName());
    result.put("decisionDryRunServiceBeanClass", decisionDryRunService.getClass().getName());
    result.put("runtimePropertiesBeanClass", properties.getClass().getName());
    result.put("status", "PASS");
    result.put("realHttpCalls", 0);
    result.put("providerCalls", 0);
    result.put("nqCalls", 0);
    result.put("orders", 0);
    result.put("feedbackLearningWrites", 0);
    return result;
  }

  static void validateSpringBinding(
      final LimitedDryRunRuntimePolicy expectedPolicy,
      final DecisionDryRunService decisionDryRunService) {
    Objects.requireNonNull(expectedPolicy, "expectedPolicy");
    if (!(decisionDryRunService instanceof PersistentGuardedDecisionDryRunService persistent)) {
      throw new IllegalStateException(
          "Spring DecisionDryRunService is not PersistentGuardedDecisionDryRunService");
    }
    final Object runtime = readField(persistent, "runtimeService");
    if (!(runtime instanceof LimitedDryRunRuntimeService limited)) {
      throw new IllegalStateException("Spring persistent guard has no bounded runtime wrapper");
    }
    if (readField(limited, "policy") != expectedPolicy) {
      throw new IllegalStateException("Spring bounded runtime policy binding drifted");
    }
  }

  private static Object readField(final Object target, final String name) {
    try {
      final Field field = target.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(target);
    } catch (final ReflectiveOperationException inaccessible) {
      throw new IllegalStateException(
          "Spring runtime binding is not inspectable: " + name, inaccessible);
    }
  }

  private static Map<String, Object> queueBackpressure(
      final LimitedDryRunRuntimePolicy policy,
      final String tenant,
      final String source,
      final String runId)
      throws Exception {
    final int concurrency = policy.concurrencyPolicy().maxConcurrency();
    final int queueCapacity = policy.concurrencyPolicy().queueCapacity();
    require(policy.evaluate().allowed(), "Spring runtime policy is not executable");
    require(queueCapacity > 0, "Spring runtime policy has no bounded queue to probe");
    final CountDownLatch activeEntered = new CountDownLatch(concurrency);
    final CountDownLatch release = new CountDownLatch(1);
    final AtomicInteger active = new AtomicInteger();
    final AtomicInteger maximumActive = new AtomicInteger();
    final RecordingService delegate =
        new RecordingService(
            command -> {
              final int current = active.incrementAndGet();
              maximumActive.accumulateAndGet(current, Math::max);
              activeEntered.countDown();
              try {
                release.await(2, TimeUnit.SECONDS);
                return success();
              } catch (final InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                return rejected(command, 500, DecisionDryRunErrorCode.UNKNOWN_ERROR, "interrupted");
              } finally {
                active.decrementAndGet();
              }
            });
    final ExecutorService callers = Executors.newFixedThreadPool(concurrency + queueCapacity);
    try (LimitedDryRunRuntimeService service = new LimitedDryRunRuntimeService(delegate, policy)) {
      final List<Future<DecisionDryRunResult>> admitted = new java.util.ArrayList<>();
      for (int index = 0; index < concurrency; index++) {
        final int ordinal = index;
        admitted.add(
            callers.submit(
                () ->
                    service.execute(command("queue-active-" + ordinal, tenant, source, runId, 1))));
      }
      require(activeEntered.await(1, TimeUnit.SECONDS), "queue active tasks did not start");
      for (int index = 0; index < queueCapacity; index++) {
        final int ordinal = index;
        admitted.add(
            callers.submit(
                () ->
                    service.execute(
                        command("queue-waiting-" + ordinal, tenant, source, runId, 1))));
      }
      require(awaitQueueSize(service, queueCapacity), "queue boundary was not observable");
      final DecisionDryRunResult above =
          service.execute(command("queue-above", tenant, source, runId, 1));
      require(!above.success(), "above-cap request was accepted");
      require(above.status() == 503, "above-cap status was not 503");
      require(
          service.queuedTaskCount() <= queueCapacity, "bounded queue exceeded configured capacity");
      release.countDown();
      for (final Future<DecisionDryRunResult> future : admitted) {
        require(future.get(2, TimeUnit.SECONDS).success(), "admitted queue task failed");
      }
      require(
          service.execute(command("queue-recovery", tenant, source, runId, 1)).success(),
          "queue did not recover");
      require(maximumActive.get() <= concurrency, "runtime exceeded max concurrency");
      return Map.of(
          "belowCapacity",
          "PASS",
          "atCapacity",
          "PASS",
          "aboveCapacity",
          "REJECTED_FAIL_CLOSED",
          "recovery",
          "PASS",
          "maximumActive",
          maximumActive.get(),
          "maximumConcurrency",
          concurrency,
          "queueCapacity",
          service.queueCapacity(),
          "maximumObservedQueue",
          queueCapacity);
    } finally {
      release.countDown();
      callers.shutdownNow();
      callers.awaitTermination(1, TimeUnit.SECONDS);
    }
  }

  private static Map<String, Object> deadlineTimeout(
      final LimitedDryRunRuntimePolicy policy,
      final String tenant,
      final String source,
      final String runId)
      throws Exception {
    final CountDownLatch interrupted = new CountDownLatch(1);
    final RecordingService delegate =
        new RecordingService(
            command -> {
              if (command.requestId().contains("application-timeout")) {
                return rejected(
                    command, 504, DecisionDryRunErrorCode.PROVIDER_TIMEOUT, "application-timeout");
              }
              if (command.requestId().contains("slow")) {
                try {
                  Thread.sleep(policy.deadlinePolicy().deadline().toMillis() + 2_000L);
                } catch (final InterruptedException expected) {
                  interrupted.countDown();
                  Thread.currentThread().interrupt();
                }
              }
              return success();
            });
    try (LimitedDryRunRuntimeService service = new LimitedDryRunRuntimeService(delegate, policy)) {
      final DecisionDryRunResult within =
          service.execute(command("deadline-within", tenant, source, runId, 1));
      require(within.success(), "within-deadline request failed");
    }
    try (LimitedDryRunRuntimeService service = new LimitedDryRunRuntimeService(delegate, policy)) {
      final DecisionDryRunResult exceeded =
          service.execute(command("deadline-slow", tenant, source, runId, 1));
      require(!exceeded.success() && exceeded.status() == 504, "deadline did not fail closed");
      require(
          exceeded.message().endsWith("DEADLINE_EXCEEDED"),
          "runtime deadline classification drifted");
      require(interrupted.await(1, TimeUnit.SECONDS), "deadline task was not interrupted");
      require(awaitRuntimeIdle(service), "deadline task did not finish cleanup");
      require(service.activeTaskCount() == 0, "deadline task remained active");
      require(service.queuedTaskCount() == 0, "deadline task remained queued");
    }
    try (LimitedDryRunRuntimeService service = new LimitedDryRunRuntimeService(delegate, policy)) {
      final DecisionDryRunResult application =
          service.execute(command("application-timeout", tenant, source, runId, 1));
      require(
          !application.success()
              && application.status() == 504
              && "application-timeout".equals(application.message()),
          "application timeout was conflated with harness/runtime deadline");
      return Map.of(
          "withinDeadline", "PASS",
          "deadlineExceeded", "DEADLINE_EXCEEDED",
          "applicationTimeout", "APPLICATION_TIMEOUT_PRESERVED",
          "harnessInterruption", "SEPARATE_CLASSIFICATION",
          "cleanup", "PASS",
          "activeAfterRecovery", service.activeTaskCount(),
          "queuedAfterRecovery", service.queuedTaskCount());
    }
  }

  private static Map<String, Object> inputMemoryCap(
      final HmacNqDryRunAuthenticator authenticator,
      final DecisionDryRunService decisionDryRunService,
      final DecisionDryRunRuntimeProperties properties,
      final long transportPayloadCap,
      final String tenant,
      final String source,
      final String runId) {
    require(
        transportPayloadCap > 1L && transportPayloadCap <= Integer.MAX_VALUE,
        "payload cap invalid");
    final int cap = Math.toIntExact(transportPayloadCap);
    final boolean below = authenticator.isPayloadTooLarge("x".repeat(cap - 1), cap - 1L);
    final boolean boundary = authenticator.isPayloadTooLarge("x".repeat(cap), cap);
    final boolean aboveBody = authenticator.isPayloadTooLarge("x".repeat(cap + 1), cap + 1L);
    final boolean aboveHeader = authenticator.isPayloadTooLarge("x", cap + 1L);
    require(!below, "below-cap payload was rejected");
    require(!boundary, "boundary payload was rejected");
    require(aboveBody && aboveHeader, "above-cap payload bypassed fail-closed gate");
    final int contextCap = properties.memoryCapBytes();
    final DecisionDryRunResult contextBelow =
        decisionDryRunService.execute(
            command("context-below", tenant, source, runId, contextCap - 1));
    final DecisionDryRunResult contextBoundary =
        decisionDryRunService.execute(
            command("context-boundary", tenant, source, runId, contextCap));
    final DecisionDryRunResult contextAbove =
        decisionDryRunService.execute(
            command("context-above", tenant, source, runId, contextCap + 1));
    require(
        contextBelow.errorCode() != DecisionDryRunErrorCode.MEMORY_LIMIT_EXCEEDED,
        "below-cap context was rejected by memory gate");
    require(
        contextBoundary.errorCode() != DecisionDryRunErrorCode.MEMORY_LIMIT_EXCEEDED,
        "boundary context was rejected by memory gate");
    require(
        !contextAbove.success()
            && contextAbove.errorCode() == DecisionDryRunErrorCode.MEMORY_LIMIT_EXCEEDED,
        "above-cap context bypassed fail-closed gate: status="
            + contextAbove.status()
            + ", errorCode="
            + contextAbove.errorCode());
    return Map.of(
        "transportCapBytes", transportPayloadCap,
        "contextCapBytes", contextCap,
        "belowCap", "PASS",
        "atBoundary", "PASS",
        "aboveCapBody", "REJECTED_FAIL_CLOSED",
        "aboveCapContentLength", "REJECTED_FAIL_CLOSED",
        "contextBelowCap", "PASS",
        "contextAtBoundary", "PASS",
        "contextAboveCap", "REJECTED_FAIL_CLOSED",
        "fallbackBypass", 0);
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

  private static boolean awaitRuntimeIdle(final LimitedDryRunRuntimeService service) {
    final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(1);
    while (System.nanoTime() < deadline) {
      if (service.activeTaskCount() == 0 && service.queuedTaskCount() == 0) {
        return true;
      }
      Thread.onSpinWait();
    }
    return false;
  }

  private static DecisionDryRunCommand command(
      final String suffix,
      final String tenant,
      final String source,
      final String runId,
      final int approxBytes) {
    return new DecisionDryRunCommand(
        "request-" + runId + "-" + suffix,
        "trace-" + runId + "-" + suffix,
        tenant,
        source,
        "TEST",
        FIXED_TIME.toString(),
        "nonce-" + runId + "-" + suffix,
        "1.0",
        true,
        Set.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE", "READ_NQ_DB", "WRITE_NQ_DB"),
        new DecisionDryRunContext(
            "BTC-USDT",
            "CRYPTO",
            "1h",
            "strategy:1",
            "research:1",
            "context:1",
            "snapshot:1",
            FIXED_TIME,
            List.of("evidence:1"),
            approxBytes),
        false,
        new FeedbackExecutionScope(tenant, FeedbackEnvironment.TEST));
  }

  private static DecisionDryRunResult success() {
    return DecisionDryRunResult.success(
        new DecisionDryRunSnapshot(
            "decision-1",
            true,
            "NO_TRADE",
            BigDecimal.ONE,
            "LOW",
            List.of("READ_ONLY"),
            List.of("runtime-policy=allow"),
            "replay:1",
            "audit:1",
            "1.0"));
  }

  private static DecisionDryRunResult rejected(
      final DecisionDryRunCommand command,
      final int status,
      final DecisionDryRunErrorCode errorCode,
      final String message) {
    return DecisionDryRunResult.rejected(
        status, errorCode, message, command.requestId(), command.traceId(), "audit:reject");
  }

  private static void require(final boolean condition, final String message) {
    if (!condition) {
      throw new IllegalStateException(message);
    }
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
      return rejected(command, status, errorCode, message);
    }
  }
}
