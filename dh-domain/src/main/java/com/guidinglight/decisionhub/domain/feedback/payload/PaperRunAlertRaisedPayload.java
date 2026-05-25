package com.guidinglight.decisionhub.domain.feedback.payload;

import java.time.Instant;
import java.util.Objects;

/**
 * Stage2-PoC-B1：{@code PAPER_RUN_ALERT_RAISED} 事件 payload。
 *
 * <p>表示 NQ 侧 paper run 触发了告警。 告警级别由 {@link AlertLevel} 给出，{@code alertCode} 由 NQ 侧字典管理。
 */
public final class PaperRunAlertRaisedPayload {

  private final String paperRunId;
  private final String alertId;
  private final AlertLevel alertLevel;
  private final String alertCode;
  private final String message;
  private final Instant raisedAt;
  private final String rawPayloadJson;

  private PaperRunAlertRaisedPayload(
      final String paperRunId,
      final String alertId,
      final AlertLevel alertLevel,
      final String alertCode,
      final String message,
      final Instant raisedAt,
      final String rawPayloadJson) {
    this.paperRunId = Objects.requireNonNull(paperRunId, "paperRunId");
    this.alertId = Objects.requireNonNull(alertId, "alertId");
    this.alertLevel = Objects.requireNonNull(alertLevel, "alertLevel");
    this.alertCode = Objects.requireNonNull(alertCode, "alertCode");
    this.message = Objects.requireNonNull(message, "message");
    this.raisedAt = Objects.requireNonNull(raisedAt, "raisedAt");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static PaperRunAlertRaisedPayload of(
      final String paperRunId,
      final String alertId,
      final AlertLevel alertLevel,
      final String alertCode,
      final String message,
      final Instant raisedAt,
      final String rawPayloadJson) {
    return new PaperRunAlertRaisedPayload(
        paperRunId, alertId, alertLevel, alertCode, message, raisedAt, rawPayloadJson);
  }

  public String getPaperRunId() {
    return paperRunId;
  }

  public String getAlertId() {
    return alertId;
  }

  public AlertLevel getAlertLevel() {
    return alertLevel;
  }

  public String getAlertCode() {
    return alertCode;
  }

  public String getMessage() {
    return message;
  }

  public Instant getRaisedAt() {
    return raisedAt;
  }

  /** 原始 payload JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
