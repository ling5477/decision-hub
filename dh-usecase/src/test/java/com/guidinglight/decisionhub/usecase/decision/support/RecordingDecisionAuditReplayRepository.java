package com.guidinglight.decisionhub.usecase.decision.support;

import com.guidinglight.decisionhub.domain.decision.DecisionReplayAuditEventView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayContextView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayOutputView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayProviderCallView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayRequestView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTimelineView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTraceStepView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQuery;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;

import java.util.ArrayList;
import java.util.List;

/**
 * K6 test-only audit/replay repository。
 *
 * <p>该类同时实现 K3 `DecisionAuditRepository` 与 K4 `DecisionReplayQueryRepository`，用于证明 orchestrator
 * 写入的 request / trace / provider / output / audit 能被现有 replay read model 读回。它只在测试内存中保存记录，
 * 不访问数据库、不访问 NQ、不调用 HTTP、不产生任何交易副作用。
 */
public final class RecordingDecisionAuditReplayRepository
        implements DecisionAuditRepository, DecisionReplayQueryRepository {

    private final List<DecisionPersistenceRecords.RequestRecord> requests = new ArrayList<>();
    private final List<DecisionPersistenceRecords.ContextSnapshotRecord> contexts = new ArrayList<>();
    private final List<DecisionPersistenceRecords.TraceStepRecord> traces = new ArrayList<>();
    private final List<DecisionPersistenceRecords.ProviderCallRecord> providerCalls = new ArrayList<>();
    private final List<DecisionPersistenceRecords.OutputRecord> outputs = new ArrayList<>();
    private final List<DecisionPersistenceRecords.AuditEventRecord> audits = new ArrayList<>();

    @Override
    public void saveRequest(final DecisionPersistenceRecords.RequestRecord record) {
        requests.add(record);
    }

    @Override
    public void saveContextSnapshot(final DecisionPersistenceRecords.ContextSnapshotRecord record) {
        contexts.add(record);
    }

    @Override
    public void saveTraceStep(final DecisionPersistenceRecords.TraceStepRecord record) {
        traces.add(record);
    }

    @Override
    public void saveProviderCall(final DecisionPersistenceRecords.ProviderCallRecord record) {
        providerCalls.add(record);
    }

    @Override
    public void saveOutput(final DecisionPersistenceRecords.OutputRecord record) {
        outputs.add(record);
    }

    @Override
    public void saveAuditEvent(final DecisionPersistenceRecords.AuditEventRecord record) {
        audits.add(record);
    }

    @Override
    public DecisionReplayView findReplay(final DecisionReplayQuery query) {
        final DecisionPersistenceRecords.RequestRecord request = firstRequest(query.decisionId());
        if (request == null) {
            return DecisionReplayView.notFound(query.tenantId(), query.decisionId());
        }
        final DecisionPersistenceRecords.ContextSnapshotRecord context = firstContext(query.decisionId());
        final DecisionPersistenceRecords.OutputRecord output = firstOutput(query.decisionId());
        if (context == null || output == null || traces.isEmpty() || providerCalls.isEmpty() || audits.isEmpty()) {
            return DecisionReplayView.incomplete(
                    request.tenantId(), request.decisionId(), request.traceId(), request.requestId(), List.of("K6_REPLAY_INCOMPLETE"));
        }
        return DecisionReplayView.found(
                requestView(request),
                contextView(context),
                outputView(output),
                new DecisionReplayTimelineView(
                        traces.stream().filter(record -> matches(record.decisionId(), query.decisionId())).map(this::traceView).toList(),
                        providerCalls.stream().filter(record -> matches(record.decisionId(), query.decisionId())).map(this::providerView).toList(),
                        audits.stream().filter(record -> matches(record.decisionId(), query.decisionId())).map(this::auditView).toList()));
    }

    /**
     * 返回记录到的 provider call 数量。
     *
     * @return provider call 记录数。
     */
    public int providerCallCount() {
        return providerCalls.size();
    }

    /**
     * 返回记录到的 output 数量。
     *
     * @return output 记录数。
     */
    public int outputCount() {
        return outputs.size();
    }

    /**
     * 返回记录到的 audit event 数量。
     *
     * @return audit event 记录数。
     */
    public int auditEventCount() {
        return audits.size();
    }

    /**
     * 返回最后一个 provider call 记录。
     *
     * @return provider call 记录。
     */
    public DecisionPersistenceRecords.ProviderCallRecord lastProviderCall() {
        return providerCalls.get(providerCalls.size() - 1);
    }

    private DecisionReplayRequestView requestView(final DecisionPersistenceRecords.RequestRecord record) {
        return new DecisionReplayRequestView(
                record.decisionId(),
                record.requestId(),
                record.traceId(),
                record.tenantId(),
                record.source(),
                record.decisionType(),
                record.subjectJson(),
                record.contextRef(),
                record.requestedAt(),
                record.schemaVersion(),
                record.createdAt());
    }

    private DecisionReplayContextView contextView(final DecisionPersistenceRecords.ContextSnapshotRecord record) {
        return new DecisionReplayContextView(
                record.decisionId(),
                record.tenantId(),
                record.traceId(),
                record.contextSnapshotJson(),
                record.evidenceRefsJson(),
                record.createdAt());
    }

    private DecisionReplayOutputView outputView(final DecisionPersistenceRecords.OutputRecord record) {
        return new DecisionReplayOutputView(
                record.decisionId(),
                record.tenantId(),
                record.traceId(),
                record.requestId(),
                record.decisionType(),
                record.action(),
                record.riskLevel(),
                record.policyStatus(),
                record.confidence(),
                record.outputJson(),
                record.createdAt());
    }

    private DecisionReplayTraceStepView traceView(final DecisionPersistenceRecords.TraceStepRecord record) {
        return new DecisionReplayTraceStepView(
                record.id(),
                record.decisionId(),
                record.tenantId(),
                record.traceId(),
                record.stepName().name(),
                record.stepStatus().name(),
                record.startedAt(),
                record.endedAt(),
                record.errorCode(),
                record.errorMessage(),
                record.createdAt());
    }

    private DecisionReplayProviderCallView providerView(
            final DecisionPersistenceRecords.ProviderCallRecord record) {
        return new DecisionReplayProviderCallView(
                record.id(),
                record.decisionId(),
                record.tenantId(),
                record.traceId(),
                record.providerName(),
                record.providerStatus(),
                record.latencyMs(),
                record.signalJson(),
                record.errorCode(),
                record.createdAt());
    }

    private DecisionReplayAuditEventView auditView(final DecisionPersistenceRecords.AuditEventRecord record) {
        return new DecisionReplayAuditEventView(
                record.id(),
                record.decisionId(),
                record.tenantId(),
                record.traceId(),
                record.eventType().name(),
                record.eventStatus().name(),
                record.eventJson(),
                record.errorCode(),
                record.createdAt());
    }

    private DecisionPersistenceRecords.RequestRecord firstRequest(final String decisionId) {
        return requests.stream()
                .filter(record -> matches(record.decisionId(), decisionId))
                .findFirst()
                .orElse(null);
    }

    private DecisionPersistenceRecords.ContextSnapshotRecord firstContext(final String decisionId) {
        return contexts.stream()
                .filter(record -> matches(record.decisionId(), decisionId))
                .findFirst()
                .orElse(null);
    }

    private DecisionPersistenceRecords.OutputRecord firstOutput(final String decisionId) {
        return outputs.stream()
                .filter(record -> matches(record.decisionId(), decisionId))
                .findFirst()
                .orElse(null);
    }

    private static boolean matches(final String left, final String right) {
        return left.equals(right);
    }
}
