package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderHealth;
import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;

import java.time.Instant;

/**
 * K5 provider health evaluator 端口。
 *
 * <p>实现只能基于本地 mock signal、latency sample 和固定规则判断健康状态，不得调用真实 provider、HTTP、LLM、
 * NQ runtime 或任何外部工具。
 */
public interface DecisionProviderHealthEvaluator {

    /**
     * 评估本次 provider signal 的健康状态。
     *
     * @param providerName provider 名称；K5 首版只允许 mock provider 名称
     * @param signal       本地 mock provider signal；null 表示调用失败或未得到安全结果
     * @param latency      本地 latency 观测；可为空，空值按无法证明健康处理
     * @param observedAt   health 观测时间
     * @return provider health 观测结果
     */
    DecisionProviderHealth evaluate(
            String providerName,
            DecisionSignalResult signal,
            DecisionProviderLatency latency,
            Instant observedAt);
}
