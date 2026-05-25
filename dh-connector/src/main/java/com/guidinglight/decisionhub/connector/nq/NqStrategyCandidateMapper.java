package com.guidinglight.decisionhub.connector.nq;

import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import java.util.Map;

/**
 * Stage1：DH StrategyCandidate -> NQ 提交结构 的映射器。
 *
 * <p>对应工单 4.5：NqStrategyCandidateMapper。
 */
public interface NqStrategyCandidateMapper {

  /**
   * 将 DH 候选映射为 NQ 可接收的结构化请求体（不包含订单语义）。
   *
   * @param candidate 候选。
   * @return 结构化请求快照。
   */
  Map<String, Object> toNqRequest(StrategyCandidate candidate);
}
