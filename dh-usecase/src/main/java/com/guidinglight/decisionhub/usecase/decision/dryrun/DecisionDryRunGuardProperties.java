package com.guidinglight.decisionhub.usecase.decision.dryrun;

import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardHardCeilings;
import java.time.Duration;

/**
 * Stage-QDR-7 persistent guard配置快照。
 *
 * <p>当runtimeEnabled=true时所有字段必须显式配置且满足B1 hard ceiling；默认0只用于endpoint关闭状态，不能作为 production
 * fallback或最终容量默认值。
 *
 * @param runtimeEnabled protected runtime是否启用。
 * @param environment deployment identity。
 * @param rateWindowSeconds fixed window秒数。
 * @param rateLimitValue quota。
 * @param leaseDuration internal lease时长。
 * @param idempotencyTtl duplicate语义TTL。
 * @param retentionPeriod terminal retention。
 */
public record DecisionDryRunGuardProperties(
    boolean runtimeEnabled,
    String environment,
    int rateWindowSeconds,
    int rateLimitValue,
    Duration leaseDuration,
    Duration idempotencyTtl,
    Duration retentionPeriod) {

  /** 对启用状态执行严格启动校验；不选择最终生产容量数值。 */
  public DecisionDryRunGuardProperties {
    environment = environment == null ? "" : environment;
    leaseDuration = leaseDuration == null ? Duration.ZERO : leaseDuration;
    idempotencyTtl = idempotencyTtl == null ? Duration.ZERO : idempotencyTtl;
    retentionPeriod = retentionPeriod == null ? Duration.ZERO : retentionPeriod;
    if (runtimeEnabled) {
      if (!java.util.Set.of("dev", "test", "staging", "prod").contains(environment)) {
        throw new IllegalArgumentException("guard environment must be explicitly configured");
      }
      if (rateWindowSeconds <= 0
          || rateWindowSeconds > PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS) {
        throw new IllegalArgumentException(
            "guard rate window must be between 1 and "
                + PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS
                + " seconds");
      }
      if (rateLimitValue <= 0
          || rateLimitValue > PersistentGuardHardCeilings.MAX_RATE_QUOTA) {
        throw new IllegalArgumentException(
            "guard quota must be between 1 and "
                + PersistentGuardHardCeilings.MAX_RATE_QUOTA
                + " requests per window/key");
      }
      requirePositiveBounded(
          leaseDuration,
          Duration.ofSeconds(PersistentGuardHardCeilings.MAX_IDEMPOTENCY_LEASE_SECONDS),
          "leaseDuration");
      requirePositiveBounded(idempotencyTtl, Duration.ofDays(7), "idempotencyTtl");
      requirePositiveBounded(retentionPeriod, Duration.ofDays(90), "retentionPeriod");
      if (idempotencyTtl.compareTo(leaseDuration) <= 0) {
        throw new IllegalArgumentException("idempotencyTtl must exceed leaseDuration");
      }
      if (retentionPeriod.compareTo(idempotencyTtl) < 0) {
        throw new IllegalArgumentException("retentionPeriod must not precede idempotencyTtl");
      }
    }
  }

  private static void requirePositiveBounded(
      final Duration value, final Duration ceiling, final String field) {
    if (value.isZero() || value.isNegative() || value.compareTo(ceiling) > 0) {
      throw new IllegalArgumentException(
          field + " must be positive and at most " + ceiling.toSeconds() + " seconds");
    }
  }
}
