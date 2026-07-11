package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceCorrelation;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;

import java.util.Objects;
import java.util.UUID;

/**
 * Canonical replay snapshot 的 tenant-bound physical/business identity。
 *
 * <p>该合同复用 B1 {@link DecisionEvidenceCorrelation}，不复制 tenant/trace/request/decision
 * correlation 模型；其余字段只表达 V6/V8/V9 physical UUID 与 business ref 的明确映射。它不读取
 * Repository、不执行 JDBC，也不允许 UUID-only 或 tenantless identity。
 *
 * @param correlation             B1 tenant-bound 四键 correlation。
 * @param snapshotId              tenant 内 immutable snapshot business ID。
 * @param decisionRequestId       V6 request 物理 UUID。
 * @param decisionRunId           V6 run 物理 UUID。
 * @param modelCallId             V8 gateway call 物理 UUID。
 * @param modelCallRef            V8 gateway call 安全业务引用。
 * @param promptVersionId         V8 prompt version 物理 UUID。
 * @param modelVersionId          V8 model version 物理 UUID。
 * @param replayCaseRowId         V9 replay case 物理 UUID。
 * @param replayCaseId            V9 replay case business ID。
 * @param evaluationCaseRowId     可选 V9 evaluation 物理 UUID。
 * @param evaluationCaseId        可选 V9 evaluation business ID。
 * @param regressionVerdictRowId  可选 V9 verdict 物理 UUID。
 * @param regressionVerdictId     可选 V9 verdict business ID。
 */
public record CanonicalReplaySnapshotIdentity(
        DecisionEvidenceCorrelation correlation,
        String snapshotId,
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
        String regressionVerdictId) {

    /**
     * 校验 tenant-bound identity 与 optional lineage pair。
     *
     * <p>optional row UUID/business ID 必须同时为空或同时存在，避免 JDBC 层通过时间、字符串相似或
     * latest row 猜测另一半 identity。
     */
    public CanonicalReplaySnapshotIdentity {
        correlation = Objects.requireNonNull(correlation, "correlation");
        snapshotId = ReplayPersistenceGuard.requireSafeText(snapshotId, "snapshotId");
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
        requirePair(evaluationCaseRowId, evaluationCaseId, "evaluationCase");
        requirePair(regressionVerdictRowId, regressionVerdictId, "regressionVerdict");
    }

    /**
     * 返回第一安全边界 tenantId。
     *
     * @return B1 correlation 中已校验的 tenantId。
     */
    public String tenantId() {
        return correlation.tenantId();
    }

    private static void requirePair(final UUID rowId, final String businessId, final String field) {
        if ((rowId == null) != (businessId == null)) {
            throw new IllegalArgumentException(field + " row UUID and business ID must be both present or absent");
        }
    }
}
