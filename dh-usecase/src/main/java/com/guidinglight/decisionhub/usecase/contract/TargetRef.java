package com.guidinglight.decisionhub.usecase.contract;

/** @deprecated Stage1-CLOSE：旧多模型平台 TargetRef。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class TargetRef {
    private String type; // model_run | final_decision | aggregate
    private String ref;

    public TargetRef() {}
    public TargetRef(String type, String ref) { this.type = type; this.ref = ref; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getRef() { return ref; }
    public void setRef(String ref) { this.ref = ref; }
}
