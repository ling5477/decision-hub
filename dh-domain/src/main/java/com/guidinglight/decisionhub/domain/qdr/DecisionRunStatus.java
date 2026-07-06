package com.guidinglight.decisionhub.domain.qdr;

/**
 * Quant Decision Review run 状态。
 *
 * <p>所有未知或异常路径必须 fail-closed 为 `FAILED`，不得返回普通成功。
 */
public enum DecisionRunStatus {
    /**
     * run 已创建，编排仍在执行。
     */
    RUNNING,

    /**
     * run 已生成只读 quant decision。
     */
    SUCCEEDED,

    /**
     * run 因安全、策略、provider guard、审计或持久化失败而关闭。
     */
    FAILED
}
