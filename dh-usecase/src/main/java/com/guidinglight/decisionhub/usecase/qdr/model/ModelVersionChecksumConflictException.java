package com.guidinglight.decisionhub.usecase.qdr.model;

/**
 * ModelVersion duplicate key checksum conflict。
 *
 * <p>同一 tenant/model version identity 只能幂等保存相同 checksum；不同 checksum 必须 fail-closed。
 */
public final class ModelVersionChecksumConflictException extends ModelVersionPersistenceException {

    /**
     * 创建 checksum conflict 异常。
     */
    public ModelVersionChecksumConflictException() {
        super("model version checksum conflict");
    }
}
