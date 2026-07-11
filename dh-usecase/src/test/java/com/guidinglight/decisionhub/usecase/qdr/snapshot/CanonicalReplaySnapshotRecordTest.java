package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Canonical replay snapshot P1 immutable persistence contracts 回归测试。 */
class CanonicalReplaySnapshotRecordTest {

    private static final Instant CAPTURED_AT = Instant.parse("2026-07-11T00:00:00Z");
    private static final String HASH = "a".repeat(64);

    @Test
    void createsTenantBoundImmutableStructuredRecord() {
        final CanonicalReplaySnapshotRecord record = validRecord();

        assertEquals("tenant-a", record.identity().tenantId());
        assertEquals(
                CanonicalReplaySnapshotVersionVector.SNAPSHOT_SCHEMA_VERSION,
                record.versionVector().snapshotSchemaVersion());
        assertEquals(1, record.evidenceRefs().size());
        assertThrows(UnsupportedOperationException.class, () -> record.evidenceRefs().clear());
        assertEquals("READ_ONLY_RECOMMENDATION", record.decisionType());
    }

    @Test
    void rejectsMovingVersionAlias() {
        final IllegalArgumentException error =
                assertThrows(IllegalArgumentException.class, () -> versionVector("latest"));
        assertTrue(error.getMessage().contains("moving alias"));
    }

    @Test
    void rejectsUnpairedOptionalLineageIdentity() {
        final DecisionEvidenceCorrelation correlation = correlation();

    final IllegalArgumentException error =
        assertThrows(
                IllegalArgumentException.class,
            () ->
                new CanonicalReplaySnapshotIdentity(
                        correlation,
                        "snapshot-1",
                        uuid(1),
                        uuid(2),
                        uuid(3),
                        "call-1",
                        uuid(4),
                        uuid(5),
                        uuid(6),
                        "case-1",
                        uuid(7),
                        null,
                        null,
                        null));
        assertTrue(error.getMessage().contains("both present or absent"));
    }

    @Test
    void rejectsEvidenceFromDifferentTenant() {
        final CanonicalReplaySnapshotRecord valid = validRecord();
        final DecisionEvidenceCorrelation other =
                new DecisionEvidenceCorrelation("tenant-b", "trace-1", "request-1", "decision-1");
        final DecisionEvidenceRef mismatched = evidenceRef(other);

    final IllegalArgumentException error =
        assertThrows(
                IllegalArgumentException.class,
            () ->
                new CanonicalReplaySnapshotRecord(
                        valid.id(),
                        valid.identity(),
                        valid.source(),
                        valid.decisionType(),
                        valid.sourceCapturedAt(),
                        valid.subject(),
                        valid.contextSnapshot(),
                        List.of(mismatched),
                        valid.replayInputRef(),
                        valid.expectedDecisionSummary(),
                        valid.versionVector(),
                        valid.replayInputHash(),
                        valid.expectedSummaryHash(),
                        valid.providerSummaryHash(),
                        valid.canonicalInputHash(),
                        valid.payloadBytes(),
                        valid.createdAt()));
        assertTrue(error.getMessage().contains("correlation"));
    }

    @Test
    void rejectsUnsafeSubjectAndExecutableDecisionType() {
        final CanonicalReplaySnapshotRecord valid = validRecord();

        assertThrows(
                IllegalArgumentException.class,
        () ->
            new CanonicalReplaySnapshotRecord(
                        valid.id(),
                        valid.identity(),
                        valid.source(),
                        "BUY",
                        valid.sourceCapturedAt(),
                        new DecisionSubject("secret token", "SPOT", "1h", null, null),
                        valid.contextSnapshot(),
                        valid.evidenceRefs(),
                        valid.replayInputRef(),
                        valid.expectedDecisionSummary(),
                        valid.versionVector(),
                        valid.replayInputHash(),
                        valid.expectedSummaryHash(),
                        valid.providerSummaryHash(),
                        valid.canonicalInputHash(),
                        valid.payloadBytes(),
                        valid.createdAt()));
    }

    @Test
    void rejectsPayloadOverFrozenLimit() {
        final CanonicalReplaySnapshotRecord valid = validRecord();

    final IllegalArgumentException error =
        assertThrows(
                IllegalArgumentException.class,
            () ->
                new CanonicalReplaySnapshotRecord(
                        valid.id(),
                        valid.identity(),
                        valid.source(),
                        valid.decisionType(),
                        valid.sourceCapturedAt(),
                        valid.subject(),
                        valid.contextSnapshot(),
                        valid.evidenceRefs(),
                        valid.replayInputRef(),
                        valid.expectedDecisionSummary(),
                        valid.versionVector(),
                        valid.replayInputHash(),
                        valid.expectedSummaryHash(),
                        valid.providerSummaryHash(),
                        valid.canonicalInputHash(),
                        262_145,
                        valid.createdAt()));
        assertTrue(error.getMessage().contains("262144"));
    }

  @Test
  void persistencePortExposesOnlyTenantBoundAppendAndExactReads() {
    final Set<String> methods =
        Arrays.stream(CanonicalReplaySnapshotPersistencePort.class.getDeclaredMethods())
            .map(method -> method.getName())
            .collect(java.util.stream.Collectors.toSet());

    assertEquals(Set.of("insert", "findByTenantAndSnapshotId", "findByTenantAndIdentity"), methods);
    assertTrue(
        methods.stream()
            .noneMatch(
                name ->
                    name.matches("(?i).*(update|delete|latest|first|list|all|scan|fallback).*")));
  }

  @Test
  void promptAndGatewayPortsExposeExactTenantBoundLookupsWithoutFallbacks() {
    final Set<String> promptMethods =
        Arrays.stream(PromptVersionPersistencePort.class.getDeclaredMethods())
            .map(method -> method.getName())
            .collect(java.util.stream.Collectors.toSet());
    final Set<String> gatewayMethods =
        Arrays.stream(ModelGatewayCallPersistencePort.class.getDeclaredMethods())
            .map(method -> method.getName())
            .collect(java.util.stream.Collectors.toSet());

    assertTrue(promptMethods.contains("findByTenantAndPromptVersionId"));
    assertTrue(gatewayMethods.contains("findByTenantAndDecisionRunAndModelCallRef"));
    assertTrue(
        promptMethods.stream()
            .noneMatch(name -> name.matches("(?i).*(latest|active|fallback|tenantless).*")));
    assertTrue(
        gatewayMethods.stream()
            .noneMatch(
                name -> name.matches("(?i).*(trace|provider|latest|fallback|tenantless).*")));
  }

  @Test
  void writeCommandRequiresCompletedCanonicalHashAndExcludesDatabaseAuditTime() {
    final Set<String> components =
        Arrays.stream(CanonicalReplaySnapshotWriteCommand.class.getRecordComponents())
            .map(component -> component.getName().toLowerCase(Locale.ROOT))
            .collect(java.util.stream.Collectors.toSet());

    assertTrue(components.contains("canonicalinputhash"));
    assertTrue(components.stream().noneMatch(name -> name.contains("createdat")));
    assertThrows(NullPointerException.class, () -> commandWithCanonicalHash(null));
    assertThrows(IllegalArgumentException.class, () -> commandWithCanonicalHash("placeholder"));
    assertThrows(IllegalArgumentException.class, () -> commandWithCanonicalHash("default"));
    assertThrows(IllegalArgumentException.class, () -> commandWithCanonicalHash("latest"));
    assertThrows(IllegalArgumentException.class, () -> commandWithCanonicalHash("0".repeat(64)));
  }

  @Test
  void writeCommandRejectsReplayInputHashDifferentFromStructuredRef() {
    final CanonicalReplaySnapshotWriteCommand valid = validCommand();

    final IllegalArgumentException error =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                new CanonicalReplaySnapshotWriteCommand(
                    valid.id(),
                    valid.identity(),
                    valid.source(),
                    valid.decisionType(),
                    valid.sourceCapturedAt(),
                    valid.subject(),
                    valid.contextSnapshot(),
                    valid.evidenceRefs(),
                    valid.replayInputRef(),
                    valid.expectedDecisionSummary(),
                    valid.versionVector(),
                    "b".repeat(64),
                    valid.expectedSummaryHash(),
                    valid.providerSummaryHash(),
                    valid.canonicalInputHash(),
                    valid.payloadBytes()));
    assertTrue(error.getMessage().contains("replayInputHash"));
  }

    private static CanonicalReplaySnapshotRecord validRecord() {
        return CanonicalReplaySnapshotRecord.persisted(
                validCommand(), CAPTURED_AT.plusSeconds(1));
    }

    private static CanonicalReplaySnapshotWriteCommand validCommand() {
        return commandWithCanonicalHash(HASH);
    }

    private static CanonicalReplaySnapshotWriteCommand commandWithCanonicalHash(
            final String canonicalInputHash) {
        final DecisionEvidenceCorrelation correlation = correlation();
    final CanonicalReplaySnapshotIdentity identity =
        new CanonicalReplaySnapshotIdentity(
                correlation,
                "snapshot-1",
                uuid(1),
                uuid(2),
                uuid(3),
                "call-1",
                uuid(4),
                uuid(5),
                uuid(6),
                "case-1",
                null,
                null,
                null,
                null);
        return new CanonicalReplaySnapshotWriteCommand(
                uuid(10),
                identity,
                "TEST_SOURCE",
                "READ_ONLY_RECOMMENDATION",
                CAPTURED_AT,
                new DecisionSubject("BTC-USDT", "SPOT", "1h", "strategy-1", "research-1"),
                new DecisionContextSnapshot("context-1", CAPTURED_AT, List.of("evidence-1")),
                List.of(evidenceRef(correlation)),
                new ReplayInputRef("SAFE_INPUT", "input-1", HASH),
                new ExpectedDecisionSummary(
                        "READ_ONLY_RECOMMENDATION",
                        "OBSERVE",
                        "MEDIUM",
                        RiskLevel.LOW,
                        List.of("evidence-1"),
                        List.of("PLACE_ORDER")),
                versionVector("policy-1"),
                HASH,
                HASH,
                null,
                canonicalInputHash,
                4096);
    }

    private static CanonicalReplaySnapshotVersionVector versionVector(final String policyVersion) {
        return new CanonicalReplaySnapshotVersionVector(
                "QDR6-REPLAY-INPUT-1",
                "DECISION-1",
                "QDR6-CONTEXT-1",
                policyVersion,
                "evaluation-policy-1",
                "prompt-1",
                HASH,
                "model-1",
                HASH,
                "gateway-1",
                "QDR6-CJSON-1",
                "QDR6-MOCK-REPLAY-1",
                "SHA-256");
    }

    private static DecisionEvidenceCorrelation correlation() {
        return new DecisionEvidenceCorrelation("tenant-a", "trace-1", "request-1", "decision-1");
    }

    private static DecisionEvidenceRef evidenceRef(final DecisionEvidenceCorrelation correlation) {
        return new DecisionEvidenceRef(
                new DecisionEvidence("evidence-1", "REQUEST", "safe request ref"),
                correlation,
                DecisionEvidencePolicy.EvidenceType.REQUEST,
                "evidence-1",
                HASH,
                "V5",
                true,
                RedactionStatus.REDACTED);
    }

    private static UUID uuid(final long value) {
        return new UUID(0L, value);
    }
}
