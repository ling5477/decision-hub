package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionProviderBudget;

/**
 * K5 provider budget guard 端口。
 *
 * <p>预算检查只做本地 deterministic 估算，用于防止未来 provider loop 或过量调用进入不可控状态；不得读取真实账单、
 * 真实模型用量、credential 或外部 provider API。
 */
public interface DecisionProviderBudgetGuard {

    /**
     * 检查本次 mock provider 调用是否在预算内。
     *
     * @param providerName provider 名称
     * @param context      单次只读 decision context
     * @return 本地预算估算结果
     */
    DecisionProviderBudget check(String providerName, DecisionContext context);
}
