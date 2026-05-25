package com.guidinglight.decisionhub.connector.nq.fake;

import com.guidinglight.decisionhub.connector.nq.NqStrategyCandidateMapper;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import java.util.HashMap;
import java.util.Map;

/**
 * Stage1：候选 -> NQ 请求的默认映射实现。
 *
 * <p>只承载“DH 建议”，不包含订单语义。
 */
public final class DefaultNqStrategyCandidateMapper implements NqStrategyCandidateMapper {

  @Override
  public Map<String, Object> toNqRequest(final StrategyCandidate candidate) {
    final Map<String, Object> req = new HashMap<>();
    req.put("traceId", candidate.getTraceId());
    req.put("runId", candidate.getRunId());
    req.put("candidateId", candidate.getCandidateId());
    req.put("requestType", "BACKTEST");
    req.put("candidatePayload", candidate.getPayloadJson());
    req.put("evidenceRefs", candidate.getEvidenceRefs());
    req.put("scoreSnapshot", candidate.getScoreSnapshot());
    return Map.copyOf(req);
  }
}
