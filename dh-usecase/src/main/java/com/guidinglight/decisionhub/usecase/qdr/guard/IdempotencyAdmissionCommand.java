package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Duration;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Idempotency首次admission命令；只携带hash，不携带canonical/raw request。
 *
 * @param identity 完整guard identity。
 * @param requestId exact requestId。
 * @param requestHash lowercase SHA-256。
 * @param hashVersion 固定canonicalization版本。
 * @param timeToLive duplicate语义TTL；绝对时间只能由PostgreSQL生成。
 * @param retentionPeriod terminal保留期；绝对时间只能由PostgreSQL生成。
 */
public record IdempotencyAdmissionCommand(
    PersistentGuardIdentity identity,
    String requestId,
    String requestHash,
    String hashVersion,
    Duration timeToLive,
    Duration retentionPeriod) {

  /** 固定hash版本。 */
  public static final String HASH_VERSION = "QDR7-DRYRUN-CJSON-1";

  private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

  /** 校验identity、hash和retention顺序。 */
  public IdempotencyAdmissionCommand {
    identity = Objects.requireNonNull(identity, "identity");
    requestId = requireExact(requestId, "requestId");
    if (!SHA_256.matcher(Objects.requireNonNull(requestHash, "requestHash")).matches()) {
      throw new IllegalArgumentException("requestHash must be lowercase SHA-256");
    }
    if (!HASH_VERSION.equals(hashVersion)) {
      throw new IllegalArgumentException("unsupported idempotency hashVersion");
    }
    timeToLive = requireDuration(timeToLive, Duration.ofDays(7), "timeToLive");
    retentionPeriod = requireDuration(retentionPeriod, Duration.ofDays(90), "retentionPeriod");
    if (retentionPeriod.compareTo(timeToLive) < 0) {
      throw new IllegalArgumentException("retentionPeriod must not precede timeToLive");
    }
  }

  private static String requireExact(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field);
    if (checked.isBlank() || !checked.equals(checked.trim())) {
      throw new IllegalArgumentException(field + " must be exact and non-blank");
    }
    return checked;
  }

  private static Duration requireDuration(
      final Duration value, final Duration ceiling, final String field) {
    final Duration checked = Objects.requireNonNull(value, field);
    if (checked.isZero() || checked.isNegative() || checked.compareTo(ceiling) > 0) {
      throw new IllegalArgumentException(field + " outside safety ceiling");
    }
    return checked;
  }
}
