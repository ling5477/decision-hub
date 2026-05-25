package com.guidinglight.decisionhub.memory.agent.inmemory;

import com.guidinglight.decisionhub.domain.experience.ExperienceEntry;
import com.guidinglight.decisionhub.memory.agent.ExperienceStore;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Stage1：ExperienceStore 的内存实现。
 *
 * <p>线程安全，复杂度 O(N) 检索；仅用于 Stage1 / 测试。
 */
public final class InMemoryExperienceStore implements ExperienceStore {

  private final Map<String, ExperienceEntry> indexByKey = new ConcurrentHashMap<>();

  @Override
  public Optional<ExperienceEntry> findByKey(final String tenantId, final String experienceKey) {
    return Optional.ofNullable(indexByKey.get(compositeKey(tenantId, experienceKey)));
  }

  @Override
  public void save(final ExperienceEntry entry) {
    indexByKey.put(compositeKey(entry.getTenantId(), entry.getExperienceKey()), entry);
  }

  @Override
  public List<ExperienceEntry> topByPattern(
      final String tenantId, final String strategyPattern, final int limit) {
    return indexByKey.values().stream()
        .filter(e -> tenantId.equals(e.getTenantId()))
        .filter(e -> strategyPattern.equals(e.getStrategyPattern()))
        .sorted(Comparator.comparingDouble(ExperienceEntry::getScore).reversed())
        .limit(limit)
        .collect(Collectors.toList());
  }

  @Override
  public List<ExperienceEntry> listAll(final String tenantId) {
    return indexByKey.values().stream()
        .filter(e -> tenantId.equals(e.getTenantId()))
        .collect(Collectors.toList());
  }

  private static String compositeKey(final String tenantId, final String experienceKey) {
    return tenantId + "::" + experienceKey;
  }
}
