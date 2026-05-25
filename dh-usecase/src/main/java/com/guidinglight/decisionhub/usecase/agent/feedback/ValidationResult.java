package com.guidinglight.decisionhub.usecase.agent.feedback;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import java.util.Objects;

/**
 * Stage2-PoC-B2：NQ feedback envelope 校验结果。
 *
 * <p>仅承载校验本身的结论，不写入任何持久层；由 ingestion service 决定后续动作。
 */
public final class ValidationResult {

  private final boolean valid;
  private final IngestionErrorCode errorCode;
  private final String message;
  private final NqFeedbackEnvelope envelope;

  private ValidationResult(
      final boolean valid,
      final IngestionErrorCode errorCode,
      final String message,
      final NqFeedbackEnvelope envelope) {
    this.valid = valid;
    this.errorCode = errorCode;
    this.message = message;
    this.envelope = envelope;
  }

  /** 校验通过：返回已构造好的 envelope，避免上层重复构造。 */
  public static ValidationResult ok(final NqFeedbackEnvelope envelope) {
    return new ValidationResult(true, null, null, Objects.requireNonNull(envelope, "envelope"));
  }

  /** 校验失败：携带错误码与消息。 */
  public static ValidationResult fail(final IngestionErrorCode code, final String message) {
    return new ValidationResult(false, Objects.requireNonNull(code, "code"), message, null);
  }

  public boolean isValid() {
    return valid;
  }

  public IngestionErrorCode getErrorCode() {
    return errorCode;
  }

  public String getMessage() {
    return message;
  }

  /** 校验通过时返回 envelope；失败时为 null。 */
  public NqFeedbackEnvelope getEnvelope() {
    return envelope;
  }
}
