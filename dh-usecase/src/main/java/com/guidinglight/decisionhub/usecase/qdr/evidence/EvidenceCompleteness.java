package com.guidinglight.decisionhub.usecase.qdr.evidence;

/** Stage-QDR-10 decision/feedback evidence consolidation 的完整度。 */
public enum EvidenceCompleteness {
    /** 决策强制证据与 feedback 在 aggregate 携带的 bounded policy 内完整。 */
    COMPLETE_WITHIN_BOUNDS,
    /** 决策强制证据完整，但 bounded policy 内允许缺失 optional feedback。 */
    PARTIAL_WITHIN_BOUNDS,
    /** 证据来源、scope、identity、correlation 或 bounded read 不满足安全边界。 */
    INCONSISTENT,
    /** 强制 decision REQUEST 或 RUN root 不存在。 */
    NOT_FOUND
}
