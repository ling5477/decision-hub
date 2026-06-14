package com.guidinglight.decisionhub.security.nq;

import java.util.Objects;

/**
 * NQ feedback header 解析器（DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1，skeleton）。
 *
 * <p>Why：把 header 读取从 controller 抽出，产出 {@link NormalizedNqDhHeaders} 归一化模型。通过
 * {@link HeaderLookup} 函数式入参与 servlet 解耦，便于单测，且本类可置于 dh-security（不依赖 web 层）。
 *
 * <p>Batch 1 仅提供 {@link #parseLegacy(HeaderLookup)}：按 legacy {@code X-DH-NQ-*} 读取，等价封装现有读取方式，
 * <b>不改变对外行为</b>。canonical {@code X-NQ-DH-*} 读取在 Batch 2 引入；本批不做 legacy/canonical 双接收。
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
   * 按 legacy {@code X-DH-NQ-*} 族解析为归一化模型（Batch 1：保持现有行为）。
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
