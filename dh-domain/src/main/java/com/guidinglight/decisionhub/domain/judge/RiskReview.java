package com.guidinglight.decisionhub.domain.judge;

import com.guidinglight.decisionhub.common.util.IdGenerator;
import java.time.Instant;
import java.util.Map;

/**
 * Stage1：RiskReviewer 出具的风险审查。
 *
 * <p>对应工单 4.1：RiskReview。
 */
public final class RiskReview {

  private final String reviewId;
  private final String candidateId;
  private final String runId;
  private final String traceId;
  private final double riskScore;
  private final boolean rejected;
  private final Map<String, Object> payloadJson;
  private final Instant createdAt;

  private RiskReview(
      final String reviewId,
      final String candidateId,
      final String runId,
      final String traceId,
      final double riskScore,
      final boolean rejected,
      final Map<String, Object> payloadJson,
      final Instant createdAt) {
    this.reviewId = reviewId;
    this.candidateId = candidateId;
    this.runId = runId;
    this.traceId = traceId;
    this.riskScore = riskScore;
    this.rejected = rejected;
    this.payloadJson = payloadJson == null ? Map.of() : Map.copyOf(payloadJson);
    this.createdAt = createdAt;
  }

  /** 工厂方法。 */
  public static RiskReview create(
      final String candidateId,
      final String runId,
      final String traceId,
      final double riskScore,
      final boolean rejected,
      final Map<String, Object> payloadJson,
      final Instant now) {
    return new RiskReview(
        IdGenerator.newId(),
        candidateId,
        runId,
        traceId,
        riskScore,
        rejected,
        payloadJson,
        now);
  }

  public String getReviewId() {
    return reviewId;
  }

  public String getCandidateId() {
    return candidateId;
  }

  public String getRunId() {
    return runId;
  }

  public String getTraceId() {
    return traceId;
  }

  public double getRiskScore() {
    return riskScore;
  }

  public boolean isRejected() {
    return rejected;
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
