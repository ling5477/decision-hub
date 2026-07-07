package com.guidinglight.decisionhub.usecase.qdr.model;

import java.util.List;

/**
 * PromptInjectionGuard 判定结果。
 *
 * @param allowed    true 表示允许继续 render/gateway；false 表示 fail-closed。
 * @param violations 拒绝原因列表，不包含 raw prompt。
 */
public record PromptInjectionDecision(boolean allowed, List<PromptInjectionViolation> violations) {

    /**
     * 复制 violation 列表，避免调用方修改。
     */
    public PromptInjectionDecision {
        violations = List.copyOf(violations == null ? List.of() : violations);
        if (allowed && !violations.isEmpty()) {
            throw new IllegalArgumentException("allowed decision must not contain violations");
        }
    }

    /**
     * 创建允许结果。
     *
     * @return allowed decision。
     */
    public static PromptInjectionDecision allow() {
        return new PromptInjectionDecision(true, List.of());
    }

    /**
     * 创建拒绝结果。
     *
     * @param violations 拒绝原因。
     * @return denied decision。
     */
    public static PromptInjectionDecision deny(final List<PromptInjectionViolation> violations) {
        return new PromptInjectionDecision(false, violations);
    }
}
