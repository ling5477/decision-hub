package com.guidinglight.decisionhub.usecase.decision;

import static com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunAssertionSupport.assertFixtureContainsNoCredentialOrExecutionIntent;
import static com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunAssertionSupport.assertMockNqRequestIsReadOnly;
import static com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunAssertionSupport.assertStructuredReadOnlyOutput;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionStatus;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunFixtures;
import org.junit.jupiter.api.Test;

/**
 * K6 mock NQ dry-run 主合同测试，验证 mock 请求可以安全驱动只读 structured output。
 */
final class MockNqDecisionDryRunContractTest {

    @Test
    void mockNqValidRequestProducesStructuredDecisionOutput() {
        final DecisionRequest request = MockNqDryRunFixtures.validDryRunRequest();
        final DecisionOutput output = new DefaultDecisionOrchestrator().decide(request);

        assertMockNqRequestIsReadOnly(request);
        assertStructuredReadOnlyOutput(output);
        assertEquals(request.getRequestId(), output.getRequestId());
        assertEquals(request.getTraceId(), output.getTraceId());
        assertEquals(request.getTenantId(), output.getTenantId());
        assertEquals(DecisionAction.NO_TRADE, output.getAction());
        assertEquals(DecisionStatus.OBSERVATION_ONLY, output.getStatus());
        assertEquals(ProviderSignalStatus.MOCKED, output.getProviderStatus());
        assertTrue(output.getReasonCodes().contains("MOCK_NO_TRADE"));
    }

    @Test
    void mockNqFixturesRemainSchemaAlignedAndCredentialFree() {
        for (String filename : MockNqDryRunFixtures.fixtureFilenames()) {
            final String fixture = MockNqDryRunFixtures.readFixture(filename);

            assertTrue(fixture.contains("\"tenantId\""));
            assertTrue(fixture.contains("\"requestId\""));
            assertTrue(fixture.contains("\"traceId\""));
            assertTrue(fixture.contains("\"source\""));
            assertTrue(fixture.contains("\"NQ_MOCK\""));
            assertTrue(fixture.contains("\"decisionType\""));
            assertTrue(fixture.contains("\"READ_ONLY_RECOMMENDATION\""));
            assertTrue(fixture.contains("\"schemaVersion\""));
            assertTrue(fixture.contains("\"subject\""));
            assertTrue(fixture.contains("\"symbol\""));
            assertTrue(fixture.contains("\"market\""));
            assertTrue(fixture.contains("\"timeframe\""));
            assertFixtureContainsNoCredentialOrExecutionIntent(fixture);
        }
    }
}
