package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Stage-QDR-4 B4 regression drift summary。
 *
 * <p>该 summary 只解释 replay/evaluation/regression 差异。所有 `PASS / WARN / FAIL / SKIPPED`
 * 都只是 regression verdict，不能被解释为交易信号；direction label 仍只是只读方向标签，不产生
 * order / risk / ledger / paper / live mutation。
 *
 * @param decisionTypeDrift           decisionType drift。
 * @param actionLabelDrift            actionLabel drift。
 * @param confidenceBandDrift         confidenceBand drift。
 * @param riskLevelDrift              riskLevel drift。
 * @param evidenceRefsDrift           evidenceRefs drift。
 * @param forbiddenActionsDrift       forbiddenActions drift。
 * @param providerSummaryHashDrift    providerSummaryHash drift。
 * @param modelGatewayVersionRefDrift modelGatewayVersionRef drift。
 * @param promptVersionRefDrift       promptVersionRef drift。
 * @param policyVersionDrift          policyVersion drift。
 */
public record RegressionDriftSummary(
        DriftState decisionTypeDrift,
        DriftState actionLabelDrift,
        DriftState confidenceBandDrift,
        DriftState riskLevelDrift,
        DriftState evidenceRefsDrift,
        DriftState forbiddenActionsDrift,
        DriftState providerSummaryHashDrift,
        DriftState modelGatewayVersionRefDrift,
        DriftState promptVersionRefDrift,
        DriftState policyVersionDrift) {

    /**
     * 校验 drift summary 所有分类均已显式给出。
     */
    public RegressionDriftSummary {
        decisionTypeDrift = Objects.requireNonNull(decisionTypeDrift, "decisionTypeDrift");
        actionLabelDrift = Objects.requireNonNull(actionLabelDrift, "actionLabelDrift");
        confidenceBandDrift = Objects.requireNonNull(confidenceBandDrift, "confidenceBandDrift");
        riskLevelDrift = Objects.requireNonNull(riskLevelDrift, "riskLevelDrift");
        evidenceRefsDrift = Objects.requireNonNull(evidenceRefsDrift, "evidenceRefsDrift");
        forbiddenActionsDrift = Objects.requireNonNull(forbiddenActionsDrift, "forbiddenActionsDrift");
        providerSummaryHashDrift = Objects.requireNonNull(providerSummaryHashDrift, "providerSummaryHashDrift");
        modelGatewayVersionRefDrift =
                Objects.requireNonNull(modelGatewayVersionRefDrift, "modelGatewayVersionRefDrift");
        promptVersionRefDrift = Objects.requireNonNull(promptVersionRefDrift, "promptVersionRefDrift");
        policyVersionDrift = Objects.requireNonNull(policyVersionDrift, "policyVersionDrift");
    }

    static RegressionDriftSummary from(
            final ReplayCaseRecord replayCase,
            final EvaluationCaseRecord evaluationCase,
            final RegressionVerdictRecord verdictRecord,
            final List<RegressionReportFindingView> findings) {
        final List<String> codes = findings.stream()
                .map(RegressionReportFindingView::findingCode)
                .map(code -> code.toUpperCase(Locale.ROOT))
                .toList();
        final ExpectedDecisionSummary expected = replayCase.expectedSummary();
        final ExpectedDecisionSummary actual = evaluationCase.actualSummary();
        return new RegressionDriftSummary(
                textDrift(expected.decisionType(), actual == null ? null : actual.decisionType()),
                actionDrift(codes, expected.actionLabel(), actual == null ? null : actual.actionLabel()),
                confidenceDrift(codes, expected.confidenceBand(), actual == null ? null : actual.confidenceBand()),
                riskDrift(codes, expected, actual),
                evidenceDrift(expected, actual),
                forbiddenActionsDrift(expected, actual),
                codeDrift(codes, "PROVIDER_SUMMARY_HASH_MISMATCH", evaluationCase.outputRef() == null),
                versionDrift(
                        codes,
                        "MODEL_GATEWAY_VERSION_REF_MISMATCH",
                        replayCase.modelGatewayVersionRef(),
                        firstPresent(
                                evaluationCase.modelGatewayVersionRef(),
                                verdictRecord.modelGatewayVersionRef())),
                codeDrift(codes, "PROMPT_VERSION_REF_MISMATCH", true),
                policyDrift(codes, replayCase.policyVersion(), evaluationCase.policyVersion(), verdictRecord.policyVersion()));
    }

    private static DriftState textDrift(final String expected, final String actual) {
        if (actual == null) {
            return DriftState.SKIPPED;
        }
        return Objects.equals(expected, actual) ? DriftState.MATCH : DriftState.MISMATCH;
    }

    private static DriftState actionDrift(
            final List<String> codes, final String expected, final String actual) {
        if (actual == null) {
            return DriftState.SKIPPED;
        }
        if (codes.contains("ACTION_LABEL_MISMATCH")) {
            return DriftState.MISMATCH;
        }
        return Objects.equals(expected, actual) ? DriftState.MATCH : DriftState.MISMATCH;
    }

    private static DriftState confidenceDrift(
            final List<String> codes, final String expected, final String actual) {
        if (actual == null) {
            return DriftState.SKIPPED;
        }
        if (codes.contains("CONFIDENCE_DRIFT_BEYOND_TOLERANCE")) {
            return DriftState.HARD_DRIFT;
        }
        if (codes.contains("CONFIDENCE_DRIFT_WITHIN_TOLERANCE")) {
            return DriftState.SOFT_DRIFT;
        }
        return Objects.equals(expected, actual) ? DriftState.MATCH : DriftState.SOFT_DRIFT;
    }

    private static DriftState riskDrift(
            final List<String> codes,
            final ExpectedDecisionSummary expected,
            final ExpectedDecisionSummary actual) {
        if (actual == null) {
            return DriftState.SKIPPED;
        }
        if (codes.contains("RISK_LEVEL_INCREASED")) {
            return DriftState.HARD_DRIFT;
        }
        if (codes.contains("RISK_LEVEL_DECREASED_WITH_WEAK_EVIDENCE")) {
            return DriftState.SOFT_DRIFT;
        }
        return Objects.equals(expected.riskLevel(), actual.riskLevel())
                ? DriftState.MATCH
                : DriftState.HARD_DRIFT;
    }

    private static DriftState evidenceDrift(
            final ExpectedDecisionSummary expected,
            final ExpectedDecisionSummary actual) {
        if (actual == null) {
            return DriftState.SKIPPED;
        }
        final List<String> expectedRefs = normalize(expected.requiredEvidenceRefs());
        final List<String> actualRefs = normalize(actual.requiredEvidenceRefs());
        if (!actualRefs.containsAll(expectedRefs)) {
            return DriftState.MISSING;
        }
        return actualRefs.size() == expectedRefs.size() ? DriftState.MATCH : DriftState.SUPERSET;
    }

    private static DriftState forbiddenActionsDrift(
            final ExpectedDecisionSummary expected,
            final ExpectedDecisionSummary actual) {
        if (actual == null) {
            return DriftState.SKIPPED;
        }
        final List<String> expectedActions = normalize(expected.forbiddenActions());
        final List<String> actualActions = normalize(actual.forbiddenActions());
        if (!actualActions.containsAll(expectedActions)) {
            return DriftState.MISSING;
        }
        return actualActions.size() == expectedActions.size() ? DriftState.MATCH : DriftState.EXTENDED;
    }

    private static DriftState codeDrift(
            final List<String> codes, final String mismatchCode, final boolean skippedWhenNoCode) {
        if (codes.contains(mismatchCode)) {
            return DriftState.DRIFT;
        }
        return skippedWhenNoCode ? DriftState.SKIPPED : DriftState.MATCH;
    }

    private static DriftState versionDrift(
            final List<String> codes,
            final String mismatchCode,
            final String expected,
            final String actual) {
        if (codes.contains(mismatchCode)) {
            return DriftState.DRIFT;
        }
        if (expected == null || actual == null) {
            return DriftState.SKIPPED;
        }
        return Objects.equals(expected, actual) ? DriftState.MATCH : DriftState.DRIFT;
    }

    private static DriftState policyDrift(
            final List<String> codes,
            final String replayPolicy,
            final String evaluationPolicy,
            final String verdictPolicy) {
        if (codes.contains("POLICY_VERSION_MISMATCH")) {
            return DriftState.DRIFT;
        }
        return Objects.equals(replayPolicy, evaluationPolicy) && Objects.equals(evaluationPolicy, verdictPolicy)
                ? DriftState.MATCH
                : DriftState.DRIFT;
    }

    private static List<String> normalize(final List<String> values) {
        return values.stream()
                .filter(Objects::nonNull)
                .map(value -> value.trim().toUpperCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .toList();
    }

    private static String firstPresent(final String left, final String right) {
        if (left != null) {
            return left;
        }
        return right;
    }

    /**
     * Drift 分类枚举。
     */
    public enum DriftState {
        /**
         * 字段匹配。
         */
        MATCH,

        /**
         * 字段不匹配。
         */
        MISMATCH,

        /**
         * 软漂移，可审计但不放宽安全边界。
         */
        SOFT_DRIFT,

        /**
         * 硬漂移，调用方必须 fail-closed 或转人工复核。
         */
        HARD_DRIFT,

        /**
         * 缺少预期 evidence / forbidden action。
         */
        MISSING,

        /**
         * 实际 evidence 覆盖并多于预期集合。
         */
        SUPERSET,

        /**
         * 实际 forbidden action 集合扩展了禁止项。
         */
        EXTENDED,

        /**
         * hash 或版本引用存在漂移。
         */
        DRIFT,

        /**
         * 该字段在当前 B2/V9 read material 中不可判定。
         */
        SKIPPED
    }
}
