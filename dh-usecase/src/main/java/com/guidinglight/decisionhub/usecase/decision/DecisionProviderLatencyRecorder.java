package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderLatency;

import java.time.Instant;

/**
 * K5 provider latency recorder 端口。
 *
 * <p>实现只能记录本地时间差或测试注入时间，不得依赖真实 HTTP client、外部 provider SDK、NQ runtime 或
 * LangGraph runtime。
 */
public interface DecisionProviderLatencyRecorder {

    /**
     * 记录一次 provider 调用的本地 latency。
     *
     * @param providerName provider 名称
     * @param startedAt    调用开始时间
     * @param endedAt      调用结束时间
     * @return latency 观测结果
     */
    DecisionProviderLatency record(String providerName, Instant startedAt, Instant endedAt);
}
