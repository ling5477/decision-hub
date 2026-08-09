package com.guidinglight.decisionhub.usecase.qdr.evidence;

/** Exact, bounded and read-only decision-origin environment provenance lookup. */
public interface DecisionEnvironmentProvenanceQueryPort {

    /** Returns PROVEN only for one exact persisted origin; missing, ambiguity and store failures fail closed. */
    DecisionEnvironmentProvenance find(DecisionEnvironmentProvenanceQuery query);
}
