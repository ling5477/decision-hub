package com.guidinglight.decisionhub.infra.jdbc.qdr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * JdbcHumanApprovalPacketRepository 单元测试。
 *
 * <p>使用 fake JdbcTemplate 校验 SQL 目标表、tenant-bound 查询/更新、duplicate fail-closed 与 DB error
 * fail-closed，不连接真实数据库、不触发外部系统。
 */
final class JdbcHumanApprovalPacketRepositoryTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String OTHER_TENANT_ID = "tenant-b";
    private static final UUID PACKET_UUID =
            UUID.fromString("00000000-0000-0000-0000-000000000201");
    private static final UUID RUN_UUID = UUID.fromString("00000000-0000-0000-0000-000000000202");
    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");

    private RecordingJdbcTemplate jdbcTemplate;
    private JdbcHumanApprovalPacketRepository repository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new RecordingJdbcTemplate();
        repository = new JdbcHumanApprovalPacketRepository(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void saveSuccessInsertsHumanApprovalPacketWithJsonbCasts() {
        final HumanApprovalPacket saved = repository.save(packet());

        assertThat(saved.id().value()).isEqualTo(PACKET_UUID);
        assertThat(jdbcTemplate.firstUpdate().sql())
                .contains("insert into human_approval_packet")
                .contains("CAST(? AS jsonb)");
    }

    @Test
    void findByTenantAndApprovalKeyUsesTenantBoundCondition() {
        jdbcTemplate.rows = List.of(row(TENANT_ID));

        final var found =
                repository.findByTenantAndApprovalKey(TENANT_ID, new ApprovalKey("approval-key-a"));

        assertThat(found).isPresent();
        assertThat(jdbcTemplate.firstQuery().sql()).contains("tenant_id = ? and approval_key = ?");
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, "approval-key-a");
    }

    @Test
    void findByTenantAndIdUsesTenantBoundCondition() {
        jdbcTemplate.rows = List.of(row(TENANT_ID));

        final var found =
                repository.findByTenantAndId(TENANT_ID, new HumanApprovalPacketId(PACKET_UUID));

        assertThat(found).isPresent();
        assertThat(jdbcTemplate.firstQuery().sql()).contains("tenant_id = ? and id = ?");
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, PACKET_UUID);
    }

    @Test
    void findByDecisionRunIdUsesTenantBoundCondition() {
        jdbcTemplate.rows = List.of(row(TENANT_ID));

        final List<HumanApprovalPacket> found = repository.findByDecisionRunId(TENANT_ID, RUN_UUID);

        assertThat(found).hasSize(1);
        assertThat(jdbcTemplate.firstQuery().sql()).contains("tenant_id = ? and decision_run_id = ?");
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, RUN_UUID);
    }

    @Test
    void tenantMismatchReturnsEmptyWithoutCrossTenantFallback() {
        jdbcTemplate.rows = List.of(row(TENANT_ID));

        final var found =
                repository.findByTenantAndApprovalKey(OTHER_TENANT_ID, new ApprovalKey("approval-key-a"));

        assertThat(found).isEmpty();
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(OTHER_TENANT_ID, "approval-key-a");
    }

    @Test
    void duplicateApprovalKeyFailsClosed() {
        jdbcTemplate.duplicateOnUpdate = true;

        assertThatThrownBy(() -> repository.save(packet()))
                .isInstanceOf(HumanApprovalPacketDuplicateException.class)
                .hasMessageContaining("duplicate approval key");
    }

    @Test
    void updateStatusUsesTenantIdIdAndPreviousStatus() {
        repository.updateStatus(
                TENANT_ID,
                new HumanApprovalPacketId(PACKET_UUID),
                ApprovalStatus.PENDING,
                ApprovalStatus.APPROVED,
                new ApprovalReviewer("reviewer-a", "reviewed"),
                NOW,
                NOW);

        final Query update = jdbcTemplate.firstUpdate();
        assertThat(update.sql())
                .contains("where tenant_id = ? and id = ? and approval_status = ?")
                .doesNotContain("where id = ?");
        assertThat(update.args())
                .containsSequence(
                        ApprovalStatus.APPROVED.name(),
                        "reviewer-a",
                        "reviewed")
                .endsWith(TENANT_ID, PACKET_UUID, ApprovalStatus.PENDING.name());
    }

    @Test
    void updateStatusCountMismatchFailsClosed() {
        jdbcTemplate.updateCount = 0;

        assertThatThrownBy(
                        () ->
                                repository.updateStatus(
                                        TENANT_ID,
                                        new HumanApprovalPacketId(PACKET_UUID),
                                        ApprovalStatus.PENDING,
                                        ApprovalStatus.APPROVED,
                                        new ApprovalReviewer("reviewer-a", "reviewed"),
                                        NOW,
                                        NOW))
                .isInstanceOf(HumanApprovalPacketPersistenceException.class)
                .hasMessageContaining("affected 0 rows");
    }

    @Test
    void databaseFailureFailsClosed() {
        jdbcTemplate.failure = new DataAccessResourceFailureException("synthetic database failure");

        assertThatThrownBy(
                        () ->
                                repository.findByTenantAndId(
                                        TENANT_ID, new HumanApprovalPacketId(PACKET_UUID)))
                .isInstanceOf(HumanApprovalPacketPersistenceException.class)
                .hasMessageContaining("find human approval packet by id failed");
    }

    @Test
    void repositoryNeverUsesUuidOnlyQueryOrUpdate() {
        repository.findByTenantAndId(TENANT_ID, new HumanApprovalPacketId(PACKET_UUID));
        repository.updateStatus(
                TENANT_ID,
                new HumanApprovalPacketId(PACKET_UUID),
                ApprovalStatus.PENDING,
                ApprovalStatus.EXPIRED,
                ApprovalReviewer.none(),
                NOW,
                NOW);

        assertThat(jdbcTemplate.queries()).allSatisfy(query -> assertThat(query.sql()).contains("tenant_id = ?"));
        assertThat(jdbcTemplate.updates()).allSatisfy(query -> assertThat(query.sql()).contains("tenant_id = ?"));
    }

    private static HumanApprovalPacket packet() {
        return new HumanApprovalPacket(
                new HumanApprovalPacketId(PACKET_UUID),
                RUN_UUID,
                TENANT_ID,
                "trace-a",
                "request-a",
                new ApprovalKey("approval-key-a"),
                ApprovalType.QUANT_DECISION_REVIEW,
                ApprovalStatus.PENDING,
                RiskLevel.LOW,
                QuantDecisionAction.OBSERVE,
                new BigDecimal("0.5000"),
                "readonly summary",
                new ApprovalChecklist(Map.of("riskReviewed", true)),
                new ApprovalEvidenceRefs(Map.of("trace", "trace://trace-a")),
                ApprovalReviewer.none(),
                null,
                NOW,
                NOW);
    }

    private static Map<String, Object> row(final String tenantId) {
        return row(
                "id",
                PACKET_UUID,
                "decision_run_id",
                RUN_UUID,
                "tenant_id",
                tenantId,
                "trace_id",
                "trace-a",
                "request_id",
                "request-a",
                "approval_key",
                "approval-key-a",
                "approval_type",
                "QUANT_DECISION_REVIEW",
                "approval_status",
                "PENDING",
                "risk_level",
                "LOW",
                "decision_action",
                "OBSERVE",
                "confidence_score",
                new BigDecimal("0.5000"),
                "summary",
                "readonly summary",
                "checklist_json",
                "{\"riskReviewed\":true}",
                "evidence_refs_json",
                "{\"trace\":\"trace://trace-a\"}",
                "reviewer_id",
                null,
                "reviewer_note",
                null,
                "decided_at",
                null,
                "created_at",
                NOW,
                "updated_at",
                NOW);
    }

    private static Map<String, Object> row(final Object... pairs) {
        final Map<String, Object> row = new LinkedHashMap<>();
        for (int index = 0; index < pairs.length; index += 2) {
            row.put(String.valueOf(pairs[index]), pairs[index + 1]);
        }
        return row;
    }

    private static final class RecordingJdbcTemplate extends JdbcTemplate {
        private final List<Query> queries = new ArrayList<>();
        private final List<Query> updates = new ArrayList<>();
        private List<Map<String, Object>> rows = List.of();
        private RuntimeException failure;
        private boolean duplicateOnUpdate;
        private int updateCount = 1;

        @Override
        public List<Map<String, Object>> queryForList(final String sql, final Object... args) {
            queries.add(new Query(sql, new ArrayList<>(Arrays.asList(args))));
            if (failure != null) {
                throw failure;
            }
            if (!args[0].equals(TENANT_ID)) {
                return List.of();
            }
            return rows;
        }

        @Override
        public int update(final String sql, final Object... args) {
            updates.add(new Query(sql, new ArrayList<>(Arrays.asList(args))));
            if (duplicateOnUpdate) {
                throw new DuplicateKeyException("duplicate approval key");
            }
            if (failure != null) {
                throw failure;
            }
            return updateCount;
        }

        Query firstQuery() {
            return queries.getFirst();
        }

        Query firstUpdate() {
            return updates.getFirst();
        }

        List<Query> queries() {
            return List.copyOf(queries);
        }

        List<Query> updates() {
            return List.copyOf(updates);
        }
    }

    private record Query(String sql, List<Object> args) {
    }
}
