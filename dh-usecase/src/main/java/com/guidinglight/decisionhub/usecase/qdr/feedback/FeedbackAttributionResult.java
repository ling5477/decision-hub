package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionResult;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import java.util.Objects;

/**
 * Attribution use case 的稳定结构化结果。
 *
 * @param status 归因状态
 * @param errorCode 稳定错误分类
 * @param attributionResult 已完成 audit 的领域结果；pre-canonical 拒绝或端口失败时为空
 * @param safeMessage 已脱敏说明
 */
public record FeedbackAttributionResult(
    FeedbackStatus status,
    FeedbackAttributionErrorCode errorCode,
    AttributionResult attributionResult,
    String safeMessage) {

  /** 拒绝空状态、空错误码或超长说明。 */
  public FeedbackAttributionResult {
    status = Objects.requireNonNull(status, "status");
    errorCode = Objects.requireNonNull(errorCode, "errorCode");
    safeMessage = Objects.requireNonNull(safeMessage, "safeMessage").trim();
    if (safeMessage.isEmpty() || safeMessage.length() > 256) {
      throw new IllegalArgumentException("safeMessage must be within [1,256]");
    }
    if (attributionResult != null && attributionResult.status() != status) {
      throw new IllegalArgumentException("attributionResult status mismatch");
    }
    if (status == FeedbackStatus.ATTRIBUTED
        && (errorCode != FeedbackAttributionErrorCode.NONE || attributionResult == null)) {
      throw new IllegalArgumentException("ATTRIBUTED result requires audited domain result and NONE");
    }
  }

  /** 创建已完成审计的结构化结果。 */
  public static FeedbackAttributionResult completed(
      final AttributionResult result,
      final FeedbackAttributionErrorCode errorCode,
      final String safeMessage) {
    return new FeedbackAttributionResult(
        Objects.requireNonNull(result, "result").status(), errorCode, result, safeMessage);
  }

  /** 创建未形成领域结果的 fail-closed 拒绝。 */
  public static FeedbackAttributionResult rejected(
      final FeedbackAttributionErrorCode errorCode, final String safeMessage) {
    return new FeedbackAttributionResult(
        FeedbackStatus.REJECTED, errorCode, null, safeMessage);
  }
}
