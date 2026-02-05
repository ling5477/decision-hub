package com.guidinglight.decisionhub.usecase.gate.evaluator;

import java.util.*;

/**
 * v1 规则版：CompletenessEvaluator
 * - checklist / prioritization / actionable_next 命中计分（3/2/1/0）
 */
public class CompletenessEvaluatorV1 {

    public Result evaluate(Map<String, Object> decisionRecord, String runId) {
        String text = OutputResolverV1.resolveModelRunText(decisionRecord, runId);

        boolean checklist = containsAny(text, List.of("-", "*", "1.", "2.", "✅", "•"));
        boolean prioritization = containsAny(text, List.of("必须", "建议", "可选", "优先级", "P0", "P1"));
        boolean actionable = containsAny(text, List.of("下一步", "建议先", "可以直接", "落地", "执行"));

        int hit = (checklist ? 1 : 0) + (prioritization ? 1 : 0) + (actionable ? 1 : 0);
        double score;
        if (hit == 3) score = 1.0;
        else if (hit == 2) score = 0.7;
        else if (hit == 1) score = 0.4;
        else score = 0.0;

        boolean passed = score >= 0.6;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("hit.checklist", checklist);
        metrics.put("hit.prioritization", prioritization);
        metrics.put("hit.actionable_next", actionable);
        metrics.put("hit_count", hit);

        return new Result(score, passed, passed ? "完整性通过" : "完整性不足", metrics);
    }

    private boolean containsAny(String text, List<String> keys) {
        if (text == null) return false;
        for (String k : keys) {
            if (text.contains(k)) return true;
        }
        return false;
    }

    public static final class Result {
        public final double score;
        public final boolean passed;
        public final String reason;
        public final Map<String, Object> metrics;

        public Result(double score, boolean passed, String reason, Map<String, Object> metrics) {
            this.score = score;
            this.passed = passed;
            this.reason = reason;
            this.metrics = metrics;
        }
    }
}
