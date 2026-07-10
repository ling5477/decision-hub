package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;

import java.util.Objects;

/**
 * Decision evidence 的 tenant-bound 主关联键。
 *
 * <p>traceId 或 requestId 都不能替代 decisionId。该对象只冻结关联语义；不会读取已有 replay，
 * 也不会执行 deterministic replay。
 *
 * @param tenantId   租户边界。
 * @param traceId    可审计链路 ID。
 * @param requestId  外部请求 ID。
 * @param decisionId 决策 ID。
 */
public record DecisionEvidenceCorrelation(
        String tenantId, String traceId, String requestId, String decisionId) {

    /** 对四个主关联键执行 trim 后的必填和安全校验。 */
    public DecisionEvidenceCorrelation {
        tenantId = DecisionEvidencePolicy.requireSafeText(tenantId, "tenantId");
        traceId = DecisionEvidencePolicy.requireSafeText(traceId, "traceId");
        requestId = DecisionEvidencePolicy.requireSafeText(requestId, "requestId");
        decisionId = DecisionEvidencePolicy.requireSafeText(decisionId, "decisionId");
    }

    /**
     * 将现有只读 replay view 转成 B1 关联键。
     *
     * <p>缺失 traceId 或 requestId 时会直接拒绝，避免把已有 replay 的可选字段降级为不完整关联。
     *
     * @param replayView 已有的只读 replay view。
     * @return 严格的 B1 correlation。
     */
    public static DecisionEvidenceCorrelation fromReplayView(final DecisionReplayView replayView) {
        final DecisionReplayView checked = Objects.requireNonNull(replayView, "replayView");
        return new DecisionEvidenceCorrelation(
                checked.tenantId(), checked.traceId(), checked.requestId(), checked.decisionId());
    }

    /**
     * 判断另一个关联键是否与当前四个主键完全一致。
     *
     * @param other 待比较关联键。
     * @return 四个键完全一致时返回 true；null 一律视为不匹配。
     */
    public boolean matches(final DecisionEvidenceCorrelation other) {
        return other != null
                && tenantId.equals(other.tenantId)
                && traceId.equals(other.traceId)
                && requestId.equals(other.requestId)
                && decisionId.equals(other.decisionId);
    }
}
