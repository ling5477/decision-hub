package com.guidinglight.decisionhub.infra.jdbc.qdr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.qdr.DecisionRequest;
import com.guidinglight.decisionhub.domain.qdr.DecisionRequestStatus;
import com.guidinglight.decisionhub.domain.qdr.DecisionRun;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import com.guidinglight.decisionhub.domain.qdr.HumanApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.QuantDecision;
import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.QuantSignal;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.usecase.qdr.DecisionCorePersistenceException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * stage-qdr-1 JDBC Decision Core repository 单元测试。
 *
 * <p>覆盖 SQL 目标表、JSONB cast 和 fail-closed 异常转换；不连接真实数据库、不调用 NQ 或 provider。
 */
@ExtendWith(MockitoExtension.class)
class JdbcDecisionCoreRepositoryTest {

    private static final Instant NOW = Instant.parse("2026-07-06T00:00:00Z");
    private static final UUID REQUEST_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RUN_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID SIGNAL_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Mock
    private JdbcTemplate jdbcTemplate;

    private JdbcDecisionCoreRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JdbcDecisionCoreRepository(jdbcTemplate, new ObjectMapper());
    }

    @Test
    void saveDecisionRequestTargetsDecisionRequestWithJsonbCasts() {
        repository.save(request());

        assertThat(captureUpdateSql(jdbcTemplate))
                .contains("insert into decision_request")
                .contains("CAST(? AS jsonb)");
    }

    @Test
    void saveDecisionRunAndCompleteUseDecisionRunTable() {
        repository.save(run());
        repository.complete(RUN_ID, DecisionRunStatus.SUCCEEDED, NOW, 10L, null, null);

        assertThat(allUpdateSql(jdbcTemplate)).contains("insert into decision_run");
        assertThat(allUpdateSql(jdbcTemplate)).contains("update decision_run set status = ?");
    }

    @Test
    void saveQuantSignalAndDecisionUseJsonbCasts() {
        repository.save(signal());
        repository.save(decision());

        final String sql = allUpdateSql(jdbcTemplate);
        assertThat(sql).contains("insert into quant_signal").contains("insert into quant_decision");
        assertThat(sql).contains("CAST(? AS jsonb)");
    }

    @Test
    void databaseFailureWrapsAsDecisionCorePersistenceException() {
        doThrow(new DataIntegrityViolationException("constraint failed"))
                .when(jdbcTemplate)
                .update(anyString(), any(Object[].class));

        assertThatThrownBy(() -> repository.save(request()))
                .isInstanceOf(DecisionCorePersistenceException.class)
                .hasMessageContaining("save decision request failed");
    }

    @Test
    void jsonSerializationFailureDoesNotWriteDatabase() {
        final JdbcDecisionCoreRepository failing =
                new JdbcDecisionCoreRepository(jdbcTemplate, new FailingObjectMapper());

        assertThatThrownBy(() -> failing.save(decision()))
                .isInstanceOf(DecisionCorePersistenceException.class)
                .hasMessageContaining("failed to serialize constraintsJson");
        assertThat(updateInvocations(jdbcTemplate)).isZero();
    }

    private static DecisionRequest request() {
        return new DecisionRequest(
                REQUEST_ID,
                "request-key-a",
                "QUANT_DECISION_REVIEW",
                "NQ_DRYRUN",
                "snapshot-a",
                "tenant-a",
                "trace-a",
                "request-a",
                Map.of("dryRun", true),
                Map.of("symbol", "BTC-USDT"),
                DecisionRequestStatus.ACCEPTED,
                NOW,
                NOW);
    }

    private static DecisionRun run() {
        return new DecisionRun(
                RUN_ID,
                REQUEST_ID,
                1,
                DecisionRunStatus.RUNNING,
                "DEFAULT_DECISION_ORCHESTRATOR",
                null,
                null,
                NOW,
                null,
                null,
                null,
                null,
                NOW);
    }

    private static QuantSignal signal() {
        return new QuantSignal(
                SIGNAL_ID,
                REQUEST_ID,
                "NQ_DRYRUN",
                "BTC-USDT",
                "CRYPTO",
                "1h",
                "UNKNOWN_REVIEW_INPUT",
                Map.of("payloadClass", "SANITIZED_REVIEW_INPUT"),
                "strategy-a",
                null,
                null,
                NOW,
                NOW);
    }

    private static QuantDecision decision() {
        return new QuantDecision(
                UUID.fromString("00000000-0000-0000-0000-000000000004"),
                SIGNAL_ID,
                RUN_ID,
                QuantDecisionAction.OBSERVE,
                new BigDecimal("0.5000"),
                RiskLevel.LOW,
                "readonly",
                Map.of("readOnly", true),
                HumanApprovalStatus.NOT_REQUIRED,
                NOW);
    }

    private static String captureUpdateSql(final JdbcTemplate template) {
        return mockingDetails(template).getInvocations().stream()
                .filter(inv -> "update".equals(inv.getMethod().getName()))
                .map(Invocation::getArguments)
                .map(args -> (String) args[0])
                .findFirst()
                .orElseThrow(() -> new AssertionError("expected JdbcTemplate.update(...) to be invoked"));
    }

    private static String allUpdateSql(final JdbcTemplate template) {
        return String.join(
                "\n",
                mockingDetails(template).getInvocations().stream()
                        .filter(inv -> "update".equals(inv.getMethod().getName()))
                        .map(Invocation::getArguments)
                        .map(args -> (String) args[0])
                        .toList());
    }

    private static long updateInvocations(final JdbcTemplate template) {
        return mockingDetails(template).getInvocations().stream()
                .filter(inv -> "update".equals(inv.getMethod().getName()))
                .count();
    }

    private static final class FailingObjectMapper extends ObjectMapper {
        @Override
        public String writeValueAsString(final Object value) throws JsonProcessingException {
            throw new JsonProcessingException("synthetic serialization failure") {
            };
        }
    }
}
