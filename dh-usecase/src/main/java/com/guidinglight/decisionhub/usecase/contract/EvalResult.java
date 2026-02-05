package com.guidinglight.decisionhub.usecase.contract;

import java.util.HashMap;
import java.util.Map;

public class EvalResult {
    private String evaluator;
    private TargetRef target;
    private double score;
    private boolean passed;
    private String reason;
    private Map<String, Object> metrics = new HashMap<>();

    public String getEvaluator() { return evaluator; }
    public void setEvaluator(String evaluator) { this.evaluator = evaluator; }
    public TargetRef getTarget() { return target; }
    public void setTarget(TargetRef target) { this.target = target; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Map<String, Object> getMetrics() { return metrics; }
    public void setMetrics(Map<String, Object> metrics) { this.metrics = metrics; }
}
