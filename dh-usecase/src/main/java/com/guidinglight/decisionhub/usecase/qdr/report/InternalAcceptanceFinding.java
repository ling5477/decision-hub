package com.guidinglight.decisionhub.usecase.qdr.report;

import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;
import java.util.Comparator;
import java.util.Locale;
import java.util.Objects;

/**
 * B4 internal report 的统一、脱敏 finding。
 *
 * <p>该对象只保存固定 source/severity、code、safe ref 与脱敏说明。构造期拒绝 raw material、凭证和可执行交易动作，
 * 避免 source finding 在聚合时突破既有 redaction 边界。
 *
 * @param source finding 来源。
 * @param severity finding 严重级别。
 * @param code 稳定 finding code。
 * @param safeRef 可选 safe ref/hash/version。
 * @param sanitizedMessage 固定脱敏说明。
 */
public record InternalAcceptanceFinding(
    Source source, Severity severity, String code, String safeRef, String sanitizedMessage) {

  /** finding 的稳定排序规则。 */
  public static final Comparator<InternalAcceptanceFinding> STABLE_ORDER =
      Comparator.comparing(InternalAcceptanceFinding::source)
          .thenComparing(InternalAcceptanceFinding::severity)
          .thenComparing(InternalAcceptanceFinding::code)
          .thenComparing(value -> Objects.toString(value.safeRef(), ""))
          .thenComparing(InternalAcceptanceFinding::sanitizedMessage);

  /** 校验 finding 只携带结构化安全信息。 */
  public InternalAcceptanceFinding {
    source = Objects.requireNonNull(source, "source");
    severity = Objects.requireNonNull(severity, "severity");
    code = requireSafeText(code, "code");
    safeRef = optionalSafeText(safeRef, "safeRef");
    sanitizedMessage = requireSafeText(sanitizedMessage, "sanitizedMessage");
  }

  static String requireSafeText(final String value, final String field) {
    final String checked = QdrPersistenceSafety.requireSafeText(value, field);
    rejectForbiddenMaterial(checked, field);
    return checked;
  }

  static String optionalSafeText(final String value, final String field) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return requireSafeText(value, field);
  }

  private static void rejectForbiddenMaterial(final String value, final String field) {
    final String normalized =
        value
            .toLowerCase(Locale.ROOT)
            .replace(" ", "")
            .replace("_", "")
            .replace("-", "");
    if (normalized.contains("rawprompt")
        || normalized.contains("prompttext")
        || normalized.contains("rawproviderresponse")
        || normalized.contains("providerraw")
        || normalized.contains("credential")
        || normalized.contains("apikey")
        || normalized.contains("apisecret")
        || normalized.contains("privatekey")
        || normalized.contains("marketorder")
        || normalized.contains("placeorder")
        || normalized.contains("cancelorder")) {
      throw new IllegalArgumentException(field + " rejected by internal report safety boundary");
    }
  }

  /** Finding 来源分类。 */
  public enum Source {
    /** Evidence aggregate。 */
    EVIDENCE,
    /** Deterministic replay。 */
    REPLAY,
    /** QDR regression。 */
    REGRESSION,
    /** Provider readiness。 */
    PROVIDER_READINESS,
    /** Model gateway observability。 */
    OBSERVABILITY,
    /** 输入 identity 或 source execution 边界。 */
    INPUT_BOUNDARY
  }

  /** Internal finding 严重级别。 */
  public enum Severity {
    /** 信息性 finding。 */
    INFO,
    /** 非阻断风险。 */
    WARN,
    /** 验收拒绝或无效输入。 */
    ERROR,
    /** 缺失、失败或安全阻断。 */
    BLOCKER
  }
}
