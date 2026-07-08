package com.guidinglight.decisionhub.usecase.qdr.replay;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * B3 mock gateway regression 的本地安全工具。
 *
 * <p>该工具只做字符串和 ref 校验，不访问数据库、不调用 provider、不发 HTTP、不接 NQ、
 * Agent 或 LangGraph。它补足 B2 guard 对 raw material 关键字正文的拒绝，避免调用方把
 * raw prompt 或 raw provider response 包进 redacted summary / evidence ref。
 */
final class QdrRegressionSafety {

    private static final Pattern RAW_MATERIAL =
            Pattern.compile(
                    "(?i)(rawPrompt|raw_prompt|promptText|rawProviderResponse|"
                            + "raw_provider_response|providerRaw)");

    private QdrRegressionSafety() {
    }

    /**
     * 校验必填安全文本。
     *
     * @param value 字段值。
     * @param field 字段名。
     * @return trim 后文本。
     */
    static String requireSafeText(final String value, final String field) {
        final String checked = ReplayPersistenceGuard.requireSafeText(value, field);
        rejectRawMaterialMention(checked, field);
        return checked;
    }

    /**
     * 校验可空安全文本。
     *
     * @param value 字段值。
     * @param field 字段名。
     * @return null 或 trim 后文本。
     */
    static String optionalSafeText(final String value, final String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireSafeText(value, field);
    }

    /**
     * 校验 SHA-256 hash。
     *
     * @param value hash 字段值。
     * @param field 字段名。
     * @return lowercase hash。
     */
    static String requireSha256Hex(final String value, final String field) {
        return ReplayPersistenceGuard.requireSha256Hex(value, field);
    }

    /**
     * 校验 tenant-bound identity 字段。
     *
     * @param tenantId tenant ID。
     * @return trim 后 tenant ID。
     */
    static String requireTenantId(final String tenantId) {
        return ReplayPersistenceGuard.requireTenantId(tenantId);
    }

    /**
     * 用于 finding message 的固定安全文本校验。
     *
     * @param value finding message。
     * @return trim 后 message。
     */
    static String requireFindingMessage(final String value) {
        final String checked = Objects.requireNonNull(value, "findingMessage").trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException("findingMessage must not be blank");
        }
        rejectRawMaterialMention(checked, "findingMessage");
        if (checked.toUpperCase(Locale.ROOT).contains("BUY")
                || checked.toUpperCase(Locale.ROOT).contains("SELL")) {
            throw new IllegalArgumentException("findingMessage rejected by trading-term guard");
        }
        return checked;
    }

    private static void rejectRawMaterialMention(final String value, final String field) {
        if (value != null && RAW_MATERIAL.matcher(value).find()) {
            throw new IllegalArgumentException(field + " rejected by raw-material guard");
        }
    }
}
