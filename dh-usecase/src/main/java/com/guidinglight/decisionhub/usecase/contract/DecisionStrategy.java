package com.guidinglight.decisionhub.usecase.contract;

import java.util.Map;

public interface DecisionStrategy {
    String type();
    DecisionOutcome decide(DecisionRequest request, Map<String, Object> strategyConfig);
}
