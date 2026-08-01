package com.guidinglight.decisionhub.usecase.agent.feedback;

import java.util.function.Supplier;

/** Legacy feedback ingest 的原子执行边界；实现必须提供 all-or-nothing 语义。 */
public interface NqFeedbackIngestionUnitOfWork {

  /** 在 required 原子边界内执行完整 ingestion，失败时不得保留部分状态。 */
  <T> T required(Supplier<T> action);
}
