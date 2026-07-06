package com.guidinglight.decisionhub.usecase.qdr.approval;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * B4 approval command 输入校验工具。
 *
 * <p>该工具只做本地 fail-closed 校验；命中 executable action、raw material 或 secret-like marker 时拒绝写入，
 * 防止 response 或 audit payload 携带交易指令、凭证或 provider 原始内容。
 */
final class ApprovalCommandValidation {

    private static final Pattern NON_WORD = Pattern.compile("[^A-Z0-9]+");

    private static final Set<String> FORBIDDEN_WORDS =
            Set.of(
                    "BUY",
                    "SELL",
                    "PLACE_ORDER",
                    "CANCEL_ORDER",
                    "MARKET_ORDER",
                    "LIMIT_ORDER",
                    "EXECUTE_ORDER",
                    "MUTATE_NQ_STATE",
                    "RAW_PROMPT",
                    "RAW_RESPONSE",
                    "RAW_PROVIDER_RESPONSE",
                    "UNREDACTED",
                    "SECRET_INCLUDED",
                    "CREDENTIAL",
                    "APIKEY",
                    "API_KEY",
                    "APISECRET",
                    "API_SECRET",
                    "PASSPHRASE",
                    "TOKEN",
                    "COOKIE");

    private ApprovalCommandValidation() {
        // utility class
    }

    static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        rejectUnsafeMaterial(checked, field);
        return checked;
    }

    static String optionalSafeText(final String value, final String field) {
        if (value == null) {
            return null;
        }
        final String checked = value.trim();
        if (checked.isEmpty()) {
            return null;
        }
        rejectUnsafeMaterial(checked, field);
        return checked;
    }

    static String requireUuidText(final String value, final String field) {
        final String checked = requireText(value, field);
        try {
            UUID.fromString(checked);
            return checked;
        } catch (final IllegalArgumentException error) {
            throw new IllegalArgumentException(field + " must be UUID", error);
        }
    }

    static Map<String, Object> safeMap(final Map<String, Object> value, final String field) {
        if (value == null) {
            return Map.of();
        }
        rejectUnsafeMaterial(value, field);
        return Map.copyOf(value);
    }

    static void rejectUnsafeMaterial(final Object value, final String field) {
        if (value == null) {
            return;
        }
        final String normalized =
                NON_WORD.matcher(value.toString().toUpperCase(Locale.ROOT)).replaceAll("_");
        for (String forbidden : FORBIDDEN_WORDS) {
            if (normalized.contains(forbidden)) {
                throw new IllegalArgumentException(field + " must not contain forbidden material");
            }
        }
    }
}
