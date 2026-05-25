package com.guidinglight.decisionhub.usecase.agent.feedback;

/**
 * Stage2-PoC-B2：NQ feedback ingestion 处理结果分类。
 *
 * <ul>
 *   <li>{@link #ACCEPTED}：首次接收并完成 handler 派发。
 *   <li>{@link #DUPLICATE}：相同 eventId 已存在，幂等命中（不重复派发 handler）。
 *   <li>{@link #REJECTED}：校验失败，未入库、未派发。
 * </ul>
 */
public enum IngestionOutcome {
  ACCEPTED,
  DUPLICATE,
  REJECTED
}
