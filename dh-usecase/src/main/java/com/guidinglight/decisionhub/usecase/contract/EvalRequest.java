package com.guidinglight.decisionhub.usecase.contract;

import java.util.Map;

/** @deprecated Stage1-CLOSE：旧 Evaluator 输入 DTO。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class EvalRequest {
    private String evaluator;
    private TargetRef target;
    private Map<String, Object> decisionRecord;

    public String getEvaluator() { return evaluator; }
    public void setEvaluator(String evaluator) { this.evaluator = evaluator; }
    public TargetRef getTarget() { return target; }
    public void setTarget(TargetRef target) { this.target = target; }
    public Map<String, Object> getDecisionRecord() { return decisionRecord; }
    public void setDecisionRecord(Map<String, Object> decisionRecord) { this.decisionRecord = decisionRecord; }
}
