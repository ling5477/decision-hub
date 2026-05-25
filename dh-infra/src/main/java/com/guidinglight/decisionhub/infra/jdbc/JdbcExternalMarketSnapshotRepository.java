package com.guidinglight.decisionhub.infra.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.guidinglight.decisionhub.connector.research.ResearchSnapshotStore;
import com.guidinglight.decisionhub.domain.marketdata.ExternalMarketSnapshot;
import com.guidinglight.decisionhub.domain.marketdata.MarketDataSource;
import com.guidinglight.decisionhub.domain.marketdata.MarketSnapshotStatus;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Stage2-PoC-B5：ResearchSnapshotStore 的 JDBC 实现，对应表 {@code dh_external_market_snapshots}。
 *
 * <p>JSON 列写入采用 {@code CAST(? AS jsonb)}；TIMESTAMPTZ 列采用 {@link Timestamp#from(Instant)} 注入；
 * date 区间使用 {@link Date#valueOf(LocalDate)}。 symbolsJson 用 Jackson 序列化；rawPayloadJson / dataJson
 * 字段保留外部 adapter 原始返回，禁止丢失。
 */
public final class JdbcExternalMarketSnapshotRepository implements ResearchSnapshotStore {

  private static final String INSERT_SQL =
      "insert into dh_external_market_snapshots"
          + " (id, trace_id, symbols_json, source, range_start, range_end, fetched_at,"
          + "  data_json, source_version, status, raw_payload_json)"
          + " values (?, ?, CAST(? AS jsonb), ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, CAST(? AS jsonb))";

  private static final String SELECT_BY_ID_SQL = baseSelect() + " where id = ?";
  private static final String SELECT_BY_TRACE_SQL =
      baseSelect() + " where trace_id = ? order by fetched_at asc";
  private static final String SELECT_OVERLAP_SQL =
      baseSelect()
          + " where range_end >= ? and range_start <= ?"
          + " and symbols_json @> CAST(? AS jsonb)"
          + " order by fetched_at asc";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /** 构造。 */
  public JdbcExternalMarketSnapshotRepository(
      final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public void save(final ExternalMarketSnapshot snapshot) {
    Objects.requireNonNull(snapshot, "snapshot");
    jdbcTemplate.update(
        INSERT_SQL,
        snapshot.getSnapshotId(),
        snapshot.getTraceId(),
        writeSymbols(snapshot.getSymbols()),
        snapshot.getSource().name(),
        Date.valueOf(snapshot.getRangeStart()),
        Date.valueOf(snapshot.getRangeEnd()),
        Timestamp.from(snapshot.getFetchedAt()),
        snapshot.getDataJson(),
        snapshot.getSourceVersion(),
        snapshot.getStatus().name(),
        snapshot.getRawPayloadJson());
  }

  @Override
  public Optional<ExternalMarketSnapshot> findById(final String snapshotId) {
    if (snapshotId == null) {
      return Optional.empty();
    }
    final List<ExternalMarketSnapshot> rows =
        jdbcTemplate.query(SELECT_BY_ID_SQL, rowMapper(), snapshotId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  @Override
  public Optional<ExternalMarketSnapshot> findByTraceId(final String traceId) {
    if (traceId == null) {
      return Optional.empty();
    }
    final List<ExternalMarketSnapshot> rows =
        jdbcTemplate.query(SELECT_BY_TRACE_SQL, rowMapper(), traceId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  @Override
  public List<ExternalMarketSnapshot> findBySymbolAndDateRange(
      final String symbol, final LocalDate start, final LocalDate end) {
    Objects.requireNonNull(symbol, "symbol");
    Objects.requireNonNull(start, "start");
    Objects.requireNonNull(end, "end");
    if (end.isBefore(start)) {
      throw new IllegalArgumentException("end must not be before start");
    }
    final String symbolFilter = writeSymbols(List.of(symbol));
    return jdbcTemplate.query(
        SELECT_OVERLAP_SQL, rowMapper(), Date.valueOf(start), Date.valueOf(end), symbolFilter);
  }

  private static String baseSelect() {
    return "select id, trace_id, symbols_json, source, range_start, range_end, fetched_at,"
        + " data_json, source_version, status, raw_payload_json from dh_external_market_snapshots";
  }

  private RowMapper<ExternalMarketSnapshot> rowMapper() {
    return (rs, rowNum) ->
        ExternalMarketSnapshot.of(
            rs.getString("id"),
            rs.getString("trace_id"),
            readSymbols(rs.getString("symbols_json")),
            MarketDataSource.valueOf(rs.getString("source")),
            rs.getDate("range_start").toLocalDate(),
            rs.getDate("range_end").toLocalDate(),
            rs.getTimestamp("fetched_at").toInstant(),
            rs.getString("data_json"),
            rs.getString("source_version"),
            MarketSnapshotStatus.valueOf(rs.getString("status")),
            rs.getString("raw_payload_json"));
  }

  private String writeSymbols(final List<String> symbols) {
    final ArrayNode array = objectMapper.createArrayNode();
    if (symbols != null) {
      for (String s : symbols) {
        array.add(s);
      }
    }
    try {
      return objectMapper.writeValueAsString(array);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("failed to serialize symbolsJson", e);
    }
  }

  private List<String> readSymbols(final String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      final JsonNode root = objectMapper.readTree(json);
      if (!root.isArray()) {
        return List.of();
      }
      final List<String> out = new ArrayList<>();
      for (JsonNode node : root) {
        out.add(node.asText());
      }
      return List.copyOf(out);
    } catch (Exception e) {
      throw new IllegalStateException("failed to deserialize symbolsJson", e);
    }
  }
}
