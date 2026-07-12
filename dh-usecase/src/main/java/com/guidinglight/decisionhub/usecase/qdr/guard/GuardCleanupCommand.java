package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Duration;
import java.util.Objects;

/**
 * Bounded cleanup命令；只允许完整environment/endpoint/source域内的indexed batch。
 *
 * @param environment 部署环境。
 * @param endpoint canonical endpoint。
 * @param source canonical source。
 * @param safetyGrace 数据库当前时间之前必须额外跨过的安全窗口。
 * @param batchSize 单事务最大影响行数。
 */
public record GuardCleanupCommand(
    String environment, String endpoint, String source, Duration safetyGrace, int batchSize) {

  /** 校验domain与batch hard ceiling。 */
  public GuardCleanupCommand {
    new PersistentGuardIdentity(environment, endpoint, source, "validation-tenant");
    safetyGrace = Objects.requireNonNull(safetyGrace, "safetyGrace");
    if (safetyGrace.isZero()
        || safetyGrace.isNegative()
        || safetyGrace.compareTo(Duration.ofDays(1)) > 0) {
      throw new IllegalArgumentException("safetyGrace must be within (0, 1 day]");
    }
    if (batchSize <= 0 || batchSize > 1_000) {
      throw new IllegalArgumentException("batchSize must be within 1..1000");
    }
  }
}
