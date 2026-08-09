package com.guidinglight.decisionhub.usecase.qdr.evidence;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.util.Objects;

/** Persisted, fail-closed decision-origin environment provenance. */
public record DecisionEnvironmentProvenance(
        Status status,
        FeedbackEnvironment environment,
        Source source,
        String safeReference) {

    /** Enforces that only a unique persisted proof may carry an environment. */
    public DecisionEnvironmentProvenance {
        status = Objects.requireNonNull(status, "status");
        source = Objects.requireNonNull(source, "source");
        safeReference = DecisionEvidencePolicy.optionalSafeText(safeReference, "safeReference");
        if (status == Status.PROVEN) {
            environment = Objects.requireNonNull(environment, "environment");
            if (source != Source.QDR7_COMPLETED_IDEMPOTENCY_GUARD || safeReference == null) {
                throw new IllegalArgumentException("proven provenance requires a persistent guard reference");
            }
        } else if (environment != null || safeReference != null) {
            throw new IllegalArgumentException("unproven provenance cannot carry inferred origin data");
        }
    }

    /** Creates unique persisted provenance from an exact completed guard row. */
    public static DecisionEnvironmentProvenance proven(
            final FeedbackEnvironment environment, final String guardReference) {
        return new DecisionEnvironmentProvenance(
                Status.PROVEN,
                environment,
                Source.QDR7_COMPLETED_IDEMPOTENCY_GUARD,
                guardReference);
    }

    public static DecisionEnvironmentProvenance missing() {
        return unproven(Status.MISSING);
    }

    public static DecisionEnvironmentProvenance ambiguous() {
        return unproven(Status.AMBIGUOUS);
    }

    public static DecisionEnvironmentProvenance invalid() {
        return unproven(Status.INVALID);
    }

    public static DecisionEnvironmentProvenance notEvaluated() {
        return unproven(Status.NOT_EVALUATED);
    }

    public boolean isProven() {
        return status == Status.PROVEN;
    }

    private static DecisionEnvironmentProvenance unproven(final Status status) {
        return new DecisionEnvironmentProvenance(
                status, null, Source.QDR7_COMPLETED_IDEMPOTENCY_GUARD, null);
    }

    public enum Status {
        PROVEN,
        MISSING,
        AMBIGUOUS,
        INVALID,
        NOT_EVALUATED
    }

    public enum Source {
        QDR7_COMPLETED_IDEMPOTENCY_GUARD
    }
}
