package com.guidinglight.decisionhub.domain.agent;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stage1：研究任务下的有向任务图。
 *
 * <p>对应工单 4.1：AgentTask + TaskNode 树。Stage1 用扁平 list 表达，依赖关系通过 TaskNode.dependsOn 字段表达。
 */
public final class AgentTask {

  private final String taskId;
  private final String runId;
  private final String tenantId;
  private final String traceId;
  private final List<TaskNode> nodes;
  private final Map<String, Object> payloadJson;
  private final Instant createdAt;

  private AgentTask(
      final String taskId,
      final String runId,
      final String tenantId,
      final String traceId,
      final List<TaskNode> nodes,
      final Map<String, Object> payloadJson,
      final Instant createdAt) {
    this.taskId = taskId;
    this.runId = runId;
    this.tenantId = tenantId;
    this.traceId = traceId;
    this.nodes = new ArrayList<>(nodes);
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.createdAt = createdAt;
  }

  /** 工厂方法。 */
  public static AgentTask create(
      final String runId,
      final String tenantId,
      final String traceId,
      final List<TaskNode> nodes,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new AgentTask(
        IdGenerator.newId(), runId, tenantId, traceId, nodes, payloadJson, now);
  }

  public String getTaskId() {
    return taskId;
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

  public List<TaskNode> getNodes() {
    return Collections.unmodifiableList(nodes);
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
