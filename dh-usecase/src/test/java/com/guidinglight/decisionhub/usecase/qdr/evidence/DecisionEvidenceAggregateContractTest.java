package com.guidinglight.decisionhub.usecase.qdr.evidence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionEvidence;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionEvidenceView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.RedactionStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class DecisionEvidenceAggregateContractTest {

    @Test
    void validCompleteAggregate() {
        final DecisionEvidenceQuery query = coreQuery();

        final DecisionEvidenceAggregate aggregate =
                DecisionEvidenceAggregate.evaluate(query, coreEvidence(query.correlation()));

        assertEquals(DecisionEvidenceStatus.COMPLETE, aggregate.status());
        assertTrue(aggregate.isComplete());
        assertTrue(aggregate.missingMandatoryEvidence().isEmpty());
    }

    @Test
    void missingTenantIdIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> query(null, "trace-1", "request-1", "decision-1", DecisionEvidencePolicy.CORE_DECISION));
    }

    @Test
    void missingTraceIdIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> query("tenant-1", null, "request-1", "decision-1", DecisionEvidencePolicy.CORE_DECISION));
    }

    @Test
    void missingRequestIdIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> query("tenant-1", "trace-1", null, "decision-1", DecisionEvidencePolicy.CORE_DECISION));
    }

    @Test
    void missingDecisionIdIsRejected() {
        assertThrows(
                NullPointerException.class,
                () -> query("tenant-1", "trace-1", "request-1", null, DecisionEvidencePolicy.CORE_DECISION));
    }

    @Test
    void blankCorrelationValueIsRejectedAfterTrim() {
        assertThrows(
                IllegalArgumentException.class,
                () -> query("tenant-1", " ", "request-1", "decision-1", DecisionEvidencePolicy.CORE_DECISION));
    }

    @Test
    void traceIdConflictIsInvalid() {
        assertInvalidCorrelation("tenant-1", "trace-other", "request-1", "decision-1");
    }

    @Test
    void requestIdConflictIsInvalid() {
        assertInvalidCorrelation("tenant-1", "trace-1", "request-other", "decision-1");
    }

    @Test
    void decisionIdConflictIsInvalidWithoutFallbackToTraceOrRequest() {
        assertInvalidCorrelation("tenant-1", "trace-1", "request-1", "decision-other");
    }

    @Test
    void tenantIdConflictIsInvalidAndRecordsTenantMismatch() {
        final DecisionEvidenceQuery query = coreQuery();
        final DecisionEvidenceRef foreignRef =
                ref(
                        new DecisionEvidenceCorrelation("tenant-other", "trace-1", "request-1", "decision-1"),
                        DecisionEvidencePolicy.EvidenceType.REQUEST,
                        true,
                        null);

        final DecisionEvidenceAggregate aggregate =
                DecisionEvidenceAggregate.evaluate(query, List.of(foreignRef));

        assertEquals(DecisionEvidenceStatus.INVALID, aggregate.status());
        assertTrue(aggregate.findings().stream().anyMatch(finding -> finding.code().equals("TENANT_MISMATCH")));
    }

    @Test
    void duplicateEvidenceRefWithDifferentHashIsInvalid() {
        final DecisionEvidenceQuery query = coreQuery();
        final DecisionEvidenceCorrelation correlation = query.correlation();
        final DecisionEvidenceRef first =
                ref(correlation, DecisionEvidencePolicy.EvidenceType.REQUEST, true, "a".repeat(64));
        final DecisionEvidenceRef inconsistent =
                ref(correlation, DecisionEvidencePolicy.EvidenceType.REQUEST, true, "b".repeat(64));

        final DecisionEvidenceAggregate aggregate =
                DecisionEvidenceAggregate.evaluate(query, List.of(first, inconsistent));

        assertEquals(DecisionEvidenceStatus.INVALID, aggregate.status());
        assertTrue(
                aggregate.findings().stream()
                        .anyMatch(finding -> finding.code().equals("EVIDENCE_REF_INVALID")));
    }

    @Test
    void missingMandatoryEvidenceReturnsIncomplete() {
        final DecisionEvidenceQuery query = coreQuery();
        final List<DecisionEvidenceRef> incompleteRefs =
                coreEvidence(query.correlation()).stream()
                        .filter(ref -> ref.evidenceType() != DecisionEvidencePolicy.EvidenceType.AUDIT_EVENT)
                        .toList();

        final DecisionEvidenceAggregate aggregate =
                DecisionEvidenceAggregate.evaluate(query, incompleteRefs);

        assertEquals(DecisionEvidenceStatus.INCOMPLETE, aggregate.status());
        assertTrue(
                aggregate.missingMandatoryEvidence().contains(DecisionEvidencePolicy.EvidenceType.AUDIT_EVENT));
    }

    @Test
    void missingOptionalEvidenceIsNotFabricatedAsPresent() {
        final DecisionEvidenceQuery query = coreQuery();

        final DecisionEvidenceAggregate aggregate =
                DecisionEvidenceAggregate.evaluate(query, coreEvidence(query.correlation()));

        assertEquals(DecisionEvidenceStatus.COMPLETE, aggregate.status());
        assertFalse(
                aggregate.evidenceRefs().stream()
                        .anyMatch(
                                ref ->
                                        ref.evidenceType()
                                                == DecisionEvidencePolicy.EvidenceType.REGRESSION_FINDING));
    }

    @Test
    void rawPromptRefIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> unsafeRef("safe-ref", "REQUEST", "raw prompt", "safe-source"));
    }

    @Test
    void rawProviderResponseRefIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> unsafeRef("safe-ref", "REQUEST", "raw provider response", "safe-source"));
    }

    @Test
    void credentialLikeRefIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> unsafeRef("apiSecret-ref", "REQUEST", "safe summary", "safe-source"));
    }

    @Test
    void tradingInstructionSemanticIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> unsafeRef("safe-ref", "REQUEST", "BUY", "safe-source"));
    }

    @Test
    void existingDecisionEvidenceViewIsAdaptedWithoutReadingItsPayload() {
        final DecisionEvidenceCorrelation correlation =
                new DecisionEvidenceCorrelation("tenant-1", "trace-1", "request-1", "decision-1");
        final DecisionEvidenceView existingView =
                new DecisionEvidenceView(
                        "run-1",
                        "tenant-1",
                        "trace-1",
                        "context-safe-ref",
                        List.of("provider-safe-ref"),
                        "output-safe-ref",
                        "quant-safe-ref",
                        RedactionStatus.REDACTED,
                        "safe-refs-json");

        final DecisionEvidenceRef ref =
                DecisionEvidenceRef.fromReadModel(
                        existingView,
                        correlation,
                        DecisionEvidencePolicy.EvidenceType.REQUEST,
                        "request-safe-ref",
                        null,
                        "decision-read-model",
                        true);

        assertEquals(RedactionStatus.REDACTED, ref.redactionStatus());
        assertEquals("request-safe-ref", ref.evidence().evidenceId());
    }

    @Test
    void existingDecisionReplayViewRequiresAllFourCorrelationKeys() {
        final DecisionReplayView replayView =
                DecisionReplayView.incomplete(
                        "tenant-1", "decision-1", "trace-1", "request-1", List.of());

        assertEquals(
                new DecisionEvidenceCorrelation("tenant-1", "trace-1", "request-1", "decision-1"),
                DecisionEvidenceCorrelation.fromReplayView(replayView));
    }

    @Test
    void contractsHaveNoRepositoryJdbcHttpProviderNqAgentOrLangGraphDependency() {
        final List<Class<?>> contractTypes =
                List.of(
                        DecisionEvidenceQuery.class,
                        DecisionEvidenceCorrelation.class,
                        DecisionEvidenceAggregate.class,
                        DecisionEvidenceRef.class,
                        DecisionEvidenceStatus.class,
                        DecisionEvidenceFinding.class,
                        DecisionEvidencePolicy.class);

        final List<String> forbiddenTypeNames =
                contractTypes.stream()
                        .flatMap(DecisionEvidenceAggregateContractTest::referencedTypes)
                        .map(Class::getName)
                        .filter(DecisionEvidenceAggregateContractTest::isForbiddenDependency)
                        .toList();

        assertTrue(forbiddenTypeNames.isEmpty(), () -> "forbidden contract dependencies: " + forbiddenTypeNames);
    }

    private static void assertInvalidCorrelation(
            final String tenantId, final String traceId, final String requestId, final String decisionId) {
        final DecisionEvidenceQuery query = coreQuery();
        final DecisionEvidenceRef conflictingRef =
                ref(
                        new DecisionEvidenceCorrelation(tenantId, traceId, requestId, decisionId),
                        DecisionEvidencePolicy.EvidenceType.REQUEST,
                        true,
                        null);

        assertEquals(
                DecisionEvidenceStatus.INVALID,
                DecisionEvidenceAggregate.evaluate(query, List.of(conflictingRef)).status());
    }

    private static DecisionEvidenceQuery coreQuery() {
        return query("tenant-1", "trace-1", "request-1", "decision-1", DecisionEvidencePolicy.CORE_DECISION);
    }

    private static DecisionEvidenceQuery query(
            final String tenantId,
            final String traceId,
            final String requestId,
            final String decisionId,
            final DecisionEvidencePolicy policy) {
        return new DecisionEvidenceQuery(
                tenantId,
                traceId,
                requestId,
                decisionId,
                null,
                null,
                null,
                null,
                null,
                policy);
    }

    private static List<DecisionEvidenceRef> coreEvidence(final DecisionEvidenceCorrelation correlation) {
        return DecisionEvidencePolicy.CORE_DECISION.mandatoryEvidenceTypes().stream()
                .map(type -> ref(correlation, type, true, null))
                .toList();
    }

    private static DecisionEvidenceRef ref(
            final DecisionEvidenceCorrelation correlation,
            final DecisionEvidencePolicy.EvidenceType type,
            final boolean mandatory,
            final String contentHash) {
        final String refId = type.name().toLowerCase() + "-ref";
        return new DecisionEvidenceRef(
                new DecisionEvidence(refId, type.name(), "safe structured summary"),
                correlation,
                type,
                refId,
                contentHash,
                "unit-test-source",
                mandatory,
                RedactionStatus.REDACTED);
    }

    private static DecisionEvidenceRef unsafeRef(
            final String refId, final String evidenceType, final String summary, final String sourceType) {
        return new DecisionEvidenceRef(
                new DecisionEvidence(refId, evidenceType, summary),
                new DecisionEvidenceCorrelation("tenant-1", "trace-1", "request-1", "decision-1"),
                DecisionEvidencePolicy.EvidenceType.valueOf(evidenceType),
                refId,
                null,
                sourceType,
                true,
                RedactionStatus.REDACTED);
    }

    private static Stream<Class<?>> referencedTypes(final Class<?> type) {
        final Stream<Class<?>> fieldTypes = Arrays.stream(type.getDeclaredFields()).map(Field::getType);
        final Stream<Class<?>> constructorTypes =
                Arrays.stream(type.getDeclaredConstructors())
                        .flatMap(DecisionEvidenceAggregateContractTest::parameterTypes);
        final Stream<Class<?>> methodTypes =
                Arrays.stream(type.getDeclaredMethods())
                        .flatMap(
                                method ->
                                        Stream.concat(
                                                Stream.of(method.getReturnType()), parameterTypes(method)));
        return Stream.concat(Stream.concat(fieldTypes, constructorTypes), methodTypes);
    }

    private static Stream<Class<?>> parameterTypes(final Constructor<?> constructor) {
        return Arrays.stream(constructor.getParameterTypes());
    }

    private static Stream<Class<?>> parameterTypes(final Method method) {
        return Arrays.stream(method.getParameterTypes());
    }

    private static boolean isForbiddenDependency(final String typeName) {
        final String normalized = typeName.toLowerCase();
        return normalized.contains(".repository.")
                || normalized.contains(".jdbc.")
                || normalized.contains(".http.")
                || normalized.contains(".provider.")
                || normalized.contains(".nq.")
                || normalized.contains(".agent.")
                || normalized.contains(".langgraph.");
    }
}
