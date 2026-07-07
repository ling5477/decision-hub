package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model budget exceeded exception。
 */
public final class ModelBudgetExceededException extends ModelGatewayException {

    /**
     * 创建 budget exceeded 异常。
     */
    public ModelBudgetExceededException() {
        super(ModelGatewayFailureCode.BUDGET_EXCEEDED);
    }
}
