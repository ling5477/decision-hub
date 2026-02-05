package com.guidinglight.decisionhub.usecase.gate.evaluator;

import java.util.*;
import java.util.regex.Pattern;

/**
 * v1 规则版：ConstraintEvaluator
 * - language/no_web/output_format/must_include/must_not_include
 * - 约束违反 => passed=false（硬门槛）
 */
public class ConstraintEvaluatorV1 {

    @SuppressWarnings("unchecked")
    public Result evaluate(Map<String, Object> decisionRecord, String runId) {
        Map<String, Object> input = (Map<String, Object>) decisionRecord.get("input_context");
        Map<String, Object> constraints = input == null ? Map.of() : (Map<String, Object>) input.getOrDefault("constraints", Map.of());

        String text = OutputResolverV1.resolveModelRunText(decisionRecord, runId);

        List<Map<String, String>> items = new ArrayList<>();
        int violations = 0;

        Object lang = constraints.get("language");
        if (lang != null && "zh-CN".equals(String.valueOf(lang))) {
            double ratio = OutputResolverV1.cjkRatio(text);
            if (ratio < 0.25) {
                violations++;
                items.add(Map.of("key", "language", "detail", "cjk_ratio<0.25"));
            }
        }

        Object mustInclude = constraints.get("must_include");
        if (mustInclude instanceof List) {
            for (Object k : (List<?>) mustInclude) {
                String kw = String.valueOf(k);
                if (!text.contains(kw)) {
                    violations++;
                    items.add(Map.of("key", "must_include", "detail", "missing:" + kw));
                }
            }
        }

        Object mustNotInclude = constraints.get("must_not_include");
        if (mustNotInclude instanceof List) {
            for (Object k : (List<?>) mustNotInclude) {
                String kw = String.valueOf(k);
                if (text.contains(kw)) {
                    violations++;
                    items.add(Map.of("key", "must_not_include", "detail", "found:" + kw));
                }
            }
        }

        boolean passed = violations == 0;
        double score = passed ? 1.0 : 0.0;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("violations", violations);
        metrics.put("items", items);

        return new Result(score, passed, passed ? "无硬约束违反" : ("硬约束违反数=" + violations), metrics);
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
