package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import java.util.Optional;

/**
 * stage-qdr-2 B1 read model query port。
 *
 * <p>该端口只定义 tenant-bound 只读查询 contract；B1 不提供 JDBC/API 实现。实现方必须保持只读，
 * 不得触发 replay execution、approval write、外部 HTTP、真实 provider、Agent runtime 或 LIVE 行为。
 */
public interface DecisionReadModelQueryPort {

    /**
     * 查询单条 decision run 详情。
     *
     * @param query tenant-bound 查询条件。
     * @return 当前 tenant 下的详情；不存在或无权访问时返回 empty，不得返回跨 tenant 数据。
     */
    Optional<DecisionRunDetailView> findDecisionRunDetail(DecisionRunReadQuery query);

    /**
     * 查询 decision run trace timeline。
     *
     * @param query tenant-bound trace 查询条件。
     * @return 只读 trace timeline；实现不得通过 replay 补写数据。
     */
    DecisionTraceTimelineView getDecisionTrace(DecisionTraceReadQuery query);

    /**
     * 查询 decision run evidence 引用。
     *
     * @param query tenant-bound evidence 查询条件。
     * @return 脱敏 evidence 引用；不存在或无权访问时返回 empty。
     */
    Optional<DecisionEvidenceView> findDecisionEvidence(DecisionEvidenceReadQuery query);
}
