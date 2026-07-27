package com.guidinglight.decisionhub.domain.qdr.feedback;

/** Raised when a trusted feedback execution scope cannot be formed fail-closed. */
public final class FeedbackExecutionScopeException extends IllegalArgumentException {

  /** Creates a scope validation failure without exposing request material. */
  public FeedbackExecutionScopeException(final String message) {
    super(message);
  }

  /** Creates a scope validation failure with its local validation cause. */
  public FeedbackExecutionScopeException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
