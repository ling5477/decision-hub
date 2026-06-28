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
  void timestampOutsideAllowedWindow_isRejected() {
    final HmacNqFeedbackAuthenticator authenticator = authenticator();
    final NqFeedbackAuthRequest past =
        signedRequest("nonce-3a", "req-3a", NOW.minus(Duration.ofMinutes(10)), "nexus-quant");
    final NqFeedbackAuthRequest future =
        signedRequest("nonce-3b", "req-3b", NOW.plus(Duration.ofSeconds(301)), "nexus-quant");

    final NqFeedbackAuthResult pastResult = authenticator.authenticate(past);
    final NqFeedbackAuthResult futureResult = authenticator.authenticate(future);

    assertFalse(pastResult.allowed());
    assertEquals("TIMESTAMP_EXPIRED", pastResult.reason());
    assertFalse(futureResult.allowed());
    assertEquals("TIMESTAMP_EXPIRED", futureResult.reason());
  }

  @Test
  void epochTimestampFormats_areRejected() {
    final HmacNqFeedbackAuthenticator authenticator = authenticator();

    assertTimestampRejected(authenticator, Long.toString(NOW.getEpochSecond()), "nonce-7a", "req-7a");
    assertTimestampRejected(
        authenticator, Long.toString(NOW.toEpochMilli()), "nonce-7b", "req-7b");
  }

  @Test
  void timestampWithNumericOffset_isRejected() {
    final HmacNqFeedbackAuthenticator authenticator = authenticator();
    final NqFeedbackAuthRequest request =
        signedRequestWithTimestampHeader(
            "nonce-8", "req-8", "2026-05-26T18:00:00+08:00", NOW.toString(), "nexus-quant");

    final NqFeedbackAuthResult result = authenticator.authenticate(request);

    assertFalse(result.allowed(), "numeric timezone offset must fail closed");
    assertEquals(401, result.status());
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
    return signedRequestWithTimestampHeader(
        nonce, requestId, timestamp.toString(), timestamp.toString(), source);
  }

  private static void assertTimestampRejected(
      final HmacNqFeedbackAuthenticator authenticator,
      final String timestampHeader,
      final String nonce,
      final String requestId) {
    final NqFeedbackAuthResult result =
        authenticator.authenticate(
            signedRequestWithTimestampHeader(
                nonce, requestId, timestampHeader, timestampHeader, "nexus-quant"));
    assertFalse(result.allowed(), "timestamp must be rejected: " + timestampHeader);
    assertEquals(401, result.status());
    assertEquals("TIMESTAMP_EXPIRED", result.reason());
  }

  private static NqFeedbackAuthRequest signedRequestWithTimestampHeader(
      final String nonce,
      final String requestId,
      final String timestampHeader,
      final String signatureTimestamp,
      final String source) {
    final NqFeedbackAuthRequest unsigned =
        new NqFeedbackAuthRequest(
            source,
            source,
            signatureTimestamp,
            nonce,
            "",
            "evt-1",
            requestId,
            "trace-1",
            "{}",
            128,
            NOW);
    final String signature =
        HmacNqFeedbackAuthenticator.hmacSha256Hex(
            SECRET, HmacNqFeedbackAuthenticator.signatureMaterial(unsigned));
    return new NqFeedbackAuthRequest(
        source,
        source,
        timestampHeader,
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
