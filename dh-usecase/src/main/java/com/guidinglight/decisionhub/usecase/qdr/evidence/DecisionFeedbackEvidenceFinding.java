package com.guidinglight.decisionhub.usecase.qdr.evidence;

import java.util.Objects;

/** Consolidated evidence 的稳定、脱敏 finding。 */
public record DecisionFeedbackEvidenceFinding(
        Code code, Severity severity, String safeRef, String summary) {

    /** 只允许稳定 code 与安全引用进入内部 aggregate。 */
    public DecisionFeedbackEvidenceFinding {
        code = Objects.requireNonNull(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        safeRef = DecisionEvidencePolicy.optionalSafeText(safeRef, "safeRef");
        summary = DecisionEvidencePolicy.requireSafeText(summary, "summary");
    }

    /** Stage-QDR-10 冻结的 stable failure taxonomy。 */
    public enum Code {
        EXECUTION_SCOPE_REQUIRED,
        ENVIRONMENT_INVALID,
        DECISION_ROOT_NOT_FOUND,
        DECISION_EVIDENCE_INCOMPLETE,
        DECISION_EVIDENCE_INVALID,
        DECISION_ENVIRONMENT_PROVENANCE_MISSING,
        DECISION_ENVIRONMENT_PROVENANCE_AMBIGUOUS,
        DECISION_ENVIRONMENT_PROVENANCE_INVALID,
        TENANT_MISMATCH,
        ENVIRONMENT_MISMATCH,
        DECISION_MISMATCH,
        TRACE_MISMATCH,
        REQUEST_MISMATCH,
        RUN_MISMATCH,
        FEEDBACK_SOURCE_FAILED,
        FEEDBACK_RESULT_LIMIT_EXCEEDED,
        FEEDBACK_IDENTITY_CONFLICT,
        FEEDBACK_AGGREGATE_INCOMPLETE,
        FEEDBACK_ORDER_INVALID,
        OPTIONAL_FEEDBACK_ABSENT,
        UNSAFE_EVIDENCE_REJECTED
    }

    /** Finding 严重度；ERROR/BLOCKER 必须使 aggregate fail-closed。 */
    public enum Severity {
        INFO,
        WARN,
        ERROR,
        BLOCKER
    }
}
