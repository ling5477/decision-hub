package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.decision.DecisionTraceStepName;
import com.guidinglight.decisionhub.usecase.decision.DecisionTraceStepStatus;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * B4 QDR decision pipeline mock ModelGateway integration。
 *
 * <p>该 service 负责把现有 dry-run / QDR pipeline 接到 B2 ModelGatewayPort，并把 B3
 * model_gateway_call metadata、V5 trace step 和 audit event 串起来。任何 gateway、persistence、trace 或
 * audit 失败都会 fail-closed；gateway result 只作为 reasoning / evidence summary，不改变 approval
 * status，不触发 NQ、交易或 replay execution。
 */
public final class DefaultQdrModelGatewayIntegrationService implements QdrModelGatewayIntegrationPort {

    private static final String STEP_MARKER = "MODEL_GATEWAY_MOCK_CALL";

    private final QdrModelGatewayBaselinePort baselinePort;
    private final ModelGatewayPort modelGatewayPort;
    private final ModelGatewayCallPersistencePort modelGatewayCallPersistencePort;
    private final DecisionAuditRepository auditRepository;
    private final Clock clock;

    /**
     * 创建 B4 gateway integration service。
     *
     * @param baselinePort                       mock baseline bootstrap。
     * @param modelGatewayPort                   B2 gateway port。
     * @param modelGatewayCallPersistencePort    B3 gateway call persistence port。
     * @param auditRepository                    V5 audit / trace repository。
     * @param clock                              时间源。
     */
    public DefaultQdrModelGatewayIntegrationService(
            final QdrModelGatewayBaselinePort baselinePort,
            final ModelGatewayPort modelGatewayPort,
            final ModelGatewayCallPersistencePort modelGatewayCallPersistencePort,
            final DecisionAuditRepository auditRepository,
            final Clock clock) {
        this.baselinePort = Objects.requireNonNull(baselinePort, "baselinePort");
        this.modelGatewayPort = Objects.requireNonNull(modelGatewayPort, "modelGatewayPort");
        this.modelGatewayCallPersistencePort =
                Objects.requireNonNull(
                        modelGatewayCallPersistencePort, "modelGatewayCallPersistencePort");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Override
    public QdrModelGatewayIntegrationResult invoke(
            final QdrModelGatewayIntegrationCommand command) {
        final QdrModelGatewayIntegrationCommand checked = Objects.requireNonNull(command, "command");
        final QdrModelGatewayBaseline baseline = prepareBaseline(checked);
        final ModelGatewayRequest request = toGatewayRequest(checked, baseline);
        final ModelGatewayResult gatewayResult;
        try {
            gatewayResult = modelGatewayPort.call(request);
        } catch (final RuntimeException error) {
            writeFailureTraceAndAuditSafely(checked, baseline, ModelGatewayFailureCode.UNKNOWN_ERROR);
            throw new QdrModelGatewayIntegrationException(
                    ModelGatewayFailureCode.UNKNOWN_ERROR, "model gateway failed closed");
        }
        if (gatewayResult == null) {
            writeFailureTraceAndAuditSafely(
                    checked, baseline, ModelGatewayFailureCode.PROVIDER_OUTPUT_INVALID);
            throw new QdrModelGatewayIntegrationException(
                    ModelGatewayFailureCode.PROVIDER_OUTPUT_INVALID, "model gateway failed closed");
        }
        if (!gatewayResult.success()) {
            persistAndWriteFailure(checked, baseline, gatewayResult);
            throw new QdrModelGatewayIntegrationException(
                    gatewayResult.failure().code(), "model gateway failed closed");
        }
        try {
            final String traceRef = persistGatewayCall(checked, baseline, gatewayResult);
            final QdrModelGatewayIntegrationResult result =
                    new QdrModelGatewayIntegrationResult(
                            gatewayResult.promptVersionId(),
                            gatewayResult.modelVersionId(),
                            gatewayResult.providerProfileId(),
                            gatewayResult.modelCallRef(),
                            ModelGatewayCallTrustDecision.ALLOWED.name(),
                            "PASSED",
                            budgetSummary(gatewayResult.usage()),
                            gatewayResult.redactedSummary(),
                            traceRef,
                            gatewayResult.auditRef());
            writeTraceAndAudit(checked, baseline, gatewayResult, result);
            return result;
        } catch (final RuntimeException error) {
            throw new QdrModelGatewayIntegrationException(
                    ModelGatewayFailureCode.UNKNOWN_ERROR,
                    "model gateway metadata write failed closed");
        }
    }

    private QdrModelGatewayBaseline prepareBaseline(
            final QdrModelGatewayIntegrationCommand command) {
        try {
            return baselinePort.prepare(
                    new QdrModelGatewayBaselineCommand(
                            command.tenantId(),
                            command.traceId(),
                            command.requestId(),
                            command.decisionRunId()));
        } catch (final RuntimeException error) {
            writeBaselineFailureSafely(command, ModelGatewayFailureCode.UNKNOWN_ERROR);
            throw new QdrModelGatewayIntegrationException(
                    ModelGatewayFailureCode.UNKNOWN_ERROR, "model gateway baseline failed closed");
        }
    }

    private void persistAndWriteFailure(
            final QdrModelGatewayIntegrationCommand command,
            final QdrModelGatewayBaseline baseline,
            final ModelGatewayResult gatewayResult) {
        try {
            persistGatewayCall(command, baseline, gatewayResult);
            writeTraceAndAudit(command, baseline, gatewayResult, null);
        } catch (final RuntimeException error) {
            writeFailureTraceAndAuditSafely(command, baseline, ModelGatewayFailureCode.UNKNOWN_ERROR);
            throw new QdrModelGatewayIntegrationException(
                    ModelGatewayFailureCode.UNKNOWN_ERROR,
                    "model gateway failure metadata write failed closed");
        }
    }

    private ModelGatewayRequest toGatewayRequest(
            final QdrModelGatewayIntegrationCommand command,
            final QdrModelGatewayBaseline baseline) {
        return new ModelGatewayRequest(
                new ModelCallContext(
                        command.tenantId(),
                        command.traceId(),
                        command.requestId(),
                        command.decisionRunId().toString(),
                        baseline.promptVersionId().toString(),
                        baseline.modelVersionId().toString(),
                        baseline.providerProfileId().toString()),
                baseline.promptTemplateId().toString(),
                baseline.promptVersion(),
                baseline.promptVersionChecksum(),
                baseline.modelVersionChecksum(),
                Map.of(
                        "symbol", command.symbol(),
                        "market", command.market(),
                        "timeframe", command.timeframe(),
                        "risk", command.riskLevel()),
                command.evidenceRefs().stream()
                        .map(ref -> "evidence-ref:" + safeHash(ref))
                        .toList(),
                baseline.policy(),
                baseline.budget(),
                baseline.redactionPolicy());
    }

    private String persistGatewayCall(
            final QdrModelGatewayIntegrationCommand command,
            final QdrModelGatewayBaseline baseline,
            final ModelGatewayResult result) {
        final boolean success = result.success();
        final ModelGatewayFailureCode failureCode =
                success ? null : result.failure().code();
        final String modelCallRef =
                success ? result.modelCallRef() : failedModelCallRef(command, failureCode);
        final String traceRef = "trace:" + modelCallRef;
        final ModelGatewayUsage usage =
                success ? result.usage() : new ModelGatewayUsage(0, 0, 0, 0, 0);
        modelGatewayCallPersistencePort.save(
                new SaveModelGatewayCallCommand(
                        stableUuid(command.tenantId(), command.requestId(), modelCallRef),
                        command.tenantId(),
                        command.traceId(),
                        command.requestId(),
                        command.decisionRunId(),
                        baseline.promptVersionId(),
                        baseline.modelVersionId(),
                        baseline.providerProfileId(),
                        baseline.providerKind(),
                        baseline.providerIdentityRef(),
                        success ? ModelGatewayCallStatus.SUCCEEDED : ModelGatewayCallStatus.FAILED,
                        failureCode,
                        success
                                ? ModelGatewayCallTrustDecision.ALLOWED
                                : ModelGatewayCallTrustDecision.DENIED,
                        success
                                ? result.providerTrustDecisionRef()
                                : "provider-trust-denied:" + failureCode.name(),
                        modelCallRef,
                        budgetSummary(usage),
                        usage.inputCharacters(),
                        usage.renderedPromptCharacters(),
                        usage.outputCharacters(),
                        usage.estimatedTokens(),
                        usage.memoryEntries(),
                        "qdr dry-run gateway input summary",
                        success ? result.redactedSummary() : null,
                        PromptModelSafetyRules.sha256Hex(
                                List.of(
                                        command.tenantId(),
                                        command.requestId(),
                                        command.decisionRunId().toString(),
                                        "gateway-input")),
                        success
                                ? PromptModelSafetyRules.sha256Hex(
                                        List.of(result.redactedSummary(), modelCallRef))
                                : null,
                        success ? result.auditRef() : null,
                        traceRef,
                        clock.instant()));
        return traceRef;
    }

    private void writeTraceAndAudit(
            final QdrModelGatewayIntegrationCommand command,
            final QdrModelGatewayBaseline baseline,
            final ModelGatewayResult gatewayResult,
            final QdrModelGatewayIntegrationResult result) {
        final boolean success = gatewayResult.success();
        final ModelGatewayFailureCode failureCode =
                success ? null : gatewayResult.failure().code();
        final Instant now = clock.instant();
        final String traceSummary =
                success
                        ? traceSummary(result)
                        : traceSummary(baseline, gatewayResult, failedModelCallRef(command, failureCode));
        auditRepository.saveTraceStep(
                new DecisionPersistenceRecords.TraceStepRecord(
                        command.requestId() + "-model-gateway-" + UUID.randomUUID(),
                        command.requestId(),
                        command.tenantId(),
                        command.traceId(),
                        DecisionTraceStepName.MODEL_GATEWAY_MOCK_CALL,
                        success ? DecisionTraceStepStatus.COMPLETED : DecisionTraceStepStatus.FAILED,
                        now,
                        now,
                        failureCode == null ? null : failureCode.name(),
                        traceSummary,
                        now));
        auditRepository.saveAuditEvent(
                new DecisionPersistenceRecords.AuditEventRecord(
                        command.requestId() + "-model-gateway-audit-" + UUID.randomUUID(),
                        command.requestId(),
                        command.tenantId(),
                        command.traceId(),
                        success
                                ? DecisionAuditEventType.DECISION_COMPLETED
                                : eventTypeFor(failureCode),
                        success ? DecisionAuditEventStatus.SUCCESS : DecisionAuditEventStatus.FAILED,
                        auditPayload(command, baseline, gatewayResult, result),
                        failureCode == null ? null : failureCode.name(),
                        now));
    }

    private void writeBaselineFailure(
            final QdrModelGatewayIntegrationCommand command,
            final ModelGatewayFailureCode failureCode) {
        final Instant now = clock.instant();
        auditRepository.saveTraceStep(
                new DecisionPersistenceRecords.TraceStepRecord(
                        command.requestId() + "-model-gateway-baseline-" + UUID.randomUUID(),
                        command.requestId(),
                        command.tenantId(),
                        command.traceId(),
                        DecisionTraceStepName.MODEL_GATEWAY_MOCK_CALL,
                        DecisionTraceStepStatus.FAILED,
                        now,
                        now,
                        failureCode.name(),
                        "gatewayFailure=" + failureCode.name(),
                        now));
        auditRepository.saveAuditEvent(
                new DecisionPersistenceRecords.AuditEventRecord(
                        command.requestId() + "-model-gateway-baseline-audit-" + UUID.randomUUID(),
                        command.requestId(),
                        command.tenantId(),
                        command.traceId(),
                        eventTypeFor(failureCode),
                        DecisionAuditEventStatus.FAILED,
                        Map.of(
                                "marker", STEP_MARKER,
                                "requestId", command.requestId(),
                                "decisionRunId", command.decisionRunId().toString(),
                                "failureCode", failureCode.name(),
                                "redactionStatus", "NOT_APPLICABLE"),
                        failureCode.name(),
                        now));
    }

    private void writeBaselineFailureSafely(
            final QdrModelGatewayIntegrationCommand command,
            final ModelGatewayFailureCode failureCode) {
        try {
            writeBaselineFailure(command, failureCode);
        } catch (final RuntimeException ignored) {
            // 审计自身失败时仍必须 fail-closed；异常内容不能向 response 侧扩散。
        }
    }

    private void writeFailureTraceAndAuditSafely(
            final QdrModelGatewayIntegrationCommand command,
            final QdrModelGatewayBaseline baseline,
            final ModelGatewayFailureCode failureCode) {
        try {
            writeTraceAndAudit(
                    command,
                    baseline,
                    ModelGatewayResult.failure(failureCode, toGatewayRequest(command, baseline)),
                    null);
        } catch (final RuntimeException ignored) {
            // trace/audit 写失败本身就是 fail-closed 条件；这里仅避免覆盖固定错误码。
        }
    }

    private static Map<String, Object> auditPayload(
            final QdrModelGatewayIntegrationCommand command,
            final QdrModelGatewayBaseline baseline,
            final ModelGatewayResult gatewayResult,
            final QdrModelGatewayIntegrationResult result) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("marker", STEP_MARKER);
        payload.put("requestId", command.requestId());
        payload.put("decisionRunId", command.decisionRunId().toString());
        payload.put("promptVersionId", baseline.promptVersionId().toString());
        payload.put("modelVersionId", baseline.modelVersionId().toString());
        payload.put("providerProfileId", baseline.providerProfileId().toString());
        payload.put(
                "gatewayCallRef",
                gatewayResult.success()
                        ? gatewayResult.modelCallRef()
                        : "failed:" + gatewayResult.failure().code().name());
        payload.put(
                "trustDecision",
                gatewayResult.success()
                        ? ModelGatewayCallTrustDecision.ALLOWED.name()
                        : ModelGatewayCallTrustDecision.DENIED.name());
        payload.put("redactionStatus", gatewayResult.success() ? "PASSED" : "FAILED");
        payload.put(
                "budgetSummary",
                result == null ? "input=0,rendered=0,output=0,estimated=0,memory=0" : result.budgetSummary());
        if (!gatewayResult.success()) {
            payload.put("failureCode", gatewayResult.failure().code().name());
        } else {
            payload.put("redactedSummary", gatewayResult.redactedSummary());
        }
        return payload;
    }

    private static String traceSummary(final QdrModelGatewayIntegrationResult result) {
        return "prompt="
                + shortRef(result.promptVersionId())
                + ";model="
                + shortRef(result.modelVersionId())
                + ";provider="
                + shortRef(result.providerProfileId())
                + ";call="
                + result.gatewayCallRef()
                + ";trust="
                + result.trustDecision()
                + ";redaction="
                + result.redactionStatus()
                + ";budget="
                + result.budgetSummary();
    }

    private static String traceSummary(
            final QdrModelGatewayBaseline baseline,
            final ModelGatewayResult gatewayResult,
            final String failedModelCallRef) {
        return "prompt="
                + shortRef(baseline.promptVersionId().toString())
                + ";model="
                + shortRef(baseline.modelVersionId().toString())
                + ";provider="
                + shortRef(baseline.providerProfileId().toString())
                + ";call="
                + failedModelCallRef
                + ";failure="
                + gatewayResult.failure().code().name();
    }

    private static DecisionAuditEventType eventTypeFor(final ModelGatewayFailureCode failureCode) {
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

    private static String budgetSummary(final ModelGatewayUsage usage) {
        return "input="
                + usage.inputCharacters()
                + ",rendered="
                + usage.renderedPromptCharacters()
                + ",output="
                + usage.outputCharacters()
                + ",estimated="
                + usage.estimatedTokens()
                + ",memory="
                + usage.memoryEntries();
    }

    private static String failedModelCallRef(
            final QdrModelGatewayIntegrationCommand command,
            final ModelGatewayFailureCode failureCode) {
        return "model-call-failed:"
                + PromptModelSafetyRules.sha256Hex(
                                List.of(
                                        command.tenantId(),
                                        command.traceId(),
                                        command.requestId(),
                                        command.decisionRunId().toString(),
                                        failureCode.name()))
                        .substring(0, 16);
    }

    private static UUID stableUuid(final String tenantId, final String requestId, final String modelCallRef) {
        return UUID.nameUUIDFromBytes(
                ("qdr-b4-call|" + tenantId + "|" + requestId + "|" + modelCallRef)
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private static String safeHash(final String value) {
        return PromptModelSafetyRules.sha256Hex(List.of(value == null ? "" : value)).substring(0, 16);
    }

    private static String shortRef(final String value) {
        return value == null || value.length() <= 12 ? value : value.substring(0, 12);
    }
}
