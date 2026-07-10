package com.guidinglight.decisionhub.usecase.qdr.evidence;

/**
 * Stage-QDR-6 B1 evidence aggregate 的安全状态。
 *
 * <p>状态只表达已传入 safe evidence contract 的校验结果，不触发读取、重放、Provider、HTTP、NQ 或交易行为。
 */
public enum DecisionEvidenceStatus {
    /** 所有当前 policy 的强制证据均存在、关联一致且引用安全。 */
    COMPLETE,
    /** 未发现不安全或冲突证据，但当前 policy 的强制证据尚不完整。 */
    INCOMPLETE,
    /** correlation、tenant 或安全引用不满足 fail-closed 边界。 */
    INVALID
}
