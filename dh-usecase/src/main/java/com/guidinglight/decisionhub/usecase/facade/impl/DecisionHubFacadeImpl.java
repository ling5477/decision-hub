package com.guidinglight.decisionhub.usecase.facade.impl;

import com.guidinglight.decisionhub.usecase.facade.DecisionHubFacade;
import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateCommand;
import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateResult;

import java.util.Objects;
import java.util.UUID;

/**
 * v1 最小可跑实现：用于 golden 回归闭环。
 *
 * @deprecated Stage1-CLOSE：与 DecisionHubFacade 一起退役；新链路通过
 *     {@code DefaultResearchRunCommandService} 提供 create/start。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class DecisionHubFacadeImpl implements DecisionHubFacade {

    @Override
    public RunCreateResult createRun(RunCreateCommand command) {
        Objects.requireNonNull(command, "command");
        if (command.getTenantId() == null || command.getTenantId().isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        if (command.getTopic() == null || command.getTopic().isBlank()) {
            throw new IllegalArgumentException("topic must not be blank");
        }
        return new RunCreateResult("run_" + UUID.randomUUID(), "CREATED");
    }
}
