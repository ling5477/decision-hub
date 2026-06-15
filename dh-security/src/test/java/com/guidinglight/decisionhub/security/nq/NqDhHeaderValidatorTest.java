package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3：validator binding 一致性校验测试。
 *
 * <p>canonical Tenant/Request/Trace header 可选；若提供则必须等于权威来源，否则 invalid /
 * HEADER_BINDING_MISMATCH。header 缺省时跳过（不强制提供）。reason 不含具体值（防泄露）。
 */
class NqDhHeaderValidatorTest {

  private final NqDhHeaderValidator validator = new NqDhHeaderValidator();

  private static NormalizedNqDhHeaders headers(
      final String tenantId, final String requestId, final String traceId) {
    return new NormalizedNqDhHeaders(
        "nexus-quant",
        tenantId,
        requestId,
        traceId,
        "2026-06-15T00:00:00Z",
        "nonce-1",
        "fake-sig",
        NormalizedNqDhHeaders.HeaderFamily.CANONICAL);
  }

  @Test
  void all_consistent_is_ok() {
    final NqDhHeaderValidationResult r =
        validator.validate(headers("tenant-a", "req-1", "trace-1"), "tenant-a", "req-1", "trace-1");
    assertTrue(r.valid());
    assertEquals("OK", r.reason());
    assertNull(r.auditCode());
  }

  @Test
  void absent_binding_headers_are_skipped() {
    // 三个 binding header 缺省（null）-> 跳过校验，由权威来源提供值（header 可选）。
    final NqDhHeaderValidationResult r =
        validator.validate(headers(null, null, null), "tenant-a", "req-1", "trace-1");
    assertTrue(r.valid());
    assertNull(r.auditCode());
  }

  @Test
  void tenant_mismatch_is_rejected_with_binding_audit() {
    final NqDhHeaderValidationResult r =
        validator.validate(
            headers("tenant-OTHER", "req-1", "trace-1"), "tenant-a", "req-1", "trace-1");
    assertFalse(r.valid());
    assertEquals("HEADER_BINDING_MISMATCH", r.auditCode());
    assertFalse(r.reason().contains("tenant-OTHER"), "reason must not leak header value");
    assertFalse(r.reason().contains("tenant-a"), "reason must not leak authoritative value");
  }

  @Test
  void requestId_mismatch_is_rejected_with_binding_audit() {
    final NqDhHeaderValidationResult r =
        validator.validate(
            headers("tenant-a", "req-WRONG", "trace-1"), "tenant-a", "req-1", "trace-1");
    assertFalse(r.valid());
    assertEquals("HEADER_BINDING_MISMATCH", r.auditCode());
  }

  @Test
  void traceId_mismatch_is_rejected_with_binding_audit() {
    final NqDhHeaderValidationResult r =
        validator.validate(
            headers("tenant-a", "req-1", "trace-WRONG"), "tenant-a", "req-1", "trace-1");
    assertFalse(r.valid());
    assertEquals("HEADER_BINDING_MISMATCH", r.auditCode());
  }

  @Test
  void validation_result_factories_carry_audit_codes() {
    final NqDhHeaderValidationResult mismatch =
        NqDhHeaderValidationResult.invalid(
            "binding mismatch", NqDhHeaderValidationResult.AUDIT_HEADER_BINDING_MISMATCH);
    assertEquals("HEADER_BINDING_MISMATCH", mismatch.auditCode());
    assertEquals(false, mismatch.valid());
  }
}
