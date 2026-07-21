package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** 包内共享的有界输入与敏感内容保护规则。 */
final class FeedbackContractGuard {

  static final int MAX_ID_LENGTH = 128;
  static final int MAX_SAFE_TEXT_LENGTH = 256;
  static final int MAX_COLLECTION_SIZE = 32;
  static final BigDecimal MIN_CONTRIBUTION = BigDecimal.ONE.negate();
  static final BigDecimal MAX_CONTRIBUTION = BigDecimal.ONE;
  private static final Pattern SAFE_CODE = Pattern.compile("[A-Z][A-Z0-9_]{0,63}");
  private static final Pattern SHA_256 = Pattern.compile("[0-9a-f]{64}");
  private static final Pattern SENSITIVE_OR_EXECUTABLE =
      Pattern.compile(
          "(?i)(authorization|api[-_]?key|api[-_]?secret|password|passphrase|private[-_]?key|"
              + "mnemonic|cookie|credential|raw[-_]?prompt|raw[-_]?provider|"
              + "place[-_]?order|cancel[-_]?order|mutate[-_]?nq|trigger[-_]?live|buy|sell)");

  private FeedbackContractGuard() {}

  static String requireId(final String value, final String field) {
    return requireSafeText(value, field, MAX_ID_LENGTH);
  }

  static String requireSafeText(final String value, final String field, final int maxLength) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    if (checked.length() > maxLength) {
      throw new IllegalArgumentException(field + " exceeds max length " + maxLength);
    }
    if (SENSITIVE_OR_EXECUTABLE.matcher(checked).find()) {
      throw new IllegalArgumentException(field + " rejected by safe-reference guard");
    }
    return checked;
  }

  static String requireReasonCode(final String value, final String field) {
    final String checked = requireSafeText(value, field, 64).toUpperCase(Locale.ROOT);
    if (!SAFE_CODE.matcher(checked).matches()) {
      throw new IllegalArgumentException(field + " must be an uppercase stable code");
    }
    return checked;
  }

  static String requireHash(final String value, final String field) {
    final String checked = requireSafeText(value, field, 64).toLowerCase(Locale.ROOT);
    if (!SHA_256.matcher(checked).matches()) {
      throw new IllegalArgumentException(field + " must be lowercase SHA-256 hex");
    }
    return checked;
  }

  static BigDecimal requireUnitRange(final BigDecimal value, final String field) {
    final BigDecimal checked = Objects.requireNonNull(value, field).stripTrailingZeros();
    if (checked.compareTo(MIN_CONTRIBUTION) < 0
        || checked.compareTo(MAX_CONTRIBUTION) > 0) {
      throw new IllegalArgumentException(field + " must be within [-1,1]");
    }
    return checked;
  }
}
