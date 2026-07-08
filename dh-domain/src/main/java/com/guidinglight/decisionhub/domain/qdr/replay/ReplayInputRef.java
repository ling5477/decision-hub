package com.guidinglight.decisionhub.domain.qdr.replay;

/**
 * QDR replay 输入引用。
 *
 * <p>该合同只保存可追踪引用、hash 或脱敏位置，不承载 prompt 正文、provider 原始响应或任何凭证材料。
 */
public record ReplayInputRef(String refType, String refId, String contentHash) {

    /**
     * 规范化 replay 输入引用字段，保留空值给 usecase fail-closed 校验处理。
     */
    public ReplayInputRef {
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
