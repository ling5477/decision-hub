package com.guidinglight.decisionhub.memory.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stage1：市场情境记忆读写端口。
 *
 * <p>对应工单 4.3：MarketRegimeMemory。承载“当前是什么市况、典型特征是什么”的轻量记忆。
 */
public interface MarketRegimeMemory {

  /** 按 regimeKey 查询。 */
  Optional<Map<String, Object>> findRegime(String tenantId, String regimeKey);

  /** Upsert。 */
  void saveRegime(String tenantId, String regimeKey, Map<String, Object> payloadJson);

  /** 列出全部市况标签（Stage1 调试用）。 */
  List<String> listRegimes(String tenantId);
}
