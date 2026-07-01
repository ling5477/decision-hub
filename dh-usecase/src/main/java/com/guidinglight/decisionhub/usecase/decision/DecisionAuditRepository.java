package com.guidinglight.decisionhub.usecase.decision;

/**
 * K3 decision audit / snapshot / trace 持久化端口。
 *
 * <p>usecase 只依赖本端口，不依赖 JDBC。实现必须在写入失败、JSON 序列化失败、核心 ID 缺失时抛出
 * {@link DecisionPersistenceException} 或等价 RuntimeException，由 orchestrator fail-closed。
 */
public interface DecisionAuditRepository {

  /** 保存 request 审计记录；失败必须向上报告。 */
  void saveRequest(DecisionPersistenceRecords.RequestRecord record);

  /** 保存 context snapshot 审计记录；失败必须向上报告。 */
  void saveContextSnapshot(DecisionPersistenceRecords.ContextSnapshotRecord record);

  /** 保存 trace step 审计记录；失败必须向上报告。 */
  void saveTraceStep(DecisionPersistenceRecords.TraceStepRecord record);

  /** 保存 provider call summary；不得保存原始敏感响应。 */
  void saveProviderCall(DecisionPersistenceRecords.ProviderCallRecord record);

  /** 保存结构化 DecisionOutput；失败必须触发 orchestrator fail-closed。 */
  void saveOutput(DecisionPersistenceRecords.OutputRecord record);

  /** 保存 audit event；失败必须触发 orchestrator fail-closed。 */
  void saveAuditEvent(DecisionPersistenceRecords.AuditEventRecord record);
}
