package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** DH-AUDIT-FIX：NQ feedback HMAC 来源认证测试。 */
final class HmacNqFeedbackAuthenticatorTest {

  private static final String SECRET = "unit-test-secret";
  private static final Instant NOW = Instant.parse("2026-05-26T10:00:00Z");

  @Test
  void validSignature_isAllowedOnce() {
    final HmacNqFeedbackAuthenticator authenticator = authenticator();
    final NqFeedbackAuthRequest request = signedRequest("nonce-1", "req-1", NOW, "nexus-quant");

    final NqFeedbackAuthResult result = authenticator.authenticate(request);

    assertTrue(result.allowed());
  }

  @Test
  void wrongSignature_isRejected() {
    final HmacNqFeedbackAuthenticator authenticator = authenticator();
    final NqFeedbackAuthRequest request =
        new NqFeedbackAuthRequest(
            "nexus-quant",
            "nexus-quant",
            NOW.toString(),
            "nonce-2",
            "bad-signature",
            "evt-1",
            "req-2",
            "trace-1",
            "{}",
            128,
            NOW);

    final NqFeedbackAuthResult result = authenticator.authenticate(request);

    assertFalse(result.allowed());
    assertEquals(401, result.status());
  }

  @Test
  void expiredTimestamp_isRejected() {
    final HmacNqFeedbackAuthenticator authenticator = authenticator();
    final NqFeedbackAuthRequest request =
        signedRequest("nonce-3", "req-3", NOW.minus(Duration.ofMinutes(10)), "nexus-quant");

    final NqFeedbackAuthResult result = authenticator.authenticate(request);

    assertFalse(result.allowed());
    assertEquals("TIMESTAMP_EXPIRED", result.reason());
  }

  @Test
  void replayNonceAndRequestId_isRejected() {
    final HmacNqFeedbackAuthenticator authenticator = authenticator();
    final NqFeedbackAuthRequest request = signedRequest("nonce-4", "req-4", NOW, "nexus-quant");

    assertTrue(authenticator.authenticate(request).allowed());
    final NqFeedbackAuthResult replay = authenticator.authenticate(request);

    assertFalse(replay.allowed());
    assertEquals(409, replay.status());
  }

  @Test
  void sourceOutsideAllowlist_isRejected() {
    final HmacNqFeedbackAuthenticator authenticator = authenticator();
    final NqFeedbackAuthRequest request = signedRequest("nonce-5", "req-5", NOW, "unknown-source");

    final NqFeedbackAuthResult result = authenticator.authenticate(request);

    assertFalse(result.allowed());
    assertEquals(403, result.status());
  }

  @Test
  void oversizedPayload_isRejected() {
    final HmacNqFeedbackAuthenticator authenticator =
        new HmacNqFeedbackAuthenticator(
            Set.of("nexus-quant"),
            SECRET,
            Duration.ofMinutes(5),
            4,
            new InMemoryNonceReplayGuard());

    final NqFeedbackAuthResult result =
        authenticator.authenticate(signedRequest("nonce-6", "req-6", NOW, "nexus-quant"));

    assertFalse(result.allowed());
    assertEquals(413, result.status());
  }

  private static HmacNqFeedbackAuthenticator authenticator() {
    return new HmacNqFeedbackAuthenticator(
        Set.of("nexus-quant"), SECRET, Duration.ofMinutes(5), 1024, new InMemoryNonceReplayGuard());
  }

  private static NqFeedbackAuthRequest signedRequest(
      final String nonce, final String requestId, final Instant timestamp, final String source) {
    final NqFeedbackAuthRequest unsigned =
        new NqFeedbackAuthRequest(
            source, source, timestamp.toString(), nonce, "", "evt-1", requestId, "trace-1", "{}", 128, NOW);
    final String signature =
        HmacNqFeedbackAuthenticator.hmacSha256Hex(
            SECRET, HmacNqFeedbackAuthenticator.signatureMaterial(unsigned));
    return new NqFeedbackAuthRequest(
        source,
        source,
        timestamp.toString(),
        nonce,
        signature,
        "evt-1",
        requestId,
        "trace-1",
        "{}",
        128,
        NOW);
  }
}
