package com.guidinglight.decisionhub.usecase.contract;

import java.util.Map;

/** @deprecated Stage1-CLOSE：旧决策策略抽象。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public interface DecisionStrategy {
    String type();
    DecisionOutcome decide(DecisionRequest request, Map<String, Object> strategyConfig);
}
