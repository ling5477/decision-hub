package com.guidinglight.decisionhub.domain.qdr.approval;

/**
 * Human Approval Packet 审批类型。
 *
 * <p>类型只表示 DH 内部人工审查类别，不代表交易授权或外部系统调用意图。
 */
public enum ApprovalType {
    /**
     * Quant Decision Review 审查。
     */
    QUANT_DECISION_REVIEW,

    /**
     * 风险审查。
     */
    RISK_REVIEW,

    /**
     * 策略发布审查。
     */
    STRATEGY_RELEASE_REVIEW,

    /**
     * 异常审查。
     */
    ANOMALY_REVIEW
}
