package com.guidinglight.decisionhub.infra.jdbc.qdr;

import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQuery;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregateService;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceQuery;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelQueryPort;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRepository;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRepository;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotAssembler;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotAssemblyException;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotAssemblyRequest;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHash;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotHasher;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotPersistencePort;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotRecord;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotSources;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.CanonicalReplaySnapshotWriteCommand;
import com.guidinglight.decisionhub.usecase.qdr.snapshot.ReplayInputSnapshot;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * P3 canonical snapshot 的 PostgreSQL {@code REPEATABLE_READ} transaction orchestration service。
 *
 * <p>本服务只组合现有 tenant-bound ports，不新增 SQL/JDBC/port。一次调用在同一 transaction 内完成 source
 * read、assembly、canonicalization/hash、source revalidation、immutable insert 与 exact read-back。任何步骤失败
 * 均抛出 RuntimeException 触发整体 rollback；不存在默认隔离级别或 direct/in-memory fallback。
 */
public final class ReplayInputSnapshotAssemblyService {

  private final DecisionReplayQueryRepository replayQueryRepository;
  private final DecisionReadModelQueryPort decisionReadModelQueryPort;
  private final PromptVersionPersistencePort promptVersionPersistencePort;
  private final ModelVersionPersistencePort modelVersionPersistencePort;
  private final ModelGatewayCallPersistencePort gatewayCallPersistencePort;
  private final ReplayCaseRepository replayCaseRepository;
  private final EvaluationCaseRepository evaluationCaseRepository;
  private final RegressionVerdictRepository regressionVerdictRepository;
  private final DecisionEvidenceAggregateService evidenceAggregateService;
  private final CanonicalReplaySnapshotPersistencePort snapshotPersistencePort;
  private final CanonicalReplaySnapshotAssembler assembler;
  private final CanonicalReplaySnapshotHasher hasher;
  private final TransactionTemplate transaction;

  /**
   * 创建 production transaction service；transaction manager 缺失时立即 fail-fast。
   */
  public ReplayInputSnapshotAssemblyService(
      final DecisionReplayQueryRepository replayQueryRepository,
      final DecisionReadModelQueryPort decisionReadModelQueryPort,
      final PromptVersionPersistencePort promptVersionPersistencePort,
      final ModelVersionPersistencePort modelVersionPersistencePort,
      final ModelGatewayCallPersistencePort gatewayCallPersistencePort,
      final ReplayCaseRepository replayCaseRepository,
      final EvaluationCaseRepository evaluationCaseRepository,
      final RegressionVerdictRepository regressionVerdictRepository,
      final DecisionEvidenceAggregateService evidenceAggregateService,
      final CanonicalReplaySnapshotPersistencePort snapshotPersistencePort,
      final CanonicalReplaySnapshotAssembler assembler,
      final CanonicalReplaySnapshotHasher hasher,
      final PlatformTransactionManager transactionManager) {
    this.replayQueryRepository = Objects.requireNonNull(replayQueryRepository, "replayQueryRepository");
    this.decisionReadModelQueryPort =
        Objects.requireNonNull(decisionReadModelQueryPort, "decisionReadModelQueryPort");
    this.promptVersionPersistencePort =
        Objects.requireNonNull(promptVersionPersistencePort, "promptVersionPersistencePort");
    this.modelVersionPersistencePort =
        Objects.requireNonNull(modelVersionPersistencePort, "modelVersionPersistencePort");
    this.gatewayCallPersistencePort =
        Objects.requireNonNull(gatewayCallPersistencePort, "gatewayCallPersistencePort");
    this.replayCaseRepository = Objects.requireNonNull(replayCaseRepository, "replayCaseRepository");
    this.evaluationCaseRepository =
        Objects.requireNonNull(evaluationCaseRepository, "evaluationCaseRepository");
    this.regressionVerdictRepository =
        Objects.requireNonNull(regressionVerdictRepository, "regressionVerdictRepository");
    this.evidenceAggregateService =
        Objects.requireNonNull(evidenceAggregateService, "evidenceAggregateService");
    this.snapshotPersistencePort =
        Objects.requireNonNull(snapshotPersistencePort, "snapshotPersistencePort");
    this.assembler = Objects.requireNonNull(assembler, "assembler");
    this.hasher = Objects.requireNonNull(hasher, "hasher");
    if (transactionManager == null) {
      throw new CanonicalReplaySnapshotAssemblyException(
          CanonicalReplaySnapshotAssemblyException.Code.TRANSACTION_MANAGER_REQUIRED,
          "production PostgreSQL transaction manager is required");
    }
    this.transaction = new TransactionTemplate(transactionManager);
    this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
    this.transaction.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    this.transaction.setReadOnly(false);
  }

  /**
   * 在单一显式 {@code REPEATABLE_READ} transaction 内组装、hash 并持久化 canonical snapshot。
   */
  public CanonicalReplaySnapshotRecord assembleHashAndPersist(
      final CanonicalReplaySnapshotAssemblyRequest request) {
    final CanonicalReplaySnapshotAssemblyRequest checked =
        Objects.requireNonNull(request, "request");
    final CanonicalReplaySnapshotRecord result =
        transaction.execute(
            status -> {
              final CanonicalReplaySnapshotSources firstSources = loadSources(checked);
              final ReplayInputSnapshot firstSnapshot = assembler.assemble(checked, firstSources);
              final CanonicalReplaySnapshotHash firstHash = hasher.hash(firstSnapshot);

              final CanonicalReplaySnapshotSources secondSources = loadSources(checked);
              final ReplayInputSnapshot secondSnapshot = assembler.assemble(checked, secondSources);
              final CanonicalReplaySnapshotHash secondHash = hasher.hash(secondSnapshot);
              if (!firstSnapshot.equals(secondSnapshot)
                  || !firstHash.lowercaseHex().equals(secondHash.lowercaseHex())
                  || !Arrays.equals(firstHash.canonicalBytes(), secondHash.canonicalBytes())) {
                throw failure(
                    CanonicalReplaySnapshotAssemblyException.Code.SOURCE_CHANGED,
                    "canonical snapshot source changed during transaction");
              }

              final CanonicalReplaySnapshotWriteCommand command =
                  assembler.toWriteCommand(checked, firstSnapshot, firstHash, secondSources);
              final CanonicalReplaySnapshotRecord inserted =
                  snapshotPersistencePort.insert(checked.tenantId(), command);
              final CanonicalReplaySnapshotRecord readBack =
                  snapshotPersistencePort
                      .findByTenantAndIdentity(
                          checked.tenantId(),
                          checked.identity(),
                          checked.versionVector().snapshotSchemaVersion())
                      .orElseThrow(
                          () ->
                              failure(
                                  CanonicalReplaySnapshotAssemblyException.Code.PERSISTENCE_MISMATCH,
                                  "persisted snapshot exact read-back missing"));
              if (!exactReadBack(inserted, readBack, command, firstHash)) {
                throw failure(
                    CanonicalReplaySnapshotAssemblyException.Code.PERSISTENCE_MISMATCH,
                    "persisted snapshot exact read-back mismatch");
              }
              return readBack;
            });
    if (result == null) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.PERSISTENCE_MISMATCH,
          "canonical snapshot transaction returned no record");
    }
    return result;
  }

  private CanonicalReplaySnapshotSources loadSources(
      final CanonicalReplaySnapshotAssemblyRequest request) {
    try {
      return loadSourcesChecked(request);
    } catch (final CanonicalReplaySnapshotAssemblyException error) {
      throw error;
    } catch (final RuntimeException error) {
      throw new CanonicalReplaySnapshotAssemblyException(
          CanonicalReplaySnapshotAssemblyException.Code.REQUIRED_SOURCE_MISSING,
          "canonical snapshot source read failed",
          error);
    }
  }

  private CanonicalReplaySnapshotSources loadSourcesChecked(
      final CanonicalReplaySnapshotAssemblyRequest request) {
    final DecisionReplayView v5 =
        requiredValue(
            replayQueryRepository.findReplay(
                new DecisionReplayQuery(
                    request.tenantId(),
                    request.decisionId(),
                    request.traceId(),
                    request.requestId())),
            "V5 replay source");
    final DecisionRunDetailView v6 =
        required(
            decisionReadModelQueryPort.findDecisionRunDetail(
                new DecisionRunReadQuery(
                    request.tenantId(),
                    request.decisionRunId().toString(),
                    null,
                    request.traceId())),
            "V6 decision run source");
    final PromptVersionRecord prompt =
        required(
            promptVersionPersistencePort.findByTenantAndPromptVersionId(
                request.tenantId(), request.promptVersionId()),
            "V8 prompt version source");
    final ModelVersionRecord model =
        required(
            modelVersionPersistencePort.findByTenantAndModelVersionId(
                request.tenantId(), request.modelVersionId()),
            "V8 model version source");
    final ModelGatewayCallRecord call =
        required(
            gatewayCallPersistencePort.findByTenantAndDecisionRunAndModelCallRef(
                request.tenantId(), request.decisionRunId(), request.modelCallRef()),
            "V8 gateway call source");
    final ReplayCaseRecord replay =
        required(
            replayCaseRepository.findById(request.tenantId(), request.replayCaseRowId()),
            "V9 replay case source");
    final EvaluationCaseRecord evaluation =
        request.evaluationCaseRowId() == null
            ? null
            : required(
                evaluationCaseRepository.findById(
                    request.tenantId(), request.evaluationCaseRowId()),
                "V9 evaluation source");
    final RegressionVerdictRecord verdict =
        request.regressionVerdictRowId() == null
            ? null
            : required(
                regressionVerdictRepository.findById(
                    request.tenantId(), request.regressionVerdictRowId()),
                "V9 verdict source");
    final DecisionEvidenceQuery evidenceQuery =
        new DecisionEvidenceQuery(
            request.tenantId(),
            request.traceId(),
            request.requestId(),
            request.decisionId(),
            request.decisionRunId().toString(),
            request.replayCaseId(),
            request.evaluationCaseId(),
            request.regressionVerdictId(),
            request.providerRef(),
            request.evidencePolicy());
    final DecisionEvidenceAggregate aggregate =
        evidenceAggregateService.aggregate(evidenceQuery, request.evidencePolicy());
    return new CanonicalReplaySnapshotSources(
        v5, v6, prompt, model, call, replay, evaluation, verdict, aggregate);
  }

  private static <T> T requiredValue(final T source, final String name) {
    if (source == null) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.REQUIRED_SOURCE_MISSING,
          name + " missing");
    }
    return source;
  }

  private static boolean exactReadBack(
      final CanonicalReplaySnapshotRecord inserted,
      final CanonicalReplaySnapshotRecord readBack,
      final CanonicalReplaySnapshotWriteCommand command,
      final CanonicalReplaySnapshotHash hash) {
    return inserted.id().equals(readBack.id())
        && inserted.createdAt().equals(readBack.createdAt())
        && command.id().equals(readBack.id())
        && command.identity().equals(readBack.identity())
        && command.versionVector().equals(readBack.versionVector())
        && command.replayInputHash().equals(readBack.replayInputHash())
        && command.expectedSummaryHash().equals(readBack.expectedSummaryHash())
        && Objects.equals(command.providerSummaryHash(), readBack.providerSummaryHash())
        && command.canonicalInputHash().equals(readBack.canonicalInputHash())
        && hash.lowercaseHex().equals(readBack.canonicalInputHash())
        && command.payloadBytes() == readBack.payloadBytes();
  }

  private static <T> T required(final Optional<T> source, final String name) {
    if (source == null || source.isEmpty()) {
      throw failure(
          CanonicalReplaySnapshotAssemblyException.Code.REQUIRED_SOURCE_MISSING,
          name + " missing");
    }
    return source.orElseThrow();
  }

  private static CanonicalReplaySnapshotAssemblyException failure(
      final CanonicalReplaySnapshotAssemblyException.Code code, final String message) {
    return new CanonicalReplaySnapshotAssemblyException(code, message);
  }
}
