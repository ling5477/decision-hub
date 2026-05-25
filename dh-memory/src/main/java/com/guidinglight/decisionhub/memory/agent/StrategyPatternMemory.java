package com.guidinglight.decisionhub.memory.agent;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Stage1：策略模式记忆读写端口。
 *
 * <p>对应工单 4.3：StrategyPatternMemory。承载“某种策略模式的典型形态/常用参数范围”等轻量记忆。
 */
public interface StrategyPatternMemory {

  /** 按 patternKey 查询。 */
  Optional<Map<String, Object>> findPattern(String tenantId, String patternKey);

  /** Upsert。 */
  void savePattern(String tenantId, String patternKey, Map<String, Object> payloadJson);

  /** 列出全部 pattern key。 */
  List<String> listPatterns(String tenantId);
}
