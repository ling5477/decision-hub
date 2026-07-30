package com.guidinglight.decisionhub.usecase.decision.dryrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** QDR7 fingerprint domain separation、稳定排序和transport字段排除回归。 */
class DecisionDryRunRequestFingerprintTest {

  private final DecisionDryRunRequestFingerprint fingerprint =
      new DecisionDryRunRequestFingerprint();

  @Test
  void retryTransportFieldsDoNotChangeBusinessFingerprint() {
    final DecisionDryRunCommand first = command("request-a", "trace-a", "nonce-a", Set.of("B", "A"));
    final DecisionDryRunCommand retry = command("request-b", "trace-b", "nonce-b", Set.of("A", "B"));

    assertEquals(fingerprint.hash(first), fingerprint.hash(retry));
    assertEquals(64, fingerprint.hash(first).length());
  }

  @Test
  void sourceSchemaTenantAndDecisionContextRemainHashBound() {
    final DecisionDryRunCommand baseline = command("request-a", "trace-a", "nonce-a", Set.of("A"));
    final DecisionDryRunCommand changedTenant =
        new DecisionDryRunCommand(
            baseline.requestId(),
            baseline.traceId(),
            "tenant-b",
            baseline.source(),
            baseline.environment(),
            baseline.timestamp(),
            baseline.nonce(),
            baseline.schemaVersion(),
            baseline.dryRun(),
            baseline.forbiddenCapabilities(),
            baseline.context(),
            false,
            new FeedbackExecutionScope("tenant-b", FeedbackEnvironment.DEV));

    assertNotEquals(fingerprint.hash(baseline), fingerprint.hash(changedTenant));
  }

  @Test
  void unicodeCodePointOrderingMatchesFrozenGoldenVector() {
    final DecisionDryRunCommand vector =
        command(
            "request-a",
            "trace-a",
            "nonce-a",
            Set.of("A", "\uE000", "\uD800\uDC00"));

    assertEquals(
        "c03af1493a39a0cc0bc28bbb95bf56bd7543b0bdb532f58a77cc3536e762d814",
        fingerprint.hash(vector));
  }

  @Test
  void decisionContextBusinessArrayOrderRemainsHashBound() {
    final DecisionDryRunCommand baseline = command("request-a", "trace-a", "nonce-a", Set.of("A"));
    final DecisionDryRunContext reorderedContext =
        new DecisionDryRunContext(
            baseline.context().symbol(),
            baseline.context().market(),
            baseline.context().timeframe(),
            baseline.context().strategyRef(),
            baseline.context().researchRef(),
            baseline.context().contextRef(),
            baseline.context().snapshotId(),
            baseline.context().capturedAt(),
            List.of("evidence-a", "evidence-b"),
            baseline.context().approxBytes());
    final DecisionDryRunCommand reordered =
        new DecisionDryRunCommand(
            baseline.requestId(),
            baseline.traceId(),
            baseline.tenantId(),
            baseline.source(),
            baseline.environment(),
            baseline.timestamp(),
            baseline.nonce(),
            baseline.schemaVersion(),
            baseline.dryRun(),
            baseline.forbiddenCapabilities(),
            reorderedContext,
            false,
            baseline.executionScope());

    assertNotEquals(fingerprint.hash(baseline), fingerprint.hash(reordered));
  }

  @Test
  void sameInputIsStableWithinEnvironmentAndDiffersAcrossDevAndTest() {
    final DecisionDryRunCommand dev =
        command("request-a", "trace-a", "nonce-a", Set.of("A"), FeedbackEnvironment.DEV);
    final DecisionDryRunCommand sameDev =
        command("request-a", "trace-a", "nonce-a", Set.of("A"), FeedbackEnvironment.DEV);
    final DecisionDryRunCommand test =
        command("request-a", "trace-a", "nonce-a", Set.of("A"), FeedbackEnvironment.TEST);

    assertEquals(fingerprint.hash(dev), fingerprint.hash(sameDev));
    assertNotEquals(fingerprint.hash(dev), fingerprint.hash(test));
  }

  private static DecisionDryRunCommand command(
      final String requestId,
      final String traceId,
      final String nonce,
      final Set<String> capabilities) {
    return command(requestId, traceId, nonce, capabilities, FeedbackEnvironment.DEV);
  }

  private static DecisionDryRunCommand command(
      final String requestId,
      final String traceId,
      final String nonce,
      final Set<String> capabilities,
      final FeedbackEnvironment environment) {
    return new DecisionDryRunCommand(
        requestId,
        traceId,
        "tenant-a",
        "NQ_DRYRUN",
        environment.name(),
        "2026-07-12T00:00:00Z",
        nonce,
        "1.0",
        true,
        capabilities,
        new DecisionDryRunContext(
            "BTC-USDT",
            "CRYPTO",
            "1h",
            "strategy-ref",
            "research-ref",
            "context-ref",
            "snapshot-1",
            Instant.parse("2026-07-12T00:00:00Z"),
            List.of("evidence-b", "evidence-a"),
            100),
        false,
        new FeedbackExecutionScope("tenant-a", environment));
  }
}
