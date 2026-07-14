package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.time.Duration;
import java.util.Objects;

/**
 * Bounded cleanup命令；只允许完整environment/endpoint/source/tenant域内的indexed batch。
 *
 * @param environment 部署环境。
 * @param endpoint canonical endpoint。
 * @param source canonical source。
 * @param tenantId 已认证tenant；不允许空值、空白或global通配符。
 * @param safetyGrace 数据库当前时间之前必须额外跨过的安全窗口。
 * @param batchSize 单事务最大影响行数。
 */
public record GuardCleanupCommand(
    String environment,
    String endpoint,
    String source,
    String tenantId,
    Duration safetyGrace,
    int batchSize) {

  /** 校验domain与batch hard ceiling。 */
  public GuardCleanupCommand {
    new PersistentGuardIdentity(environment, endpoint, source, tenantId);
    if ("*".equals(tenantId)) {
      throw new IllegalArgumentException("tenantId wildcard is forbidden for cleanup");
    }
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
