package com.guidinglight.decisionhub.usecase.qdr.readmodel;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * stage-qdr-2 B1 read model 的包内校验工具。
 *
 * <p>该工具只服务 DTO / query contract 构造期 fail-closed 校验，不做查询、不写库、不触发 replay、
 * provider、HTTP 或 approval write。
 */
final class ReadModelValidation {

    private static final Pattern NON_WORD = Pattern.compile("[^A-Z0-9]+");

    private static final Set<String> EXECUTABLE_ACTION_WORDS =
            Set.of("BUY", "SELL", "PLACE_ORDER", "CANCEL_ORDER");

    private static final Set<String> RAW_MATERIAL_WORDS =
            Set.of(
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

    private ReadModelValidation() {
        // utility class
    }

    static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }

    static String optionalText(final String value, final String field) {
        if (value == null) {
            return null;
        }
        return requireText(value, field);
    }

    static Integer requirePositive(final Integer value, final String field) {
        if (value == null || value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }

    static Long optionalNonNegative(final Long value, final String field) {
        if (value != null && value < 0) {
            throw new IllegalArgumentException(field + " must not be negative");
        }
        return value;
    }

    static String optionalSummary(final String value, final String field) {
        if (value == null) {
            return null;
        }
        final String checked = requireText(value, field);
        rejectUnsafeMaterial(checked, field);
        return checked;
    }

    static List<String> optionalRefs(final List<String> values, final String field) {
        if (values == null) {
            return List.of();
        }
        final List<String> checked = new ArrayList<>(values.size());
        for (int index = 0; index < values.size(); index++) {
            final String item = requireText(values.get(index), field + "[" + index + "]");
            rejectUnsafeMaterial(item, field + "[" + index + "]");
            checked.add(item);
        }
        return List.copyOf(checked);
    }

    static void rejectUnsafeMaterial(final String value, final String field) {
        final String normalized = normalized(value);
        for (final String forbidden : EXECUTABLE_ACTION_WORDS) {
            if (normalized.contains(forbidden)) {
                throw new IllegalArgumentException(
                        field + " must not contain executable action word: " + forbidden);
            }
        }
        for (final String forbidden : RAW_MATERIAL_WORDS) {
            if (normalized.contains(forbidden)) {
                throw new IllegalArgumentException(field + " must not contain raw material: " + forbidden);
            }
        }
    }

    private static String normalized(final String value) {
        return NON_WORD.matcher(value.toUpperCase(Locale.ROOT)).replaceAll("_");
    }
}
