package com.guidinglight.decisionhub.usecase.qdr.replay;

/**
 * QDR replay / evaluation persistence fail-closed exception。
 *
 * <p>repository port 或 JDBC adapter 在校验、序列化、引用一致性或数据库访问失败时必须抛出本异常或子类；
 * 错误消息只能包含脱敏摘要，不得回显 raw prompt、raw provider response 或 credential。
 */
public class ReplayPersistenceException extends RuntimeException {

    /**
     * 创建持久化异常。
     *
     * @param message 脱敏错误摘要。
     */
    public ReplayPersistenceException(final String message) {
        super(message);
    }

    /**
     * 创建持久化异常。
     *
     * @param message 脱敏错误摘要。
     * @param cause   原始异常。
     */
    public ReplayPersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
