package com.guidinglight.decisionhub.usecase.qdr.guard;

import java.util.Objects;

/**
 * Fixed-window admission命令。
 *
 * @param identity 完整tenant/source/environment/endpoint身份。
 * @param windowSeconds fixed window秒数；只作为受校验配置，不是application clock。
 * @param limitValue 单窗口最大accepted数量。
 */
public record RateLimitAdmissionCommand(
    PersistentGuardIdentity identity, int windowSeconds, int limitValue) {

  /** 校验正数和B1安全hard ceiling，防止溢出或无界配置。 */
  public RateLimitAdmissionCommand {
    identity = Objects.requireNonNull(identity, "identity");
    if (windowSeconds <= 0
        || windowSeconds > PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS) {
      throw new IllegalArgumentException(
          "windowSeconds must be within 1.."
              + PersistentGuardHardCeilings.MAX_RATE_WINDOW_SECONDS);
    }
    if (limitValue <= 0 || limitValue > PersistentGuardHardCeilings.MAX_RATE_QUOTA) {
      throw new IllegalArgumentException(
          "limitValue must be within 1.." + PersistentGuardHardCeilings.MAX_RATE_QUOTA);
    }
  }
}
