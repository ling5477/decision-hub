package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Gateway 只读 decision hint action。
 */
public enum ModelGatewayDecisionAction {
    /** 不给出方向性建议。 */
    ABSTAIN,
    /** 只观察，不建议交易。 */
    OBSERVE,
    /** 明确不交易。 */
    NO_TRADE,
    /** 只读方向性偏多，不是交易指令。 */
    LONG_BIAS,
    /** 只读方向性偏空，不是交易指令。 */
    SHORT_BIAS
}
