package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Integration-1 limited dry-run HMAC gate 回归测试。
 *
 * <p>覆盖 canonical UTC `Z` timestamp、±300s window、nonce replay、tenant/source pair allowlist、
 * payload cap 和 signature fail-closed。测试不发 HTTP、不调用 NQ、不接 provider。
 */
class HmacNqDryRunAuthenticatorTest {

  private static final Instant NOW = Instant.parse("2026-07-04T00:00:00Z");
  private static final String SECRET = "unit-test-dryrun-secret";
  private static final String BODY =
      "{\"requestId\":\"req-1\",\"traceId\":\"trace-1\",\"tenantId\":\"tenant-a\","
          + "\"source\":\"NQ_DRYRUN\",\"timestamp\":\"2026-07-04T00:00:00Z\","
          + "\"nonce\":\"nonce-1\",\"schemaVersion\":\"1.0.0\",\"dryRun\":true}";

  @Test
  void validRequestPassesAndReplayIsRejected() {
    final HmacNqDryRunAuthenticator authenticator = authenticator();
    final NqDryRunAuthRequest request = signed("nonce-1", NOW.toString(), BODY);

    assertTrue(authenticator.authenticate(request).allowed());

    final NqDryRunAuthResult replay = authenticator.authenticate(request);

    assertEquals("NONCE_REPLAY", replay.errorCode());
    assertEquals(409, replay.status());
  }

  @Test
  void invalidSignatureIsRejected() {
    final NqDryRunAuthRequest request =
        new NqDryRunAuthRequest(
            "POST",
            "/api/ai/decision-dry-runs",
            "NQ_DRYRUN",
            "NQ_DRYRUN",
            "tenant-a",
            "tenant-a",
            NOW.toString(),
            "nonce-bad",
            "bad",
            "req-1",
            "trace-1",
            "1.0.0",
            BODY,
            BODY.length(),
            NOW);

    final NqDryRunAuthResult result = authenticator().authenticate(request);

    assertEquals("SIGNATURE_INVALID", result.errorCode());
    assertEquals(401, result.status());
  }

  @Test
  void epochSecondsEpochMillisAndNonUtcOffsetAreRejectedAsTimestampInvalid() {
    for (String timestamp : Set.of("1783123200", "1783123200000", "2026-07-04T08:00:00+08:00")) {
      final NqDryRunAuthResult result =
          authenticator().authenticate(signed("nonce-" + timestamp.length(), timestamp, BODY));

      assertEquals("TIMESTAMP_INVALID", result.errorCode());
      assertEquals(401, result.status());
    }
  }

  @Test
  void timestampOutsideWindowIsRejected() {
    final NqDryRunAuthResult result =
        authenticator()
            .authenticate(signed("nonce-window", NOW.minus(Duration.ofMinutes(6)).toString(), BODY));

    assertEquals("TIMESTAMP_OUT_OF_WINDOW", result.errorCode());
    assertEquals(401, result.status());
  }

  @Test
  void sourceAndTenantPairMustBeAllowlisted() {
    final NqDryRunAuthResult sourceDenied =
        authenticator().authenticate(signedWithSource("nexus-quant", "tenant-a", "nonce-src"));
    final NqDryRunAuthResult tenantDenied =
        authenticator().authenticate(signedWithSource("NQ_DRYRUN", "tenant-b", "nonce-tenant"));

    assertEquals("SOURCE_DENIED", sourceDenied.errorCode());
    assertEquals("TENANT_MISMATCH", tenantDenied.errorCode());
  }

  @Test
  void payloadTooLargeIsRejectedBeforeSignatureWork() {
    final String largeBody = "x".repeat(256);
    final NqDryRunAuthResult result =
        new HmacNqDryRunAuthenticator(
                Set.of("NQ_DRYRUN"),
                Set.of("tenant-a:NQ_DRYRUN"),
                SECRET,
                Duration.ofMinutes(5),
                32,
                new InMemoryNonceReplayGuard(100, Duration.ofMinutes(10), fixedClock()))
            .authenticate(signed("nonce-large", NOW.toString(), largeBody));

    assertEquals("PAYLOAD_TOO_LARGE", result.errorCode());
    assertEquals(413, result.status());
  }

  @Test
  void nqUppercaseSourceSignatureMaterialMatchesWireValueAndPasses() {
    final NqDryRunAuthRequest unsigned =
        new NqDryRunAuthRequest(
            "POST",
            "/api/ai/decision-dry-runs",
            "NQ_DRYRUN",
            "NQ_DRYRUN",
            "tenant-a",
            "tenant-a",
            NOW.toString(),
            "nonce-nq-uppercase-source",
            "",
            "req-1",
            "trace-1",
            "1.0.0",
            BODY,
            BODY.length(),
            NOW);
    final String nqStyleMaterial =
        String.join(
            "\n",
            "POST",
            "/api/ai/decision-dry-runs",
            "NQ_DRYRUN",
            "tenant-a",
            "req-1",
            "trace-1",
            NOW.toString(),
            "nonce-nq-uppercase-source",
            "1.0.0",
            sha256Hex(BODY));
    final NqDryRunAuthRequest nqSigned =
        new NqDryRunAuthRequest(
            unsigned.method(),
            unsigned.path(),
            unsigned.sourceHeader(),
            unsigned.sourceSystem(),
            unsigned.authenticatedTenantId(),
            unsigned.tenantId(),
            unsigned.timestampHeader(),
            unsigned.nonce(),
            HmacNqDryRunAuthenticator.hmacSha256Hex(SECRET, nqStyleMaterial),
            unsigned.requestId(),
            unsigned.traceId(),
            unsigned.schemaVersion(),
            unsigned.rawBody(),
            unsigned.contentLength(),
            unsigned.now());

    final NqDryRunAuthResult result = authenticator().authenticate(nqSigned);

    assertEquals(nqStyleMaterial, HmacNqDryRunAuthenticator.signatureMaterial(unsigned));
    assertTrue(result.allowed());
  }

  @Test
  void lowercaseSourceAndAliasAreDeniedEvenWhenSignedWithWireValue() {
    final NqDryRunAuthResult lowercase =
        authenticator().authenticate(signedWithSource("nq_dryrun", "tenant-a", "nonce-lowercase-source"));
    final NqDryRunAuthResult alias =
        authenticator().authenticate(signedWithSource("NQ-DRYRUN", "tenant-a", "nonce-alias-source"));

    assertEquals("SOURCE_DENIED", lowercase.errorCode());
    assertEquals(403, lowercase.status());
    assertEquals("SOURCE_DENIED", alias.errorCode());
    assertEquals(403, alias.status());
  }

  @Test
  void whitespaceAndHeaderBodyCaseMismatchAreDeniedWithoutChangingWireValue() {
    final NqDryRunAuthResult whitespace =
        authenticator().authenticate(signedWithSource(" NQ_DRYRUN ", "tenant-a", "nonce-source-whitespace"));
    final NqDryRunAuthResult headerBodyMismatch =
        authenticator()
            .authenticate(
                signedWithHeaderAndBodySource(
                    "NQ_DRYRUN", "nq_dryrun", "tenant-a", "nonce-source-case-mismatch"));

    assertEquals("SOURCE_DENIED", whitespace.errorCode());
    assertEquals(403, whitespace.status());
    assertEquals("SOURCE_DENIED", headerBodyMismatch.errorCode());
    assertEquals(403, headerBodyMismatch.status());
  }

  @Test
  void sourceAndBodyMutationInvalidateAnExistingSignature() {
    final NqDryRunAuthRequest canonical =
        signed("nonce-source-mutation", NOW.toString(), BODY);
    final String mutatedBody = BODY.replace("NQ_DRYRUN", "nq_dryrun");
    final NqDryRunAuthRequest mutated =
        new NqDryRunAuthRequest(
            canonical.method(),
            canonical.path(),
            "nq_dryrun",
            "nq_dryrun",
            canonical.authenticatedTenantId(),
            canonical.tenantId(),
            canonical.timestampHeader(),
            canonical.nonce(),
            canonical.signature(),
            canonical.requestId(),
            canonical.traceId(),
            canonical.schemaVersion(),
            mutatedBody,
            mutatedBody.length(),
            canonical.now());

    final NqDryRunAuthResult result = authenticator().authenticate(mutated);

    assertEquals("SIGNATURE_INVALID", result.errorCode());
    assertEquals(401, result.status());
  }

  @Test
  void signatureMaterialMismatchStillReturnsSignatureInvalid() {
    final NqDryRunAuthRequest unsigned =
        new NqDryRunAuthRequest(
            "POST",
            "/api/ai/decision-dry-runs",
            "NQ_DRYRUN",
            "NQ_DRYRUN",
            "tenant-a",
            "tenant-a",
            NOW.toString(),
            "nonce-mismatched-material",
            "",
            "req-1",
            "trace-1",
            "1.0.0",
            BODY,
            BODY.length(),
            NOW);
    final String mismatchedMaterial =
        String.join(
            "\n",
            "POST",
            "/api/ai/decision-dry-runs",
            "nq_dryrun",
            "tenant-a",
            "req-1",
            "trace-1",
            NOW.toString(),
            "nonce-mismatched-material",
            "1.0.0",
            sha256Hex(BODY));
    final NqDryRunAuthRequest signedWithMismatchedMaterial =
        new NqDryRunAuthRequest(
            unsigned.method(),
            unsigned.path(),
            unsigned.sourceHeader(),
            unsigned.sourceSystem(),
            unsigned.authenticatedTenantId(),
            unsigned.tenantId(),
            unsigned.timestampHeader(),
            unsigned.nonce(),
            HmacNqDryRunAuthenticator.hmacSha256Hex(SECRET, mismatchedMaterial),
            unsigned.requestId(),
            unsigned.traceId(),
            unsigned.schemaVersion(),
            unsigned.rawBody(),
            unsigned.contentLength(),
            unsigned.now());

    final NqDryRunAuthResult result = authenticator().authenticate(signedWithMismatchedMaterial);

    assertEquals("SIGNATURE_INVALID", result.errorCode());
    assertEquals(401, result.status());
  }

  private static HmacNqDryRunAuthenticator authenticator() {
    return new HmacNqDryRunAuthenticator(
        Set.of("NQ_DRYRUN"),
        Set.of("tenant-a:NQ_DRYRUN"),
        SECRET,
        Duration.ofMinutes(5),
        2048,
        new InMemoryNonceReplayGuard(100, Duration.ofMinutes(10), fixedClock()));
  }

  private static NqDryRunAuthRequest signed(
      final String nonce, final String timestamp, final String body) {
    final NqDryRunAuthRequest unsigned =
        new NqDryRunAuthRequest(
            "POST",
            "/api/ai/decision-dry-runs",
            "NQ_DRYRUN",
            "NQ_DRYRUN",
            "tenant-a",
            "tenant-a",
            timestamp,
            nonce,
            "",
            "req-1",
            "trace-1",
            "1.0.0",
            body,
            body.getBytes(java.nio.charset.StandardCharsets.UTF_8).length,
            NOW);
    return withSignature(unsigned);
  }

  private static NqDryRunAuthRequest signedWithSource(
      final String source, final String tenantId, final String nonce) {
    final NqDryRunAuthRequest unsigned =
        new NqDryRunAuthRequest(
            "POST",
            "/api/ai/decision-dry-runs",
            source,
            source,
            "tenant-a",
            tenantId,
            NOW.toString(),
            nonce,
            "",
            "req-1",
            "trace-1",
            "1.0.0",
            BODY,
            BODY.length(),
            NOW);
    return withSignature(unsigned);
  }

  private static NqDryRunAuthRequest signedWithHeaderAndBodySource(
      final String sourceHeader, final String sourceBody, final String tenantId, final String nonce) {
    final NqDryRunAuthRequest unsigned =
        new NqDryRunAuthRequest(
            "POST",
            "/api/ai/decision-dry-runs",
            sourceHeader,
            sourceBody,
            "tenant-a",
            tenantId,
            NOW.toString(),
            nonce,
            "",
            "req-1",
            "trace-1",
            "1.0.0",
            BODY,
            BODY.length(),
            NOW);
    return withSignature(unsigned);
  }

  private static NqDryRunAuthRequest withSignature(final NqDryRunAuthRequest unsigned) {
    final String signature =
        HmacNqDryRunAuthenticator.hmacSha256Hex(
            SECRET, HmacNqDryRunAuthenticator.signatureMaterial(unsigned));
    return new NqDryRunAuthRequest(
        unsigned.method(),
        unsigned.path(),
        unsigned.sourceHeader(),
        unsigned.sourceSystem(),
        unsigned.authenticatedTenantId(),
        unsigned.tenantId(),
        unsigned.timestampHeader(),
        unsigned.nonce(),
        signature,
        unsigned.requestId(),
        unsigned.traceId(),
        unsigned.schemaVersion(),
        unsigned.rawBody(),
        unsigned.contentLength(),
        unsigned.now());
  }

  private static Clock fixedClock() {
    return Clock.fixed(NOW, ZoneOffset.UTC);
  }

  private static String sha256Hex(final String value) {
    try {
      final java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      final byte[] hash =
          digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      final StringBuilder sb = new StringBuilder(hash.length * 2);
      for (final byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (final java.security.NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 not available", error);
    }
  }
}
