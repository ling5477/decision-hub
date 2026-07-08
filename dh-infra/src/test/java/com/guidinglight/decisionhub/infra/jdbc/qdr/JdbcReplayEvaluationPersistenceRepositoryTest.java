package com.guidinglight.decisionhub.infra.jdbc.qdr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationCase;
import com.guidinglight.decisionhub.domain.qdr.replay.EvaluationPolicy;
import com.guidinglight.decisionhub.domain.qdr.replay.ExpectedDecisionSummary;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionSeverity;
import com.guidinglight.decisionhub.domain.qdr.replay.RegressionVerdict;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayCase;
import com.guidinglight.decisionhub.domain.qdr.replay.ReplayInputRef;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayChecksumConflictException;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveEvaluationCaseCommand;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveRegressionFindingCommand;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveRegressionVerdictCommand;
import com.guidinglight.decisionhub.usecase.qdr.replay.SaveReplayCaseCommand;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * stage-qdr-4 B2 replay/evaluation persistence JDBC repository tests。
 *
 * <p>使用 fake JdbcTemplate 验证 tenant-bound SQL、duplicate checksum、redaction guard、trading-term guard 和
 * fail-closed；不启动数据库、不调用 provider/HTTP/NQ/Agent/LangGraph。
 */
final class JdbcReplayEvaluationPersistenceRepositoryTest {

    private static final String TENANT_ID = "tenant-a";
    private static final String OTHER_TENANT_ID = "tenant-b";
    private static final String CASE_ID = "case-a";
    private static final String EVALUATION_ID = "evaluation-a";
    private static final String VERDICT_ID = "verdict-a";
    private static final String HASH_A =
            "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String HASH_B =
            "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final UUID REPLAY_CASE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000901");
    private static final UUID EVALUATION_CASE_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000902");
    private static final UUID INPUT_REF_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000903");
    private static final UUID EXPECTED_SUMMARY_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000904");
    private static final UUID VERDICT_RECORD_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000905");
    private static final UUID FINDING_ID =
            UUID.fromString("00000000-0000-0000-0000-000000000906");
    private static final Instant NOW = Instant.parse("2026-07-08T00:00:00Z");

    private RecordingJdbcTemplate jdbcTemplate;
    private JdbcReplayCaseRepository replayRepository;
    private JdbcEvaluationCaseRepository evaluationRepository;
    private JdbcRegressionVerdictRepository verdictRepository;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new RecordingJdbcTemplate();
        final ObjectMapper objectMapper = new ObjectMapper();
        replayRepository = new JdbcReplayCaseRepository(jdbcTemplate, objectMapper);
        evaluationRepository = new JdbcEvaluationCaseRepository(jdbcTemplate, objectMapper);
        verdictRepository = new JdbcRegressionVerdictRepository(jdbcTemplate);
    }

    @Test
    void saveReplayCaseWritesTenantIdAndStructuredJsonRefs() {
        final var saved = replayRepository.save(replayCommand(HASH_A, summary("OBSERVE")));

        assertThat(saved.tenantId()).isEqualTo(TENANT_ID);
        assertThat(jdbcTemplate.updates()).hasSize(3);
        assertThat(jdbcTemplate.updates().get(0).sql())
                .contains("insert into qdr_replay_input_ref")
                .contains("tenant_id");
        assertThat(jdbcTemplate.updates().get(0).args()).contains(TENANT_ID);
        assertThat(jdbcTemplate.updates().get(0).args().toString())
                .doesNotContain("rawPrompt")
                .doesNotContain("raw_provider_response")
                .doesNotContain("credential");
    }

    @Test
    void findReplayCaseIsTenantBound() {
        jdbcTemplate.rows = List.of(replayRow(TENANT_ID, HASH_A));

        final var found = replayRepository.findByCaseId(TENANT_ID, CASE_ID);

        assertThat(found).isPresent();
        assertThat(jdbcTemplate.firstQuery().sql())
                .contains("where rc.tenant_id = ? and rc.case_id = ?");
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(TENANT_ID, CASE_ID);
    }

    @Test
    void crossTenantReplayCaseReadReturnsEmpty() {
        jdbcTemplate.rows = List.of(replayRow(TENANT_ID, HASH_A));

        final var found = replayRepository.findByCaseId(OTHER_TENANT_ID, CASE_ID);

        assertThat(found).isEmpty();
        assertThat(jdbcTemplate.firstQuery().args()).containsExactly(OTHER_TENANT_ID, CASE_ID);
    }

    @Test
    void duplicateReplayCaseSameChecksumIsDeterministic() {
        jdbcTemplate.rows = List.of(replayRow(TENANT_ID, HASH_A));

        final var saved = replayRepository.save(replayCommand(HASH_A, summary("OBSERVE")));

        assertThat(saved.caseChecksum()).isEqualTo(HASH_A);
        assertThat(jdbcTemplate.updates()).isEmpty();
    }

    @Test
    void duplicateReplayCaseDifferentChecksumFailsClosed() {
        jdbcTemplate.rows = List.of(replayRow(TENANT_ID, HASH_A));

        assertThatThrownBy(() -> replayRepository.save(replayCommand(HASH_B, summary("OBSERVE"))))
                .isInstanceOf(ReplayChecksumConflictException.class);
    }

    @Test
    void saveEvaluationRejectsMissingReplayCaseRef() {
        jdbcTemplate.replayCaseRefExists = false;

        assertThatThrownBy(() -> evaluationRepository.save(evaluationCommand(HASH_A)))
                .isInstanceOf(ReplayPersistenceException.class)
                .hasMessageContaining("replay case ref missing");
    }

    @Test
    void saveEvaluationCaseWritesTenantAndCanBeQueried() {
        final var saved = evaluationRepository.save(evaluationCommand(HASH_A));
        jdbcTemplate.rows = List.of(evaluationRow(TENANT_ID, HASH_A));

        final var found = evaluationRepository.findByEvaluationId(TENANT_ID, EVALUATION_ID);

        assertThat(saved.tenantId()).isEqualTo(TENANT_ID);
        assertThat(found).isPresent();
        assertThat(jdbcTemplate.updates()).anySatisfy(update -> assertThat(update.sql()).contains("tenant_id"));
        assertThat(jdbcTemplate.queries()).anySatisfy(
                query -> assertThat(query.sql()).contains("ec.tenant_id = ? and ec.evaluation_id = ?"));
    }

    @Test
    void duplicateEvaluationCaseSameChecksumIsDeterministic() {
        jdbcTemplate.rows = List.of(evaluationRow(TENANT_ID, HASH_A));

        final var saved = evaluationRepository.save(evaluationCommand(HASH_A));

        assertThat(saved.evaluationChecksum()).isEqualTo(HASH_A);
        assertThat(jdbcTemplate.updates()).isEmpty();
    }

    @Test
    void duplicateEvaluationCaseDifferentChecksumFailsClosed() {
        jdbcTemplate.rows = List.of(evaluationRow(TENANT_ID, HASH_A));

        assertThatThrownBy(() -> evaluationRepository.save(evaluationCommand(HASH_B)))
                .isInstanceOf(ReplayChecksumConflictException.class);
    }

    @Test
    void crossTenantEvaluationAndVerdictReadsReturnEmpty() {
        jdbcTemplate.rows = List.of(evaluationRow(TENANT_ID, HASH_A));
        assertThat(evaluationRepository.findByEvaluationId(OTHER_TENANT_ID, EVALUATION_ID)).isEmpty();

        jdbcTemplate.rows = List.of(verdictRow(TENANT_ID));
        assertThat(verdictRepository.findByVerdictId(OTHER_TENANT_ID, VERDICT_ID)).isEmpty();
    }

    @Test
    void paginationLimitOverOneHundredIsRejected() {
        assertThatThrownBy(() -> replayRepository.listByTenant(TENANT_ID, 101, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> evaluationRepository.listByTenant(TENANT_ID, 101, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> verdictRepository.listByTenant(TENANT_ID, 101, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void regressionVerdictCanBeSavedAndQueriedTenantBound() {
        final var saved = verdictRepository.save(verdictCommand());
        jdbcTemplate.rows = List.of(verdictRow(TENANT_ID));

        final var found = verdictRepository.findByVerdictId(TENANT_ID, VERDICT_ID);

        assertThat(saved.verdict()).isEqualTo(RegressionVerdict.Status.FAIL);
        assertThat(found).isPresent();
        assertThat(jdbcTemplate.updates()).anySatisfy(
                update -> assertThat(update.sql()).contains("insert into qdr_regression_verdict"));
        assertThat(jdbcTemplate.queries()).anySatisfy(
                query -> assertThat(query.sql()).contains("where tenant_id = ? and verdict_id = ?"));
    }

    @Test
    void regressionFindingListCanBeSavedAndQueriedTenantBound() {
        final var saved = verdictRepository.saveFindings(
                TENANT_ID, VERDICT_ID, List.of(findingCommand()));
        jdbcTemplate.rows = List.of(findingRow(TENANT_ID));

        final var found = verdictRepository.listFindingsByVerdictId(TENANT_ID, VERDICT_ID, 100, 0);

        assertThat(saved).hasSize(1);
        assertThat(found).hasSize(1);
        assertThat(jdbcTemplate.updates()).anySatisfy(
                update -> assertThat(update.sql()).contains("insert into qdr_regression_finding"));
        assertThat(jdbcTemplate.queries()).anySatisfy(
                query -> assertThat(query.sql()).contains("where tenant_id = ? and verdict_id = ?"));
    }

    @Test
    void executableTradingActionsCannotBePersistedAsAllowedAction() {
        for (String action :
                List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE")) {
            assertThatThrownBy(() -> replayRepository.save(replayCommand(HASH_A, summary(action))))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    void repositorySqlFailureFailsClosed() {
        jdbcTemplate.failure = new DataAccessResourceFailureException("synthetic database failure");

        assertThatThrownBy(() -> replayRepository.findByCaseId(TENANT_ID, CASE_ID))
                .isInstanceOf(ReplayPersistenceException.class)
                .hasMessageContaining("find replay case by caseId failed");
        assertThatThrownBy(() -> evaluationRepository.save(evaluationCommand(HASH_A)))
                .isInstanceOf(ReplayPersistenceException.class);
        assertThatThrownBy(() -> verdictRepository.save(verdictCommand()))
                .isInstanceOf(ReplayPersistenceException.class);
    }

    @Test
    void repositorySqlNeverUsesUuidOnlyQueryOrTenantlessWrite() {
        jdbcTemplate.rows = List.of(replayRow(TENANT_ID, HASH_A));
        replayRepository.findById(TENANT_ID, REPLAY_CASE_ID);
        jdbcTemplate.rows = List.of(evaluationRow(TENANT_ID, HASH_A));
        evaluationRepository.findById(TENANT_ID, EVALUATION_CASE_ID);
        jdbcTemplate.rows = List.of(verdictRow(TENANT_ID));
        verdictRepository.findById(TENANT_ID, VERDICT_RECORD_ID);
        jdbcTemplate.rows = List.of(findingRow(TENANT_ID));
        verdictRepository.listFindingsByVerdictId(TENANT_ID, VERDICT_ID, 100, 0);
        jdbcTemplate.rows = List.of();
        replayRepository.save(replayCommand(HASH_A, summary("OBSERVE")));

        assertThat(jdbcTemplate.queries()).allSatisfy(query -> assertThat(query.sql()).contains("tenant_id = ?"));
        assertThat(jdbcTemplate.updates()).allSatisfy(query -> assertThat(query.sql()).contains("tenant_id"));
        assertThat(jdbcTemplate.queries())
                .noneSatisfy(
                        query ->
                                assertThat(query.sql().toLowerCase(Locale.ROOT))
                                        .contains("where id = ?"));
    }

    private static SaveReplayCaseCommand replayCommand(
            final String checksum, final ExpectedDecisionSummary summary) {
        return new SaveReplayCaseCommand(
                REPLAY_CASE_ID,
                INPUT_REF_ID,
                EXPECTED_SUMMARY_ID,
                new ReplayCase(
                        TENANT_ID,
                        CASE_ID,
                        "source-decision-a",
                        "source-request-a",
                        "trace-a",
                        inputRef(),
                        summary,
                        policy(),
                        NOW),
                "request-a",
                "gateway-version-a",
                HASH_A,
                checksum,
                NOW);
    }

    private static SaveEvaluationCaseCommand evaluationCommand(final String checksum) {
        return new SaveEvaluationCaseCommand(
                EVALUATION_CASE_ID,
                INPUT_REF_ID,
                null,
                EXPECTED_SUMMARY_ID,
                null,
                new EvaluationCase(
                        TENANT_ID,
                        EVALUATION_ID,
                        CASE_ID,
                        "policy-v1",
                        "model-version-a",
                        "gateway-version-a",
                        summary("OBSERVE"),
                        null,
                        null,
                        null),
                "source-decision-a",
                "source-request-a",
                "trace-a",
                "request-a",
                inputRef(),
                HASH_A,
                null,
                checksum,
                NOW,
                NOW);
    }

    private static SaveRegressionVerdictCommand verdictCommand() {
        return new SaveRegressionVerdictCommand(
                VERDICT_RECORD_ID,
                TENANT_ID,
                CASE_ID,
                EVALUATION_ID,
                VERDICT_ID,
                "source-decision-a",
                "source-request-a",
                "trace-a",
                "request-a",
                "policy-v1",
                "gateway-version-a",
                INPUT_REF_ID,
                null,
                EXPECTED_SUMMARY_ID,
                null,
                HASH_A,
                null,
                RegressionVerdict.fail("summary mismatch"),
                RegressionSeverity.ERROR,
                "SUMMARY_MISMATCH",
                "expected summary mismatch",
                NOW,
                NOW);
    }

    private static SaveRegressionFindingCommand findingCommand() {
        return new SaveRegressionFindingCommand(
                FINDING_ID,
                TENANT_ID,
                CASE_ID,
                EVALUATION_ID,
                VERDICT_ID,
                "source-decision-a",
                "source-request-a",
                "trace-a",
                "request-a",
                "policy-v1",
                "gateway-version-a",
                INPUT_REF_ID,
                null,
                EXPECTED_SUMMARY_ID,
                null,
                HASH_A,
                null,
                RegressionVerdict.Status.FAIL,
                RegressionSeverity.ERROR,
                "SUMMARY_MISMATCH",
                "expected summary mismatch",
                "evidence-ref-a",
                NOW,
                NOW);
    }

    private static ReplayInputRef inputRef() {
        return new ReplayInputRef("DECISION_REF", "input-ref-a", HASH_A);
    }

    private static ExpectedDecisionSummary summary(final String actionLabel) {
        return new ExpectedDecisionSummary(
                "READ_ONLY_RECOMMENDATION",
                actionLabel,
                "MEDIUM",
                RiskLevel.LOW,
                List.of("evidence-ref-a"),
                List.of("BUY", "SELL", "MARKET_ORDER", "PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE"));
    }

    private static EvaluationPolicy policy() {
        return new EvaluationPolicy(
                "policy-v1",
                BigDecimal.ZERO,
                0,
                EvaluationPolicy.RequiredEvidenceMode.STRICT,
                false);
    }

    private static Map<String, Object> replayRow(final String tenantId, final String checksum) {
        return row(
                "id", REPLAY_CASE_ID,
                "tenant_id", tenantId,
                "case_id", CASE_ID,
                "source_decision_id", "source-decision-a",
                "source_request_id", "source-request-a",
                "trace_id", "trace-a",
                "request_id", "request-a",
                "policy_version", "policy-v1",
                "model_gateway_version_ref", "gateway-version-a",
                "input_ref_id", INPUT_REF_ID,
                "expected_summary_id", EXPECTED_SUMMARY_ID,
                "expected_summary_hash", HASH_A,
                "case_checksum", checksum,
                "created_at", NOW,
                "updated_at", NOW,
                "input_ref", inputRefJson(),
                "expected_summary_json", summaryJson("OBSERVE"),
                "required_evidence_refs_json", "[\"evidence-ref-a\"]",
                "forbidden_actions_json", "[\"BUY\"]");
    }

    private static Map<String, Object> evaluationRow(final String tenantId, final String checksum) {
        return row(
                "id", EVALUATION_CASE_ID,
                "tenant_id", tenantId,
                "case_id", CASE_ID,
                "evaluation_id", EVALUATION_ID,
                "verdict_id", null,
                "source_decision_id", "source-decision-a",
                "source_request_id", "source-request-a",
                "trace_id", "trace-a",
                "request_id", "request-a",
                "policy_version", "policy-v1",
                "model_version_ref", "model-version-a",
                "model_gateway_version_ref", "gateway-version-a",
                "input_ref_id", INPUT_REF_ID,
                "output_ref_id", null,
                "expected_summary_id", EXPECTED_SUMMARY_ID,
                "actual_summary_id", null,
                "expected_summary_hash", HASH_A,
                "actual_summary_hash", null,
                "verdict", null,
                "severity", null,
                "finding_code", null,
                "finding_message", null,
                "evaluation_checksum", checksum,
                "created_at", NOW,
                "updated_at", NOW,
                "input_ref", inputRefJson(),
                "output_ref", null,
                "expected_summary_json", summaryJson("OBSERVE"),
                "expected_evidence_refs_json", "[\"evidence-ref-a\"]",
                "expected_forbidden_actions_json", "[\"BUY\"]",
                "actual_summary_json", null,
                "actual_evidence_refs_json", null,
                "actual_forbidden_actions_json", null);
    }

    private static Map<String, Object> verdictRow(final String tenantId) {
        return row(
                "id", VERDICT_RECORD_ID,
                "tenant_id", tenantId,
                "case_id", CASE_ID,
                "evaluation_id", EVALUATION_ID,
                "verdict_id", VERDICT_ID,
                "source_decision_id", "source-decision-a",
                "source_request_id", "source-request-a",
                "trace_id", "trace-a",
                "request_id", "request-a",
                "policy_version", "policy-v1",
                "model_gateway_version_ref", "gateway-version-a",
                "input_ref_id", INPUT_REF_ID,
                "output_ref_id", null,
                "expected_summary_id", EXPECTED_SUMMARY_ID,
                "actual_summary_id", null,
                "expected_summary_hash", HASH_A,
                "actual_summary_hash", null,
                "verdict", "FAIL",
                "severity", "ERROR",
                "finding_code", "SUMMARY_MISMATCH",
                "finding_message", "expected summary mismatch",
                "failure_reason", "summary mismatch",
                "created_at", NOW,
                "updated_at", NOW);
    }

    private static Map<String, Object> findingRow(final String tenantId) {
        return row(
                "id", FINDING_ID,
                "tenant_id", tenantId,
                "case_id", CASE_ID,
                "evaluation_id", EVALUATION_ID,
                "verdict_id", VERDICT_ID,
                "source_decision_id", "source-decision-a",
                "source_request_id", "source-request-a",
                "trace_id", "trace-a",
                "request_id", "request-a",
                "policy_version", "policy-v1",
                "model_gateway_version_ref", "gateway-version-a",
                "input_ref_id", INPUT_REF_ID,
                "output_ref_id", null,
                "expected_summary_id", EXPECTED_SUMMARY_ID,
                "actual_summary_id", null,
                "expected_summary_hash", HASH_A,
                "actual_summary_hash", null,
                "verdict", "FAIL",
                "severity", "ERROR",
                "finding_code", "SUMMARY_MISMATCH",
                "finding_message", "expected summary mismatch",
                "evidence_ref", "evidence-ref-a",
                "created_at", NOW,
                "updated_at", NOW);
    }

    private static String inputRefJson() {
        return "{\"refType\":\"DECISION_REF\",\"refId\":\"input-ref-a\",\"contentHash\":\"" + HASH_A + "\"}";
    }

    private static String summaryJson(final String actionLabel) {
        return "{\"decisionType\":\"READ_ONLY_RECOMMENDATION\",\"actionLabel\":\""
                + actionLabel
                + "\",\"confidenceBand\":\"MEDIUM\",\"riskLevel\":\"LOW\","
                + "\"requiredEvidenceRefs\":[\"evidence-ref-a\"],\"forbiddenActions\":[\"BUY\"]}";
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
        private boolean replayCaseRefExists = true;
        private boolean inputRefExists = true;
        private boolean outputRefExists = true;
        private boolean summaryRefExists = true;
        private boolean evaluationRefExists = true;
        private boolean verdictRefExists = true;

        @Override
        public List<Map<String, Object>> queryForList(final String sql, final Object... args) {
            queries.add(new Query(sql, new ArrayList<>(Arrays.asList(args))));
            if (failure != null) {
                throw failure;
            }
            if (args.length > 0 && !TENANT_ID.equals(args[0])) {
                return List.of();
            }
            final String lower = sql.toLowerCase(Locale.ROOT);
            if (lower.startsWith("select id from qdr_replay_case")) {
                return replayCaseRefExists ? List.of(row("id", REPLAY_CASE_ID)) : List.of();
            }
            if (lower.startsWith("select id from qdr_replay_input_ref")) {
                return inputRefExists ? List.of(row("id", INPUT_REF_ID)) : List.of();
            }
            if (lower.startsWith("select id from qdr_replay_output_ref")) {
                return outputRefExists ? List.of(row("id", UUID.randomUUID())) : List.of();
            }
            if (lower.startsWith("select id from qdr_expected_decision_summary")) {
                return summaryRefExists ? List.of(row("id", EXPECTED_SUMMARY_ID)) : List.of();
            }
            if (lower.startsWith("select id from qdr_evaluation_case")) {
                return evaluationRefExists ? List.of(row("id", EVALUATION_CASE_ID)) : List.of();
            }
            if (lower.startsWith("select id from qdr_regression_verdict")) {
                return verdictRefExists ? List.of(row("id", VERDICT_RECORD_ID)) : List.of();
            }
            return rows;
        }

        @Override
        public int update(final String sql, final Object... args) {
            updates.add(new Query(sql, new ArrayList<>(Arrays.asList(args))));
            if (failure != null) {
                throw failure;
            }
            return 1;
        }

        Query firstQuery() {
            return queries.getFirst();
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
