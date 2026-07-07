package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway fail-closed failure code。
 */
public enum ModelGatewayFailureCode {
    /** 必填 tenant/trace/request/run/version/provider 字段缺失。 */
    MISSING_REQUIRED_FIELD,
    /** prompt version registry 未命中。 */
    PROMPT_VERSION_NOT_FOUND,
    /** model version registry 未命中。 */
    MODEL_VERSION_NOT_FOUND,
    /** registry 命中但 tenant/id/checksum 不一致。 */
    REGISTRY_MISMATCH,
    /** prompt injection guard 或 render policy 拒绝。 */
    PROMPT_DENIED,
    /** prompt render 失败。 */
    PROMPT_RENDER_FAILED,
    /** ProviderTrustPolicy 拒绝。 */
    POLICY_DENIED,
    /** provider profile 未知。 */
    UNKNOWN_PROVIDER,
    /** provider profile disabled 或 planned。 */
    PROVIDER_DISABLED,
    /** real provider 在 B2 被禁止。 */
    REAL_PROVIDER_FORBIDDEN,
    /** mock provider 不可用。 */
    PROVIDER_UNAVAILABLE,
    /** mock provider timeout。 */
    PROVIDER_TIMEOUT,
    /** budget 缺失、非法或超限。 */
    BUDGET_EXCEEDED,
    /** redaction guard 失败。 */
    REDACTION_FAILED,
    /** provider structured result 缺失或畸形。 */
    PROVIDER_OUTPUT_INVALID,
    /** 未分类异常，必须 fail-closed。 */
    UNKNOWN_ERROR
}
