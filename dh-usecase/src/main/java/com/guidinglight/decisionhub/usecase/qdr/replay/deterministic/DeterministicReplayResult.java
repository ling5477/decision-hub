package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotIdentity;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/** Structured deterministic replay result；只表示内部 reproducibility evidence。 */
public record DeterministicReplayResult(
    String tenantId,
    String snapshotId,
    String traceId,
    String requestId,
    String decisionId,
    UUID decisionRunId,
    String snapshotSchemaVersion,
    String canonicalizationVersion,
    String executorVersion,
    String canonicalInputHash,
    String baselineOutputHash,
    String replayOutputHash,
    ReplayReproducibilityStatus status,
    List<ReplayDifference> differences,
    ReplayFailureCode failureCode,
    String sanitizedMessage,
    String evidenceScope) {

  /** Result 中固定、不可解释为外部授权的安全声明。 */
  public static final String INTERNAL_EVIDENCE_ONLY =
      "internal reproducibility evidence only; not provider authorization; not NQ integration permission; not trading or execution permission; not Paper or LIVE permission";

  /** 校验成功/差异/失败状态互斥，并稳定排序 differences。 */
  public DeterministicReplayResult {
    tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
    snapshotId = ReplayPersistenceGuard.requireSafeText(snapshotId, "snapshotId");
    traceId = ReplayPersistenceGuard.requireSafeText(traceId, "traceId");
    requestId = ReplayPersistenceGuard.requireSafeText(requestId, "requestId");
    decisionId = ReplayPersistenceGuard.requireSafeText(decisionId, "decisionId");
    decisionRunId = ReplayPersistenceGuard.requireUuid(decisionRunId, "decisionRunId");
    snapshotSchemaVersion =
        ReplayPersistenceGuard.requireSafeText(snapshotSchemaVersion, "snapshotSchemaVersion");
    canonicalizationVersion =
        ReplayPersistenceGuard.requireSafeText(
            canonicalizationVersion, "canonicalizationVersion");
    executorVersion = ReplayPersistenceGuard.requireSafeText(executorVersion, "executorVersion");
    canonicalInputHash =
        ReplayPersistenceGuard.optionalSha256Hex(canonicalInputHash, "canonicalInputHash");
    baselineOutputHash =
        ReplayPersistenceGuard.optionalSha256Hex(baselineOutputHash, "baselineOutputHash");
    replayOutputHash =
        ReplayPersistenceGuard.optionalSha256Hex(replayOutputHash, "replayOutputHash");
    status = Objects.requireNonNull(status, "status");
    differences = sortedDifferences(differences);
    sanitizedMessage =
        ReplayPersistenceGuard.optionalSafeText(sanitizedMessage, "sanitizedMessage");
    evidenceScope = ReplayPersistenceGuard.requireSafeText(evidenceScope, "evidenceScope");
    if (!INTERNAL_EVIDENCE_ONLY.equals(evidenceScope)) {
      throw new IllegalArgumentException("evidenceScope must preserve the frozen authorization guard");
    }
    validateState(status, differences, failureCode, baselineOutputHash, replayOutputHash);
  }

  static DeterministicReplayResult completed(
      final DeterministicReplayCommand command,
      final String canonicalInputHash,
      final String baselineOutputHash,
      final String replayOutputHash,
      final List<ReplayDifference> differences) {
    final ReplayReproducibilityStatus status =
        differences.isEmpty()
            ? ReplayReproducibilityStatus.REPRODUCIBLE
            : ReplayReproducibilityStatus.DIFFERENT;
    return result(
        command,
        canonicalInputHash,
        baselineOutputHash,
        replayOutputHash,
        status,
        differences,
        null,
        status == ReplayReproducibilityStatus.REPRODUCIBLE
            ? "frozen mock replay output is reproducible"
            : "structured replay differences detected");
  }

  static DeterministicReplayResult failed(
      final DeterministicReplayCommand command,
      final ReplayFailureCode code,
      final String canonicalInputHash,
      final String message) {
    return result(
        command,
        canonicalInputHash,
        null,
        null,
        code.status(),
        List.of(),
        code,
        message);
  }

  private static DeterministicReplayResult result(
      final DeterministicReplayCommand command,
      final String canonicalInputHash,
      final String baselineOutputHash,
      final String replayOutputHash,
      final ReplayReproducibilityStatus status,
      final List<ReplayDifference> differences,
      final ReplayFailureCode failureCode,
      final String message) {
    final CanonicalReplaySnapshotIdentity identity = command.identity();
    return new DeterministicReplayResult(
        command.tenantId(),
        identity.snapshotId(),
        identity.correlation().traceId(),
        identity.correlation().requestId(),
        identity.correlation().decisionId(),
        identity.decisionRunId(),
        command.requiredSnapshotSchemaVersion(),
        command.requiredCanonicalizationVersion(),
        command.requestedExecutorVersion(),
        canonicalInputHash,
        baselineOutputHash,
        replayOutputHash,
        status,
        differences,
        failureCode,
        message,
        INTERNAL_EVIDENCE_ONLY);
  }

  private static List<ReplayDifference> sortedDifferences(
      final List<ReplayDifference> values) {
    final List<ReplayDifference> checked = values == null ? List.of() : values;
    return List.copyOf(checked.stream()
        .map(value -> Objects.requireNonNull(value, "difference"))
        .sorted(Comparator.comparing(ReplayDifference::type).thenComparing(ReplayDifference::path))
        .toList());
  }

  private static void validateState(
      final ReplayReproducibilityStatus status,
      final List<ReplayDifference> differences,
      final ReplayFailureCode failureCode,
      final String baselineOutputHash,
      final String replayOutputHash) {
    if (status == ReplayReproducibilityStatus.REPRODUCIBLE) {
      if (!differences.isEmpty()
          || failureCode != null
          || baselineOutputHash == null
          || !baselineOutputHash.equals(replayOutputHash)) {
        throw new IllegalArgumentException("REPRODUCIBLE requires equal output hashes and no differences");
      }
    } else if (status == ReplayReproducibilityStatus.DIFFERENT) {
      if (differences.isEmpty() || failureCode != null) {
        throw new IllegalArgumentException("DIFFERENT requires differences and no failure code");
      }
    } else if (failureCode == null || failureCode.status() != status || !differences.isEmpty()) {
      throw new IllegalArgumentException("failure result status must match failure code");
    }
  }
}
