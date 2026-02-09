package com.guidinglight.decisionhub.usecase.facade.impl;

import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateCommand;
import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DecisionHubFacadeImplTest {

    @Test
    void shouldCreateRunWithCreatedStatusWhenInputValid() {
        DecisionHubFacadeImpl facade = new DecisionHubFacadeImpl();
        RunCreateCommand command = new RunCreateCommand();
        command.setTenantId("t1");
        command.setTopic("decision hub v1");

        RunCreateResult result = facade.createRun(command);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getRunId());
        Assertions.assertTrue(result.getRunId().startsWith("run_"));
        Assertions.assertEquals("CREATED", result.getStatus());
    }
}
