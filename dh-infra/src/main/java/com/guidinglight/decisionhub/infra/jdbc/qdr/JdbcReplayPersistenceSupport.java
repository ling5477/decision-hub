package com.guidinglight.decisionhub.infra.jdbc.qdr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayOutputRef;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceGuard;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * QDR replay persistence JDBC 支撑函数。
 *
 * <p>集中处理 JSONB 安全序列化、tenant-bound 行映射和基础类型转换；不访问外部服务，不执行 replay，
 * 不接 provider/HTTP/Agent/LangGraph/NQ/LIVE。
 */
final class JdbcReplayPersistenceSupport {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private JdbcReplayPersistenceSupport() {
    }

    static String writeJson(
            final ObjectMapper objectMapper, final Object value, final String fieldName) {
        final Object safe = objectMapper.convertValue(value, Object.class);
        ReplayPersistenceGuard.rejectUnsafeJson(safe, fieldName);
        try {
            return objectMapper.writeValueAsString(safe);
        } catch (final JsonProcessingException error) {
            throw new ReplayPersistenceException("serialize " + fieldName + " failed", error);
        }
    }

    static String writeJsonOrNull(
            final ObjectMapper objectMapper, final Object value, final String fieldName) {
        return value == null ? null : writeJson(objectMapper, value, fieldName);
    }

    static Map<String, Object> readMap(
            final ObjectMapper objectMapper, final Object value, final String fieldName) {
        if (value instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> cast = (Map<String, Object>) map;
            ReplayPersistenceGuard.rejectUnsafeJson(cast, fieldName);
            return cast;
        }
        try {
            final Map<String, Object> parsed =
                    objectMapper.readValue(Objects.requireNonNull(value, fieldName).toString(), MAP_TYPE);
            ReplayPersistenceGuard.rejectUnsafeJson(parsed, fieldName);
            return parsed;
        } catch (final JsonProcessingException error) {
            throw new ReplayPersistenceException("parse " + fieldName + " failed", error);
        }
    }

    static ReplayInputRef readInputRef(
            final ObjectMapper objectMapper, final Object value, final String fieldName) {
        final Map<String, Object> payload = readMap(objectMapper, value, fieldName);
        return ReplayPersistenceGuard.requireInputRef(
                new ReplayInputRef(
                        text(payload, "refType"),
                        text(payload, "refId"),
                        text(payload, "contentHash")));
    }

    static ReplayOutputRef readOutputRef(
            final ObjectMapper objectMapper, final Object value, final String fieldName) {
        final Map<String, Object> payload = readMap(objectMapper, value, fieldName);
        return ReplayPersistenceGuard.requireOutputRef(
                new ReplayOutputRef(
                        text(payload, "refType"),
                        text(payload, "refId"),
                        text(payload, "contentHash")));
    }

    static ReplayOutputRef readOutputRefOrNull(
            final ObjectMapper objectMapper, final Object value, final String fieldName) {
        return value == null ? null : readOutputRef(objectMapper, value, fieldName);
    }

    static ExpectedDecisionSummary readSummary(
            final ObjectMapper objectMapper,
            final Object summaryValue,
            final Object evidenceRefsValue,
            final Object forbiddenActionsValue,
            final String fieldName) {
        final Map<String, Object> summary = readMap(objectMapper, summaryValue, fieldName);
        return ReplayPersistenceGuard.requireSummary(
                new ExpectedDecisionSummary(
                        text(summary, "decisionType"),
                        text(summary, "actionLabel"),
                        text(summary, "confidenceBand"),
                        RiskLevel.valueOf(text(summary, "riskLevel")),
                        stringList(objectMapper, evidenceRefsValue, fieldName + ".requiredEvidenceRefs"),
                        stringList(objectMapper, forbiddenActionsValue, fieldName + ".forbiddenActions")));
    }

    static ExpectedDecisionSummary readSummaryOrNull(
            final ObjectMapper objectMapper,
            final Object summaryValue,
            final Object evidenceRefsValue,
            final Object forbiddenActionsValue,
            final String fieldName) {
        if (summaryValue == null) {
            return null;
        }
        return readSummary(objectMapper, summaryValue, evidenceRefsValue, forbiddenActionsValue, fieldName);
    }

    static String text(final Map<String, Object> row, final String key) {
        final String value = optionalText(row, key);
        if (value == null) {
            throw new ReplayPersistenceException(key + " must not be null");
        }
        return value;
    }

    static String optionalText(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        final String checked = value.toString().trim();
        return checked.isEmpty() ? null : checked;
    }

    static UUID uuid(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    static UUID uuidOrNull(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    static Timestamp timestamp(final Instant value) {
        return Timestamp.from(Objects.requireNonNull(value, "value"));
    }

    static Instant instant(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof Instant instant) {
            return instant;
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof OffsetDateTime dateTime) {
            return dateTime.toInstant();
        }
        if (value instanceof LocalDateTime dateTime) {
            return dateTime.toInstant(ZoneOffset.UTC);
        }
        return Instant.parse(value.toString());
    }

    static <E extends Enum<E>> E enumValue(
            final Map<String, Object> row, final String key, final Class<E> enumType) {
        return Enum.valueOf(enumType, text(row, key));
    }

    static <E extends Enum<E>> E enumValueOrNull(
            final Map<String, Object> row, final String key, final Class<E> enumType) {
        final String value = optionalText(row, key);
        return value == null ? null : Enum.valueOf(enumType, value);
    }

    private static List<String> stringList(
            final ObjectMapper objectMapper, final Object value, final String fieldName) {
        final Object safe = value == null ? List.of() : objectMapper.convertValue(value, Object.class);
        ReplayPersistenceGuard.rejectUnsafeJson(safe, fieldName);
        if (safe instanceof List<?> list) {
            return list.stream()
                    .map(item -> Objects.requireNonNull(item, fieldName).toString())
                    .toList();
        }
        try {
            final List<?> parsed = objectMapper.readValue(value.toString(), List.class);
            ReplayPersistenceGuard.rejectUnsafeJson(parsed, fieldName);
            return parsed.stream()
                    .map(item -> Objects.requireNonNull(item, fieldName).toString())
                    .toList();
        } catch (final JsonProcessingException error) {
            throw new ReplayPersistenceException("parse " + fieldName + " failed", error);
        }
    }
}
