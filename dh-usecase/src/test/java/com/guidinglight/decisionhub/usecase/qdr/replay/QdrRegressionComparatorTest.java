package com.guidinglight.decisionhub.usecase.qdr.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionEvidenceRef;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionFinding;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Stage-qdr-4 B3 comparator 回归测试。
 *
 * <p>测试只比较本地 deterministic summary/ref/hash/version，不访问数据库、不调用 provider/HTTP/NQ，
 * 不启动 Agent 或 LangGraph，也不生成交易信号。
 */
final class QdrRegressionComparatorTest {

    private static final String HASH_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    private final QdrRegressionComparator comparator = new QdrRegressionComparator();

    @Test
    void identicalMockOutputReturnsPass() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, true, true),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_A,
                "mock-gateway:v1",
                "prompt:v1",
                "policy-v1"));

        assertEquals(RegressionVerdict.Status.PASS, verdict.status());
        assertTrue(verdict.findings().isEmpty());
    }

    @Test
    void confidenceDriftWithinToleranceReturnsWarnWhenPolicyRequestsFinding() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, true, true),
                summary("LONG_BIAS", "0.50", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "0.55", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_A,
                "mock-gateway:v1",
                "prompt:v1",
                "policy-v1"));

        assertEquals(RegressionVerdict.Status.WARN, verdict.status());
        assertFindingCodes(verdict, "CONFIDENCE_DRIFT_WITHIN_TOLERANCE");
    }

    @Test
    void confidenceDriftBeyondToleranceReturnsFailUnderStrictPolicy() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, true, true),
                summary("LONG_BIAS", "LOW", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "HIGH", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_A,
                "mock-gateway:v1",
                "prompt:v1",
                "policy-v1"));

        assertEquals(RegressionVerdict.Status.FAIL, verdict.status());
        assertFindingCodes(verdict, "CONFIDENCE_DRIFT_BEYOND_TOLERANCE");
    }

    @Test
    void riskLevelIncreaseReturnsFailUnderStrictPolicy() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, true, true),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.LOW, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.HIGH, evidence(), forbidden()),
                HASH_A,
                "mock-gateway:v1",
                "prompt:v1",
                "policy-v1"));

        assertEquals(RegressionVerdict.Status.FAIL, verdict.status());
        assertFindingCodes(verdict, "RISK_LEVEL_INCREASED");
    }

    @Test
    void missingEvidenceRefReturnsFail() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, true, true),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, List.of("gateway-call-ref"), forbidden()),
                HASH_A,
                "mock-gateway:v1",
                "prompt:v1",
                "policy-v1"));

        assertEquals(RegressionVerdict.Status.FAIL, verdict.status());
        assertFindingCodes(verdict, "EVIDENCE_REFS_MISSING");
    }

    @Test
    void missingForbiddenActionReturnsFail() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, true, true),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), List.of("PLACE_ORDER")),
                HASH_A,
                "mock-gateway:v1",
                "prompt:v1",
                "policy-v1"));

        assertEquals(RegressionVerdict.Status.FAIL, verdict.status());
        assertFindingCodes(verdict, "FORBIDDEN_ACTIONS_MISSING");
    }

    @Test
    void providerSummaryHashMismatchReturnsFailUnderStrictPolicy() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, true, true),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_B,
                "mock-gateway:v1",
                "prompt:v1",
                "policy-v1"));

        assertEquals(RegressionVerdict.Status.FAIL, verdict.status());
        assertFindingCodes(verdict, "PROVIDER_SUMMARY_HASH_MISMATCH");
    }

    @Test
    void modelAndPromptVersionMismatchAreRecordedAsFindings() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, false, false),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_A,
                "mock-gateway:v2",
                "prompt:v2",
                "policy-v1"));

        assertEquals(RegressionVerdict.Status.WARN, verdict.status());
        assertFindingCodes(
                verdict, "MODEL_GATEWAY_VERSION_REF_MISMATCH", "PROMPT_VERSION_REF_MISMATCH");
    }

    @Test
    void policyVersionMismatchCanSkipComparison() {
        final RegressionVerdict verdict = comparator.compare(input(
                policy(true, false, false),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                summary("LONG_BIAS", "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden()),
                HASH_A,
                "mock-gateway:v1",
                "prompt:v1",
                "policy-v2"));

        assertEquals(RegressionVerdict.Status.SKIPPED, verdict.status());
        assertFindingCodes(verdict, "POLICY_VERSION_MISMATCH");
    }

    @Test
    void executableExpectedActionFailsAtBoundary() {
        for (String action : List.of("BUY", "SELL", "MARKET_ORDER")) {
            final ExpectedDecisionSummary unsafe =
                    summary(action, "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> input(
                            policy(true, true, true),
                            unsafe,
                            summary(
                                    "LONG_BIAS",
                                    "MEDIUM",
                                    RiskLevel.MEDIUM,
                                    evidence(),
                                    forbidden()),
                            HASH_A,
                            "mock-gateway:v1",
                            "prompt:v1",
                            "policy-v1"));
        }
    }

    @Test
    void executableAllowedActionFailsAtBoundary() {
        for (String action : List.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE")) {
            final ExpectedDecisionSummary unsafe =
                    summary(action, "MEDIUM", RiskLevel.MEDIUM, evidence(), forbidden());

            assertThrows(
                    IllegalArgumentException.class,
                    () -> input(
                            policy(true, true, true),
                            summary(
                                    "LONG_BIAS",
                                    "MEDIUM",
                                    RiskLevel.MEDIUM,
                                    evidence(),
                                    forbidden()),
                            unsafe,
                            HASH_A,
                            "mock-gateway:v1",
                            "prompt:v1",
                            "policy-v1"));
        }
    }

    private static QdrRegressionComparator.ComparisonInput input(
            final RegressionBaselinePolicy policy,
            final ExpectedDecisionSummary expected,
            final ExpectedDecisionSummary actual,
            final String providerSummaryHash,
            final String modelGatewayVersionRef,
            final String promptVersionRef,
            final String policyVersion) {
        return new QdrRegressionComparator.ComparisonInput(
                "tenant-a",
                "trace-a",
                "request-a",
                "decision-a",
                policy,
                expected,
                actual,
                List.of(new RegressionEvidenceRef(
                        "MOCK_GATEWAY_SUMMARY",
                        "gateway-call-a",
                        providerSummaryHash,
                        "ACTUAL_SUMMARY")),
                providerSummaryHash,
                modelGatewayVersionRef,
                promptVersionRef,
                policyVersion);
    }

    private static RegressionBaselinePolicy policy(
            final boolean warnWithinTolerance,
            final boolean failProviderHash,
            final boolean failRiskIncrease) {
        return new RegressionBaselinePolicy(
                "policy-v1",
                new BigDecimal("0.10"),
                warnWithinTolerance,
                true,
                failRiskIncrease,
                failProviderHash,
                true,
                "mock-gateway:v1",
                "prompt:v1",
                HASH_A);
    }

    private static ExpectedDecisionSummary summary(
            final String actionLabel,
            final String confidenceBand,
            final RiskLevel riskLevel,
            final List<String> evidenceRefs,
            final List<String> forbiddenActions) {
        return new ExpectedDecisionSummary(
                "READ_ONLY_RECOMMENDATION",
                actionLabel,
                confidenceBand,
                riskLevel,
                evidenceRefs,
                forbiddenActions);
    }

    private static List<String> evidence() {
        return List.of("gateway-call-ref", "decision-run-ref");
    }

    private static void assertFindingCodes(final RegressionVerdict verdict, final String... codes) {
        final List<String> actualCodes = verdict.findings().stream()
                .map(RegressionFinding::code)
                .toList();
        for (String code : codes) {
            assertTrue(actualCodes.contains(code), actualCodes.toString());
        }
    }

    private static List<String> forbidden() {
        return List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE");
    }
}
