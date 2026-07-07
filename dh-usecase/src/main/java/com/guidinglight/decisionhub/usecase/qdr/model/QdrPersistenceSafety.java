package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * QDR model persistence command / record safety helpers。
 *
 * <p>该工具只做本地字段校验与脱敏边界检查，不访问数据库、不调用 provider、不读取凭证、不触发 HTTP。
 */
public final class QdrPersistenceSafety {

    private static final Pattern SHA_256_HEX = Pattern.compile("[0-9a-f]{64}");

    private QdrPersistenceSafety() {
    }

    /**
     * 校验必填文本。
     *
     * @param value 字段值。
     * @param field 字段名。
     * @return trim 后文本。
     */
    public static String requireText(final String value, final String field) {
        return PromptModelSafetyRules.requireText(value, field);
    }

    /**
     * 校验脱敏文本，不允许疑似凭证材料或可执行交易指令进入持久化 contract。
     *
     * @param value 字段值。
     * @param field 字段名。
     * @return trim 后文本。
     */
    public static String requireSafeText(final String value, final String field) {
        final String checked = requireText(value, field);
        if (PromptModelSafetyRules.containsSecretLikeMaterial(checked)
                || PromptModelSafetyRules.containsExecutableTradingInstruction(checked)) {
            throw new IllegalArgumentException(field + " rejected by QDR persistence redaction boundary");
        }
        return checked;
    }

    /**
     * 校验可空脱敏文本。
     *
     * @param value 字段值。
     * @param field 字段名。
     * @return null 或 trim 后文本。
     */
    public static String optionalSafeText(final String value, final String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireSafeText(value, field);
    }

    /**
     * 校验 64 位 SHA-256 hex。
     *
     * @param value hash 字段值。
     * @param field 字段名。
     * @return lowercase hash。
     */
    public static String requireSha256Hex(final String value, final String field) {
        final String checked = requireText(value, field).toLowerCase();
        if (!SHA_256_HEX.matcher(checked).matches()) {
            throw new IllegalArgumentException(field + " must be SHA-256 hex");
        }
        return checked;
    }

    /**
     * 校验可空 SHA-256 hex。
     *
     * @param value hash 字段值。
     * @param field 字段名。
     * @return null 或 lowercase hash。
     */
    public static String optionalSha256Hex(final String value, final String field) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return requireSha256Hex(value, field);
    }

    /**
     * 校验 UUID。
     *
     * @param value UUID 字段值。
     * @param field 字段名。
     * @return 原 UUID。
     */
    public static UUID requireUuid(final UUID value, final String field) {
        return Objects.requireNonNull(value, field);
    }

    /**
     * 校验创建时间。
     *
     * @param value 时间字段。
     * @param field 字段名。
     * @return 原时间。
     */
    public static Instant requireInstant(final Instant value, final String field) {
        return Objects.requireNonNull(value, field);
    }

    /**
     * 校验非负计数。
     *
     * @param value 数值。
     * @param field 字段名。
     * @return 原数值。
     */
    public static int requireNonNegative(final int value, final String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative");
        }
        return value;
    }

    /**
     * 校验正数计数。
     *
     * @param value 数值。
     * @param field 字段名。
     * @return 原数值。
     */
    public static int requirePositive(final int value, final String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
        return value;
    }
}
