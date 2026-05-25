package com.guidinglight.decisionhub.usecase.agent.feedback;

/**
 * Stage2-PoC-B2：NQ feedback ingestion 错误码。
 *
 * <p>对应 docs/current/STAGE2_POC_WORK_ORDER.md §Batch 2.4 校验规则。所有 400 响应都使用本枚举值。
 */
public enum IngestionErrorCode {
  /** eventType 字符串不在 {@code NqFeedbackEventType} 枚举内。 */
  UNKNOWN_EVENT_TYPE,
  /** envelope schema 字段缺失 / schemaVersion 不合规 / payload 结构不匹配 eventType。 */
  INVALID_SCHEMA,
  /** traceId 未命中任何 ResearchRun。 */
  UNKNOWN_TRACE
}
