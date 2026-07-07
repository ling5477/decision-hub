package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Map;
import java.util.Objects;

/**
 * Gateway 结构化只读决策 hint。
 *
 * <p>该对象不表达交易执行，不包含数量、价格、杠杆、订单或撤单指令。调用方仍必须经过 QDR
 * policy/risk/audit 后才能进入下一步只读审查。
 *
 * @param decisionType 固定只读类型。
 * @param action       只读 action。
 * @param confidence   本地 mock confidence，范围由调用方解释。
 * @param rationale    脱敏摘要。
 * @param constraints  脱敏约束。
 */
public record ModelGatewayDecision(
        String decisionType,
        ModelGatewayDecisionAction action,
        double confidence,
        String rationale,
        Map<String, String> constraints) {

    /**
     * 校验结构化 decision 基本形态。
     */
    public ModelGatewayDecision {
        decisionType = Objects.requireNonNullElse(decisionType, "READ_ONLY_RECOMMENDATION");
        action = Objects.requireNonNull(action, "action");
        constraints = Map.copyOf(constraints == null ? Map.of() : constraints);
    }

    /**
     * 创建 mock provider 默认只读 decision。
     *
     * @param rationale 脱敏摘要。
     * @return structured decision。
     */
    public static ModelGatewayDecision observe(final String rationale) {
        return new ModelGatewayDecision(
                "READ_ONLY_RECOMMENDATION",
                ModelGatewayDecisionAction.OBSERVE,
                0.5D,
                rationale,
                Map.of("execution", "forbidden", "provider", "mock-only"));
    }
}
