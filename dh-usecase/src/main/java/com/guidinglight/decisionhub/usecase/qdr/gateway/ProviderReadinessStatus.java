package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider readiness / health 的非执行状态。
 *
 * <p>这些值只表示 observability evidence 或未来 readiness 条件，不表示真实 provider authorization、
 * HTTP enablement、LIVE permission 或交易权限。
 */
public enum ProviderReadinessStatus {
    /** readiness 条件满足，但仍不可执行真实 provider 或 LIVE。 */
    READY,
    /** readiness 条件不满足，必须 fail-closed。 */
    NOT_READY,
    /** readiness evidence 降级，不可执行。 */
    DEGRADED,
    /** readiness evidence 被跳过，不可执行。 */
    SKIPPED
}
