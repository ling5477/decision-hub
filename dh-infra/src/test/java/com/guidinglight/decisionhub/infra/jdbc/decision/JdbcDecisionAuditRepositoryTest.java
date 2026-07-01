package com.guidinglight.decisionhub.infra.jdbc.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockingDetails;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceException;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.decision.DecisionTraceStepName;
import com.guidinglight.decisionhub.usecase.decision.DecisionTraceStepStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * K3 JDBC audit repository 单元测试。
 *
 * <p>覆盖 SQL 目标表、JSONB cast、数据库异常转换和 JSON 序列化异常转换；不连接真实数据库、不调用 NQ 或真实
 * provider。
 */
@ExtendWith(MockitoExtension.class)
class JdbcDecisionAuditRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  @Mock private JdbcTemplate jdbcTemplate;

  private JdbcDecisionAuditRepository repository;

  @BeforeEach
  void setUp() {
    repository =
        new JdbcDecisionAuditRepository(jdbcTemplate, new ObjectMapper().findAndRegisterModules());
  }

  @Test
  void saveRequest_targetsDecisionRequestWithJsonbCast() {
    repository.saveRequest(requestRecord());

    assertThat(captureUpdateSql(jdbcTemplate))
        .contains("insert into dh_decision_request")
        .contains("CAST(? AS jsonb)");
  }

  @Test
  void saveContextSnapshot_targetsSnapshotTableWithTwoJsonbCasts() {
    repository.saveContextSnapshot(contextSnapshotRecord());

    final String sql = captureUpdateSql(jdbcTemplate);
    assertThat(sql).contains("insert into dh_decision_context_snapshot");
    assertThat(jsonbCastCount(sql)).isGreaterThanOrEqualTo(2);
  }

  @Test
  void saveTraceStep_targetsTraceStepTableWithoutJsonbCast() {
    repository.saveTraceStep(traceStepRecord());

    assertThat(captureUpdateSql(jdbcTemplate))
        .contains("insert into dh_decision_trace_step")
        .doesNotContain("CAST(? AS jsonb)");
  }

  @Test
  void saveProviderCall_targetsProviderCallLogWithJsonbCast() {
    repository.saveProviderCall(providerCallRecord());

    assertThat(captureUpdateSql(jdbcTemplate))
        .contains("insert into dh_decision_provider_call_log")
        .contains("CAST(? AS jsonb)");
  }

  @Test
  void saveOutput_targetsOutputTableWithJsonbCast() {
    repository.saveOutput(outputRecord());

    assertThat(captureUpdateSql(jdbcTemplate))
        .contains("insert into dh_decision_output")
        .contains("CAST(? AS jsonb)");
  }

  @Test
  void saveAuditEvent_targetsAuditEventTableWithJsonbCast() {
    repository.saveAuditEvent(auditEventRecord());

    assertThat(captureUpdateSql(jdbcTemplate))
        .contains("insert into dh_decision_audit_event")
        .contains("CAST(? AS jsonb)");
  }

  @Test
  void saveRequest_wrapsDatabaseWriteFailureAsDecisionPersistenceException() {
    doThrow(new DataIntegrityViolationException("constraint failed"))
        .when(jdbcTemplate)
        .update(anyString(), any(Object[].class));

    assertThatThrownBy(() -> repository.saveRequest(requestRecord()))
        .isInstanceOf(DecisionPersistenceException.class)
        .hasMessageContaining("save decision request failed");
  }

  @Test
  void saveOutput_wrapsJsonSerializationFailureWithoutDatabaseWrite() {
    final JdbcDecisionAuditRepository failingRepository =
        new JdbcDecisionAuditRepository(jdbcTemplate, new FailingObjectMapper());

    assertThatThrownBy(() -> failingRepository.saveOutput(outputRecord()))
        .isInstanceOf(DecisionPersistenceException.class)
        .hasMessageContaining("failed to serialize outputJson");
    assertThat(updateInvocations(jdbcTemplate)).isZero();
  }

  @Test
  void recordsRejectBlankTraceIdBeforeJdbcWrite() {
    assertThatThrownBy(
            () ->
                new DecisionPersistenceRecords.RequestRecord(
                    "decision-1",
                    "request-1",
                    " ",
                    "tenant-1",
                    "codex-test",
                    DecisionType.READ_ONLY_RECOMMENDATION,
                    Map.of(),
                    "context://safe",
                    NOW,
                    "1.0.0",
                    NOW))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("traceId");
  }

  private static DecisionPersistenceRecords.RequestRecord requestRecord() {
    return new DecisionPersistenceRecords.RequestRecord(
        "decision-1",
        "request-1",
        "trace-1",
        "tenant-1",
        "codex-test",
        DecisionType.READ_ONLY_RECOMMENDATION,
        Map.of("symbol", "BTC-USDT"),
        "context://safe",
        NOW,
        "1.0.0",
        NOW);
  }

  private static DecisionPersistenceRecords.ContextSnapshotRecord contextSnapshotRecord() {
    return new DecisionPersistenceRecords.ContextSnapshotRecord(
        "decision-1",
        "tenant-1",
        "trace-1",
        Map.of("snapshotPresent", true, "evidenceCount", 1),
        List.of("evidence://case-1"),
        NOW);
  }

  private static DecisionPersistenceRecords.TraceStepRecord traceStepRecord() {
    return new DecisionPersistenceRecords.TraceStepRecord(
        "trace-step-1",
        "decision-1",
        "tenant-1",
        "trace-1",
        DecisionTraceStepName.POLICY_CHECK,
        DecisionTraceStepStatus.COMPLETED,
        NOW,
        NOW,
        null,
        null,
        NOW);
  }

  private static DecisionPersistenceRecords.ProviderCallRecord providerCallRecord() {
    return new DecisionPersistenceRecords.ProviderCallRecord(
        "provider-1",
        "decision-1",
        "tenant-1",
        "trace-1",
        "MOCK_DECISION_PROVIDER",
        ProviderSignalStatus.MOCKED,
        0,
        Map.of("providerMode", "MOCK", "status", "MOCKED"),
        null,
        NOW);
  }

  private static DecisionPersistenceRecords.OutputRecord outputRecord() {
    return new DecisionPersistenceRecords.OutputRecord(
        "decision-1",
        "tenant-1",
        "trace-1",
        "request-1",
        DecisionType.READ_ONLY_RECOMMENDATION,
        DecisionAction.NO_TRADE,
        DecisionRiskLevel.LOW,
        DecisionPolicyStatus.ALLOWED,
        new BigDecimal("0.5000"),
        Map.of(
            "requestId",
            "request-1",
            "traceId",
            "trace-1",
            "tenantId",
            "tenant-1",
            "decisionType",
            "READ_ONLY_RECOMMENDATION",
            "action",
            "NO_TRADE",
            "createdAt",
            NOW.toString()),
        NOW);
  }

  private static DecisionPersistenceRecords.AuditEventRecord auditEventRecord() {
    return new DecisionPersistenceRecords.AuditEventRecord(
        "audit-1",
        "decision-1",
        "tenant-1",
        "trace-1",
        DecisionAuditEventType.DECISION_COMPLETED,
        DecisionAuditEventStatus.SUCCESS,
        Map.of("requestId", "request-1", "action", "NO_TRADE"),
        null,
        NOW);
  }

  /** Mockito 5 varargs ArgumentCaptor 行为不稳，直接从 invocation log 提第一个 update SQL。 */
  private static String captureUpdateSql(final JdbcTemplate template) {
    return mockingDetails(template).getInvocations().stream()
        .filter(inv -> "update".equals(inv.getMethod().getName()))
        .map(Invocation::getArguments)
        .map(args -> (String) args[0])
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected JdbcTemplate.update(...) to be invoked"));
  }

  private static int jsonbCastCount(final String sql) {
    return sql.split("CAST\\(\\? AS jsonb\\)", -1).length - 1;
  }

  private static long updateInvocations(final JdbcTemplate template) {
    return mockingDetails(template).getInvocations().stream()
        .filter(inv -> "update".equals(inv.getMethod().getName()))
        .count();
  }

  private static final class FailingObjectMapper extends ObjectMapper {
    @Override
    public String writeValueAsString(final Object value) throws JsonProcessingException {
      throw new JsonProcessingException("synthetic serialization failure") {};
    }
  }
}
