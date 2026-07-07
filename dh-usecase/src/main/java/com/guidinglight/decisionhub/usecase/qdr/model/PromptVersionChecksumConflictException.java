package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * PromptVersion duplicate key checksum conflict。
 *
 * <p>同一 tenant/template/version 只能幂等保存相同 checksum；不同 checksum 必须 fail-closed，不能覆盖。
 */
public final class PromptVersionChecksumConflictException extends PromptVersionPersistenceException {

    /**
     * 创建 checksum conflict 异常。
     */
    public PromptVersionChecksumConflictException() {
        super("prompt version checksum conflict");
    }
}
