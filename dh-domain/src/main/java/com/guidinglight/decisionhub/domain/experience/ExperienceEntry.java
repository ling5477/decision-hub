package com.guidinglight.decisionhub.domain.experience;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Map;

/**
 * Stage1：经验沉淀条目。
 *
 * <p>对应工单 4.1：ExperienceEntry。蚁群机制的载体：strategyPattern + marketRegime + dataSource + agentRole
 * 形成经验 key，NQ 回流结果调整 score。
 */
public final class ExperienceEntry {

  private final String entryId;
  private final String tenantId;
  private final String traceId;
  /** 业务复合 key：用于检索经验。 */
  private final String experienceKey;
  private final String strategyPattern;
  private final String marketRegime;
  private final String dataSource;
  private final String agentRole;
  /** 经验得分（蚁群信息素强度的轻量映射）。 */
  private double score;
  private long successCount;
  private long failureCount;
  private final Map<String, Object> payloadJson;
  private final Instant createdAt;
  private Instant updatedAt;

  private ExperienceEntry(
      final String entryId,
      final String tenantId,
      final String traceId,
      final String experienceKey,
      final String strategyPattern,
      final String marketRegime,
      final String dataSource,
      final String agentRole,
      final double score,
      final long successCount,
      final long failureCount,
      final Map<String, Object> payloadJson,
      final Instant createdAt,
      final Instant updatedAt) {
    this.entryId = entryId;
    this.tenantId = tenantId;
    this.traceId = traceId;
    this.experienceKey = experienceKey;
    this.strategyPattern = strategyPattern;
    this.marketRegime = marketRegime;
    this.dataSource = dataSource;
    this.agentRole = agentRole;
    this.score = score;
    this.successCount = successCount;
    this.failureCount = failureCount;
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  /** 工厂：新建条目。 */
  public static ExperienceEntry create(
      final String tenantId,
      final String traceId,
      final String experienceKey,
      final String strategyPattern,
      final String marketRegime,
      final String dataSource,
      final String agentRole,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new ExperienceEntry(
        IdGenerator.newId(),
        tenantId,
        traceId,
        experienceKey,
        strategyPattern,
        marketRegime,
        dataSource,
        agentRole,
        0.0,
        0L,
        0L,
        payloadJson,
        now,
        now);
  }

  /**
   * 用一次正反馈累加得分。
   *
   * @param delta 加分幅度，必须为正数。
   */
  public void reinforce(final double delta, final Instant now) {
    this.score += delta;
    this.successCount++;
    this.updatedAt = now;
  }

  /**
   * 用一次负反馈衰减得分。
   *
   * @param penalty 衰减幅度，必须为正数；内部按减号生效。
   */
  public void penalize(final double penalty, final Instant now) {
    this.score -= penalty;
    this.failureCount++;
    this.updatedAt = now;
  }

  public String getEntryId() {
    return entryId;
  }

  public String getTenantId() {
    return tenantId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getExperienceKey() {
    return experienceKey;
  }

  public String getStrategyPattern() {
    return strategyPattern;
  }

  public String getMarketRegime() {
    return marketRegime;
  }

  public String getDataSource() {
    return dataSource;
  }

  public String getAgentRole() {
    return agentRole;
  }

  public double getScore() {
    return score;
  }

  public long getSuccessCount() {
    return successCount;
  }

  public long getFailureCount() {
    return failureCount;
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
