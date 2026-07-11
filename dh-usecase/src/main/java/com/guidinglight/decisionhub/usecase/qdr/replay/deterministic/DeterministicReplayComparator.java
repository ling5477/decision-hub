package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import com.guidinglight.decisionhub.usecase.qdr.snapshot.Qdr6CanonicalJson;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

/** 仅比较冻结结构化字段并输出稳定、无 raw value differences 的 comparator。 */
public final class DeterministicReplayComparator {

  private final Qdr6CanonicalJson canonicalJson;

  /** 创建使用冻结 canonicalizer 生成安全 field fingerprints 的 comparator。 */
  public DeterministicReplayComparator(final Qdr6CanonicalJson canonicalJson) {
    this.canonicalJson = Objects.requireNonNull(canonicalJson, "canonicalJson");
  }

  List<ReplayDifference> compare(
      final DeterministicReplayProjection baseline,
      final DeterministicReplayProjection replay,
      final String baselineOutputHash,
      final String replayOutputHash) {
    if (baseline == null || replay == null) {
      return List.of(
          difference(
              ReplayDifferenceType.MISSING_REQUIRED_INPUT,
              "projection",
              baseline == null ? null : "present",
              replay == null ? null : "present"));
    }
    final List<ReplayDifference> differences = new ArrayList<>();
    compareFingerprint(
        differences,
        ReplayDifferenceType.CONTEXT_DIFFERENCE,
        "context",
        baseline.context(),
        replay.context());
    compareRef(
        differences,
        ReplayDifferenceType.POLICY_VERSION_DIFFERENCE,
        "policyVersion",
        baseline.policyVersion(),
        replay.policyVersion());
    compareRef(
        differences,
        ReplayDifferenceType.PROMPT_VERSION_DIFFERENCE,
        "promptVersion",
        baseline.promptVersion(),
        replay.promptVersion());
    compareRef(
        differences,
        ReplayDifferenceType.MODEL_VERSION_DIFFERENCE,
        "modelVersion",
        baseline.modelVersion(),
        replay.modelVersion());
    compareRef(
        differences,
        ReplayDifferenceType.GATEWAY_VERSION_DIFFERENCE,
        "gatewayVersion",
        baseline.gatewayVersion(),
        replay.gatewayVersion());
    compareFingerprint(
        differences,
        ReplayDifferenceType.EXPECTED_SUMMARY_DIFFERENCE,
        "expectedSummary",
        baseline.expectedSummary(),
        replay.expectedSummary());
    compareFingerprint(
        differences,
        ReplayDifferenceType.EVIDENCE_DIFFERENCE,
        "evidenceRefs",
        baseline.evidenceRefs(),
        replay.evidenceRefs());
    compareRef(
        differences,
        ReplayDifferenceType.OUTPUT_HASH_DIFFERENCE,
        "outputHash",
        baselineOutputHash,
        replayOutputHash);
    return differences.stream()
        .sorted(Comparator.comparing(ReplayDifference::type).thenComparing(ReplayDifference::path))
        .toList();
  }

  private void compareFingerprint(
      final List<ReplayDifference> target,
      final ReplayDifferenceType type,
      final String path,
      final Object expected,
      final Object actual) {
    final String expectedHash = fingerprint(expected);
    final String actualHash = fingerprint(actual);
    compareRef(target, type, path, expectedHash, actualHash);
  }

  private static void compareRef(
      final List<ReplayDifference> target,
      final ReplayDifferenceType type,
      final String path,
      final String expected,
      final String actual) {
    if (!Objects.equals(expected, actual)) {
      target.add(difference(type, path, expected, actual));
    }
  }

  private static ReplayDifference difference(
      final ReplayDifferenceType type,
      final String path,
      final String expected,
      final String actual) {
    return new ReplayDifference(
        type, path, expected, actual, "structured replay field differs from frozen baseline");
  }

  private String fingerprint(final Object value) {
    if (value == null) {
      return null;
    }
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(canonicalJson.canonicalize(value)));
    } catch (final NoSuchAlgorithmException error) {
      throw new IllegalStateException("structured fingerprint SHA-256 unavailable", error);
    }
  }
}
