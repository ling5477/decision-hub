package com.guidinglight.decisionhub.infra.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.common.util.JsonUtil;
import com.guidinglight.decisionhub.domain.run.Run;
import com.guidinglight.decisionhub.domain.run.RunStatus;
import com.guidinglight.decisionhub.infra.jpa.RunEntity;
import com.guidinglight.decisionhub.infra.jpa.RunJpaRepository;
import com.guidinglight.decisionhub.usecase.run.RunRepository;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

@Repository
public class JpaRunRepository implements RunRepository {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private final RunJpaRepository repo;

  public JpaRunRepository(RunJpaRepository repo) {
    this.repo = repo;
  }

  @Override
  public void save(Run run) {
    RunEntity e = new RunEntity();
    e.setRunId(run.getRunId());
    e.setTenantId(run.getTenantId());
    e.setStatus(run.getStatus().name());
    e.setQuestion(run.getQuestion());
    e.setConfigSnapshotJson(JsonUtil.toJson(run.getConfigSnapshot()));
    e.setCreatedAt(run.getCreatedAt());
    e.setUpdatedAt(run.getUpdatedAt());
    repo.save(e);
  }

  @Override
  public Optional<Run> findById(String runId) {
    return repo.findById(runId).map(e -> Run.rehydrate(
        e.getRunId(),
        e.getTenantId(),
        RunStatus.valueOf(e.getStatus()),
        e.getQuestion(),
        parseJsonMap(e.getConfigSnapshotJson()),
        e.getCreatedAt(),
        e.getUpdatedAt()
    ));
  }

  private static Map<String, Object> parseJsonMap(String json) {
    try {
      if (json == null || json.isBlank()) return Map.of();
      return MAPPER.readValue(json, new TypeReference<>() {});
    } catch (Exception ex) {
      return Map.of("raw", json);
    }
  }
}
