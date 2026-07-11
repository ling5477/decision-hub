package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayAuditEventView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayContextView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayOutputView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayRequestView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTimelineView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayTraceStepView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQuery;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ObservabilityReportService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ProviderReadinessGuardService;
import com.guidinglight.decisionhub.usecase.qdr.gateway.SaveModelGatewayCallCommand;
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
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveEvaluationCaseCommand;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveRegressionFindingCommand;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveRegressionVerdictCommand;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveReplayCaseCommand;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Stage-QDR-6 B2 evidence aggregation 的纯单元回归测试。 */
class DecisionEvidenceAggregateServiceTest {

    private static final String TENANT = "tenant-a";
    private static final String TRACE = "trace-a";
    private static final String REQUEST = "request-a";
    private static final String DECISION = "decision-a";
    private static final String RUN = "00000000-0000-0000-0000-000000000101";
    private static final Instant NOW = Instant.parse("2026-07-11T00:00:00Z");

    @Test
    void allMandatoryEvidenceProducesComplete() {
        final Fixture fixture = new Fixture();

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.COMPLETE, result.status());
        assertTrue(result.missingMandatoryEvidence().isEmpty());
    }

    @Test
    void missingRequestEvidenceProducesIncomplete() {
        final Fixture fixture = new Fixture();
        fixture.replayView = DecisionReplayView.incomplete(TENANT, DECISION, TRACE, REQUEST, List.of("REQUEST_MISSING"));

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INCOMPLETE, result.status());
        assertTrue(result.missingMandatoryEvidence().contains(DecisionEvidencePolicy.EvidenceType.REQUEST));
    }

    @Test
    void missingSnapshotEvidenceProducesIncomplete() {
        final Fixture fixture = new Fixture();
        fixture.replayView = DecisionReplayView.incomplete(TENANT, DECISION, TRACE, REQUEST, List.of("CONTEXT_MISSING"));

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INCOMPLETE, result.status());
        assertTrue(result.missingMandatoryEvidence().contains(DecisionEvidencePolicy.EvidenceType.CONTEXT_SNAPSHOT));
    }

    @Test
    void missingOptionalEvidenceDoesNotAffectCoreComplete() {
        final Fixture fixture = new Fixture();
        fixture.evidence = Optional.empty();

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.COMPLETE, result.status());
        assertFalse(result.evidenceRefs().stream().anyMatch(ref ->
                ref.evidenceType() == DecisionEvidencePolicy.EvidenceType.PROVIDER_HEALTH));
    }

    @Test
    void missingQueryCorrelationFailsClosed() {
        final DecisionEvidenceAggregate result = new Fixture().service().aggregate(null, DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "CORRELATION_KEY_REQUIRED");
    }

    @Test
    void crossTenantSourceIsInvalid() {
        final Fixture fixture = new Fixture();
        fixture.replayView = foundView("tenant-b", TRACE, REQUEST, DECISION, false, false);

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "CORRELATION_MISMATCH");
    }

    @Test
    void traceMismatchIsInvalid() {
        final Fixture fixture = new Fixture();
        fixture.replayView = foundView(TENANT, "trace-b", REQUEST, DECISION, false, false);

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "CORRELATION_MISMATCH");
    }

    @Test
    void requestMismatchIsInvalid() {
        final Fixture fixture = new Fixture();
        fixture.replayView = foundView(TENANT, TRACE, "request-b", DECISION, false, false);

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "CORRELATION_MISMATCH");
    }

    @Test
    void decisionMismatchIsInvalid() {
        final Fixture fixture = new Fixture();
        fixture.replayView = foundView(TENANT, TRACE, REQUEST, "decision-b", false, false);

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "CORRELATION_MISMATCH");
    }

    @Test
    void v5V6CorrelationConflictIsInvalid() {
        final Fixture fixture = new Fixture();
        fixture.detail = Optional.of(detail(TENANT, TRACE, "request-b"));

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "CORRELATION_MISMATCH");
    }

    @Test
    void duplicateContradictoryEvidenceIsInvalid() {
        final Fixture fixture = new Fixture();
        fixture.replayView = foundView(TENANT, TRACE, REQUEST, DECISION, true, false);

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "EVIDENCE_REF_INVALID");
    }

    @Test
    void sourceReadExceptionReturnsStructuredFailure() {
        final Fixture fixture = new Fixture();
        fixture.failReplay = true;

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "SOURCE_READ_FAILED");
        assertFalse(result.findings().stream().anyMatch(finding -> finding.message().contains("synthetic")));
    }

    @Test
    void unsafeEvidenceRefIsRejected() {
        final Fixture fixture = new Fixture();
        fixture.replayView = foundView(TENANT, TRACE, REQUEST, DECISION, false, true);

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "UNSAFE_EVIDENCE_REJECTED");
    }

    @Test
    void outputContainsNoTradingOrAuthorizationSemantics() {
        final DecisionEvidenceAggregate result = new Fixture().service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);
        final String output = result.toString().toUpperCase();

        List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "AUTHORIZATION", "LIVE_PERMISSION")
                .forEach(word -> assertFalse(output.contains(word), word));
    }

    @Test
    void evidenceRefsAndFindingsHaveStableOrder() {
        final Fixture first = new Fixture();
        first.replayView = foundView(TENANT, TRACE, REQUEST, DECISION, true, false);
        final Fixture second = new Fixture();
        second.replayView = foundView(TENANT, TRACE, REQUEST, DECISION, true, false);

        final DecisionEvidenceAggregate left = first.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);
        final DecisionEvidenceAggregate right = second.service().aggregate(coreQuery(), DecisionEvidencePolicy.CORE_DECISION);

        assertEquals(left.evidenceRefs(), right.evidenceRefs());
        assertEquals(left.findings(), right.findings());
    }

    @Test
    void policyMismatchFailsClosedWithoutReadingSources() {
        final Fixture fixture = new Fixture();

        final DecisionEvidenceAggregate result = fixture.service().aggregate(coreQuery(), DecisionEvidencePolicy.DETERMINISTIC_REPLAY);

        assertEquals(DecisionEvidenceStatus.INVALID, result.status());
        assertFinding(result, "POLICY_MISMATCH");
        assertEquals(0, fixture.replayReads);
    }

    @Test
    void productionPackageAddsNoRepositoryJdbcHttpProviderClientNqOrAgentClass() throws Exception {
        final Path sourceRoot = Path.of("src/main/java/com/guidinglight/decisionhub/usecase/qdr/evidence");
        final String sources;
        try (var paths = Files.list(sourceRoot)) {
            sources = paths.filter(path -> path.toString().endsWith(".java"))
                    .map(DecisionEvidenceAggregateServiceTest::read)
                    .reduce("", String::concat);
        }

        List.of(
                        "class DecisionEvidenceReaderPort",
                        "java.sql",
                        "JdbcTemplate",
                        "WebClient",
                        "RestTemplate",
                        "java.net.http",
                        "ModelProviderPort",
                        "usecase.agent",
                        "nexusquant")
                .forEach(forbidden -> assertFalse(sources.contains(forbidden), forbidden));
    }

    private static DecisionEvidenceQuery coreQuery() {
        return new DecisionEvidenceQuery(
                TENANT, TRACE, REQUEST, DECISION, RUN, null, null, null, null,
                DecisionEvidencePolicy.CORE_DECISION);
    }

    private static DecisionReplayView foundView(
            final String tenantId,
            final String traceId,
            final String requestId,
            final String decisionId,
            final boolean duplicateTrace,
            final boolean unsafeAudit) {
        final List<DecisionReplayTraceStepView> traces = new ArrayList<>();
        traces.add(traceStep(tenantId, traceId, decisionId, "trace-step-1", "CONTEXT"));
        if (duplicateTrace) {
            traces.add(traceStep(tenantId, traceId, decisionId, "trace-step-1", "POLICY"));
        }
        final DecisionReplayAuditEventView audit = new DecisionReplayAuditEventView(
                "audit-1",
                decisionId,
                tenantId,
                traceId,
                unsafeAudit ? "raw_prompt" : "DECISION_RECORDED",
                "RECORDED",
                Map.of("summary", "safe"),
                null,
                NOW);
        return DecisionReplayView.found(
                new DecisionReplayRequestView(
                        decisionId,
                        requestId,
                        traceId,
                        tenantId,
                        "unit-test",
                        DecisionType.READ_ONLY_RECOMMENDATION,
                        Map.of("subject", "safe"),
                        "context-safe-ref",
                        NOW,
                        "1.0.0",
                        NOW),
                new DecisionReplayContextView(
                        decisionId,
                        tenantId,
                        traceId,
                        Map.of("snapshot", "present"),
                        List.of("evidence-safe-ref"),
                        NOW),
                new DecisionReplayOutputView(
                        decisionId,
                        tenantId,
                        traceId,
                        requestId,
                        DecisionType.READ_ONLY_RECOMMENDATION,
                        DecisionAction.NO_TRADE,
                        DecisionRiskLevel.LOW,
                        DecisionPolicyStatus.ALLOWED,
                        new BigDecimal("0.5"),
                        Map.of("action", "NO_TRADE"),
                        NOW),
                new DecisionReplayTimelineView(traces, List.of(), List.of(audit)));
    }

    private static DecisionReplayTraceStepView traceStep(
            final String tenantId,
            final String traceId,
            final String decisionId,
            final String id,
            final String stepName) {
        return new DecisionReplayTraceStepView(
                id, decisionId, tenantId, traceId, stepName, "SUCCEEDED", NOW, NOW, null, null, NOW);
    }

    private static DecisionRunDetailView detail(
            final String tenantId, final String traceId, final String requestId) {
        return new DecisionRunDetailView(
                "00000000-0000-0000-0000-000000000100",
                RUN,
                tenantId,
                traceId,
                requestId,
                "request-key-a",
                "DECISION_REVIEW",
                "INTERNAL_TEST",
                "source-safe-ref",
                1,
                DecisionRunStatus.SUCCEEDED,
                NOW,
                NOW.plusMillis(1),
                1L,
                null,
                null,
                "signal-summary-safe",
                "decision-summary-safe",
                "output-summary-safe",
                NOW);
    }

    private static void assertFinding(final DecisionEvidenceAggregate result, final String code) {
        assertTrue(result.findings().stream().anyMatch(finding -> finding.code().equals(code)), code);
    }

    private static String read(final Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (final Exception error) {
            throw new IllegalStateException("source guard read failed", error);
        }
    }

    private static final class Fixture {

        private DecisionReplayView replayView = foundView(TENANT, TRACE, REQUEST, DECISION, false, false);
        private Optional<DecisionRunDetailView> detail = Optional.of(detail(TENANT, TRACE, REQUEST));
        private Optional<DecisionEvidenceView> evidence = Optional.of(new DecisionEvidenceView(
                RUN, TENANT, TRACE, null, List.of(), null, null, RedactionStatus.SUMMARY_ONLY, null));
        private DecisionTraceTimelineView timeline =
                new DecisionTraceTimelineView(RUN, TENANT, TRACE, REQUEST, List.of());
        private boolean failReplay;
        private int replayReads;

        private DecisionEvidenceAggregateService service() {
            return new DecisionEvidenceAggregateService(
                    query -> {
                        replayReads++;
                        if (failReplay) {
                            throw new IllegalStateException("synthetic source failure");
                        }
                        return replayView;
                    },
                    new DecisionReadModelQueryPort() {
                        @Override
                        public Optional<DecisionRunDetailView> findDecisionRunDetail(
                                final DecisionRunReadQuery query) {
                            return detail;
                        }

                        @Override
                        public DecisionTraceTimelineView getDecisionTrace(
                                final DecisionTraceReadQuery query) {
                            return timeline;
                        }

                        @Override
                        public Optional<DecisionEvidenceView> findDecisionEvidence(
                                final DecisionEvidenceReadQuery query) {
                            return evidence;
                        }
                    },
                    new EmptyReplayCaseRepository(),
                    new EmptyEvaluationCaseRepository(),
                    new EmptyRegressionVerdictRepository(),
                    new EmptyGatewayCallPort(),
                    new ProviderReadinessGuardService(),
                    new ObservabilityReportService());
        }
    }

    private static final class EmptyReplayCaseRepository implements ReplayCaseRepository {

        @Override
        public ReplayCaseRecord save(final SaveReplayCaseCommand command) {
            throw new UnsupportedOperationException("write forbidden in unit stub");
        }

        @Override
        public Optional<ReplayCaseRecord> findById(final String tenantId, final UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<ReplayCaseRecord> findByCaseId(final String tenantId, final String caseId) {
            return Optional.empty();
        }

        @Override
        public List<ReplayCaseRecord> listByTraceId(
                final String tenantId, final String traceId, final int limit, final int offset) {
            return List.of();
        }

        @Override
        public List<ReplayCaseRecord> listBySourceRequestId(
                final String tenantId, final String sourceRequestId, final int limit, final int offset) {
            return List.of();
        }

        @Override
        public List<ReplayCaseRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            return List.of();
        }
    }

    private static final class EmptyEvaluationCaseRepository implements EvaluationCaseRepository {

        @Override
        public EvaluationCaseRecord save(final SaveEvaluationCaseCommand command) {
            throw new UnsupportedOperationException("write forbidden in unit stub");
        }

        @Override
        public Optional<EvaluationCaseRecord> findById(final String tenantId, final UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<EvaluationCaseRecord> findByEvaluationId(
                final String tenantId, final String evaluationId) {
            return Optional.empty();
        }

        @Override
        public List<EvaluationCaseRecord> listByCaseId(
                final String tenantId, final String caseId, final int limit, final int offset) {
            return List.of();
        }

        @Override
        public List<EvaluationCaseRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            return List.of();
        }
    }

    private static final class EmptyRegressionVerdictRepository implements RegressionVerdictRepository {

        @Override
        public RegressionVerdictRecord save(final SaveRegressionVerdictCommand command) {
            throw new UnsupportedOperationException("write forbidden in unit stub");
        }

        @Override
        public List<RegressionFindingRecord> saveFindings(
                final String tenantId,
                final String verdictId,
                final List<SaveRegressionFindingCommand> commands) {
            throw new UnsupportedOperationException("write forbidden in unit stub");
        }

        @Override
        public Optional<RegressionVerdictRecord> findById(final String tenantId, final UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<RegressionVerdictRecord> findByVerdictId(
                final String tenantId, final String verdictId) {
            return Optional.empty();
        }

        @Override
        public Optional<RegressionVerdictRecord> findByEvaluationId(
                final String tenantId, final String evaluationId) {
            return Optional.empty();
        }

        @Override
        public List<RegressionFindingRecord> listFindingsByVerdictId(
                final String tenantId, final String verdictId, final int limit, final int offset) {
            return List.of();
        }

        @Override
        public List<RegressionVerdictRecord> listByTenant(
                final String tenantId, final int limit, final int offset) {
            return List.of();
        }
    }

    private static final class EmptyGatewayCallPort implements ModelGatewayCallPersistencePort {

        @Override
        public ModelGatewayCallRecord save(final SaveModelGatewayCallCommand command) {
            throw new UnsupportedOperationException("write forbidden in unit stub");
        }

        @Override
        public Optional<ModelGatewayCallRecord> findByTenantAndModelCallRef(
                final String tenantId, final String modelCallRef) {
            return Optional.empty();
        }

        @Override
        public Optional<ModelGatewayCallRecord> findByTenantAndDecisionRunAndModelCallRef(
                final String tenantId,
                final UUID decisionRunId,
                final String modelCallRef) {
            return Optional.empty();
        }
    }
}
