package com.guidinglight.decisionhub.usecase.qdr.report;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceFinding;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceRef;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayObservabilityReport;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayObservabilitySummary;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ObservabilityReportCommand;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ObservabilityReportService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderFailureClassification;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderHealthReadModelQuery;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderHealthReadModelService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderHealthReadModelView;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderLatencyBudgetSummary;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessDecision;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessDecisionReason;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessEvaluationCommand;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessEvaluationResult;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessSignal;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessStatus;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderTrustDecisionSummary;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionDriftSummary;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionReportFindingView;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionReportView;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.DeterministicReplayResult;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayDifference;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayDifferenceType;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayFailureCode;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.ReplayReproducibilityStatus;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Stage-QDR-6 B4 internal report 状态、边界、排序与无副作用回归。 */
class DecisionEvidenceReplayReportServiceTest {

  private static final String TENANT = "tenant-a";
  private static final String TRACE = "trace-a";
  private static final String REQUEST = "request-a";
  private static final String DECISION = "decision-a";
  private static final String PROVIDER = "mock-provider-a";
  private static final String GATEWAY = "gateway-v1";
  private static final String POLICY = "readiness-policy-v1";
  private static final String HASH_A = "a".repeat(64);
  private static final String HASH_B = "b".repeat(64);
  private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");
  private static final DecisionEvidenceCorrelation CORRELATION =
      new DecisionEvidenceCorrelation(TENANT, TRACE, REQUEST, DECISION);
  private final DecisionEvidenceReplayReportService service =
      new DecisionEvidenceReplayReportService();

  @Test
  void completeReproducibleRegressionPassAndReadinessPassAreAccepted() {
    final ProviderReadinessEvaluationResult readiness = readyReadiness();
    final DecisionEvidenceReplayInternalReport report =
        generate(completeEvidence(), reproducibleReplay(), regressionPass(), readiness, observability(readiness));

    assertEquals(InternalAcceptanceStatus.ACCEPTED, report.acceptanceStatus());
    assertEquals(DecisionEvidenceStatus.COMPLETE, report.evidenceStatus());
    assertEquals(ReplayReproducibilityStatus.REPRODUCIBLE, report.replayStatus());
    assertEquals(RegressionVerdict.Status.PASS, report.regressionVerdict());
    assertEquals(ProviderReadinessDecision.READY, report.providerReadinessDecision());
  }

  @Test
  void mandatoryEvidenceMissingIsIncomplete() {
    final DecisionEvidenceAggregate incomplete =
        new DecisionEvidenceAggregate(
            CORRELATION,
            List.of(),
            DecisionEvidenceStatus.INCOMPLETE,
            List.of(),
            List.of(DecisionEvidencePolicy.EvidenceType.REQUEST));

    assertEquals(
        InternalAcceptanceStatus.INCOMPLETE,
        generate(incomplete, reproducibleReplay(), regressionPass(), readyReadiness(), observability(readyReadiness()))
            .acceptanceStatus());
  }

  @Test
  void invalidEvidenceIsInvalid() {
    final DecisionEvidenceAggregate invalid =
        new DecisionEvidenceAggregate(
            CORRELATION,
            List.of(),
            DecisionEvidenceStatus.INVALID,
            List.of(
                new DecisionEvidenceFinding(
                    "EVIDENCE_INVALID",
                    DecisionEvidenceFinding.Severity.BLOCKER,
                    null,
                    null,
                    "evidence contract is invalid")),
            List.of());

    assertEquals(
        InternalAcceptanceStatus.INVALID,
        generate(invalid, reproducibleReplay(), regressionPass(), readyReadiness(), observability(readyReadiness()))
            .acceptanceStatus());
  }

  @Test
  void replayDifferentIsRejected() {
    assertEquals(
        InternalAcceptanceStatus.REJECTED,
        generate(completeEvidence(), differentReplay(), regressionPass(), readyReadiness(), observability(readyReadiness()))
            .acceptanceStatus());
  }

  @Test
  void replayInvalidInputIsInvalid() {
    assertEquals(
        InternalAcceptanceStatus.INVALID,
        generate(
                completeEvidence(),
                failedReplay(ReplayFailureCode.UNSAFE_INPUT),
                regressionPass(),
                readyReadiness(),
                observability(readyReadiness()))
            .acceptanceStatus());
  }

  @Test
  void replayUnsupportedVersionIsUnsupported() {
    assertEquals(
        InternalAcceptanceStatus.UNSUPPORTED,
        generate(
                completeEvidence(),
                failedReplay(ReplayFailureCode.EXECUTOR_VERSION_UNSUPPORTED),
                regressionPass(),
                readyReadiness(),
                observability(readyReadiness()))
            .acceptanceStatus());
  }

  @Test
  void replayExecutionFailureIsFailed() {
    assertEquals(
        InternalAcceptanceStatus.FAILED,
        generate(
                completeEvidence(),
                failedReplay(ReplayFailureCode.PERSISTENCE_READ_FAILED),
                regressionPass(),
                readyReadiness(),
                observability(readyReadiness()))
            .acceptanceStatus());
  }

  @Test
  void regressionFailIsRejected() {
    final ProviderReadinessEvaluationResult readiness = readyReadiness();
    assertEquals(
        InternalAcceptanceStatus.REJECTED,
        generate(completeEvidence(), reproducibleReplay(), regressionFail(), readiness, observability(readiness))
            .acceptanceStatus());
  }

  @Test
  void providerReadinessRejectIsRejected() {
    final ProviderReadinessEvaluationResult readiness = rejectedReadiness();
    assertEquals(
        InternalAcceptanceStatus.REJECTED,
        generate(completeEvidence(), reproducibleReplay(), regressionPass(), readiness, observability(readiness))
            .acceptanceStatus());
  }

  @Test
  void missingRequiredObservabilityIsIncomplete() {
    assertEquals(
        InternalAcceptanceStatus.INCOMPLETE,
        generate(completeEvidence(), reproducibleReplay(), regressionPass(), readyReadiness(), null)
            .acceptanceStatus());
  }

  @Test
  void crossTenantInputIsInvalid() {
    final DecisionEvidenceCorrelation other =
        new DecisionEvidenceCorrelation("tenant-b", TRACE, REQUEST, DECISION);
    final DecisionEvidenceAggregate evidence =
        new DecisionEvidenceAggregate(
            other, List.of(), DecisionEvidenceStatus.COMPLETE, List.of(), List.of());

    assertEquals(
        InternalAcceptanceStatus.INVALID,
        generate(evidence, reproducibleReplay(), regressionPass(), readyReadiness(), observability(readyReadiness()))
            .acceptanceStatus());
  }

  @Test
  void sourceExceptionIsSanitizedAndFailed() {
    final DecisionEvidenceReplayInternalReport report =
        service.generateFromSources(
            CORRELATION,
            () -> {
              throw new IllegalStateException("rawPrompt:payload-value");
            },
            this::reproducibleReplay,
            this::regressionPass,
            this::readyReadiness,
            () -> observability(readyReadiness()));

    assertEquals(InternalAcceptanceStatus.FAILED, report.acceptanceStatus());
    assertFalse(report.toString().contains("payload-value"));
  }

  @Test
  void findingsDifferencesAndRefsUseStableOrdering() {
    final DecisionEvidenceAggregate evidence =
        new DecisionEvidenceAggregate(
            CORRELATION,
            completeEvidence().evidenceRefs(),
            DecisionEvidenceStatus.COMPLETE,
            List.of(
                new DecisionEvidenceFinding(
                    "Z_FINDING",
                    DecisionEvidenceFinding.Severity.WARN,
                    null,
                    "z-ref",
                    "structured z finding"),
                new DecisionEvidenceFinding(
                    "A_FINDING",
                    DecisionEvidenceFinding.Severity.INFO,
                    null,
                    "a-ref",
                    "structured a finding")),
            List.of());
    final DecisionEvidenceReplayInternalReport report =
        generate(evidence, differentReplay(), regressionFail(), readyReadiness(), observability(readyReadiness()));
    final List<InternalAcceptanceFinding> sorted = new ArrayList<>(report.findings());
    sorted.sort(InternalAcceptanceFinding.STABLE_ORDER);

    assertEquals(sorted, report.findings());
    assertEquals(report.evidenceRefs().stream().sorted().toList(), report.evidenceRefs());
    assertEquals(
        report.replayDifferences().stream()
            .sorted(Comparator.comparing(ReplayDifference::type).thenComparing(ReplayDifference::path))
            .toList(),
        report.replayDifferences());
  }

  @Test
  void unsafeFindingMaterialIsRejectedAndNeverStored() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new InternalAcceptanceFinding(
                InternalAcceptanceFinding.Source.EVIDENCE,
                InternalAcceptanceFinding.Severity.BLOCKER,
                "UNSAFE_FINDING",
                null,
                "rawProviderResponse:value"));
  }

  @Test
  void reportCarriesExplicitNonAuthorizationSemantics() {
    final ProviderReadinessEvaluationResult readiness = readyReadiness();
    final DecisionEvidenceReplayInternalReport report =
        generate(completeEvidence(), reproducibleReplay(), regressionPass(), readiness, observability(readiness));

    assertEquals(DecisionEvidenceReplayInternalReport.INTERNAL_ACCEPTANCE_ONLY, report.safetyDeclaration());
    assertTrue(report.safetyDeclaration().contains("not provider authorization"));
    assertTrue(report.safetyDeclaration().contains("not NQ integration permission"));
    assertFalse(report.acceptanceStatus().authorizesProvider());
    assertFalse(report.acceptanceStatus().authorizesNqIntegration());
    assertFalse(report.acceptanceStatus().allowsTradingOrExecution());
    assertFalse(report.acceptanceStatus().enablesPaperOrLive());
  }

  @Test
  void reportPackageHasNoApiPersistenceExternalIoProviderAgentOrTradingDependency()
      throws IOException {
    final Path sourceRoot =
        moduleRoot().resolve("src/main/java/com/guidinglight/decisionhub/usecase/qdr/report");
    final List<Pattern> forbidden =
        List.of(
            Pattern.compile("java\\.net"),
            Pattern.compile("org\\.springframework"),
            Pattern.compile("com\\.guidinglight\\.decisionhub\\.infra"),
            Pattern.compile("Repository"),
            Pattern.compile("Jdbc"),
            Pattern.compile("Controller"),
            Pattern.compile("WebClient"),
            Pattern.compile("RestTemplate"),
            Pattern.compile("Instant\\.now"),
            Pattern.compile("randomUUID"),
            Pattern.compile("System\\.getenv"),
            Pattern.compile("\\.insert\\("),
            Pattern.compile("\\.save\\("));

    try (var paths = Files.walk(sourceRoot)) {
      final String source =
          paths.filter(path -> path.toString().endsWith(".java"))
              .map(DecisionEvidenceReplayReportServiceTest::read)
              .reduce("", String::concat);
      forbidden.forEach(pattern -> assertFalse(pattern.matcher(source).find(), pattern.toString()));
    }
  }

  private DecisionEvidenceReplayInternalReport generate(
      final DecisionEvidenceAggregate evidence,
      final DeterministicReplayResult replay,
      final RegressionReportView regression,
      final ProviderReadinessEvaluationResult readiness,
      final ModelGatewayObservabilityReport observability) {
    return service.generate(CORRELATION, evidence, replay, regression, readiness, observability);
  }

  private DecisionEvidenceAggregate completeEvidence() {
    final DecisionEvidenceRef ref =
        new DecisionEvidenceRef(
            new DecisionEvidence("request-ref", "REQUEST", "structured request evidence"),
            CORRELATION,
            DecisionEvidencePolicy.EvidenceType.REQUEST,
            "request-ref",
            HASH_A,
            "V5",
            true,
            RedactionStatus.REDACTED);
    return new DecisionEvidenceAggregate(
        CORRELATION, List.of(ref), DecisionEvidenceStatus.COMPLETE, List.of(), List.of());
  }

  private DeterministicReplayResult reproducibleReplay() {
    return replay(
        ReplayReproducibilityStatus.REPRODUCIBLE, List.of(), null, HASH_A, HASH_A);
  }

  private DeterministicReplayResult differentReplay() {
    return replay(
        ReplayReproducibilityStatus.DIFFERENT,
        List.of(
            new ReplayDifference(
                ReplayDifferenceType.OUTPUT_HASH_DIFFERENCE,
                "outputHash",
                HASH_A,
                HASH_B,
                "structured replay output differs")),
        null,
        HASH_A,
        HASH_B);
  }

  private DeterministicReplayResult failedReplay(final ReplayFailureCode code) {
    return replay(code.status(), List.of(), code, null, null);
  }

  private DeterministicReplayResult replay(
      final ReplayReproducibilityStatus status,
      final List<ReplayDifference> differences,
      final ReplayFailureCode failure,
      final String baselineHash,
      final String replayHash) {
    return new DeterministicReplayResult(
        TENANT,
        "snapshot-a",
        TRACE,
        REQUEST,
        DECISION,
        new UUID(0L, 2L),
        "QDR6-REPLAY-INPUT-1",
        "QDR6-CJSON-1",
        "QDR6-MOCK-REPLAY-1",
        HASH_A,
        baselineHash,
        replayHash,
        status,
        differences,
        failure,
        "structured replay result",
        DeterministicReplayResult.INTERNAL_EVIDENCE_ONLY);
  }

  private RegressionReportView regressionPass() {
    return regression(RegressionVerdict.Status.PASS, RegressionSeverity.INFO, List.of());
  }

  private RegressionReportView regressionFail() {
    return regression(
        RegressionVerdict.Status.FAIL,
        RegressionSeverity.ERROR,
        List.of(
            new RegressionReportFindingView(
                "REGRESSION_MISMATCH",
                RegressionSeverity.ERROR,
                "structured regression mismatch",
                "evidence-ref",
                NOW,
                NOW)));
  }

  private RegressionReportView regression(
      final RegressionVerdict.Status verdict,
      final RegressionSeverity severity,
      final List<RegressionReportFindingView> findings) {
    final RegressionDriftSummary.DriftState match = RegressionDriftSummary.DriftState.MATCH;
    return new RegressionReportView(
        TENANT,
        "case-a",
        "evaluation-a",
        "verdict-a",
        TRACE,
        REQUEST,
        DECISION,
        "READ_ONLY_RECOMMENDATION",
        "OBSERVE",
        "MEDIUM",
        RiskLevel.LOW,
        verdict,
        severity,
        HASH_A,
        GATEWAY,
        "prompt-v1",
        "policy-v1",
        NOW,
        NOW,
        "structured regression report",
        List.of("evidence:request-ref"),
        new RegressionDriftSummary(
            match, match, match, match, match, match, match, match, match, match),
        findings);
  }

  private ProviderReadinessEvaluationResult readyReadiness() {
    final ProviderHealthReadModelView health = healthView();
    return ProviderReadinessEvaluationResult.ready(
        new ProviderReadinessEvaluationCommand(
            TENANT,
            "source:mock-safe",
            PROVIDER,
            GATEWAY,
            HASH_A,
            ProviderFailureClassification.NONE,
            latency(),
            ProviderTrustDecisionSummary.allowed("trust-ref-a"),
            ProviderReadinessSignal.ready("readiness-ref-a"),
            health,
            POLICY,
            TRACE,
            REQUEST,
            NOW,
            NOW));
  }

  private ProviderReadinessEvaluationResult rejectedReadiness() {
    return ProviderReadinessEvaluationResult.failClosed(
        ProviderReadinessDecision.NOT_READY,
        ProviderReadinessDecisionReason.POLICY_EVIDENCE_REJECTED,
        POLICY,
        TRACE,
        REQUEST,
        PROVIDER);
  }

  private ModelGatewayObservabilityReport observability(
      final ProviderReadinessEvaluationResult readiness) {
    return new ObservabilityReportService()
        .generate(
            new ObservabilityReportCommand(
                TENANT,
                "source:mock-safe",
                observabilitySummary(),
                healthView(),
                readiness,
                NOW,
                NOW));
  }

  private ProviderHealthReadModelView healthView() {
    return new ProviderHealthReadModelService(List.of(observabilitySummary()))
        .findProviderHealth(
            new ProviderHealthReadModelQuery(
                TENANT,
                PROVIDER,
                GATEWAY,
                TRACE,
                REQUEST,
                null,
                null,
                ProviderReadinessStatus.READY,
                null,
                null,
                null,
                null,
                50,
                0))
        .getFirst();
  }

  private ModelGatewayObservabilitySummary observabilitySummary() {
    return new ModelGatewayObservabilitySummary(
        TENANT,
        TRACE,
        REQUEST,
        GATEWAY,
        PROVIDER,
        HASH_A,
        latency(),
        ProviderFailureClassification.NONE,
        ProviderTrustDecisionSummary.allowed("trust-ref-a"),
        ProviderReadinessSignal.ready("readiness-ref-a"),
        NOW);
  }

  private ProviderLatencyBudgetSummary latency() {
    return new ProviderLatencyBudgetSummary(20L, 60L, 90L, 200L, 0.45d, 15L);
  }

  private static Path moduleRoot() {
    final Path cwd = Path.of("").toAbsolutePath();
    if (Files.isDirectory(cwd.resolve("src/main/java"))) {
      return cwd;
    }
    return cwd.resolve("dh-usecase");
  }

  private static String read(final Path path) {
    try {
      return Files.readString(path);
    } catch (final IOException error) {
      throw new IllegalStateException("report source scan failed", error);
    }
  }
}
