package com.guidinglight.decisionhub.usecase.qdr.feedback;

/** Usecase-owned internal port for bounded historical evidence reads. */
public interface HistoricalFeedbackEvidenceQueryPort {

  /** Returns a complete, tenant/environment-bound keyset page or fails closed. */
  HistoricalFeedbackEvidencePage query(HistoricalFeedbackEvidenceQuery query);
}
