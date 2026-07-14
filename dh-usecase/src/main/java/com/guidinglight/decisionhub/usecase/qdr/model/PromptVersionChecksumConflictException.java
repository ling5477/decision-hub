package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * PromptVersion immutable definition conflict。
 *
 * <p>同一 tenant/template/version 只能幂等保存相同稳定身份与语义内容；checksum 或其他行为字段不一致都必须 fail-closed，不能覆盖 canonical
 * row。类名为既有兼容合同，异常语义已覆盖完整 immutable definition。
 */
public final class PromptVersionChecksumConflictException
    extends PromptVersionPersistenceException {

  /** 创建 checksum conflict 异常。 */
  public PromptVersionChecksumConflictException() {
    super("prompt version semantic/checksum conflict");
  }
}
