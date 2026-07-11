package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHash;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHasher;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotIdentity;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotRecord;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotVersionVector;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotWriteCommand;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.Qdr6CanonicalJson;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.ReplayInputSnapshot;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** QDR6-MOCK-REPLAY-1 executor、reconstruction、hash 与 architecture boundary 回归。 */
class DeterministicReplayExecutorTest {

  private static final Instant CAPTURED = Instant.parse("2026-07-11T00:00:00Z");
  private static final String HASH_A = "a".repeat(64);
  private static final String HASH_B = "b".repeat(64);
  private static final String HASH_C = "c".repeat(64);
  private final Qdr6CanonicalJson canonicalJson = new Qdr6CanonicalJson();
  private final CanonicalReplaySnapshotHasher inputHasher =
      new CanonicalReplaySnapshotHasher(canonicalJson);

  @Test
  void samePersistedSnapshotRepeatedExecutionIsExactlyReproducibleAndReadOnly() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    final FakeSnapshotPort port = new FakeSnapshotPort(record);
    final DeterministicReplayExecutor executor = executor(port);

    final DeterministicReplayResult first = executor.execute(command(record));
    final DeterministicReplayResult second = executor.execute(command(record));

    assertEquals(first, second);
    assertEquals(ReplayReproducibilityStatus.REPRODUCIBLE, first.status());
    assertEquals(first.baselineOutputHash(), first.replayOutputHash());
    assertNotEquals(first.canonicalInputHash(), first.replayOutputHash());
    assertTrue(first.differences().isEmpty());
    assertNull(first.failureCode());
    assertEquals(2, port.reads.get());
    assertEquals(0, port.inserts.get());
    assertEquals(DeterministicReplayResult.INTERNAL_EVIDENCE_ONLY, first.evidenceScope());
  }

  @Test
  void persistedRecordReconstructsCanonicalSnapshotAndInputHashWithoutAuditFields() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    final ReplayInputSnapshot reconstructed = ReplayInputSnapshot.fromPersistedRecord(record);
    final CanonicalReplaySnapshotHash hash = inputHasher.hash(reconstructed);
    final ReplayInputSnapshot second = ReplayInputSnapshot.fromPersistedRecord(record);

    assertEquals(record.identity().tenantId(), reconstructed.tenantId());
    assertEquals(record.identity().decisionRunId(), reconstructed.decisionRunId());
    assertEquals(record.canonicalInputHash(), hash.lowercaseHex());
    assertEquals(reconstructed, second);
    assertArrayEquals(hash.canonicalBytes(), inputHasher.hash(second).canonicalBytes());
    assertFalse(reconstructed.canonicalValue().containsKey("createdAt"));
    assertFalse(reconstructed.canonicalValue().containsKey("id"));
  }

  @Test
  void commandRejectsTenantMismatchAndMovingVersionAliases() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DeterministicReplayCommand(
                "tenant-b",
                record.identity(),
                record.canonicalInputHash(),
                CanonicalReplaySnapshotVersionVector.SNAPSHOT_SCHEMA_VERSION,
                CanonicalReplaySnapshotVersionVector.CANONICALIZATION_VERSION,
                CanonicalReplaySnapshotVersionVector.REPLAY_EXECUTOR_VERSION));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new DeterministicReplayCommand(
                "tenant-a",
                record.identity(),
                record.canonicalInputHash(),
                "latest",
                CanonicalReplaySnapshotVersionVector.CANONICALIZATION_VERSION,
                CanonicalReplaySnapshotVersionVector.REPLAY_EXECUTOR_VERSION));
  }

  @Test
  void missingSnapshotAndPersistenceFailureAreStructuredAndNeverWrite() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    final FakeSnapshotPort missing = new FakeSnapshotPort(null);
    final DeterministicReplayResult incomplete = executor(missing).execute(command(record));
    assertFailure(
        incomplete,
        ReplayReproducibilityStatus.INCOMPLETE,
        ReplayFailureCode.SNAPSHOT_NOT_FOUND);

    final FakeSnapshotPort failed = new FakeSnapshotPort(record);
    failed.failRead = true;
    final DeterministicReplayResult executionFailed = executor(failed).execute(command(record));
    assertFailure(
        executionFailed,
        ReplayReproducibilityStatus.EXECUTION_FAILED,
        ReplayFailureCode.PERSISTENCE_READ_FAILED);
    assertEquals(0, missing.inserts.get());
    assertEquals(0, failed.inserts.get());
  }

  @Test
  void inputHashAndIdentityConflictsAreInvalidInput() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    final DeterministicReplayCommand wrongHash =
        new DeterministicReplayCommand(
            record.identity().tenantId(),
            record.identity(),
            HASH_B,
            CanonicalReplaySnapshotVersionVector.SNAPSHOT_SCHEMA_VERSION,
            CanonicalReplaySnapshotVersionVector.CANONICALIZATION_VERSION,
            CanonicalReplaySnapshotVersionVector.REPLAY_EXECUTOR_VERSION);
    assertFailure(
        executor(new FakeSnapshotPort(record)).execute(wrongHash),
        ReplayReproducibilityStatus.INVALID_INPUT,
        ReplayFailureCode.CANONICAL_HASH_MISMATCH);

    final CanonicalReplaySnapshotRecord other = record("tenant-b", "snapshot-other");
    assertFailure(
        executor(new FakeSnapshotPort(other)).execute(command(record)),
        ReplayReproducibilityStatus.INVALID_INPUT,
        ReplayFailureCode.TENANT_IDENTITY_MISMATCH);

    final CanonicalReplaySnapshotRecord otherCorrelation =
        record("tenant-a", "snapshot-1", "trace-2");
    assertFailure(
        executor(new FakeSnapshotPort(otherCorrelation)).execute(command(record)),
        ReplayReproducibilityStatus.INVALID_INPUT,
        ReplayFailureCode.CORRELATION_MISMATCH);
  }

  @Test
  void unsupportedSchemaCanonicalizationAndExecutorVersionsFailBeforeRead() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    final FakeSnapshotPort port = new FakeSnapshotPort(record);
    final DeterministicReplayCommand unsupportedSchema =
        new DeterministicReplayCommand(
            record.identity().tenantId(),
            record.identity(),
            record.canonicalInputHash(),
            "QDR6-REPLAY-INPUT-2",
            CanonicalReplaySnapshotVersionVector.CANONICALIZATION_VERSION,
            CanonicalReplaySnapshotVersionVector.REPLAY_EXECUTOR_VERSION);
    final DeterministicReplayCommand unsupportedCanonical =
        new DeterministicReplayCommand(
            record.identity().tenantId(),
            record.identity(),
            record.canonicalInputHash(),
            CanonicalReplaySnapshotVersionVector.SNAPSHOT_SCHEMA_VERSION,
            "QDR6-CJSON-2",
            CanonicalReplaySnapshotVersionVector.REPLAY_EXECUTOR_VERSION);
    final DeterministicReplayCommand unsupportedExecutor =
        new DeterministicReplayCommand(
            record.identity().tenantId(),
            record.identity(),
            record.canonicalInputHash(),
            CanonicalReplaySnapshotVersionVector.SNAPSHOT_SCHEMA_VERSION,
            CanonicalReplaySnapshotVersionVector.CANONICALIZATION_VERSION,
            "QDR6-MOCK-REPLAY-2");

    assertFailure(
        executor(port).execute(unsupportedSchema),
        ReplayReproducibilityStatus.UNSUPPORTED_VERSION,
        ReplayFailureCode.CANONICALIZATION_VERSION_UNSUPPORTED);
    assertFailure(
        executor(port).execute(unsupportedCanonical),
        ReplayReproducibilityStatus.UNSUPPORTED_VERSION,
        ReplayFailureCode.CANONICALIZATION_VERSION_UNSUPPORTED);
    assertFailure(
        executor(port).execute(unsupportedExecutor),
        ReplayReproducibilityStatus.UNSUPPORTED_VERSION,
        ReplayFailureCode.EXECUTOR_VERSION_UNSUPPORTED);
    assertEquals(0, port.reads.get());
  }

  @Test
  void transformationAndCanonicalizationExceptionsAreStructuredExecutionFailures() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    final FakeSnapshotPort port = new FakeSnapshotPort(record);
    final DeterministicReplayOutputHasher outputHasher =
        new DeterministicReplayOutputHasher(canonicalJson);
    final DeterministicReplayComparator comparator =
        new DeterministicReplayComparator(canonicalJson);
    final DeterministicReplayExecutor transformFailure =
        new DeterministicReplayExecutor(
            port,
            inputHasher,
            outputHasher,
            comparator,
            (snapshot, identity) -> {
              throw new IllegalStateException("local mock failure");
            });
    assertFailure(
        transformFailure.execute(command(record)),
        ReplayReproducibilityStatus.EXECUTION_FAILED,
        ReplayFailureCode.MOCK_TRANSFORMATION_FAILED);

    final DeterministicReplayExecutor canonicalFailure =
        new DeterministicReplayExecutor(
            port,
            inputHasher,
            outputHasher,
            comparator,
            (snapshot, identity) -> {
              final DeterministicReplayProjection valid =
                  DeterministicReplayProjection.fromSnapshot(snapshot, identity);
              return new DeterministicReplayProjection(
                  Map.of("unsupported", new Object()),
                  valid.policyVersion(),
                  valid.promptVersion(),
                  valid.modelVersion(),
                  valid.gatewayVersion(),
                  valid.expectedSummary(),
                  valid.evidenceRefs(),
                  valid.replayInputHash(),
                  valid.expectedSummaryHash(),
                  valid.providerSummaryHash());
            });
    assertFailure(
        canonicalFailure.execute(command(record)),
        ReplayReproducibilityStatus.EXECUTION_FAILED,
        ReplayFailureCode.OUTPUT_CANONICALIZATION_FAILED);
  }

  @Test
  void structuredMockDifferenceProducesDifferentResultWithAuditableTaxonomy() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    final DeterministicReplayOutputHasher outputHasher =
        new DeterministicReplayOutputHasher(canonicalJson);
    final DeterministicReplayExecutor changedExecutor =
        new DeterministicReplayExecutor(
            new FakeSnapshotPort(record),
            inputHasher,
            outputHasher,
            new DeterministicReplayComparator(canonicalJson),
            (snapshot, identity) -> {
              final DeterministicReplayProjection baseline =
                  DeterministicReplayProjection.fromSnapshot(snapshot, identity);
              return new DeterministicReplayProjection(
                  Map.of("snapshotId", "context-changed"),
                  baseline.policyVersion(),
                  baseline.promptVersion(),
                  baseline.modelVersion(),
                  baseline.gatewayVersion(),
                  baseline.expectedSummary(),
                  baseline.evidenceRefs(),
                  baseline.replayInputHash(),
                  baseline.expectedSummaryHash(),
                  baseline.providerSummaryHash());
            });

    final DeterministicReplayResult result = changedExecutor.execute(command(record));

    assertEquals(ReplayReproducibilityStatus.DIFFERENT, result.status());
    assertNull(result.failureCode());
    assertEquals(2, result.differences().size());
    assertEquals(ReplayDifferenceType.CONTEXT_DIFFERENCE, result.differences().get(0).type());
    assertEquals(ReplayDifferenceType.OUTPUT_HASH_DIFFERENCE, result.differences().get(1).type());
    assertNotEquals(result.baselineOutputHash(), result.replayOutputHash());
  }

  @Test
  void outputCanonicalBytesAndDomainHashAreStableAndIndependentFromEnvironment() {
    final CanonicalReplaySnapshotRecord record = record("tenant-a", "snapshot-1");
    final ReplayInputSnapshot snapshot = ReplayInputSnapshot.fromPersistedRecord(record);
    final DeterministicReplayProjection projection =
        DeterministicReplayProjection.fromSnapshot(snapshot, record.identity());
    final DeterministicReplayOutputHasher hasher =
        new DeterministicReplayOutputHasher(canonicalJson);
    final DeterministicReplayOutputHasher.OutputHash first =
        hasher.hash(projection, record.versionVector());
    final String unusedEnvironment = System.getenv("QDR6_UNUSED_TEST_VALUE");
    final DeterministicReplayOutputHasher.OutputHash second =
        hasher.hash(projection, record.versionVector());

    assertArrayEquals(first.canonicalBytes(), second.canonicalBytes());
    assertEquals(first.lowercaseHex(), second.lowercaseHex());
    assertEquals(64, first.lowercaseHex().length());
    assertNotEquals(record.canonicalInputHash(), first.lowercaseHex());
    assertTrue(unusedEnvironment == null || unusedEnvironment.length() >= 0);
  }

  @Test
  void failureTaxonomyHasOneFrozenStatusMapping() {
    final Map<ReplayFailureCode, ReplayReproducibilityStatus> expected =
        new EnumMap<>(ReplayFailureCode.class);
    expected.put(ReplayFailureCode.SNAPSHOT_NOT_FOUND, ReplayReproducibilityStatus.INCOMPLETE);
    expected.put(
        ReplayFailureCode.TENANT_IDENTITY_MISMATCH,
        ReplayReproducibilityStatus.INVALID_INPUT);
    expected.put(
        ReplayFailureCode.CORRELATION_MISMATCH, ReplayReproducibilityStatus.INVALID_INPUT);
    expected.put(
        ReplayFailureCode.CANONICAL_HASH_MISMATCH,
        ReplayReproducibilityStatus.INVALID_INPUT);
    expected.put(
        ReplayFailureCode.VERSION_VECTOR_INCOMPLETE,
        ReplayReproducibilityStatus.INCOMPLETE);
    expected.put(
        ReplayFailureCode.CANONICALIZATION_VERSION_UNSUPPORTED,
        ReplayReproducibilityStatus.UNSUPPORTED_VERSION);
    expected.put(
        ReplayFailureCode.EXECUTOR_VERSION_UNSUPPORTED,
        ReplayReproducibilityStatus.UNSUPPORTED_VERSION);
    expected.put(
        ReplayFailureCode.LEGACY_NOT_REPLAYABLE, ReplayReproducibilityStatus.INVALID_INPUT);
    expected.put(ReplayFailureCode.UNSAFE_INPUT, ReplayReproducibilityStatus.INVALID_INPUT);
    expected.put(ReplayFailureCode.BASELINE_INCOMPLETE, ReplayReproducibilityStatus.INCOMPLETE);
    expected.put(
        ReplayFailureCode.PERSISTENCE_READ_FAILED,
        ReplayReproducibilityStatus.EXECUTION_FAILED);
    expected.put(
        ReplayFailureCode.MOCK_TRANSFORMATION_FAILED,
        ReplayReproducibilityStatus.EXECUTION_FAILED);
    expected.put(
        ReplayFailureCode.OUTPUT_CANONICALIZATION_FAILED,
        ReplayReproducibilityStatus.EXECUTION_FAILED);

    assertEquals(expected.size(), ReplayFailureCode.values().length);
    expected.forEach((code, status) -> assertEquals(status, code.status()));
  }

  @Test
  void resultCarriesNoRawMaterialOrAuthorizationAndPackageHasNoExternalIoDependency()
      throws IOException {
    final DeterministicReplayResult result =
        executor(new FakeSnapshotPort(record("tenant-a", "snapshot-1")))
            .execute(command(record("tenant-a", "snapshot-1")));
    final String rendered = result.toString().toLowerCase(java.util.Locale.ROOT);
    assertFalse(rendered.contains("rawprompt"));
    assertFalse(rendered.contains("rawproviderresponse"));
    assertFalse(rendered.contains("credential"));
    assertTrue(result.evidenceScope().contains("not provider"));
    assertTrue(result.evidenceScope().contains("not NQ"));

    final Path sourceRoot =
        Path.of(
            "src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay/deterministic");
    try (Stream<Path> files = Files.walk(sourceRoot)) {
      final String source =
          files.filter(path -> path.toString().endsWith(".java"))
              .map(DeterministicReplayExecutorTest::read)
              .reduce("", (left, right) -> left + right);
      assertFalse(source.contains("java.net.http"));
      assertFalse(source.contains("WebClient"));
      assertFalse(source.contains("RestTemplate"));
      assertFalse(source.contains("com.guidinglight.decisionhub.providers"));
      assertFalse(source.contains("com.guidinglight.decisionhub.infra"));
      assertFalse(source.contains(".insert("));
      assertFalse(source.contains("Instant.now("));
      assertFalse(source.contains("currentTimeMillis("));
      assertFalse(source.contains("randomUUID("));
      assertFalse(source.contains("System.getenv("));
    }
  }

  private DeterministicReplayExecutor executor(final CanonicalReplaySnapshotPersistencePort port) {
    return new DeterministicReplayExecutor(port, inputHasher, canonicalJson);
  }

  private DeterministicReplayCommand command(final CanonicalReplaySnapshotRecord record) {
    return new DeterministicReplayCommand(
        record.identity().tenantId(),
        record.identity(),
        record.canonicalInputHash(),
        CanonicalReplaySnapshotVersionVector.SNAPSHOT_SCHEMA_VERSION,
        CanonicalReplaySnapshotVersionVector.CANONICALIZATION_VERSION,
        CanonicalReplaySnapshotVersionVector.REPLAY_EXECUTOR_VERSION);
  }

  private CanonicalReplaySnapshotRecord record(final String tenantId, final String snapshotId) {
    return record(tenantId, snapshotId, "trace-1");
  }

  private CanonicalReplaySnapshotRecord record(
      final String tenantId, final String snapshotId, final String traceId) {
    final DecisionEvidenceCorrelation correlation =
        new DecisionEvidenceCorrelation(tenantId, traceId, "request-1", "decision-1");
    final CanonicalReplaySnapshotIdentity identity = identity(correlation, snapshotId);
    final CanonicalReplaySnapshotVersionVector versions = versions();
    final ReplayInputSnapshot snapshot = snapshot(correlation, identity, versions);
    final CanonicalReplaySnapshotHash hash = inputHasher.hash(snapshot);
    final CanonicalReplaySnapshotWriteCommand command =
        new CanonicalReplaySnapshotWriteCommand(
            uuid(10),
            identity,
            snapshot.source(),
            snapshot.decisionType(),
            snapshot.sourceCapturedAt(),
            snapshot.subject(),
            snapshot.contextSnapshot(),
            snapshot.evidenceRefs(),
            snapshot.replayInputRef(),
            snapshot.expectedDecisionSummary(),
            versions,
            snapshot.replayInputHash(),
            snapshot.expectedSummaryHash(),
            snapshot.providerSummaryHash(),
            hash.lowercaseHex(),
            4096);
    return CanonicalReplaySnapshotRecord.persisted(command, CAPTURED.plusSeconds(30));
  }

  private static CanonicalReplaySnapshotIdentity identity(
      final DecisionEvidenceCorrelation correlation, final String snapshotId) {
    return new CanonicalReplaySnapshotIdentity(
        correlation,
        snapshotId,
        uuid(1),
        uuid(2),
        uuid(3),
        "call-1",
        uuid(4),
        uuid(5),
        uuid(6),
        "case-1",
        uuid(7),
        "evaluation-1",
        uuid(8),
        "verdict-1");
  }

  private static ReplayInputSnapshot snapshot(
      final DecisionEvidenceCorrelation correlation,
      final CanonicalReplaySnapshotIdentity identity,
      final CanonicalReplaySnapshotVersionVector versions) {
    return new ReplayInputSnapshot(
        versions.snapshotSchemaVersion(),
        correlation.tenantId(),
        correlation.traceId(),
        correlation.requestId(),
        correlation.decisionId(),
        identity.decisionRunId(),
        "TEST_SOURCE",
        "READ_ONLY_RECOMMENDATION",
        CAPTURED,
        new DecisionSubject("BTC-USDT", "SPOT", "1h", "strategy-1", "research-1"),
        new DecisionContextSnapshot("context-1", CAPTURED, List.of("evidence-1")),
        List.of(evidence(correlation)),
        versions.policyVersion(),
        versions.evaluationPolicyVersion(),
        versions.modelVersionRef(),
        versions.modelGatewayVersionRef(),
        versions.promptVersionRef(),
        new ReplayInputRef("SAFE_INPUT", "input-1", HASH_A),
        HASH_A,
        new ExpectedDecisionSummary(
            "READ_ONLY_RECOMMENDATION",
            "OBSERVE",
            "MEDIUM",
            RiskLevel.LOW,
            List.of("evidence-1"),
            List.of("PLACE_ORDER", "CANCEL_ORDER")),
        HASH_B,
        HASH_C,
        versions.replayExecutorVersion(),
        versions.canonicalizationVersion(),
        versions.hashAlgorithmVersion());
  }

  private static CanonicalReplaySnapshotVersionVector versions() {
    return new CanonicalReplaySnapshotVersionVector(
        "QDR6-REPLAY-INPUT-1",
        "DECISION-1",
        "QDR6-CONTEXT-1",
        "policy-1",
        "evaluation-policy-1",
        "prompt-1",
        HASH_A,
        "model-1",
        HASH_B,
        "gateway-1",
        "QDR6-CJSON-1",
        "QDR6-MOCK-REPLAY-1",
        "SHA-256");
  }

  private static DecisionEvidenceRef evidence(
      final DecisionEvidenceCorrelation correlation) {
    return new DecisionEvidenceRef(
        new DecisionEvidence("evidence-1", "REQUEST", "safe request ref"),
        correlation,
        DecisionEvidencePolicy.EvidenceType.REQUEST,
        "evidence-1",
        HASH_A,
        "V5",
        true,
        RedactionStatus.REDACTED);
  }

  private static void assertFailure(
      final DeterministicReplayResult result,
      final ReplayReproducibilityStatus status,
      final ReplayFailureCode failureCode) {
    assertEquals(status, result.status());
    assertEquals(failureCode, result.failureCode());
    assertTrue(result.differences().isEmpty());
  }

  private static String read(final Path path) {
    try {
      return Files.readString(path, StandardCharsets.UTF_8);
    } catch (final IOException error) {
      throw new IllegalStateException(error);
    }
  }

  private static UUID uuid(final long value) {
    return new UUID(0L, value);
  }

  private static final class FakeSnapshotPort
      implements CanonicalReplaySnapshotPersistencePort {

    private final CanonicalReplaySnapshotRecord record;
    private final AtomicInteger reads = new AtomicInteger();
    private final AtomicInteger inserts = new AtomicInteger();
    private boolean failRead;

    private FakeSnapshotPort(final CanonicalReplaySnapshotRecord record) {
      this.record = record;
    }

    @Override
    public CanonicalReplaySnapshotRecord insert(
        final String tenantId, final CanonicalReplaySnapshotWriteCommand command) {
      inserts.incrementAndGet();
      throw new AssertionError("deterministic replay must not write snapshot");
    }

    @Override
    public Optional<CanonicalReplaySnapshotRecord> findByTenantAndSnapshotId(
        final String tenantId, final String snapshotId) {
      throw new AssertionError("executor must use full identity exact read");
    }

    @Override
    public Optional<CanonicalReplaySnapshotRecord> findByTenantAndIdentity(
        final String tenantId,
        final CanonicalReplaySnapshotIdentity identity,
        final String snapshotSchemaVersion) {
      reads.incrementAndGet();
      if (failRead) {
        throw new IllegalStateException("database unavailable");
      }
      return Optional.ofNullable(record);
    }
  }
}
