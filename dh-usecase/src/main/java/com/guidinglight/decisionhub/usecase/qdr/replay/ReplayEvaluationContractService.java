package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationCase;
import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationPolicy;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionFinding;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayCase;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * QDR replay / evaluation 合同服务。
 *
 * <p>B1 只做内存合同与 fail-closed 校验，不执行真实 replay、不落库、不调用 HTTP、不接真实 provider、
 * 不启动外部编排 runtime，也不产生 trading signal。
 */
public final class ReplayEvaluationContractService {

    private static final Pattern EXPECTED_SUMMARY_TRADING_TERMS =
            Pattern.compile("\\b(BUY|SELL|MARKET_ORDER)\\b");
    private static final Pattern FORBIDDEN_ACTION_TRADING_TERMS =
            Pattern.compile("\\b(PLACE_ORDER|CANCEL_ORDER|MUTATE_NQ_STATE)\\b");

    /**
     * 校验 replay case 基础完整性和只读安全边界。
     *
     * @param replayCase replay case 合同。
     * @return PASS 或 FAIL verdict；异常路径统一 FAIL。
     */
    public RegressionVerdict validateReplayCase(final ReplayCase replayCase) {
        try {
            final String failure = replayCaseFailure(replayCase);
            if (failure != null) {
                return fail(failure);
            }
            return RegressionVerdict.pass();
        } catch (final RuntimeException error) {
            return fail("REPLAY_CASE_VALIDATION_EXCEPTION");
        }
    }

    /**
     * 校验 evaluation case 基础完整性和只读安全边界。
     *
     * @param evaluationCase evaluation case 合同。
     * @return PASS 或 FAIL verdict；异常路径统一 FAIL。
     */
    public RegressionVerdict validateEvaluationCase(final EvaluationCase evaluationCase) {
        try {
            final String failure = evaluationCaseFailure(evaluationCase);
            if (failure != null) {
                return fail(failure);
            }
            return RegressionVerdict.pass();
        } catch (final RuntimeException error) {
            return fail("EVALUATION_CASE_VALIDATION_EXCEPTION");
        }
    }

    /**
     * 生成初始 regression verdict。
     *
     * <p>B1 默认不执行真实 replay；合同有效时返回 `SKIPPED`，合同无效时返回 `FAIL`。
     *
     * @param replayCase     replay case 合同。
     * @param evaluationCase evaluation case 合同，可为空。
     * @return 初始 verdict。
     */
    public RegressionVerdict createInitialVerdict(
            final ReplayCase replayCase, final EvaluationCase evaluationCase) {
        final RegressionVerdict replayVerdict = validateReplayCase(replayCase);
        if (replayVerdict.status() == RegressionVerdict.Status.FAIL) {
            return replayVerdict;
        }
        if (evaluationCase != null) {
            final RegressionVerdict evaluationVerdict = validateEvaluationCase(evaluationCase);
            if (evaluationVerdict.status() == RegressionVerdict.Status.FAIL) {
                return evaluationVerdict;
            }
        }
        return RegressionVerdict.skipped(
                "REPLAY_NOT_EXECUTED_IN_B1",
                List.of(
                        new RegressionFinding(
                                "REPLAY_NOT_EXECUTED_IN_B1",
                                RegressionSeverity.INFO,
                                "B1 only creates replay/evaluation contracts; real replay remains disabled.",
                                replayCase == null ? null : replayCase.caseId())));
    }

    private static String replayCaseFailure(final ReplayCase replayCase) {
        if (replayCase == null) {
            return "REPLAY_CASE_REQUIRED";
        }
        if (!hasText(replayCase.tenantId())) {
            return "TENANT_ID_REQUIRED";
        }
        if (!hasText(replayCase.caseId())) {
            return "CASE_ID_REQUIRED";
        }
        if (!hasText(replayCase.traceId())) {
            return "TRACE_ID_REQUIRED";
        }
        if (!hasText(replayCase.sourceDecisionId()) && !hasText(replayCase.sourceRequestId())) {
            return "SOURCE_DECISION_OR_REQUEST_REQUIRED";
        }
        final String inputRefFailure = inputRefFailure(replayCase.inputRef());
        if (inputRefFailure != null) {
            return inputRefFailure;
        }
        final String summaryFailure = expectedSummaryFailure(replayCase.expectedSummary());
        if (summaryFailure != null) {
            return summaryFailure;
        }
        final String policyFailure = policyFailure(replayCase.policy());
        if (policyFailure != null) {
            return policyFailure;
        }
        if (replayCase.createdAt() == null) {
            return "CREATED_AT_REQUIRED";
        }
        return null;
    }

    private static String evaluationCaseFailure(final EvaluationCase evaluationCase) {
        if (evaluationCase == null) {
            return "EVALUATION_CASE_REQUIRED";
        }
        if (!hasText(evaluationCase.tenantId())) {
            return "TENANT_ID_REQUIRED";
        }
        if (!hasText(evaluationCase.evaluationId())) {
            return "EVALUATION_ID_REQUIRED";
        }
        if (!hasText(evaluationCase.caseId())) {
            return "CASE_ID_REQUIRED";
        }
        if (!hasText(evaluationCase.policyVersion())) {
            return "POLICY_VERSION_REQUIRED";
        }
        if (!hasText(evaluationCase.modelVersionRef())
                && !hasText(evaluationCase.modelGatewayVersionRef())) {
            return "MODEL_OR_GATEWAY_VERSION_REF_REQUIRED";
        }
        final String expectedFailure = expectedSummaryFailure(evaluationCase.expectedSummary());
        if (expectedFailure != null) {
            return expectedFailure;
        }
        if (evaluationCase.actualSummary() != null) {
            final String actualFailure = expectedSummaryFailure(evaluationCase.actualSummary());
            if (actualFailure != null) {
                return actualFailure;
            }
        }
        if (evaluationCase.outputRef() != null) {
            return outputRefFailure(evaluationCase.outputRef());
        }
        return null;
    }

    private static String inputRefFailure(final ReplayInputRef inputRef) {
        if (inputRef == null) {
            return "INPUT_REF_REQUIRED";
        }
        if (!hasText(inputRef.refType()) || !hasText(inputRef.refId())) {
            return "INPUT_REF_REQUIRED";
        }
        return null;
    }

    private static String outputRefFailure(final ReplayOutputRef outputRef) {
        if (!hasText(outputRef.refType()) || !hasText(outputRef.refId())) {
            return "OUTPUT_REF_REQUIRED";
        }
        return null;
    }

    private static String expectedSummaryFailure(final ExpectedDecisionSummary summary) {
        if (summary == null) {
            return "EXPECTED_SUMMARY_REQUIRED";
        }
        if (!hasText(summary.decisionType())) {
            return "DECISION_TYPE_REQUIRED";
        }
        if (!hasText(summary.actionLabel())) {
            return "ACTION_LABEL_REQUIRED";
        }
        if (!hasText(summary.confidenceBand())) {
            return "CONFIDENCE_BAND_REQUIRED";
        }
        if (summary.riskLevel() == null) {
            return "RISK_LEVEL_REQUIRED";
        }
        if (summary.requiredEvidenceRefs().isEmpty()
                || summary.requiredEvidenceRefs().stream().anyMatch(value -> !hasText(value))) {
            return "REQUIRED_EVIDENCE_REFS_REQUIRED";
        }
        if (containsExpectedSummaryTradingTerm(summary.decisionType())
                || containsExpectedSummaryTradingTerm(summary.actionLabel())
                || containsExpectedSummaryTradingTerm(summary.confidenceBand())
                || summary.requiredEvidenceRefs().stream()
                .filter(Objects::nonNull)
                .anyMatch(ReplayEvaluationContractService::containsExpectedSummaryTradingTerm)
                || summary.forbiddenActions().stream()
                .filter(Objects::nonNull)
                .anyMatch(ReplayEvaluationContractService::containsExpectedSummaryTradingTerm)) {
            return "EXPECTED_SUMMARY_TRADING_TERM_FORBIDDEN";
        }
        if (summary.forbiddenActions().stream()
                .filter(Objects::nonNull)
                .anyMatch(ReplayEvaluationContractService::containsForbiddenActionTradingTerm)) {
            return "FORBIDDEN_ACTION_TRADING_MUTATION_TERM_FORBIDDEN";
        }
        return null;
    }

    private static String policyFailure(final EvaluationPolicy policy) {
        if (policy == null) {
            return "EVALUATION_POLICY_REQUIRED";
        }
        if (!hasText(policy.policyVersion())) {
            return "POLICY_VERSION_REQUIRED";
        }
        if (policy.confidenceTolerance() == null
                || policy.confidenceTolerance().compareTo(BigDecimal.ZERO) < 0
                || policy.confidenceTolerance().compareTo(BigDecimal.ONE) > 0) {
            return "CONFIDENCE_TOLERANCE_INVALID";
        }
        if (policy.riskLevelTolerance() < 0) {
            return "RISK_LEVEL_TOLERANCE_INVALID";
        }
        if (policy.requiredEvidenceMode() == null) {
            return "REQUIRED_EVIDENCE_MODE_REQUIRED";
        }
        if (policy.usesRawProviderResponseDependency()) {
            return "RAW_PROVIDER_RESPONSE_COMPARISON_FORBIDDEN";
        }
        if (policy.requiredEvidenceMode() == EvaluationPolicy.RequiredEvidenceMode.PROVIDER_SUMMARY_ONLY
                && !policy.allowProviderSummaryOnly()) {
            return "PROVIDER_SUMMARY_ONLY_NOT_ALLOWED";
        }
        return null;
    }

    private static RegressionVerdict fail(final String reason) {
        return RegressionVerdict.fail(
                reason,
                List.of(
                        new RegressionFinding(
                                reason,
                                RegressionSeverity.BLOCKER,
                                "QDR replay/evaluation contract failed closed.",
                                null)));
    }

    private static boolean containsExpectedSummaryTradingTerm(final String value) {
        return value != null && EXPECTED_SUMMARY_TRADING_TERMS.matcher(value).find();
    }

    private static boolean containsForbiddenActionTradingTerm(final String value) {
        return value != null && FORBIDDEN_ACTION_TRADING_TERMS.matcher(value).find();
    }

    private static boolean hasText(final String value) {
        return value != null && !value.isBlank();
    }
}
