package com.guidinglight.decisionhub.domain.candidate;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Stage1：策略候选下的具体信号提案（如交易方向、目标合约、时间窗口等结构化建议）。
 *
 * <p>对应工单 4.1：SignalProposal。注意：这只是 AI 输出的建议，不是订单或交易指令。
 */
public final class SignalProposal {

  private final String proposalId;
  private final String candidateId;
  private final String runId;
  private final String traceId;
  private final List<String> instruments;
  private final Map<String, Object> payloadJson;
  private final Instant createdAt;

  private SignalProposal(
      final String proposalId,
      final String candidateId,
      final String runId,
      final String traceId,
      final List<String> instruments,
      final Map<String, Object> payloadJson,
      final Instant createdAt) {
    this.proposalId = proposalId;
    this.candidateId = candidateId;
    this.runId = runId;
    this.traceId = traceId;
    this.instruments = instruments == null ? List.of() : List.copyOf(instruments);
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.createdAt = createdAt;
  }

  /** 工厂方法。 */
  public static SignalProposal create(
      final String candidateId,
      final String runId,
      final String traceId,
      final List<String> instruments,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new SignalProposal(
        IdGenerator.newId(), candidateId, runId, traceId, instruments, payloadJson, now);
  }

  public String getProposalId() {
    return proposalId;
  }

  public String getCandidateId() {
    return candidateId;
  }

  public String getRunId() {
    return runId;
  }

  public String getTraceId() {
    return traceId;
  }

  public List<String> getInstruments() {
    return Collections.unmodifiableList(instruments);
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
