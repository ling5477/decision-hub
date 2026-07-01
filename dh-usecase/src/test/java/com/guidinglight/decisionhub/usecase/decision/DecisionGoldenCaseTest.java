package com.guidinglight.decisionhub.usecase.decision;

import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.REQUIRED_FILENAMES;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.all;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.fieldNames;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.requiredObject;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.requiredText;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.schema;
import static com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.stringList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.guidinglight.decisionhub.usecase.decision.support.DecisionGoldenCaseFixtures.GoldenCase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * K7 golden case 结构测试。
 *
 * <p>该测试把 `golden_cases/decision/*.json` 固化为 K7 eval baseline：所有 fixture 必须可解析，且 input /
 * expectedDecision 必须贴合 K1 request/output schema 的 required、properties 与 enum 约束。
 */
final class DecisionGoldenCaseTest {

    private static final Set<String> FIXED_FORBIDDEN_ACTIONS =
            Set.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE", "READ_NQ_DB", "WRITE_NQ_DB");
    private static final Set<String> ALLOWED_ACTIONS =
            Set.of("ABSTAIN", "OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS");

    @Test
    void allRequiredGoldenCaseFilesExistAndAreWrapped() {
        final List<GoldenCase> cases = all();
        final Set<String> actual = new HashSet<>(cases.stream().map(GoldenCase::filename).toList());

        assertEquals(Set.copyOf(REQUIRED_FILENAMES), actual);
        assertEquals(12, cases.size());
        for (GoldenCase goldenCase : cases) {
            assertFalse(goldenCase.caseId().isBlank());
            assertFalse(requiredText(goldenCase.root(), "description").isBlank());
            assertTrue(goldenCase.input().isObject());
            assertTrue(goldenCase.expectedDecision().isObject());
            assertTrue(requiredObject(goldenCase.root(), "forbiddenAssertions").isObject());
            assertTrue(requiredObject(goldenCase.root(), "securityBoundary").isObject());
        }
    }

    @Test
    void allInputsMatchDecisionRequestSchemaShape() {
        final JsonNode requestSchema = schema("dh-decision-request.schema.json");
        for (GoldenCase goldenCase : all()) {
            assertObjectMatchesSchemaTopLevel(goldenCase.input(), requestSchema, goldenCase.caseId());
            assertNestedObjectMatchesSchema(
                    goldenCase.input(), requestSchema, "subject", goldenCase.caseId());
            assertNestedObjectMatchesSchema(
                    goldenCase.input(), requestSchema, "contextSnapshot", goldenCase.caseId());
            assertEquals(
                    "READ_ONLY_RECOMMENDATION",
                    requiredText(goldenCase.input(), "decisionType"),
                    goldenCase.caseId());
            assertEquals("1.0.0", requiredText(goldenCase.input(), "schemaVersion"), goldenCase.caseId());
        }
    }

    @Test
    void allExpectedDecisionsMatchDecisionOutputSchemaShape() {
        final JsonNode outputSchema = schema("dh-decision-output.schema.json");
        for (GoldenCase goldenCase : all()) {
            final JsonNode expected = goldenCase.expectedDecision();

            assertObjectMatchesSchemaTopLevel(expected, outputSchema, goldenCase.caseId());
            assertEquals("READ_ONLY_RECOMMENDATION", requiredText(expected, "decisionType"));
            assertTrue(ALLOWED_ACTIONS.contains(requiredText(expected, "action")), goldenCase.caseId());
            assertEquals(FIXED_FORBIDDEN_ACTIONS, Set.copyOf(stringList(expected.path("forbiddenActions"))));
            assertEquals("1.0.0", requiredText(expected, "schemaVersion"), goldenCase.caseId());
        }
    }

    private static void assertObjectMatchesSchemaTopLevel(
            final JsonNode payload, final JsonNode schemaNode, final String caseId) {
        final Set<String> required = Set.copyOf(stringList(schemaNode.path("required")));
        final Set<String> allowed = fieldNames(schemaNode.path("properties"));

        assertTrue(fieldNames(payload).containsAll(required), caseId);
        assertTrue(allowed.containsAll(fieldNames(payload)), caseId);
    }

    private static void assertNestedObjectMatchesSchema(
            final JsonNode payload, final JsonNode schemaNode, final String field, final String caseId) {
        final JsonNode nestedPayload = payload.path(field);
        if (nestedPayload.isMissingNode() || nestedPayload.isNull()) {
            return;
        }
        final JsonNode nestedSchema = schemaNode.path("properties").path(field);
        final Set<String> required = Set.copyOf(stringList(nestedSchema.path("required")));
        final Set<String> allowed = fieldNames(nestedSchema.path("properties"));

        assertTrue(fieldNames(nestedPayload).containsAll(required), caseId + ":" + field);
        assertTrue(allowed.containsAll(fieldNames(nestedPayload)), caseId + ":" + field);
    }
}
