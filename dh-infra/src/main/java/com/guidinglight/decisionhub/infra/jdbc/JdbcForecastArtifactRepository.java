package com.guidinglight.decisionhub.infra.jdbc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.guidinglight.decisionhub.connector.tools.ForecastArtifactStore;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifact;
import com.guidinglight.decisionhub.domain.forecast.ForecastArtifactStatus;
import com.guidinglight.decisionhub.domain.forecast.ForecastHorizon;
import com.guidinglight.decisionhub.domain.forecast.ForecastPoint;
import com.guidinglight.decisionhub.domain.forecast.ForecastTarget;
import com.fasterxml.jackson.databind.JsonNode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

/**
 * Stage2-PoC-B5：ForecastArtifactStore 的 JDBC 实现。
 *
 * <p>JSON 列写入采用 {@code CAST(? AS jsonb)}；TIMESTAMPTZ 列采用 {@link Timestamp#from(Instant)} 注入。
 * predictionsJson 用 Jackson 序列化 / 反序列化；rawPayloadJson 字段保留外部 adapter 原始返回，禁止丢失。
 */
public final class JdbcForecastArtifactRepository implements ForecastArtifactStore {

  private static final String INSERT_SQL =
      "insert into dh_forecast_artifacts"
          + " (id, trace_id, symbol, horizon, target, predictions_json, model_version,"
          + "  generated_at, status, raw_payload_json)"
          + " values (?, ?, ?, ?, ?, CAST(? AS jsonb), ?, ?, ?, CAST(? AS jsonb))";

  private static final String SELECT_BY_ID_SQL = baseSelect() + " where id = ?";
  private static final String SELECT_BY_TRACE_SQL =
      baseSelect() + " where trace_id = ? order by generated_at asc";
  private static final String SELECT_BY_SYMBOL_SQL =
      baseSelect() + " where symbol = ? order by generated_at asc";

  private final JdbcTemplate jdbcTemplate;
  private final ObjectMapper objectMapper;

  /** 构造。 */
  public JdbcForecastArtifactRepository(
      final JdbcTemplate jdbcTemplate, final ObjectMapper objectMapper) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
    this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
  }

  @Override
  public void save(final ForecastArtifact artifact) {
    Objects.requireNonNull(artifact, "artifact");
    jdbcTemplate.update(
        INSERT_SQL,
        artifact.getArtifactId(),
        artifact.getTraceId(),
        artifact.getSymbol(),
        artifact.getHorizon().name(),
        artifact.getTarget().name(),
        writePredictions(artifact.getPredictions()),
        artifact.getModelVersion(),
        Timestamp.from(artifact.getGeneratedAt()),
        artifact.getStatus().name(),
        artifact.getRawPayloadJson());
  }

  @Override
  public Optional<ForecastArtifact> findById(final String artifactId) {
    if (artifactId == null) {
      return Optional.empty();
    }
    final List<ForecastArtifact> rows = jdbcTemplate.query(SELECT_BY_ID_SQL, rowMapper(), artifactId);
    return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
  }

  @Override
  public List<ForecastArtifact> findByTraceId(final String traceId) {
    if (traceId == null) {
      return List.of();
    }
    return jdbcTemplate.query(SELECT_BY_TRACE_SQL, rowMapper(), traceId);
  }

  @Override
  public List<ForecastArtifact> findBySymbol(final String symbol) {
    if (symbol == null) {
      return List.of();
    }
    return jdbcTemplate.query(SELECT_BY_SYMBOL_SQL, rowMapper(), symbol);
  }

  private static String baseSelect() {
    return "select id, trace_id, symbol, horizon, target, predictions_json, model_version,"
        + " generated_at, status, raw_payload_json from dh_forecast_artifacts";
  }

  private RowMapper<ForecastArtifact> rowMapper() {
    return (rs, rowNum) ->
        ForecastArtifact.of(
            rs.getString("id"),
            rs.getString("trace_id"),
            rs.getString("symbol"),
            ForecastHorizon.valueOf(rs.getString("horizon")),
            ForecastTarget.valueOf(rs.getString("target")),
            readPredictions(rs.getString("predictions_json")),
            rs.getString("model_version"),
            rs.getTimestamp("generated_at").toInstant(),
            ForecastArtifactStatus.valueOf(rs.getString("status")),
            rs.getString("raw_payload_json"));
  }

  private String writePredictions(final List<ForecastPoint> points) {
    final ArrayNode array = objectMapper.createArrayNode();
    if (points != null) {
      for (ForecastPoint point : points) {
        final ObjectNode node = objectMapper.createObjectNode();
        node.put("date", point.getDate().toString());
        node.put("value", point.getValue());
        node.put("confidence", point.getConfidence());
        array.add(node);
      }
    }
    try {
      return objectMapper.writeValueAsString(array);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("failed to serialize predictionsJson", e);
    }
  }

  private List<ForecastPoint> readPredictions(final String json) {
    if (json == null || json.isBlank()) {
      return List.of();
    }
    try {
      final JsonNode root = objectMapper.readTree(json);
      if (!root.isArray()) {
        return List.of();
      }
      final List<ForecastPoint> out = new ArrayList<>();
      for (JsonNode node : root) {
        out.add(
            ForecastPoint.of(
                LocalDate.parse(node.get("date").asText()),
                node.get("value").asDouble(),
                node.get("confidence").asDouble()));
      }
      return List.copyOf(out);
    } catch (Exception e) {
      throw new IllegalStateException("failed to deserialize predictionsJson", e);
    }
  }
}
