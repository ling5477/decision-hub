package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.time.Duration;
import java.util.Objects;

/** Internal-only, one-scope retention request with frozen safety defaults and hard limits. */
public record FeedbackRetentionCommand(
    String tenantId,
    FeedbackEnvironment environment,
    boolean enabled,
    Duration retentionAge,
    int batchSize) {

  /** Disabled-by-default retention period. */
  public static final Duration DEFAULT_RETENTION_AGE = Duration.ofDays(365);
  /** Maximum aggregates one invocation may inspect or delete. */
  public static final int MAX_BATCH_SIZE = 100;

  /** Validates one explicit tenant/environment command before it can reach a persistence port. */
  public FeedbackRetentionCommand {
    tenantId = requireTenant(tenantId);
    environment = Objects.requireNonNull(environment, "environment");
    retentionAge = Objects.requireNonNull(retentionAge, "retentionAge");
    if (retentionAge.isZero() || retentionAge.isNegative()) {
      throw failure(FeedbackPersistenceErrorCode.INVALID_RETENTION_AGE, "retention age must be positive");
    }
    if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
      throw failure(FeedbackPersistenceErrorCode.INVALID_BATCH_SIZE, "retention batch must be within 1..100");
    }
  }

  /** Returns the only default command: disabled, 365 days, and a bounded batch of 100. */
  public static FeedbackRetentionCommand defaults(
      final String tenantId, final FeedbackEnvironment environment) {
    return new FeedbackRetentionCommand(
        tenantId, environment, false, DEFAULT_RETENTION_AGE, MAX_BATCH_SIZE);
  }

  private static String requireTenant(final String value) {
    final String checked = Objects.requireNonNull(value, "tenantId").trim();
    if (checked.isEmpty() || checked.length() > 128 || "*".equals(checked)) {
      throw failure(FeedbackPersistenceErrorCode.INVALID_RETENTION_SCOPE, "retention tenant scope is invalid");
    }
    return checked;
  }

  private static FeedbackPersistenceException failure(
      final FeedbackPersistenceErrorCode code, final String message) {
    return new FeedbackPersistenceException(code, message);
  }
}
