package com.guidinglight.decisionhub.domain.qdr.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class FeedbackExecutionScopeTest {

  @Test
  void acceptsOnlyExplicitCanonicalWireEnvironments() {
    assertEquals(FeedbackEnvironment.DEV, FeedbackEnvironment.fromWire("DEV"));
    assertEquals(FeedbackEnvironment.TEST, FeedbackEnvironment.fromWire("TEST"));

    for (String rejected :
        new String[] {"", " ", "UNKNOWN", "LOCAL", "PAPER", "PROD", "PRODUCTION", "LIVE", "dev"}) {
      assertThrows(
          FeedbackExecutionScopeException.class,
          () -> FeedbackEnvironment.fromWire(rejected));
    }
    assertThrows(FeedbackExecutionScopeException.class, () -> FeedbackEnvironment.fromWire(null));
  }

  @Test
  void guardConfigurationMapsOnlyExplicitDevAndTestWithoutDefault() {
    assertEquals(FeedbackEnvironment.DEV, FeedbackEnvironment.fromGuardConfiguration("dev"));
    assertEquals(FeedbackEnvironment.TEST, FeedbackEnvironment.fromGuardConfiguration("test"));
    assertNull(FeedbackEnvironment.fromGuardConfiguration(null));
    assertNull(FeedbackEnvironment.fromGuardConfiguration(""));
    assertNull(FeedbackEnvironment.fromGuardConfiguration("staging"));
    assertNull(FeedbackEnvironment.fromGuardConfiguration("prod"));
  }

  @Test
  void scopeRequiresExactTenantAndNonNullEnvironment() {
    final FeedbackExecutionScope scope =
        new FeedbackExecutionScope("tenant-a", FeedbackEnvironment.DEV);

    assertEquals("tenant-a", scope.tenantId());
    assertEquals(FeedbackEnvironment.DEV, scope.environment());
    assertThrows(
        FeedbackExecutionScopeException.class,
        () -> new FeedbackExecutionScope(" ", FeedbackEnvironment.DEV));
    assertThrows(
        FeedbackExecutionScopeException.class,
        () -> new FeedbackExecutionScope(" tenant-a", FeedbackEnvironment.DEV));
    assertThrows(
        NullPointerException.class, () -> new FeedbackExecutionScope("tenant-a", null));
  }
}
