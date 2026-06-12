package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2 回归：确认 64KiB（65536 bytes）payload 上限闸门未被本批改动破坏。
 *
 * <p>本批只做 bounded memory cap，不得放松既有 payload 体积限制。size gate 在认证最前置（早于签名/重放校验），
 * 因此超限请求无需有效签名即应 413。
 */
class NqFeedbackPayloadSizeGateTest {

  private static final String SECRET = "unit-test-secret";
  private static final Instant NOW = Instant.parse("2026-06-12T10:00:00Z");
  private static final long MAX_64_KIB = 64L * 1024L;

  private static HmacNqFeedbackAuthenticator authenticator() {
    return new HmacNqFeedbackAuthenticator(
        Set.of("nexus-quant"), SECRET, Duration.ofMinutes(5), MAX_64_KIB, new InMemoryNonceReplayGuard());
  }

  private static NqFeedbackAuthRequest request(final String payloadJson, final int contentLength) {
    return new NqFeedbackAuthRequest(
        "nexus-quant",
        "nexus-quant",
        NOW.toString(),
        "nonce-size",
        "sig",
        "evt-1",
        "req-size",
        "trace-1",
        payloadJson,
        contentLength,
        NOW);
  }

  @Test
  void payload_64kib_gate_remains_valid() {
    // 超过 64KiB 一字节 -> 413 PAYLOAD_TOO_LARGE（size gate 前置，无需有效签名）。
    final String over = "a".repeat((int) MAX_64_KIB + 1);
    final NqFeedbackAuthResult result = authenticator().authenticate(request(over, over.length()));
    assertFalse(result.allowed());
    assertEquals(413, result.status());
    assertEquals("PAYLOAD_TOO_LARGE", result.reason());
  }

  @Test
  void payload_at_64kib_passes_size_gate() {
    // 恰好 64KiB 不触发 413；后续因签名错误返回 401，证明未被 size gate 拦截（限制未被收紧）。
    final String atLimit = "a".repeat((int) MAX_64_KIB);
    final NqFeedbackAuthResult result =
        authenticator().authenticate(request(atLimit, atLimit.length()));
    assertEquals(401, result.status());
  }
}
