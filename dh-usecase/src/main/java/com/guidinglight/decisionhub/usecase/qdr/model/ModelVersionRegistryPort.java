package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.ModelVersion;

import java.util.Optional;

/**
 * ModelVersion registry port。
 *
 * <p>B2 只允许 in-memory/mock 实现；不得依赖 Spring Web、JDBC/JPA、dh-api、dh-infra、
 * provider SDK、HTTP client、LangGraph、AutoGen 或 CrewAI。
 */
public interface ModelVersionRegistryPort {

    /**
     * 注册 immutable ModelVersion。
     *
     * <p>相同 tenant/modelVersionId + 相同 checksum 可以幂等接受；相同 key 但 checksum 不同必须
     * fail-closed。
     *
     * @param command registration command。
     * @return registry result。
     */
    ModelVersionRegistryResult register(ModelVersionRegistrationCommand command);

    /**
     * 查询当前 tenant 下的 ModelVersion。
     *
     * @param query lookup query。
     * @return 命中结果；跨 tenant 查询必须返回 empty 或 fail-closed。
     */
    Optional<ModelVersion> lookup(ModelVersionLookupQuery query);
}
