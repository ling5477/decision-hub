package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;

import java.util.List;
import java.util.Objects;

/**
 * B3 QDR mock gateway regression evaluation service。
 *
 * <p>Service 串联 existing dry-run / QDR decision artifact、mock gateway safe summary、B2
 * replay/evaluation persistence、expected/actual summary、comparator、verdict 和 finding。
 * 它只调用既有 B2 repository ports，不新增 schema，不执行 replay API，不接 provider/HTTP/NQ，
 * 不启动 Agent/LangGraph，也不把 regression output 写成 trading signal。
 */
public final class QdrRegressionEvaluationService {

    private final ReplayCaseRepository replayCaseRepository;
    private final EvaluationCaseRepository evaluationCaseRepository;
    private final RegressionVerdictRepository regressionVerdictRepository;
    private final MockGatewayRegressionCaseBuilder caseBuilder;
    private final QdrRegressionComparator comparator;

    /**
     * 创建 B3 regression evaluation service。
     *
     * @param replayCaseRepository        B2 replay case repository port。
     * @param evaluationCaseRepository    B2 evaluation case repository port。
     * @param regressionVerdictRepository B2 verdict/finding repository port。
     * @param caseBuilder                 deterministic case builder。
     * @param comparator                  deterministic comparator。
     */
    public QdrRegressionEvaluationService(
            final ReplayCaseRepository replayCaseRepository,
            final EvaluationCaseRepository evaluationCaseRepository,
            final RegressionVerdictRepository regressionVerdictRepository,
            final MockGatewayRegressionCaseBuilder caseBuilder,
            final QdrRegressionComparator comparator) {
        this.replayCaseRepository =
                Objects.requireNonNull(replayCaseRepository, "replayCaseRepository");
        this.evaluationCaseRepository =
                Objects.requireNonNull(evaluationCaseRepository, "evaluationCaseRepository");
        this.regressionVerdictRepository =
                Objects.requireNonNull(regressionVerdictRepository, "regressionVerdictRepository");
        this.caseBuilder = Objects.requireNonNull(caseBuilder, "caseBuilder");
        this.comparator = Objects.requireNonNull(comparator, "comparator");
    }

    /**
     * 执行 B3 mock gateway regression flow。
     *
     * @param command regression evaluation command。
     * @return regression evaluation result；repository failure 时返回 FAIL/BLOCKER。
     */
    public QdrRegressionEvaluationResult evaluate(final QdrRegressionEvaluationCommand command) {
        final QdrRegressionEvaluationCommand checked = Objects.requireNonNull(command, "command");
        try {
            final RegressionVerdict verdict = comparator.compare(toComparisonInput(checked));
            final MockGatewayRegressionCaseBuilder.BuiltRegressionCase builtCase =
                    caseBuilder.build(checked, verdict);
            final ReplayCaseRecord replayRecord =
                    replayCaseRepository.save(
                            builtCase.toReplayCommand(
                                    checked.requestId(), checked.modelGatewayVersionRef()));
            final EvaluationCaseRecord evaluationRecord =
                    evaluationCaseRepository.save(
                            builtCase.toEvaluationCommand(
                                    checked.decisionId(),
                                    checked.sourceRequestId(),
                                    checked.traceId(),
                                    checked.requestId()));
            final RegressionVerdictRecord verdictRecord =
                    regressionVerdictRepository.save(builtCase.toVerdictCommand(checked));
            final List<RegressionFindingRecord> findingRecords =
                    regressionVerdictRepository.saveFindings(
                            checked.tenantId(),
                            builtCase.verdictId(),
                            builtCase.toFindingCommands(checked));
            return new QdrRegressionEvaluationResult(
                    verdict, replayRecord, evaluationRecord, verdictRecord, findingRecords, false);
        } catch (final ReplayPersistenceException error) {
            return QdrRegressionEvaluationResult.blocked("repository persistence blocked");
        } catch (final RuntimeException error) {
            return QdrRegressionEvaluationResult.blocked("regression evaluation failed closed");
        }
    }

    private static QdrRegressionComparator.ComparisonInput toComparisonInput(
            final QdrRegressionEvaluationCommand command) {
        return new QdrRegressionComparator.ComparisonInput(
                command.tenantId(),
                command.traceId(),
                command.requestId(),
                command.decisionId(),
                command.policy(),
                command.expectedSummary(),
                command.actualSummary(),
                command.evidenceRefs(),
                command.providerSummaryHash(),
                command.modelGatewayVersionRef(),
                command.promptVersionRef(),
                command.policy().policyVersion());
    }
}
