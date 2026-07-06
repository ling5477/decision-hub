package com.guidinglight.decisionhub.usecase.qdr.readmodel;

/**
 * 读取模型证据脱敏状态。
 *
 * <p>仅作为只读边界语义，不携带任何敏感明文。
 */
public enum RedactionStatus {
    /**
     * 已脱敏，仅保留引用和摘要。
     */
    REDACTED,

    /**
     * 只返回 summary / refs，不返回原始敏感数据。
     */
    SUMMARY_ONLY,

    /**
     * 该查询上下文无敏感数据。
     */
    NO_SENSITIVE_DATA,

    /**
     * 当前场景无需脱敏。
     */
    NOT_APPLICABLE;
}
