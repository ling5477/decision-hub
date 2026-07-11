package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayContextView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayOutputView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayRequestView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTimelineView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderKind;
import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfileStatus;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallTrustDecision;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRecord;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** CanonicalReplaySnapshotAssembler 的 source completeness/identity/safety 回归。 */
class CanonicalReplaySnapshotAssemblerTest {

  private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
  private static final String HASH_A = "a".repeat(64);
  private static final String HASH_B = "b".repeat(64);
  private final Qdr6CanonicalJson canonical = new Qdr6CanonicalJson();
  private final CanonicalReplaySnapshotAssembler assembler =
      new CanonicalReplaySnapshotAssembler(canonical);
  private final CanonicalReplaySnapshotHasher hasher =
      new CanonicalReplaySnapshotHasher(canonical);

  @Test
  void completeStructuredSourcesAssembleAndProducePostHashWriteCommand() {
    final CanonicalReplaySnapshotAssemblyRequest request = request();
    final CanonicalReplaySnapshotSources sources = sources("tenant-a", "case-1", safeSubject());
    final ReplayInputSnapshot snapshot = assembler.assemble(request, sources);
    final CanonicalReplaySnapshotHash hash = hasher.hash(snapshot);
    final CanonicalReplaySnapshotWriteCommand command =
        assembler.toWriteCommand(request, snapshot, hash, sources);

    assertEquals("tenant-a", snapshot.tenantId());
    assertEquals(HASH_A, snapshot.replayInputHash());
    assertEquals(HASH_A, snapshot.expectedSummaryHash());
    assertEquals(hash.lowercaseHex(), command.canonicalInputHash());
    assertEquals(request.identity(), command.identity());
  }

  @Test
  void legacyOrIncompleteV5SourceFailsClosed() {
    final CanonicalReplaySnapshotSources valid = sources("tenant-a", "case-1", safeSubject());
    final CanonicalReplaySnapshotSources legacy =
        new CanonicalReplaySnapshotSources(
            DecisionReplayView.incomplete(
                "tenant-a", "decision-1", "trace-1", "request-1", List.of("LEGACY")),
            valid.v6Run(),
            valid.v8Prompt(),
            valid.v8Model(),
            valid.v8GatewayCall(),
            valid.v9ReplayCase(),
            null,
            null,
            valid.evidenceAggregate());
    assertCode(
        CanonicalReplaySnapshotAssemblyException.Code.LEGACY_NOT_REPLAYABLE,
        () -> assembler.assemble(request(), legacy));
  }

  @Test
  void missingOrMovingVersionVectorFailsBeforeAssembly() {
    assertThrows(
        NullPointerException.class,
        () -> copyRequest(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> versionVector("latest"));
  }

  @Test
  void v5V6V8V9IdentityMismatchAndCrossTenantFailClosed() {
    assertCode(
        CanonicalReplaySnapshotAssemblyException.Code.IDENTITY_MISMATCH,
        () -> assembler.assemble(request(), sources("tenant-a", "case-other", safeSubject())));
    assertCode(
        CanonicalReplaySnapshotAssemblyException.Code.TENANT_MISMATCH,
        () -> assembler.assemble(request(), sources("tenant-b", "case-1", safeSubject())));
  }

  @Test
  void unsafeRawMaterialIsRejected() {
    final Map<String, Object> unsafe = new LinkedHashMap<>(safeSubject());
    unsafe.put("rawPrompt", "forbidden");
    assertCode(
        CanonicalReplaySnapshotAssemblyException.Code.UNSAFE_INPUT,
        () -> assembler.assemble(request(), sources("tenant-a", "case-1", unsafe)));
  }

  @Test
  void optionalLineageAbsenceRemainsAbsent() {
    final CanonicalReplaySnapshotAssemblyRequest request = request();
    final ReplayInputSnapshot snapshot =
        assembler.assemble(request, sources("tenant-a", "case-1", safeSubject()));
    assertNull(request.identity().evaluationCaseRowId());
    assertNull(request.identity().regressionVerdictRowId());
    assertEquals("case-1", snapshot.replayInputRef().refId());
  }

  @Test
  void sourceHashDriftBetweenAssemblyAndWriteIsRejected() {
    final CanonicalReplaySnapshotAssemblyRequest request = request();
    final CanonicalReplaySnapshotSources first = sources("tenant-a", "case-1", safeSubject());
    final ReplayInputSnapshot snapshot = assembler.assemble(request, first);
    final CanonicalReplaySnapshotSources changed = withExpectedHash(first, HASH_B);
    assertCode(
        CanonicalReplaySnapshotAssemblyException.Code.SOURCE_CHANGED,
        () -> assembler.toWriteCommand(request, snapshot, hasher.hash(snapshot), changed));
  }

  private static CanonicalReplaySnapshotAssemblyRequest request() {
    return copyRequest(versionVector("policy-1"));
  }

  private static CanonicalReplaySnapshotAssemblyRequest copyRequest(
      final CanonicalReplaySnapshotVersionVector versions) {
    return new CanonicalReplaySnapshotAssemblyRequest(
        uuid(10),
        "snapshot-1",
        "tenant-a",
        "trace-1",
        "request-1",
        "decision-1",
        uuid(1),
        uuid(2),
        uuid(8),
        "call-1",
        uuid(4),
        uuid(7),
        uuid(22),
        "case-1",
        null,
        null,
        null,
        null,
        null,
        DecisionEvidencePolicy.CORE_DECISION,
        versions);
  }

  private static CanonicalReplaySnapshotSources sources(
      final String tenant, final String caseId, final Map<String, Object> subject) {
    final DecisionReplayView v5 = foundView(tenant, subject);
    final DecisionRunDetailView v6 =
        new DecisionRunDetailView(
            uuid(1).toString(),
            uuid(2).toString(),
            tenant,
            "trace-1",
            "request-1",
            "request-key",
            "DECISION_REVIEW",
            "TEST_SOURCE",
            "source-ref",
            1,
            DecisionRunStatus.SUCCEEDED,
            NOW,
            NOW.plusSeconds(1),
            1L,
            null,
            null,
            "signal-summary",
            "decision-summary",
            "output-summary",
            NOW);
    final PromptVersionRecord prompt =
        new PromptVersionRecord(
            uuid(3),
            uuid(4),
            tenant,
            "template-1",
            "prompt-1",
            "render-policy",
            "prompt-ref",
            HASH_A,
            "redacted-summary",
            PromptVersionStatus.ACTIVE,
            HASH_A,
            NOW,
            "system");
    final ModelVersionRecord model =
        new ModelVersionRecord(
            uuid(5),
            uuid(7),
            uuid(6),
            tenant,
            ProviderKind.MOCK,
            "provider-key",
            "model-key",
            "display",
            "capability",
            8192,
            1024,
            ProviderProfileStatus.ENABLED,
            "trust-policy",
            "model-name",
            "model-1",
            HASH_A,
            NOW);
    final ModelGatewayCallRecord call =
        new ModelGatewayCallRecord(
            uuid(8),
            tenant,
            "trace-1",
            "request-1",
            uuid(2),
            uuid(4),
            uuid(7),
            uuid(6),
            ProviderKind.MOCK,
            "provider-ref",
            ModelGatewayCallStatus.SUCCEEDED,
            null,
            ModelGatewayCallTrustDecision.ALLOWED,
            "trust-ref",
            "call-1",
            "budget-safe",
            10,
            10,
            10,
            10,
            0,
            "input-summary",
            "output-summary",
            HASH_A,
            HASH_B,
            "audit-ref",
            "trace-ref",
            NOW);
    final ReplayCaseRecord replay = replayCase(tenant, caseId, HASH_A);
    return new CanonicalReplaySnapshotSources(
        v5, v6, prompt, model, call, replay, null, null, aggregate(tenant));
  }

  private static DecisionReplayView foundView(
      final String tenant, final Map<String, Object> subject) {
    return DecisionReplayView.found(
        new DecisionReplayRequestView(
            "decision-1",
            "request-1",
            "trace-1",
            tenant,
            "TEST_SOURCE",
            DecisionType.READ_ONLY_RECOMMENDATION,
            subject,
            "context-ref",
            NOW,
            "DECISION-1",
            NOW),
        new DecisionReplayContextView(
            "decision-1",
            tenant,
            "trace-1",
            Map.of(
                "snapshotId", "context-1",
                "capturedAt", NOW.toString(),
                "evidenceRefs", List.of("evidence-1")),
            List.of("evidence-1"),
            NOW),
        new DecisionReplayOutputView(
            "decision-1",
            tenant,
            "trace-1",
            "request-1",
            DecisionType.READ_ONLY_RECOMMENDATION,
            DecisionAction.NO_TRADE,
            DecisionRiskLevel.LOW,
            DecisionPolicyStatus.ALLOWED,
            new BigDecimal("0.5"),
            Map.of("action", "NO_TRADE"),
            NOW),
        DecisionReplayTimelineView.empty());
  }

  private static ReplayCaseRecord replayCase(
      final String tenant, final String caseId, final String expectedHash) {
    return new ReplayCaseRecord(
        uuid(22),
        tenant,
        caseId,
        "decision-1",
        "request-1",
        "trace-1",
        "request-1",
        "policy-1",
        "gateway-1",
        uuid(20),
        uuid(21),
        new ReplayInputRef("SAFE_INPUT", "case-1", HASH_A),
        new ExpectedDecisionSummary(
            "READ_ONLY_RECOMMENDATION",
            "OBSERVE",
            "MEDIUM",
            RiskLevel.LOW,
            List.of("evidence-1"),
            List.of("PLACE_ORDER", "CANCEL_ORDER")),
        expectedHash,
        HASH_A,
        NOW,
        NOW);
  }

  private static DecisionEvidenceAggregate aggregate(final String tenant) {
    final DecisionEvidenceCorrelation correlation =
        new DecisionEvidenceCorrelation(tenant, "trace-1", "request-1", "decision-1");
    return new DecisionEvidenceAggregate(
        correlation,
        List.of(
            new DecisionEvidenceRef(
                new DecisionEvidence("evidence-1", "REQUEST", "safe request ref"),
                correlation,
                DecisionEvidencePolicy.EvidenceType.REQUEST,
                "evidence-1",
                HASH_A,
                "V5",
                true,
                RedactionStatus.REDACTED)),
        DecisionEvidenceStatus.COMPLETE,
        List.of(),
        List.of());
  }

  private static CanonicalReplaySnapshotSources withExpectedHash(
      final CanonicalReplaySnapshotSources source, final String hash) {
    return new CanonicalReplaySnapshotSources(
        source.v5Replay(),
        source.v6Run(),
        source.v8Prompt(),
        source.v8Model(),
        source.v8GatewayCall(),
        replayCase(source.v9ReplayCase().tenantId(), source.v9ReplayCase().caseId(), hash),
        null,
        null,
        source.evidenceAggregate());
  }

  private static Map<String, Object> safeSubject() {
    return Map.of(
        "symbol", "BTC-USDT",
        "market", "SPOT",
        "timeframe", "1h",
        "strategyRef", "strategy-1");
  }

  private static CanonicalReplaySnapshotVersionVector versionVector(final String policy) {
    return new CanonicalReplaySnapshotVersionVector(
        "QDR6-REPLAY-INPUT-1",
        "DECISION-1",
        "QDR6-CONTEXT-1",
        policy,
        "evaluation-policy-1",
        "prompt-1",
        HASH_A,
        "model-1",
        HASH_A,
        "gateway-1",
        "QDR6-CJSON-1",
        "QDR6-MOCK-REPLAY-1",
        "SHA-256");
  }

  private static void assertCode(
      final CanonicalReplaySnapshotAssemblyException.Code expected,
      final org.junit.jupiter.api.function.Executable action) {
    assertEquals(
        expected,
        assertThrows(CanonicalReplaySnapshotAssemblyException.class, action).code());
  }

  private static UUID uuid(final long value) {
    return new UUID(0L, value);
  }
}
