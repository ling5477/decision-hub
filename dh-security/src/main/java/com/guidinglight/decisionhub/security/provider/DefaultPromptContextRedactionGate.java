package com.guidinglight.decisionhub.security.provider;

import java.util.Locale;
import java.util.Objects;

/**
 * 最小 PromptContextRedactionGate 实现。
 *
 * <p>Why：本轮不接真实 provider，因此先建立默认拒绝策略。敏感上下文只能发往 OFFICIAL_API 或
 * SELF_HOSTED_GATEWAY；CONTROLLED_RELAY 必须在后续工单实现更强脱敏后再放行。
 */
public final class DefaultPromptContextRedactionGate implements PromptContextRedactionGate {

  @Override
  public String assertAllowed(final String context, final ProviderTrustDecision decision) {
    Objects.requireNonNull(decision, "decision");
    if (!decision.allowed()) {
      throw new SecurityException("provider not trusted: " + decision.reason());
    }
    if (containsSensitiveContext(context)
        && decision.trustLevel() != ProviderTrustLevel.OFFICIAL_API
        && decision.trustLevel() != ProviderTrustLevel.SELF_HOSTED_GATEWAY) {
      throw new SecurityException("sensitive context requires trusted provider");
    }
    return context == null ? "" : context;
  }

  private static boolean containsSensitiveContext(final String context) {
    if (context == null || context.isBlank()) {
      return false;
    }
    final String lower = context.toLowerCase(Locale.ROOT);
    return lower.contains("api_key")
        || lower.contains("apikey")
        || lower.contains("authorization:")
        || lower.contains("bearer ")
        || lower.contains("secret")
        || lower.contains("private key")
        || lower.contains("token=");
  }
}
