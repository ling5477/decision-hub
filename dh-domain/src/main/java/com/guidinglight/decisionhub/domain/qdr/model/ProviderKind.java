package com.guidinglight.decisionhub.domain.qdr.model;

/**
 * stage-qdr-3 B1 provider 类型。
 *
 * <p>B1 只允许 mock/local-planned 语义；该枚举不代表真实 SDK 或真实 outbound provider。
 */
public enum ProviderKind {
    /**
     * 纯内存 mock provider identity。
     */
    MOCK,
    /**
     * 后续本地/离线规划占位，不代表 runtime 已接入。
     */
    LOCAL_PLANNED
}
