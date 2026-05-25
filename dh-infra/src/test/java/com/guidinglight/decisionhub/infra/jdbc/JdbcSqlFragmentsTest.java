package com.guidinglight.decisionhub.infra.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointEntry;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointStatus;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointType;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifact;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifactStatus;
import com.guidinglight.decisionhub.domain.forecast.ForecastHorizon;
import com.guidinglight.decisionhub.domain.forecast.ForecastPoint;
import com.guidinglight.decisionhub.domain.forecast.ForecastTarget;
import com.guidinglight.decisionhub.domain.marketdata.ExternalMarketSnapshot;
import com.guidinglight.decisionhub.domain.marketdata.MarketDataSource;
import com.guidinglight.decisionhub.domain.marketdata.MarketSnapshotStatus;
import com.guidinglight.decisionhub.domain.reflection.ReflectionEntry;
import com.guidinglight.decisionhub.domain.reflection.ReflectionType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Stage2-PoC-B5：跨 4 个 Stage2 JDBC 仓储统一校验 SQL 片段。
 *
 * <p>覆盖：CAST(? AS jsonb) JSONB 转写；insert 命中正确表名；查询走 RowMapper 路径。
 */
@ExtendWith(MockitoExtension.class)
class JdbcSqlFragmentsTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  void reflection_repo_insert_targets_correct_table_with_jsonb_cast() {
    final JdbcReflectionEntryRepository repo = new JdbcReflectionEntryRepository(jdbcTemplate);
    repo.save(
        ReflectionEntry.of(
            "ref-1",
            "run-1",
            "trace-1",
            0,
            AgentRole.LEADER,
            ReflectionType.STEP_REFLECTION,
            "content",
            Instant.parse("2026-05-20T10:00:00Z"),
            "{}"));
    final String sql = captureUpdateSql(jdbcTemplate);
    assertThat(sql).contains("insert into dh_reflection_entries").contains("CAST(? AS jsonb)");
  }

  @Test
  void checkpoint_repo_insert_targets_correct_table_with_jsonb_cast() {
    final JdbcCheckpointEntryRepository repo = new JdbcCheckpointEntryRepository(jdbcTemplate);
    repo.save(
        CheckpointEntry.of(
            "cp-1",
            "run-1",
            "trace-1",
            0,
            CheckpointType.CANDIDATE_FROZEN,
            CheckpointStatus.RECORDED,
            "{}",
            Instant.parse("2026-05-20T10:00:00Z")));
    final String sql = captureUpdateSql(jdbcTemplate);
    assertThat(sql).contains("insert into dh_checkpoint_entries").contains("CAST(? AS jsonb)");
  }

  @Test
  void forecast_repo_insert_uses_two_jsonb_casts() {
    final JdbcForecastArtifactRepository repo =
        new JdbcForecastArtifactRepository(jdbcTemplate, new ObjectMapper());
    repo.save(
        ForecastArtifact.of(
            "fa-1",
            "trace-1",
            "AAPL",
            ForecastHorizon.D5,
            ForecastTarget.PRICE,
            List.of(ForecastPoint.of(LocalDate.of(2026, 5, 21), 100.0, 0.8)),
            "kronos-v1",
            Instant.parse("2026-05-20T10:00:00Z"),
            ForecastArtifactStatus.COMPLETED,
            "{}"));
    final String sql = captureUpdateSql(jdbcTemplate);
    assertThat(sql).contains("insert into dh_forecast_artifacts");
    final int jsonbHits = sql.split("CAST\\(\\? AS jsonb\\)", -1).length - 1;
    assertThat(jsonbHits).isGreaterThanOrEqualTo(2);
  }

  @Test
  void external_snapshot_repo_insert_uses_three_jsonb_casts() {
    final JdbcExternalMarketSnapshotRepository repo =
        new JdbcExternalMarketSnapshotRepository(jdbcTemplate, new ObjectMapper());
    repo.save(
        ExternalMarketSnapshot.of(
            "snap-1",
            "trace-1",
            List.of("AAPL"),
            MarketDataSource.GLOBAL_STOCK_DATA,
            LocalDate.of(2026, 5, 1),
            LocalDate.of(2026, 5, 20),
            Instant.parse("2026-05-20T10:00:00Z"),
            "{}",
            "v1",
            MarketSnapshotStatus.COMPLETED,
            "{}"));
    final String sql = captureUpdateSql(jdbcTemplate);
    assertThat(sql).contains("insert into dh_external_market_snapshots");
    final int jsonbHits = sql.split("CAST\\(\\? AS jsonb\\)", -1).length - 1;
    assertThat(jsonbHits).isGreaterThanOrEqualTo(3);
  }

  @Test
  void external_snapshot_repo_findById_uses_row_mapper() {
    final JdbcExternalMarketSnapshotRepository repo =
        new JdbcExternalMarketSnapshotRepository(jdbcTemplate, new ObjectMapper());
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("missing")))
        .thenReturn(List.of());
    assertThat(repo.findById("missing")).isEmpty();
  }

  /** Mockito 5 varargs ArgumentCaptor 行为不稳，直接从 invocation log 提第一个 update 的 SQL。 */
  private static String captureUpdateSql(final JdbcTemplate template) {
    return mockingDetails(template).getInvocations().stream()
        .filter(inv -> "update".equals(inv.getMethod().getName()))
        .map(Invocation::getArguments)
        .map(args -> (String) args[0])
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected JdbcTemplate.update(...) to be invoked"));
  }
}
