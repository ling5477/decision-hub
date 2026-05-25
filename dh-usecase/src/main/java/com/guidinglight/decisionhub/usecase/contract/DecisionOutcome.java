package com.guidinglight.decisionhub.usecase.contract;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** @deprecated Stage1-CLOSE：旧多模型平台决策契约。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class DecisionOutcome {
    private TargetRef selected;
    private double confidence;
    private Map<String, Object> output = new HashMap<>();
    private List<String> warnings = new ArrayList<>();
    private Map<String, Object> debug = new HashMap<>(); // 用于复盘（例如 quality、票数、权重等）

    public TargetRef getSelected() { return selected; }
    public void setSelected(TargetRef selected) { this.selected = selected; }
    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
    public Map<String, Object> getOutput() { return output; }
    public void setOutput(Map<String, Object> output) { this.output = output; }
    public List<String> getWarnings() { return warnings; }
    public void setWarnings(List<String> warnings) { this.warnings = warnings; }
    public Map<String, Object> getDebug() { return debug; }
    public void setDebug(Map<String, Object> debug) { this.debug = debug; }
}
