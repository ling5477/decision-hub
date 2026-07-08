package com.guidinglight.decisionhub.usecase.qdr.replay;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;
import com.guidinglight.decisionhub.usecase.qdr.model.QdrPersistenceSafety;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * QDR replay persistence redaction 与交易语义守卫。
 *
 * <p>该守卫只做本地字段、DTO 属性和 JSON key/value 校验，不访问数据库、不调用 provider、不触发 HTTP、
 * Agent、LangGraph、NQ 或交易路径。`forbiddenActions` 中允许出现交易禁止词，但任何 action label、
 * allowed action、summary 正文或 finding 说明都不得把这些词保存为可执行动作。
 */
public final class ReplayPersistenceGuard {

    private static final List<String> FORBIDDEN_KEYS = List.of(
            "rawprompt",
            "raw_prompt",
            "prompttext",
            "rawproviderresponse",
            "raw_provider_response",
            "providerraw",
            "credential",
            "apikey",
            "apisecret",
            "passphrase",
            "token",
            "cookie",
            "secret");

    private static final List<String> FORBIDDEN_ACTION_VALUES = List.of(
            "BUY",
            "SELL",
            "MARKET_ORDER",
            "PLACE_ORDER",
            "CANCEL_ORDER",
            "MUTATE_NQ_STATE");

    private ReplayPersistenceGuard() {
    }

    /**
     * 校验 tenant ID。
     *
     * @param tenantId tenant ID。
     * @return trim 后 tenant ID。
     */
    public static String requireTenantId(final String tenantId) {
        return QdrPersistenceSafety.requireText(tenantId, "tenantId");
    }

    /**
     * 校验必填脱敏文本。
     *
     * @param value 字段值。
     * @param field 字段名。
     * @return trim 后文本。
     */
    public static String requireSafeText(final String value, final String field) {
        rejectForbiddenPropertyName(field);
        return QdrPersistenceSafety.requireSafeText(value, field);
    }

    /**
     * 校验可空脱敏文本。
     *
     * @param value 字段值。
     * @param field 字段名。
     * @return null 或 trim 后文本。
     */
    public static String optionalSafeText(final String value, final String field) {
        rejectForbiddenPropertyName(field);
        return QdrPersistenceSafety.optionalSafeText(value, field);
    }

    /**
     * 校验可空来源字段。
     *
     * @param sourceDecisionId 来源 decision ID。
     * @param sourceRequestId  来源 request ID。
     */
    public static void requireSourceRef(final String sourceDecisionId, final String sourceRequestId) {
        if ((sourceDecisionId == null || sourceDecisionId.isBlank())
                && (sourceRequestId == null || sourceRequestId.isBlank())) {
            throw new IllegalArgumentException("sourceDecisionId or sourceRequestId is required");
        }
    }

    /**
     * 校验 UUID。
     *
     * @param value UUID 字段。
     * @param field 字段名。
     * @return 原 UUID。
     */
    public static UUID requireUuid(final UUID value, final String field) {
        rejectForbiddenPropertyName(field);
        return QdrPersistenceSafety.requireUuid(value, field);
    }

    /**
     * 校验可空 UUID。
     *
     * @param value UUID 字段。
     * @param field 字段名。
     * @return 原 UUID 或 null。
     */
    public static UUID optionalUuid(final UUID value, final String field) {
        rejectForbiddenPropertyName(field);
        return value;
    }

    /**
     * 校验 SHA-256 hash。
     *
     * @param value hash 字段。
     * @param field 字段名。
     * @return lowercase hash。
     */
    public static String requireSha256Hex(final String value, final String field) {
        rejectForbiddenPropertyName(field);
        return QdrPersistenceSafety.requireSha256Hex(value, field);
    }

    /**
     * 校验可空 SHA-256 hash。
     *
     * @param value hash 字段。
     * @param field 字段名。
     * @return lowercase hash 或 null。
     */
    public static String optionalSha256Hex(final String value, final String field) {
        rejectForbiddenPropertyName(field);
        return QdrPersistenceSafety.optionalSha256Hex(value, field);
    }

    /**
     * 校验时间字段。
     *
     * @param value 时间字段。
     * @param field 字段名。
     * @return 原时间。
     */
    public static Instant requireInstant(final Instant value, final String field) {
        rejectForbiddenPropertyName(field);
        return QdrPersistenceSafety.requireInstant(value, field);
    }

    /**
     * 校验 input ref。
     *
     * @param inputRef input ref。
     * @return 原 input ref。
     */
    public static ReplayInputRef requireInputRef(final ReplayInputRef inputRef) {
        final ReplayInputRef checked = Objects.requireNonNull(inputRef, "inputRef");
        requireSafeText(checked.refType(), "inputRef.refType");
        requireSafeText(checked.refId(), "inputRef.refId");
        requireSha256Hex(checked.contentHash(), "inputRef.contentHash");
        return checked;
    }

    /**
     * 校验 output ref。
     *
     * @param outputRef output ref。
     * @return 原 output ref。
     */
    public static ReplayOutputRef requireOutputRef(final ReplayOutputRef outputRef) {
        final ReplayOutputRef checked = Objects.requireNonNull(outputRef, "outputRef");
        requireSafeText(checked.refType(), "outputRef.refType");
        requireSafeText(checked.refId(), "outputRef.refId");
        requireSha256Hex(checked.contentHash(), "outputRef.contentHash");
        return checked;
    }

    /**
     * 校验可空 output ref。
     *
     * @param outputRef output ref。
     * @return 原 output ref 或 null。
     */
    public static ReplayOutputRef optionalOutputRef(final ReplayOutputRef outputRef) {
        return outputRef == null ? null : requireOutputRef(outputRef);
    }

    /**
     * 校验结构化 summary。
     *
     * @param summary summary。
     * @return 原 summary。
     */
    public static ExpectedDecisionSummary requireSummary(final ExpectedDecisionSummary summary) {
        final ExpectedDecisionSummary checked = Objects.requireNonNull(summary, "summary");
        requireSafeText(checked.decisionType(), "summary.decisionType");
        requireActionLabel(checked.actionLabel());
        requireSafeText(checked.confidenceBand(), "summary.confidenceBand");
        Objects.requireNonNull(checked.riskLevel(), "summary.riskLevel");
        if (checked.requiredEvidenceRefs().isEmpty()) {
            throw new IllegalArgumentException("requiredEvidenceRefs must not be empty");
        }
        checked.requiredEvidenceRefs()
                .forEach(value -> requireSafeText(value, "summary.requiredEvidenceRefs"));
        checked.forbiddenActions().forEach(ReplayPersistenceGuard::requireForbiddenActionLabel);
        return checked;
    }

    /**
     * 校验可空 summary。
     *
     * @param summary summary。
     * @return 原 summary 或 null。
     */
    public static ExpectedDecisionSummary optionalSummary(final ExpectedDecisionSummary summary) {
        return summary == null ? null : requireSummary(summary);
    }

    /**
     * 校验 action label 不含可执行交易动作。
     *
     * @param value action label。
     * @return trim 后 action label。
     */
    public static String requireActionLabel(final String value) {
        final String checked = QdrPersistenceSafety.requireText(value, "actionLabel");
        if (FORBIDDEN_ACTION_VALUES.contains(checked.toUpperCase(Locale.ROOT))
                || PromptModelSafetyRules.containsExecutableTradingInstruction(checked)) {
            throw new IllegalArgumentException("actionLabel rejected by trading-term guard");
        }
        return checked;
    }

    /**
     * 校验 forbiddenActions 中的条目。这里允许交易禁止词出现，但只表示禁止项。
     *
     * @param value forbidden action label。
     * @return trim 后 forbidden action label。
     */
    public static String requireForbiddenActionLabel(final String value) {
        final String checked = QdrPersistenceSafety.requireText(value, "forbiddenActions");
        if (PromptModelSafetyRules.containsSecretLikeMaterial(checked)) {
            throw new IllegalArgumentException("forbiddenActions rejected by redaction guard");
        }
        return checked;
    }

    /**
     * 递归校验 JSONB 候选对象，不允许敏感 key 或可执行交易正文。
     *
     * @param value JSONB 候选对象。
     * @param path  字段路径。
     */
    public static void rejectUnsafeJson(final Object value, final String path) {
        rejectForbiddenPropertyName(path);
        if (value == null) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                final String key = String.valueOf(entry.getKey());
                rejectForbiddenPropertyName(key);
                rejectUnsafeJson(entry.getValue(), path + "." + key);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                rejectUnsafeJson(item, path);
            }
            return;
        }
        if (value instanceof String text) {
            rejectUnsafeJsonText(text, path);
        }
    }

    private static void rejectUnsafeJsonText(final String text, final String path) {
        if (text == null || text.isBlank()) {
            return;
        }
        if (PromptModelSafetyRules.containsSecretLikeMaterial(text)) {
            throw new IllegalArgumentException(path + " rejected by redaction guard");
        }
        final String normalizedPath = path.toLowerCase(Locale.ROOT);
        if (!normalizedPath.contains("forbiddenactions")
                && PromptModelSafetyRules.containsExecutableTradingInstruction(text)) {
            throw new IllegalArgumentException(path + " rejected by trading-term guard");
        }
    }

    private static void rejectForbiddenPropertyName(final String field) {
        final String lower = field.toLowerCase(Locale.ROOT).replace("-", "").replace("_", "");
        for (String forbidden : FORBIDDEN_KEYS) {
            if (lower.contains(forbidden.replace("_", ""))) {
                throw new IllegalArgumentException(field + " rejected by redaction guard");
            }
        }
    }
}
