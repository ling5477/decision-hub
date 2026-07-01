package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

final class DecisionOrchestratorPersistenceTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");
  private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

  @Test
  void validRequestWritesRequestSnapshotTraceProviderOutputAndAudit() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    final DecisionOrchestrator orchestrator =
        orchestrator(repository, new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.NO_TRADE, output.getAction());
    assertEquals(1, repository.requests.size());
    assertEquals(1, repository.contextSnapshots.size());
    assertEquals(1, repository.providerCalls.size());
    assertEquals(1, repository.outputs.size());
    assertEquals(1, repository.auditEvents.size());
    assertTraceCompleted(repository, DecisionTraceStepName.POLICY_CHECK);
    assertTraceCompleted(repository, DecisionTraceStepName.CONTEXT_BUILD);
    assertTraceCompleted(repository, DecisionTraceStepName.MOCK_PROVIDER_SIGNAL);
    assertTraceCompleted(repository, DecisionTraceStepName.RISK_REVIEW);
    assertEquals(DecisionAuditEventType.DECISION_COMPLETED, repository.auditEvents.get(0).eventType());
    assertEquals(DecisionAuditEventStatus.SUCCESS, repository.auditEvents.get(0).eventStatus());
  }

  @Test
  void policyDeniedStillWritesOutputAndAuditWithoutProviderCall() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    final DecisionOrchestrator orchestrator =
        orchestrator(repository, new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(forbiddenRequest());

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionStatus.BLOCKED, output.getStatus());
    assertEquals(DecisionPolicyStatus.DENIED, output.getPolicyStatus());
    assertEquals(1, repository.requests.size());
    assertEquals(0, repository.providerCalls.size());
    assertEquals(1, repository.outputs.size());
    assertEquals(1, repository.auditEvents.size());
    assertEquals(DecisionAuditEventType.POLICY_DENIED, repository.auditEvents.get(0).eventType());
    assertEquals("[REDACTED]", repository.requests.get(0).subjectJson().get("strategyRef"));
  }

  @Test
  void providerTimeoutWritesProviderCallAndReturnsAbstain() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    final DecisionOrchestrator orchestrator =
        orchestrator(
            repository,
            new MockDecisionSignalProvider(
                ProviderSignalStatus.TIMEOUT,
                DecisionAction.ABSTAIN,
                List.of("MOCK_PROVIDER_TIMEOUT")));

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(ProviderSignalStatus.TIMEOUT, output.getProviderStatus());
    assertEquals(1, repository.providerCalls.size());
    assertEquals(ProviderSignalStatus.TIMEOUT, repository.providerCalls.get(0).providerStatus());
    assertEquals(DecisionAuditEventType.PROVIDER_FAILED, repository.auditEvents.get(0).eventType());
  }

  @Test
  void highRiskDoesNotAllowDirectionalBiasAndPersistsAudit() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    final DecisionOrchestrator orchestrator =
        orchestrator(
            repository,
            new MockDecisionSignalProvider(
                ProviderSignalStatus.MOCKED,
                DecisionAction.LONG_BIAS,
                List.of("MOCK_LONG_BIAS", "HIGH_RISK_SIGNAL")));

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals(DecisionRiskLevel.HIGH, output.getRiskLevel());
    assertEquals(DecisionAuditEventType.RISK_BLOCKED, repository.auditEvents.get(0).eventType());
  }

  @Test
  void auditWriteFailureFailsClosed() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    repository.failOnAuditEvent = true;
    final DecisionOrchestrator orchestrator =
        orchestrator(repository, new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertTrue(output.getReasonCodes().contains("PERSISTENCE_FAILURE"));
    assertTrue(repository.outputs.size() >= 1);
  }

  @Test
  void outputWriteFailureFailsClosedEvenWhenFailClosedOutputCannotPersist() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    repository.failOnOutput = true;
    final DecisionOrchestrator orchestrator =
        orchestrator(repository, new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertTrue(output.getReasonCodes().contains("PERSISTENCE_FAILURE"));
    assertEquals(0, repository.outputs.size());
  }

  @Test
  void contextSnapshotWriteFailureFailsClosed() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    repository.failOnContextSnapshot = true;
    final DecisionOrchestrator orchestrator =
        orchestrator(repository, new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertTrue(output.getReasonCodes().contains("PERSISTENCE_FAILURE"));
    assertEquals(0, repository.contextSnapshots.size());
  }

  @Test
  void requestWriteFailureFailsClosedBeforePolicyCheck() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    repository.failOnRequest = true;
    final DecisionOrchestrator orchestrator =
        orchestrator(repository, new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(validRequest(List.of("evidence://case-1")));

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertTrue(output.getReasonCodes().contains("PERSISTENCE_FAILURE"));
    assertEquals(0, repository.requests.size());
    assertEquals(1, repository.outputs.size());
    assertEquals(1, repository.auditEvents.size());
    assertEquals(DecisionAuditEventType.PERSISTENCE_FAILED, repository.auditEvents.get(0).eventType());
  }

  @Test
  void missingRequestUsesUnknownIdsAndFailsClosedByPolicy() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    final DecisionOrchestrator orchestrator =
        orchestrator(repository, new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(null);

    assertEquals(DecisionAction.ABSTAIN, output.getAction());
    assertEquals("unknown-request", output.getRequestId());
    assertEquals("unknown-trace", output.getTraceId());
    assertEquals("unknown-tenant", output.getTenantId());
    assertEquals(DecisionPolicyStatus.INVALID, output.getPolicyStatus());
    assertEquals("unknown-request", repository.requests.get(0).requestId());
    assertEquals("unknown-trace", repository.requests.get(0).traceId());
    assertEquals("unknown-tenant", repository.requests.get(0).tenantId());
  }

  @Test
  void requestPersistenceRedactsSensitiveContextRefBeforePolicyDenial() {
    final RecordingDecisionAuditRepository repository = new RecordingDecisionAuditRepository();
    final DecisionOrchestrator orchestrator =
        orchestrator(repository, new MockDecisionSignalProvider());

    final DecisionOutput output = orchestrator.decide(secretBearingRequest());

    assertEquals(DecisionStatus.BLOCKED, output.getStatus());
    assertEquals("[REDACTED]", repository.requests.get(0).contextRef());
    assertFalse(repository.requests.get(0).contextRef().contains("apiSecret"));
  }

  private static DecisionOrchestrator orchestrator(
      final DecisionAuditRepository repository, final DecisionSignalProvider signalProvider) {
    return new DefaultDecisionOrchestrator(
        new DefaultDecisionContextBuilder(),
        new DefaultDecisionPolicyChecker(),
        signalProvider,
        new DefaultDecisionRiskReviewer(),
        new DecisionOutputAssembler(),
        repository,
        CLOCK);
  }

  private static void assertTraceCompleted(
      final RecordingDecisionAuditRepository repository, final DecisionTraceStepName stepName) {
    assertTrue(
        repository.traceSteps.stream()
            .anyMatch(
                step ->
                    step.stepName() == stepName
                        && step.stepStatus() == DecisionTraceStepStatus.COMPLETED),
        stepName + " should have a COMPLETED trace row");
  }

  private static DecisionRequest validRequest(final List<String> evidenceRefs) {
    return DecisionRequest.readOnlyRecommendation(
        "req-1",
        "trace-1",
        "tenant-1",
        "codex-test",
        new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
        "context://safe",
        new DecisionContextSnapshot("snapshot-1", NOW, evidenceRefs),
        NOW);
  }

  private static DecisionRequest forbiddenRequest() {
    return DecisionRequest.readOnlyRecommendation(
        "req-policy",
        "trace-policy",
        "tenant-1",
        "codex-test",
        new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "placeOrder-plan", "research-1"),
        "context://safe",
        new DecisionContextSnapshot("snapshot-policy", NOW, List.of("evidence://case-1")),
        NOW);
  }

  private static DecisionRequest secretBearingRequest() {
    return DecisionRequest.readOnlyRecommendation(
        "req-secret",
        "trace-secret",
        "tenant-1",
        "codex-test",
        new DecisionSubject("BTC-USDT", "CRYPTO", "1h", "strategy-1", "research-1"),
        "apiSecret=do-not-store",
        new DecisionContextSnapshot("snapshot-secret", NOW, List.of("evidence://case-1")),
        NOW);
  }

  private static final class RecordingDecisionAuditRepository implements DecisionAuditRepository {
    private final List<DecisionPersistenceRecords.RequestRecord> requests = new ArrayList<>();
    private final List<DecisionPersistenceRecords.ContextSnapshotRecord> contextSnapshots =
        new ArrayList<>();
    private final List<DecisionPersistenceRecords.TraceStepRecord> traceSteps = new ArrayList<>();
    private final List<DecisionPersistenceRecords.ProviderCallRecord> providerCalls =
        new ArrayList<>();
    private final List<DecisionPersistenceRecords.OutputRecord> outputs = new ArrayList<>();
    private final List<DecisionPersistenceRecords.AuditEventRecord> auditEvents =
        new ArrayList<>();
    private boolean failOnRequest;
    private boolean failOnContextSnapshot;
    private boolean failOnOutput;
    private boolean failOnAuditEvent;

    @Override
    public void saveRequest(final DecisionPersistenceRecords.RequestRecord record) {
      if (failOnRequest) {
        throw new DecisionPersistenceException("request write failed");
      }
      requests.add(record);
    }

    @Override
    public void saveContextSnapshot(
        final DecisionPersistenceRecords.ContextSnapshotRecord record) {
      if (failOnContextSnapshot) {
        throw new DecisionPersistenceException("context snapshot write failed");
      }
      contextSnapshots.add(record);
    }

    @Override
    public void saveTraceStep(final DecisionPersistenceRecords.TraceStepRecord record) {
      traceSteps.add(record);
    }

    @Override
    public void saveProviderCall(final DecisionPersistenceRecords.ProviderCallRecord record) {
      providerCalls.add(record);
    }

    @Override
    public void saveOutput(final DecisionPersistenceRecords.OutputRecord record) {
      if (failOnOutput) {
        throw new DecisionPersistenceException("output write failed");
      }
      outputs.add(record);
    }

    @Override
    public void saveAuditEvent(final DecisionPersistenceRecords.AuditEventRecord record) {
      if (failOnAuditEvent) {
        throw new DecisionPersistenceException("audit write failed");
      }
      auditEvents.add(record);
    }
  }
}
