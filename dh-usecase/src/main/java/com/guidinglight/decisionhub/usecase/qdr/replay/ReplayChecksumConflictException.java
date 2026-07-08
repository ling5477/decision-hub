package com.guidinglight.decisionhub.usecase.qdr.replay;

/**
 * QDR replay / evaluation checksum 冲突异常。
 *
 * <p>同一 tenant 下重复 `case_id` 或 `evaluation_id` 只能在 checksum 完全一致时幂等返回；checksum 不一致
 * 表示 baseline 被覆盖或污染风险，必须 fail-closed。
 */
public final class ReplayChecksumConflictException extends ReplayPersistenceException {

    /**
     * 创建 checksum 冲突异常。
     */
    public ReplayChecksumConflictException() {
        super("QDR replay/evaluation checksum conflict");
    }
}
