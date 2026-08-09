package com.guidinglight.decisionhub.usecase.qdr.report;

import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionFeedbackEvidenceService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayObservabilityReport;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessEvaluationResult;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionReportView;
import com.guidinglight.decisionhub.usecase.qdr.replay.deterministic.DeterministicReplayResult;
import java.util.Objects;

/**
 * Stage-QDR-11 consolidated evidence 的只读 internal acceptance facade。
 *
 * <p>Facade 只调用一次既有 bounded evidence service，再把唯一 aggregate authority 交给既有 report
 * evaluator；不持久化、不重试，也不触发 Provider、HTTP、NQ、learning、Agent、LangGraph 或交易行为。
 */
public final class DecisionFeedbackInternalAcceptanceService {

  private final DecisionFeedbackEvidenceService evidenceService;
  private final DecisionEvidenceReplayReportService reportService;

  /** 创建唯一 consolidated evidence -> internal report 调用链。 */
  public DecisionFeedbackInternalAcceptanceService(
      final DecisionFeedbackEvidenceService evidenceService,
      final DecisionEvidenceReplayReportService reportService) {
    this.evidenceService = Objects.requireNonNull(evidenceService, "evidenceService");
    this.reportService = Objects.requireNonNull(reportService, "reportService");
  }

  /**
   * 在调用方声明的 tenant/environment/bounds 内生成无副作用 internal acceptance report。
   *
   * @throws IllegalStateException aggregate source 在 report 创建前异常时固定 fail-closed。
   */
  public DecisionEvidenceReplayInternalReport evaluate(
      final DecisionFeedbackEvidenceQuery query,
      final DeterministicReplayResult replay,
      final RegressionReportView regression,
      final ProviderReadinessEvaluationResult readiness,
      final ModelGatewayObservabilityReport observability) {
    final DecisionFeedbackEvidenceAggregate aggregate;
    try {
      aggregate =
          Objects.requireNonNull(
              evidenceService.aggregate(Objects.requireNonNull(query, "query")), "aggregate");
    } catch (final RuntimeException error) {
      throw new IllegalStateException("INTERNAL_ACCEPTANCE_FAILURE");
    }
    return reportService.generate(aggregate, replay, regression, readiness, observability);
  }
}
