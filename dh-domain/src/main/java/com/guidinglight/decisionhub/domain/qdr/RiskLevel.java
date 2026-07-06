package com.guidinglight.decisionhub.domain.qdr;

import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;

/**
 * Quant Decision Review 风险级别。
 *
 * <p>未知风险必须按 `UNKNOWN` 处理；`HIGH` 与 `BLOCKED` 不得驱动方向性交易动作。
 */
public enum RiskLevel {
    /**
     * 低风险，只表示审查信息，不授权交易。
     */
    LOW,

    /**
     * 中等风险，仍只读记录。
     */
    MEDIUM,

    /**
     * 高风险，后续阶段应进入人工审查或拒绝。
     */
    HIGH,

    /**
     * 安全或策略已阻断。
     */
    BLOCKED,

    /**
     * 风险不可得，调用方必须 fail-closed。
     */
    UNKNOWN;

    /**
     * 从既有 Stage4 risk level 转换为 QDR risk level。
     *
     * @param riskLevel 既有 risk level。
     * @return QDR 风险级别。
     */
    public static RiskLevel fromDecisionRiskLevel(final DecisionRiskLevel riskLevel) {
        if (riskLevel == null) {
            return UNKNOWN;
        }
        return switch (riskLevel) {
            case LOW -> LOW;
            case MEDIUM -> MEDIUM;
            case HIGH -> HIGH;
            case BLOCKED -> BLOCKED;
            case UNKNOWN -> UNKNOWN;
        };
    }
}
