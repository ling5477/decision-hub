package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.util.function.Supplier;

/**
 * Feedback aggregate 的 usecase-owned强制事务边界。
 *
 * <p>B2 production 实现必须使用 DH PostgreSQL transaction manager、PROPAGATION_REQUIRED 与
 * REPEATABLE_READ；缺少 transaction manager 时 fail-fast。
 */
public interface FeedbackPersistenceTransactionBoundary {

  /**
   * 在单个 required/repeatable-read transaction 中执行无外部 IO 的 aggregate 动作。
   *
   * @param action observation、attribution、children 与 exact read-back 动作
   * @param <T> transaction 返回类型
   * @return transaction 提交后的值
   */
  <T> T requiredRepeatableRead(Supplier<T> action);
}
