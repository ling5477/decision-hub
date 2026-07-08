package com.guidinglight.decisionhub.usecase.qdr.replay;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Stage-QDR-4 B4 regression report 内部 read model service。
 *
 * <p>Service 只组合 B2 repository port 做 tenant-bound report 查询；不执行 replay，不调用 provider、
 * 不发 HTTP，不访问 NQ，不生成 trading signal，也不返回 raw prompt、raw provider response 或
 * credential-like fields。所有 repository 或 projection 失败都会转换为 fail-closed read model failure。
 */
public final class RegressionReadModelService {

    private final ReplayCaseRepository replayCaseRepository;
    private final EvaluationCaseRepository evaluationCaseRepository;
    private final RegressionVerdictRepository regressionVerdictRepository;

    /**
     * 创建 B4 regression read model service。
     *
     * @param replayCaseRepository        B2 replay case repository port。
     * @param evaluationCaseRepository    B2 evaluation case repository port。
     * @param regressionVerdictRepository B2 regression verdict repository port。
     */
    public RegressionReadModelService(
            final ReplayCaseRepository replayCaseRepository,
            final EvaluationCaseRepository evaluationCaseRepository,
            final RegressionVerdictRepository regressionVerdictRepository) {
        this.replayCaseRepository =
                Objects.requireNonNull(replayCaseRepository, "replayCaseRepository");
        this.evaluationCaseRepository =
                Objects.requireNonNull(evaluationCaseRepository, "evaluationCaseRepository");
        this.regressionVerdictRepository =
                Objects.requireNonNull(regressionVerdictRepository, "regressionVerdictRepository");
    }

    /**
     * 查询 regression report。
     *
     * @param query tenant-bound report 查询条件。
     * @return 当前 tenant 下的 regression report 列表；不存在或跨租户时返回空列表。
     */
    public List<RegressionReportView> findReports(final RegressionReportQuery query) {
        final RegressionReportQuery checked = Objects.requireNonNull(query, "query");
        try {
            return findReportsFailClosed(checked).stream()
                    .filter(report -> matches(checked, report))
                    .limit(checked.limit())
                    .toList();
        } catch (final ReplayPersistenceException error) {
            throw new ReplayPersistenceException("regression read model query failed closed", error);
        } catch (final RuntimeException error) {
            throw new ReplayPersistenceException("regression read model query failed closed", error);
        }
    }

    private List<RegressionReportView> findReportsFailClosed(final RegressionReportQuery query) {
        if (query.hasVerdictSelector()) {
            return regressionVerdictRepository.findByVerdictId(query.tenantId(), query.verdictId())
                    .flatMap(verdict -> assembleReport(query.tenantId(), verdict))
                    .stream()
                    .toList();
        }
        if (query.hasEvaluationSelector()) {
            return evaluationCaseRepository.findByEvaluationId(query.tenantId(), query.evaluationId())
                    .flatMap(evaluation -> reportByEvaluation(query.tenantId(), evaluation))
                    .stream()
                    .toList();
        }
        if (query.hasCaseSelector()) {
            return replayCaseRepository.findByCaseId(query.tenantId(), query.caseId())
                    .map(replayCase -> reportsByReplayCase(query, replayCase))
                    .orElseGet(List::of);
        }
        if (query.hasTraceSelector()) {
            return replayCaseRepository
                    .listByTraceId(query.tenantId(), query.traceId(), query.limit(), query.offset())
                    .stream()
                    .flatMap(replayCase -> reportsByReplayCase(query, replayCase).stream())
                    .toList();
        }
        if (query.hasSourceRequestSelector()) {
            return replayCaseRepository
                    .listBySourceRequestId(
                            query.tenantId(), query.sourceRequestId(), query.limit(), query.offset())
                    .stream()
                    .flatMap(replayCase -> reportsByReplayCase(query, replayCase).stream())
                    .toList();
        }
        return regressionVerdictRepository
                .listByTenant(query.tenantId(), query.limit(), query.offset())
                .stream()
                .flatMap(verdict -> assembleReport(query.tenantId(), verdict).stream())
                .toList();
    }

    private List<RegressionReportView> reportsByReplayCase(
            final RegressionReportQuery query, final ReplayCaseRecord replayCase) {
        return evaluationCaseRepository
                .listByCaseId(query.tenantId(), replayCase.caseId(), query.limit(), query.offset())
                .stream()
                .flatMap(evaluation -> reportByEvaluation(query.tenantId(), evaluation).stream())
                .toList();
    }

    private Optional<RegressionReportView> reportByEvaluation(
            final String tenantId, final EvaluationCaseRecord evaluation) {
        if (!tenantId.equals(evaluation.tenantId())) {
            return Optional.empty();
        }
        return regressionVerdictRepository
                .findByEvaluationId(tenantId, evaluation.evaluationId())
                .flatMap(verdict -> assembleReport(tenantId, verdict));
    }

    private Optional<RegressionReportView> assembleReport(
            final String tenantId, final RegressionVerdictRecord verdict) {
        if (!tenantId.equals(verdict.tenantId())) {
            return Optional.empty();
        }
        final Optional<ReplayCaseRecord> replayCase =
                replayCaseRepository.findByCaseId(tenantId, verdict.caseId());
        final Optional<EvaluationCaseRecord> evaluationCase =
                evaluationCaseRepository.findByEvaluationId(tenantId, verdict.evaluationId());
        if (replayCase.isEmpty() || evaluationCase.isEmpty()) {
            return Optional.empty();
        }
        final List<RegressionFindingRecord> findings =
                regressionVerdictRepository.listFindingsByVerdictId(
                        tenantId, verdict.verdictId(), ReplayPageRequest.MAX_LIMIT, 0);
        return Optional.of(RegressionReportView.from(
                replayCase.get(), evaluationCase.get(), verdict, findings));
    }

    private static boolean matches(
            final RegressionReportQuery query, final RegressionReportView report) {
        return textMatches(query.caseId(), report.caseId())
                && textMatches(query.evaluationId(), report.evaluationId())
                && textMatches(query.verdictId(), report.verdictId())
                && textMatches(query.traceId(), report.traceId())
                && textMatches(query.sourceRequestId(), report.sourceRequestId())
                && textMatches(query.sourceDecisionId(), report.sourceDecisionId())
                && (query.verdict() == null || query.verdict() == report.verdict())
                && (query.severity() == null || query.severity() == report.severity())
                && (query.createdFrom() == null || !report.createdAt().isBefore(query.createdFrom()))
                && (query.createdTo() == null || !report.createdAt().isAfter(query.createdTo()));
    }

    private static boolean textMatches(final String expected, final String actual) {
        return expected == null || expected.equals(actual);
    }
}
