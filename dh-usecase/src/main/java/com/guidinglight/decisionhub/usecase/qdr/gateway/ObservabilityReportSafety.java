package com.guidinglight.decisionhub.usecase.qdr.gateway;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Stage-QDR-5 B4 report 专用安全校验工具。
 *
 * <p>B4 只整理 B1/B2/B3 safe evidence，不接真实 provider、不发 HTTP、不读取凭证、不访问 NQ、不输出交易信号。
 * 该工具集中拒绝 raw material、credential-like material、执行型交易词和 runtime enablement 语义，确保 report
 * 字段只保留 safe refs、enum、hash 与 redacted summary。
 */
final class ObservabilityReportSafety {

    private ObservabilityReportSafety() {
    }

    /**
     * 校验必填 safe text。
     *
     * @param value 字段值。
     * @param field 字段名。
     * @return trim 后的 safe text。
     */
    static String requireSafeText(final String value, final String field) {
        final String checked = QdrPersistenceSafety.requireSafeText(value, field);
        rejectForbiddenValue(field, checked);
        return checked;
    }

    /**
     * 校验 64 位 SHA-256 hex，并拒绝任何附带 unsafe marker 的值。
     *
     * @param value hash 字段值。
     * @param field 字段名。
     * @return lowercase hash。
     */
    static String requireSha256Hex(final String value, final String field) {
        final String checked = QdrPersistenceSafety.requireSha256Hex(value, field);
        rejectForbiddenValue(field, checked);
        return checked;
    }

    /**
     * 校验安全时间戳。
     *
     * @param value 时间字段。
     * @param field 字段名。
     * @return 原时间。
     */
    static Instant requireInstant(final Instant value, final String field) {
        return QdrPersistenceSafety.requireInstant(value, field);
    }

    /**
     * 校验 safe ref 列表，并复制为不可变列表。
     *
     * @param values safe ref 列表。
     * @param field  字段名。
     * @return 不可变 safe ref 列表。
     */
    static List<String> requireSafeTexts(final List<String> values, final String field) {
        return Objects.requireNonNullElse(values, List.<String>of()).stream()
                .map(value -> requireSafeText(value, field))
                .toList();
    }

    private static void rejectForbiddenValue(final String field, final String value) {
        final String normalized = normalize(value);
        if (normalized.contains("rawprompt")
                || normalized.contains("prompttext")
                || normalized.contains("rawproviderresponse")
                || normalized.contains("rawrequestpayload")
                || normalized.contains("rawresponsepayload")
                || normalized.contains("providerraw")) {
            throw new IllegalArgumentException(field + " rejected by raw material boundary");
        }
        if (normalized.contains("mutatenqstate")
                || normalized.contains("placeorder")
                || normalized.contains("cancelorder")) {
            throw new IllegalArgumentException(field + " rejected by NQ mutation boundary");
        }
        if (normalized.contains("enablerealprovider")
                || normalized.contains("realproviderenabled")
                || normalized.contains("enablerealhttp")
                || normalized.contains("realhttpenabled")
                || normalized.contains("enablelive")
                || normalized.contains("liveenabled")
                || normalized.contains("livepermission")
                || normalized.contains("providerauthorization")
                || normalized.contains("tradingpermission")
                || normalized.contains("tradingsignal")
                || normalized.contains("nqexecutionallowed")) {
            throw new IllegalArgumentException(field + " rejected by B4 report boundary");
        }
        if (PromptModelSafetyRules.containsExecutableTradingInstruction(value)
                || PromptModelSafetyRules.containsSecretLikeMaterial(value)) {
            throw new IllegalArgumentException(field + " rejected by QDR report redaction boundary");
        }
    }

    private static String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "");
    }
}
