package com.guidinglight.decisionhub.infra.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Stage2-PoC-B5：JdbcNqFeedbackEventRepository 单元测试。
 *
 * <p>本地无 Docker，无法跑真实 Postgres；本测试只校验 SQL 片段 + 幂等行为，确保实现签名与 V3 schema 一致。
 */
@ExtendWith(MockitoExtension.class)
class JdbcNqFeedbackEventRepositoryTest {

  @Mock private JdbcTemplate jdbcTemplate;
  private JdbcNqFeedbackEventRepository repository;

  @BeforeEach
  void setUp() {
    repository = new JdbcNqFeedbackEventRepository(jdbcTemplate, new ObjectMapper());
  }

  @Test
  void saveEnvelope_first_write_returns_true_and_uses_jsonb_cast() {
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("evt-001")))
        .thenReturn(List.of());

    final NqFeedbackEnvelope envelope = sampleEnvelope("evt-001");
    final boolean inserted = repository.saveEnvelope(envelope);

    assertThat(inserted).isTrue();

    final String sql = captureUpdateSql(jdbcTemplate);
    assertThat(sql)
        .contains("insert into dh_nq_feedback_events")
        .contains("CAST(? AS jsonb)");
  }

  @Test
  void saveEnvelope_returns_false_when_findEnvelopeByEventId_hits() {
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("evt-dup")))
        .thenReturn(List.of(sampleEnvelope("evt-dup")));

    final boolean inserted = repository.saveEnvelope(sampleEnvelope("evt-dup"));

    assertThat(inserted).isFalse();
    assertThat(updateInvocations(jdbcTemplate)).isZero();
  }

  @Test
  void saveEnvelope_returns_false_on_unique_violation_race() {
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("evt-race")))
        .thenReturn(List.of());
    // varargs in Mockito 5：用 doThrow + 实参类型方法签名，避免 (Object[]) any() 不命中 varargs slot。
    org.mockito.Mockito.doThrow(new DuplicateKeyException("unique constraint hit"))
        .when(jdbcTemplate)
        .update(anyString(), any(Object[].class));

    final boolean inserted = repository.saveEnvelope(sampleEnvelope("evt-race"));

    assertThat(inserted).isFalse();
    assertThat(updateInvocations(jdbcTemplate)).isEqualTo(1);
  }

  @Test
  void findEnvelopeByEventId_returns_empty_for_null() {
    assertThat(repository.findEnvelopeByEventId(null)).isEmpty();
  }

  private NqFeedbackEnvelope sampleEnvelope(final String eventId) {
    return NqFeedbackEnvelope.of(
        eventId,
        NqFeedbackEventType.PAPER_RUN_CREATED,
        Instant.parse("2026-05-20T10:00:00Z"),
        NqFeedbackEnvelope.SOURCE_SYSTEM_NEXUS_QUANT,
        "job-1",
        "trace-1",
        "req-1",
        "corr-1",
        NqFeedbackEnvelope.DEFAULT_SCHEMA_VERSION,
        "{\"paperRunId\":\"pr-1\"}",
        Instant.parse("2026-05-20T10:00:01Z"));
  }

  private static String captureUpdateSql(final JdbcTemplate template) {
    return mockingDetails(template).getInvocations().stream()
        .filter(inv -> "update".equals(inv.getMethod().getName()))
        .map(Invocation::getArguments)
        .map(args -> (String) args[0])
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected JdbcTemplate.update(...) to be invoked"));
  }

  private static long updateInvocations(final JdbcTemplate template) {
    return mockingDetails(template).getInvocations().stream()
        .filter(inv -> "update".equals(inv.getMethod().getName()))
        .count();
  }
}
