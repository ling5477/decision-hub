package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider readiness 的本地 policy evaluation contract。
 *
 * <p>Policy 只处理已由 guard 校验过的 safe evidence，不接真实 provider、不发 HTTP、不读取凭证、不访问
 * NQ、不启动 Agent/LangGraph，也不生成交易信号。
 */
@FunctionalInterface
public interface ProviderReadinessPolicy {

    /**
     * 评估 provider readiness。
     *
     * @param command 已通过 guard 校验的 safe command。
     * @return readiness evaluation result。
     */
    ProviderReadinessEvaluationResult evaluate(ProviderReadinessEvaluationCommand command);
}
