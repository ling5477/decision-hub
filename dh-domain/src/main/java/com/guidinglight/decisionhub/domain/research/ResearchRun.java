package com.guidinglight.decisionhub.domain.research;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Map;

/**
 * Stage1：研究任务聚合根。
 *
 * <p>对应工单 4.1：ResearchRun。Stage1 只承载任务元数据 + 状态机；候选/仲裁等子聚合分表存储。
 */
public final class ResearchRun {

  private final String runId;
  private final String tenantId;
  /** 同一研究链路的关联 traceId，用于审计/复盘。 */
  private final String traceId;
  private final String topic;
  private final Map<String, Object> payloadJson;
  private ResearchRunStatus status;
  private final Instant createdAt;
  private Instant updatedAt;

  private ResearchRun(
      final String runId,
      final String tenantId,
      final String traceId,
      final String topic,
      final Map<String, Object> payloadJson,
      final ResearchRunStatus status,
      final Instant createdAt,
      final Instant updatedAt) {
    this.runId = runId;
    this.tenantId = tenantId;
    this.traceId = traceId;
    this.topic = topic;
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** 工厂：创建新 ResearchRun，状态为 CREATED。 */
  public static ResearchRun create(
      final String tenantId,
      final String topic,
      final Map<String, Object> payloadJson,
      final Instant now) {
    final String id = IdGenerator.newId();
    return new ResearchRun(
        id, tenantId, id, topic, payloadJson, ResearchRunStatus.CREATED, now, now);
  }

  /** 重建用：从持久层 rehydrate。 */
  public static ResearchRun rehydrate(
      final String runId,
      final String tenantId,
      final String traceId,
      final String topic,
      final Map<String, Object> payloadJson,
      final ResearchRunStatus status,
      final Instant createdAt,
      final Instant updatedAt) {
    return new ResearchRun(
        runId, tenantId, traceId, topic, payloadJson, status, createdAt, updatedAt);
  }

  /** 状态前进。Stage1 不做严格状态机校验，由 use case 控制流转顺序。 */
  public void transitionTo(final ResearchRunStatus next, final Instant now) {
    this.status = next;
    this.updatedAt = now;
  }

  public String getRunId() {
    return runId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getTopic() {
    return topic;
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public ResearchRunStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
