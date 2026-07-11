package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHash;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHasher;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotRecord;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotVersionVector;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.Qdr6CanonicalJson;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.ReplayInputSnapshot;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * {@code QDR6-MOCK-REPLAY-1} 纯本地 deterministic replay executor。
 *
 * <p>唯一 IO 是现有 tenant-bound snapshot exact read；不读取 mutable source、不写数据库、不执行 prompt、
 * provider、policy runtime、NQ、Agent、LangGraph 或交易行为。
 */
public final class DeterministicReplayExecutor {

  private final CanonicalReplaySnapshotPersistencePort snapshotPort;
  private final CanonicalReplaySnapshotHasher inputHasher;
  private final DeterministicReplayOutputHasher outputHasher;
  private final DeterministicReplayComparator comparator;
  private final MockReplayTransformer transformer;

  /** 使用现有 snapshot port 与冻结 canonicalizer 创建 production-safe local executor。 */
  public DeterministicReplayExecutor(
      final CanonicalReplaySnapshotPersistencePort snapshotPort,
      final CanonicalReplaySnapshotHasher inputHasher,
      final Qdr6CanonicalJson canonicalJson) {
    this(
        snapshotPort,
        inputHasher,
        new DeterministicReplayOutputHasher(canonicalJson),
        new DeterministicReplayComparator(canonicalJson),
        DeterministicReplayProjection::fromSnapshot);
  }

  DeterministicReplayExecutor(
      final CanonicalReplaySnapshotPersistencePort snapshotPort,
      final CanonicalReplaySnapshotHasher inputHasher,
      final DeterministicReplayOutputHasher outputHasher,
      final DeterministicReplayComparator comparator,
      final MockReplayTransformer transformer) {
    this.snapshotPort = Objects.requireNonNull(snapshotPort, "snapshotPort");
    this.inputHasher = Objects.requireNonNull(inputHasher, "inputHasher");
    this.outputHasher = Objects.requireNonNull(outputHasher, "outputHasher");
    this.comparator = Objects.requireNonNull(comparator, "comparator");
    this.transformer = Objects.requireNonNull(transformer, "transformer");
  }

  /** 执行 fixed-order exact load、input verification、mock projection 与 structured comparison。 */
  public DeterministicReplayResult execute(final DeterministicReplayCommand command) {
    final DeterministicReplayCommand checked = Objects.requireNonNull(command, "command");
    final DeterministicReplayResult compatibilityFailure = compatibilityFailure(checked);
    if (compatibilityFailure != null) {
      return compatibilityFailure;
    }

    final Optional<CanonicalReplaySnapshotRecord> loaded;
    try {
      loaded =
          snapshotPort.findByTenantAndIdentity(
              checked.tenantId(), checked.identity(), checked.requiredSnapshotSchemaVersion());
    } catch (final RuntimeException error) {
      return failed(checked, ReplayFailureCode.PERSISTENCE_READ_FAILED, null, "snapshot read failed");
    }
    if (loaded == null || loaded.isEmpty()) {
      return failed(checked, ReplayFailureCode.SNAPSHOT_NOT_FOUND, null, "snapshot is missing");
    }
    final CanonicalReplaySnapshotRecord record = loaded.orElseThrow();
    if (!checked.tenantId().equals(record.identity().tenantId())) {
      return failed(
          checked,
          ReplayFailureCode.TENANT_IDENTITY_MISMATCH,
          null,
          "persisted snapshot identity mismatch");
    }
    if (!checked.identity().correlation().equals(record.identity().correlation())) {
      return failed(
          checked,
          ReplayFailureCode.CORRELATION_MISMATCH,
          null,
          "persisted snapshot correlation mismatch");
    }
    if (!checked.identity().equals(record.identity())) {
      return failed(
          checked,
          ReplayFailureCode.TENANT_IDENTITY_MISMATCH,
          null,
          "persisted snapshot identity mismatch");
    }
    final CanonicalReplaySnapshotVersionVector versions = record.versionVector();
    if (!supportedVersions(checked, versions)) {
      return failed(
          checked,
          ReplayFailureCode.CANONICALIZATION_VERSION_UNSUPPORTED,
          record.canonicalInputHash(),
          "persisted snapshot version is unsupported");
    }

    final ReplayInputSnapshot snapshot;
    final CanonicalReplaySnapshotHash inputHash;
    try {
      snapshot = ReplayInputSnapshot.fromPersistedRecord(record);
      inputHash = inputHasher.hash(snapshot);
    } catch (final IllegalArgumentException | NullPointerException error) {
      return failed(
          checked, ReplayFailureCode.UNSAFE_INPUT, null, "persisted snapshot reconstruction failed");
    } catch (final RuntimeException error) {
      return failed(
          checked,
          ReplayFailureCode.OUTPUT_CANONICALIZATION_FAILED,
          null,
          "canonical input verification failed");
    }
    if (!checked.expectedCanonicalInputHash().equals(record.canonicalInputHash())
        || !inputHash.lowercaseHex().equals(record.canonicalInputHash())) {
      return failed(
          checked,
          ReplayFailureCode.CANONICAL_HASH_MISMATCH,
          inputHash.lowercaseHex(),
          "canonical input hash mismatch");
    }

    final DeterministicReplayProjection baseline;
    try {
      baseline = DeterministicReplayProjection.fromSnapshot(snapshot, record.identity());
    } catch (final RuntimeException error) {
      return failed(
          checked,
          ReplayFailureCode.BASELINE_INCOMPLETE,
          inputHash.lowercaseHex(),
          "structured replay baseline is incomplete");
    }
    final DeterministicReplayProjection replay;
    try {
      replay = transformer.transform(snapshot, record.identity());
    } catch (final RuntimeException error) {
      return failed(
          checked,
          ReplayFailureCode.MOCK_TRANSFORMATION_FAILED,
          inputHash.lowercaseHex(),
          "local mock transformation failed");
    }

    try {
      final DeterministicReplayOutputHasher.OutputHash baselineHash =
          outputHasher.hash(baseline, versions);
      final DeterministicReplayOutputHasher.OutputHash replayHash = outputHasher.hash(replay, versions);
      if (inputHash.lowercaseHex().equals(replayHash.lowercaseHex())) {
        return failed(
            checked,
            ReplayFailureCode.OUTPUT_CANONICALIZATION_FAILED,
            inputHash.lowercaseHex(),
            "replay output hash must be domain-separated from input hash");
      }
      final List<ReplayDifference> differences =
          comparator.compare(
              baseline, replay, baselineHash.lowercaseHex(), replayHash.lowercaseHex());
      return DeterministicReplayResult.completed(
          checked,
          inputHash.lowercaseHex(),
          baselineHash.lowercaseHex(),
          replayHash.lowercaseHex(),
          differences);
    } catch (final RuntimeException error) {
      return failed(
          checked,
          ReplayFailureCode.OUTPUT_CANONICALIZATION_FAILED,
          inputHash.lowercaseHex(),
          "replay output canonicalization failed");
    }
  }

  private static DeterministicReplayResult compatibilityFailure(
      final DeterministicReplayCommand command) {
    if (!CanonicalReplaySnapshotVersionVector.REPLAY_EXECUTOR_VERSION.equals(
        command.requestedExecutorVersion())) {
      return failed(
          command,
          ReplayFailureCode.EXECUTOR_VERSION_UNSUPPORTED,
          null,
          "requested executor version is unsupported");
    }
    if (!CanonicalReplaySnapshotVersionVector.SNAPSHOT_SCHEMA_VERSION.equals(
            command.requiredSnapshotSchemaVersion())
        || !CanonicalReplaySnapshotVersionVector.CANONICALIZATION_VERSION.equals(
            command.requiredCanonicalizationVersion())) {
      return failed(
          command,
          ReplayFailureCode.CANONICALIZATION_VERSION_UNSUPPORTED,
          null,
          "required snapshot or canonicalization version is unsupported");
    }
    return null;
  }

  private static boolean supportedVersions(
      final DeterministicReplayCommand command,
      final CanonicalReplaySnapshotVersionVector versions) {
    return command.requiredSnapshotSchemaVersion().equals(versions.snapshotSchemaVersion())
        && command.requiredCanonicalizationVersion().equals(versions.canonicalizationVersion())
        && command.requestedExecutorVersion().equals(versions.replayExecutorVersion())
        && CanonicalReplaySnapshotVersionVector.HASH_ALGORITHM_VERSION.equals(
            versions.hashAlgorithmVersion());
  }

  private static DeterministicReplayResult failed(
      final DeterministicReplayCommand command,
      final ReplayFailureCode code,
      final String canonicalInputHash,
      final String message) {
    return DeterministicReplayResult.failed(command, code, canonicalInputHash, message);
  }

  @FunctionalInterface
  interface MockReplayTransformer {
    DeterministicReplayProjection transform(
        ReplayInputSnapshot snapshot,
        com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotIdentity identity);
  }
}
