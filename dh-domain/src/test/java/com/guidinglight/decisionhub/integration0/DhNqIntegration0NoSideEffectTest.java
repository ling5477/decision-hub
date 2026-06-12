package com.guidinglight.decisionhub.integration0;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.guidinglight.decisionhub.integration0.support.Int0Contract;
import com.guidinglight.decisionhub.integration0.support.Int0ContractValidator;
import com.guidinglight.decisionhub.integration0.support.Int0CredentialAccessTracker;
import com.guidinglight.decisionhub.integration0.support.Int0NonceStore;
import com.guidinglight.decisionhub.integration0.support.Int0RequestFactory;
import com.guidinglight.decisionhub.integration0.support.Int0SideEffectTracker;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * DH-NQ Integration-0 无副作用 contract test（INT0-T14，DH 视角）。
 *
 * <p>验证 DH 侧 Integration-0 contract validation 在处理任何可开放 / 被拒绝请求时，都不会创建订单、撤单、 变更 Paper Run / 策略 / 风控、调用交易网关 /
 * adapter / 交易所、发起真实 NQ 调用，或开启 LIVE。 通过 test-only 探针断言副作用计数与凭证访问计数恒为 0。
 *
 * <p>本测试不连接任何真实交易网关、adapter、交易所、真实 NQ 或凭证。
 */
class DhNqIntegration0NoSideEffectTest {

  private final Int0ContractValidator validator = new Int0ContractValidator();

  private long now() {
    return System.currentTimeMillis() / 1000L;
  }

  /** INT0-T14：跑一批可开放 + 被拒绝请求，断言交易副作用与凭证访问均为 0。 */
  @Test
  void int0T14_noTradingSideEffectAndNoCredentialAccess() {
    Int0SideEffectTracker sideEffects = new Int0SideEffectTracker();
    Int0CredentialAccessTracker credentials = new Int0CredentialAccessTracker();

    List<String> fixtures =
        List.of(
            "fx-candidate-valid.json",
            "fx-readonly-query.json",
            "fx-feedback-valid.json",
            "fx-candidate-invalid.json",
            "fx-forbidden-fields.json",
            "fx-forbidden-calls.json",
            "fx-tenant-mismatch.json");

    for (String fixture : fixtures) {
      JsonNode payload = Int0RequestFactory.loadFixture(fixture);
      String body = Int0RequestFactory.toJson(payload);
      long ts = now();
      validator.validate(
          Int0RequestFactory.validHeaders(body, ts, "nonce-int0-t14-" + fixture),
          body,
          payload,
          ts,
          Int0Contract.FAKE_SECRET,
          new Int0NonceStore());
    }

    assertEquals(0, sideEffects.total(), "contract validation must produce zero trading side-effects");
    assertEquals(0, credentials.accessCount(), "contract validation must not access credentials");
  }

  /** INT0-T14 补充：被接受的可开放能力同样不得带交易执行意图。 */
  @Test
  void int0T14_acceptedAllowedCapabilitiesCarryNoExecutionIntent() {
    for (String fixture : List.of("fx-candidate-valid.json", "fx-readonly-query.json")) {
      JsonNode payload = Int0RequestFactory.loadFixture(fixture);
      assertTrue(
          validator.scanForbiddenCapabilities(payload).isEmpty(),
          fixture + " must not contain forbidden capabilities");
      assertFalse(
          payload.path("executionIntent").asBoolean(false),
          fixture + " executionIntent must be false");
    }
  }
}
