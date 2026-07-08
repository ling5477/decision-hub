package com.guidinglight.decisionhub.usecase.qdr.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationCase;
import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationPolicy;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionFinding;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayCase;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Stage-qdr-4 B1 replay / evaluation 合同服务回归。
 *
 * <p>测试只验证内存对象和 fail-closed 合同，不依赖数据库、Testcontainers、真实网络、provider SDK、
 * Agent 或 LangGraph runtime。
 */
final class ReplayEvaluationContractServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-08T00:00:00Z");
    private static final String TENANT = "tenant-a";

    private final ReplayEvaluationContractService service = new ReplayEvaluationContractService();

    @Test
    void validReplayCaseCanBeCreatedAndValidated() {
        final ReplayCase replayCase = validReplayCase(validSummary("LONG_BIAS", List.of("NO_SIDE_EFFECT")));

        final RegressionVerdict validation = service.validateReplayCase(replayCase);
        final RegressionVerdict initialVerdict = service.createInitialVerdict(replayCase, validEvaluationCase());

        assertEquals(RegressionVerdict.Status.PASS, validation.status());
        assertEquals(RegressionVerdict.Status.SKIPPED, initialVerdict.status());
        assertEquals("REPLAY_NOT_EXECUTED_IN_B1", initialVerdict.failureReason());
    }

    @Test
    void missingTenantIdFailsClosed() {
        final RegressionVerdict verdict = service.validateReplayCase(new ReplayCase(
                null,
                "case-a",
                "decision-a",
                "request-a",
                "trace-a",
                inputRef(),
                validSummary("OBSERVE", List.of("NO_SIDE_EFFECT")),
                policy(),
                NOW));

        assertFailsClosed(verdict, "TENANT_ID_REQUIRED");
    }

    @Test
    void missingTraceIdFailsClosed() {
        final RegressionVerdict verdict = service.validateReplayCase(new ReplayCase(
                TENANT,
                "case-a",
                "decision-a",
                "request-a",
                null,
                inputRef(),
                validSummary("OBSERVE", List.of("NO_SIDE_EFFECT")),
                policy(),
                NOW));

        assertFailsClosed(verdict, "TRACE_ID_REQUIRED");
    }

    @Test
    void expectedSummaryContainingBuyFailsClosed() {
        assertFailsClosed(
                service.validateReplayCase(validReplayCase(validSummary("BUY", List.of("NO_SIDE_EFFECT")))),
                "EXPECTED_SUMMARY_TRADING_TERM_FORBIDDEN");
    }

    @Test
    void expectedSummaryContainingSellFailsClosed() {
        assertFailsClosed(
                service.validateReplayCase(validReplayCase(validSummary("SELL", List.of("NO_SIDE_EFFECT")))),
                "EXPECTED_SUMMARY_TRADING_TERM_FORBIDDEN");
    }

    @Test
    void expectedSummaryContainingMarketOrderFailsClosed() {
        assertFailsClosed(
                service.validateReplayCase(validReplayCase(validSummary("MARKET_ORDER", List.of("NO_SIDE_EFFECT")))),
                "EXPECTED_SUMMARY_TRADING_TERM_FORBIDDEN");
    }

    @Test
    void forbiddenActionsContainingPlaceOrderFailsClosed() {
        assertFailsClosed(
                service.validateReplayCase(validReplayCase(validSummary("OBSERVE", List.of("PLACE_ORDER")))),
                "FORBIDDEN_ACTION_TRADING_MUTATION_TERM_FORBIDDEN");
    }

    @Test
    void forbiddenActionsContainingCancelOrderFailsClosed() {
        assertFailsClosed(
                service.validateReplayCase(validReplayCase(validSummary("OBSERVE", List.of("CANCEL_ORDER")))),
                "FORBIDDEN_ACTION_TRADING_MUTATION_TERM_FORBIDDEN");
    }

    @Test
    void forbiddenActionsContainingMutateNqStateFailsClosed() {
        assertFailsClosed(
                service.validateReplayCase(validReplayCase(validSummary("OBSERVE", List.of("MUTATE_NQ_STATE")))),
                "FORBIDDEN_ACTION_TRADING_MUTATION_TERM_FORBIDDEN");
    }

    @Test
    void regressionVerdictSupportsPassFailWarnAndSkipped() {
        final RegressionFinding finding = new RegressionFinding(
                "QDR_DIFF", RegressionSeverity.WARN, "structured summary drift", "case-a");

        assertEquals(RegressionVerdict.Status.PASS, RegressionVerdict.pass().status());
        assertEquals(RegressionVerdict.Status.FAIL, RegressionVerdict.fail("QDR_FAIL").status());
        assertEquals(
                RegressionVerdict.Status.WARN,
                RegressionVerdict.warn("QDR_WARN", List.of(finding)).status());
        assertEquals(
                RegressionVerdict.Status.SKIPPED,
                RegressionVerdict.skipped("QDR_SKIPPED", List.of(finding)).status());
    }

    @Test
    void evaluationPolicyRejectsRawProviderResponseDependency() {
        final EvaluationPolicy policy = policy();

        assertFalse(policy.usesRawProviderResponseDependency());
        assertThrows(
                IllegalArgumentException.class,
                () -> EvaluationPolicy.RequiredEvidenceMode.fromContractValue("RAW_PROVIDER_RESPONSE"));
    }

    @Test
    void validEvaluationCaseCanBeValidatedWithoutActualSummaryOrVerdict() {
        final RegressionVerdict verdict = service.validateEvaluationCase(validEvaluationCase());

        assertEquals(RegressionVerdict.Status.PASS, verdict.status());
    }

    @Test
    void noRealHttpProviderOrLangGraphClassesAreIntroduced() throws IOException {
        final Path projectRoot = projectRoot();
        final List<Path> roots = List.of(
                projectRoot.resolve("dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/replay"),
                projectRoot.resolve("dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/replay"));
        final String forbiddenTerms =
                "RealClient|HttpClient|WebClient|RestTemplate|OkHttp|LangGraph|AutoGen|CrewAI";

        for (Path root : roots) {
            try (Stream<Path> files = Files.walk(root)) {
                final List<Path> offenders = files
                        .filter(Files::isRegularFile)
                        .filter(path -> containsForbiddenRuntimeTerm(path, forbiddenTerms))
                        .toList();
                assertTrue(offenders.isEmpty(), "unexpected runtime/provider terms: " + offenders);
            }
        }
    }

    private static Path projectRoot() {
        final Path workingDirectory = Path.of("").toAbsolutePath().normalize();
        if ("dh-usecase".equals(workingDirectory.getFileName().toString())) {
            return workingDirectory.getParent();
        }
        return workingDirectory;
    }

    private static boolean containsForbiddenRuntimeTerm(final Path path, final String forbiddenTerms) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8).matches("(?s).*(" + forbiddenTerms + ").*");
        } catch (final IOException error) {
            throw new AssertionError("failed to inspect " + path, error);
        }
    }

    private static void assertFailsClosed(final RegressionVerdict verdict, final String reason) {
        assertEquals(RegressionVerdict.Status.FAIL, verdict.status());
        assertEquals(reason, verdict.failureReason());
        assertFalse(verdict.findings().isEmpty());
        assertEquals(RegressionSeverity.BLOCKER, verdict.findings().get(0).severity());
    }

    private static ReplayCase validReplayCase(final ExpectedDecisionSummary expectedSummary) {
        return new ReplayCase(
                TENANT,
                "case-a",
                "decision-a",
                "request-a",
                "trace-a",
                inputRef(),
                expectedSummary,
                policy(),
                NOW);
    }

    private static EvaluationCase validEvaluationCase() {
        return new EvaluationCase(
                TENANT,
                "evaluation-a",
                "case-a",
                "qdr-eval-policy-v1",
                "model-version:v1",
                "mock-gateway:v1",
                validSummary("LONG_BIAS", List.of("NO_SIDE_EFFECT")),
                null,
                null,
                new ReplayOutputRef("gateway-summary-ref", "summary-ref-a", "hash-a"));
    }

    private static ExpectedDecisionSummary validSummary(
            final String actionLabel, final List<String> forbiddenActions) {
        return new ExpectedDecisionSummary(
                "READ_ONLY_RECOMMENDATION",
                actionLabel,
                "MEDIUM",
                RiskLevel.MEDIUM,
                List.of("gateway-call-ref", "decision-run-ref"),
                forbiddenActions);
    }

    private static ReplayInputRef inputRef() {
        return new ReplayInputRef("decision-run-ref", "decision-run-a", "hash-a");
    }

    private static EvaluationPolicy policy() {
        return new EvaluationPolicy(
                "qdr-eval-policy-v1",
                new BigDecimal("0.10"),
                1,
                EvaluationPolicy.RequiredEvidenceMode.STRICT,
                false);
    }
}
