package com.guidinglight.decisionhub.security.nq;

/**
 * 归一化的 NQ feedback header 模型（DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1）。
 *
 * <p>Why：把"从哪个 header 族读取"与"下游如何使用 header 值"解耦。controller / authenticator 只面向本模型，
 * 不再直接接触 header 名。Batch 1 仅由 legacy parser 填充；Batch 2 起 canonical parser 也产出本模型，
 * 后续冲突/一致性校验（{@link NqDhHeaderValidator}）也基于本模型，从而保证签名基于单一归一化值、杜绝混签。
 *
 * <p>字段为各逻辑 header 槽位的"按 header 解析得到的值"。注意：DH 入站当前只把
 * {@code source/timestamp/nonce/signature} 用于认证；{@code tenantId/requestId/traceId} 的权威来源是
 * 认证上下文 / body，header 槽位即使被填充也不得覆盖权威来源（Batch 2/3 仅做一致性校验）。
 *
 * <p>安全：{@link #toString()} 绝不输出 {@code signature} 原文，避免签名材料进日志（其余字段非机密）。
 *
 * @param source 来源系统标识（header 值）
 * @param tenantId 租户 header 值（可为 null；非权威）
 * @param requestId 幂等键 header 值（可为 null；非权威）
 * @param traceId 追踪键 header 值（可为 null；非权威）
 * @param timestamp 时间戳 header 值
 * @param nonce 一次性随机值
 * @param signature HMAC 签名（敏感，不入日志）
 * @param family 本次解析命中的 header 族
 */
public record NormalizedNqDhHeaders(
    String source,
    String tenantId,
    String requestId,
    String traceId,
    String timestamp,
    String nonce,
    String signature,
    HeaderFamily family) {

  /** 解析命中的 header 族。 */
  public enum HeaderFamily {
    /** canonical {@code X-NQ-DH-*}（Batch 2 起）。 */
    CANONICAL,
    /** legacy {@code X-DH-NQ-*}（Batch 1 现状）。 */
    LEGACY,
    /** 无可识别 header。 */
    NONE
  }

  /** 不输出 signature 原文（安全：避免签名材料进日志）。 */
  @Override
  public String toString() {
    return "NormalizedNqDhHeaders{source="
        + source
        + ", tenantId="
        + tenantId
        + ", requestId="
        + requestId
        + ", traceId="
        + traceId
        + ", timestamp="
        + timestamp
        + ", nonce="
        + nonce
        + ", signature="
        + (signature == null || signature.isBlank() ? "<absent>" : "[REDACTED]")
        + ", family="
        + family
        + "}";
  }
}
