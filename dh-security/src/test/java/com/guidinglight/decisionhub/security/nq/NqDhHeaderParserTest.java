package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1：parser / 归一化模型测试。
 *
 * <p>覆盖：legacy 读取产出归一化模型；canonical-only 请求在 Batch 1 不被 legacy parser 读取（保持现状）；
 * 模型 toString 不泄露 signature 原文。全部 test-only，使用假 header 值，不读 .env / secret / token。
 */
class NqDhHeaderParserTest {

  private static final String FAKE_SIGNATURE = "deadbeefcafe-fake-signature";

  private final NqDhHeaderParser parser = new NqDhHeaderParser();

  private static NqDhHeaderParser.HeaderLookup lookup(final Map<String, String> headers) {
    return headers::get;
  }

  @Test
  void parse_legacy_reads_x_dh_nq_headers_into_model() {
    final Map<String, String> h = new HashMap<>();
    h.put("X-DH-NQ-Source", "nexus-quant");
    h.put("X-DH-NQ-Timestamp", "2026-06-14T00:00:00Z");
    h.put("X-DH-NQ-Nonce", "nonce-1");
    h.put("X-DH-NQ-Signature", FAKE_SIGNATURE);

    final NormalizedNqDhHeaders model = parser.parseLegacy(lookup(h));

    assertEquals("nexus-quant", model.source());
    assertEquals("2026-06-14T00:00:00Z", model.timestamp());
    assertEquals("nonce-1", model.nonce());
    assertEquals(FAKE_SIGNATURE, model.signature());
    assertEquals(NormalizedNqDhHeaders.HeaderFamily.LEGACY, model.family());
  }

  @Test
  void parse_legacy_does_not_read_canonical_headers_in_batch1() {
    // 仅提供 canonical X-NQ-DH-*；Batch 1 legacy parser 不应读到（canonical 读取留待 Batch 2）。
    final Map<String, String> h = new HashMap<>();
    h.put("X-NQ-DH-Source", "nexus-quant");
    h.put("X-NQ-DH-Timestamp", "2026-06-14T00:00:00Z");
    h.put("X-NQ-DH-Nonce", "nonce-1");
    h.put("X-NQ-DH-Signature", FAKE_SIGNATURE);

    final NormalizedNqDhHeaders model = parser.parseLegacy(lookup(h));

    assertNull(model.source());
    assertNull(model.timestamp());
    assertNull(model.nonce());
    assertNull(model.signature());
  }

  @Test
  void model_toString_does_not_leak_signature() {
    final Map<String, String> h = new HashMap<>();
    h.put("X-DH-NQ-Source", "nexus-quant");
    h.put("X-DH-NQ-Signature", FAKE_SIGNATURE);
    final NormalizedNqDhHeaders model = parser.parseLegacy(lookup(h));

    final String rendered = model.toString();
    assertFalse(rendered.contains(FAKE_SIGNATURE), "toString must not contain raw signature");
    assertTrue(rendered.contains("[REDACTED]"), "signature must be redacted in toString");
  }
}
