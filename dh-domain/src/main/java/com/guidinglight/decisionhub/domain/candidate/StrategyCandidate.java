package com.guidinglight.decisionhub.domain.candidate;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stage1：策略候选。
 *
 * <p>对应工单 4.1：StrategyCandidate。蜂群机制的产物：同一 ResearchRun 下并行生成多个候选， 每个候选记录
 * sourceAgent、searchPath、evidenceRefs、scoreSnapshot 以便复盘。
 */
public final class StrategyCandidate {

  private final String candidateId;
  private final String runId;
  private final String tenantId;
  private final String traceId;
  /** 候选生成的 Agent 路径（蜂群搜索的“分支标记”）。 */
  private final String sourceAgent;
  /** Stage1：标记搜索路径，例如 "scout-1#analyst-2#strategy-3"。 */
  private final String searchPath;
  /** 引用的证据 artifactId 列表。 */
  private final List<String> evidenceRefs;
  /** 候选本体的结构化定义。 */
  private final Map<String, Object> payloadJson;
  private Map<String, Object> scoreSnapshot;
  private CandidateStatus status;
  private final Instant createdAt;
  private Instant updatedAt;

  private StrategyCandidate(
      final String candidateId,
      final String runId,
      final String tenantId,
      final String traceId,
      final String sourceAgent,
      final String searchPath,
      final List<String> evidenceRefs,
      final Map<String, Object> payloadJson,
      final CandidateStatus status,
      final Instant createdAt,
      final Instant updatedAt) {
    this.candidateId = candidateId;
    this.runId = runId;
    this.tenantId = tenantId;
    this.traceId = traceId;
    this.sourceAgent = sourceAgent;
    this.searchPath = searchPath;
    this.evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** 工厂：新候选，初始状态 GENERATED。 */
  public static StrategyCandidate create(
      final String runId,
      final String tenantId,
      final String traceId,
      final String sourceAgent,
      final String searchPath,
      final List<String> evidenceRefs,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new StrategyCandidate(
        IdGenerator.newId(),
        runId,
        tenantId,
        traceId,
        sourceAgent,
        searchPath,
        evidenceRefs,
        payloadJson,
        CandidateStatus.GENERATED,
        now,
        now);
  }

  /** 写入或更新评分快照。 */
  public void applyScoreSnapshot(final Map<String, Object> snapshot, final Instant now) {
    this.scoreSnapshot = snapshot == null ? Map.of() : Map.copyOf(snapshot);
    this.updatedAt = now;
  }

  /** 状态前进。 */
  public void transitionTo(final CandidateStatus next, final Instant now) {
    this.status = next;
    this.updatedAt = now;
  }

  public String getCandidateId() {
    return candidateId;
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

  public String getSourceAgent() {
    return sourceAgent;
  }

  public String getSearchPath() {
    return searchPath;
  }

  public List<String> getEvidenceRefs() {
    return Collections.unmodifiableList(evidenceRefs);
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Map<String, Object> getScoreSnapshot() {
    return scoreSnapshot == null ? Map.of() : scoreSnapshot;
  }

  public CandidateStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
