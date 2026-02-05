package com.guidinglight.decisionhub.usecase.gate.evaluator;

import java.util.*;

/**
 * v1 规则版：ConsistencyEvaluator
 * - 语言一致（cjk_ratio）
 * - 声明一致：若文本中声称使用 web/mcp/skills，则必须在 tool_traces 中找到对应痕迹（否则冲突）
 */
public class ConsistencyEvaluatorV1 {

    @SuppressWarnings("unchecked")
    public Result evaluate(Map<String, Object> decisionRecord, String runId) {
        String text = OutputResolverV1.resolveModelRunText(decisionRecord, runId);

        double score = 0.0;
        List<String> conflicts = new ArrayList<>();

        double ratio = OutputResolverV1.cjkRatio(text);
        if (ratio >= 0.25) score += 0.5;

        boolean claimWeb = text.contains("web.run") || text.contains("联网") || text.contains("搜索");
        boolean claimMcp = text.toLowerCase().contains("mcp");
        boolean claimSkills = text.toLowerCase().contains("skills") || text.contains("skill");

        boolean evidenceOk = true;
        List<Map<String, Object>> traces = OutputResolverV1.resolveToolTraces(decisionRecord, runId);

        if (claimWeb && !OutputResolverV1.traceContains(traces, "web.run")) {
            conflicts.add("claim_web_without_trace");
            evidenceOk = false;
        }
        if (claimMcp && !OutputResolverV1.traceContains(traces, "mcp")) {
            conflicts.add("claim_mcp_without_trace");
            evidenceOk = false;
        }
        if (claimSkills && !OutputResolverV1.traceContains(traces, "skills")) {
            conflicts.add("claim_skills_without_trace");
            evidenceOk = false;
        }

        if (evidenceOk) score += 0.5;

        boolean passed = conflicts.isEmpty() && score >= 0.6;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("lang.cjk_ratio", ratio);
        metrics.put("claim.web_used", claimWeb);
        metrics.put("claim.mcp_used", claimMcp);
        metrics.put("claim.skills_used", claimSkills);
        metrics.put("conflicts", conflicts);

        return new Result(score, passed, passed ? "一致性通过" : ("一致性冲突=" + conflicts), metrics);
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
