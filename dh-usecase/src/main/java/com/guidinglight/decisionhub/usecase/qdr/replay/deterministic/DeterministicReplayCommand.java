package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotIdentity;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Tenant-bound deterministic replay 的 selector/compatibility command；不携带 snapshot payload。 */
public record DeterministicReplayCommand(
    String tenantId,
    CanonicalReplaySnapshotIdentity identity,
    String expectedCanonicalInputHash,
    String requiredSnapshotSchemaVersion,
    String requiredCanonicalizationVersion,
    String requestedExecutorVersion) {

  private static final Set<String> MOVING_ALIASES = Set.of("latest", "current", "default");

  /** 校验完整 tenant identity、expected hash 与 immutable compatibility versions。 */
  public DeterministicReplayCommand {
    tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
    identity = Objects.requireNonNull(identity, "identity");
    if (!tenantId.equals(identity.tenantId())) {
      throw new IllegalArgumentException("tenantId must match snapshot identity tenant");
    }
    expectedCanonicalInputHash =
        ReplayPersistenceGuard.requireSha256Hex(
            expectedCanonicalInputHash, "expectedCanonicalInputHash");
    requiredSnapshotSchemaVersion =
        immutableVersion(requiredSnapshotSchemaVersion, "requiredSnapshotSchemaVersion");
    requiredCanonicalizationVersion =
        immutableVersion(requiredCanonicalizationVersion, "requiredCanonicalizationVersion");
    requestedExecutorVersion = immutableVersion(requestedExecutorVersion, "requestedExecutorVersion");
  }

  private static String immutableVersion(final String value, final String field) {
    final String checked = ReplayPersistenceGuard.requireSafeText(value, field);
    if (MOVING_ALIASES.contains(checked.toLowerCase(Locale.ROOT))) {
      throw new IllegalArgumentException(field + " must not use a moving alias");
    }
    return checked;
  }
}
