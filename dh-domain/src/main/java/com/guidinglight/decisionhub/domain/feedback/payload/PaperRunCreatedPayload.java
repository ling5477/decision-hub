package com.guidinglight.decisionhub.domain.feedback.payload;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：{@code PAPER_RUN_CREATED} 事件 payload。
 *
 * <p>表示 NQ 侧已经为某个候选创建了 paper run（尚未启动）。 字段全部不可变；{@code rawPayloadJson} 保留 NQ 原始 JSON，禁止丢失。
 */
public final class PaperRunCreatedPayload {

  private final String paperRunId;
  private final String candidateId;
  private final String strategyName;
  private final String requestedBy;
  private final Instant createdAt;
  private final String configHash;
  private final String rawPayloadJson;

  private PaperRunCreatedPayload(
      final String paperRunId,
      final String candidateId,
      final String strategyName,
      final String requestedBy,
      final Instant createdAt,
      final String configHash,
      final String rawPayloadJson) {
    this.paperRunId = Objects.requireNonNull(paperRunId, "paperRunId");
    this.candidateId = Objects.requireNonNull(candidateId, "candidateId");
    this.strategyName = Objects.requireNonNull(strategyName, "strategyName");
    this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy");
    this.createdAt = Objects.requireNonNull(createdAt, "createdAt");
    this.configHash = configHash;
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static PaperRunCreatedPayload of(
      final String paperRunId,
      final String candidateId,
      final String strategyName,
      final String requestedBy,
      final Instant createdAt,
      final String configHash,
      final String rawPayloadJson) {
    return new PaperRunCreatedPayload(
        paperRunId, candidateId, strategyName, requestedBy, createdAt, configHash, rawPayloadJson);
  }

  public String getPaperRunId() {
    return paperRunId;
  }

  public String getCandidateId() {
    return candidateId;
  }

  public String getStrategyName() {
    return strategyName;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  /** 配置摘要，可空。 */
  public String getConfigHash() {
    return configHash;
  }

  /** 原始 payload JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
