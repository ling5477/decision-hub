package com.guidinglight.decisionhub.domain.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/** Verifies the immutable, explicit feedback tenant/environment authority contract. */
class FeedbackExecutionScopeTest {

  @Test
  void acceptsOnlyAnExplicitTenantAndCanonicalEnvironment() {
    final FeedbackExecutionScope scope = new FeedbackExecutionScope(" tenant-a ", FeedbackEnvironment.DEV);

    assertEquals("tenant-a", scope.tenantId());
    assertEquals(FeedbackEnvironment.DEV, scope.environment());
    assertEquals(FeedbackEnvironment.TEST, FeedbackEnvironment.fromWire("TEST"));
  }

  @Test
  void rejectsMissingOrInferredAuthorityWithoutDefaults() {
    assertThrows(
        FeedbackExecutionScopeException.class,
        () -> new FeedbackExecutionScope(" ", FeedbackEnvironment.DEV));
    assertThrows(
        NullPointerException.class,
        () -> new FeedbackExecutionScope("tenant-a", null));
    assertThrows(FeedbackExecutionScopeException.class, () -> FeedbackEnvironment.fromWire(null));
    assertThrows(FeedbackExecutionScopeException.class, () -> FeedbackEnvironment.fromWire("dev"));
    assertThrows(FeedbackExecutionScopeException.class, () -> FeedbackEnvironment.fromWire("LIVE"));
  }
}
