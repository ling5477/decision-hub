package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.ProviderProfile;

import java.util.Optional;

/**
 * Mock provider profile registry port。
 *
 * <p>B4 只允许内存 mock profile bootstrap；该端口不读取环境变量、不读取 credential、不访问 HTTP，也不代表
 * real provider 已接入。ProviderTrustPolicy 必须通过本 registry 查询 profile，避免业务代码绕过 trust
 * policy。
 */
public interface ProviderProfileRegistryPort {

    /**
     * 注册 mock provider profile。
     *
     * @param providerProfile mock provider profile。
     * @return 已注册或已存在且完全一致的 profile。
     */
    ProviderProfile register(ProviderProfile providerProfile);

    /**
     * 按 providerProfileId 查询 profile。
     *
     * @param providerProfileId provider profile id。
     * @return 命中 profile；missing 时由 ProviderTrustPolicy fail-closed。
     */
    Optional<ProviderProfile> findById(String providerProfileId);
}
