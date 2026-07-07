package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * 保存 ModelGatewayCall 脱敏 metadata 的命令。
 *
 * <p>命令只携带 call ref、hash、budget/usage、redacted summary 与结构化状态；不携带 prompt 正文或
 * provider 原始响应。
 *
 * @param modelGatewayCallId        gateway call id。
 * @param tenantId                  tenant 边界。
 * @param traceId                   traceId。
 * @param requestId                 requestId。
 * @param decisionRunId             decision run id。
 * @param promptVersionId           prompt version id。
 * @param modelVersionId            model version id。
 * @param providerProfileId         provider profile identity ref。
 * @param providerKind              provider 类型。
 * @param providerIdentityRef       provider identity 安全引用。
 * @param status                    call 状态。
 * @param failureCode               失败 code；成功时必须为空。
 * @param trustDecision             ProviderTrustPolicy 判定摘要。
 * @param providerTrustDecisionRef  ProviderTrustPolicy 安全引用。
 * @param modelCallRef              model call ref。
 * @param budgetSummary             脱敏预算摘要。
 * @param inputCharacters           input 字符数。
 * @param renderedPromptCharacters  rendered prompt 字符数。
 * @param outputCharacters          output 字符数。
 * @param estimatedTokens           本地估算 token。
 * @param memoryEntries             memory entry 数量。
 * @param redactedInputSummary      脱敏输入摘要。
 * @param redactedOutputSummary     脱敏输出摘要。
 * @param inputHash                 input SHA-256 hash。
 * @param outputHash                output SHA-256 hash。
 * @param auditRef                  audit 安全引用。
 * @param traceRef                  trace 安全引用。
 * @param createdAt                 创建时间。
 */
public record SaveModelGatewayCallCommand(
        UUID modelGatewayCallId,
        String tenantId,
        String traceId,
        String requestId,
        UUID decisionRunId,
        UUID promptVersionId,
        UUID modelVersionId,
        UUID providerProfileId,
        ProviderKind providerKind,
        String providerIdentityRef,
        ModelGatewayCallStatus status,
        ModelGatewayFailureCode failureCode,
        ModelGatewayCallTrustDecision trustDecision,
        String providerTrustDecisionRef,
        String modelCallRef,
        String budgetSummary,
        int inputCharacters,
        int renderedPromptCharacters,
        int outputCharacters,
        int estimatedTokens,
        int memoryEntries,
        String redactedInputSummary,
        String redactedOutputSummary,
        String inputHash,
        String outputHash,
        String auditRef,
        String traceRef,
        Instant createdAt) {

    /**
     * 校验保存命令，不允许 raw material 或真实 provider credential 进入持久化 contract。
     */
    public SaveModelGatewayCallCommand {
        modelGatewayCallId = QdrPersistenceSafety.requireUuid(modelGatewayCallId, "modelGatewayCallId");
        tenantId = QdrPersistenceSafety.requireText(tenantId, "tenantId");
        traceId = QdrPersistenceSafety.requireSafeText(traceId, "traceId");
        requestId = QdrPersistenceSafety.requireSafeText(requestId, "requestId");
        decisionRunId = QdrPersistenceSafety.requireUuid(decisionRunId, "decisionRunId");
        promptVersionId = QdrPersistenceSafety.requireUuid(promptVersionId, "promptVersionId");
        modelVersionId = QdrPersistenceSafety.requireUuid(modelVersionId, "modelVersionId");
        providerProfileId = QdrPersistenceSafety.requireUuid(providerProfileId, "providerProfileId");
        providerKind = Objects.requireNonNull(providerKind, "providerKind");
        providerIdentityRef = QdrPersistenceSafety.requireSafeText(providerIdentityRef, "providerIdentityRef");
        status = Objects.requireNonNull(status, "status");
        if (status == ModelGatewayCallStatus.SUCCEEDED && failureCode != null) {
            throw new IllegalArgumentException("successful model gateway call must not have failureCode");
        }
        if (status == ModelGatewayCallStatus.FAILED && failureCode == null) {
            throw new IllegalArgumentException("failed model gateway call must have failureCode");
        }
        trustDecision = Objects.requireNonNull(trustDecision, "trustDecision");
        providerTrustDecisionRef =
                QdrPersistenceSafety.requireSafeText(providerTrustDecisionRef, "providerTrustDecisionRef");
        modelCallRef = QdrPersistenceSafety.requireSafeText(modelCallRef, "modelCallRef");
        budgetSummary = QdrPersistenceSafety.requireSafeText(budgetSummary, "budgetSummary");
        inputCharacters = QdrPersistenceSafety.requireNonNegative(inputCharacters, "inputCharacters");
        renderedPromptCharacters =
                QdrPersistenceSafety.requireNonNegative(
                        renderedPromptCharacters, "renderedPromptCharacters");
        outputCharacters = QdrPersistenceSafety.requireNonNegative(outputCharacters, "outputCharacters");
        estimatedTokens = QdrPersistenceSafety.requireNonNegative(estimatedTokens, "estimatedTokens");
        memoryEntries = QdrPersistenceSafety.requireNonNegative(memoryEntries, "memoryEntries");
        redactedInputSummary =
                QdrPersistenceSafety.requireSafeText(redactedInputSummary, "redactedInputSummary");
        redactedOutputSummary =
                QdrPersistenceSafety.optionalSafeText(redactedOutputSummary, "redactedOutputSummary");
        inputHash = QdrPersistenceSafety.requireSha256Hex(inputHash, "inputHash");
        outputHash = QdrPersistenceSafety.optionalSha256Hex(outputHash, "outputHash");
        auditRef = QdrPersistenceSafety.optionalSafeText(auditRef, "auditRef");
        traceRef = QdrPersistenceSafety.optionalSafeText(traceRef, "traceRef");
        createdAt = QdrPersistenceSafety.requireInstant(createdAt, "createdAt");
    }
}
