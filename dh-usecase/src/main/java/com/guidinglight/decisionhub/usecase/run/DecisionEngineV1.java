package com.guidinglight.decisionhub.usecase.run;

import com.guidinglight.decisionhub.usecase.contract.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * v1 冻结版：
 * - Gate：constraint + consistency 必须 passed
 * - 质量分：Q = 0.50*completeness + 0.30*consistency + 0.20*constraint
 * - 策略：hybrid（rule_based -> weighted_vote -> majority_vote）
 *
 * 注意：该类是“骨架”，你可以把真正的模型调用/评估器执行/配置加载放到外层服务里。
 *
 * @deprecated Stage1-CLOSE：旧多模型平台决策引擎，新链路使用
 *     {@link com.guidinglight.decisionhub.usecase.agent.JudgeDecisionService}。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class DecisionEngineV1 {

    public static List<String> REQUIRED_GATE = Arrays.asList("constraint", "consistency");

    @SuppressWarnings("unchecked")
    public List<String> gateCandidates(Map<String, Object> decisionRecord) {
        List<Map<String, Object>> evals = (List<Map<String, Object>>) decisionRecord.getOrDefault("eval_results", Collections.emptyList());

        // 收集每个 run 的 evaluator -> passed/score
        Map<String, Map<String, Map<String, Object>>> byRun = new HashMap<>();
        for (Map<String, Object> e : evals) {
            Map<String, Object> tgt = (Map<String, Object>) e.get("target");
            if (tgt == null) continue;
            if (!"model_run".equals(String.valueOf(tgt.get("type")))) continue;
            String ref = String.valueOf(tgt.get("ref"));
            String ev = String.valueOf(e.get("evaluator"));
            byRun.computeIfAbsent(ref, k -> new HashMap<>()).put(ev, e);
        }

        List<Map<String, Object>> runs = (List<Map<String, Object>>) decisionRecord.getOrDefault("model_runs", Collections.emptyList());
        List<String> runIds = runs.stream().map(r -> String.valueOf(r.get("run_id"))).collect(Collectors.toList());

        List<String> candidates = new ArrayList<>();
        for (String runId : runIds) {
            Map<String, Map<String, Object>> evMap = byRun.getOrDefault(runId, Collections.emptyMap());
            boolean ok = true;
            for (String gate : REQUIRED_GATE) {
                Map<String, Object> r = evMap.get(gate);
                if (r == null || !Boolean.TRUE.equals(r.get("passed"))) {
                    ok = false;
                    break;
                }
            }
            if (ok) candidates.add(runId);
        }
        return candidates;
    }

    @SuppressWarnings("unchecked")
    public double quality(Map<String, Object> decisionRecord, String runId) {
        List<Map<String, Object>> evals = (List<Map<String, Object>>) decisionRecord.getOrDefault("eval_results", Collections.emptyList());
        double c = 0, s = 0, p = 0;
        for (Map<String, Object> e : evals) {
            Map<String, Object> tgt = (Map<String, Object>) e.get("target");
            if (tgt == null) continue;
            if (!"model_run".equals(String.valueOf(tgt.get("type")))) continue;
            if (!runId.equals(String.valueOf(tgt.get("ref")))) continue;

            String name = String.valueOf(e.get("evaluator"));
            double score = toDouble(e.get("score"));
            if ("constraint".equals(name)) c = score;
            if ("consistency".equals(name)) s = score;
            if ("completeness".equals(name)) p = score;
        }
        return 0.50 * p + 0.30 * s + 0.20 * c;
    }

    private double toDouble(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).doubleValue();
        try { return Double.parseDouble(String.valueOf(v)); } catch (Exception ignore) { return 0; }
    }
}
