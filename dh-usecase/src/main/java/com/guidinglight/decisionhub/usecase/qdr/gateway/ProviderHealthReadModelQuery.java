package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.time.Instant;

/**
 * Stage-QDR-5 B2 provider health / gateway call 内部 read model 查询条件。
 *
 * <p>`tenantId` 是强制边界；本 query 不提供 UUID-only selector。列表查询必须携带至少一个 selector 或
 * 时间窗口，并显式分页，避免 tenantless / unbounded read。
 *
 * @param tenantId                tenant 边界，必填。
 * @param providerRef             provider safe ref。
 * @param modelGatewayVersionRef  model gateway version safe ref。
 * @param traceId                 traceId。
 * @param sourceRequestId         source request safe ref。
 * @param failureClassification   failure classification 过滤。
 * @param trustDecision           trust decision 过滤。
 * @param readinessStatus         readiness status 过滤。
 * @param createdFrom             创建时间下界，包含。
 * @param createdTo               创建时间上界，包含。
 * @param observedFrom            观察时间下界，包含。
 * @param observedTo              观察时间上界，包含。
 * @param limit                   page size，范围 1..100。
 * @param offset                  page offset，必须非负。
 */
public record ProviderHealthReadModelQuery(
        String tenantId,
        String providerRef,
        String modelGatewayVersionRef,
        String traceId,
        String sourceRequestId,
        ProviderFailureClassification failureClassification,
        ProviderTrustDecisionSummary.Decision trustDecision,
        ProviderReadinessStatus readinessStatus,
        Instant createdFrom,
        Instant createdTo,
        Instant observedFrom,
        Instant observedTo,
        int limit,
        int offset) {

    /** 单页最大行数。 */
    public static final int MAX_LIMIT = 100;

    /** 校验 tenant-bound selector/window 与分页边界。 */
    public ProviderHealthReadModelQuery {
        tenantId = QdrPersistenceSafety.requireSafeText(tenantId, "tenantId");
        providerRef = optionalSafeText(providerRef, "providerRef");
        modelGatewayVersionRef = optionalSafeText(modelGatewayVersionRef, "modelGatewayVersionRef");
        traceId = optionalSafeText(traceId, "traceId");
        sourceRequestId = optionalSafeText(sourceRequestId, "sourceRequestId");
        validateRange(createdFrom, createdTo, "created");
        validateRange(observedFrom, observedTo, "observed");
        validatePage(limit, offset);
        if (!hasSelector(
                providerRef,
                modelGatewayVersionRef,
                traceId,
                sourceRequestId,
                failureClassification,
                trustDecision,
                readinessStatus)
                && !hasWindow(createdFrom, createdTo, observedFrom, observedTo)) {
            throw new IllegalArgumentException("selector or list window is required");
        }
    }

    boolean hasProviderSelector() {
        return providerRef != null;
    }

    boolean hasModelGatewayVersionSelector() {
        return modelGatewayVersionRef != null;
    }

    boolean hasTraceSelector() {
        return traceId != null;
    }

    boolean hasSourceRequestSelector() {
        return sourceRequestId != null;
    }

    private static String optionalSafeText(final String value, final String field) {
        return QdrPersistenceSafety.optionalSafeText(value, field);
    }

    private static void validateRange(
            final Instant from, final Instant to, final String label) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException(label + "From must not be after " + label + "To");
        }
    }

    private static void validatePage(final int limit, final int offset) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        if (limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must not exceed 100");
        }
        if (offset < 0) {
            throw new IllegalArgumentException("offset must be non-negative");
        }
    }

    private static boolean hasSelector(
            final String providerRef,
            final String modelGatewayVersionRef,
            final String traceId,
            final String sourceRequestId,
            final ProviderFailureClassification failureClassification,
            final ProviderTrustDecisionSummary.Decision trustDecision,
            final ProviderReadinessStatus readinessStatus) {
        return providerRef != null
                || modelGatewayVersionRef != null
                || traceId != null
                || sourceRequestId != null
                || failureClassification != null
                || trustDecision != null
                || readinessStatus != null;
    }

    private static boolean hasWindow(
            final Instant createdFrom,
            final Instant createdTo,
            final Instant observedFrom,
            final Instant observedTo) {
        return createdFrom != null || createdTo != null || observedFrom != null || observedTo != null;
    }
}
