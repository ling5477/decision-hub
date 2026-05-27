package com.guidinglight.decisionhub.security.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** DH-AUDIT-FIX：Provider trust gate 回归测试。 */
final class ProviderTrustPolicyTest {

  @Test
  void emptyBaseUrl_isRejectedAsUnknown() {
    final ProviderTrustPolicy policy = new DefaultProviderTrustPolicy(Map.of());

    final ProviderTrustDecision decision = policy.evaluate("future-provider", "");

    assertFalse(decision.allowed());
    assertEquals(ProviderTrustLevel.UNKNOWN, decision.trustLevel());
  }

  @Test
  void unknownBaseUrl_isRejected() {
    final ProviderTrustPolicy policy = new DefaultProviderTrustPolicy(Map.of());

    final ProviderTrustDecision decision =
        policy.evaluate("future-provider", "https://future.example.test/v1");

    assertFalse(decision.allowed());
    assertEquals("BASE_URL_NOT_ALLOWED", decision.reason());
  }

  @Test
  void untrustedRelayPattern_isRejectedEvenIfConfigured() {
    final ProviderTrustPolicy policy =
        new DefaultProviderTrustPolicy(
            Map.of("https://openrouter.example.test/v1", ProviderTrustLevel.CONTROLLED_RELAY));

    final ProviderTrustDecision decision =
        policy.evaluate("openai-compatible", "https://openrouter.example.test/v1");

    assertFalse(decision.allowed());
    assertEquals(ProviderTrustLevel.UNTRUSTED_RELAY, decision.trustLevel());
  }

  @Test
  void allowlistedOfficialAndSelfHosted_areAllowed() {
    final ProviderTrustPolicy policy =
        new DefaultProviderTrustPolicy(
            Map.of(
                "https://official.example.test/v1", ProviderTrustLevel.OFFICIAL_API,
                "https://dh-gateway.internal/v1", ProviderTrustLevel.SELF_HOSTED_GATEWAY));

    assertTrue(policy.evaluate("official", "https://official.example.test/v1").allowed());
    assertTrue(policy.evaluate("gateway", "https://dh-gateway.internal/v1").allowed());
  }

  @Test
  void sensitiveContextCannotBeSentToNonTrustedProvider() {
    final PromptContextRedactionGate gate = new DefaultPromptContextRedactionGate();
    final ProviderTrustDecision controlledRelay =
        ProviderTrustDecision.allow(
            "relay", "https://controlled.example.test/v1", ProviderTrustLevel.CONTROLLED_RELAY);

    assertThrows(
        SecurityException.class,
        () -> gate.assertAllowed("authorization: Bearer test-token", controlledRelay));
  }

  @Test
  void auditFieldsDoNotExposeRawBaseUrl() {
    final ProviderTrustDecision decision =
        ProviderTrustDecision.allow(
            "official", "https://official.example.test/v1", ProviderTrustLevel.OFFICIAL_API);

    final Map<String, Object> fields = decision.auditFields("trace-1");

    assertEquals("official", fields.get("provider"));
    assertEquals("OFFICIAL_API", fields.get("trustLevel"));
    assertEquals("trace-1", fields.get("traceId"));
    assertFalse(fields.containsValue("https://official.example.test/v1"));
  }
}
