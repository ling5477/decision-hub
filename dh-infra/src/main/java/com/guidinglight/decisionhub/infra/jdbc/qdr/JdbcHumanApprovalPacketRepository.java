package com.guidinglight.decisionhub.infra.jdbc.qdr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalChecklist;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalEvidenceRefs;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalKey;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalReviewer;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalType;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacketId;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketDuplicateException;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketRepository;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Human Approval Packet JDBC repository adapter。
 *
 * <p>本 adapter 只访问 DH 自身 `human_approval_packet` 表；所有查询和更新都带 `tenant_id`，
 * 状态更新额外带当前 `approval_status` 防止无条件覆盖。不访问 NQ DB、不调用外部 HTTP、不接 provider、
 * 不触发交易链路。
 */
public final class JdbcHumanApprovalPacketRepository implements HumanApprovalPacketRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final String COLUMNS =
            "id, decision_run_id, tenant_id, trace_id, request_id, approval_key,"
                    + " approval_type, approval_status, risk_level, decision_action,"
                    + " confidence_score, summary, checklist_json::text as checklist_json,"
                    + " evidence_refs_json::text as evidence_refs_json, reviewer_id, reviewer_note,"
                    + " decided_at, created_at, updated_at";

    private static final String INSERT_PACKET =
            "insert into human_approval_packet"
                    + " (id, decision_run_id, tenant_id, trace_id, request_id, approval_key,"
                    + " approval_type, approval_status, risk_level, decision_action, confidence_score,"
                    + " summary, checklist_json, evidence_refs_json, reviewer_id, reviewer_note,"
                    + " decided_at, created_at, updated_at)"
                    + " values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb),"
                    + " CAST(? AS jsonb), ?, ?, ?, ?, ?)";

    private static final String SELECT_BY_TENANT_APPROVAL_KEY =
            "select " + COLUMNS + " from human_approval_packet"
                    + " where tenant_id = ? and approval_key = ? limit 1";

    private static final String SELECT_BY_TENANT_ID =
            "select " + COLUMNS + " from human_approval_packet"
                    + " where tenant_id = ? and id = ? limit 1";

    private static final String SELECT_BY_TENANT_DECISION_RUN =
            "select " + COLUMNS + " from human_approval_packet"
                    + " where tenant_id = ? and decision_run_id = ? order by created_at asc";

    private static final String UPDATE_STATUS =
            "update human_approval_packet set approval_status = ?, reviewer_id = ?,"
                    + " reviewer_note = ?, decided_at = ?, updated_at = ?"
                    + " where tenant_id = ? and id = ? and approval_status = ?";

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建 JDBC adapter。
     *
     * @param jdbcTemplate DH datasource 对应的 JDBC template。
     * @param objectMapper JSON mapper。
     */
    public JdbcHumanApprovalPacketRepository(
            final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
    }

    @Override
    public HumanApprovalPacket save(final HumanApprovalPacket packet) {
        final HumanApprovalPacket checked = Objects.requireNonNull(packet, "packet");
        try {
            jdbcTemplate.update(
                    INSERT_PACKET,
                    checked.id().value(),
                    checked.decisionRunId(),
                    checked.tenantId(),
                    checked.traceId(),
                    checked.requestId(),
                    checked.approvalKey().value(),
                    checked.approvalType().name(),
                    checked.approvalStatus().name(),
                    checked.riskLevel().name(),
                    checked.decisionAction().name(),
                    checked.confidenceScore(),
                    checked.summary(),
                    writeJson(checked.checklist().items(), "checklistJson"),
                    writeJsonOrNull(
                            checked.evidenceRefs() == null ? null : checked.evidenceRefs().refs(),
                            "evidenceRefsJson"),
                    checked.reviewer().reviewerId(),
                    checked.reviewer().reviewerNote(),
                    timestampOrNull(checked.decidedAt()),
                    timestamp(checked.createdAt()),
                    timestamp(checked.updatedAt()));
            return checked;
        } catch (final DuplicateKeyException error) {
            throw new HumanApprovalPacketDuplicateException(
                    "human approval packet duplicate approval key", error);
        } catch (final DataAccessException error) {
            throw new HumanApprovalPacketPersistenceException(
                    "save human approval packet failed", error);
        }
    }

    @Override
    public Optional<HumanApprovalPacket> findByTenantAndApprovalKey(
            final String tenantId, final ApprovalKey approvalKey) {
        try {
            return jdbcTemplate
                    .queryForList(
                            SELECT_BY_TENANT_APPROVAL_KEY,
                            checkedTenant(tenantId),
                            Objects.requireNonNull(approvalKey, "approvalKey").value())
                    .stream()
                    .findFirst()
                    .map(this::mapPacket);
        } catch (final DataAccessException error) {
            throw new HumanApprovalPacketPersistenceException(
                    "find human approval packet by key failed", error);
        } catch (final RuntimeException error) {
            throw reject("find human approval packet by key rejected", error);
        }
    }

    @Override
    public Optional<HumanApprovalPacket> findByTenantAndId(
            final String tenantId, final HumanApprovalPacketId id) {
        try {
            return jdbcTemplate
                    .queryForList(
                            SELECT_BY_TENANT_ID,
                            checkedTenant(tenantId),
                            Objects.requireNonNull(id, "id").value())
                    .stream()
                    .findFirst()
                    .map(this::mapPacket);
        } catch (final DataAccessException error) {
            throw new HumanApprovalPacketPersistenceException(
                    "find human approval packet by id failed", error);
        } catch (final RuntimeException error) {
            throw reject("find human approval packet by id rejected", error);
        }
    }

    @Override
    public List<HumanApprovalPacket> findByDecisionRunId(
            final String tenantId, final UUID decisionRunId) {
        try {
            return jdbcTemplate
                    .queryForList(
                            SELECT_BY_TENANT_DECISION_RUN,
                            checkedTenant(tenantId),
                            Objects.requireNonNull(decisionRunId, "decisionRunId"))
                    .stream()
                    .map(this::mapPacket)
                    .toList();
        } catch (final DataAccessException error) {
            throw new HumanApprovalPacketPersistenceException(
                    "find human approval packets by decision run failed", error);
        } catch (final RuntimeException error) {
            throw reject("find human approval packets by decision run rejected", error);
        }
    }

    @Override
    public void updateStatus(
            final String tenantId,
            final HumanApprovalPacketId id,
            final ApprovalStatus previousStatus,
            final ApprovalStatus nextStatus,
            final ApprovalReviewer reviewer,
            final Instant decidedAt,
            final Instant updatedAt) {
        try {
            final int updated =
                    jdbcTemplate.update(
                            UPDATE_STATUS,
                            Objects.requireNonNull(nextStatus, "nextStatus").name(),
                            nullableReviewer(reviewer).reviewerId(),
                            nullableReviewer(reviewer).reviewerNote(),
                            timestampOrNull(decidedAt),
                            timestamp(updatedAt),
                            checkedTenant(tenantId),
                            Objects.requireNonNull(id, "id").value(),
                            Objects.requireNonNull(previousStatus, "previousStatus").name());
            if (updated != 1) {
                throw new HumanApprovalPacketPersistenceException(
                        "update human approval packet status affected " + updated + " rows",
                        new IllegalStateException("approval status update count mismatch"));
            }
        } catch (final DataAccessException error) {
            throw new HumanApprovalPacketPersistenceException(
                    "update human approval packet status failed", error);
        }
    }

    private HumanApprovalPacket mapPacket(final Map<String, Object> row) {
        return new HumanApprovalPacket(
                new HumanApprovalPacketId(uuid(row, "id")),
                uuid(row, "decision_run_id"),
                text(row, "tenant_id"),
                text(row, "trace_id"),
                text(row, "request_id"),
                new ApprovalKey(text(row, "approval_key")),
                enumValue(row, "approval_type", ApprovalType.class),
                enumValue(row, "approval_status", ApprovalStatus.class),
                enumValue(row, "risk_level", RiskLevel.class),
                HumanApprovalPacket.decisionActionFrom(text(row, "decision_action")),
                decimalOrNull(row, "confidence_score"),
                optionalText(row, "summary"),
                new ApprovalChecklist(readMap(text(row, "checklist_json"), "checklistJson")),
                evidenceRefs(row),
                new ApprovalReviewer(optionalText(row, "reviewer_id"), optionalText(row, "reviewer_note")),
                instantOrNull(row, "decided_at"),
                instant(row, "created_at"),
                instant(row, "updated_at"));
    }

    private ApprovalEvidenceRefs evidenceRefs(final Map<String, Object> row) {
        final String json = optionalText(row, "evidence_refs_json");
        return json == null ? null : new ApprovalEvidenceRefs(readMap(json, "evidenceRefsJson"));
    }

    private String writeJson(final Object payload, final String fieldName) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (final JsonProcessingException error) {
            throw new HumanApprovalPacketPersistenceException(
                    "failed to serialize " + fieldName, error);
        }
    }

    private String writeJsonOrNull(final Object payload, final String fieldName) {
        return payload == null ? null : writeJson(payload, fieldName);
    }

    private Map<String, Object> readMap(final String value, final String fieldName) {
        try {
            return objectMapper.readValue(value, MAP_TYPE);
        } catch (final JsonProcessingException error) {
            throw new HumanApprovalPacketPersistenceException("failed to parse " + fieldName, error);
        }
    }

    private static String checkedTenant(final String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            throw new IllegalArgumentException("tenantId must not be blank");
        }
        return tenantId;
    }

    private static ApprovalReviewer nullableReviewer(final ApprovalReviewer reviewer) {
        return reviewer == null ? ApprovalReviewer.none() : reviewer;
    }

    private static HumanApprovalPacketPersistenceException reject(
            final String message, final RuntimeException error) {
        if (error instanceof HumanApprovalPacketPersistenceException persistenceException) {
            return persistenceException;
        }
        return new HumanApprovalPacketPersistenceException(message, error);
    }

    private static Timestamp timestamp(final Instant value) {
        return Timestamp.from(Objects.requireNonNull(value, "value"));
    }

    private static Timestamp timestampOrNull(final Instant value) {
        return value == null ? null : Timestamp.from(value);
    }

    private static <E extends Enum<E>> E enumValue(
            final Map<String, Object> row, final String key, final Class<E> enumType) {
        return Enum.valueOf(enumType, text(row, key));
    }

    private static String text(final Map<String, Object> row, final String key) {
        final String value = optionalText(row, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must not be null");
        }
        return value;
    }

    private static String optionalText(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        final String checked = value.toString().trim();
        return checked.isEmpty() ? null : checked;
    }

    private static UUID uuid(final Map<String, Object> row, final String key) {
        final Object value = Objects.requireNonNull(row.get(key), key);
        if (value instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(value.toString());
    }

    private static BigDecimal decimalOrNull(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof BigDecimal decimal) {
            return decimal;
        }
        return new BigDecimal(value.toString());
    }

    private static Instant instant(final Map<String, Object> row, final String key) {
        final Instant value = instantOrNull(row, key);
        if (value == null) {
            throw new IllegalArgumentException(key + " must not be null");
        }
        return value;
    }

    private static Instant instantOrNull(final Map<String, Object> row, final String key) {
        final Object value = row.get(key);
        if (value == null) {
            return null;
        }
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
}
