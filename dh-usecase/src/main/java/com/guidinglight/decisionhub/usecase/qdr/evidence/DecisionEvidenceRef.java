package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;

import java.util.Objects;

/**
 * Stage-QDR-6 aggregate 的 typed safe-ref adapter。
 *
 * <p>该对象复用已有 {@link DecisionEvidence}，同时增加 tenant-bound correlation、hash、来源类型和
 * redaction 状态。它不保存原始 payload，也不替代 {@code DecisionEvidenceView} 或 replay model。
 *
 * @param evidence       既有的结构化 evidence。
 * @param correlation    evidence 所属的四键 correlation。
 * @param evidenceType   evidence 类型。
 * @param refId          安全引用 ID，必须与 {@code evidence.evidenceId()} 一致。
 * @param contentHash    可选 lowercase SHA-256 内容 hash。
 * @param sourceType     来源类型的安全标识。
 * @param mandatory      当前 source 将其声明为强制证据的意图；policy 不接受降级 required type。
 * @param redactionStatus 只读脱敏状态，不能是 {@link RedactionStatus#NOT_APPLICABLE}。
 */
public record DecisionEvidenceRef(
        DecisionEvidence evidence,
        DecisionEvidenceCorrelation correlation,
        DecisionEvidencePolicy.EvidenceType evidenceType,
        String refId,
        String contentHash,
        String sourceType,
        boolean mandatory,
        RedactionStatus redactionStatus) {

    /** 校验复用 evidence 与 safe ref 元数据，拒绝原始材料、凭证和交易指令。 */
    public DecisionEvidenceRef {
        evidence = Objects.requireNonNull(evidence, "evidence");
        correlation = Objects.requireNonNull(correlation, "correlation");
        evidenceType = Objects.requireNonNull(evidenceType, "evidenceType");
        refId = DecisionEvidencePolicy.requireSafeText(refId, "refId");
        contentHash = DecisionEvidencePolicy.optionalSha256Hex(contentHash, "contentHash");
        sourceType = DecisionEvidencePolicy.requireSafeText(sourceType, "sourceType");
        redactionStatus = Objects.requireNonNull(redactionStatus, "redactionStatus");

        final String safeEvidenceId =
                DecisionEvidencePolicy.requireSafeText(evidence.evidenceId(), "evidence.evidenceId");
        final String safeEvidenceType =
                DecisionEvidencePolicy.requireSafeText(evidence.evidenceType(), "evidence.evidenceType");
        final String safeSummary =
                DecisionEvidencePolicy.requireSafeText(evidence.summary(), "evidence.summary");
        if (!safeEvidenceId.equals(refId)) {
            throw new IllegalArgumentException("refId must match evidence.evidenceId");
        }
        if (!safeEvidenceType.equals(evidenceType.name())) {
            throw new IllegalArgumentException("evidenceType must match evidence.evidenceType");
        }
        if (redactionStatus == RedactionStatus.NOT_APPLICABLE) {
            throw new IllegalArgumentException("redactionStatus must declare a safe read boundary");
        }
        evidence = new DecisionEvidence(safeEvidenceId, safeEvidenceType, safeSummary);
    }

    /**
     * 返回用于重复冲突检测的稳定 ref identity。
     *
     * @return evidence type 与 ref ID 组合；不同 hash、source 或 correlation 的同 identity 视为冲突。
     */
    public String identity() {
        return evidenceType + ":" + refId;
    }

    /**
     * 将既有 {@link DecisionEvidenceView} 的已脱敏边界适配为单个 B1 safe ref。
     *
     * <p>该方法只校验传入 read model 的 tenant/trace 与主关联键一致，并复用它的 redaction 状态；不读取
     * read model port、不解析 {@code evidenceRefsJson}、不保存任何原始内容。
     *
     * @param evidenceView 已有的只读 evidence view。
     * @param correlation  本 aggregate 的严格主关联键。
     * @param evidenceType B1 evidence 类型。
     * @param refId        安全引用 ID。
     * @param contentHash  可选 SHA-256 hash。
     * @param sourceType   安全来源标识。
     * @param mandatory    当前 source 的 mandatory 声明。
     * @return 与既有 read model 脱敏状态一致的 typed safe ref。
     */
    public static DecisionEvidenceRef fromReadModel(
            final DecisionEvidenceView evidenceView,
            final DecisionEvidenceCorrelation correlation,
            final DecisionEvidencePolicy.EvidenceType evidenceType,
            final String refId,
            final String contentHash,
            final String sourceType,
            final boolean mandatory) {
        final DecisionEvidenceView checkedView = Objects.requireNonNull(evidenceView, "evidenceView");
        final DecisionEvidenceCorrelation checkedCorrelation =
                Objects.requireNonNull(correlation, "correlation");
        if (!checkedCorrelation.tenantId().equals(checkedView.tenantId())
                || !checkedCorrelation.traceId().equals(checkedView.traceId())) {
            throw new IllegalArgumentException("DecisionEvidenceView tenant or trace does not match correlation");
        }
        final String safeRefId = DecisionEvidencePolicy.requireSafeText(refId, "refId");
        final DecisionEvidencePolicy.EvidenceType checkedType =
                Objects.requireNonNull(evidenceType, "evidenceType");
        return new DecisionEvidenceRef(
                new DecisionEvidence(safeRefId, checkedType.name(), sourceType),
                checkedCorrelation,
                checkedType,
                safeRefId,
                contentHash,
                sourceType,
                mandatory,
                checkedView.redactionStatus());
    }
}
