package com.guidinglight.decisionhub.infra.jdbc.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQuery;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.core.JdbcTemplate;

/** K4 JDBC replay read repository 单元测试，不连接真实数据库、不写库、不调用外部系统。 */
@ExtendWith(MockitoExtension.class)
class JdbcDecisionReplayQueryRepositoryTest {

  private static final Instant NOW = Instant.parse("2026-07-01T00:00:00Z");

  @Mock private JdbcTemplate jdbcTemplate;

  private JdbcDecisionReplayQueryRepository repository;

  @BeforeEach
  void setUp() {
    repository =
        new JdbcDecisionReplayQueryRepository(
            jdbcTemplate, new ObjectMapper().findAndRegisterModules());
  }

  @Test
  void findReplay_existingRowsReturnsFoundViewAndUsesTenantScopedQueries() {
    stubRows(true, true, true, true, true, true);

    final DecisionReplayView view = repository.findReplay(query());

    assertThat(view.replayStatus()).isEqualTo(DecisionReplayStatus.FOUND);
    assertThat(view.request().subjectJson()).containsEntry("symbol", "BTC-USDT");
    assertThat(view.timeline().traceSteps()).extracting("id").containsExactly("trace-1", "trace-2");
    assertThat(view.timeline().providerCalls()).extracting("id").containsExactly("provider-1", "provider-2");
    assertThat(view.timeline().providerCalls().get(0).providerStatus())
        .isEqualTo(com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus.TIMEOUT);
    assertThat(view.timeline().providerCalls().get(0).latencyMs()).isEqualTo(125L);
    assertThat(view.timeline().providerCalls().get(0).errorCode()).isEqualTo("TIMEOUT");
    assertThat(view.timeline().providerCalls().get(0).signalJson())
        .containsEntry("failureClass", "TIMEOUT")
        .containsEntry("latencyMs", 125);
    assertThat(view.timeline().auditEvents()).extracting("id").containsExactly("audit-1", "audit-2");
    assertAllSelectsAreTenantAndDecisionScoped();
    assertNoJdbcWrites();
  }

  @Test
  void findReplay_missingDecisionReturnsNotFound() {
    stubRows(false, false, false, false, false, false);

    final DecisionReplayView view = repository.findReplay(query());

    assertThat(view.replayStatus()).isEqualTo(DecisionReplayStatus.NOT_FOUND);
    assertThat(view.request()).isNull();
    assertNoJdbcWrites();
  }

  @Test
  void findReplay_partialRowsReturnIncomplete() {
    stubRows(true, true, true, true, false, true);

    final DecisionReplayView view = repository.findReplay(query());

    assertThat(view.replayStatus()).isEqualTo(DecisionReplayStatus.INCOMPLETE);
    assertThat(view.reasonCodes()).contains("OUTPUT_MISSING");
    assertThat(view.isComplete()).isFalse();
  }

  @Test
  void findReplay_corruptJsonReturnsCorrupted() {
    when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
        .thenAnswer(
            invocation -> {
              final String sql = invocation.getArgument(0, String.class);
              if (sql.contains("dh_decision_request")) {
                return List.of(requestRow("{not-json"));
              }
              return rowsForSql(sql, true, true, true, true, true, true);
            });

    final DecisionReplayView view = repository.findReplay(query());

    assertThat(view.replayStatus()).isEqualTo(DecisionReplayStatus.CORRUPTED);
    assertThat(view.request()).isNull();
  }

  @Test
  void findReplay_databaseReadFailureReturnsBlocked() {
    when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
        .thenThrow(new QueryTimeoutException("synthetic timeout"));

    final DecisionReplayView view = repository.findReplay(query());

    assertThat(view.replayStatus()).isEqualTo(DecisionReplayStatus.BLOCKED);
    assertThat(view.reasonCodes()).contains("REPLAY_QUERY_FAILED");
  }

  @Test
  void findReplay_traceProviderAndAuditQueriesUseStableOrdering() {
    stubRows(true, true, true, true, true, true);

    repository.findReplay(query());

    assertThat(selectSqls())
        .anySatisfy(sql -> assertThat(sql).contains("dh_decision_trace_step").contains("order by started_at asc"))
        .anySatisfy(sql -> assertThat(sql).contains("dh_decision_provider_call_log").contains("order by created_at asc"))
        .anySatisfy(sql -> assertThat(sql).contains("dh_decision_audit_event").contains("order by created_at asc"));
  }

  private void stubRows(
      final boolean request,
      final boolean context,
      final boolean trace,
      final boolean provider,
      final boolean output,
      final boolean audit) {
    when(jdbcTemplate.queryForList(anyString(), any(Object[].class)))
        .thenAnswer(invocation -> rowsForSql(invocation.getArgument(0, String.class), request, context, trace, provider, output, audit));
  }

  private static List<Map<String, Object>> rowsForSql(
      final String sql,
      final boolean request,
      final boolean context,
      final boolean trace,
      final boolean provider,
      final boolean output,
      final boolean audit) {
    if (sql.contains("dh_decision_request")) {
      return request ? List.of(requestRow("{\"symbol\":\"BTC-USDT\"}")) : List.of();
    }
    if (sql.contains("dh_decision_context_snapshot")) {
      return context ? List.of(contextRow()) : List.of();
    }
    if (sql.contains("dh_decision_trace_step")) {
      return trace ? List.of(traceRow("trace-1", "POLICY_CHECK"), traceRow("trace-2", "RISK_REVIEW")) : List.of();
    }
    if (sql.contains("dh_decision_provider_call_log")) {
      return provider
          ? List.of(
              providerRow(
                  "provider-1",
                  "TIMEOUT",
                  125L,
                  "TIMEOUT",
                  "{\"status\":\"TIMEOUT\",\"failureClass\":\"TIMEOUT\",\"latencyMs\":125}"),
              providerRow(
                  "provider-2",
                  "MOCKED",
                  0L,
                  null,
                  "{\"status\":\"MOCKED\",\"failureClass\":\"NONE\",\"latencyMs\":0}"))
          : List.of();
    }
    if (sql.contains("dh_decision_output")) {
      return output ? List.of(outputRow()) : List.of();
    }
    if (sql.contains("dh_decision_audit_event")) {
      return audit ? List.of(auditRow("audit-1"), auditRow("audit-2")) : List.of();
    }
    throw new AssertionError("unexpected SQL: " + sql);
  }

  private static Map<String, Object> requestRow(final String subjectJson) {
    return Map.ofEntries(
        Map.entry("decision_id", "decision-1"),
        Map.entry("request_id", "request-1"),
        Map.entry("trace_id", "trace-1"),
        Map.entry("tenant_id", "tenant-1"),
        Map.entry("source", "codex-test"),
        Map.entry("decision_type", "READ_ONLY_RECOMMENDATION"),
        Map.entry("subject_json", subjectJson),
        Map.entry("context_ref", "context://safe"),
        Map.entry("requested_at", Timestamp.from(NOW)),
        Map.entry("schema_version", "1.0.0"),
        Map.entry("created_at", Timestamp.from(NOW)));
  }

  private static Map<String, Object> contextRow() {
    return Map.of(
        "decision_id", "decision-1",
        "tenant_id", "tenant-1",
        "trace_id", "trace-1",
        "context_snapshot_json", "{\"snapshotPresent\":true}",
        "evidence_refs_json", "[\"evidence://case-1\"]",
        "created_at", Timestamp.from(NOW));
  }

  private static Map<String, Object> traceRow(final String id, final String stepName) {
    return Map.of(
        "id", id,
        "decision_id", "decision-1",
        "tenant_id", "tenant-1",
        "trace_id", "trace-1",
        "step_name", stepName,
        "step_status", "COMPLETED",
        "started_at", Timestamp.from(NOW),
        "ended_at", Timestamp.from(NOW),
        "created_at", Timestamp.from(NOW));
  }

  private static Map<String, Object> providerRow(
      final String id,
      final String providerStatus,
      final long latencyMs,
      final String errorCode,
      final String signalJson) {
    final Map<String, Object> row = new LinkedHashMap<>();
    row.put("id", id);
    row.put("decision_id", "decision-1");
    row.put("tenant_id", "tenant-1");
    row.put("trace_id", "trace-1");
    row.put("provider_name", "MOCK_DECISION_PROVIDER");
    row.put("provider_status", providerStatus);
    row.put("latency_ms", latencyMs);
    row.put("signal_json", signalJson);
    if (errorCode != null) {
      row.put("error_code", errorCode);
    }
    row.put("created_at", Timestamp.from(NOW));
    return row;
  }

  private static Map<String, Object> outputRow() {
    return Map.ofEntries(
        Map.entry("decision_id", "decision-1"),
        Map.entry("tenant_id", "tenant-1"),
        Map.entry("trace_id", "trace-1"),
        Map.entry("request_id", "request-1"),
        Map.entry("decision_type", "READ_ONLY_RECOMMENDATION"),
        Map.entry("action", "NO_TRADE"),
        Map.entry("risk_level", "LOW"),
        Map.entry("policy_status", "ALLOWED"),
        Map.entry("confidence", new BigDecimal("0.5000")),
        Map.entry("output_json", "{\"action\":\"NO_TRADE\"}"),
        Map.entry("created_at", Timestamp.from(NOW)));
  }

  private static Map<String, Object> auditRow(final String id) {
    return Map.of(
        "id", id,
        "decision_id", "decision-1",
        "tenant_id", "tenant-1",
        "trace_id", "trace-1",
        "event_type", "DECISION_COMPLETED",
        "event_status", "SUCCESS",
        "event_json", "{\"action\":\"NO_TRADE\"}",
        "created_at", Timestamp.from(NOW));
  }

  private static DecisionReplayQuery query() {
    return new DecisionReplayQuery("tenant-1", "decision-1", null, null);
  }

  private void assertAllSelectsAreTenantAndDecisionScoped() {
    assertThat(selectSqls())
        .hasSize(6)
        .allSatisfy(sql -> assertThat(sql).contains("tenant_id = ?").contains("decision_id = ?"));
    mockingDetails(jdbcTemplate).getInvocations().stream()
        .filter(inv -> "queryForList".equals(inv.getMethod().getName()))
        .forEach(
            inv -> {
              final Object[] args = inv.getArguments();
              assertThat(args).containsExactly(inv.getArgument(0), "tenant-1", "decision-1");
            });
  }

  private void assertNoJdbcWrites() {
    assertThat(
            mockingDetails(jdbcTemplate).getInvocations().stream()
                .filter(inv -> "update".equals(inv.getMethod().getName()))
                .count())
        .isZero();
  }

  private List<String> selectSqls() {
    return mockingDetails(jdbcTemplate).getInvocations().stream()
        .filter(inv -> "queryForList".equals(inv.getMethod().getName()))
        .map(Invocation::getArguments)
        .map(args -> (String) args[0])
        .toList();
  }
}
