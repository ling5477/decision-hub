package com.guidinglight.decisionhub.usecase.facade;

import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateCommand;
import com.guidinglight.decisionhub.usecase.facade.dto.RunCreateResult;

/**
 * 决策平台用例门面：为 dh-app 与 dh-eval 提供稳定入口。
 */
public interface DecisionHubFacade {

    /**
     * 创建 Run（一次决策执行实例）。
     */
    RunCreateResult createRun(RunCreateCommand command);
}
