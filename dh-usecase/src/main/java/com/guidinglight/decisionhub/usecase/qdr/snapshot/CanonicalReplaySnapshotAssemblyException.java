package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import java.util.Objects;

/**
 * Canonical snapshot assembly/hash/persistence 的结构化 fail-closed 异常。
 *
 * <p>异常只暴露固定 code 与脱敏 message，不携带 source payload、raw prompt、provider response 或凭证。
 * 调用方不得把该异常降级为 in-memory success，也不得在失败后继续写入 snapshot。
 */
public final class CanonicalReplaySnapshotAssemblyException extends RuntimeException {

  private final Code code;

  /** 使用固定 code 与安全 message 创建异常。 */
  public CanonicalReplaySnapshotAssemblyException(final Code code, final String message) {
    super(requireMessage(message));
    this.code = Objects.requireNonNull(code, "code");
  }

  /** 使用固定 code、脱敏 message 与内部 cause 创建异常。 */
  public CanonicalReplaySnapshotAssemblyException(
      final Code code, final String message, final Throwable cause) {
    super(requireMessage(message), Objects.requireNonNull(cause, "cause"));
    this.code = Objects.requireNonNull(code, "code");
  }

  /** 返回稳定失败 code，供测试、审计和上层 fail-closed 路由使用。 */
  public Code code() {
    return code;
  }

  private static String requireMessage(final String value) {
    final String checked = Objects.requireNonNull(value, "message").trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException("message must not be blank");
    }
    return checked;
  }

  /** P3 允许暴露的结构化失败分类。 */
  public enum Code {
    LEGACY_NOT_REPLAYABLE,
    REQUIRED_SOURCE_MISSING,
    VERSION_VECTOR_INVALID,
    IDENTITY_MISMATCH,
    TENANT_MISMATCH,
    UNSAFE_INPUT,
    CANONICALIZATION_FAILED,
    HASH_FAILED,
    SOURCE_CHANGED,
    TRANSACTION_MANAGER_REQUIRED,
    PERSISTENCE_MISMATCH
  }
}
