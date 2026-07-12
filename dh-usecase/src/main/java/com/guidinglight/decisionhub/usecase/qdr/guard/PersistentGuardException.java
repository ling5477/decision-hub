package com.guidinglight.decisionhub.usecase.qdr.guard;

/** Persistent guard fail-closed异常基类；message不得携带identity、hash或lease token。 */
public class PersistentGuardException extends RuntimeException {

  /** 创建脱敏异常。 */
  public PersistentGuardException(final String message) {
    super(message);
  }

  /** 创建带底层原因的脱敏异常。 */
  public PersistentGuardException(final String message, final Throwable cause) {
    super(message, cause);
  }
}
