package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderGuardResult;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;

import java.time.Instant;

/**
 * K5 provider guard 端口。
 *
 * <p>guard 分为 provider 调用前的 enabled/budget 检查，以及调用后的 latency/health/signal 检查。实现必须
 * fail-closed，不得把业务失败作为裸 RuntimeException 抛给 orchestrator。
 */
public interface DecisionProviderGuard {

    /**
     * provider 调用前检查 enabled 与 budget。
     *
     * @param providerName provider 名称
     * @param context      单次只读 context
     * @param checkedAt    检查时间
     * @return guard 结果；失败时 orchestrator 不得调用 provider
     */
    DecisionProviderGuardResult beforeProviderCall(
            String providerName, DecisionContext context, Instant checkedAt);

    /**
     * provider 调用后检查 latency、health 与 signal 合法性。
     *
     * @param providerName provider 名称
     * @param context      单次只读 context
     * @param signal       mock provider signal；null 表示 provider 调用失败
     * @param latency      本地 latency 观测
     * @param checkedAt    检查时间
     * @return guard 结果；失败时 orchestrator 必须输出 ABSTAIN/BLOCKED
     */
    DecisionProviderGuardResult afterProviderCall(
            String providerName,
            DecisionContext context,
            DecisionSignalResult signal,
            DecisionProviderLatency latency,
            Instant checkedAt);
}
