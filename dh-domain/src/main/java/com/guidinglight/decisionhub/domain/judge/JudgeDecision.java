package com.guidinglight.decisionhub.domain.judge;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stage1：Judge Agent 给出的最终结构化仲裁结果。
 *
 * <p>对应工单 4.1：JudgeDecision。Stage1 不允许任何单 Agent 直接给最终结论；JudgeDecision 是 ResearchRun
 * 唯一的“决策出口”。
 */
public final class JudgeDecision {

  private final String decisionId;
  private final String runId;
  private final String tenantId;
  private final String traceId;
  /** 被 Judge 选中的候选 id 列表。 */
  private final List<String> selectedCandidateIds;
  /** 被 Judge 显式拒绝的候选 id 列表。 */
  private final List<String> rejectedCandidateIds;
  private final Map<String, Object> payloadJson;
  private JudgeDecisionStatus status;
  private final Instant createdAt;
  private Instant updatedAt;

  private JudgeDecision(
      final String decisionId,
      final String runId,
      final String tenantId,
      final String traceId,
      final List<String> selectedCandidateIds,
      final List<String> rejectedCandidateIds,
      final Map<String, Object> payloadJson,
      final JudgeDecisionStatus status,
      final Instant createdAt,
      final Instant updatedAt) {
    this.decisionId = decisionId;
    this.runId = runId;
    this.tenantId = tenantId;
    this.traceId = traceId;
    this.selectedCandidateIds =
        selectedCandidateIds == null ? List.of() : List.copyOf(selectedCandidateIds);
    this.rejectedCandidateIds =
        rejectedCandidateIds == null ? List.of() : List.copyOf(rejectedCandidateIds);
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** 工厂：默认 DRAFT 状态。 */
  public static JudgeDecision draft(
      final String runId,
      final String tenantId,
      final String traceId,
      final List<String> selectedCandidateIds,
      final List<String> rejectedCandidateIds,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new JudgeDecision(
        IdGenerator.newId(),
        runId,
        tenantId,
        traceId,
        selectedCandidateIds,
        rejectedCandidateIds,
        payloadJson,
        JudgeDecisionStatus.DRAFT,
        now,
        now);
  }

  /** 状态前进。 */
  public void transitionTo(final JudgeDecisionStatus next, final Instant now) {
    this.status = next;
    this.updatedAt = now;
  }

  public String getDecisionId() {
    return decisionId;
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

  public List<String> getSelectedCandidateIds() {
    return Collections.unmodifiableList(selectedCandidateIds);
  }

  public List<String> getRejectedCandidateIds() {
    return Collections.unmodifiableList(rejectedCandidateIds);
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public JudgeDecisionStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
