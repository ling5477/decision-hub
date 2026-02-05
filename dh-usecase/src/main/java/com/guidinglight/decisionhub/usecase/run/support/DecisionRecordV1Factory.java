package com.guidinglight.decisionhub.usecase.run.support;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.run.Run;

import java.time.Instant;
import java.util.*;

/**
 * v1：DecisionRecord 初始化工厂
 *
 * 冻结要点：decision_id = run_id（强烈建议），便于全链路追踪与回放。
 */
public final class DecisionRecordV1Factory {
    private DecisionRecordV1Factory() {}

    public static Map<String, Object> init(Run run) {
        Instant now = TimeProvider.now();

        Map<String, Object> record = new LinkedHashMap<>();
        record.put("schema_version", "1.0.0");
        record.put("decision_id", run.getRunId());
        record.put("attempt", 1);
        record.put("created_at", now.toString());
        record.put("status", "INIT");
        record.put("tags", List.of("decision-hub", "v1"));

        Map<String, Object> input = new LinkedHashMap<>();
        input.put("user_prompt", run.getQuestion());
        // constraints/environment 可从 configSnapshot 拆分；v1 先原样塞进 environment 便于复盘
        input.put("constraints", run.getConfigSnapshot().getOrDefault("constraints", Map.of()));
        Map<String, Object> env = new LinkedHashMap<>();
        env.put("config_snapshot", run.getConfigSnapshot());
        input.put("environment", env);
        input.put("context_refs", List.of());
        record.put("input_context", input);

        record.put("model_runs", new ArrayList<>());
        record.put("eval_results", new ArrayList<>());

        // final_decision 先占位，后续在策略阶段写回
        Map<String, Object> finalDecision = new LinkedHashMap<>();
        finalDecision.put("strategy", Map.of("type", "rule_based", "params", Map.of("note", "init_placeholder")));
        finalDecision.put("selected", Map.of("type", "model_run", "ref", ""));
        finalDecision.put("confidence", 0.0);
        finalDecision.put("output", Map.of("text", "", "format", "markdown"));
        finalDecision.put("warnings", new ArrayList<>());
        record.put("final_decision", finalDecision);

        record.put("audit", Map.of(
            "trace_id", "",
            "session_id", "",
            "user_id", ""
        ));

        return record;
    }
}
