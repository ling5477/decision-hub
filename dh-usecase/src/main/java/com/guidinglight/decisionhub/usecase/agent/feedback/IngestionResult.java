package com.guidinglight.decisionhub.usecase.agent.feedback;

import java.util.Objects;

/**
 * Stage2-PoC-B2：NQ feedback ingestion 结果。
 *
 * <p>结果只有三种 outcome：{@link IngestionOutcome#ACCEPTED} / {@link IngestionOutcome#DUPLICATE} / {@link
 * IngestionOutcome#REJECTED}。ACCEPTED 与 DUPLICATE 均回 HTTP 202；REJECTED 回 HTTP 400 并携带 {@link
 * #getErrorCode()}。
 */
public final class IngestionResult {

  private final IngestionOutcome outcome;
  private final String eventId;
  private final String status;
  private final IngestionErrorCode errorCode;
  private final String errorMessage;

  private IngestionResult(
      final IngestionOutcome outcome,
      final String eventId,
      final String status,
      final IngestionErrorCode errorCode,
      final String errorMessage) {
    this.outcome = Objects.requireNonNull(outcome, "outcome");
    this.eventId = eventId;
    this.status = status;
    this.errorCode = errorCode;
    this.errorMessage = errorMessage;
  }

  /** 首次接收并完成 handler 派发。 */
  public static IngestionResult accepted(final String eventId) {
    return new IngestionResult(IngestionOutcome.ACCEPTED, eventId, "RECEIVED", null, null);
  }

  /** 重放命中，按 {@code 原 status} 回 202；不重复派发 handler。 */
  public static IngestionResult duplicate(final String eventId) {
    return new IngestionResult(IngestionOutcome.DUPLICATE, eventId, "RECEIVED", null, null);
  }

  /** 校验失败。 */
  public static IngestionResult rejected(
      final String eventId, final IngestionErrorCode code, final String message) {
    return new IngestionResult(IngestionOutcome.REJECTED, eventId, null, code, message);
  }

  public IngestionOutcome getOutcome() {
    return outcome;
  }

  /** eventId；REJECTED 时可能为 null（如 eventId 本身缺失）。 */
  public String getEventId() {
    return eventId;
  }

  /** {@code status} 当且仅当 outcome != REJECTED 时非空。 */
  public String getStatus() {
    return status;
  }

  public IngestionErrorCode getErrorCode() {
    return errorCode;
  }

  public String getErrorMessage() {
    return errorMessage;
  }
}
