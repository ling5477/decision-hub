package com.guidinglight.decisionhub.usecase.decision.dryrun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionOutputAssembler;
import com.guidinglight.decisionhub.usecase.decision.DecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionContextBuilder;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionOrchestrator;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionPolicyChecker;
import com.guidinglight.decisionhub.usecase.decision.DefaultDecisionRiskReviewer;
import com.guidinglight.decisionhub.usecase.decision.MockDecisionSignalProvider;
import com.guidinglight.decisionhub.usecase.decision.support.RecordingDecisionAuditReplayRepository;
import com.guidinglight.decisionhub.usecase.qdr.InMemoryDecisionCoreRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Integration-1 limited dry-run usecase 回归测试。
 *
 * <p>覆盖 feature flag、tenant/source allowlist、dryRun policy、forbidden material、memory cap、audit
 * fail-closed、ABSTAIN 外部映射与 provider failure taxonomy。测试不发 HTTP、不调用 NQ、不接真实 provider。
 */
class DefaultDecisionDryRunServiceTest {

  private static final Instant NOW = Instant.parse("2026-07-04T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void validDryRunReturnsReadonlySnapshotAndWritesAuditTraceReplayRefs() {
    final RecordingDecisionAuditReplayRepository repository =
        new RecordingDecisionAuditReplayRepository();
    final InMemoryDecisionCoreRepository decisionCoreRepository =
        new InMemoryDecisionCoreRepository();
    final DecisionDryRunResult result =
        service(repository, decisionCoreRepository, enabled(), defaultOrchestrator(repository))
            .execute(command());

    assertTrue(result.success());
    assertEquals(200, result.status());
    assertNotNull(result.snapshot().auditRef());
    assertNotNull(result.snapshot().replayRef());
    assertTrue(Set.of("OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS").contains(result.snapshot().action()));
    assertFalse(result.snapshot().action().equals("BUY"));
    assertFalse(result.snapshot().action().equals("SELL"));
    assertTrue(repository.auditEventCount() >= 1);
    assertTrue(repository.outputCount() >= 1);
    final var decisionRequest =
        decisionCoreRepository
            .findByTenantIdAndRequestKey("tenant-a", "req-dryrun-1")
            .orElseThrow();
    final var decisionRun =
        decisionCoreRepository.findByDecisionRequestId(decisionRequest.id()).getFirst();
    assertEquals("trace-dryrun-1", decisionRequest.traceId());
    assertEquals(1, decisionCoreRepository.findByDecisionRunId(decisionRun.id()).size());
  }

  @Test
  void featureDisabledFailsClosedWithPolicyDenied() {
    final DecisionDryRunResult result =
        service(new RecordingDecisionAuditReplayRepository(), disabled(), new DefaultDecisionOrchestrator())
            .execute(command());

    assertRejected(result, 403, DecisionDryRunErrorCode.POLICY_DENIED);
  }

  @Test
  void sourcePairDeniedFailsClosed() {
    final DecisionDryRunRuntimeProperties properties =
        new DecisionDryRunRuntimeProperties(
            true, false, false, true, Set.of("NQ_DRYRUN"), Set.of("tenant-b:NQ_DRYRUN"), 32768);

    final DecisionDryRunResult result =
        service(new RecordingDecisionAuditReplayRepository(), properties, new DefaultDecisionOrchestrator())
            .execute(command());

    assertRejected(result, 403, DecisionDryRunErrorCode.SOURCE_DENIED);
  }

  @Test
  void dryRunFalseAndForbiddenMaterialFailClosedWithPolicyDenied() {
    final DefaultDecisionDryRunService service =
        service(
            new RecordingDecisionAuditReplayRepository(),
            enabled(),
            new DefaultDecisionOrchestrator());

    assertRejected(
        service.execute(command(false, false, 1024)),
        403,
        DecisionDryRunErrorCode.POLICY_DENIED);
    assertRejected(
        service.execute(command(true, true, 1024)), 403, DecisionDryRunErrorCode.POLICY_DENIED);
  }

  @Test
  void contextOverMemoryCapFailsClosed() {
    final DecisionDryRunResult result =
        service(new RecordingDecisionAuditReplayRepository(), enabled(), new DefaultDecisionOrchestrator())
            .execute(command(true, false, 100_000));

    assertRejected(result, 500, DecisionDryRunErrorCode.MEMORY_LIMIT_EXCEEDED);
  }

  @Test
  void auditWriteFailureDoesNotReturnSuccessDecision() {
    final DecisionDryRunResult result =
        service(new FailingAuditRepository(), enabled(), new DefaultDecisionOrchestrator()).execute(command());

    assertRejected(result, 500, DecisionDryRunErrorCode.UNKNOWN_ERROR);
    assertFalse(result.success());
  }

  @Test
  void internalAbstainIsMappedToExternalNoTradeWithReason() {
    final DecisionOrchestrator abstain =
        request ->
            DecisionOutput.abstainForNoEvidence(
                request.getRequestId(), request.getTraceId(), request.getTenantId(), NOW);

    final DecisionDryRunResult result =
        service(new RecordingDecisionAuditReplayRepository(), enabled(), abstain).execute(command());

    assertTrue(result.success());
    assertEquals("NO_TRADE", result.snapshot().action());
    assertTrue(result.snapshot().reasons().contains("INTERNAL_ABSTAIN_MAPPED"));
  }

  @Test
  void providerDisabledMapsToProviderDisabledError() {
    final DecisionOrchestrator providerDisabled =
        request ->
            DecisionOutput.abstainForProviderFailure(
                request.getRequestId(),
                request.getTraceId(),
                request.getTenantId(),
                ProviderSignalStatus.DISABLED,
                List.of("PROVIDER_DISABLED"),
                NOW);

    final DecisionDryRunResult result =
        service(new RecordingDecisionAuditReplayRepository(), enabled(), providerDisabled).execute(command());

    assertRejected(result, 503, DecisionDryRunErrorCode.PROVIDER_DISABLED);
  }

  private static DefaultDecisionDryRunService service(
      final DecisionAuditRepository repository,
      final DecisionDryRunRuntimeProperties properties,
      final DecisionOrchestrator orchestrator) {
    return new DefaultDecisionDryRunService(orchestrator, repository, properties, CLOCK);
  }

  private static DefaultDecisionDryRunService service(
      final DecisionAuditRepository repository,
      final InMemoryDecisionCoreRepository decisionCoreRepository,
      final DecisionDryRunRuntimeProperties properties,
      final DecisionOrchestrator orchestrator) {
    return new DefaultDecisionDryRunService(
        orchestrator,
        repository,
        decisionCoreRepository,
        decisionCoreRepository,
        decisionCoreRepository,
        decisionCoreRepository,
        properties,
        CLOCK);
  }

  private static DecisionOrchestrator defaultOrchestrator(final DecisionAuditRepository repository) {
    return new DefaultDecisionOrchestrator(
        new DefaultDecisionContextBuilder(),
        new DefaultDecisionPolicyChecker(),
        new MockDecisionSignalProvider(),
        new DefaultDecisionRiskReviewer(),
        new DecisionOutputAssembler(),
        repository,
        CLOCK);
  }

  private static DecisionDryRunRuntimeProperties enabled() {
    return new DecisionDryRunRuntimeProperties(
        true, false, false, true, Set.of("NQ_DRYRUN"), Set.of("tenant-a:NQ_DRYRUN"), 32768);
  }

  private static DecisionDryRunRuntimeProperties disabled() {
    return new DecisionDryRunRuntimeProperties(
        false, false, false, true, Set.of("NQ_DRYRUN"), Set.of("tenant-a:NQ_DRYRUN"), 32768);
  }

  private static DecisionDryRunCommand command() {
    return command(true, false, 1024);
  }

  private static DecisionDryRunCommand command(
      final boolean dryRun, final boolean forbiddenMaterialDetected, final int approxBytes) {
    return new DecisionDryRunCommand(
        "req-dryrun-1",
        "trace-dryrun-1",
        "tenant-a",
        "NQ_DRYRUN",
        "2026-07-04T00:00:00Z",
        "nonce-dryrun-1",
        "1.0.0",
        dryRun,
        Set.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE", "READ_NQ_DB", "WRITE_NQ_DB"),
        new DecisionDryRunContext(
            "BTC-USDT",
            "CRYPTO",
            "1h",
            "strategy-readonly",
            "research-readonly",
            "context://readonly",
            "snapshot-dryrun-1",
            NOW,
            List.of("evidence://dryrun/1"),
            approxBytes),
        forbiddenMaterialDetected);
  }

  private static void assertRejected(
      final DecisionDryRunResult result,
      final int status,
      final DecisionDryRunErrorCode errorCode) {
    assertFalse(result.success());
    assertEquals(status, result.status());
    assertEquals(errorCode, result.errorCode());
    assertNotNull(result.requestId());
    assertNotNull(result.traceId());
  }

  private static final class FailingAuditRepository implements DecisionAuditRepository {
    @Override
    public void saveRequest(final DecisionPersistenceRecords.RequestRecord record) {
      throw failure();
    }

    @Override
    public void saveContextSnapshot(final DecisionPersistenceRecords.ContextSnapshotRecord record) {
      throw failure();
    }

    @Override
    public void saveTraceStep(final DecisionPersistenceRecords.TraceStepRecord record) {
      throw failure();
    }

    @Override
    public void saveProviderCall(final DecisionPersistenceRecords.ProviderCallRecord record) {
      throw failure();
    }

    @Override
    public void saveOutput(final DecisionPersistenceRecords.OutputRecord record) {
      throw failure();
    }

    @Override
    public void saveAuditEvent(final DecisionPersistenceRecords.AuditEventRecord record) {
      throw failure();
    }

    private static RuntimeException failure() {
      return new IllegalStateException("audit write failure");
    }
  }
}
