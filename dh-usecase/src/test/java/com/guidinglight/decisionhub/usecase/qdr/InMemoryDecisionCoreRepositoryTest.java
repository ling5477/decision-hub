package com.guidinglight.decisionhub.usecase.qdr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.qdr.DecisionRequest;
import com.guidinglight.decisionhub.domain.qdr.DecisionRequestStatus;
import com.guidinglight.decisionhub.domain.qdr.DecisionRun;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.HumanApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.QuantDecision;
import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.QuantSignal;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

/**
 * stage-qdr-1 Decision Core repository 回归。
 *
 * <p>使用内存实现验证 repository port 的 insert/query 语义；不访问数据库、不调用 NQ、不产生交易副作用。
 */
class InMemoryDecisionCoreRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    @Test
    void insertAndQueryDecisionCoreMainlineRecords() {
        final InMemoryDecisionCoreRepository repository = new InMemoryDecisionCoreRepository();
        final UUID requestId = UUID.randomUUID();
        final UUID runId = UUID.randomUUID();
        final UUID signalId = UUID.randomUUID();

        repository.save(request(requestId));
        repository.save(run(runId, requestId));
        repository.save(signal(signalId, requestId));
        repository.save(decision(runId, signalId, QuantDecisionAction.OBSERVE));
        repository.complete(runId, DecisionRunStatus.SUCCEEDED, NOW, 12L, null, null);

        final DecisionRequest loadedRequest =
                repository.findByTenantIdAndRequestKey("tenant-a", "request-key-a").orElseThrow();
        assertEquals(requestId, loadedRequest.id());
        assertEquals("trace-a", loadedRequest.traceId());

        final List<DecisionRun> runs = repository.findByDecisionRequestId(requestId);
        assertEquals(1, runs.size());
        assertEquals(DecisionRunStatus.SUCCEEDED, runs.getFirst().status());

    final List<QuantSignal> signals = repository.findSignalsByDecisionRequestId(requestId);
        assertEquals(1, signals.size());
        assertEquals("UNKNOWN_REVIEW_INPUT", signals.getFirst().signalType());

        final List<QuantDecision> decisions = repository.findByDecisionRunId(runId);
        assertEquals(1, decisions.size());
        assertEquals(QuantDecisionAction.OBSERVE, decisions.getFirst().action());
        assertEquals(HumanApprovalStatus.NOT_REQUIRED, decisions.getFirst().humanApprovalStatus());
    }

    @Test
    void duplicateTenantRequestKeyFailsClosed() {
        final InMemoryDecisionCoreRepository repository = new InMemoryDecisionCoreRepository();
        repository.save(request(UUID.randomUUID()));

        final DecisionCorePersistenceException error =
                assertThrows(
                        DecisionCorePersistenceException.class,
                        () -> repository.save(request(UUID.randomUUID())));

        assertTrue(error.getMessage().contains("duplicate"));
    }

    @Test
    void illegalExecutableActionIsRejectedByDomainValidation() {
        assertThrows(IllegalArgumentException.class, () -> QuantDecisionAction.valueOf("BUY"));
    }

    private static DecisionRequest request(final UUID id) {
        return new DecisionRequest(
                id,
                "request-key-a",
                "QUANT_DECISION_REVIEW",
                "NQ_DRYRUN",
                "snapshot-a",
                "tenant-a",
                "trace-a",
                "request-a",
                Map.of("dryRun", true),
                Map.of("symbol", "BTC-USDT"),
                DecisionRequestStatus.ACCEPTED,
                NOW,
                NOW);
    }

    private static DecisionRun run(final UUID id, final UUID requestId) {
        return new DecisionRun(
                id,
                requestId,
                1,
                DecisionRunStatus.RUNNING,
                "DEFAULT_DECISION_ORCHESTRATOR",
                null,
                null,
                NOW,
                null,
                null,
                null,
                null,
                NOW);
    }

    private static QuantSignal signal(final UUID id, final UUID requestId) {
        return new QuantSignal(
                id,
                requestId,
                "NQ_DRYRUN",
                "BTC-USDT",
                "CRYPTO",
                "1h",
                "UNKNOWN_REVIEW_INPUT",
                Map.of("payloadClass", "SANITIZED_REVIEW_INPUT"),
                "strategy-a",
                null,
                null,
                NOW,
                NOW);
    }

    private static QuantDecision decision(
            final UUID runId, final UUID signalId, final QuantDecisionAction action) {
        return new QuantDecision(
                UUID.randomUUID(),
                signalId,
                runId,
                action,
                new BigDecimal("0.5000"),
                RiskLevel.LOW,
                "readonly",
                Map.of("readOnly", true),
                HumanApprovalStatus.NOT_REQUIRED,
                NOW);
    }
}
