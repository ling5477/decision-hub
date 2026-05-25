package com.guidinglight.decisionhub.infra.jdbc;

import com.guidinglight.decisionhub.domain.checkpoint.CheckpointEntry;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointStatus;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointType;
import com.guidinglight.decisionhub.usecase.agent.CheckpointEntryRepository;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Stage2-PoC-B5：CheckpointEntryRepository 的 JDBC 实现。
 *
 * <p>JSON 列写入采用 {@code CAST(? AS jsonb)}；TIMESTAMPTZ 列采用 {@link Timestamp#from(Instant)} 注入。
 * 同一 runId 内 checkpointIndex 唯一（由 V3 迁移 {@code ux_dh_checkpoint_entries_index} 兜底）。
 */
public final class JdbcCheckpointEntryRepository implements CheckpointEntryRepository {

  private static final String INSERT_SQL =
      "insert into dh_checkpoint_entries"
          + " (id, run_id, trace_id, checkpoint_index, type, status, snapshot_json, created_at)"
          + " values (?, ?, ?, ?, ?, ?, CAST(? AS jsonb), ?)";

  private static final String SELECT_BY_RUN_SQL =
      "select id, run_id, trace_id, checkpoint_index, type, status, snapshot_json, created_at"
          + " from dh_checkpoint_entries"
          + " where run_id = ?"
          + " order by checkpoint_index asc";

  private final JdbcTemplate jdbcTemplate;

  /** 构造。 */
  public JdbcCheckpointEntryRepository(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public void save(final CheckpointEntry entry) {
    Objects.requireNonNull(entry, "entry");
    jdbcTemplate.update(
        INSERT_SQL,
        entry.getCheckpointId(),
        entry.getRunId(),
        entry.getTraceId(),
        entry.getCheckpointIndex(),
        entry.getType().name(),
        entry.getStatus().name(),
        entry.getSnapshotJson(),
        Timestamp.from(entry.getCreatedAt()));
  }

  @Override
  public List<CheckpointEntry> listByRun(final String runId) {
    Objects.requireNonNull(runId, "runId");
    return jdbcTemplate.query(SELECT_BY_RUN_SQL, rowMapper(), runId);
  }

  private RowMapper<CheckpointEntry> rowMapper() {
    return (rs, rowNum) ->
        CheckpointEntry.of(
            rs.getString("id"),
            rs.getString("run_id"),
            rs.getString("trace_id"),
            rs.getInt("checkpoint_index"),
            CheckpointType.valueOf(rs.getString("type")),
            CheckpointStatus.valueOf(rs.getString("status")),
            rs.getString("snapshot_json"),
            rs.getTimestamp("created_at").toInstant());
  }
}
