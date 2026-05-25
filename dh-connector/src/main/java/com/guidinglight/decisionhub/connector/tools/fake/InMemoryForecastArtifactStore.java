package com.guidinglight.decisionhub.connector.tools.fake;

import com.guidinglight.decisionhub.connector.tools.ForecastArtifactStore;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifact;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stage2-PoC-B5：ForecastArtifactStore 的内存实现。
 *
 * <p>主索引：{@code artifactId -> ForecastArtifact}。本实现进程重启即丢；
 * 生产部署应启用 JDBC 实现（{@code decisionhub.stage2.jdbc.enabled=true}）。
 */
public final class InMemoryForecastArtifactStore implements ForecastArtifactStore {

  private final Map<String, ForecastArtifact> byArtifactId = new ConcurrentHashMap<>();

  @Override
  public void save(final ForecastArtifact artifact) {
    Objects.requireNonNull(artifact, "artifact");
    byArtifactId.put(artifact.getArtifactId(), artifact);
  }

  @Override
  public Optional<ForecastArtifact> findById(final String artifactId) {
    if (artifactId == null) {
      return Optional.empty();
    }
    return Optional.ofNullable(byArtifactId.get(artifactId));
  }

  @Override
  public List<ForecastArtifact> findByTraceId(final String traceId) {
    if (traceId == null) {
      return List.of();
    }
    final List<ForecastArtifact> hits = new ArrayList<>();
    for (ForecastArtifact artifact : byArtifactId.values()) {
      if (traceId.equals(artifact.getTraceId())) {
        hits.add(artifact);
      }
    }
    hits.sort(Comparator.comparing(ForecastArtifact::getGeneratedAt));
    return List.copyOf(hits);
  }

  @Override
  public List<ForecastArtifact> findBySymbol(final String symbol) {
    if (symbol == null) {
      return List.of();
    }
    final List<ForecastArtifact> hits = new ArrayList<>();
    for (ForecastArtifact artifact : byArtifactId.values()) {
      if (symbol.equals(artifact.getSymbol())) {
        hits.add(artifact);
      }
    }
    hits.sort(Comparator.comparing(ForecastArtifact::getGeneratedAt));
    return List.copyOf(hits);
  }
}
