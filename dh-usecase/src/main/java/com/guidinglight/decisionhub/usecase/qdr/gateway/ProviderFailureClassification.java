package com.guidinglight.decisionhub.usecase.qdr.gateway;

import java.util.Objects;

/**
 * Provider readiness / observability 的结构化失败分类。
 *
 * <p>该分类只用于内部 evidence 与 fail-closed readiness 判断，不表示真实 provider、HTTP、LIVE 或交易权限。
 */
public enum ProviderFailureClassification {
    /** 无失败。 */
    NONE,
    /** provider 或 mock gateway 超时。 */
    TIMEOUT,
    /** 本地预算或 usage guard 超限。 */
    BUDGET_EXCEEDED,
    /** trust / policy guard 拒绝。 */
    POLICY_DENIED,
    /** provider source、profile、kind 或 planned source 被拒绝。 */
    SOURCE_DENIED,
    /** 请求签名无效；B1 只保留分类，不做验签实现。 */
    SIGNATURE_INVALID,
    /** nonce replay；B1 只保留分类，不做 replay store。 */
    NONCE_REPLAY,
    /** tenant 边界不匹配。 */
    TENANT_MISMATCH,
    /** payload、redaction 或 registry contract 被拒绝。 */
    PAYLOAD_REJECTED,
    /** provider unavailable。 */
    PROVIDER_UNAVAILABLE,
    /** 未分类异常，必须 fail-closed。 */
    UNKNOWN;

    /**
     * 将既有 model gateway failure code 映射为 B1 observability 分类。
     *
     * @param failureCode 既有 gateway fail-closed code。
     * @return B1 provider failure classification。
     */
    public static ProviderFailureClassification fromGatewayFailureCode(
            final ModelGatewayFailureCode failureCode) {
        return switch (Objects.requireNonNull(failureCode, "failureCode")) {
            case PROVIDER_TIMEOUT -> TIMEOUT;
            case BUDGET_EXCEEDED -> BUDGET_EXCEEDED;
            case POLICY_DENIED -> POLICY_DENIED;
            case UNKNOWN_PROVIDER, PROVIDER_DISABLED, REAL_PROVIDER_FORBIDDEN -> SOURCE_DENIED;
            case PROVIDER_UNAVAILABLE -> PROVIDER_UNAVAILABLE;
            case UNKNOWN_ERROR -> UNKNOWN;
            case MISSING_REQUIRED_FIELD,
                    PROMPT_VERSION_NOT_FOUND,
                    MODEL_VERSION_NOT_FOUND,
                    REGISTRY_MISMATCH,
                    PROMPT_DENIED,
                    PROMPT_RENDER_FAILED,
                    REDACTION_FAILED,
                    PROVIDER_OUTPUT_INVALID -> PAYLOAD_REJECTED;
        };
    }
}
