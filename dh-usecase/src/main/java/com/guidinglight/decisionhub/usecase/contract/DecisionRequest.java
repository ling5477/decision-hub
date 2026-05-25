package com.guidinglight.decisionhub.usecase.contract;

import java.util.Map;

/** @deprecated Stage1-CLOSE：旧决策请求 DTO。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class DecisionRequest {
    private Map<String, Object> decisionRecord;

    public DecisionRequest() {}
    public DecisionRequest(Map<String, Object> decisionRecord) { this.decisionRecord = decisionRecord; }

    public Map<String, Object> getDecisionRecord() { return decisionRecord; }
    public void setDecisionRecord(Map<String, Object> decisionRecord) { this.decisionRecord = decisionRecord; }
}
