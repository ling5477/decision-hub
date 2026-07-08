package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;

import java.time.Instant;

/**
 * Stage-QDR-4 B4 regression report 查询条件。
 *
 * <p>该 query 只服务内部 read model。`tenantId` 是所有查询的第一边界，本类型不提供 UUID-only
 * selector；列表查询始终携带 `limit / offset`，并复用 B2 `ReplayPageRequest` 的 100 行上限。
 *
 * @param tenantId         tenant ID，必填。
 * @param caseId           租户内 replay case ID。
 * @param evaluationId     租户内 evaluation ID。
 * @param verdictId        租户内 regression verdict ID。
 * @param traceId          traceId。
 * @param sourceRequestId  来源 request ID。
 * @param sourceDecisionId 来源 decision ID。
 * @param verdict          regression verdict 状态过滤。
 * @param severity         finding severity 过滤。
 * @param createdFrom      创建时间下界，包含。
 * @param createdTo        创建时间上界，包含。
 * @param limit            page size，范围 1..100。
 * @param offset           page offset，必须非负。
 */
public record RegressionReportQuery(
        String tenantId,
        String caseId,
        String evaluationId,
        String verdictId,
        String traceId,
        String sourceRequestId,
        String sourceDecisionId,
        RegressionVerdict.Status verdict,
        RegressionSeverity severity,
        Instant createdFrom,
        Instant createdTo,
        int limit,
        int offset) {

    /**
     * 校验 tenant-bound 查询条件和分页边界。
     */
    public RegressionReportQuery {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId is required");
        }
        tenantId = ReplayPersistenceGuard.requireTenantId(tenantId);
        caseId = ReplayPersistenceGuard.optionalSafeText(caseId, "caseId");
        evaluationId = ReplayPersistenceGuard.optionalSafeText(evaluationId, "evaluationId");
        verdictId = ReplayPersistenceGuard.optionalSafeText(verdictId, "verdictId");
        traceId = ReplayPersistenceGuard.optionalSafeText(traceId, "traceId");
        sourceRequestId = ReplayPersistenceGuard.optionalSafeText(sourceRequestId, "sourceRequestId");
        sourceDecisionId = ReplayPersistenceGuard.optionalSafeText(sourceDecisionId, "sourceDecisionId");
        new ReplayPageRequest(limit, offset);
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new IllegalArgumentException("createdFrom must not be after createdTo");
        }
    }

    boolean hasCaseSelector() {
        return caseId != null;
    }

    boolean hasEvaluationSelector() {
        return evaluationId != null;
    }

    boolean hasVerdictSelector() {
        return verdictId != null;
    }

    boolean hasTraceSelector() {
        return traceId != null;
    }

    boolean hasSourceRequestSelector() {
        return sourceRequestId != null;
    }
}
