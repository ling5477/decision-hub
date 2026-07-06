package com.guidinglight.decisionhub.usecase.qdr.readmodel;

/**
 * QDR read model 查询不可用异常。
 *
 * <p>用于承载 JDBC 读取失败、持久化数据不满足只读安全合同、或 projection 构造被 fail-closed
 * 校验拒绝的场景。调用方必须把该异常转换为普通失败响应，不得回退为 success / approved / low risk。
 */
public final class DecisionReadModelUnavailableException extends RuntimeException {

    /**
     * 创建 read model 不可用异常。
     *
     * @param message 安全错误摘要，不包含 raw provider response 或凭证。
     * @param cause   原始异常；调用方不得向 API 响应透出 cause message。
     */
    public DecisionReadModelUnavailableException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
