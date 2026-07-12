package com.guidinglight.decisionhub.usecase.qdr.guard;

/** CAS丢失、非法转换、unknown/corrupt row等状态错误；不得自动重放业务。 */
public final class PersistentGuardStateException extends PersistentGuardException {

  /** 创建脱敏状态异常。 */
  public PersistentGuardStateException(final String message) {
    super(message);
  }
}
