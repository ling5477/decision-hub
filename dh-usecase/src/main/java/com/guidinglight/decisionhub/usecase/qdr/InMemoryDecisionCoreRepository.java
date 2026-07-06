package com.guidinglight.decisionhub.usecase.qdr;

import com.guidinglight.decisionhub.domain.qdr.DecisionRequest;
import com.guidinglight.decisionhub.domain.qdr.DecisionRun;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.QuantDecision;
import com.guidinglight.decisionhub.domain.qdr.QuantSignal;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Decision Core repository 的内存实现。
 *
 * <p>该实现用于单元测试和本地兜底，不访问数据库、不调用 NQ、不产生任何交易副作用。生产 JDBC adapter
 * 必须保持与本实现相同的 fail-closed 语义。
 */
public final class InMemoryDecisionCoreRepository
        implements DecisionRequestRepository,
        DecisionRunRepository,
        QuantSignalRepository,
        QuantDecisionRepository {

    private final Map<UUID, DecisionRequest> requests = new LinkedHashMap<>();
    private final Map<UUID, DecisionRun> runs = new LinkedHashMap<>();
    private final Map<UUID, QuantSignal> signals = new LinkedHashMap<>();
    private final Map<UUID, QuantDecision> decisions = new LinkedHashMap<>();

    @Override
    public synchronized void save(final DecisionRequest request) {
        final DecisionRequest checked = Objects.requireNonNull(request, "request");
        final boolean duplicateKey =
                requests.values().stream()
                        .anyMatch(
                                existing ->
                                        existing.tenantId().equals(checked.tenantId())
                                                && existing.requestKey().equals(checked.requestKey()));
        if (duplicateKey || requests.containsKey(checked.id())) {
            throw new DecisionCorePersistenceException(
                    "decision request duplicate key", new IllegalStateException("duplicate request"));
        }
        requests.put(checked.id(), checked);
    }

    @Override
    public synchronized Optional<DecisionRequest> findByTenantIdAndRequestKey(
            final String tenantId, final String requestKey) {
        return requests.values().stream()
                .filter(
                        request -> request.tenantId().equals(tenantId) && request.requestKey().equals(requestKey))
                .findFirst();
    }

    @Override
    public synchronized void save(final DecisionRun run) {
        final DecisionRun checked = Objects.requireNonNull(run, "run");
        final boolean duplicateRunNo =
                runs.values().stream()
                        .anyMatch(
                                existing ->
                                        existing.decisionRequestId().equals(checked.decisionRequestId())
                                                && existing.runNo() == checked.runNo());
        if (duplicateRunNo || runs.containsKey(checked.id())) {
            throw new DecisionCorePersistenceException(
                    "decision run duplicate key", new IllegalStateException("duplicate run"));
        }
        runs.put(checked.id(), checked);
    }

    @Override
    public synchronized void complete(
            final UUID id,
            final DecisionRunStatus status,
            final Instant finishedAt,
            final Long latencyMs,
            final String errorCode,
            final String errorMessage) {
        final DecisionRun current =
                Optional.ofNullable(runs.get(id))
                        .orElseThrow(
                                () ->
                                        new DecisionCorePersistenceException(
                                                "decision run not found", new IllegalStateException("missing run")));
        runs.put(
                id,
                new DecisionRun(
                        current.id(),
                        current.decisionRequestId(),
                        current.runNo(),
                        status,
                        current.orchestratorKey(),
                        current.modelProvider(),
                        current.modelName(),
                        current.startedAt(),
                        finishedAt,
                        latencyMs,
                        errorCode,
                        errorMessage,
                        current.createdAt()));
    }

    @Override
    public synchronized List<DecisionRun> findByDecisionRequestId(final UUID decisionRequestId) {
        return runs.values().stream()
                .filter(run -> run.decisionRequestId().equals(decisionRequestId))
                .sorted(Comparator.comparingInt(DecisionRun::runNo))
                .toList();
    }

    @Override
    public synchronized Optional<DecisionRun> findById(final UUID id) {
        return Optional.ofNullable(runs.get(id));
    }

    @Override
    public synchronized void save(final QuantSignal signal) {
        final QuantSignal checked = Objects.requireNonNull(signal, "signal");
        if (signals.containsKey(checked.id())) {
            throw new DecisionCorePersistenceException(
                    "quant signal duplicate key", new IllegalStateException("duplicate signal"));
        }
        signals.put(checked.id(), checked);
    }

    @Override
  public synchronized List<QuantSignal> findSignalsByDecisionRequestId(final UUID decisionRequestId) {
    return signals.values().stream()
        .filter(signal -> signal.decisionRequestId().equals(decisionRequestId))
        .sorted(Comparator.comparing(QuantSignal::createdAt))
                .toList();
    }

    @Override
    public synchronized Optional<QuantSignal> findSignalById(final UUID id) {
        return Optional.ofNullable(signals.get(id));
    }

    @Override
    public synchronized void save(final QuantDecision decision) {
        final QuantDecision checked = Objects.requireNonNull(decision, "decision");
        if (decisions.containsKey(checked.id())) {
            throw new DecisionCorePersistenceException(
                    "quant decision duplicate key", new IllegalStateException("duplicate decision"));
        }
        decisions.put(checked.id(), checked);
    }

    @Override
    public synchronized List<QuantDecision> findByDecisionRunId(final UUID decisionRunId) {
        return decisions.values().stream()
                .filter(decision -> decision.decisionRunId().equals(decisionRunId))
                .sorted(Comparator.comparing(QuantDecision::createdAt))
                .toList();
    }
}
