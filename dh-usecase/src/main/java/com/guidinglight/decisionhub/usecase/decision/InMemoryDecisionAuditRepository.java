package com.guidinglight.decisionhub.usecase.decision;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * K3 usecase 测试和默认构造使用的内存审计仓储。
 *
 * <p>该实现只用于单 JVM 内的 deterministic 测试或未接 Spring wiring 的默认对象；生产持久化由 dh-infra
 * JDBC adapter 负责。所有集合只追加，不做 replay read model。
 */
public final class InMemoryDecisionAuditRepository implements DecisionAuditRepository {

  private final List<DecisionPersistenceRecords.RequestRecord> requests = new ArrayList<>();
  private final List<DecisionPersistenceRecords.ContextSnapshotRecord> contextSnapshots =
      new ArrayList<>();
  private final List<DecisionPersistenceRecords.TraceStepRecord> traceSteps = new ArrayList<>();
  private final List<DecisionPersistenceRecords.ProviderCallRecord> providerCalls =
      new ArrayList<>();
  private final List<DecisionPersistenceRecords.OutputRecord> outputs = new ArrayList<>();
  private final List<DecisionPersistenceRecords.AuditEventRecord> auditEvents = new ArrayList<>();

  @Override
  public void saveRequest(final DecisionPersistenceRecords.RequestRecord record) {
    requests.add(Objects.requireNonNull(record, "record"));
  }

  @Override
  public void saveContextSnapshot(
      final DecisionPersistenceRecords.ContextSnapshotRecord record) {
    contextSnapshots.add(Objects.requireNonNull(record, "record"));
  }

  @Override
  public void saveTraceStep(final DecisionPersistenceRecords.TraceStepRecord record) {
    traceSteps.add(Objects.requireNonNull(record, "record"));
  }

  @Override
  public void saveProviderCall(final DecisionPersistenceRecords.ProviderCallRecord record) {
    providerCalls.add(Objects.requireNonNull(record, "record"));
  }

  @Override
  public void saveOutput(final DecisionPersistenceRecords.OutputRecord record) {
    outputs.add(Objects.requireNonNull(record, "record"));
  }

  @Override
  public void saveAuditEvent(final DecisionPersistenceRecords.AuditEventRecord record) {
    auditEvents.add(Objects.requireNonNull(record, "record"));
  }

  /** 返回已写入 request 记录快照。 */
  public List<DecisionPersistenceRecords.RequestRecord> requests() {
    return Collections.unmodifiableList(requests);
  }

  /** 返回已写入 context snapshot 记录快照。 */
  public List<DecisionPersistenceRecords.ContextSnapshotRecord> contextSnapshots() {
    return Collections.unmodifiableList(contextSnapshots);
  }

  /** 返回已写入 trace step 记录快照。 */
  public List<DecisionPersistenceRecords.TraceStepRecord> traceSteps() {
    return Collections.unmodifiableList(traceSteps);
  }

  /** 返回已写入 provider call 记录快照。 */
  public List<DecisionPersistenceRecords.ProviderCallRecord> providerCalls() {
    return Collections.unmodifiableList(providerCalls);
  }

  /** 返回已写入 output 记录快照。 */
  public List<DecisionPersistenceRecords.OutputRecord> outputs() {
    return Collections.unmodifiableList(outputs);
  }

  /** 返回已写入 audit event 记录快照。 */
  public List<DecisionPersistenceRecords.AuditEventRecord> auditEvents() {
    return Collections.unmodifiableList(auditEvents);
  }
}
