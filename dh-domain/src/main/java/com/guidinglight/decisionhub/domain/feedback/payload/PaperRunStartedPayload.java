package com.guidinglight.decisionhub.domain.feedback.payload;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：{@code PAPER_RUN_STARTED} 事件 payload。
 *
 * <p>表示 NQ 侧 paper run 已经从 CREATED 进入运行态并开始接收行情。
 */
public final class PaperRunStartedPayload {

  private final String paperRunId;
  private final Instant startedAt;
  private final String mode;
  private final String rawPayloadJson;

  private PaperRunStartedPayload(
      final String paperRunId,
      final Instant startedAt,
      final String mode,
      final String rawPayloadJson) {
    this.paperRunId = Objects.requireNonNull(paperRunId, "paperRunId");
    this.startedAt = Objects.requireNonNull(startedAt, "startedAt");
    this.mode = Objects.requireNonNull(mode, "mode");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static PaperRunStartedPayload of(
      final String paperRunId,
      final Instant startedAt,
      final String mode,
      final String rawPayloadJson) {
    return new PaperRunStartedPayload(paperRunId, startedAt, mode, rawPayloadJson);
  }

  public String getPaperRunId() {
    return paperRunId;
  }

  public Instant getStartedAt() {
    return startedAt;
  }

  /** 运行模式，例如 CONTINUOUS / BURST / REPLAY，文本由 NQ 侧定义。 */
  public String getMode() {
    return mode;
  }

  /** 原始 payload JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
