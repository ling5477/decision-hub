package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionContextSnapshot;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import java.util.List;
import java.util.Objects;

/**
 * K2 默认上下文构造器。
 *
 * <p>实现只复制 request.contextSnapshot.evidenceRefs，不做 RAG、检索、DB 访问、NQ 访问或外部调用。该类无可变状态，
 * 可在多线程下复用。
 */
public final class DefaultDecisionContextBuilder implements DecisionContextBuilder {

  /**
   * 从 K1 request 复制 evidence 引用。
   *
   * @param request K1 冻结的只读请求
   * @return 只读编排上下文；snapshot 为空时 evidence 为空，由 orchestrator fail-closed
   */
  @Override
  public DecisionContext build(final DecisionRequest request) {
    Objects.requireNonNull(request, "request");
    final DecisionContextSnapshot snapshot = request.getContextSnapshot();
    final List<String> evidenceRefs = snapshot == null ? List.of() : snapshot.evidenceRefs();
    return new DecisionContext(request, evidenceRefs);
  }
}
