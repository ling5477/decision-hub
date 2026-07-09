package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider readiness guard contract。
 *
 * <p>Guard 负责 tenant/source/provider/policy/evidence 边界校验与 fail-closed，不授权真实 provider、
 * HTTP、LIVE、交易或 NQ execution。
 */
public interface ProviderReadinessGuard {

    /**
     * 执行 provider readiness guard。
     *
     * @param command policy evaluation command。
     * @return readiness evaluation result。
     */
    ProviderReadinessEvaluationResult evaluate(ProviderReadinessEvaluationCommand command);
}
