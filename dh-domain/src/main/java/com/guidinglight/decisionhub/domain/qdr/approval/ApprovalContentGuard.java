package com.guidinglight.decisionhub.domain.qdr.approval;

import java.util.Locale;

/**
 * Human Approval Packet 文本与 JSON 内容安全校验。
 *
 * <p>该 guard 只做本地 fail-closed 检查，不解析凭证、不输出原文；命中疑似敏感材料时直接拒绝写入审批包。
 */
final class ApprovalContentGuard {

    private static final String[] SECRET_MARKERS = {
        "credential", "token", "cookie", "apikey", "apisecret", "passphrase", "privatekey", "mnemonic"
    };

    private ApprovalContentGuard() {
    }

    static void rejectSecretLike(final String fieldName, final Object value) {
        if (value == null) {
            return;
        }
        final String normalized =
                value.toString()
                        .toLowerCase(Locale.ROOT)
                        .replace("_", "")
                        .replace("-", "")
                        .replace(" ", "");
        for (String marker : SECRET_MARKERS) {
            if (normalized.contains(marker)) {
                throw new IllegalArgumentException(fieldName + " contains secret-like material");
            }
        }
    }

    static String requiredText(final String value, final String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }
}
