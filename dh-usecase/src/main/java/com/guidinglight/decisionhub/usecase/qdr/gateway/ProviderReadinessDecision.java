package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider readiness policy 的安全决策枚举。
 *
 * <p>`READY` 仅表示 future readiness criteria 已满足；所有枚举值都不打开真实 provider、真实 HTTP、
 * LIVE、交易权限、provider credential 授权或 NQ execution permission。
 */
public enum ProviderReadinessDecision {
    /** 满足 future readiness criteria，但仍不可执行真实 provider、HTTP、LIVE 或交易。 */
    READY,
    /** readiness evidence 不满足要求，必须 fail-closed。 */
    NOT_READY,
    /** evidence 可用于降级观察，但不可执行。 */
    DEGRADED,
    /** policy 缺少必要版本或显式跳过，不可执行。 */
    SKIPPED;

    /**
     * Provider readiness decision 永远不启用真实 provider。
     *
     * @return 固定为 false。
     */
    public boolean enablesRealProvider() {
        return false;
    }

    /**
     * Provider readiness decision 永远不启用真实 HTTP。
     *
     * @return 固定为 false。
     */
    public boolean enablesRealHttp() {
        return false;
    }

    /**
     * Provider readiness decision 永远不启用 LIVE。
     *
     * @return 固定为 false。
     */
    public boolean enablesLive() {
        return false;
    }

    /**
     * Provider readiness decision 永远不授予交易权限。
     *
     * @return 固定为 false。
     */
    public boolean allowsTrading() {
        return false;
    }
}
