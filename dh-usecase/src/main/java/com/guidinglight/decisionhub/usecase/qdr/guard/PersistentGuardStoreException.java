package com.guidinglight.decisionhub.usecase.qdr.guard;

/** PostgreSQL guard store不可用或SQL失败；调用方必须拒绝，禁止in-memory fallback。 */
public final class PersistentGuardStoreException extends PersistentGuardException {

  /** 创建脱敏store异常。 */
  public PersistentGuardStoreException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
