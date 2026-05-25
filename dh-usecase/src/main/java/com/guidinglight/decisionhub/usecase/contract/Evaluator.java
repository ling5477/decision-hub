package com.guidinglight.decisionhub.usecase.contract;

/** @deprecated Stage1-CLOSE：旧 Evaluator 抽象。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public interface Evaluator {
    String name();
    EvalResult evaluate(EvalRequest request);
}
