package com.guidinglight.decisionhub.domain.qdr.replay;

/**
 * QDR regression finding 严重度。
 *
 * <p>严重度只用于 replay/evaluation 报告分级，不授权 API、交易或 NQ mutation。
 */
public enum RegressionSeverity {
    /**
     * 信息性 finding，不阻断 case。
     */
    INFO,

    /**
     * 需要关注但可继续评估的偏差。
     */
    WARN,

    /**
     * 影响 regression verdict 的错误。
     */
    ERROR,

    /**
     * 安全边界或合同完整性阻断。
     */
    BLOCKER
}
