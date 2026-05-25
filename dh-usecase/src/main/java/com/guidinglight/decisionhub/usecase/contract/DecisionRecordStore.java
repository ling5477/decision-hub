package com.guidinglight.decisionhub.usecase.contract;

import java.util.Map;
import java.util.Optional;

/** @deprecated Stage1-CLOSE：旧 DecisionRecord 存储端口。 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public interface DecisionRecordStore {
    void upsert(String decisionId, Map<String, Object> recordJson);
    Optional<Map<String, Object>> get(String decisionId);
}
