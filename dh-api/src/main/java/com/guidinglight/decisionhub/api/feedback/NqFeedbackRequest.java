package com.guidinglight.decisionhub.api.feedback;

import com.guidinglight.decisionhub.domain.feedback.FeedbackSource;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.Map;

/**
 * Stage1：接收 NQ 回流事件的请求体。
 *
 * <p>对应工单 4.6：POST /api/ai/feedback/nq。
 */
public final class NqFeedbackRequest {

  @NotBlank(message = "runId must not be blank")
  private String runId;

  private String candidateId;

  @NotNull(message = "source must not be null")
  private FeedbackSource source;

  @NotBlank(message = "eventType must not be blank")
  private String eventType;

  private boolean positive;

  private Map<String, Object> payloadJson;

  private Instant occurredAt;

  public String getRunId() {
    return runId;
  }

  public void setRunId(final String runId) {
    this.runId = runId;
  }

  public String getCandidateId() {
    return candidateId;
  }

  public void setCandidateId(final String candidateId) {
    this.candidateId = candidateId;
  }

  public FeedbackSource getSource() {
    return source;
  }

  public void setSource(final FeedbackSource source) {
    this.source = source;
  }

  public String getEventType() {
    return eventType;
  }

  public void setEventType(final String eventType) {
    this.eventType = eventType;
  }

  public boolean isPositive() {
    return positive;
  }

  public void setPositive(final boolean positive) {
    this.positive = positive;
  }

  public Map<String, Object> getPayloadJson() {
    return payloadJson;
  }

  public void setPayloadJson(final Map<String, Object> payloadJson) {
    this.payloadJson = payloadJson;
  }

  public Instant getOccurredAt() {
    return occurredAt;
  }

  public void setOccurredAt(final Instant occurredAt) {
    this.occurredAt = occurredAt;
  }
}
