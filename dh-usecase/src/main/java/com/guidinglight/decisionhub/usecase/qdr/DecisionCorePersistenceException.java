package com.guidinglight.decisionhub.usecase.qdr;

/**
 * Decision Core 主线持久化异常。
 *
 * <p>repository adapter 发生数据库写入、查询或 JSON 转换失败时必须抛出本异常或等价 RuntimeException，
 * 由 dry-run usecase fail-closed；不得吞异常后继续返回成功。
 */
public final class DecisionCorePersistenceException extends RuntimeException {

  /**
   * 创建持久化异常。
   *
   * @param message 脱敏错误摘要。
   * @param cause 原始异常。
   */
  public DecisionCorePersistenceException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
