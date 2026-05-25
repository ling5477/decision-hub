package com.guidinglight.decisionhub.usecase.agent.impl;

import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.candidate.StrategyCandidate;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import com.guidinglight.decisionhub.usecase.agent.CandidateGenerationService;
import com.guidinglight.decisionhub.usecase.agent.StrategyCandidateRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stage1：默认候选生成服务。
 *
 * <p>蜂群轻量实现：对同一 ResearchRun 生成固定数量（默认 3）的候选，每个候选标注独立的搜索路径并写入 payload。
 */
public final class DefaultCandidateGenerationService implements CandidateGenerationService {

  private static final int DEFAULT_CANDIDATE_COUNT = 3;

  private final StrategyCandidateRepository candidateRepository;

  /** 构造。 */
  public DefaultCandidateGenerationService(final StrategyCandidateRepository candidateRepository) {
    this.candidateRepository = candidateRepository;
  }

  @Override
  public List<StrategyCandidate> generate(final ResearchRun run) {
    final List<StrategyCandidate> out = new ArrayList<>();
    for (int i = 0; i < DEFAULT_CANDIDATE_COUNT; i++) {
      final Map<String, Object> payload = new HashMap<>();
      payload.put("variant", i);
      payload.put("expectedSharpe", 0.5 + (i * 0.2));
      payload.put("maxDrawdown", 0.1 + (i * 0.05));
      payload.put("topic", run.getTopic());

      final List<String> evidenceRefs = List.of("ev-" + i + "-a", "ev-" + i + "-b");
      final StrategyCandidate candidate =
          StrategyCandidate.create(
              run.getRunId(),
              run.getTenantId(),
              run.getTraceId(),
              "strategy-agent#" + i,
              "scout#" + i + "->analyst#" + i + "->strategy#" + i,
              evidenceRefs,
              Map.copyOf(payload),
              TimeProvider.now());
      candidateRepository.save(candidate);
      out.add(candidate);
    }
    return List.copyOf(out);
  }
}
