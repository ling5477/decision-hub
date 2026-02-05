package com.guidinglight.decisionhub.usecase.contract;

public interface Evaluator {
    String name();
    EvalResult evaluate(EvalRequest request);
}
