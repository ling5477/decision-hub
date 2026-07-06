package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * stage-qdr-2 B2 read model 查询服务。
 *
 * <p>本服务只做 tenant-bound query orchestration 与输入 fail-closed 校验；不依赖 Spring Web、JDBC、
 * API、provider SDK、Agent runtime 或外部 HTTP。trace 查询先确认当前 tenant 下的 run 存在，避免
 * 对不存在或跨租户资源返回任何 trace/evidence 细节。
 */
public final class DecisionReadModelService {

    private final DecisionReadModelQueryPort queryPort;

    /**
     * 创建 read model query service。
     *
     * @param queryPort tenant-bound 只读查询 port。
     */
    public DecisionReadModelService(final DecisionReadModelQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "queryPort");
    }

    /**
     * 读取 decision run 详情。
     *
     * @param query 已认证 tenant 绑定查询。
     * @return 当前 tenant 下的详情；不存在或跨租户时 empty。
     */
    public Optional<DecisionRunDetailView> findDecisionRunDetail(final DecisionRunReadQuery query) {
        final DecisionRunReadQuery checked = Objects.requireNonNull(query, "query");
        requireUuidText(checked.decisionRunId(), "decisionRunId");
        return queryPort.findDecisionRunDetail(checked);
    }

    /**
     * 读取 decision run trace timeline。
     *
     * @param query 已认证 tenant 绑定 trace 查询。
     * @return 当前 tenant 下的 timeline；不存在或跨租户时 empty。
     */
    public Optional<DecisionTraceTimelineView> findDecisionTrace(
            final DecisionTraceReadQuery query) {
        final DecisionTraceReadQuery checked = Objects.requireNonNull(query, "query");
        requireUuidText(checked.decisionRunId(), "decisionRunId");
        final DecisionRunReadQuery existenceQuery =
                new DecisionRunReadQuery(
                        checked.tenantId(), checked.decisionRunId(), checked.requesterId(), checked.traceId());
        if (queryPort.findDecisionRunDetail(existenceQuery).isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(queryPort.getDecisionTrace(checked));
    }

    /**
     * 读取 decision run evidence refs。
     *
     * @param query 已认证 tenant 绑定 evidence 查询。
     * @return 当前 tenant 下的脱敏 evidence 引用；不存在或跨租户时 empty。
     */
    public Optional<DecisionEvidenceView> findDecisionEvidence(final DecisionEvidenceReadQuery query) {
        final DecisionEvidenceReadQuery checked = Objects.requireNonNull(query, "query");
        requireUuidText(checked.decisionRunId(), "decisionRunId");
        return queryPort.findDecisionEvidence(checked);
    }

    private static void requireUuidText(final String value, final String field) {
        try {
            UUID.fromString(ReadModelValidation.requireText(value, field));
        } catch (final IllegalArgumentException error) {
            throw new IllegalArgumentException(field + " must be UUID", error);
        }
    }
}
