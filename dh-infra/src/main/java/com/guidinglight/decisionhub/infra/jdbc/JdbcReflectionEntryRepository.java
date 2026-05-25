package com.guidinglight.decisionhub.infra.jdbc;

import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.reflection.ReflectionEntry;
import com.guidinglight.decisionhub.domain.reflection.ReflectionType;
import com.guidinglight.decisionhub.usecase.agent.ReflectionEntryRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Stage2-PoC-B5：ReflectionEntryRepository 的 JDBC 实现。
 *
 * <p>JSON 列写入采用 {@code CAST(? AS jsonb)}；TIMESTAMPTZ 列采用 {@link Timestamp#from(Instant)} 注入。
 * 同一 runId 内 stepIndex 唯一（由 V3 迁移 {@code ux_dh_reflection_entries_step} 兜底）。
 */
public final class JdbcReflectionEntryRepository implements ReflectionEntryRepository {

  private static final String INSERT_SQL =
      "insert into dh_reflection_entries"
          + " (id, run_id, trace_id, step_index, agent_role, type, content, payload_json, created_at)"
          + " values (?, ?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)";

  private static final String SELECT_BY_RUN_SQL =
      "select id, run_id, trace_id, step_index, agent_role, type, content, payload_json, created_at"
          + " from dh_reflection_entries"
          + " where run_id = ?"
          + " order by step_index asc";

  private final JdbcTemplate jdbcTemplate;

  /** 构造。 */
  public JdbcReflectionEntryRepository(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public void save(final ReflectionEntry entry) {
    Objects.requireNonNull(entry, "entry");
    jdbcTemplate.update(
        INSERT_SQL,
        entry.getReflectionId(),
        entry.getRunId(),
        entry.getTraceId(),
        entry.getStepIndex(),
        entry.getAgentRole().name(),
        entry.getType().name(),
        entry.getContent(),
        entry.getPayloadJson(),
        Timestamp.from(entry.getCreatedAt()));
  }

  @Override
  public List<ReflectionEntry> listByRun(final String runId) {
    Objects.requireNonNull(runId, "runId");
    return jdbcTemplate.query(SELECT_BY_RUN_SQL, rowMapper(), runId);
  }

  private RowMapper<ReflectionEntry> rowMapper() {
    return (rs, rowNum) ->
        ReflectionEntry.of(
            rs.getString("id"),
            rs.getString("run_id"),
            rs.getString("trace_id"),
            rs.getInt("step_index"),
            AgentRole.valueOf(rs.getString("agent_role")),
            ReflectionType.valueOf(rs.getString("type")),
            rs.getString("content"),
            rs.getTimestamp("created_at").toInstant(),
            rs.getString("payload_json"));
  }
}
