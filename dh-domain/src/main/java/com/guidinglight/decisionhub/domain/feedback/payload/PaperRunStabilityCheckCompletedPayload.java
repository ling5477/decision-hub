package com.guidinglight.decisionhub.domain.feedback.payload;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：{@code PAPER_RUN_STABILITY_CHECK_COMPLETED} 事件 payload。
 *
 * <p>表示 NQ 侧 paper run 完成了一次稳定性检查；结果由 {@link StabilityCheckResult} 给出。
 */
public final class PaperRunStabilityCheckCompletedPayload {

  private final String paperRunId;
  private final String checkId;
  private final StabilityCheckResult result;
  private final String summary;
  private final Instant completedAt;
  private final String rawPayloadJson;

  private PaperRunStabilityCheckCompletedPayload(
      final String paperRunId,
      final String checkId,
      final StabilityCheckResult result,
      final String summary,
      final Instant completedAt,
      final String rawPayloadJson) {
    this.paperRunId = Objects.requireNonNull(paperRunId, "paperRunId");
    this.checkId = Objects.requireNonNull(checkId, "checkId");
    this.result = Objects.requireNonNull(result, "result");
    this.summary = Objects.requireNonNull(summary, "summary");
    this.completedAt = Objects.requireNonNull(completedAt, "completedAt");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static PaperRunStabilityCheckCompletedPayload of(
      final String paperRunId,
      final String checkId,
      final StabilityCheckResult result,
      final String summary,
      final Instant completedAt,
      final String rawPayloadJson) {
    return new PaperRunStabilityCheckCompletedPayload(
        paperRunId, checkId, result, summary, completedAt, rawPayloadJson);
  }

  public String getPaperRunId() {
    return paperRunId;
  }

  public String getCheckId() {
    return checkId;
  }

  public StabilityCheckResult getResult() {
    return result;
  }

  public String getSummary() {
    return summary;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  /** 原始 payload JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
