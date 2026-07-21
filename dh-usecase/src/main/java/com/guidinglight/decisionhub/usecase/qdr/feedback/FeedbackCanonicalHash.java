package com.guidinglight.decisionhub.usecase.qdr.feedback;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 完整 canonical input 与两个 domain-separated hash。
 *
 * @param canonicalValue 稳定、长度分隔的规范输入
 * @param value 全部归因输入的 SHA-256
 * @param idempotencyKey tenant/environment/decision/observation 冻结键的 SHA-256
 */
public record FeedbackCanonicalHash(String canonicalValue, String value, String idempotencyKey) {

  private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");

  /** 构造期拒绝空 canonical value 与非法 hash。 */
  public FeedbackCanonicalHash {
    canonicalValue = Objects.requireNonNull(canonicalValue, "canonicalValue");
    if (canonicalValue.isEmpty()) {
      throw new IllegalArgumentException("canonicalValue must not be empty");
    }
    value = requireHash(value, "value");
    idempotencyKey = requireHash(idempotencyKey, "idempotencyKey");
  }

  private static String requireHash(final String candidate, final String field) {
    final String checked = Objects.requireNonNull(candidate, field);
    if (!SHA_256.matcher(checked).matches()) {
      throw new IllegalArgumentException(field + " must be lowercase SHA-256 hex");
    }
    return checked;
  }
}
