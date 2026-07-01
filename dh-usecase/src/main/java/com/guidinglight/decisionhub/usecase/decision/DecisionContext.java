package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import java.util.List;
import java.util.Objects;

/**
 * K2 编排内部只读上下文。
 *
 * <p>该对象只承接 K1 request 和 snapshot evidence 引用，不读取数据库、文件、NQ runtime 或外部
 * provider。线程安全性来自不可变字段，生命周期限定在单次编排调用内。
 */
public record DecisionContext(DecisionRequest request, List<String> evidenceRefs) {

  /** 规范化 evidence 引用，避免后续编排阶段修改输入快照。 */
  public DecisionContext {
    request = Objects.requireNonNull(request, "request");
    evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
  }

  /** 返回是否具备可审计的 evidence 引用；无 evidence 时必须 fail-closed。 */
  public boolean hasEvidence() {
    return !evidenceRefs.isEmpty();
  }
}
