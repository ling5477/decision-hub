package com.guidinglight.decisionhub.usecase.qdr.snapshot;

/**
 * Canonical replay snapshot persistence 的结构化 fail-closed 异常。
 *
 * <p>该异常用于区分数据库/映射/identity 验证失败与正常 empty 查询；调用方不得将其转换为成功或空结果。
 */
public class CanonicalReplaySnapshotPersistenceException extends RuntimeException {

  /**
   * 创建无底层 cause 的结构化失败。
   *
   * @param message 不含敏感材料的失败说明。
   */
  public CanonicalReplaySnapshotPersistenceException(final String message) {
    super(message);
  }

  /**
   * 创建保留底层 cause 的结构化失败。
   *
   * @param message 不含敏感材料的失败说明。
   * @param cause JDBC、映射或校验 cause。
   */
  public CanonicalReplaySnapshotPersistenceException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
