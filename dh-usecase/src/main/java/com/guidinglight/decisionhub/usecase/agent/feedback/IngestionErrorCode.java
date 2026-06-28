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
  /** payload 命中 Integration-0 冻结的敏感字段黑名单，必须拒绝且不得入库。 */
  FORBIDDEN_FIELD,
  /** payload 声明交易执行、NQ mutation、凭证访问等禁止能力，必须拒绝且不得派发。 */
  FORBIDDEN_CAPABILITY,
  /** traceId 未命中任何 ResearchRun。 */
  UNKNOWN_TRACE
}
