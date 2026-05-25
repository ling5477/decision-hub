package com.guidinglight.decisionhub.domain.feedback;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Stage2-PoC-B1：信封字段必填、不混淆 traceId/requestId/correlationId/sourceJobId、payloadJson 不丢失。 */
class NqFeedbackEnvelopeTest {

  private static final String RAW =
      "{\"paperRunId\":\"p-1\",\"candidateId\":\"c-1\",\"strategyName\":\"S1\","
          + "\"requestedBy\":\"alice\",\"createdAt\":\"2026-05-25T08:00:00Z\",\"rawPayloadJson\":\"{}\"}";

  @Test
  void of_keepsAllRequiredFieldsAndPreservesRawPayloadJson() {
    final Instant occurred = Instant.parse("2026-05-25T08:00:00Z");
    final NqFeedbackEnvelope env =
        NqFeedbackEnvelope.of(
            "e-1",
            NqFeedbackEventType.PAPER_RUN_CREATED,
            occurred,
            NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
            "job-1",
            "trace-1",
            "req-1",
            "corr-1",
            NqFeedbackEnvelope.DEFAULT_SCHEMA_VERSION,
            RAW,
            null);

    assertEquals("e-1", env.getEventId());
    assertEquals(NqFeedbackEventType.PAPER_RUN_CREATED, env.getEventType());
    assertEquals(occurred, env.getOccurredAt());
    assertEquals("nexus-quant", env.getSourceSystem());
    assertEquals("job-1", env.getSourceJobId());
    assertEquals("trace-1", env.getTraceId());
    assertEquals("req-1", env.getRequestId());
    assertEquals("corr-1", env.getCorrelationId());
    assertEquals("1.0.0", env.getSchemaVersion());
    assertEquals(RAW, env.getPayloadJson());
    assertNull(env.getReceivedAt());
  }

  /** traceId / requestId / correlationId / sourceJobId 必须互不混淆，且各自可独立取出。 */
  @Test
  void of_keepsTraceRequestCorrelationSourceJobDistinct() {
    final NqFeedbackEnvelope env =
        NqFeedbackEnvelope.of(
            "e-2",
            NqFeedbackEventType.BACKTEST_RESULT_READY,
            Instant.now(),
            NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
            "src-job",
            "trc",
            "req",
            "corr",
            "1.0.0",
            "{}",
            null);
    assertNotNull(env.getSourceJobId());
    assertEquals("src-job", env.getSourceJobId());
    assertEquals("trc", env.getTraceId());
    assertEquals("req", env.getRequestId());
    assertEquals("corr", env.getCorrelationId());
  }

  @Test
  void of_rejectsNullRequiredFields() {
    assertThrows(
        NullPointerException.class,
        () ->
            NqFeedbackEnvelope.of(
                null,
                NqFeedbackEventType.PAPER_RUN_CREATED,
                Instant.now(),
                NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
                "j",
                "t",
                "r",
                "c",
                "1.0.0",
                "{}",
                null));
    assertThrows(
        NullPointerException.class,
        () ->
            NqFeedbackEnvelope.of(
                "e",
                null,
                Instant.now(),
                NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
                "j",
                "t",
                "r",
                "c",
                "1.0.0",
                "{}",
                null));
    assertThrows(
        NullPointerException.class,
        () ->
            NqFeedbackEnvelope.of(
                "e",
                NqFeedbackEventType.PAPER_RUN_CREATED,
                Instant.now(),
                NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
                "j",
                "t",
                "r",
                "c",
                "1.0.0",
                null,
                null));
  }

  @Test
  void eventType_isFullyDistinctAndCovers8Types() {
    assertEquals(8, NqFeedbackEventType.values().length);
  }
}
