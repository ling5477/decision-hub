package com.guidinglight.decisionhub.usecase.qdr.evidence;

/** Stage-QDR-10 decision/feedback evidence consolidation 的完整度。 */
public enum EvidenceCompleteness {
    /** 决策强制证据与至少一条 feedback evidence 均完整且关联一致。 */
    COMPLETE,
    /** 决策强制证据完整，但允许缺失的 optional feedback evidence 不存在。 */
    PARTIAL,
    /** 证据来源、scope、identity、correlation 或 bounded read 不满足安全边界。 */
    INCONSISTENT,
    /** 强制 decision REQUEST 或 RUN root 不存在。 */
    NOT_FOUND
}
