package com.guidinglight.decisionhub.memory.agent;

import com.guidinglight.decisionhub.domain.experience.PheromoneEdge;
import java.util.List;
import java.util.Optional;

/**
 * Stage1：信息素边的读写端口。
 *
 * <p>对应工单 4.3：PheromoneStore。
 */
public interface PheromoneStore {

  /** 按 from/to 节点查找已有边。 */
  Optional<PheromoneEdge> find(String tenantId, String fromNode, String toNode);

  /** Upsert。 */
  void save(PheromoneEdge edge);

  /** 列出某起点节点的所有出边（按 pheromoneScore 倒序）。 */
  List<PheromoneEdge> listByFrom(String tenantId, String fromNode);
}
