package com.guidinglight.decisionhub.usecase.decision;

/**
 * K3 持久化失败异常。
 *
 * <p>Repository 实现必须用该异常或其 cause 向 orchestrator 报告失败；orchestrator 捕获后返回
 * ABSTAIN/BLOCKED 的结构化 fail-closed output，不得把失败误判为普通成功。
 */
public final class DecisionPersistenceException extends RuntimeException {

  /** 创建带原因的持久化异常。 */
  public DecisionPersistenceException(final String message, final Throwable cause) {
    super(message, cause);
  }

  /** 创建不带底层 cause 的持久化异常。 */
  public DecisionPersistenceException(final String message) {
    super(message);
  }
}
