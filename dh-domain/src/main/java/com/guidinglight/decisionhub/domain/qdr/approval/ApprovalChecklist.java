package com.guidinglight.decisionhub.domain.qdr.approval;

import java.util.Map;
import java.util.Objects;

/**
 * 人工审批 checklist。
 *
 * <p>checklist 只保存脱敏审查项，不保存 credential、token、cookie、apiKey、apiSecret、passphrase、
 * raw provider response 或 raw prompt。
 */
public record ApprovalChecklist(Map<String, Object> items) {

    /**
     * 校验 checklist 非空且不含疑似敏感材料。
     */
    public ApprovalChecklist {
        items = Map.copyOf(Objects.requireNonNull(items, "items"));
        ApprovalContentGuard.rejectSecretLike("checklistJson", items);
    }
}
