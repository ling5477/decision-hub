package com.guidinglight.decisionhub.security.nq;

import java.util.Objects;

/**
 * NQ feedback header 校验器（DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3）。
 *
 * <p>定位：canonical header 的 binding 一致性闸门，本批正式接入 controller。对 canonical
 * {@code X-NQ-DH-Tenant-Id / Request-Id / Trace-Id} 做与权威来源的一致性校验。<b>权威来源不变</b>
 * （tenant=认证上下文，requestId/traceId=body / request model），<b>header 绝不覆盖权威来源</b>。
 *
 * <p>语义（fail-closed）：上述三个 header 为可选；<b>若提供（非空）则必须等于对应权威来源</b>，任一不一致即
 * invalid，auditCode = {@link NqDhHeaderValidationResult#AUDIT_HEADER_BINDING_MISMATCH}。header 缺省则跳过
 * （由权威来源提供值）。缺 canonical source/timestamp/nonce/signature 仍由
 * {@link HmacNqFeedbackAuthenticator} fail-closed（不在本类职责内；{@code AUDIT_MISSING_CANONICAL_HEADER}
 * 保留为预留码，当前缺失由 authenticator 既有错误码覆盖）。
 *
 * <p>本类无状态、不发起 IO、不持有凭证、不记录 header 原值 / 签名 / 密钥；{@code reason} 仅含字段类型说明，
 * 不含 header / 权威来源的具体值。
 */
public final class NqDhHeaderValidator {

  /**
   * 校验 canonical Tenant/Request/Trace header 与权威来源的 binding 一致性（Batch 3）。
   *
   * @param headers 归一化 header 模型；不可为 null
   * @param authoritativeTenant 权威 tenant（认证上下文）
   * @param authoritativeRequestId 权威 requestId（body / request model）
   * @param authoritativeTraceId 权威 traceId（body / request model）
   * @return 一致或 header 缺省时 {@link NqDhHeaderValidationResult#ok()}；任一不一致时 invalid /
   *     {@link NqDhHeaderValidationResult#AUDIT_HEADER_BINDING_MISMATCH}
   */
  public NqDhHeaderValidationResult validate(
      final NormalizedNqDhHeaders headers,
      final String authoritativeTenant,
      final String authoritativeRequestId,
      final String authoritativeTraceId) {
    Objects.requireNonNull(headers, "headers");
    // header 可选：若提供则必须与权威来源相等；绝不以 header 覆盖权威来源。reason 仅含字段名，不含具体值。
    if (mismatch(headers.tenantId(), authoritativeTenant)) {
      return NqDhHeaderValidationResult.invalid(
          "tenant header does not match authenticated tenant",
          NqDhHeaderValidationResult.AUDIT_HEADER_BINDING_MISMATCH);
    }
    if (mismatch(headers.requestId(), authoritativeRequestId)) {
      return NqDhHeaderValidationResult.invalid(
          "requestId header does not match request body",
          NqDhHeaderValidationResult.AUDIT_HEADER_BINDING_MISMATCH);
    }
    if (mismatch(headers.traceId(), authoritativeTraceId)) {
      return NqDhHeaderValidationResult.invalid(
          "traceId header does not match request body",
          NqDhHeaderValidationResult.AUDIT_HEADER_BINDING_MISMATCH);
    }
    return NqDhHeaderValidationResult.ok();
  }

  /**
   * header 槽位为空 -> 跳过（header 可选）；非空且与权威来源不相等 -> mismatch。
   *
   * @param headerValue header 值（可为 null）
   * @param authoritative 权威来源值
   * @return 是否构成 binding mismatch
   */
  private static boolean mismatch(final String headerValue, final String authoritative) {
    return headerValue != null && !headerValue.isBlank() && !headerValue.equals(authoritative);
  }
}
