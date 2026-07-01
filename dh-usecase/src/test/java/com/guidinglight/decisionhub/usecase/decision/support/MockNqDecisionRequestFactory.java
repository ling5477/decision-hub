package com.guidinglight.decisionhub.usecase.decision.support;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;

import java.time.Instant;
import java.util.List;

/**
 * K6 mock NQ dry-run 请求工厂。
 *
 * <p>该工厂只构造 K1 `DecisionRequest` 的只读推荐语义，用来模拟 NQ 侧“请求 DH 给出结构化建议”的意图。
 * 这里不暴露账户、订单、价格、数量、side、credential 或任何执行字段，避免测试 fixture 被误用成 runtime 集成输入。
 */
public final class MockNqDecisionRequestFactory {

    /**
     * K6 固定时间，避免测试受当前系统时间影响。
     */
    public static final Instant REQUESTED_AT = Instant.parse("2026-07-01T00:00:00Z");

    private MockNqDecisionRequestFactory() {
    }

    /**
     * 创建 mock NQ 的有效 dry-run 请求。
     *
     * @return 包含 evidence refs 的只读 recommendation 请求。
     */
    public static DecisionRequest validDryRunRequest() {
        return request(
                "mock-nq-dryrun-valid-1",
                "trace-mock-nq-dryrun-valid-1",
                "tenant-mock-nq",
                "snapshot-mock-nq-valid",
                List.of("evidence://mock-nq/market-regime", "evidence://mock-nq/risk-summary"));
    }

    /**
     * 创建 provider guard 被触发时使用的 mock NQ 请求。
     *
     * @return 只读 recommendation 请求；是否 blocked 由 provider guard 测试控制。
     */
    public static DecisionRequest providerBlockedRequest() {
        return request(
                "mock-nq-provider-blocked-1",
                "trace-mock-nq-provider-blocked-1",
                "tenant-mock-nq",
                "snapshot-mock-nq-provider-blocked",
                List.of("evidence://mock-nq/provider-budget"));
    }

    /**
     * 创建 no-live-trade guard 用的 mock NQ 请求。
     *
     * @return 只读 recommendation 请求；名称中的 dry-run/no-live 只用于测试语义，不代表 LIVE 能力。
     */
    public static DecisionRequest noLiveTradeGuardRequest() {
        return request(
                "mock-nq-no-live-trade-guard-1",
                "trace-mock-nq-no-live-trade-guard-1",
                "tenant-mock-nq",
                "snapshot-mock-nq-no-live-trade",
                List.of("evidence://mock-nq/no-live-trade-guard"));
    }

    /**
     * 复制现有 mock NQ 请求到另一个 tenant，用于验证 K4 replay 不允许跨 tenant 读取。
     *
     * @param tenantId 目标租户 ID。
     * @return 租户替换后的只读 recommendation 请求。
     */
    public static DecisionRequest validDryRunRequestForTenant(final String tenantId) {
        return request(
                "mock-nq-dryrun-valid-1",
                "trace-mock-nq-dryrun-valid-1",
                tenantId,
                "snapshot-mock-nq-valid",
                List.of("evidence://mock-nq/market-regime", "evidence://mock-nq/risk-summary"));
    }

    private static DecisionRequest request(
            final String requestId,
            final String traceId,
            final String tenantId,
            final String snapshotId,
            final List<String> evidenceRefs) {
        return DecisionRequest.readOnlyRecommendation(
                requestId,
                traceId,
                tenantId,
                "NQ_MOCK",
                new DecisionSubject("BTC-USDT", "CRYPTO_SPOT", "1h", "mock-nq-readonly-strategy", null),
                "mock-nq://decision-pipeline/dryrun",
                new DecisionContextSnapshot(snapshotId, REQUESTED_AT, evidenceRefs),
                REQUESTED_AT);
    }
}
