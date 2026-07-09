package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Stage-QDR-5 B4 内部验收证据状态。
 *
 * <p>`PASS` 只表示 Stage-QDR-5 internal acceptance evidence passed。`PASS` 不等于 provider authorization，
 * 不等于真实 provider enabled，不等于真实 HTTP enabled，不等于 LIVE enabled，不等于 trading allowed，
 * 也不等于 NQ execution allowed。
 */
public enum ProviderReadinessAcceptanceStatus {
    /**
     * 内部验收 evidence 通过，但仍不授权 runtime 或交易。
     */
    PASS,
    /**
     * 内部验收 evidence 存在降级或非阻断风险。
     */
    WARN,
    /**
     * 内部验收 evidence 失败或 fail-closed。
     */
    FAIL,
    /**
     * 内部验收 evidence 缺少必要输入或被安全跳过。
     */
    SKIPPED;

    /**
     * B4 acceptance status 永远不授权 provider。
     *
     * @return 固定为 false。
     */
    public boolean authorizesProvider() {
        return false;
    }

    /**
     * B4 acceptance status 永远不启用真实 provider。
     *
     * @return 固定为 false。
     */
    public boolean enablesRealProvider() {
        return false;
    }

    /**
     * B4 acceptance status 永远不启用真实 HTTP。
     *
     * @return 固定为 false。
     */
    public boolean enablesRealHttp() {
        return false;
    }

    /**
     * B4 acceptance status 永远不启用 LIVE。
     *
     * @return 固定为 false。
     */
    public boolean enablesLive() {
        return false;
    }

    /**
     * B4 acceptance status 永远不授予交易权限。
     *
     * @return 固定为 false。
     */
    public boolean allowsTrading() {
        return false;
    }

    /**
     * B4 acceptance status 永远不允许 NQ execution。
     *
     * @return 固定为 false。
     */
    public boolean allowsNqExecution() {
        return false;
    }
}
