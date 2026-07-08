package com.guidinglight.decisionhub.usecase.qdr.replay;

/**
 * QDR decision summary 持久化角色。
 *
 * <p>`EXPECTED` 与 `ACTUAL` 都只是 replay/evaluation summary，不是交易执行意图，也不触发 provider。
 */
public enum DecisionSummaryRole {
    /**
     * 回放基线预期 summary。
     */
    EXPECTED,

    /**
     * 评估产物实际 summary。
     */
    ACTUAL
}
