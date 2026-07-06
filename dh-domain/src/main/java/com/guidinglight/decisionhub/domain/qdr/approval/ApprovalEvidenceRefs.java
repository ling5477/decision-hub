package com.guidinglight.decisionhub.domain.qdr.approval;

import java.util.Map;
import java.util.Objects;

/**
 * 人工审批 evidence 引用集合。
 *
 * <p>这里只保存 ref 或摘要，不保存 raw provider response、raw prompt、credential 或其它敏感材料。
 */
public record ApprovalEvidenceRefs(Map<String, Object> refs) {

    /**
     * 校验 evidence refs 不含疑似敏感材料。
     */
    public ApprovalEvidenceRefs {
        refs = Map.copyOf(Objects.requireNonNull(refs, "refs"));
        ApprovalContentGuard.rejectSecretLike("evidenceRefsJson", refs);
    }
}
