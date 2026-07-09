package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider readiness finding 的严重度。
 */
public enum ProviderReadinessSeverity {
    /** 信息级 evidence。 */
    INFO,
    /** 需要关注但不一定阻断。 */
    WARNING,
    /** 错误级 finding。 */
    ERROR,
    /** 阻断级 finding，必须 fail-closed。 */
    BLOCKING
}
