package com.guidinglight.decisionhub.domain.qdr;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;

/**
 * Quant Decision Review 输出动作枚举。
 *
 * <p>本枚举故意不包含 `BUY`、`SELL`、`PLACE_ORDER`、`CANCEL_ORDER`。未知或异常路径必须落到
 * `NEEDS_REVIEW` 或 `REJECTED`，不得转换成可执行交易指令。
 */
public enum QuantDecisionAction {
    /**
     * 仅观察，不表达方向性偏好。
     */
    OBSERVE,

    /**
     * 明确不交易；只读审查结论，不是交易指令。
     */
    NO_TRADE,

    /**
     * 方向性 long bias；不是 BUY。
     */
    LONG_BIAS,

    /**
     * 方向性 short bias；不是 SELL。
     */
    SHORT_BIAS,

    /**
     * 需要后续人工或 read model 审查。
     */
    NEEDS_REVIEW,

    /**
     * 策略、安全、provider 或持久化失败后的 fail-closed 结果。
     */
    REJECTED;

    /**
     * 从既有 read-only `DecisionOutput` action 映射到 QDR action。
     *
     * @param action 既有 Stage4 action。
     * @return QDR 安全集合内的 action。
     */
    public static QuantDecisionAction fromDecisionAction(final DecisionAction action) {
        if (action == null) {
            return NEEDS_REVIEW;
        }
        return switch (action) {
            case OBSERVE -> OBSERVE;
            case NO_TRADE, ABSTAIN -> NO_TRADE;
            case LONG_BIAS -> LONG_BIAS;
            case SHORT_BIAS -> SHORT_BIAS;
        };
    }
}
