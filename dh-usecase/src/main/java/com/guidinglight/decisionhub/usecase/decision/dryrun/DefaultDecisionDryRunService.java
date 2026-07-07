package com.guidinglight.decisionhub.usecase.decision.dryrun;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.domain.qdr.DecisionRequestStatus;
import com.guidinglight.decisionhub.domain.qdr.DecisionRun;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.HumanApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.QuantDecision;
import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.QuantSignal;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRequestRepository;
import com.guidinglight.decisionhub.usecase.qdr.DecisionRunRepository;
import com.guidinglight.decisionhub.usecase.qdr.InMemoryDecisionCoreRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantDecisionRepository;
import com.guidinglight.decisionhub.usecase.qdr.QuantSignalRepository;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayFailureCode;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationCommand;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationException;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationPort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.QdrModelGatewayIntegrationResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * limited dry-run endpoint 的默认 usecase 实现。
 *
 * <p>流程为 feature/profile gate -> request policy -> source pair gate -> audit received ->
 * existing mock-only DecisionOrchestrator -> readonly response mapping。实现不接 NQ、不接真实 provider、不发 HTTP、
 * 不读写交易状态；audit 写失败会阻断成功响应并返回 fail-closed error envelope。
 */
public final class DefaultDecisionDryRunService implements DecisionDryRunService {

    private static final String ENDPOINT = "/api/ai/decision-dry-runs";
    private static final String REQUEST_TYPE = "QUANT_DECISION_REVIEW";
    private static final String ORCHESTRATOR_KEY = "DEFAULT_DECISION_ORCHESTRATOR";

    private final DecisionOrchestrator orchestrator;
    private final DecisionAuditRepository auditRepository;
    private final DecisionRequestRepository decisionRequestRepository;
    private final DecisionRunRepository decisionRunRepository;
    private final QuantSignalRepository quantSignalRepository;
    private final QuantDecisionRepository quantDecisionRepository;
    private final QdrModelGatewayIntegrationPort qdrModelGatewayIntegration;
    private final DecisionDryRunRuntimeProperties properties;
    private final Clock clock;

    /**
     * 创建 dry-run service。
     *
     * @param orchestrator    既有 mock-only DecisionOrchestrator。
     * @param auditRepository 既有 audit / trace / replay persistence port。
     * @param qdrModelGatewayIntegration stage-qdr-3 B4 mock gateway integration。
     * @param properties      runtime feature gate 配置。
     * @param clock           时间源。
     */
    public DefaultDecisionDryRunService(
            final DecisionOrchestrator orchestrator,
            final DecisionAuditRepository auditRepository,
            final QdrModelGatewayIntegrationPort qdrModelGatewayIntegration,
            final DecisionDryRunRuntimeProperties properties,
            final Clock clock) {
        this(
                orchestrator,
                auditRepository,
                qdrModelGatewayIntegration,
                new InMemoryDecisionCoreRepository(),
                properties,
                clock);
    }

    private DefaultDecisionDryRunService(
            final DecisionOrchestrator orchestrator,
            final DecisionAuditRepository auditRepository,
            final QdrModelGatewayIntegrationPort qdrModelGatewayIntegration,
            final InMemoryDecisionCoreRepository decisionCoreRepository,
            final DecisionDryRunRuntimeProperties properties,
            final Clock clock) {
        this(
                orchestrator,
                auditRepository,
                qdrModelGatewayIntegration,
                decisionCoreRepository,
                decisionCoreRepository,
                decisionCoreRepository,
                decisionCoreRepository,
                properties,
                clock);
    }

    /**
     * 创建带 Decision Core 主线 repository 的 dry-run service。
     *
     * @param orchestrator              既有 mock-only DecisionOrchestrator。
     * @param auditRepository           既有 audit / trace / replay persistence port。
     * @param qdrModelGatewayIntegration stage-qdr-3 B4 mock gateway integration。
     * @param decisionRequestRepository request 主线 repository。
     * @param decisionRunRepository     run 主线 repository。
     * @param quantSignalRepository     signal 主线 repository。
     * @param quantDecisionRepository   quant decision 主线 repository。
     * @param properties                runtime feature gate 配置。
     * @param clock                     时间源。
     */
    public DefaultDecisionDryRunService(
            final DecisionOrchestrator orchestrator,
            final DecisionAuditRepository auditRepository,
            final QdrModelGatewayIntegrationPort qdrModelGatewayIntegration,
            final DecisionRequestRepository decisionRequestRepository,
            final DecisionRunRepository decisionRunRepository,
            final QuantSignalRepository quantSignalRepository,
            final QuantDecisionRepository quantDecisionRepository,
            final DecisionDryRunRuntimeProperties properties,
            final Clock clock) {
        this.orchestrator = Objects.requireNonNull(orchestrator, "orchestrator");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.qdrModelGatewayIntegration =
                Objects.requireNonNull(qdrModelGatewayIntegration, "qdrModelGatewayIntegration");
        this.decisionRequestRepository =
                Objects.requireNonNull(decisionRequestRepository, "decisionRequestRepository");
        this.decisionRunRepository =
                Objects.requireNonNull(decisionRunRepository, "decisionRunRepository");
        this.quantSignalRepository =
                Objects.requireNonNull(quantSignalRepository, "quantSignalRepository");
        this.quantDecisionRepository =
                Objects.requireNonNull(quantDecisionRepository, "quantDecisionRepository");
        this.properties = Objects.requireNonNull(properties, "properties");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 执行 limited dry-run；任一异常均转换为 fail-closed result。
     *
     * @param command dry-run 命令。
     * @return 成功 snapshot 或 error envelope result。
     */
    @Override
    public DecisionDryRunResult execute(final DecisionDryRunCommand command) {
        DecisionCoreSession decisionCoreSession = null;
        try {
            final DecisionDryRunResult preflight = preflight(command);
            if (preflight != null) {
                return preflight;
            }
            decisionCoreSession = createDecisionCoreSession(command);
            final String receivedAuditRef =
                    writeAudit(
                            command,
                            DecisionAuditEventType.DECISION_COMPLETED,
                            DecisionAuditEventStatus.SUCCESS,
                            null,
                            "DRY_RUN_RECEIVED");
            final DecisionRequest request = toDecisionRequest(command);
            final DecisionOutput output = orchestrator.decide(request);
            final DecisionDryRunResult providerFailure = mapProviderFailure(command, output);
            if (providerFailure != null) {
                recordQuantDecision(decisionCoreSession, command, output, null);
                return providerFailure;
            }
            final QdrModelGatewayIntegrationResult gatewayResult =
                    qdrModelGatewayIntegration.invoke(toGatewayCommand(command, decisionCoreSession, output));
            recordQuantDecision(decisionCoreSession, command, output, gatewayResult);
            final DecisionDryRunSnapshot snapshot = toSnapshot(output, receivedAuditRef, gatewayResult);
            return DecisionDryRunResult.success(snapshot);
        } catch (final QdrModelGatewayIntegrationException error) {
            failDecisionRun(decisionCoreSession, error);
            return rejectWithoutThrowing(
                    command,
                    statusForGatewayFailure(error.failureCode()),
                    errorCodeForGatewayFailure(error.failureCode()),
                    "model gateway failed closed",
                    eventTypeForGatewayFailure(error.failureCode()));
        } catch (final RuntimeException error) {
            failDecisionRun(decisionCoreSession, error);
            return rejectWithoutThrowing(
                    command,
                    500,
                    DecisionDryRunErrorCode.UNKNOWN_ERROR,
                    "dry-run request failed closed",
                    DecisionAuditEventType.DECISION_FAILED);
        }
    }

    /**
     * 记录 controller/security 层拒绝，并统一返回 error envelope result。
     *
     * @param command   已解析出的命令；解析失败时可为 null。
     * @param status    HTTP 状态建议。
     * @param errorCode canonical error code。
     * @param message   脱敏错误摘要。
     * @return fail-closed result。
     */
    @Override
    public DecisionDryRunResult reject(
            final DecisionDryRunCommand command,
            final int status,
            final DecisionDryRunErrorCode errorCode,
            final String message) {
        return rejectWithoutThrowing(command, status, errorCode, message, eventTypeFor(errorCode));
    }

    private DecisionDryRunResult preflight(final DecisionDryRunCommand command) {
        if (command == null) {
            return rejectWithoutThrowing(
                    null,
                    403,
                    DecisionDryRunErrorCode.POLICY_DENIED,
                    "missing dry-run request",
                    DecisionAuditEventType.POLICY_DENIED);
        }
        if (!properties.runtimeEnabled()) {
            return rejectWithoutThrowing(
                    command,
                    403,
                    DecisionDryRunErrorCode.POLICY_DENIED,
                    "dry-run endpoint disabled by feature gate",
                    DecisionAuditEventType.POLICY_DENIED);
        }
        if (!requiredEnvelopePresent(command) || !command.dryRun()) {
            return rejectWithoutThrowing(
                    command,
                    403,
                    DecisionDryRunErrorCode.POLICY_DENIED,
                    "dry-run request envelope is invalid",
                    DecisionAuditEventType.POLICY_DENIED);
        }
        if (!properties.sourceAllowed(command.source())
                || !properties.tenantSourceAllowed(command.tenantId(), command.source())) {
            return rejectWithoutThrowing(
                    command,
                    403,
                    DecisionDryRunErrorCode.SOURCE_DENIED,
                    "dry-run source denied by tenant/source allowlist",
                    DecisionAuditEventType.POLICY_DENIED);
        }
        if (command.forbiddenMaterialDetected()
                || !declaresMandatoryForbiddenCapabilities(command.forbiddenCapabilities())) {
            return rejectWithoutThrowing(
                    command,
                    403,
                    DecisionDryRunErrorCode.POLICY_DENIED,
                    "dry-run request contains forbidden capability or execution material",
                    DecisionAuditEventType.POLICY_DENIED);
        }
        if (command.context() == null || command.context().approxBytes() > properties.memoryCapBytes()) {
            return rejectWithoutThrowing(
                    command,
                    500,
                    DecisionDryRunErrorCode.MEMORY_LIMIT_EXCEEDED,
                    "dry-run context exceeds memory cap",
                    DecisionAuditEventType.DECISION_FAILED);
        }
        if (!requiredContextPresent(command.context())) {
            return rejectWithoutThrowing(
                    command,
                    403,
                    DecisionDryRunErrorCode.POLICY_DENIED,
                    "dry-run context is missing read-only subject fields",
                    DecisionAuditEventType.POLICY_DENIED);
        }
        return null;
    }

    private DecisionDryRunResult mapProviderFailure(
            final DecisionDryRunCommand command, final DecisionOutput output) {
        if (output == null) {
            return rejectWithoutThrowing(
                    command,
                    500,
                    DecisionDryRunErrorCode.UNKNOWN_ERROR,
                    "dry-run orchestrator returned no output",
                    DecisionAuditEventType.DECISION_FAILED);
        }
        if (output.getProviderStatus() == ProviderSignalStatus.DISABLED) {
            return rejectWithoutThrowing(
                    command,
                    503,
                    DecisionDryRunErrorCode.PROVIDER_DISABLED,
                    "provider disabled by guard",
                    DecisionAuditEventType.PROVIDER_FAILED);
        }
        if (output.getProviderStatus() == ProviderSignalStatus.TIMEOUT) {
            return rejectWithoutThrowing(
                    command,
                    504,
                    DecisionDryRunErrorCode.PROVIDER_TIMEOUT,
                    "provider timed out by guard",
                    DecisionAuditEventType.PROVIDER_FAILED);
        }
        if (output.getProviderStatus() == ProviderSignalStatus.BUDGET_EXCEEDED) {
            return rejectWithoutThrowing(
                    command,
                    429,
                    DecisionDryRunErrorCode.BUDGET_EXCEEDED,
                    "provider budget exceeded by guard",
                    DecisionAuditEventType.PROVIDER_FAILED);
        }
        if (output.getPolicyStatus() == DecisionPolicyStatus.DENIED
                || output.getPolicyStatus() == DecisionPolicyStatus.INVALID
                || output.getPolicyStatus() == DecisionPolicyStatus.BLOCKED) {
            return rejectWithoutThrowing(
                    command,
                    403,
                    DecisionDryRunErrorCode.POLICY_DENIED,
                    "dry-run policy denied",
                    DecisionAuditEventType.POLICY_DENIED);
        }
        return null;
    }

    private DecisionCoreSession createDecisionCoreSession(final DecisionDryRunCommand command) {
        final Instant startedAt = clock.instant();
        final UUID requestId = stableUuid("decision-request", command.tenantId(), command.requestId());
        final UUID runId = stableUuid("decision-run", requestId.toString(), "1");
        final UUID signalId = stableUuid("quant-signal", requestId.toString(), command.requestId());

        decisionRequestRepository.save(
                new com.guidinglight.decisionhub.domain.qdr.DecisionRequest(
                        requestId,
                        command.requestId(),
                        REQUEST_TYPE,
                        command.source(),
                        sourceRefId(command),
                        command.tenantId(),
                        command.traceId(),
                        command.requestId(),
                        inputPayload(command),
                        contextPayload(command.context()),
                        DecisionRequestStatus.ACCEPTED,
                        startedAt,
                        startedAt));
        decisionRunRepository.save(
                new DecisionRun(
                        runId,
                        requestId,
                        1,
                        DecisionRunStatus.RUNNING,
                        ORCHESTRATOR_KEY,
                        null,
                        null,
                        startedAt,
                        null,
                        null,
                        null,
                        null,
                        startedAt));
        quantSignalRepository.save(
                new QuantSignal(
                        signalId,
                        requestId,
                        command.source(),
                        command.context().symbol(),
                        command.context().market(),
                        command.context().timeframe(),
                        signalType(command.context()),
                        signalPayload(command.context()),
                        command.context().strategyRef(),
                        null,
                        null,
                        command.context().capturedAt() == null ? startedAt : command.context().capturedAt(),
                        startedAt));
        return new DecisionCoreSession(requestId, runId, signalId, startedAt);
    }

    private void recordQuantDecision(
            final DecisionCoreSession session,
            final DecisionDryRunCommand command,
            final DecisionOutput output,
            final QdrModelGatewayIntegrationResult gatewayResult) {
        final Instant finishedAt = clock.instant();
        final QuantDecisionAction action = quantAction(output);
        final RiskLevel riskLevel = RiskLevel.fromDecisionRiskLevel(output.getRiskLevel());
        final String errorCode = runErrorCode(output);
        final DecisionRunStatus runStatus =
                errorCode == null ? DecisionRunStatus.SUCCEEDED : DecisionRunStatus.FAILED;
        quantDecisionRepository.save(
                new QuantDecision(
                        stableUuid("quant-decision", session.runId().toString(), output.getRequestId()),
                        session.signalId(),
                        session.runId(),
                        action,
                        confidenceFor(action),
                        riskLevel,
                        rationale(output, gatewayResult),
                        constraints(command, output, gatewayResult),
                        HumanApprovalStatus.NOT_REQUIRED,
                        finishedAt));
        decisionRunRepository.complete(
                session.runId(),
                runStatus,
                finishedAt,
                Duration.between(session.startedAt(), finishedAt).toMillis(),
                errorCode,
                errorCode == null ? null : "dry-run decision failed closed");
    }

    private void failDecisionRun(final DecisionCoreSession session, final RuntimeException error) {
        if (session == null) {
            return;
        }
        try {
            final Instant failedAt = clock.instant();
            decisionRunRepository.complete(
                    session.runId(),
                    DecisionRunStatus.FAILED,
                    failedAt,
                    Duration.between(session.startedAt(), failedAt).toMillis(),
                    "DRY_RUN_FAILED_CLOSED",
                    error == null ? "dry-run failed closed" : error.getClass().getSimpleName());
        } catch (final RuntimeException ignored) {
            // 主线失败更新也失败时仍保持 fail-closed 响应；不能为了补偿失败返回成功。
        }
    }

    private QdrModelGatewayIntegrationCommand toGatewayCommand(
            final DecisionDryRunCommand command,
            final DecisionCoreSession decisionCoreSession,
            final DecisionOutput output) {
        return new QdrModelGatewayIntegrationCommand(
                command.tenantId(),
                command.traceId(),
                command.requestId(),
                decisionCoreSession.runId(),
                command.context().symbol(),
                command.context().market(),
                command.context().timeframe(),
                output.getRiskLevel().name(),
                command.context().evidenceRefs());
    }

    private DecisionRequest toDecisionRequest(final DecisionDryRunCommand command) {
        final DecisionDryRunContext context = command.context();
        return DecisionRequest.readOnlyRecommendation(
                command.requestId(),
                command.traceId(),
                command.tenantId(),
                command.source(),
                new DecisionSubject(
                        context.symbol(),
                        context.market(),
                        context.timeframe(),
                        context.strategyRef(),
                        context.researchRef()),
                context.contextRef(),
                new DecisionContextSnapshot(
                        context.snapshotId(),
                        context.capturedAt() == null ? clock.instant() : context.capturedAt(),
                        context.evidenceRefs()),
                clock.instant());
    }

    private DecisionDryRunSnapshot toSnapshot(
            final DecisionOutput output,
            final String receivedAuditRef,
            final QdrModelGatewayIntegrationResult gatewayResult) {
        final List<String> reasons = new ArrayList<>(output.getReasonCodes());
        reasons.add("MODEL_GATEWAY_MOCK_CALL");
        final DecisionAction externalAction;
        if (output.getAction() == DecisionAction.ABSTAIN) {
            externalAction = DecisionAction.NO_TRADE;
            reasons.add("INTERNAL_ABSTAIN_MAPPED");
        } else {
            externalAction = output.getAction();
        }
        return new DecisionDryRunSnapshot(
                output.getRequestId(),
                true,
                externalAction.name(),
                confidenceFor(externalAction),
                output.getRiskLevel().name(),
                reasons,
                List.of(
                        "request:" + output.getRequestId(),
                        "trace:" + output.getTraceId(),
                        "policy:" + output.getPolicyStatus().name(),
                        "provider:" + output.getProviderStatus().name(),
                        gatewayResult.traceSummaryEntries().get(0),
                        gatewayResult.traceSummaryEntries().get(1),
                        gatewayResult.traceSummaryEntries().get(2),
                        gatewayResult.traceSummaryEntries().get(3),
                        gatewayResult.traceSummaryEntries().get(4),
                        gatewayResult.traceSummaryEntries().get(5),
                        gatewayResult.traceSummaryEntries().get(6),
                        gatewayResult.traceSummaryEntries().get(7)),
                "replay:" + output.getRequestId(),
                receivedAuditRef,
                output.getSchemaVersion());
    }

    private DecisionDryRunResult rejectWithoutThrowing(
            final DecisionDryRunCommand command,
            final int status,
            final DecisionDryRunErrorCode errorCode,
            final String message,
            final DecisionAuditEventType eventType) {
        try {
            final String auditRef =
                    writeAudit(
                            command,
                            eventType,
                            DecisionAuditEventStatus.FAILED,
                            errorCode.name(),
                            "DRY_RUN_REJECTED");
            return DecisionDryRunResult.rejected(
                    status, errorCode, message, requestId(command), traceId(command), auditRef);
        } catch (final RuntimeException auditFailure) {
            return DecisionDryRunResult.rejected(
                    500,
                    DecisionDryRunErrorCode.UNKNOWN_ERROR,
                    "dry-run audit write failed closed",
                    requestId(command),
                    traceId(command),
                    null);
        }
    }

    private String writeAudit(
            final DecisionDryRunCommand command,
            final DecisionAuditEventType eventType,
            final DecisionAuditEventStatus eventStatus,
            final String errorCode,
            final String marker) {
        final String requestId = requestId(command);
        final String auditId =
                requestId
                        + "-dryrun-audit-"
                        + marker.toLowerCase(Locale.ROOT)
                        + "-"
                        + UUID.randomUUID();
        auditRepository.saveAuditEvent(
                new DecisionPersistenceRecords.AuditEventRecord(
                        auditId,
                        requestId,
                        tenantId(command),
                        traceId(command),
                        eventType,
                        eventStatus,
                        Map.of(
                                "endpoint", ENDPOINT,
                                "marker", marker,
                                "requestId", requestId,
                                "source", source(command),
                                "schemaVersion", schemaVersion(command),
                                "dryRun", command != null && command.dryRun()),
                        errorCode,
                        clock.instant()));
        return "audit:" + auditId;
    }

    private static boolean requiredEnvelopePresent(final DecisionDryRunCommand command) {
        return !isBlank(command.requestId())
                && !isBlank(command.traceId())
                && !isBlank(command.tenantId())
                && !isBlank(command.source())
                && !isBlank(command.timestamp())
                && !isBlank(command.nonce())
                && !isBlank(command.schemaVersion());
    }

    private static boolean requiredContextPresent(final DecisionDryRunContext context) {
        return !isBlank(context.symbol())
                && !isBlank(context.market())
                && !isBlank(context.timeframe())
                && !isBlank(context.snapshotId());
    }

    private static boolean declaresMandatoryForbiddenCapabilities(final Set<String> capabilities) {
        final Set<String> normalized =
                capabilities.stream()
                        .filter(s -> s != null && !s.isBlank())
                        .map(s -> s.trim().toUpperCase(Locale.ROOT))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return ForbiddenAction.mandatorySet().stream().map(Enum::name).allMatch(normalized::contains);
    }

    private static BigDecimal confidenceFor(final DecisionAction action) {
        return switch (action) {
            case LONG_BIAS, SHORT_BIAS -> new BigDecimal("0.650000");
            case OBSERVE, NO_TRADE -> new BigDecimal("0.500000");
            case ABSTAIN -> BigDecimal.ZERO;
        };
    }

    private static BigDecimal confidenceFor(final QuantDecisionAction action) {
        return switch (action) {
            case LONG_BIAS, SHORT_BIAS -> new BigDecimal("0.6500");
            case OBSERVE, NO_TRADE -> new BigDecimal("0.5000");
            case NEEDS_REVIEW -> new BigDecimal("0.2500");
            case REJECTED -> BigDecimal.ZERO;
        };
    }

    private static QuantDecisionAction quantAction(final DecisionOutput output) {
        if (output.getProviderStatus() == ProviderSignalStatus.DISABLED
                || output.getProviderStatus() == ProviderSignalStatus.TIMEOUT
                || output.getProviderStatus() == ProviderSignalStatus.BUDGET_EXCEEDED
                || output.getPolicyStatus() == DecisionPolicyStatus.DENIED
                || output.getPolicyStatus() == DecisionPolicyStatus.INVALID
                || output.getPolicyStatus() == DecisionPolicyStatus.BLOCKED) {
            return QuantDecisionAction.REJECTED;
        }
        return QuantDecisionAction.fromDecisionAction(output.getAction());
    }

    private static String runErrorCode(final DecisionOutput output) {
        if (output.getProviderStatus() == ProviderSignalStatus.DISABLED
                || output.getProviderStatus() == ProviderSignalStatus.TIMEOUT
                || output.getProviderStatus() == ProviderSignalStatus.BUDGET_EXCEEDED) {
            return output.getProviderStatus().name();
        }
        if (output.getPolicyStatus() == DecisionPolicyStatus.DENIED
                || output.getPolicyStatus() == DecisionPolicyStatus.INVALID
                || output.getPolicyStatus() == DecisionPolicyStatus.BLOCKED) {
            return output.getPolicyStatus().name();
        }
        return null;
    }

    private static String rationale(
            final DecisionOutput output, final QdrModelGatewayIntegrationResult gatewayResult) {
        if (output.getReasonCodes().isEmpty()) {
            return gatewayResult == null
                    ? "QDR_READONLY_DECISION"
                    : "QDR_READONLY_DECISION," + gatewayResult.redactedSummary();
        }
        final List<String> reasons = new ArrayList<>(output.getReasonCodes());
        if (gatewayResult != null) {
            reasons.add(gatewayResult.redactedSummary());
        }
        return String.join(",", reasons);
    }

    private static Map<String, Object> constraints(
            final DecisionDryRunCommand command,
            final DecisionOutput output,
            final QdrModelGatewayIntegrationResult gatewayResult) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("readOnly", true);
        payload.put("dryRun", command.dryRun());
        payload.put("forbiddenCapabilities", List.copyOf(command.forbiddenCapabilities()));
        payload.put("forbiddenActions", List.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER"));
        payload.put("source", command.source());
        payload.put("policyStatus", output.getPolicyStatus().name());
        payload.put("providerStatus", output.getProviderStatus().name());
        if (gatewayResult != null) {
            payload.put("modelGateway", gatewayResult.toConstraintRefs());
        }
        return payload;
    }

    private static Map<String, Object> inputPayload(final DecisionDryRunCommand command) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("requestId", command.requestId());
        payload.put("traceId", command.traceId());
        payload.put("tenantId", command.tenantId());
        payload.put("source", command.source());
        payload.put("schemaVersion", command.schemaVersion());
        payload.put("dryRun", command.dryRun());
        payload.put("forbiddenCapabilities", List.copyOf(command.forbiddenCapabilities()));
        return payload;
    }

    private static Map<String, Object> contextPayload(final DecisionDryRunContext context) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        putIfPresent(payload, "symbol", context.symbol());
        putIfPresent(payload, "exchange", context.market());
        putIfPresent(payload, "timeframe", context.timeframe());
        putIfPresent(payload, "strategyId", context.strategyRef());
        putIfPresent(payload, "researchRef", context.researchRef());
        putIfPresent(payload, "contextRef", context.contextRef());
        putIfPresent(payload, "snapshotId", context.snapshotId());
        if (context.capturedAt() != null) {
            payload.put("capturedAt", context.capturedAt().toString());
        }
        payload.put("evidenceRefs", context.evidenceRefs());
        payload.put("signalType", signalType(context));
        return payload;
    }

    private static Map<String, Object> signalPayload(final DecisionDryRunContext context) {
        final Map<String, Object> payload = new LinkedHashMap<>(contextPayload(context));
        payload.put("payloadClass", "SANITIZED_REVIEW_INPUT");
        payload.put("approxBytes", context.approxBytes());
        return payload;
    }

    private static String signalType(final DecisionDryRunContext context) {
        final String contextRef = context.contextRef() == null ? "" : context.contextRef();
        final String researchRef = context.researchRef() == null ? "" : context.researchRef();
        final String joined = (contextRef + " " + researchRef).toLowerCase(Locale.ROOT);
        if (joined.contains("signal")) {
            return "QUANT_SIGNAL";
        }
        if (joined.contains("backtest")) {
            return "BACKTEST_REVIEW";
        }
        if (joined.contains("report")) {
            return "REPORT_REVIEW";
        }
        if (joined.contains("risk")) {
            return "RISK_EVENT_REVIEW";
        }
        return "UNKNOWN_REVIEW_INPUT";
    }

    private static String sourceRefId(final DecisionDryRunCommand command) {
        return command.context().snapshotId() == null ? command.requestId() : command.context().snapshotId();
    }

    private static UUID stableUuid(final String prefix, final String first, final String second) {
        return UUID.nameUUIDFromBytes(
                (prefix + "|" + first + "|" + second).getBytes(StandardCharsets.UTF_8));
    }

    private static void putIfPresent(
            final Map<String, Object> payload, final String key, final String value) {
        if (value != null && !value.isBlank()) {
            payload.put(key, value);
        }
    }

    private static DecisionAuditEventType eventTypeFor(final DecisionDryRunErrorCode errorCode) {
        if (errorCode == DecisionDryRunErrorCode.PROVIDER_DISABLED
                || errorCode == DecisionDryRunErrorCode.PROVIDER_TIMEOUT
                || errorCode == DecisionDryRunErrorCode.BUDGET_EXCEEDED) {
            return DecisionAuditEventType.PROVIDER_FAILED;
        }
        if (errorCode == DecisionDryRunErrorCode.POLICY_DENIED
                || errorCode == DecisionDryRunErrorCode.SOURCE_DENIED
                || errorCode == DecisionDryRunErrorCode.TENANT_MISMATCH) {
            return DecisionAuditEventType.POLICY_DENIED;
        }
        return DecisionAuditEventType.DECISION_FAILED;
    }

    private static DecisionAuditEventType eventTypeForGatewayFailure(
            final ModelGatewayFailureCode failureCode) {
        if (failureCode == ModelGatewayFailureCode.BUDGET_EXCEEDED
                || failureCode == ModelGatewayFailureCode.PROVIDER_TIMEOUT
                || failureCode == ModelGatewayFailureCode.PROVIDER_UNAVAILABLE
                || failureCode == ModelGatewayFailureCode.PROVIDER_DISABLED
                || failureCode == ModelGatewayFailureCode.PROVIDER_OUTPUT_INVALID) {
            return DecisionAuditEventType.PROVIDER_FAILED;
        }
        if (failureCode == ModelGatewayFailureCode.POLICY_DENIED
                || failureCode == ModelGatewayFailureCode.PROMPT_DENIED
                || failureCode == ModelGatewayFailureCode.REDACTION_FAILED
                || failureCode == ModelGatewayFailureCode.REAL_PROVIDER_FORBIDDEN
                || failureCode == ModelGatewayFailureCode.UNKNOWN_PROVIDER
                || failureCode == ModelGatewayFailureCode.REGISTRY_MISMATCH
                || failureCode == ModelGatewayFailureCode.PROMPT_VERSION_NOT_FOUND
                || failureCode == ModelGatewayFailureCode.MODEL_VERSION_NOT_FOUND
                || failureCode == ModelGatewayFailureCode.MISSING_REQUIRED_FIELD) {
            return DecisionAuditEventType.POLICY_DENIED;
        }
        return DecisionAuditEventType.DECISION_FAILED;
    }

    private static int statusForGatewayFailure(final ModelGatewayFailureCode failureCode) {
        return switch (failureCode) {
            case BUDGET_EXCEEDED -> 429;
            case PROVIDER_TIMEOUT -> 504;
            case PROVIDER_UNAVAILABLE, PROVIDER_DISABLED, UNKNOWN_PROVIDER, REAL_PROVIDER_FORBIDDEN -> 503;
            case PROVIDER_OUTPUT_INVALID, UNKNOWN_ERROR -> 500;
            default -> 403;
        };
    }

    private static DecisionDryRunErrorCode errorCodeForGatewayFailure(
            final ModelGatewayFailureCode failureCode) {
        return switch (failureCode) {
            case BUDGET_EXCEEDED -> DecisionDryRunErrorCode.BUDGET_EXCEEDED;
            case PROVIDER_TIMEOUT -> DecisionDryRunErrorCode.PROVIDER_TIMEOUT;
            case PROVIDER_UNAVAILABLE, PROVIDER_DISABLED, UNKNOWN_PROVIDER, REAL_PROVIDER_FORBIDDEN ->
                    DecisionDryRunErrorCode.PROVIDER_DISABLED;
            case PROVIDER_OUTPUT_INVALID, UNKNOWN_ERROR -> DecisionDryRunErrorCode.UNKNOWN_ERROR;
            default -> DecisionDryRunErrorCode.POLICY_DENIED;
        };
    }

    private static String requestId(final DecisionDryRunCommand command) {
        return isBlank(command == null ? null : command.requestId())
                ? "unknown-request"
                : command.requestId();
    }

    private static String traceId(final DecisionDryRunCommand command) {
        return isBlank(command == null ? null : command.traceId())
                ? "unknown-trace"
                : command.traceId();
    }

    private static String tenantId(final DecisionDryRunCommand command) {
        return isBlank(command == null ? null : command.tenantId())
                ? "unknown-tenant"
                : command.tenantId();
    }

    private static String source(final DecisionDryRunCommand command) {
        return isBlank(command == null ? null : command.source())
                ? "unknown-source"
                : command.source();
    }

    private static String schemaVersion(final DecisionDryRunCommand command) {
        return isBlank(command == null ? null : command.schemaVersion())
                ? "unknown-schema"
                : command.schemaVersion();
    }

    private static boolean isBlank(final String value) {
        return value == null || value.isBlank();
    }

    private record DecisionCoreSession(
            UUID requestId, UUID runId, UUID signalId, Instant startedAt) {
    }
}
