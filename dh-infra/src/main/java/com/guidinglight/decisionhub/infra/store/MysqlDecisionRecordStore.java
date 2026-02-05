package com.guidinglight.decisionhub.infra.store;

import com.guidinglight.decisionhub.usecase.contract.DecisionRecordStore;

import java.util.Map;
import java.util.Optional;

/**
 * v1 示例：MySQL/JSON 落盘 Store（骨架）。
 *
 * 你需要在 dh-infra 中按你现有的 DAO/Mapper 体系实现：
 * - upsert: INSERT ... ON DUPLICATE KEY UPDATE
 * - get: SELECT record_json FROM decision_record WHERE decision_id=?
 */
public class MysqlDecisionRecordStore implements DecisionRecordStore {

    @Override
    public void upsert(String decisionId, Map<String, Object> recordJson) {
        // TODO: wire to your mapper/repository
        throw new UnsupportedOperationException("Implement via your persistence layer");
    }

    @Override
    public Optional<Map<String, Object>> get(String decisionId) {
        // TODO: wire to your mapper/repository
        return Optional.empty();
    }
}
