package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1：canonical / legacy header 名常量覆盖测试。
 *
 * <p>固化 canonical {@code X-NQ-DH-*} 与 legacy {@code X-DH-NQ-*} 字面量，防止后续重构误改命名。
 */
class NqDhHeaderNamesTest {

  @Test
  void canonical_header_names_match_frozen_contract() {
    assertEquals("X-NQ-DH-Source", NqDhHeaderNames.SOURCE);
    assertEquals("X-NQ-DH-Tenant-Id", NqDhHeaderNames.TENANT_ID);
    assertEquals("X-NQ-DH-Request-Id", NqDhHeaderNames.REQUEST_ID);
    assertEquals("X-NQ-DH-Trace-Id", NqDhHeaderNames.TRACE_ID);
    assertEquals("X-NQ-DH-Timestamp", NqDhHeaderNames.TIMESTAMP);
    assertEquals("X-NQ-DH-Nonce", NqDhHeaderNames.NONCE);
    assertEquals("X-NQ-DH-Signature", NqDhHeaderNames.SIGNATURE);
  }

  @Test
  void legacy_header_names_are_x_dh_nq_family() {
    assertEquals("X-DH-NQ-Source", NqDhHeaderNames.LEGACY_SOURCE);
    assertEquals("X-DH-NQ-Tenant-Id", NqDhHeaderNames.LEGACY_TENANT_ID);
    assertEquals("X-DH-NQ-Request-Id", NqDhHeaderNames.LEGACY_REQUEST_ID);
    assertEquals("X-DH-NQ-Trace-Id", NqDhHeaderNames.LEGACY_TRACE_ID);
    assertEquals("X-DH-NQ-Timestamp", NqDhHeaderNames.LEGACY_TIMESTAMP);
    assertEquals("X-DH-NQ-Nonce", NqDhHeaderNames.LEGACY_NONCE);
    assertEquals("X-DH-NQ-Signature", NqDhHeaderNames.LEGACY_SIGNATURE);
  }
}
