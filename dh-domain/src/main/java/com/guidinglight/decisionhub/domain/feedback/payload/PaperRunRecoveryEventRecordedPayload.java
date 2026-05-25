package com.guidinglight.decisionhub.domain.feedback.payload;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：{@code PAPER_RUN_RECOVERY_EVENT_RECORDED} 事件 payload。
 *
 * <p>表示 NQ 侧 paper run 记录了一次恢复事件（例如重启完成、对账成功、状态机回滚）。
 * 恢复原因由 NQ 侧文本承载；DH 用于经验权重，不直接据此触发任何执行动作。
 */
public final class PaperRunRecoveryEventRecordedPayload {

  private final String paperRunId;
  private final String recoveryEventId;
  private final String recoveryReason;
  private final Instant recoveredAt;
  private final String rawPayloadJson;

  private PaperRunRecoveryEventRecordedPayload(
      final String paperRunId,
      final String recoveryEventId,
      final String recoveryReason,
      final Instant recoveredAt,
      final String rawPayloadJson) {
    this.paperRunId = Objects.requireNonNull(paperRunId, "paperRunId");
    this.recoveryEventId = Objects.requireNonNull(recoveryEventId, "recoveryEventId");
    this.recoveryReason = Objects.requireNonNull(recoveryReason, "recoveryReason");
    this.recoveredAt = Objects.requireNonNull(recoveredAt, "recoveredAt");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static PaperRunRecoveryEventRecordedPayload of(
      final String paperRunId,
      final String recoveryEventId,
      final String recoveryReason,
      final Instant recoveredAt,
      final String rawPayloadJson) {
    return new PaperRunRecoveryEventRecordedPayload(
        paperRunId, recoveryEventId, recoveryReason, recoveredAt, rawPayloadJson);
  }

  public String getPaperRunId() {
    return paperRunId;
  }

  public String getRecoveryEventId() {
    return recoveryEventId;
  }

  public String getRecoveryReason() {
    return recoveryReason;
  }

  public Instant getRecoveredAt() {
    return recoveredAt;
  }

  /** 原始 payload JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
