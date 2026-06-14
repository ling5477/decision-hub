package com.guidinglight.decisionhub.security.nq;

/**
 * NQ feedback header 校验结果（DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1，skeleton）。
 *
 * <p>独立于认证结果 {@link NqFeedbackAuthResult}：header 校验是认证前的结构校验闸门，供 Batch 2/3 在
 * {@link NqDhHeaderValidator} 中实现 canonical-only 必填与 binding 一致性校验。{@code reason} 只含类型，
 * 不含 header 原值 / 签名 / 密钥。
 *
 * @param valid 是否通过
 * @param reason 失败类型（仅类型，不含敏感值）
 * @param auditCode 审计码（失败时），用于可观测记录
 */
public record NqDhHeaderValidationResult(boolean valid, String reason, String auditCode) {

  /** 审计码：canonical header 值与权威来源（auth-context / body）不一致。 */
  public static final String AUDIT_HEADER_BINDING_MISMATCH = "HEADER_BINDING_MISMATCH";

  /** 审计码：缺少必需 canonical header。 */
  public static final String AUDIT_MISSING_CANONICAL_HEADER = "MISSING_CANONICAL_HEADER";

  /** 通过。 */
  public static NqDhHeaderValidationResult ok() {
    return new NqDhHeaderValidationResult(true, "OK", null);
  }

  /**
   * 不通过。
   *
   * @param reason 失败类型
   * @param auditCode 审计码
   * @return 失败结果
   */
  public static NqDhHeaderValidationResult invalid(final String reason, final String auditCode) {
    return new NqDhHeaderValidationResult(false, reason, auditCode);
  }
}
