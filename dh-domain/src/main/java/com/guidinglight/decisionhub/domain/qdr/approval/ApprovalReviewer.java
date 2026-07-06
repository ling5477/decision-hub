package com.guidinglight.decisionhub.domain.qdr.approval;

/**
 * Human Approval Packet 审查人信息。
 *
 * <p>未决审批包允许 reviewer 为空；转入 APPROVED / REJECTED / NEEDS_REVIEW 时必须有 reviewerId，
 * 防止 reviewer 缺失时被自动通过。
 */
public record ApprovalReviewer(String reviewerId, String reviewerNote) {

    /**
     * 校验 reviewer note 不含疑似敏感材料。
     */
    public ApprovalReviewer {
        if (reviewerId != null && reviewerId.isBlank()) {
            throw new IllegalArgumentException("reviewerId must not be blank when present");
        }
        ApprovalContentGuard.rejectSecretLike("reviewerNote", reviewerNote);
    }

    /**
     * 空 reviewer，用于未决或系统过期状态。
     *
     * @return 空 reviewer。
     */
    public static ApprovalReviewer none() {
        return new ApprovalReviewer(null, null);
    }

    /**
     * 是否包含有效 reviewerId。
     *
     * @return true 表示有人审查。
     */
    public boolean present() {
        return reviewerId != null && !reviewerId.isBlank();
    }
}
