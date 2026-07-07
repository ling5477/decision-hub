package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * Model / provider profile 校验异常。
 *
 * <p>异常消息只包含字段级固定原因，不输出 profile 原始值、凭证、token 或 secret-like material。
 */
public final class ModelVersionValidationException extends IllegalArgumentException {

    private ModelVersionValidationException(final String message) {
        super(message);
    }

    /**
     * 创建疑似密钥材料异常。
     *
     * @param field 字段名。
     * @return 安全异常。
     */
    public static ModelVersionValidationException secretLikeMaterial(final String field) {
        return new ModelVersionValidationException(field + " rejected: secret-like material");
    }

    /**
     * 创建 checksum mismatch 异常。
     *
     * @return 安全异常。
     */
    public static ModelVersionValidationException checksumMismatch() {
        return new ModelVersionValidationException("model version checksum mismatch");
    }

    /**
     * 创建资源边界异常。
     *
     * @param message 固定安全消息。
     * @return 安全异常。
     */
    public static ModelVersionValidationException invalidBounds(final String message) {
        return new ModelVersionValidationException(message);
    }
}
