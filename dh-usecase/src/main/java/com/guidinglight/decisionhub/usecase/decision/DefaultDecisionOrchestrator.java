package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyResult;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskReview;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * K3 默认 orchestrator。
 *
 * <p>流程固定为 request audit -> policy -> context -> mock signal -> risk -> output/audit。该实现只依赖
 * usecase port，不依赖 JDBC；无 HTTP、无 NQ runtime、无真实 provider、无 LangGraph runtime。任何
 * audit/snapshot/trace/output 写失败都会 fail-closed。
 */
public final class DefaultDecisionOrchestrator implements DecisionOrchestrator {

  private static final String UNKNOWN_REQUEST = "unknown-request";
  private static final String UNKNOWN_TRACE = "unknown-trace";
  private static final String UNKNOWN_TENANT = "unknown-tenant";
  private static final String UNKNOWN_SOURCE = "unknown-source";
  private static final String MOCK_PROVIDER_NAME = "MOCK_DECISION_PROVIDER";
  private static final String REDACTED = "[REDACTED]";

  private final DecisionContextBuilder contextBuilder;
  private final DecisionPolicyChecker policyChecker;
  private final DecisionSignalProvider signalProvider;
  private final DecisionRiskReviewer riskReviewer;
  private final DecisionOutputAssembler outputAssembler;
  private final DecisionAuditRepository auditRepository;
  private final Clock clock;

  /** 使用 K3 默认 mock-only 组件和内存审计仓储创建 orchestrator。 */
  public DefaultDecisionOrchestrator() {
    this(
        new DefaultDecisionContextBuilder(),
        new DefaultDecisionPolicyChecker(),
        new MockDecisionSignalProvider(),
        new DefaultDecisionRiskReviewer(),
        new DecisionOutputAssembler(),
        new InMemoryDecisionAuditRepository(),
        Clock.systemUTC());
  }

  /**
   * 创建兼容 K2 单元测试的 orchestrator，默认使用内存审计仓储。
   *
   * @param contextBuilder 只读上下文构造器
   * @param policyChecker read-only policy checker
   * @param signalProvider deterministic mock provider
   * @param riskReviewer deterministic risk reviewer
   * @param outputAssembler fail-closed output assembler
   * @param clock 输出时间源
   */
  public DefaultDecisionOrchestrator(
      final DecisionContextBuilder contextBuilder,
      final DecisionPolicyChecker policyChecker,
      final DecisionSignalProvider signalProvider,
      final DecisionRiskReviewer riskReviewer,
      final DecisionOutputAssembler outputAssembler,
      final Clock clock) {
    this(
        contextBuilder,
        policyChecker,
        signalProvider,
        riskReviewer,
        outputAssembler,
        new InMemoryDecisionAuditRepository(),
        clock);
  }

  /**
   * 创建可注入持久化端口的 orchestrator。
   *
   * @param contextBuilder 只读上下文构造器
   * @param policyChecker read-only policy checker
   * @param signalProvider deterministic mock provider
   * @param riskReviewer deterministic risk reviewer
   * @param outputAssembler fail-closed output assembler
   * @param auditRepository K3 audit / snapshot / trace / output 持久化端口
   * @param clock 输出时间源
   */
  public DefaultDecisionOrchestrator(
      final DecisionContextBuilder contextBuilder,
      final DecisionPolicyChecker policyChecker,
      final DecisionSignalProvider signalProvider,
      final DecisionRiskReviewer riskReviewer,
      final DecisionOutputAssembler outputAssembler,
      final DecisionAuditRepository auditRepository,
      final Clock clock) {
    this.contextBuilder = Objects.requireNonNull(contextBuilder, "contextBuilder");
    this.policyChecker = Objects.requireNonNull(policyChecker, "policyChecker");
    this.signalProvider = Objects.requireNonNull(signalProvider, "signalProvider");
    this.riskReviewer = Objects.requireNonNull(riskReviewer, "riskReviewer");
    this.outputAssembler = Objects.requireNonNull(outputAssembler, "outputAssembler");
    this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * 执行 K3 只读编排，并保证异常路径也返回结构化 output。
   *
   * @param request K1 冻结请求合同；null 会被 policy 拒绝
   * @return read-only structured output
   */
  @Override
  public DecisionOutput decide(final DecisionRequest request) {
    final DecisionRun run = DecisionRun.from(request, clock.instant());
    DecisionTraceStepName currentStep = DecisionTraceStepName.POLICY_CHECK;
    try {
      persist(() -> auditRepository.saveRequest(requestRecord(run, request)));

      final Instant policyStarted = traceStarted(run, DecisionTraceStepName.POLICY_CHECK);
      final DecisionPolicyResult policy = policyChecker.check(request);
      traceCompleted(run, DecisionTraceStepName.POLICY_CHECK, policyStarted, null);
      if (!policy.isAllowed()) {
        final DecisionOutput output = outputAssembler.policyDenied(request, policy, clock.instant());
        return finishTerminal(
            run,
            output,
            DecisionAuditEventType.POLICY_DENIED,
            DecisionAuditEventStatus.FAILED,
            "POLICY_DENIED");
      }

      currentStep = DecisionTraceStepName.CONTEXT_BUILD;
      final Instant contextStarted = traceStarted(run, DecisionTraceStepName.CONTEXT_BUILD);
      final DecisionContext context = contextBuilder.build(request);
      traceCompleted(run, DecisionTraceStepName.CONTEXT_BUILD, contextStarted, null);
      persist(() -> auditRepository.saveContextSnapshot(contextSnapshotRecord(run, context)));
      if (!context.hasEvidence()) {
        final DecisionOutput output = outputAssembler.noEvidence(request, clock.instant());
        return finishTerminal(
            run,
            output,
            DecisionAuditEventType.DECISION_FAILED,
            DecisionAuditEventStatus.FAILED,
            "NO_EVIDENCE");
      }

      currentStep = DecisionTraceStepName.MOCK_PROVIDER_SIGNAL;
      final Instant providerStarted = traceStarted(run, DecisionTraceStepName.MOCK_PROVIDER_SIGNAL);
      final DecisionSignalResult signal;
      try {
        signal = signalProvider.signal(context);
      } catch (final RuntimeException providerError) {
        final Instant failedAt = clock.instant();
        traceFailed(
            run,
            DecisionTraceStepName.MOCK_PROVIDER_SIGNAL,
            providerStarted,
            "PROVIDER_EXCEPTION",
            providerError);
        persist(
            () ->
                auditRepository.saveProviderCall(
                    providerCallRecord(
                        run,
                        ProviderSignalStatus.FAILED,
                        Map.of("reasonCode", "PROVIDER_EXCEPTION"),
                        "PROVIDER_EXCEPTION",
                        providerStarted,
                        failedAt)));
        final DecisionOutput output =
            DecisionOutput.abstainForProviderFailure(
                requestId(request),
                traceId(request),
                tenantId(request),
                ProviderSignalStatus.FAILED,
                clock.instant());
        return finishTerminal(
            run,
            output,
            DecisionAuditEventType.PROVIDER_FAILED,
            DecisionAuditEventStatus.FAILED,
            "PROVIDER_EXCEPTION");
      }
      final Instant providerEnded = clock.instant();
      traceCompleted(run, DecisionTraceStepName.MOCK_PROVIDER_SIGNAL, providerStarted, null);
      persist(
          () ->
              auditRepository.saveProviderCall(
                  providerCallRecord(
                      run,
                      signal.status(),
                      signalJson(signal),
                      signal.requiresAbstain() ? signal.status().name() : null,
                      providerStarted,
                      providerEnded)));

      currentStep = DecisionTraceStepName.RISK_REVIEW;
      final Instant riskStarted = traceStarted(run, DecisionTraceStepName.RISK_REVIEW);
      final DecisionRiskReview risk = riskReviewer.review(context, signal);
      traceCompleted(run, DecisionTraceStepName.RISK_REVIEW, riskStarted, null);

      final DecisionOutput output =
          outputAssembler.assemble(context, policy, signal, risk, clock.instant());
      return finishTerminal(
          run,
          output,
          eventTypeFor(output, signal),
          eventStatusFor(output),
          errorCodeFor(output, signal));
    } catch (final DecisionPersistenceException error) {
      return failClosedForPersistenceFailure(run, request, error);
    } catch (final RuntimeException error) {
      return failClosedForInternalFailure(run, request, currentStep, error);
    }
  }

  private DecisionOutput finishTerminal(
      final DecisionRun run,
      final DecisionOutput output,
      final DecisionAuditEventType eventType,
      final DecisionAuditEventStatus eventStatus,
      final String errorCode) {
    final Instant outputStarted = traceStarted(run, DecisionTraceStepName.OUTPUT_WRITE);
    persist(() -> auditRepository.saveOutput(outputRecord(run, output)));
    traceCompleted(run, DecisionTraceStepName.OUTPUT_WRITE, outputStarted, null);

    final Instant auditStarted = traceStarted(run, DecisionTraceStepName.AUDIT_WRITE);
    persist(
        () ->
            auditRepository.saveAuditEvent(
                auditEventRecord(run, output, eventType, eventStatus, errorCode)));
    traceCompleted(run, DecisionTraceStepName.AUDIT_WRITE, auditStarted, null);
    return output;
  }

  private DecisionOutput failClosedForPersistenceFailure(
      final DecisionRun run, final DecisionRequest request, final RuntimeException error) {
    final DecisionOutput output = outputAssembler.persistenceFailure(request, clock.instant());
    try {
      auditRepository.saveOutput(outputRecord(run, output));
      auditRepository.saveAuditEvent(
          auditEventRecord(
              run,
              output,
              DecisionAuditEventType.PERSISTENCE_FAILED,
              DecisionAuditEventStatus.FAILED,
              "PERSISTENCE_FAILURE"));
    } catch (final RuntimeException ignored) {
      // 连 fail-closed output/audit 都无法落库时，仍返回结构化 ABSTAIN，避免调用方收到普通成功或裸异常。
    }
    return output;
  }

  private DecisionOutput failClosedForInternalFailure(
      final DecisionRun run,
      final DecisionRequest request,
      final DecisionTraceStepName currentStep,
      final RuntimeException error) {
    try {
      traceFailed(run, currentStep, clock.instant(), "INTERNAL_EXCEPTION", error);
    } catch (final RuntimeException ignored) {
      return failClosedForPersistenceFailure(run, request, ignored);
    }
    final DecisionOutput output = outputAssembler.unexpectedFailure(request, error, clock.instant());
    try {
      return finishTerminal(
          run,
          output,
          DecisionAuditEventType.DECISION_FAILED,
          DecisionAuditEventStatus.FAILED,
          "INTERNAL_EXCEPTION");
    } catch (final RuntimeException persistenceError) {
      return failClosedForPersistenceFailure(run, request, persistenceError);
    }
  }

  private Instant traceStarted(final DecisionRun run, final DecisionTraceStepName stepName) {
    final Instant startedAt = clock.instant();
    persist(
        () ->
            auditRepository.saveTraceStep(
                traceStepRecord(
                    run,
                    stepName,
                    DecisionTraceStepStatus.STARTED,
                    startedAt,
                    null,
                    null,
                    null)));
    return startedAt;
  }

  private void traceCompleted(
      final DecisionRun run,
      final DecisionTraceStepName stepName,
      final Instant startedAt,
      final String errorCode) {
    persist(
        () ->
            auditRepository.saveTraceStep(
                traceStepRecord(
                    run,
                    stepName,
                    DecisionTraceStepStatus.COMPLETED,
                    startedAt,
                    clock.instant(),
                    errorCode,
                    null)));
  }

  private void traceFailed(
      final DecisionRun run,
      final DecisionTraceStepName stepName,
      final Instant startedAt,
      final String errorCode,
      final RuntimeException error) {
    persist(
        () ->
            auditRepository.saveTraceStep(
                traceStepRecord(
                    run,
                    stepName,
                    DecisionTraceStepStatus.FAILED,
                    startedAt,
                    clock.instant(),
                    errorCode,
                    safeErrorMessage(error))));
  }

  private DecisionPersistenceRecords.RequestRecord requestRecord(
      final DecisionRun run, final DecisionRequest request) {
    return new DecisionPersistenceRecords.RequestRecord(
        run.decisionId,
        requestId(request),
        traceId(request),
        tenantId(request),
        source(request),
        decisionType(request),
        subjectJson(request == null ? null : request.getSubject()),
        sanitize(request == null ? null : request.getContextRef()),
        request == null ? run.startedAt : request.getRequestedAt(),
        request == null ? DecisionRequest.DEFAULT_SCHEMA_VERSION : request.getSchemaVersion(),
        clock.instant());
  }

  private DecisionPersistenceRecords.ContextSnapshotRecord contextSnapshotRecord(
      final DecisionRun run, final DecisionContext context) {
    return new DecisionPersistenceRecords.ContextSnapshotRecord(
        run.decisionId,
        run.tenantId,
        run.traceId,
        contextSnapshotJson(context),
        context.evidenceRefs(),
        clock.instant());
  }

  private DecisionPersistenceRecords.TraceStepRecord traceStepRecord(
      final DecisionRun run,
      final DecisionTraceStepName stepName,
      final DecisionTraceStepStatus stepStatus,
      final Instant startedAt,
      final Instant endedAt,
      final String errorCode,
      final String errorMessage) {
    return new DecisionPersistenceRecords.TraceStepRecord(
        run.nextId("trace"),
        run.decisionId,
        run.tenantId,
        run.traceId,
        stepName,
        stepStatus,
        startedAt,
        endedAt,
        errorCode,
        errorMessage,
        clock.instant());
  }

  private DecisionPersistenceRecords.ProviderCallRecord providerCallRecord(
      final DecisionRun run,
      final ProviderSignalStatus status,
      final Map<String, Object> signalJson,
      final String errorCode,
      final Instant startedAt,
      final Instant endedAt) {
    return new DecisionPersistenceRecords.ProviderCallRecord(
        run.nextId("provider"),
        run.decisionId,
        run.tenantId,
        run.traceId,
        MOCK_PROVIDER_NAME,
        status,
        Math.max(0L, endedAt.toEpochMilli() - startedAt.toEpochMilli()),
        signalJson,
        errorCode,
        clock.instant());
  }

  private DecisionPersistenceRecords.OutputRecord outputRecord(
      final DecisionRun run, final DecisionOutput output) {
    return new DecisionPersistenceRecords.OutputRecord(
        run.decisionId,
        output.getTenantId(),
        output.getTraceId(),
        output.getRequestId(),
        output.getDecisionType(),
        output.getAction(),
        output.getRiskLevel(),
        output.getPolicyStatus(),
        confidenceFor(output),
        outputJson(output),
        clock.instant());
  }

  private DecisionPersistenceRecords.AuditEventRecord auditEventRecord(
      final DecisionRun run,
      final DecisionOutput output,
      final DecisionAuditEventType eventType,
      final DecisionAuditEventStatus eventStatus,
      final String errorCode) {
    return new DecisionPersistenceRecords.AuditEventRecord(
        run.nextId("audit"),
        run.decisionId,
        output.getTenantId(),
        output.getTraceId(),
        eventType,
        eventStatus,
        Map.of(
            "requestId", output.getRequestId(),
            "decisionType", output.getDecisionType().name(),
            "action", output.getAction().name(),
            "status", output.getStatus().name(),
            "riskLevel", output.getRiskLevel().name(),
            "policyStatus", output.getPolicyStatus().name(),
            "providerStatus", output.getProviderStatus().name()),
        errorCode,
        clock.instant());
  }

  private static Map<String, Object> subjectJson(final DecisionSubject subject) {
    if (subject == null) {
      return Map.of();
    }
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("symbol", sanitize(subject.symbol()));
    payload.put("market", sanitize(subject.market()));
    payload.put("timeframe", sanitize(subject.timeframe()));
    if (subject.strategyRef() != null) {
      payload.put("strategyRef", sanitize(subject.strategyRef()));
    }
    if (subject.researchRef() != null) {
      payload.put("researchRef", sanitize(subject.researchRef()));
    }
    return payload;
  }

  private static Map<String, Object> contextSnapshotJson(final DecisionContext context) {
    final DecisionRequest request = context.request();
    if (request.getContextSnapshot() == null) {
      return Map.of("snapshotPresent", false);
    }
    return Map.of(
        "snapshotPresent",
        true,
        "snapshotId",
        sanitize(request.getContextSnapshot().snapshotId()),
        "capturedAt",
        request.getContextSnapshot().capturedAt().toString(),
        "evidenceCount",
        request.getContextSnapshot().evidenceRefs().size());
  }

  private static Map<String, Object> signalJson(final DecisionSignalResult signal) {
    return Map.of(
        "providerMode",
        "MOCK",
        "status",
        signal.status().name(),
        "action",
        signal.action().name(),
        "reasonCodes",
        signal.reasonCodes());
  }

  private static Map<String, Object> outputJson(final DecisionOutput output) {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("requestId", output.getRequestId());
    payload.put("traceId", output.getTraceId());
    payload.put("tenantId", output.getTenantId());
    payload.put("decisionType", output.getDecisionType().name());
    payload.put("action", output.getAction().name());
    payload.put("status", output.getStatus().name());
    payload.put("riskLevel", output.getRiskLevel().name());
    payload.put("policyStatus", output.getPolicyStatus().name());
    payload.put("providerStatus", output.getProviderStatus().name());
    payload.put(
        "forbiddenActions",
        output.getForbiddenActions().stream().map(Enum::name).sorted().toList());
    payload.put("reasonCodes", output.getReasonCodes());
    payload.put("evidenceRefs", output.getEvidenceRefs());
    payload.put("createdAt", output.getCreatedAt().toString());
    payload.put("schemaVersion", output.getSchemaVersion());
    return payload;
  }

  private static DecisionAuditEventType eventTypeFor(
      final DecisionOutput output, final DecisionSignalResult signal) {
    if (signal != null && signal.requiresAbstain()) {
      return DecisionAuditEventType.PROVIDER_FAILED;
    }
    if (riskForbidsDirectionalBias(output.getRiskLevel())) {
      return DecisionAuditEventType.RISK_BLOCKED;
    }
    return DecisionAuditEventType.DECISION_COMPLETED;
  }

  private static DecisionAuditEventStatus eventStatusFor(final DecisionOutput output) {
    return output.getPolicyStatus() == DecisionPolicyStatus.ALLOWED
            && output.getAction() != DecisionAction.ABSTAIN
        ? DecisionAuditEventStatus.SUCCESS
        : DecisionAuditEventStatus.FAILED;
  }

  private static String errorCodeFor(
      final DecisionOutput output, final DecisionSignalResult signal) {
    if (signal != null && signal.requiresAbstain()) {
      return signal.status().name();
    }
    if (riskForbidsDirectionalBias(output.getRiskLevel())) {
      return "RISK_BLOCKED";
    }
    return null;
  }

  private static BigDecimal confidenceFor(final DecisionOutput output) {
    return switch (output.getAction()) {
      case LONG_BIAS, SHORT_BIAS -> new BigDecimal("0.6500");
      case OBSERVE, NO_TRADE -> new BigDecimal("0.5000");
      case ABSTAIN -> BigDecimal.ZERO;
    };
  }

  private static boolean riskForbidsDirectionalBias(final DecisionRiskLevel riskLevel) {
    return riskLevel == DecisionRiskLevel.HIGH || riskLevel == DecisionRiskLevel.BLOCKED;
  }

  private static DecisionType decisionType(final DecisionRequest request) {
    return request == null ? DecisionType.READ_ONLY_RECOMMENDATION : request.getDecisionType();
  }

  private static String requestId(final DecisionRequest request) {
    return request == null ? UNKNOWN_REQUEST : request.getRequestId();
  }

  private static String traceId(final DecisionRequest request) {
    return request == null ? UNKNOWN_TRACE : request.getTraceId();
  }

  private static String tenantId(final DecisionRequest request) {
    return request == null ? UNKNOWN_TENANT : request.getTenantId();
  }

  private static String source(final DecisionRequest request) {
    return request == null ? UNKNOWN_SOURCE : request.getSource();
  }

  private static String sanitize(final String value) {
    if (value == null) {
      return null;
    }
    final String normalized = value.toLowerCase(java.util.Locale.ROOT);
    if (normalized.contains("secret")
        || normalized.contains("token")
        || normalized.contains("credential")
        || normalized.contains("api_key")
        || normalized.contains("apikey")
        || normalized.contains("passphrase")
        || normalized.contains("cookie")
        || normalized.contains("buy")
        || normalized.contains("sell")
        || normalized.contains("place_order")
        || normalized.contains("cancel_order")
        || normalized.contains("placeorder")
        || normalized.contains("cancelorder")
        || normalized.contains("orderid")
        || normalized.contains("accountid")) {
      return REDACTED;
    }
    return value;
  }

  private static String safeErrorMessage(final RuntimeException error) {
    return error == null ? null : error.getClass().getSimpleName();
  }

  private static void persist(final PersistenceWrite write) {
    try {
      write.run();
    } catch (final DecisionPersistenceException error) {
      throw error;
    } catch (final RuntimeException error) {
      throw new DecisionPersistenceException("decision persistence write failed", error);
    }
  }

  @FunctionalInterface
  private interface PersistenceWrite {
    void run();
  }

  private static final class DecisionRun {
    private final String decisionId;
    private final String traceId;
    private final String tenantId;
    private final Instant startedAt;
    private int sequence;

    private DecisionRun(
        final String decisionId, final String traceId, final String tenantId, final Instant startedAt) {
      this.decisionId = decisionId;
      this.traceId = traceId;
      this.tenantId = tenantId;
      this.startedAt = startedAt;
    }

    private static DecisionRun from(final DecisionRequest request, final Instant startedAt) {
      final String requestId = requestId(request);
      return new DecisionRun(requestId, traceId(request), tenantId(request), startedAt);
    }

    private String nextId(final String prefix) {
      sequence++;
      return decisionId + "-" + prefix + "-" + sequence;
    }
  }
}
