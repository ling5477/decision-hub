package com.guidinglight.decisionhub.domain.qdr;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Quant Decision Review 的只读审查结果。
 *
 * <p>该对象对应 `quant_decision` 表。`LONG_BIAS` / `SHORT_BIAS` 只表示方向性审查意见，不得映射为
 * `BUY` / `SELL`，也不得触发订单、风控、账本、Paper 或 LIVE mutation。
 */
public record QuantDecision(
        UUID id,
        UUID quantSignalId,
        UUID decisionRunId,
        QuantDecisionAction action,
        BigDecimal confidenceScore,
        RiskLevel riskLevel,
        String rationale,
        Map<String, Object> constraintsJson,
        HumanApprovalStatus humanApprovalStatus,
        Instant createdAt) {

    /**
     * 校验只读 decision 的 action、risk、confidence 与审批预留状态。
     */
    public QuantDecision {
        id = Objects.requireNonNull(id, "id");
        decisionRunId = Objects.requireNonNull(decisionRunId, "decisionRunId");
        action = Objects.requireNonNull(action, "action");
        confidenceScore = normalizeConfidence(confidenceScore);
        riskLevel = Objects.requireNonNull(riskLevel, "riskLevel");
        constraintsJson = constraintsJson == null ? null : Map.copyOf(constraintsJson);
        humanApprovalStatus =
                humanApprovalStatus == null ? HumanApprovalStatus.NOT_REQUIRED : humanApprovalStatus;
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    private static BigDecimal normalizeConfidence(final BigDecimal confidenceScore) {
        if (confidenceScore == null) {
            return null;
        }
        if (confidenceScore.compareTo(BigDecimal.ZERO) < 0
                || confidenceScore.compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("confidenceScore must be between 0 and 1");
        }
        return confidenceScore;
    }
}
