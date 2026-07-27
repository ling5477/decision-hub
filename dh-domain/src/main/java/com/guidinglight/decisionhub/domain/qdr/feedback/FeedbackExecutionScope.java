package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.util.Objects;

/**
 * Immutable, verified feedback execution authority for one tenant and isolated environment.
 *
 * <p>This value is created only at a verified root boundary after signature and joint authorization;
 * it intentionally carries no source, request material, headers, nonce, or credentials.
 *
 * @param tenantId verified tenant identity
 * @param environment verified DEV or TEST environment
 */
public record FeedbackExecutionScope(String tenantId, FeedbackEnvironment environment) {

  /** Validates the two authoritative scope attributes without applying a default. */
  public FeedbackExecutionScope {
    tenantId = Objects.requireNonNull(tenantId, "tenantId").trim();
    if (tenantId.isEmpty()) {
      throw new FeedbackExecutionScopeException("feedback execution tenant must not be blank");
    }
    environment = Objects.requireNonNull(environment, "environment");
  }
}
