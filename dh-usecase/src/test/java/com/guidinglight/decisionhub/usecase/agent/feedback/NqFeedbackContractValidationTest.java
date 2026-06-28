package com.guidinglight.decisionhub.usecase.agent.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.ResearchRunRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.impl.DefaultNqFeedbackContractValidator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Stage2-PoC-B2：契约校验测试。
 *
 * <p>覆盖 docs/current/STAGE2_POC_TEST_PLAN.md §2.2 全部 6 类用例：
 *
 * <ol>
 *   <li>8 种 eventType 各 1 条合法事件 -> ok。
 *   <li>未知 eventType -> UNKNOWN_EVENT_TYPE。
 *   <li>缺少 traceId（空串）-> INVALID_SCHEMA。
 *   <li>traceId 未命中 ResearchRun -> UNKNOWN_TRACE。
 *   <li>schemaVersion < 1.0.0 -> INVALID_SCHEMA。
 *   <li>payload 缺少 eventType 要求的必填字段 -> INVALID_SCHEMA。
 * </ol>
 */
class NqFeedbackContractValidationTest {

  private static final String TRACE = "trace-1";

  private NqFeedbackContractValidator newValidator() {
    final ResearchRunRepository repo = B2TestFixtures.repoWithRun(TRACE);
    return new DefaultNqFeedbackContractValidator(repo, new ObjectMapper());
  }

  @Test
  void all8EventTypes_legalCommandPasses() {
    final NqFeedbackContractValidator v = newValidator();
    for (NqFeedbackEventType t : NqFeedbackEventType.values()) {
      final ValidationResult r = v.validate(B2TestFixtures.legalCommand("evt-" + t, t, TRACE));
      assertTrue(r.isValid(), "expect ok for " + t + " got " + r.getErrorCode() + " "
          + r.getMessage());
      assertNotNull(r.getEnvelope());
      assertEquals(t, r.getEnvelope().getEventType());
    }
  }

  @Test
  void unknownEventType_returnsUnknownEventType() {
    final NqFeedbackContractValidator v = newValidator();
    final IngestionCommand base =
        B2TestFixtures.legalCommand("e1", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);
    final IngestionCommand mutated = B2TestFixtures.commandWith(base, "rawEventType", "FOOBAR");
    final ValidationResult r = v.validate(mutated);
    assertFalse(r.isValid());
    assertEquals(IngestionErrorCode.UNKNOWN_EVENT_TYPE, r.getErrorCode());
  }

  @Test
  void blankTraceId_returnsInvalidSchema() {
    final NqFeedbackContractValidator v = newValidator();
    final IngestionCommand base =
        B2TestFixtures.legalCommand("e2", NqFeedbackEventType.PAPER_RUN_STARTED, TRACE);
    final IngestionCommand mutated = B2TestFixtures.commandWith(base, "traceId", "   ");
    final ValidationResult r = v.validate(mutated);
    assertFalse(r.isValid());
    assertEquals(IngestionErrorCode.INVALID_SCHEMA, r.getErrorCode());
  }

  @Test
  void unknownTraceId_returnsUnknownTrace() {
    final NqFeedbackContractValidator v = newValidator();
    final IngestionCommand base =
        B2TestFixtures.legalCommand("e3", NqFeedbackEventType.PAPER_RUN_STOPPED, TRACE);
    final IngestionCommand mutated =
        B2TestFixtures.commandWith(base, "traceId", "unknown-trace-xyz");
    final ValidationResult r = v.validate(mutated);
    assertFalse(r.isValid());
    assertEquals(IngestionErrorCode.UNKNOWN_TRACE, r.getErrorCode());
  }

  @Test
  void schemaVersionBelow1_0_0_returnsInvalidSchema() {
    final NqFeedbackContractValidator v = newValidator();
    final IngestionCommand base =
        B2TestFixtures.legalCommand("e4", NqFeedbackEventType.PAPER_RUN_ALERT_RAISED, TRACE);
    final IngestionCommand mutated = B2TestFixtures.commandWith(base, "schemaVersion", "0.9.0");
    final ValidationResult r = v.validate(mutated);
    assertFalse(r.isValid());
    assertEquals(IngestionErrorCode.INVALID_SCHEMA, r.getErrorCode());
  }

  @Test
  void payloadMissingRequiredField_returnsInvalidSchema() {
    final NqFeedbackContractValidator v = newValidator();
    final IngestionCommand base =
        B2TestFixtures.legalCommand("e5", NqFeedbackEventType.BACKTEST_RESULT_READY, TRACE);
    // 删除 backtestId 字段 -> 必填字段缺失
    final String trimmedPayload =
        base.getPayloadJson().replaceAll("\"backtestId\"\\s*:\\s*\"[^\"]*\"\\s*,?", "");
    final IngestionCommand mutated = B2TestFixtures.commandWith(base, "payloadJson", trimmedPayload);
    final ValidationResult r = v.validate(mutated);
    assertFalse(r.isValid());
    assertEquals(IngestionErrorCode.INVALID_SCHEMA, r.getErrorCode());
  }

  @Test
  void sourceSystemMustBeNexusQuant() {
    final NqFeedbackContractValidator v = newValidator();
    final IngestionCommand base =
        B2TestFixtures.legalCommand("e6", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);
    final IngestionCommand mutated = B2TestFixtures.commandWith(base, "sourceSystem", "other-sys");
    final ValidationResult r = v.validate(mutated);
    assertFalse(r.isValid());
    assertEquals(IngestionErrorCode.INVALID_SCHEMA, r.getErrorCode());
  }

  @Test
  void nestedForbiddenField_returnsForbiddenField() {
    final NqFeedbackContractValidator v = newValidator();
    final IngestionCommand base =
        B2TestFixtures.legalCommand("e-forbidden-field", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);
    final Map<String, Object> payload = legalPaperRunCreatedPayload();
    payload.put("rawPayloadJson", B2TestFixtures.toJson(Map.of("nested", Map.of("apiSecret", "redacted"))));

    final ValidationResult r =
        v.validate(B2TestFixtures.commandWith(base, "payloadJson", B2TestFixtures.toJson(payload)));

    assertFalse(r.isValid());
    assertEquals(IngestionErrorCode.FORBIDDEN_FIELD, r.getErrorCode());
  }

  @Test
  void nestedForbiddenCapability_returnsForbiddenCapability() {
    final NqFeedbackContractValidator v = newValidator();
    final IngestionCommand base =
        B2TestFixtures.legalCommand(
            "e-forbidden-capability", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE);
    final Map<String, Object> payload = legalPaperRunCreatedPayload();
    payload.put("capabilityPlan", Map.of("nextAction", "placeOrder"));

    final ValidationResult r =
        v.validate(B2TestFixtures.commandWith(base, "payloadJson", B2TestFixtures.toJson(payload)));

    assertFalse(r.isValid());
    assertEquals(IngestionErrorCode.FORBIDDEN_CAPABILITY, r.getErrorCode());
  }

  @Test
  void validPayloadStillAcceptedAfterForbiddenContractHardening() {
    final NqFeedbackContractValidator v = newValidator();

    final ValidationResult r =
        v.validate(
            B2TestFixtures.legalCommand(
                "e-valid-after-hardening", NqFeedbackEventType.PAPER_RUN_CREATED, TRACE));

    assertTrue(r.isValid());
    assertNotNull(r.getEnvelope());
  }

  /** 构造 PAPER_RUN_CREATED 的合法 payload，再由单测只改一个风险字段。 */
  private static Map<String, Object> legalPaperRunCreatedPayload() {
    final Map<String, Object> payload = new LinkedHashMap<>();
    payload.put("paperRunId", "pr-1");
    payload.put("candidateId", "cand-1");
    payload.put("strategyName", "S1");
    payload.put("requestedBy", "alice");
    payload.put("createdAt", "2026-05-25T08:00:00Z");
    payload.put("rawPayloadJson", "{\"src\":\"fake\"}");
    return payload;
  }
}
