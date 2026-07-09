package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Provider readiness guard 的固定 reason code。
 *
 * <p>Reason 只表达 fail-closed 或 safe-ready 原因，不携带 raw prompt、provider 原始响应、credential、
 * 交易动作或 NQ mutation 指令。
 */
public enum ProviderReadinessDecisionReason {
    /** tenant/source/provider/policy/safe evidence 均满足。 */
    READY_SAFE_CONTEXT,
    /** tenantId 缺失或不安全。 */
    MISSING_TENANT,
    /** providerRef 缺失或不安全。 */
    MISSING_PROVIDER_REF,
    /** policyVersion 缺失，按 policy 跳过。 */
    MISSING_POLICY_VERSION,
    /** source boundary 拒绝。 */
    SOURCE_DENIED,
    /** policy 或 trust decision 拒绝。 */
    POLICY_DENIED,
    /** timeout evidence 触发降级或 fail-closed。 */
    TIMEOUT,
    /** budget evidence 触发降级或 fail-closed。 */
    BUDGET_EXCEEDED,
    /** unknown classification 必须 fail-closed。 */
    UNKNOWN_CLASSIFICATION,
    /** credential-like 输入被拒绝。 */
    SENSITIVE_INPUT_REJECTED,
    /** raw prompt marker 被拒绝。 */
    RAW_PROMPT_REJECTED,
    /** raw provider response marker 被拒绝。 */
    RAW_PROVIDER_RESPONSE_REJECTED,
    /** BUY / SELL / MARKET_ORDER 等交易输入被拒绝。 */
    TRADING_INPUT_REJECTED,
    /** PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE 等执行输入被拒绝。 */
    NQ_MUTATION_REJECTED,
    /** safe evidence 合同校验失败。 */
    POLICY_EVIDENCE_REJECTED,
    /** policy evaluation 内部异常，必须 fail-closed。 */
    INTERNAL_ERROR
}
