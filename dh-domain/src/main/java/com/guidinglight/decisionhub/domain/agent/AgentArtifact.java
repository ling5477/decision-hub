package com.guidinglight.decisionhub.domain.agent;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Map;

/**
 * Stage1：Agent 在节点执行过程中产生的结构化产物。
 *
 * <p>对应工单 4.1：AgentArtifact。
 * 例如 ScoutAgent 收集到的数据 reference，AnalystAgent 整理的证据摘要，StrategyAgent 提出的策略提案等。
 */
public final class AgentArtifact {

  private final String artifactId;
  private final String runId;
  private final String nodeId;
  private final AgentRole role;
  private final String type;
  private final Map<String, Object> payloadJson;
  private final Instant createdAt;

  private AgentArtifact(
      final String artifactId,
      final String runId,
      final String nodeId,
      final AgentRole role,
      final String type,
      final Map<String, Object> payloadJson,
      final Instant createdAt) {
    this.artifactId = artifactId;
    this.runId = runId;
    this.nodeId = nodeId;
    this.role = role;
    this.type = type;
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.createdAt = createdAt;
  }

  /** 工厂方法。 */
  public static AgentArtifact create(
      final String runId,
      final String nodeId,
      final AgentRole role,
      final String type,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new AgentArtifact(
        IdGenerator.newId(), runId, nodeId, role, type, payloadJson, now);
  }

  public String getArtifactId() {
    return artifactId;
  }

  public String getRunId() {
    return runId;
  }

  public String getNodeId() {
    return nodeId;
  }

  public AgentRole getRole() {
    return role;
  }

  public String getType() {
    return type;
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
