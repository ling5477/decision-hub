package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * MockModelProvider deterministic mode。
 */
public enum MockModelProviderMode {
    /** 正常返回结构化 mock result。 */
    NORMAL,
    /** 模拟 provider unavailable。 */
    UNAVAILABLE,
    /** 模拟 provider timeout。 */
    TIMEOUT,
    /** 模拟 provider 内部 budget exceeded。 */
    BUDGET_EXCEEDED,
    /** 模拟 provider 内部 policy denied。 */
    POLICY_DENIED,
    /** 模拟 provider 返回畸形结构。 */
    MALFORMED,
    /** 模拟 provider 输出 redaction failure。 */
    REDACTION_FAILURE
}
