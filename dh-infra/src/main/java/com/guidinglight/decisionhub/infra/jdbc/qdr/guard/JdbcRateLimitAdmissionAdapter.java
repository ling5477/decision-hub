package com.guidinglight.decisionhub.infra.jdbc.qdr.guard;

import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionPort;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionResult;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PostgreSQL fixed-window multi-instance rate admission adapter。
 *
 * <p>Window identity只从PostgreSQL transaction time计算；conditional upsert依靠composite primary key和row
 * lock产生精确winner。任何store错误均返回独立fail-closed分类，不回退到JVM-local limiter。
 */
public final class JdbcRateLimitAdmissionAdapter implements RateLimitAdmissionPort {

  private static final String UPSERT_SQL =
      "with db_window as (select"
          + " to_timestamp(floor(extract(epoch from transaction_timestamp()) / ?) * ?) as window_start)"
          + " insert into dh_qdr7_rate_limit_bucket"
          + " (environment, endpoint, source, tenant_id, window_start, window_end,"
          + " window_seconds, limit_value, request_count, created_at, updated_at)"
          + " select ?, ?, ?, ?, window_start, window_start+(? * interval '1 second'),"
          + " ?, ?, 1, transaction_timestamp(), transaction_timestamp() from db_window"
          + " on conflict (environment, endpoint, source, tenant_id, window_start) do update"
          + " set request_count = dh_qdr7_rate_limit_bucket.request_count + 1,"
          + " updated_at = transaction_timestamp()"
          + " where dh_qdr7_rate_limit_bucket.limit_value = excluded.limit_value"
          + " and dh_qdr7_rate_limit_bucket.window_seconds = excluded.window_seconds"
          + " and dh_qdr7_rate_limit_bucket.window_end = excluded.window_end"
          + " and dh_qdr7_rate_limit_bucket.request_count < dh_qdr7_rate_limit_bucket.limit_value"
          + " returning window_start, window_end, transaction_timestamp() as db_now, request_count, limit_value";

  private static final String SELECT_EXACT_SQL =
      "select window_start, window_end, window_seconds, request_count, limit_value"
          + " from dh_qdr7_rate_limit_bucket"
          + " where environment = ? and endpoint = ? and source = ? and tenant_id = ?"
          + " and window_start = ?";

  private final JdbcTemplate jdbcTemplate;

  /** @param jdbcTemplate DH-owned PostgreSQL JdbcTemplate。 */
  public JdbcRateLimitAdmissionAdapter(final JdbcTemplate jdbcTemplate) {
    this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate");
  }

  @Override
  public RateLimitAdmissionResult tryAcquire(final RateLimitAdmissionCommand command) {
    final RateLimitAdmissionCommand checked = Objects.requireNonNull(command, "command");
    try {
      final List<RateLimitAdmissionResult> accepted =
          jdbcTemplate.query(
              UPSERT_SQL,
              (rs, rowNum) ->
                  new RateLimitAdmissionResult(
                      RateLimitAdmissionStatus.ACCEPTED,
                      rs.getTimestamp("window_start").toInstant(),
                      rs.getTimestamp("window_end").toInstant(),
                      rs.getTimestamp("db_now").toInstant(),
                      rs.getLong("request_count"),
                      rs.getInt("limit_value")),
              checked.windowSeconds(),
              checked.windowSeconds(),
              checked.identity().environment(),
              checked.identity().endpoint(),
              checked.identity().source(),
              checked.identity().tenantId(),
              checked.windowSeconds(),
              checked.windowSeconds(),
              checked.limitValue());
      if (accepted.size() == 1) {
        return accepted.get(0);
      }
      if (!accepted.isEmpty()) {
        return invalid(null, null, checked.limitValue());
      }
      final List<Map<String, Object>> rows =
          jdbcTemplate.queryForList(
              SELECT_EXACT_SQL,
              checked.identity().environment(),
              checked.identity().endpoint(),
              checked.identity().source(),
              checked.identity().tenantId(),
              Timestamp.from(
                  accepted.isEmpty()
                      ? currentWindowStart(checked.windowSeconds())
                      : accepted.get(0).windowStart()));
      if (rows.size() != 1) {
        return invalid(null, null, checked.limitValue());
      }
      final Map<String, Object> row = rows.get(0);
      final long count = number(row, "request_count").longValue();
      final int storedLimit = number(row, "limit_value").intValue();
      final int storedWindow = number(row, "window_seconds").intValue();
      final Instant windowStart = timestamp(row.get("window_start"));
      final Instant storedEnd = timestamp(row.get("window_end"));
      final Instant windowEnd = windowStart.plusSeconds(storedWindow);
      final Instant databaseNow = databaseNow();
      if (storedLimit != checked.limitValue()
          || storedWindow != checked.windowSeconds()
          || !storedEnd.equals(windowEnd)) {
        return invalid(windowStart, windowEnd, storedLimit);
      }
      return new RateLimitAdmissionResult(
          RateLimitAdmissionStatus.RATE_LIMITED,
          windowStart,
          windowEnd,
          databaseNow,
          count,
          storedLimit);
    } catch (final DataAccessException error) {
      return RateLimitAdmissionResult.storeUnavailable();
    }
  }

  private Instant currentWindowStart(final int windowSeconds) {
    return jdbcTemplate.queryForObject(
        "select to_timestamp(floor(extract(epoch from transaction_timestamp()) / ?) * ?)",
        (rs, rowNum) -> rs.getTimestamp(1).toInstant(),
        windowSeconds,
        windowSeconds);
  }

  private Instant databaseNow() {
    return jdbcTemplate.queryForObject(
        "select transaction_timestamp()",
        (rs, rowNum) -> rs.getTimestamp(1).toInstant());
  }

  private static RateLimitAdmissionResult invalid(
      final Instant start, final Instant end, final int limit) {
    return new RateLimitAdmissionResult(
        RateLimitAdmissionStatus.CONFIGURATION_INVALID, start, end, null, null, limit);
  }

  private static Instant timestamp(final Object value) {
    if (value instanceof Timestamp timestamp) {
      return timestamp.toInstant();
    }
    if (value instanceof Instant instant) {
      return instant;
    }
    throw new IllegalStateException("database timestamp has unsupported type");
  }

  private static Number number(final Map<String, Object> row, final String key) {
    final Object value = row.get(key);
    if (value instanceof Number number) {
      return number;
    }
    throw new IllegalStateException("database counter has unsupported type");
  }
}
