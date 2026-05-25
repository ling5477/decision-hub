package com.guidinglight.decisionhub.domain.reflection;

import com.guidinglight.decisionhub.domain.agent.AgentRole;
import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：reflection 记录。
 *
 * <p>每条 ReflectionEntry 代表一次过程中的回顾，不允许直接输出最终建议（JudgeDecision 仍是唯一最终出口）。
 *
 * <p>{@code stepIndex} >= 0；同一 runId 内单调递增。 {@code payloadJson} 用于扩展，禁止承载交易事实。
 */
public final class ReflectionEntry {

  private final String reflectionId;
  private final String runId;
  private final String traceId;
  private final int stepIndex;
  private final AgentRole agentRole;
  private final ReflectionType type;
  private final String content;
  private final Instant createdAt;
  private final String payloadJson;

  private ReflectionEntry(
      final String reflectionId,
      final String runId,
      final String traceId,
      final int stepIndex,
      final AgentRole agentRole,
      final ReflectionType type,
      final String content,
      final Instant createdAt,
      final String payloadJson) {
    this.reflectionId = Objects.requireNonNull(reflectionId, "reflectionId");
    this.runId = Objects.requireNonNull(runId, "runId");
    this.traceId = Objects.requireNonNull(traceId, "traceId");
    if (stepIndex < 0) {
      throw new IllegalArgumentException("stepIndex must be >= 0");
    }
    this.stepIndex = stepIndex;
    this.agentRole = Objects.requireNonNull(agentRole, "agentRole");
    this.type = Objects.requireNonNull(type, "type");
    this.content = Objects.requireNonNull(content, "content");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.payloadJson = payloadJson;
  }

  /** 工厂方法。 */
  public static ReflectionEntry of(
      final String reflectionId,
      final String runId,
      final String traceId,
      final int stepIndex,
      final AgentRole agentRole,
      final ReflectionType type,
      final String content,
      final Instant createdAt,
      final String payloadJson) {
    return new ReflectionEntry(
        reflectionId, runId, traceId, stepIndex, agentRole, type, content, createdAt, payloadJson);
  }

  public String getReflectionId() {
    return reflectionId;
  }

  public String getRunId() {
    return runId;
  }

  public String getTraceId() {
    return traceId;
  }

  public int getStepIndex() {
    return stepIndex;
  }

  public AgentRole getAgentRole() {
    return agentRole;
  }

  public ReflectionType getType() {
    return type;
  }

  /** 自由文本回顾内容，建议 <= 4KB。 */
  public String getContent() {
    return content;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /** 扩展 JSON，可空，禁止承载交易事实。 */
  public String getPayloadJson() {
    return payloadJson;
  }
}
