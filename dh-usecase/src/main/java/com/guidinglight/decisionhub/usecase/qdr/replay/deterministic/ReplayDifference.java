package com.guidinglight.decisionhub.usecase.qdr.replay.deterministic;

import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;
import java.util.Objects;

/** 只保存 path、taxonomy、hash/version/safe-ref 与脱敏说明的审计 difference。 */
public record ReplayDifference(
    ReplayDifferenceType type,
    String path,
    String expectedHashOrRef,
    String actualHashOrRef,
    String sanitizedMessage) {

  /** 校验 difference 不携带 raw payload，并保持字段可稳定排序。 */
  public ReplayDifference {
    type = Objects.requireNonNull(type, "type");
    path = ReplayPersistenceGuard.requireSafeText(path, "difference.path");
    expectedHashOrRef =
        ReplayPersistenceGuard.optionalSafeText(
            expectedHashOrRef, "difference.expectedHashOrRef");
    actualHashOrRef =
        ReplayPersistenceGuard.optionalSafeText(actualHashOrRef, "difference.actualHashOrRef");
    sanitizedMessage =
        ReplayPersistenceGuard.requireSafeText(sanitizedMessage, "difference.sanitizedMessage");
  }
}
