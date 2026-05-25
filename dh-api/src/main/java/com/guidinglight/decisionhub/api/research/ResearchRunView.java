package com.guidinglight.decisionhub.api.research;

import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.domain.research.ResearchRunStatus;
import java.time.Instant;
import java.util.Map;

/** Stage1：ResearchRun 对外返回视图。 */
public final class ResearchRunView {

  private final String runId;
  private final String tenantId;
  private final String traceId;
  private final String topic;
  private final ResearchRunStatus status;
  private final Map<String, Object> payloadJson;
  private final Instant createdAt;
  private final Instant updatedAt;

  private ResearchRunView(
      final String runId,
      final String tenantId,
      final String traceId,
      final String topic,
      final ResearchRunStatus status,
      final Map<String, Object> payloadJson,
      final Instant createdAt,
      final Instant updatedAt) {
    this.runId = runId;
    this.tenantId = tenantId;
    this.traceId = traceId;
    this.topic = topic;
    this.status = status;
    this.payloadJson = payloadJson;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** 从领域对象构造视图。 */
  public static ResearchRunView of(final ResearchRun run) {
    return new ResearchRunView(
        run.getRunId(),
        run.getTenantId(),
        run.getTraceId(),
        run.getTopic(),
        run.getStatus(),
        run.getPayloadJson(),
        run.getCreatedAt(),
        run.getUpdatedAt());
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

  public ResearchRunStatus getStatus() {
    return status;
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
