package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidencePolicy;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import java.util.Objects;
import java.util.UUID;

/**
 * P3 canonical snapshot assembly 的 tenant-bound exact selector。
 *
 * <p>该 command 明确区分 physical UUID、business ID 与 safe ref；所有 selector 只允许收窄现有 exact
 * ports，不允许 latest/current/default、时间顺序或字符串相似推断。它不携带 raw material，也不直接写库。
 */
public record CanonicalReplaySnapshotAssemblyRequest(
    UUID snapshotRecordId,
    String snapshotId,
    String tenantId,
    String traceId,
    String requestId,
    String decisionId,
    UUID decisionRequestId,
    UUID decisionRunId,
    UUID modelCallId,
    String modelCallRef,
    UUID promptVersionId,
    UUID modelVersionId,
    UUID replayCaseRowId,
    String replayCaseId,
    UUID evaluationCaseRowId,
    String evaluationCaseId,
    UUID regressionVerdictRowId,
    String regressionVerdictId,
    String providerRef,
    DecisionEvidencePolicy evidencePolicy,
    CanonicalReplaySnapshotVersionVector versionVector) {

  /** 校验全部 exact selector、optional lineage pair 与冻结 version vector。 */
  public CanonicalReplaySnapshotAssemblyRequest {
    snapshotRecordId = ReplayPersistenceGuard.requireUuid(snapshotRecordId, "snapshotRecordId");
    snapshotId = ReplayPersistenceGuard.requireSafeText(snapshotId, "snapshotId");
    tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
    traceId = ReplayPersistenceGuard.requireSafeText(traceId, "traceId");
    requestId = ReplayPersistenceGuard.requireSafeText(requestId, "requestId");
    decisionId = ReplayPersistenceGuard.requireSafeText(decisionId, "decisionId");
    decisionRequestId = ReplayPersistenceGuard.requireUuid(decisionRequestId, "decisionRequestId");
    decisionRunId = ReplayPersistenceGuard.requireUuid(decisionRunId, "decisionRunId");
    modelCallId = ReplayPersistenceGuard.requireUuid(modelCallId, "modelCallId");
    modelCallRef = ReplayPersistenceGuard.requireSafeText(modelCallRef, "modelCallRef");
    promptVersionId = ReplayPersistenceGuard.requireUuid(promptVersionId, "promptVersionId");
    modelVersionId = ReplayPersistenceGuard.requireUuid(modelVersionId, "modelVersionId");
    replayCaseRowId = ReplayPersistenceGuard.requireUuid(replayCaseRowId, "replayCaseRowId");
    replayCaseId = ReplayPersistenceGuard.requireSafeText(replayCaseId, "replayCaseId");
    evaluationCaseId = ReplayPersistenceGuard.optionalSafeText(evaluationCaseId, "evaluationCaseId");
    regressionVerdictId =
        ReplayPersistenceGuard.optionalSafeText(regressionVerdictId, "regressionVerdictId");
    providerRef = ReplayPersistenceGuard.optionalSafeText(providerRef, "providerRef");
    requirePair(evaluationCaseRowId, evaluationCaseId, "evaluationCase");
    requirePair(regressionVerdictRowId, regressionVerdictId, "regressionVerdict");
    if (regressionVerdictRowId != null && evaluationCaseRowId == null) {
      throw new IllegalArgumentException("regression verdict requires evaluation lineage");
    }
    evidencePolicy = Objects.requireNonNull(evidencePolicy, "evidencePolicy");
    versionVector = Objects.requireNonNull(versionVector, "versionVector");
  }

  /** 返回与 persistence contract 完全一致的 tenant-bound identity。 */
  public CanonicalReplaySnapshotIdentity identity() {
    return new CanonicalReplaySnapshotIdentity(
        new com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation(
            tenantId, traceId, requestId, decisionId),
        snapshotId,
        decisionRequestId,
        decisionRunId,
        modelCallId,
        modelCallRef,
        promptVersionId,
        modelVersionId,
        replayCaseRowId,
        replayCaseId,
        evaluationCaseRowId,
        evaluationCaseId,
        regressionVerdictRowId,
        regressionVerdictId);
  }

  private static void requirePair(final UUID rowId, final String businessId, final String field) {
    if ((rowId == null) != (businessId == null)) {
      throw new IllegalArgumentException(field + " row UUID and business ID must be paired");
    }
  }
}
