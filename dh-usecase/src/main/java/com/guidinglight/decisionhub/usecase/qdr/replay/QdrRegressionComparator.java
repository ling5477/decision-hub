package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionEvidenceRef;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionFinding;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * B3 QDR deterministic regression comparator。
 *
 * <p>Comparator 只比较结构化 summary、safe refs、hash 和版本信息，不读取 raw prompt、
 * raw provider response、credential，不调用 provider/HTTP/NQ，也不生成 trading signal。
 */
public final class QdrRegressionComparator {

    /**
     * 比较 expected 与 actual regression summary。
     *
     * @param input comparison input。
     * @return PASS / WARN / FAIL / SKIPPED verdict。
     */
    public RegressionVerdict compare(final ComparisonInput input) {
        final ComparisonInput checked = Objects.requireNonNull(input, "input");
        final List<RegressionFinding> findings = new ArrayList<>();
        boolean fail = false;
        boolean warn = false;
        boolean skipped = false;

        if (!checked.policy().policyVersion().equals(checked.actualPolicyVersion())) {
            findings.add(finding(
                    "POLICY_VERSION_MISMATCH",
                    RegressionSeverity.WARN,
                    "policy version drift recorded",
                    checked.firstEvidenceRef()));
            if (checked.policy().skipOnPolicyVersionMismatch()) {
                skipped = true;
            } else {
                warn = true;
            }
        }

        if (!checked.policy().modelGatewayVersionRef().equals(checked.actualModelGatewayVersionRef())) {
            findings.add(finding(
                    "MODEL_GATEWAY_VERSION_REF_MISMATCH",
                    RegressionSeverity.WARN,
                    "model gateway version drift recorded",
                    checked.firstEvidenceRef()));
            warn = true;
        }
        if (!checked.policy().promptVersionRef().equals(checked.actualPromptVersionRef())) {
            findings.add(finding(
                    "PROMPT_VERSION_REF_MISMATCH",
                    RegressionSeverity.WARN,
                    "prompt version drift recorded",
                    checked.firstEvidenceRef()));
            warn = true;
        }
        if (!checked.policy().providerSummaryHash().equals(checked.actualProviderSummaryHash())) {
            findings.add(finding(
                    "PROVIDER_SUMMARY_HASH_MISMATCH",
                    checked.policy().failOnProviderSummaryHashMismatch()
                            ? RegressionSeverity.ERROR
                            : RegressionSeverity.WARN,
                    "provider summary hash drift recorded",
                    checked.firstEvidenceRef()));
            fail = fail || checked.policy().failOnProviderSummaryHashMismatch();
            warn = warn || !checked.policy().failOnProviderSummaryHashMismatch();
        }

        if (!textEquals(checked.expectedSummary().decisionType(), checked.actualSummary().decisionType())) {
            findings.add(errorFinding("DECISION_TYPE_MISMATCH", "decision type mismatch", checked));
            fail = true;
        }
        if (!textEquals(checked.expectedSummary().actionLabel(), checked.actualSummary().actionLabel())) {
            findings.add(errorFinding("ACTION_LABEL_MISMATCH", "action label mismatch", checked));
            fail = true;
        }

        final ConfidenceDrift confidenceDrift =
                confidenceDrift(checked.expectedSummary().confidenceBand(), checked.actualSummary().confidenceBand());
        if (confidenceDrift.drift().compareTo(BigDecimal.ZERO) > 0) {
            if (confidenceDrift.drift().compareTo(checked.policy().confidenceTolerance()) <= 0) {
                if (checked.policy().warnOnConfidenceDriftWithinTolerance()) {
                    findings.add(finding(
                            "CONFIDENCE_DRIFT_WITHIN_TOLERANCE",
                            RegressionSeverity.WARN,
                            "confidence drift is within policy tolerance",
                            checked.firstEvidenceRef()));
                    warn = true;
                }
            } else {
                findings.add(finding(
                        "CONFIDENCE_DRIFT_BEYOND_TOLERANCE",
                        checked.policy().failOnConfidenceDriftBeyondTolerance()
                                ? RegressionSeverity.ERROR
                                : RegressionSeverity.WARN,
                        "confidence drift exceeds policy tolerance",
                        checked.firstEvidenceRef()));
                fail = fail || checked.policy().failOnConfidenceDriftBeyondTolerance();
                warn = warn || !checked.policy().failOnConfidenceDriftBeyondTolerance();
            }
        }

        final int riskDelta = riskRank(checked.actualSummary().riskLevel())
                - riskRank(checked.expectedSummary().riskLevel());
        if (riskDelta > 0) {
            findings.add(finding(
                    "RISK_LEVEL_INCREASED",
                    checked.policy().failOnRiskLevelIncrease()
                            ? RegressionSeverity.ERROR
                            : RegressionSeverity.WARN,
                    "risk level increased from baseline",
                    checked.firstEvidenceRef()));
            fail = fail || checked.policy().failOnRiskLevelIncrease();
            warn = warn || !checked.policy().failOnRiskLevelIncrease();
        } else if (riskDelta < 0 && !evidenceCoversExpected(checked)) {
            findings.add(finding(
                    "RISK_LEVEL_DECREASED_WITH_WEAK_EVIDENCE",
                    RegressionSeverity.WARN,
                    "risk level decreased but evidence coverage is weak",
                    checked.firstEvidenceRef()));
            warn = true;
        }

        if (!evidenceCoversExpected(checked)) {
            findings.add(errorFinding("EVIDENCE_REFS_MISSING", "required evidence refs missing", checked));
            fail = true;
        }
        if (!forbiddenActionsCoverExpected(checked)) {
            findings.add(errorFinding("FORBIDDEN_ACTIONS_MISSING", "forbidden actions missing", checked));
            fail = true;
        }

        if (fail) {
            return RegressionVerdict.fail("QDR_REGRESSION_COMPARISON_FAILED", findings);
        }
        if (skipped) {
            return RegressionVerdict.skipped("QDR_REGRESSION_POLICY_MISMATCH", findings);
        }
        if (warn || !findings.isEmpty()) {
            return RegressionVerdict.warn("QDR_REGRESSION_COMPARISON_WARN", findings);
        }
        return RegressionVerdict.pass();
    }

    private static RegressionFinding errorFinding(
            final String code, final String message, final ComparisonInput input) {
        return finding(code, RegressionSeverity.ERROR, message, input.firstEvidenceRef());
    }

    private static RegressionFinding finding(
            final String code,
            final RegressionSeverity severity,
            final String message,
            final String evidenceRef) {
        return new RegressionFinding(
                code,
                severity,
                QdrRegressionSafety.requireFindingMessage(message),
                evidenceRef);
    }

    private static boolean evidenceCoversExpected(final ComparisonInput input) {
        final List<String> actualRefs = normalize(input.actualSummary().requiredEvidenceRefs());
        return !actualRefs.isEmpty()
                && actualRefs.containsAll(normalize(input.expectedSummary().requiredEvidenceRefs()));
    }

    private static boolean forbiddenActionsCoverExpected(final ComparisonInput input) {
        final List<String> actualActions = normalize(input.actualSummary().forbiddenActions());
        return !actualActions.isEmpty()
                && actualActions.containsAll(normalize(input.expectedSummary().forbiddenActions()));
    }

    private static List<String> normalize(final List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static boolean textEquals(final String left, final String right) {
        return Objects.equals(
                left == null ? null : left.trim(),
                right == null ? null : right.trim());
    }

    private static ConfidenceDrift confidenceDrift(final String expected, final String actual) {
        final BigDecimal expectedValue = confidenceValue(expected);
        final BigDecimal actualValue = confidenceValue(actual);
        return new ConfidenceDrift(expectedValue.subtract(actualValue).abs());
    }

    private static BigDecimal confidenceValue(final String value) {
        final String checked = QdrRegressionSafety.requireSafeText(value, "confidenceBand")
                .toUpperCase(Locale.ROOT);
        return switch (checked) {
            case "LOW" -> new BigDecimal("0.25");
            case "MEDIUM" -> new BigDecimal("0.50");
            case "HIGH" -> new BigDecimal("0.75");
            case "BLOCKED", "UNKNOWN" -> BigDecimal.ZERO;
            default -> new BigDecimal(checked);
        };
    }

    private static int riskRank(final RiskLevel riskLevel) {
        return switch (Objects.requireNonNull(riskLevel, "riskLevel")) {
            case LOW -> 0;
            case MEDIUM -> 1;
            case HIGH -> 2;
            case BLOCKED -> 3;
            case UNKNOWN -> 4;
        };
    }

    private record ConfidenceDrift(BigDecimal drift) {
    }

    /**
     * QDR regression comparison input。
     *
     * <p>该 input 把 tenant/request/decision/trace 绑定到同一 comparison，避免 UUID-only 或
     * cross-tenant 比对。所有 summary/hash/ref 都必须是本地 deterministic safe metadata。
     *
     * @param tenantId                     tenant ID。
     * @param traceId                      traceId。
     * @param requestId                    requestId。
     * @param decisionId                   decisionId。
     * @param policy                       regression baseline policy。
     * @param expectedSummary              expected decision summary。
     * @param actualSummary                actual decision summary。
     * @param evidenceRefs                 safe evidence refs。
     * @param actualProviderSummaryHash    actual provider summary hash。
     * @param actualModelGatewayVersionRef actual model gateway version ref。
     * @param actualPromptVersionRef       actual prompt version ref。
     * @param actualPolicyVersion          actual policy version。
     */
    public record ComparisonInput(
            String tenantId,
            String traceId,
            String requestId,
            String decisionId,
            RegressionBaselinePolicy policy,
            ExpectedDecisionSummary expectedSummary,
            ExpectedDecisionSummary actualSummary,
            List<RegressionEvidenceRef> evidenceRefs,
            String actualProviderSummaryHash,
            String actualModelGatewayVersionRef,
            String actualPromptVersionRef,
            String actualPolicyVersion) {

        /**
         * 校验 comparison input 的身份绑定和 safe metadata。
         */
        public ComparisonInput {
            tenantId = QdrRegressionSafety.requireTenantId(tenantId);
            traceId = QdrRegressionSafety.requireSafeText(traceId, "traceId");
            requestId = QdrRegressionSafety.requireSafeText(requestId, "requestId");
            decisionId = QdrRegressionSafety.requireSafeText(decisionId, "decisionId");
            policy = Objects.requireNonNull(policy, "policy");
            expectedSummary = ReplayPersistenceGuard.requireSummary(expectedSummary);
            actualSummary = ReplayPersistenceGuard.requireSummary(actualSummary);
            evidenceRefs = List.copyOf(Objects.requireNonNullElse(evidenceRefs, List.of()));
            if (evidenceRefs.isEmpty()) {
                throw new IllegalArgumentException("evidenceRefs must not be empty");
            }
            actualProviderSummaryHash =
                    QdrRegressionSafety.requireSha256Hex(
                            actualProviderSummaryHash, "actualProviderSummaryHash");
            actualModelGatewayVersionRef =
                    QdrRegressionSafety.requireSafeText(
                            actualModelGatewayVersionRef, "actualModelGatewayVersionRef");
            actualPromptVersionRef =
                    QdrRegressionSafety.requireSafeText(
                            actualPromptVersionRef, "actualPromptVersionRef");
            actualPolicyVersion =
                    QdrRegressionSafety.requireSafeText(actualPolicyVersion, "actualPolicyVersion");
        }

        private String firstEvidenceRef() {
            return evidenceRefs.getFirst().compactRef();
        }
    }
}
