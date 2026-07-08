package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionFinding;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;

import java.util.List;
import java.util.Objects;

/**
 * B3 mock gateway regression evaluation result。
 *
 * <p>结果只表示 replay/evaluation regression 状态和 B2 persistence refs；它不是交易建议，
 * 不触发 NQ execution，也不暴露 raw prompt、raw provider response 或 credential。
 *
 * @param verdict                 regression verdict。
 * @param replayCaseRecord        已保存 replay case，可为空。
 * @param evaluationCaseRecord    已保存 evaluation case，可为空。
 * @param regressionVerdictRecord 已保存 regression verdict，可为空。
 * @param findingRecords          已保存 finding records。
 * @param persistenceBlocked      true 表示 repository 保存失败，调用方必须 fail-closed。
 */
public record QdrRegressionEvaluationResult(
        RegressionVerdict verdict,
        ReplayCaseRecord replayCaseRecord,
        EvaluationCaseRecord evaluationCaseRecord,
        RegressionVerdictRecord regressionVerdictRecord,
        List<RegressionFindingRecord> findingRecords,
        boolean persistenceBlocked) {

    /**
     * 校验 result 字段，finding records 使用不可变复制。
     */
    public QdrRegressionEvaluationResult {
        verdict = Objects.requireNonNull(verdict, "verdict");
        findingRecords = List.copyOf(Objects.requireNonNullElse(findingRecords, List.of()));
    }

    /**
     * 创建 repository failure result。
     *
     * @param reason 固定失败原因。
     * @return fail-closed result。
     */
    public static QdrRegressionEvaluationResult blocked(final String reason) {
        final RegressionFinding finding = new RegressionFinding(
                "REGRESSION_REPOSITORY_BLOCKED",
                com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity.BLOCKER,
                QdrRegressionSafety.requireFindingMessage(reason),
                null);
        return new QdrRegressionEvaluationResult(
                RegressionVerdict.fail("QDR_REGRESSION_REPOSITORY_BLOCKED", List.of(finding)),
                null,
                null,
                null,
                List.of(),
                true);
    }
}
