package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Instant;
import java.util.Objects;

/**
 * Bounded cleanup命令；只允许完整environment/endpoint/source域内的indexed batch。
 *
 * @param environment 部署环境。
 * @param endpoint canonical endpoint。
 * @param source canonical source。
 * @param cutoffExclusive DB候选必须早于该安全cutoff。
 * @param batchSize 单事务最大影响行数。
 */
public record GuardCleanupCommand(
    String environment, String endpoint, String source, Instant cutoffExclusive, int batchSize) {

  /** 校验domain与batch hard ceiling。 */
  public GuardCleanupCommand {
    new PersistentGuardIdentity(environment, endpoint, source, "validation-tenant");
    cutoffExclusive = Objects.requireNonNull(cutoffExclusive, "cutoffExclusive");
    if (batchSize <= 0 || batchSize > 1_000) {
      throw new IllegalArgumentException("batchSize must be within 1..1000");
    }
  }
}
