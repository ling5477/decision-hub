package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersion;

import java.util.Optional;

/**
 * PromptVersion registry port。
 *
 * <p>B1 只允许 in-memory/mock 实现；不得依赖 Spring Web、JDBC/JPA、dh-api、dh-infra、provider SDK、
 * HTTP client、LangGraph、AutoGen 或 CrewAI。
 */
public interface PromptVersionRegistryPort {

    /**
     * 注册 immutable PromptVersion。
     *
     * <p>相同 tenant/template/version + 相同 checksum 可以幂等接受；相同 key 但 checksum 不同必须
     * fail-closed。
     *
     * @param command registration command。
     * @return registry result。
     */
    PromptVersionRegistryResult register(PromptVersionRegistrationCommand command);

    /**
     * 查询当前 tenant 下的 PromptVersion。
     *
     * @param query lookup query。
     * @return 命中结果；跨 tenant 查询必须返回 empty 或 fail-closed。
     */
    Optional<PromptVersion> lookup(PromptVersionLookupQuery query);
}
