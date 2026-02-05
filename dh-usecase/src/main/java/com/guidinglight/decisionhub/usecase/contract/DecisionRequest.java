package com.guidinglight.decisionhub.usecase.contract;

import java.util.Map;

public class DecisionRequest {
    private Map<String, Object> decisionRecord;

    public DecisionRequest() {}
    public DecisionRequest(Map<String, Object> decisionRecord) { this.decisionRecord = decisionRecord; }

    public Map<String, Object> getDecisionRecord() { return decisionRecord; }
    public void setDecisionRecord(Map<String, Object> decisionRecord) { this.decisionRecord = decisionRecord; }
}
