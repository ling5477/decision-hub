package com.guidinglight.decisionhub.security.nq;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1：validator skeleton 契约测试。
 *
 * <p>Batch 1 validator 为 pass-through（恒 ok，不引入新拒绝），保证对外行为不变；canonical-only 与 binding
 * 一致性校验在 Batch 2/3 实现。本测试固化"Batch 1 不拒绝"契约，避免后续误把强制逻辑提前混入。
 */
class NqDhHeaderValidatorTest {

  private final NqDhHeaderValidator validator = new NqDhHeaderValidator();

  @Test
  void batch1_validator_is_pass_through_ok() {
    final NormalizedNqDhHeaders model =
        new NormalizedNqDhHeaders(
            "nexus-quant",
            null,
            null,
            null,
            "2026-06-14T00:00:00Z",
            "nonce-1",
            "fake-sig",
            NormalizedNqDhHeaders.HeaderFamily.LEGACY);

    final NqDhHeaderValidationResult result = validator.validate(model);

    assertTrue(result.valid());
    assertEquals("OK", result.reason());
    assertNull(result.auditCode());
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
