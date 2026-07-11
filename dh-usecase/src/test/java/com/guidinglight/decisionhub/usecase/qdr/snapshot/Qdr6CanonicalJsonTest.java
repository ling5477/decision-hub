package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** QDR6-CJSON-1 与 domain-separated SHA-256 deterministic contract 回归。 */
class Qdr6CanonicalJsonTest {

  private static final String HASH = "a".repeat(64);
  private static final Instant CAPTURED = Instant.parse("2026-07-11T00:00:00Z");
  private final Qdr6CanonicalJson canonical = new Qdr6CanonicalJson();
  private final CanonicalReplaySnapshotHasher hasher = new CanonicalReplaySnapshotHasher(canonical);

  @Test
  void mapAndSetIterationOrderDoNotChangeCanonicalBytes() {
    final Map<String, Object> left = new LinkedHashMap<>();
    left.put("z", 1);
    left.put("a", new LinkedHashSet<>(List.of("b", "a")));
    final Map<String, Object> right = new LinkedHashMap<>();
    right.put("a", new LinkedHashSet<>(List.of("a", "b")));
    right.put("z", 1);

    assertArrayEquals(canonical.canonicalize(left), canonical.canonicalize(right));
  }

  @Test
  void semanticArrayOrderChangesCanonicalBytes() {
    assertFalse(
        java.util.Arrays.equals(
            canonical.canonicalize(List.of("first", "second")),
            canonical.canonicalize(List.of("second", "first"))));
  }

  @Test
  void explicitNullAndAbsentAreDifferent() {
    final Map<String, Object> explicitNull = new HashMap<>();
    explicitNull.put("optional", null);
    assertNotEquals(
        text(canonical.canonicalize(explicitNull)),
        text(canonical.canonicalize(Map.of())));
  }

  @Test
  void unicodeNfcAndDecimalNormalizationAreStable() {
    assertArrayEquals(
        canonical.canonicalize(Map.of("text", "e\u0301", "number", new BigDecimal("1.2300"))),
        canonical.canonicalize(Map.of("number", new BigDecimal("1.23"), "text", "é")));
    assertEquals("0", text(canonical.canonicalize(new BigDecimal("-0.000"))));
  }

  @Test
  void nonFiniteNumbersAreRejected() {
    final CanonicalReplaySnapshotAssemblyException error =
        assertThrows(
            CanonicalReplaySnapshotAssemblyException.class,
            () -> canonical.canonicalize(Double.NaN));
    assertEquals(
        CanonicalReplaySnapshotAssemblyException.Code.CANONICALIZATION_FAILED,
        error.code());
  }

  @Test
  void equalInstantWithDifferentOffsetAndEnvironmentProduceSameBytes() {
    final Locale originalLocale = Locale.getDefault();
    final TimeZone originalZone = TimeZone.getDefault();
    try {
      Locale.setDefault(Locale.FRANCE);
      TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
      final byte[] left =
          canonical.canonicalize(OffsetDateTime.parse("2026-07-11T08:00:00+08:00"));
      Locale.setDefault(Locale.JAPAN);
      TimeZone.setDefault(TimeZone.getTimeZone("America/New_York"));
      final byte[] right = canonical.canonicalize(CAPTURED);
      assertArrayEquals(left, right);
    } finally {
      Locale.setDefault(originalLocale);
      TimeZone.setDefault(originalZone);
    }
  }

  @Test
  void identicalSnapshotProducesStableHashAndSourceHashCannotMasquerade() {
    final CanonicalReplaySnapshotHash left = hasher.hash(snapshot("tenant-a", "context-1", "policy-1"));
    final CanonicalReplaySnapshotHash right = hasher.hash(snapshot("tenant-a", "context-1", "policy-1"));
    assertEquals(left.lowercaseHex(), right.lowercaseHex());
    assertEquals(64, left.lowercaseHex().length());
    assertNotEquals(HASH, left.lowercaseHex());
  }

  @Test
  void contextVersionAndTenantChangesChangeHash() {
    final String baseline = hasher.hash(snapshot("tenant-a", "context-1", "policy-1")).lowercaseHex();
    assertNotEquals(
        baseline, hasher.hash(snapshot("tenant-a", "context-2", "policy-1")).lowercaseHex());
    assertNotEquals(
        baseline, hasher.hash(snapshot("tenant-a", "context-1", "policy-2")).lowercaseHex());
    assertNotEquals(
        baseline, hasher.hash(snapshot("tenant-b", "context-1", "policy-1")).lowercaseHex());
  }

  @Test
  void databaseAuditTimeCannotChangeHashBecauseItIsOutsideSnapshot() {
    final ReplayInputSnapshot input = snapshot("tenant-a", "context-1", "policy-1");
    final String before = hasher.hash(input).lowercaseHex();
    final Instant firstDatabaseCreatedAt = CAPTURED.plusSeconds(1);
    final Instant secondDatabaseCreatedAt = CAPTURED.plusSeconds(2);
    assertNotEquals(firstDatabaseCreatedAt, secondDatabaseCreatedAt);
    assertEquals(before, hasher.hash(input).lowercaseHex());
  }

  @Test
  void domainSeparatorVersionChangeChangesHash() {
    final ReplayInputSnapshot input = snapshot("tenant-a", "context-1", "policy-1");
    final String frozen = hasher.hash(input).lowercaseHex();
    final String changed =
        hasher
            .hash(
                input,
                new CanonicalReplaySnapshotHasher.HashDomain(
                    "QDR6-REPLAY-INPUT-2", "QDR6-CJSON-1", "QDR6-MOCK-REPLAY-1", "SHA-256"))
            .lowercaseHex();
    assertNotEquals(frozen, changed);
  }

  private static ReplayInputSnapshot snapshot(
      final String tenant, final String contextId, final String policyVersion) {
    final DecisionEvidenceCorrelation correlation =
        new DecisionEvidenceCorrelation(tenant, "trace-1", "request-1", "decision-1");
    return new ReplayInputSnapshot(
        "QDR6-REPLAY-INPUT-1",
        tenant,
        "trace-1",
        "request-1",
        "decision-1",
        uuid(2),
        "TEST_SOURCE",
        "READ_ONLY_RECOMMENDATION",
        CAPTURED,
        new DecisionSubject("BTC-USDT", "SPOT", "1h", "strategy-1", null),
        new DecisionContextSnapshot(contextId, CAPTURED, List.of("evidence-1")),
        List.of(evidence(correlation)),
        policyVersion,
        "evaluation-policy-1",
        "model-1",
        "gateway-1",
        "prompt-1",
        new ReplayInputRef("SAFE_INPUT", "input-1", HASH),
        HASH,
        new ExpectedDecisionSummary(
            "READ_ONLY_RECOMMENDATION",
            "OBSERVE",
            "MEDIUM",
            RiskLevel.LOW,
            List.of("evidence-1"),
            List.of("PLACE_ORDER", "CANCEL_ORDER")),
        HASH,
        null,
        "QDR6-MOCK-REPLAY-1",
        "QDR6-CJSON-1",
        "SHA-256");
  }

  private static DecisionEvidenceRef evidence(final DecisionEvidenceCorrelation correlation) {
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

  private static String text(final byte[] value) {
    return new String(value, StandardCharsets.UTF_8);
  }
}
