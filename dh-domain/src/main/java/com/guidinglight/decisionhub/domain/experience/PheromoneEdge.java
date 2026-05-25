package com.guidinglight.decisionhub.domain.experience;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Map;

/**
 * Stage1：信息素边（蚁群机制的轻量版本）。
 *
 * <p>对应工单 4.1：PheromoneEdge。表达两个节点（如 marketRegime -> strategyPattern）之间的“路径偏好强度”。
 * 第一阶段只做分数读写，不做全局优化算法。
 */
public final class PheromoneEdge {

  private final String edgeId;
  private final String tenantId;
  private final String fromNode;
  private final String toNode;
  private double pheromoneScore;
  private final Map<String, Object> payloadJson;
  private final Instant createdAt;
  private Instant updatedAt;

  private PheromoneEdge(
      final String edgeId,
      final String tenantId,
      final String fromNode,
      final String toNode,
      final double pheromoneScore,
      final Map<String, Object> payloadJson,
      final Instant createdAt,
      final Instant updatedAt) {
    this.edgeId = edgeId;
    this.tenantId = tenantId;
    this.fromNode = fromNode;
    this.toNode = toNode;
    this.pheromoneScore = pheromoneScore;
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** 工厂：新建信息素边，初始分数为 0。 */
  public static PheromoneEdge create(
      final String tenantId,
      final String fromNode,
      final String toNode,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new PheromoneEdge(
        IdGenerator.newId(), tenantId, fromNode, toNode, 0.0, payloadJson, now, now);
  }

  /** 成功增强：score += delta。 */
  public void reinforce(final double delta, final Instant now) {
    this.pheromoneScore += delta;
    this.updatedAt = now;
  }

  /** 失败衰减：score -= penalty。 */
  public void decay(final double penalty, final Instant now) {
    this.pheromoneScore -= penalty;
    this.updatedAt = now;
  }

  public String getEdgeId() {
    return edgeId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getFromNode() {
    return fromNode;
  }

  public String getToNode() {
    return toNode;
  }

  public double getPheromoneScore() {
    return pheromoneScore;
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
