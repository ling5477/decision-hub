package com.guidinglight.decisionhub.security.nq;

import java.util.Objects;

/**
 * NQ feedback header 解析器（DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2）。
 *
 * <p>Why：把 header 读取从 controller 抽出，产出 {@link NormalizedNqDhHeaders} 归一化模型。通过
 * {@link HeaderLookup} 函数式入参与 servlet 解耦，便于单测，且本类可置于 dh-security（不依赖 web 层）。
 *
 * <p>Batch 2 起生产入站为 <b>canonical-only</b>：controller 使用 {@link #parseCanonical(HeaderLookup)} 按
 * canonical {@code X-NQ-DH-*} 读取，<b>不再读取 legacy、也不做 legacy/canonical 双接收</b>。canonical-only 下仅有
 * legacy header 的请求等同缺失 canonical（source/timestamp/nonce/signature 为 null），由下游 authenticator
 * fail-closed 拒绝。{@link #parseLegacy(HeaderLookup)} 仅保留为历史引用 / 单测，不再被生产 controller 调用。
 *
 * <p>本类无状态、不发起 IO、不持有凭证、不记录任何 header 值（解析结果由调用方决定如何使用）。
 */
public final class NqDhHeaderParser {

  /** header 取值抽象：name -> value（缺失返回 null）。便于以 {@code httpRequest::getHeader} 适配。 */
  @FunctionalInterface
  public interface HeaderLookup {
    /**
     * 取指定 header 值。
     *
     * @param name header 名
     * @return header 值；缺失返回 null
     */
    String header(String name);
  }

  /**
   * 按 canonical {@code X-NQ-DH-*} 族解析为归一化模型（Batch 2：canonical-only 读取）。
   *
   * <p>读取 canonical 7 header（Source / Tenant-Id / Request-Id / Trace-Id / Timestamp / Nonce /
   * Signature）。其中 Tenant-Id / Request-Id / Trace-Id 仅进入模型，<b>不得覆盖权威来源</b>（tenant=认证上下文，
   * requestId/traceId=body）；其 binding 一致性强制校验留待 Batch 3（{@link NqDhHeaderValidator}）。缺失的
   * header 槽位为 null，由下游 authenticator fail-closed 拒绝（保持现有 401/403 缺失语义）。
   *
   * @param lookup header 取值函数；不可为 null
   * @return 归一化 header 模型，{@code family=CANONICAL}
   */
  public NormalizedNqDhHeaders parseCanonical(final HeaderLookup lookup) {
    Objects.requireNonNull(lookup, "lookup");
    return new NormalizedNqDhHeaders(
        lookup.header(NqDhHeaderNames.SOURCE),
        lookup.header(NqDhHeaderNames.TENANT_ID),
        lookup.header(NqDhHeaderNames.REQUEST_ID),
        lookup.header(NqDhHeaderNames.TRACE_ID),
        lookup.header(NqDhHeaderNames.TIMESTAMP),
        lookup.header(NqDhHeaderNames.NONCE),
        lookup.header(NqDhHeaderNames.SIGNATURE),
        NormalizedNqDhHeaders.HeaderFamily.CANONICAL);
  }

  /**
   * 按 legacy {@code X-DH-NQ-*} 族解析为归一化模型。
   *
   * <p><b>canonical-only（Batch 2+）下不再被生产 controller 调用</b>，仅保留为历史引用 / 单测；生产入站请用
   * {@link #parseCanonical(HeaderLookup)}。
   *
   * @param lookup header 取值函数；不可为 null
   * @return 归一化 header 模型，{@code family=LEGACY}
   */
  public NormalizedNqDhHeaders parseLegacy(final HeaderLookup lookup) {
    Objects.requireNonNull(lookup, "lookup");
    return new NormalizedNqDhHeaders(
        lookup.header(NqDhHeaderNames.LEGACY_SOURCE),
        lookup.header(NqDhHeaderNames.LEGACY_TENANT_ID),
        lookup.header(NqDhHeaderNames.LEGACY_REQUEST_ID),
        lookup.header(NqDhHeaderNames.LEGACY_TRACE_ID),
        lookup.header(NqDhHeaderNames.LEGACY_TIMESTAMP),
        lookup.header(NqDhHeaderNames.LEGACY_NONCE),
        lookup.header(NqDhHeaderNames.LEGACY_SIGNATURE),
        NormalizedNqDhHeaders.HeaderFamily.LEGACY);
  }
}
