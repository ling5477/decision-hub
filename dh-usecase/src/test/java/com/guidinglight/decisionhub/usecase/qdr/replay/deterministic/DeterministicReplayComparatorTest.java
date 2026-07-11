package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.usecase.qdr.snapshot.Qdr6CanonicalJson;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/** Frozen structured comparator taxonomy、sorting 与 result invariants 回归。 */
class DeterministicReplayComparatorTest {

  private static final String HASH_A = "a".repeat(64);
  private static final String HASH_B = "b".repeat(64);
  private final DeterministicReplayComparator comparator =
      new DeterministicReplayComparator(new Qdr6CanonicalJson());

  @Test
  void identicalProjectionHasNoDifferenceAndChangedFieldsUseOnlyFrozenTaxonomy() {
    final DeterministicReplayProjection baseline = projection();
    assertTrue(comparator.compare(baseline, baseline, HASH_A, HASH_A).isEmpty());

    final DeterministicReplayProjection changed =
        new DeterministicReplayProjection(
            Map.of("snapshotId", "context-2"),
            "policy-2",
            "prompt-2",
            "model-2",
            "gateway-2",
            Map.of("actionLabel", "WAIT"),
            List.of(Map.of("refId", "evidence-2")),
            baseline.replayInputHash(),
            baseline.expectedSummaryHash(),
            baseline.providerSummaryHash());
    final List<ReplayDifference> differences =
        comparator.compare(baseline, changed, HASH_A, HASH_B);

    assertEquals(
        Set.of(
            ReplayDifferenceType.CONTEXT_DIFFERENCE,
            ReplayDifferenceType.POLICY_VERSION_DIFFERENCE,
            ReplayDifferenceType.PROMPT_VERSION_DIFFERENCE,
            ReplayDifferenceType.MODEL_VERSION_DIFFERENCE,
            ReplayDifferenceType.GATEWAY_VERSION_DIFFERENCE,
            ReplayDifferenceType.EXPECTED_SUMMARY_DIFFERENCE,
            ReplayDifferenceType.EVIDENCE_DIFFERENCE,
            ReplayDifferenceType.OUTPUT_HASH_DIFFERENCE),
        differences.stream().map(ReplayDifference::type).collect(Collectors.toSet()));
    assertEquals(
        differences.stream()
            .sorted(Comparator.comparing(ReplayDifference::type).thenComparing(ReplayDifference::path))
            .toList(),
        differences);
    assertTrue(
        differences.stream()
            .filter(value -> value.type() == ReplayDifferenceType.CONTEXT_DIFFERENCE)
            .allMatch(value -> value.expectedHashOrRef().matches("[0-9a-f]{64}")));
  }

  @Test
  void missingProjectionUsesOnlyMissingRequiredInput() {
    final List<ReplayDifference> differences =
        comparator.compare(null, projection(), HASH_A, HASH_A);
    assertEquals(1, differences.size());
    assertEquals(ReplayDifferenceType.MISSING_REQUIRED_INPUT, differences.getFirst().type());
  }

  @Test
  void reproducibleAndDifferentResultInvariantsAreFailClosed() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            result(
                ReplayReproducibilityStatus.REPRODUCIBLE,
                List.of(
                    new ReplayDifference(
                        ReplayDifferenceType.CONTEXT_DIFFERENCE,
                        "context",
                        HASH_A,
                        HASH_B,
                        "structured context differs")),
                null,
                HASH_A,
                HASH_A));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            result(
                ReplayReproducibilityStatus.DIFFERENT,
                List.of(),
                null,
                HASH_A,
                HASH_B));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            result(
                ReplayReproducibilityStatus.EXECUTION_FAILED,
                List.of(),
                ReplayFailureCode.CANONICAL_HASH_MISMATCH,
                null,
                null));
  }

  @Test
  void differenceTaxonomyCannotSilentlyGrow() {
    assertEquals(
        Set.of(
            "CONTEXT_DIFFERENCE",
            "POLICY_VERSION_DIFFERENCE",
            "PROMPT_VERSION_DIFFERENCE",
            "MODEL_VERSION_DIFFERENCE",
            "GATEWAY_VERSION_DIFFERENCE",
            "EXPECTED_SUMMARY_DIFFERENCE",
            "EVIDENCE_DIFFERENCE",
            "OUTPUT_HASH_DIFFERENCE",
            "MISSING_REQUIRED_INPUT"),
        java.util.Arrays.stream(ReplayDifferenceType.values())
            .map(Enum::name)
            .collect(Collectors.toSet()));
    assertEquals(6, ReplayReproducibilityStatus.values().length);
  }

  private static DeterministicReplayProjection projection() {
    return new DeterministicReplayProjection(
        Map.of("snapshotId", "context-1"),
        "policy-1",
        "prompt-1",
        "model-1",
        "gateway-1",
        Map.of("actionLabel", "OBSERVE"),
        List.of(Map.of("refId", "evidence-1")),
        HASH_A,
        HASH_A,
        null);
  }

  private static DeterministicReplayResult result(
      final ReplayReproducibilityStatus status,
      final List<ReplayDifference> differences,
      final ReplayFailureCode code,
      final String baselineHash,
      final String replayHash) {
    return new DeterministicReplayResult(
        "tenant-a",
        "snapshot-1",
        "trace-1",
        "request-1",
        "decision-1",
        new java.util.UUID(0L, 2L),
        "QDR6-REPLAY-INPUT-1",
        "QDR6-CJSON-1",
        "QDR6-MOCK-REPLAY-1",
        HASH_A,
        baselineHash,
        replayHash,
        status,
        differences,
        code,
        "structured test result",
        DeterministicReplayResult.INTERNAL_EVIDENCE_ONLY);
  }
}
