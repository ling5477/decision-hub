package com.guidinglight.decisionhub.usecase.gate.evaluator;

import java.util.*;

/**
 * v1：轻量工具，不依赖你现有 util，避免循环依赖。
 * 如果你更喜欢复用 dh-common 的 OutputResolver，可把这里替换掉。
 *
 * @deprecated Stage1-CLOSE：旧 Gate 评估器内部工具。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public final class OutputResolverV1 {
    private OutputResolverV1() {}

    @SuppressWarnings("unchecked")
    public static String resolveModelRunText(Map<String, Object> decisionRecord, String runId) {
        Object runsObj = decisionRecord.get("model_runs");
        if (runsObj instanceof List) {
            for (Map<String, Object> run : (List<Map<String, Object>>) runsObj) {
                if (runId.equals(String.valueOf(run.get("run_id")))) {
                    Object rawObj = run.get("raw_output");
                    if (rawObj instanceof Map) {
                        Object text = ((Map<String, Object>) rawObj).get("text");
                        return text == null ? "" : String.valueOf(text);
                    }
                }
            }
        }
        return "";
    }

    @SuppressWarnings("unchecked")
    public static List<Map<String, Object>> resolveToolTraces(Map<String, Object> decisionRecord, String runId) {
        Object runsObj = decisionRecord.get("model_runs");
        if (runsObj instanceof List) {
            for (Map<String, Object> run : (List<Map<String, Object>>) runsObj) {
                if (runId.equals(String.valueOf(run.get("run_id")))) {
                    Object traces = run.get("tool_traces");
                    if (traces instanceof List) return (List<Map<String, Object>>) traces;
                }
            }
        }
        return List.of();
    }

    public static boolean traceContains(List<Map<String, Object>> traces, String key) {
        for (Map<String, Object> t : traces) {
            for (Object v : t.values()) {
                if (v != null && String.valueOf(v).toLowerCase().contains(key.toLowerCase())) return true;
            }
        }
        return false;
    }

    public static double cjkRatio(String s) {
        if (s == null || s.isEmpty()) return 0.0;
        int cjk = 0;
        int total = s.length();
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ((ch >= 0x4E00 && ch <= 0x9FFF) || (ch >= 0x3400 && ch <= 0x4DBF)) {
                cjk++;
            }
        }
        return total == 0 ? 0.0 : (double) cjk / (double) total;
    }
}
