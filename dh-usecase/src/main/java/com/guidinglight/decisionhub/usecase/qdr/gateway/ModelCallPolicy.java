package com.guidinglight.decisionhub.usecase.qdr.gateway;

/**
 * Model gateway 调用策略。
 *
 * <p>B2 只允许 mock provider。若 policy 缺失、允许非 mock 或无法证明 provider enabled，
 * gateway 必须 fail-closed。
 *
 * @param policyRef              policy ref，用于审计引用。
 * @param mockOnly               true 表示只允许 mock provider。
 * @param requireEnabledProvider true 表示 provider profile 必须 ENABLED。
 * @param requireAuditRef        true 表示成功结果必须返回 planned audit ref。
 */
public record ModelCallPolicy(
        String policyRef,
        boolean mockOnly,
        boolean requireEnabledProvider,
        boolean requireAuditRef) {
}
