package com.guidinglight.decisionhub.usecase.run.support;

import java.util.List;
import java.util.Map;

/** @deprecated Stage1-CLOSE：旧多模型平台 DecisionRecord 字段解析器。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public final class OutputResolver {
    private OutputResolver() {}

    @SuppressWarnings("unchecked")
    public static String resolveText(Map<String, Object> decisionRecord, String targetType, String targetRef) {
        if ("model_run".equals(targetType)) {
            Object runsObj = decisionRecord.get("model_runs");
            if (runsObj instanceof List) {
                for (Map<String, Object> run : (List<Map<String, Object>>) runsObj) {
                    if (targetRef.equals(String.valueOf(run.get("run_id")))) {
                        Object rawObj = run.get("raw_output");
                        if (rawObj instanceof Map) {
                            Object text = ((Map<String, Object>) rawObj).get("text");
                            return text == null ? "" : String.valueOf(text);
                        }
                        return "";
                    }
                }
            }
        }
        if ("final_decision".equals(targetType)) {
            Object fdObj = decisionRecord.get("final_decision");
            if (fdObj instanceof Map) {
                Object outObj = ((Map<String, Object>) fdObj).get("output");
                if (outObj instanceof Map) {
                    Object text = ((Map<String, Object>) outObj).get("text");
                    return text == null ? "" : String.valueOf(text);
                }
            }
        }
        return "";
    }
}
