package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.util.function.Supplier;

/** Usecase-owned本地事务边界；production实现必须使用同一DH PostgreSQL transaction manager。 */
public interface GuardTransactionBoundary {

  /**
   * 在required transaction中执行guard、audit和result状态动作。
   *
   * @param action 不得包含外部IO。
   * @param <T> 返回类型。
   * @return transaction提交后的结果。
   */
  <T> T required(Supplier<T> action);
}
