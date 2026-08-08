package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/**
 * Stage-QDR-10 internal-only consolidated evidence query。
 *
 * <p>{@link FeedbackExecutionScope} 是 tenant/environment 的唯一 authority；本 query 不接受第二组
 * tenant/environment 字符串，也不从 profile、repository 或 feedback row 推断环境。
 */
public record DecisionFeedbackEvidenceQuery(
        FeedbackExecutionScope executionScope,
        String traceId,
        String requestId,
        String decisionId,
        String decisionRunId,
        Instant fromObservedAt,
        Instant toObservedAt,
        int maxFeedbackItems) {

    private static final Duration MAX_RANGE = Duration.ofDays(90);

    /** 构造期冻结强关联键、90 天窗口与单页 1..100 上限。 */
    public DecisionFeedbackEvidenceQuery {
        if (executionScope == null) {
            throw new IllegalArgumentException("EXECUTION_SCOPE_REQUIRED");
        }
        if (executionScope.environment() == null) {
            throw new IllegalArgumentException("ENVIRONMENT_INVALID");
        }
        traceId = requireIdentity(traceId, "traceId");
        requestId = requireIdentity(requestId, "requestId");
        decisionId = requireIdentity(decisionId, "decisionId");
        decisionRunId = requireIdentity(decisionRunId, "decisionRunId");
        fromObservedAt = Objects.requireNonNull(fromObservedAt, "fromObservedAt");
        toObservedAt = Objects.requireNonNull(toObservedAt, "toObservedAt");
        if (fromObservedAt.isAfter(toObservedAt)
                || Duration.between(fromObservedAt, toObservedAt).compareTo(MAX_RANGE) > 0) {
            throw new IllegalArgumentException("feedback evidence range must be within 90 days");
        }
        if (maxFeedbackItems < 1 || maxFeedbackItems > HistoricalFeedbackEvidenceQuery.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("maxFeedbackItems must be within [1,100]");
        }
    }

    /** 创建固定 CORE_DECISION 且强制 RUN selector 的既有 decision query。 */
    public DecisionEvidenceQuery decisionEvidenceQuery() {
        return new DecisionEvidenceQuery(
                executionScope.tenantId(),
                traceId,
                requestId,
                decisionId,
                decisionRunId,
                null,
                null,
                null,
                null,
                DecisionEvidencePolicy.CORE_DECISION);
    }

    /** 创建 tenant/environment/decision/trace-bound 的单页 historical feedback query。 */
    public HistoricalFeedbackEvidenceQuery feedbackEvidenceQuery() {
        return new HistoricalFeedbackEvidenceQuery(
                executionScope.tenantId(),
                executionScope.environment(),
                fromObservedAt,
                toObservedAt,
                maxFeedbackItems,
                null,
                decisionId,
                traceId,
                null,
                null,
                null,
                null,
                null);
    }

    /** 返回 decision side 的严格 tenant/trace/request/decision correlation。 */
    public DecisionEvidenceCorrelation decisionCorrelation() {
        return decisionEvidenceQuery().correlation();
    }

    private static String requireIdentity(final String value, final String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }
        return DecisionEvidencePolicy.requireSafeText(value, field);
    }
}
