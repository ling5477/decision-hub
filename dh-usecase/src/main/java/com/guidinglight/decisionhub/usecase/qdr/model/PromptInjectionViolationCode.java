package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * Prompt injection guard 拒绝类型。
 */
public enum PromptInjectionViolationCode {
    /**
     * 试图忽略或覆盖系统策略。
     */
    IGNORE_SYSTEM_POLICY,
    /**
     * 试图泄露 secret。
     */
    EXFILTRATE_SECRET,
    /**
     * 试图请求 credential。
     */
    REQUEST_CREDENTIAL,
    /**
     * 试图绕过风控或策略。
     */
    OVERRIDE_RISK_GATE,
    /**
     * 试图执行交易。
     */
    EXECUTE_TRADE,
    /**
     * 试图把只读 bias 映射为订单。
     */
    MAP_BIAS_TO_ORDER,
    /**
     * 试图修改 NQ 状态。
     */
    MUTATE_NQ,
    /**
     * 试图读取 raw/system prompt。
     */
    RAW_PROMPT_REQUEST,
    /**
     * 试图读取 raw provider response。
     */
    RAW_PROVIDER_RESPONSE_REQUEST,
    /**
     * 试图直接调用外部 provider 或提升到 Agent/tool runtime。
     */
    TOOL_OR_AGENT_ESCALATION,
    /**
     * 试图关闭或跳过 audit。
     */
    DISABLE_AUDIT,
    /**
     * guard 内部异常，必须 fail-closed。
     */
    GUARD_FAILURE
}
