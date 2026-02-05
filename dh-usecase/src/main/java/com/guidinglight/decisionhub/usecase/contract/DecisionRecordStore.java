package com.guidinglight.decisionhub.usecase.contract;

import java.util.Map;
import java.util.Optional;

public interface DecisionRecordStore {
    void upsert(String decisionId, Map<String, Object> recordJson);
    Optional<Map<String, Object>> get(String decisionId);
}
