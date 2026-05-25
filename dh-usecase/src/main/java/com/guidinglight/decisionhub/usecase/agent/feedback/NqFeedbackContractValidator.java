package com.guidinglight.decisionhub.usecase.agent.feedback;

/**
 * Stage2-PoC-B2：NQ feedback envelope 契约校验端口。
 *
 * <p>对应 docs/current/STAGE2_POC_WORK_ORDER.md §Batch 2.4：
 *
 * <ol>
 *   <li>envelope 字段必填（eventId / eventType / occurredAt / sourceSystem / sourceJobId / traceId /
 *       requestId / correlationId / schemaVersion / payloadJson）。
 *   <li>{@code rawEventType} 必须能映射到 {@code NqFeedbackEventType} 枚举（否则 {@link
 *       IngestionErrorCode#UNKNOWN_EVENT_TYPE}）。
 *   <li>{@code schemaVersion} >= "1.0.0"（否则 {@link IngestionErrorCode#INVALID_SCHEMA}）。
 *   <li>{@code traceId} 必须命中 {@code ResearchRunRepository}（否则 {@link
 *       IngestionErrorCode#UNKNOWN_TRACE}）。
 *   <li>{@code payloadJson} 按 eventType 做最小结构校验（必填字段命中；否则 {@link
 *       IngestionErrorCode#INVALID_SCHEMA}）。
 * </ol>
 */
public interface NqFeedbackContractValidator {

  /** 执行 §Batch 2.4 全部校验；通过时返回构造好的 envelope。 */
  ValidationResult validate(IngestionCommand command);
}
