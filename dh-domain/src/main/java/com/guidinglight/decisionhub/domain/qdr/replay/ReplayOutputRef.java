package com.guidinglight.decisionhub.domain.qdr.replay;

/**
 * QDR replay / evaluation 输出引用。
 *
 * <p>该合同只描述 read-only 输出位置或 checksum，不保存原始 provider 响应，不表示交易信号。
 */
public record ReplayOutputRef(String refType, String refId, String contentHash) {

    /**
     * 规范化输出引用字段，缺失字段由 usecase 合同校验 fail-closed。
     */
    public ReplayOutputRef {
        refType = trimToNull(refType);
        refId = trimToNull(refId);
        contentHash = trimToNull(contentHash);
    }

    private static String trimToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
