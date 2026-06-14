package com.guidinglight.decisionhub.security.nq;

import java.util.Objects;

/**
 * NQ feedback header 校验器（DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1，skeleton）。
 *
 * <p>定位：canonical 化校验的承载点，<b>本批为 skeleton，不做任何拒绝、不改变对外行为</b>——既有
 * {@link HmacNqFeedbackAuthenticator} 仍是唯一强制点。本批先固化契约与单测，供后续批次填充逻辑。
 *
 * <p>后续批次（不在本轮实现）：
 *
 * <ul>
 *   <li>Batch 2：canonical-only 必填校验（缺必需 canonical header -> invalid /
 *       {@link NqDhHeaderValidationResult#AUDIT_MISSING_CANONICAL_HEADER}）。
 *   <li>Batch 3：tenant / requestId / traceId 的 header 值与权威来源（auth-context / body）一致性校验，
 *       不一致 -> invalid / {@link NqDhHeaderValidationResult#AUDIT_HEADER_BINDING_MISMATCH}（fail-closed，
 *       绝不以 header 覆盖权威来源）。
 * </ul>
 *
 * <p>本类不发起 IO、不持有凭证、不记录 header 原值 / 签名 / 密钥。
 */
public final class NqDhHeaderValidator {

  /**
   * 校验归一化 header（Batch 1：pass-through，恒为 ok，不引入新拒绝）。
   *
   * @param headers 归一化 header 模型；不可为 null
   * @return Batch 1 恒为 {@link NqDhHeaderValidationResult#ok()}
   */
  public NqDhHeaderValidationResult validate(final NormalizedNqDhHeaders headers) {
    Objects.requireNonNull(headers, "headers");
    // Batch 1 skeleton：不做任何强制，保持对外行为不变。Batch 2/3 在此填充 canonical-only 与 binding 校验。
    return NqDhHeaderValidationResult.ok();
  }
}
