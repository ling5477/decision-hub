package com.guidinglight.decisionhub.domain.feedback.payload;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：{@code PAPER_RUN_STOPPED} 事件 payload。
 *
 * <p>表示 NQ 侧 paper run 已停止；停止原因由 {@code reason} 携带，文本由 NQ 侧定义（如 MANUAL / AUTOMATIC / RISK_TRIGGERED）。
 */
public final class PaperRunStoppedPayload {

  private final String paperRunId;
  private final Instant stoppedAt;
  private final String reason;
  private final String rawPayloadJson;

  private PaperRunStoppedPayload(
      final String paperRunId,
      final Instant stoppedAt,
      final String reason,
      final String rawPayloadJson) {
    this.paperRunId = Objects.requireNonNull(paperRunId, "paperRunId");
    this.stoppedAt = Objects.requireNonNull(stoppedAt, "stoppedAt");
    this.reason = Objects.requireNonNull(reason, "reason");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static PaperRunStoppedPayload of(
      final String paperRunId,
      final Instant stoppedAt,
      final String reason,
      final String rawPayloadJson) {
    return new PaperRunStoppedPayload(paperRunId, stoppedAt, reason, rawPayloadJson);
  }

  public String getPaperRunId() {
    return paperRunId;
  }

  public Instant getStoppedAt() {
    return stoppedAt;
  }

  /** 停止原因，文本由 NQ 侧定义；DH 不应该把它解析为强类型分支。 */
  public String getReason() {
    return reason;
  }

  /** 原始 payload JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
