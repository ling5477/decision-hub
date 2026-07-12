package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Instant;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Idempotency首次admission命令；只携带hash，不携带canonical/raw request。
 *
 * @param identity 完整guard identity。
 * @param requestId exact requestId。
 * @param requestHash lowercase SHA-256。
 * @param hashVersion 固定canonicalization版本。
 * @param expiresAt duplicate语义过期时间。
 * @param retentionUntil 最早物理清理时间。
 */
public record IdempotencyAdmissionCommand(
    PersistentGuardIdentity identity,
    String requestId,
    String requestHash,
    String hashVersion,
    Instant expiresAt,
    Instant retentionUntil) {

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
    expiresAt = Objects.requireNonNull(expiresAt, "expiresAt");
    retentionUntil = Objects.requireNonNull(retentionUntil, "retentionUntil");
    if (retentionUntil.isBefore(expiresAt)) {
      throw new IllegalArgumentException("retentionUntil must not precede expiresAt");
    }
  }

  private static String requireExact(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field);
    if (checked.isBlank() || !checked.equals(checked.trim())) {
      throw new IllegalArgumentException(field + " must be exact and non-blank");
    }
    return checked;
  }
}
