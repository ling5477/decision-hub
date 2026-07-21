package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackAuditReference;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackPolicy;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackSubject;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeObservation;
import com.guidinglight.decisionhub.domain.qdr.feedback.OutcomeSource;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

final class FeedbackAttributionTestSupport {

  static final Instant EVALUATION_TIME = Instant.parse("2026-07-21T10:00:00Z");

  private FeedbackAttributionTestSupport() {}

  static FeedbackAttributionCommand validCommand() {
    return command(
        "tenant-a",
        FeedbackEnvironment.DEV,
        "decision-1",
        "trace-1",
        "observation-1",
        OutcomeSource.DRY_RUN_RESULT,
        ObservedDecisionOutcome.SUCCEEDED,
        Map.of(
            AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.8"),
            AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.6")),
        Map.of(
            AttributionDimension.EVIDENCE_QUALITY, "SAFE:evidence-1",
            AttributionDimension.RISK_DISCIPLINE, "SAFE:risk-1"));
  }

  static FeedbackAttributionCommand command(
      final String tenantId,
      final FeedbackEnvironment environment,
      final String decisionId,
      final String traceId,
      final String observationId,
      final OutcomeSource source,
      final ObservedDecisionOutcome outcome,
      final Map<AttributionDimension, BigDecimal> measurements,
      final Map<AttributionDimension, String> evidence) {
    final FeedbackSubject subject =
        new FeedbackSubject(tenantId, environment, decisionId, traceId);
    final OutcomeObservation observation =
        new OutcomeObservation(
            tenantId,
            environment,
            decisionId,
            traceId,
            observationId,
            source,
            outcome,
            EVALUATION_TIME.minusSeconds(60),
            measurements,
            evidence,
            Map.of("schema", "fixture-v1"));
    final FeedbackPolicy policy =
        new FeedbackPolicy(
            "policy-1",
            "v1",
            EVALUATION_TIME,
            Duration.ofHours(1),
            EnumSet.of(
                OutcomeSource.DRY_RUN_RESULT,
                OutcomeSource.DETERMINISTIC_REPLAY,
                OutcomeSource.STRUCTURED_TEST_FIXTURE),
            EnumSet.of(
                AttributionDimension.EVIDENCE_QUALITY,
                AttributionDimension.RISK_DISCIPLINE),
            Map.of(
                AttributionDimension.EVIDENCE_QUALITY, new BigDecimal("0.75"),
                AttributionDimension.RISK_DISCIPLINE, new BigDecimal("0.5")));
    return new FeedbackAttributionCommand(subject, observation, policy);
  }

  static DefaultFeedbackAttributionService service(
      final BoundedIdempotencyPort idempotencyPort, final RecordingAuditPort auditPort) {
    return new DefaultFeedbackAttributionService(
        new FeedbackCanonicalizer(),
        new FeedbackAttributionPolicyEvaluator(),
        idempotencyPort,
        auditPort);
  }

  static final class RecordingAuditPort implements FeedbackAttributionAuditPort {

    private final AtomicInteger writes = new AtomicInteger();
    private final boolean fail;

    RecordingAuditPort() {
      this(false);
    }

    RecordingAuditPort(final boolean fail) {
      this.fail = fail;
    }

    @Override
    public FeedbackAuditReference write(final FeedbackAttributionAuditRecord record) {
      if (fail) {
        throw new FeedbackAttributionAuditException("test audit unavailable");
      }
      writes.incrementAndGet();
      return new FeedbackAuditReference(
          record.command().subject().tenantId(),
          record.command().subject().environment(),
          record.command().subject().decisionId(),
          record.command().subject().traceId(),
          record.command().observation().observationId(),
          record.resultIdentity(),
          record.command().policy().policyId(),
          record.command().policy().policyVersion(),
          record.canonicalHash().value(),
          "AUDIT:" + record.resultIdentity().substring(0, 16),
          "REPLAY:" + record.canonicalHash().value().substring(0, 16));
    }

    int writeCount() {
      return writes.get();
    }
  }

  static final class BoundedIdempotencyPort implements FeedbackAttributionIdempotencyPort {

    private final int capacity;
    private final ConcurrentHashMap<String, Entry> entries = new ConcurrentHashMap<>();
    private final boolean unavailable;

    BoundedIdempotencyPort(final int capacity) {
      this(capacity, false);
    }

    BoundedIdempotencyPort(final int capacity, final boolean unavailable) {
      this.capacity = capacity;
      this.unavailable = unavailable;
    }

    @Override
    public FeedbackAttributionResult execute(
        final String idempotencyKey,
        final String canonicalHash,
        final FirstAttributionExecution firstExecution) {
      if (unavailable) {
        throw new FeedbackAttributionIdempotencyException(
            FeedbackAttributionIdempotencyException.Kind.UNAVAILABLE,
            "test idempotency unavailable");
      }
      final Entry entry =
          entries.compute(
              idempotencyKey,
              (key, existing) -> {
                if (existing != null) {
                  if (!existing.canonicalHash().equals(canonicalHash)) {
                    throw new FeedbackAttributionIdempotencyException(
                        FeedbackAttributionIdempotencyException.Kind.CONFLICT,
                        "test canonical hash conflict");
                  }
                  return existing;
                }
                if (entries.size() >= capacity) {
                  throw new FeedbackAttributionIdempotencyException(
                      FeedbackAttributionIdempotencyException.Kind.UNAVAILABLE,
                      "test idempotency capacity reached");
                }
                return new Entry(canonicalHash, firstExecution.execute());
              });
      return entry.result();
    }

    int size() {
      return entries.size();
    }

    private record Entry(String canonicalHash, FeedbackAttributionResult result) {}
  }
}
