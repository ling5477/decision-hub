package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider readiness 的安全 finding。
 *
 * <p>finding 只能保存 safe code/ref/summary；不得保存 raw prompt、provider raw output、credential-like
 * material 或任何交易执行动作。
 *
 * @param severity   finding severity。
 * @param code       safe finding code。
 * @param summaryRef safe finding summary ref。
 */
public record ProviderReadinessFinding(
        ProviderReadinessSeverity severity, String code, String summaryRef) {
}
