package com.guidinglight.decisionhub.domain.agent;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stage1：任务图中单个节点。
 *
 * <p>对应工单 4.1：TaskNode。承载执行角色、依赖关系、状态与结构化产物快照。
 */
public final class TaskNode {

  private final String nodeId;
  private final String taskId;
  private final AgentRole role;
  private final String name;
  private final List<String> dependsOn;
  private TaskNodeStatus status;
  private final Map<String, Object> payloadJson;
  private Map<String, Object> outputJson;
  private Instant startedAt;
  private Instant finishedAt;

  private TaskNode(
      final String nodeId,
      final String taskId,
      final AgentRole role,
      final String name,
      final List<String> dependsOn,
      final TaskNodeStatus status,
      final Map<String, Object> payloadJson) {
    this.nodeId = nodeId;
    this.taskId = taskId;
    this.role = role;
    this.name = name;
    this.dependsOn = dependsOn == null ? List.of() : List.copyOf(dependsOn);
    this.status = status;
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
  }

  /** 工厂：PENDING 状态的新节点。taskId 在节点被纳入 AgentTask 时通过 rehydrate 设置。 */
  public static TaskNode create(
      final AgentRole role,
      final String name,
      final List<String> dependsOn,
      final Map<String, Object> payloadJson) {
    return new TaskNode(
        IdGenerator.newId(), null, role, name, dependsOn, TaskNodeStatus.PENDING, payloadJson);
  }

  /** 标记节点开始执行。 */
  public void markRunning(final Instant now) {
    this.status = TaskNodeStatus.RUNNING;
    this.startedAt = now;
  }

  /** 标记节点成功并附上结构化产物快照。 */
  public void markSucceeded(final Map<String, Object> outputJson, final Instant now) {
    this.status = TaskNodeStatus.SUCCEEDED;
    this.outputJson = outputJson == null ? Map.of() : Map.copyOf(outputJson);
    this.finishedAt = now;
  }

  /** 标记节点失败。 */
  public void markFailed(final Map<String, Object> outputJson, final Instant now) {
    this.status = TaskNodeStatus.FAILED;
    this.outputJson = outputJson == null ? Map.of() : Map.copyOf(outputJson);
    this.finishedAt = now;
  }

  /** 标记节点被跳过。 */
  public void markSkipped(final Instant now) {
    this.status = TaskNodeStatus.SKIPPED;
    this.finishedAt = now;
  }

  public String getNodeId() {
    return nodeId;
  }

  public String getTaskId() {
    return taskId;
  }

  public AgentRole getRole() {
    return role;
  }

  public String getName() {
    return name;
  }

  public List<String> getDependsOn() {
    return Collections.unmodifiableList(dependsOn);
  }

  public TaskNodeStatus getStatus() {
    return status;
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Map<String, Object> getOutputJson() {
    return outputJson == null ? Map.of() : outputJson;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  public Instant getFinishedAt() {
    return finishedAt;
  }
}
