package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQuery;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayObservabilityReport;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayObservabilitySummary;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ObservabilityReportCommand;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ObservabilityReportService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderHealthReadModelQuery;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderHealthReadModelService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderHealthReadModelView;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderLatencyBudgetSummary;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessEvaluationCommand;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessEvaluationResult;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessGuardService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessSignal;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderTrustDecisionSummary;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceTimelineView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionFindingRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRepository;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Stage-QDR-6 B2 的内部 tenant-bound decision evidence 聚合服务。
 *
 * <p>Service 只组合现有 V5/V6/V8/V9 ports 和 Stage-QDR-5 安全 read models，不新增 persistence
 * boundary，不修改现有查询合同，也不执行 deterministic replay。所有来源均只读；任何读取异常、映射异常、
 * 未知状态、跨租户或关联冲突都会转换为结构化 INVALID 结果，不泄露异常文本或原始 payload。
 *
 * <p>线程安全：依赖均为不可变引用，方法内状态不共享；实际线程安全还要求注入的 ports 自身支持并发只读。
 * 副作用：仅调用既有只读查询方法，不写库、不调用 HTTP/Provider/NQ/Agent/LangGraph，不产生任何授权。
 */
public final class DecisionEvidenceAggregateService {

    private static final int SOURCE_PAGE_LIMIT = 100;

    private final DecisionReplayQueryRepository replayQueryRepository;
    private final DecisionReadModelQueryPort decisionReadModelQueryPort;
    private final ReplayCaseRepository replayCaseRepository;
    private final EvaluationCaseRepository evaluationCaseRepository;
    private final RegressionVerdictRepository regressionVerdictRepository;
    private final ModelGatewayCallPersistencePort gatewayCallPersistencePort;
    private final ProviderReadinessGuardService readinessGuardService;
    private final ObservabilityReportService observabilityReportService;
    private final DecisionEvidenceCorrelationResolver correlationResolver;
    private final DecisionEvidenceConsistencyEvaluator consistencyEvaluator;

    /**
     * 创建只使用既有 ports/read models 的 evidence 聚合服务。
     *
     * @param replayQueryRepository V5 decision replay 只读 port。
     * @param decisionReadModelQueryPort V6 decision read model port。
     * @param replayCaseRepository V9 replay case port。
     * @param evaluationCaseRepository V9 evaluation case port。
     * @param regressionVerdictRepository V9 regression verdict port。
     * @param gatewayCallPersistencePort V8 model gateway call port；仅调用其 tenant-bound read 方法。
     * @param readinessGuardService Stage-QDR-5 readiness policy service。
     * @param observabilityReportService Stage-QDR-5 internal report service。
     */
    public DecisionEvidenceAggregateService(
            final DecisionReplayQueryRepository replayQueryRepository,
            final DecisionReadModelQueryPort decisionReadModelQueryPort,
            final ReplayCaseRepository replayCaseRepository,
            final EvaluationCaseRepository evaluationCaseRepository,
            final RegressionVerdictRepository regressionVerdictRepository,
            final ModelGatewayCallPersistencePort gatewayCallPersistencePort,
            final ProviderReadinessGuardService readinessGuardService,
            final ObservabilityReportService observabilityReportService) {
        this(
                replayQueryRepository,
                decisionReadModelQueryPort,
                replayCaseRepository,
                evaluationCaseRepository,
                regressionVerdictRepository,
                gatewayCallPersistencePort,
                readinessGuardService,
                observabilityReportService,
                new DecisionEvidenceCorrelationResolver(),
                new DecisionEvidenceConsistencyEvaluator());
    }

    /**
     * 创建可注入纯 resolver/evaluator 的服务，便于不依赖 Spring 的单元测试。
     *
     * @param replayQueryRepository V5 decision replay 只读 port。
     * @param decisionReadModelQueryPort V6 decision read model port。
     * @param replayCaseRepository V9 replay case port。
     * @param evaluationCaseRepository V9 evaluation case port。
     * @param regressionVerdictRepository V9 regression verdict port。
     * @param gatewayCallPersistencePort V8 model gateway call port。
     * @param readinessGuardService Stage-QDR-5 readiness service。
     * @param observabilityReportService Stage-QDR-5 observability report service。
     * @param correlationResolver 四键 correlation resolver。
     * @param consistencyEvaluator 完整度与冲突 evaluator。
     */
    public DecisionEvidenceAggregateService(
            final DecisionReplayQueryRepository replayQueryRepository,
            final DecisionReadModelQueryPort decisionReadModelQueryPort,
            final ReplayCaseRepository replayCaseRepository,
            final EvaluationCaseRepository evaluationCaseRepository,
            final RegressionVerdictRepository regressionVerdictRepository,
            final ModelGatewayCallPersistencePort gatewayCallPersistencePort,
            final ProviderReadinessGuardService readinessGuardService,
            final ObservabilityReportService observabilityReportService,
            final DecisionEvidenceCorrelationResolver correlationResolver,
            final DecisionEvidenceConsistencyEvaluator consistencyEvaluator) {
        this.replayQueryRepository = Objects.requireNonNull(replayQueryRepository, "replayQueryRepository");
        this.decisionReadModelQueryPort = Objects.requireNonNull(decisionReadModelQueryPort, "decisionReadModelQueryPort");
        this.replayCaseRepository = Objects.requireNonNull(replayCaseRepository, "replayCaseRepository");
        this.evaluationCaseRepository = Objects.requireNonNull(evaluationCaseRepository, "evaluationCaseRepository");
        this.regressionVerdictRepository =
                Objects.requireNonNull(regressionVerdictRepository, "regressionVerdictRepository");
        this.gatewayCallPersistencePort =
                Objects.requireNonNull(gatewayCallPersistencePort, "gatewayCallPersistencePort");
        this.readinessGuardService = Objects.requireNonNull(readinessGuardService, "readinessGuardService");
        this.observabilityReportService =
                Objects.requireNonNull(observabilityReportService, "observabilityReportService");
        this.correlationResolver = Objects.requireNonNull(correlationResolver, "correlationResolver");
        this.consistencyEvaluator = Objects.requireNonNull(consistencyEvaluator, "consistencyEvaluator");
    }

    /**
     * 聚合一次 tenant-bound evidence 查询。
     *
     * <p>query 与 policy 必须同时提供且一致。V6 只在 query 提供 decisionRunId 时读取；V8 只使用 V6
     * 返回的 safe provider call refs 做 tenant-bound 查询。缺少 selector/ref 由 B1 completeness 表达为
     * INCOMPLETE，绝不退化为 tenantless scan。
     *
     * @param query B1 evidence query；null 会返回结构化 INVALID。
     * @param policy 调用方声明的固定 completeness policy，必须与 query.policy 一致。
     * @return COMPLETE、INCOMPLETE 或 INVALID aggregate；不抛出来源异常。
     */
    public DecisionEvidenceAggregate aggregate(
            final DecisionEvidenceQuery query, final DecisionEvidencePolicy policy) {
        if (query == null || policy == null) {
            return invalidInput(query, policy, "CORRELATION_KEY_REQUIRED", "查询关联键或 policy 缺失");
        }
        if (query.policy() != policy) {
            return invalidInput(query, policy, "POLICY_MISMATCH", "查询 policy 与调用 policy 不一致");
        }

        final AggregationState state = new AggregationState(correlationResolver.resolve(query));
        if (!loadV5(query, state)) {
            return consistencyEvaluator.evaluate(query, state.refs, state.findings);
        }
        if (!loadV6(query, state) || state.hasBlockingCorrelationFinding()) {
            return consistencyEvaluator.evaluate(query, state.refs, state.findings);
        }
        if (!loadV9(query, state) || state.hasBlockingCorrelationFinding()) {
            return consistencyEvaluator.evaluate(query, state.refs, state.findings);
        }
        loadV8AndProviderEvidence(query, state);
        return consistencyEvaluator.evaluate(query, state.refs, state.findings);
    }

    private boolean loadV5(final DecisionEvidenceQuery query, final AggregationState state) {
        try {
            final DecisionReplayView view = Objects.requireNonNull(
                    replayQueryRepository.findReplay(new DecisionReplayQuery(
                            query.tenantId(), query.decisionId(), query.traceId(), query.requestId())),
                    "V5 replay view");
            if (view.replayStatus() == DecisionReplayStatus.TENANT_MISMATCH) {
                state.correlationFinding("TENANT_MISMATCH", "V5 replay 返回跨租户状态");
                return true;
            }
            if (view.replayStatus() == DecisionReplayStatus.BLOCKED
                    || view.replayStatus() == DecisionReplayStatus.CORRUPTED) {
                state.sourceFailure("V5_REPLAY", "V5 replay 状态不可安全读取");
                return false;
            }
            if (view.replayStatus() != DecisionReplayStatus.FOUND) {
                return true;
            }
            if (!correlationResolver.matches(
                    state.correlation,
                    view.tenantId(),
                    view.traceId(),
                    view.requestId(),
                    view.decisionId())) {
                state.correlationFinding("CORRELATION_MISMATCH", "V5 replay 主关联键不一致");
                return true;
            }
            if (!correlationResolver.matches(
                    state.correlation,
                    view.request().tenantId(),
                    view.request().traceId(),
                    view.request().requestId(),
                    view.request().decisionId())
                    || !correlationResolver.matches(
                    state.correlation,
                    view.output().tenantId(),
                    view.output().traceId(),
                    view.output().requestId(),
                    view.output().decisionId())
                    || !state.correlation.tenantId().equals(view.context().tenantId())
                    || !state.correlation.traceId().equals(view.context().traceId())
                    || !state.correlation.decisionId().equals(view.context().decisionId())) {
                state.correlationFinding("CORRELATION_MISMATCH", "V5 request/context/output 关联冲突");
                return true;
            }

            addRef(state, query, DecisionEvidencePolicy.EvidenceType.REQUEST,
                    "v5-request:" + view.request().requestId(), null, "V5_REQUEST");
            addRef(state, query, DecisionEvidencePolicy.EvidenceType.CONTEXT_SNAPSHOT,
                    "v5-context:" + view.context().decisionId(), null, "V5_CONTEXT");
            addRef(state, query, DecisionEvidencePolicy.EvidenceType.DECISION_OUTPUT,
                    "v5-output:" + view.output().decisionId(), null, "V5_OUTPUT");
            view.timeline().auditEvents().forEach(event -> {
                if (!state.correlation.tenantId().equals(event.tenantId())
                        || !state.correlation.traceId().equals(event.traceId())
                        || !state.correlation.decisionId().equals(event.decisionId())) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V5 audit event 关联冲突");
                    return;
                }
                addRef(state, query, DecisionEvidencePolicy.EvidenceType.AUDIT_EVENT,
                        "v5-audit:" + event.id(), null, "V5_AUDIT_" + event.eventType());
            });
            view.timeline().traceSteps().forEach(step -> {
                if (!state.correlation.tenantId().equals(step.tenantId())
                        || !state.correlation.traceId().equals(step.traceId())
                        || !state.correlation.decisionId().equals(step.decisionId())) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V5 trace step 关联冲突");
                    return;
                }
                addRef(state, query, DecisionEvidencePolicy.EvidenceType.TRACE_STEP,
                        "v5-trace:" + step.id(), null, "V5_TRACE_" + step.stepName());
            });
            return true;
        } catch (final RuntimeException error) {
            state.sourceFailure("V5_REPLAY", "V5 replay source 读取失败");
            return false;
        }
    }

    private boolean loadV6(final DecisionEvidenceQuery query, final AggregationState state) {
        if (query.decisionRunId() == null) {
            return true;
        }
        try {
            final DecisionRunReadQuery runQuery = new DecisionRunReadQuery(
                    query.tenantId(), query.decisionRunId(), null, query.traceId());
            final Optional<DecisionRunDetailView> detailResult =
                    decisionReadModelQueryPort.findDecisionRunDetail(runQuery);
            final DecisionRunDetailView detail = detailResult == null ? null : detailResult.orElse(null);
            if (detail != null) {
                if (!correlationResolver.matchesWithoutDecision(
                        state.correlation, detail.tenantId(), detail.traceId(), detail.requestId())
                        || !query.decisionRunId().equals(detail.decisionRunId())) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V6 decision run 关联冲突");
                    return true;
                }
                addRef(state, query, DecisionEvidencePolicy.EvidenceType.RUN,
                        "v6-run:" + detail.decisionRunId(), null, "V6_RUN");
            }

            final Optional<DecisionEvidenceView> evidenceResult = decisionReadModelQueryPort.findDecisionEvidence(
                    new DecisionEvidenceReadQuery(
                            query.tenantId(), query.decisionRunId(), null, query.traceId()));
            final DecisionEvidenceView evidence = evidenceResult == null ? null : evidenceResult.orElse(null);
            if (evidence != null) {
                if (!state.correlation.tenantId().equals(evidence.tenantId())
                        || !state.correlation.traceId().equals(evidence.traceId())
                        || !query.decisionRunId().equals(evidence.decisionRunId())) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V6 evidence view 关联冲突");
                    return true;
                }
                if (evidence.contextSnapshotRef() != null) {
                    addRef(state, query, DecisionEvidencePolicy.EvidenceType.CONTEXT_SNAPSHOT,
                            evidence.contextSnapshotRef(), null, "V6_CONTEXT");
                }
                if (evidence.decisionOutputRef() != null) {
                    addRef(state, query, DecisionEvidencePolicy.EvidenceType.DECISION_OUTPUT,
                            evidence.decisionOutputRef(), null, "V6_OUTPUT");
                }
                state.gatewayCallRefs.addAll(evidence.providerCallLogRefs());
            }

            final DecisionTraceTimelineView trace = decisionReadModelQueryPort.getDecisionTrace(
                    new DecisionTraceReadQuery(
                            query.tenantId(), query.decisionRunId(), null, query.traceId()));
            if (trace != null) {
                if (!correlationResolver.matchesWithoutDecision(
                        state.correlation, trace.tenantId(), trace.traceId(), trace.requestId())
                        || !query.decisionRunId().equals(trace.decisionRunId())) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V6 trace timeline 关联冲突");
                    return true;
                }
                trace.steps().forEach(step -> addRef(
                        state,
                        query,
                        DecisionEvidencePolicy.EvidenceType.TRACE_STEP,
                        "v6-trace:" + step.stepId(),
                        null,
                        "V6_TRACE_" + step.stepName()));
                trace.steps().stream()
                        .map(step -> step.providerCallRef())
                        .filter(Objects::nonNull)
                        .forEach(state.gatewayCallRefs::add);
            }
            return true;
        } catch (final RuntimeException error) {
            state.sourceFailure("V6_READ_MODEL", "V6 read model source 读取失败");
            return false;
        }
    }

    private boolean loadV9(final DecisionEvidenceQuery query, final AggregationState state) {
        if (query.policy() == DecisionEvidencePolicy.CORE_DECISION) {
            return true;
        }
        try {
            final List<ReplayCaseRecord> replayCases = query.caseId() == null
                    ? safeList(replayCaseRepository.listByTraceId(
                    query.tenantId(), query.traceId(), SOURCE_PAGE_LIMIT, 0))
                    : optionalList(replayCaseRepository.findByCaseId(query.tenantId(), query.caseId()));
            for (final ReplayCaseRecord record : replayCases) {
                if (!matchesV9(state.correlation, record.tenantId(), record.traceId(), record.requestId(),
                        record.sourceDecisionId(), record.sourceRequestId())) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V9 replay case 关联冲突");
                    return true;
                }
                addRef(state, query, DecisionEvidencePolicy.EvidenceType.QDR_REPLAY_CASE,
                        "v9-replay:" + record.caseId(), record.caseChecksum(), "V9_REPLAY_CASE");
            }

            final List<EvaluationCaseRecord> evaluations = new ArrayList<>();
            if (query.evaluationId() != null) {
                evaluations.addAll(optionalList(evaluationCaseRepository.findByEvaluationId(
                        query.tenantId(), query.evaluationId())));
            } else {
                replayCases.forEach(record -> evaluations.addAll(safeList(
                        evaluationCaseRepository.listByCaseId(
                                query.tenantId(), record.caseId(), SOURCE_PAGE_LIMIT, 0))));
            }
            for (final EvaluationCaseRecord record : evaluations) {
                if (!matchesV9(state.correlation, record.tenantId(), record.traceId(), record.requestId(),
                        record.sourceDecisionId(), record.sourceRequestId())) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V9 evaluation case 关联冲突");
                    return true;
                }
                addRef(state, query, DecisionEvidencePolicy.EvidenceType.QDR_EVALUATION_CASE,
                        "v9-evaluation:" + record.evaluationId(), record.evaluationChecksum(),
                        "V9_EVALUATION_CASE");
            }

            final List<RegressionVerdictRecord> verdicts = new ArrayList<>();
            if (query.verdictId() != null) {
                verdicts.addAll(optionalList(regressionVerdictRepository.findByVerdictId(
                        query.tenantId(), query.verdictId())));
            } else {
                evaluations.forEach(record -> verdicts.addAll(optionalList(
                        regressionVerdictRepository.findByEvaluationId(
                                query.tenantId(), record.evaluationId()))));
            }
            for (final RegressionVerdictRecord record : verdicts) {
                if (!matchesV9(state.correlation, record.tenantId(), record.traceId(), record.requestId(),
                        record.sourceDecisionId(), record.sourceRequestId())) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V9 regression verdict 关联冲突");
                    return true;
                }
                addRef(state, query, DecisionEvidencePolicy.EvidenceType.REGRESSION_VERDICT,
                        "v9-verdict:" + record.verdictId(), record.expectedSummaryHash(),
                        "V9_REGRESSION_VERDICT");
                final List<RegressionFindingRecord> findings = safeList(
                        regressionVerdictRepository.listFindingsByVerdictId(
                                query.tenantId(), record.verdictId(), SOURCE_PAGE_LIMIT, 0));
                for (final RegressionFindingRecord finding : findings) {
                    if (!matchesV9(state.correlation, finding.tenantId(), finding.traceId(), finding.requestId(),
                            finding.sourceDecisionId(), finding.sourceRequestId())) {
                        state.correlationFinding("CORRELATION_MISMATCH", "V9 regression finding 关联冲突");
                        return true;
                    }
                    addRef(state, query, DecisionEvidencePolicy.EvidenceType.REGRESSION_FINDING,
                            "v9-finding:" + finding.id(), finding.expectedSummaryHash(),
                            "V9_REGRESSION_FINDING");
                }
            }
            return true;
        } catch (final RuntimeException error) {
            state.sourceFailure("V9_REPLAY_EVALUATION", "V9 replay/evaluation source 读取失败");
            return false;
        }
    }

    private void loadV8AndProviderEvidence(
            final DecisionEvidenceQuery query, final AggregationState state) {
        for (final String modelCallRef : state.gatewayCallRefs) {
            try {
                final Optional<ModelGatewayCallRecord> recordResult =
                        gatewayCallPersistencePort.findByTenantAndModelCallRef(query.tenantId(), modelCallRef);
                final ModelGatewayCallRecord record = recordResult == null ? null : recordResult.orElse(null);
                if (record == null) {
                    continue;
                }
                if (!correlationResolver.matchesWithoutDecision(
                        state.correlation, record.tenantId(), record.traceId(), record.requestId())
                        || query.decisionRunId() == null
                        || !query.decisionRunId().equals(record.decisionRunId().toString())
                        || (query.providerRef() != null
                        && !query.providerRef().equals(record.providerIdentityRef()))) {
                    state.correlationFinding("CORRELATION_MISMATCH", "V8 gateway call 关联冲突");
                    return;
                }
                projectProviderEvidence(query, record, state);
            } catch (final RuntimeException error) {
                state.sourceFailure("V8_GATEWAY_CALL", "V8 gateway call source 读取失败");
                return;
            }
        }
    }

    private void projectProviderEvidence(
            final DecisionEvidenceQuery query,
            final ModelGatewayCallRecord record,
            final AggregationState state) {
        final ProviderHealthReadModelView health = ProviderHealthReadModelService.fromGatewayCalls(List.of(record))
                .findProviderHealth(new ProviderHealthReadModelQuery(
                        query.tenantId(),
                        record.providerIdentityRef(),
                        null,
                        query.traceId(),
                        query.requestId(),
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        1,
                        0))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("provider health projection missing"));
        addRef(state, query, DecisionEvidencePolicy.EvidenceType.PROVIDER_HEALTH,
                "provider-health:" + record.modelCallRef(), health.providerSummaryHash(),
                "STAGE_QDR5_PROVIDER_HEALTH");

        final ModelGatewayObservabilitySummary summary = toSummary(health);
        final ProviderReadinessEvaluationResult readiness = readinessGuardService.evaluate(
                new ProviderReadinessEvaluationCommand(
                        query.tenantId(),
                        "evidence-source:" + record.modelCallRef(),
                        health.providerRef(),
                        health.modelGatewayVersionRef(),
                        health.providerSummaryHash(),
                        summary.failureClassification(),
                        summary.latencyBudgetSummary(),
                        summary.trustDecisionSummary(),
                        summary.readinessSignal(),
                        health,
                        "stage-qdr6-b2-readonly",
                        query.traceId(),
                        query.requestId(),
                        health.createdAt(),
                        health.lastObservedAt()));
        addRef(state, query, DecisionEvidencePolicy.EvidenceType.PROVIDER_READINESS,
                readiness.readinessSignal().readinessRef(), health.providerSummaryHash(),
                "STAGE_QDR5_PROVIDER_READINESS");

        final ModelGatewayObservabilityReport report = observabilityReportService.generate(
                new ObservabilityReportCommand(
                        query.tenantId(),
                        "evidence-source:" + record.modelCallRef(),
                        summary,
                        health,
                        readiness,
                        health.createdAt(),
                        health.lastObservedAt()));
        if (!correlationResolver.matchesWithoutDecision(
                state.correlation,
                report.tenantId(),
                report.traceId(),
                report.sourceRequestId())) {
            state.correlationFinding("CORRELATION_MISMATCH", "Provider observability report 关联冲突");
            return;
        }
        addRef(state, query, DecisionEvidencePolicy.EvidenceType.OBSERVABILITY_REPORT,
                "observability:" + record.modelCallRef(), report.providerSummaryHash(),
                "STAGE_QDR5_OBSERVABILITY");
        addRef(state, query, DecisionEvidencePolicy.EvidenceType.INTERNAL_ACCEPTANCE_RESULT,
                report.acceptanceEvidence().acceptanceEvidenceRef(), report.providerSummaryHash(),
                "STAGE_QDR5_INTERNAL_ACCEPTANCE");
    }

    private static ModelGatewayObservabilitySummary toSummary(
            final ProviderHealthReadModelView view) {
        return new ModelGatewayObservabilitySummary(
                view.tenantId(),
                view.traceId(),
                view.sourceRequestId(),
                view.modelGatewayVersionRef(),
                view.providerRef(),
                view.providerSummaryHash(),
                new ProviderLatencyBudgetSummary(
                        view.latencyBudget().p50Ms(),
                        view.latencyBudget().p95Ms(),
                        view.latencyBudget().p99Ms(),
                        view.latencyBudget().timeoutMs(),
                        view.latencyBudget().budgetUsedRatio(),
                        view.latencyBudget().sampleCount()),
                view.failureClassification().classification(),
                new ProviderTrustDecisionSummary(
                        view.trustDecision().decision(), view.trustDecision().decisionRef()),
                new ProviderReadinessSignal(
                        view.readinessSignal().status(),
                        view.readinessSignal().readinessRef(),
                        view.readinessSignal().findings()),
                view.createdAt());
    }

    private boolean matchesV9(
            final DecisionEvidenceCorrelation correlation,
            final String tenantId,
            final String traceId,
            final String requestId,
            final String sourceDecisionId,
            final String sourceRequestId) {
        return correlationResolver.matchesWithoutDecision(correlation, tenantId, traceId, requestId)
                && (sourceDecisionId == null || correlation.decisionId().equals(sourceDecisionId))
                && (sourceRequestId == null || correlation.requestId().equals(sourceRequestId));
    }

    private static DecisionEvidenceRef ref(
            final DecisionEvidenceQuery query,
            final DecisionEvidencePolicy.EvidenceType type,
            final String refId,
            final String hash,
            final String sourceType) {
        return new DecisionEvidenceRef(
                new DecisionEvidence(refId, type.name(), sourceType),
                query.correlation(),
                type,
                refId,
                hash,
                sourceType,
                query.policy().mandatoryEvidenceTypes().contains(type),
                RedactionStatus.SUMMARY_ONLY);
    }

    private static void addRef(
            final AggregationState state,
            final DecisionEvidenceQuery query,
            final DecisionEvidencePolicy.EvidenceType type,
            final String refId,
            final String hash,
            final String sourceType) {
        try {
            state.addRef(ref(query, type, refId, hash, sourceType));
        } catch (final RuntimeException error) {
            state.unsafeFinding("evidence-ref-unavailable");
        }
    }

    private static <T> List<T> optionalList(final Optional<T> optional) {
        return optional == null ? List.of() : optional.stream().toList();
    }

    private static <T> List<T> safeList(final List<T> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private static DecisionEvidenceAggregate invalidInput(
            final DecisionEvidenceQuery query,
            final DecisionEvidencePolicy requestedPolicy,
            final String code,
            final String message) {
        final DecisionEvidencePolicy policy = requestedPolicy == null
                ? DecisionEvidencePolicy.CORE_DECISION
                : requestedPolicy;
        final DecisionEvidenceCorrelation correlation = query == null
                ? new DecisionEvidenceCorrelation(
                "tenant-unavailable", "trace-unavailable", "request-unavailable", "decision-unavailable")
                : query.correlation();
        return new DecisionEvidenceAggregate(
                correlation,
                List.of(),
                DecisionEvidenceStatus.INVALID,
                List.of(new DecisionEvidenceFinding(
                        code,
                        DecisionEvidenceFinding.Severity.BLOCKER,
                        null,
                        null,
                        message)),
                policy.mandatoryEvidenceTypes().stream().sorted().toList());
    }

    private static final class AggregationState {

        private final DecisionEvidenceCorrelation correlation;
        private final List<DecisionEvidenceRef> refs = new ArrayList<>();
        private final List<DecisionEvidenceFinding> findings = new ArrayList<>();
        private final Set<String> gatewayCallRefs = new LinkedHashSet<>();

        private AggregationState(final DecisionEvidenceCorrelation correlation) {
            this.correlation = correlation;
        }

        private void addRef(final DecisionEvidenceRef evidenceRef) {
            try {
                refs.add(Objects.requireNonNull(evidenceRef, "evidenceRef"));
            } catch (final RuntimeException error) {
                unsafeFinding("evidence-ref-unavailable");
            }
        }

        private void correlationFinding(final String code, final String message) {
            findings.add(new DecisionEvidenceFinding(
                    code,
                    DecisionEvidenceFinding.Severity.BLOCKER,
                    null,
                    null,
                    message));
        }

        private void sourceFailure(final String safeRef, final String message) {
            findings.add(new DecisionEvidenceFinding(
                    "SOURCE_READ_FAILED",
                    DecisionEvidenceFinding.Severity.BLOCKER,
                    null,
                    safeRef,
                    message));
        }

        private void unsafeFinding(final String safeRef) {
            findings.add(new DecisionEvidenceFinding(
                    "UNSAFE_EVIDENCE_REJECTED",
                    DecisionEvidenceFinding.Severity.BLOCKER,
                    null,
                    safeRef,
                    "不安全 evidence ref 已被拒绝"));
        }

        private boolean hasBlockingCorrelationFinding() {
            return findings.stream().anyMatch(finding ->
                    finding.code().equals("CORRELATION_MISMATCH")
                            || finding.code().equals("TENANT_MISMATCH"));
        }
    }
}
