package com.guidinglight.decisionhub.domain.qdr.replay;

import java.util.Objects;

/**
 * QDR regression finding。
 *
 * <p>finding 只记录结构化错误码、严重度和脱敏 evidence ref，不记录 raw prompt 或 raw provider response。
 */
public record RegressionFinding(
        String code,
        RegressionSeverity severity,
        String message,
        String evidenceRef) {

    /**
     * 校验 regression finding 的结构化字段。
     */
    public RegressionFinding {
        code = requireText(code, "code");
        severity = Objects.requireNonNull(severity, "severity");
        message = requireText(message, "message");
        evidenceRef = trimToNull(evidenceRef);
    }

    private static String requireText(final String value, final String field) {
        final String checked = Objects.requireNonNull(value, field).trim();
        if (checked.isEmpty()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return checked;
    }

    private static String trimToNull(final String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
