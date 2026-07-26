package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import static org.assertj.core.api.Assertions.assertThat;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackIntegrityReport;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackRetentionCommand;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** PostgreSQL regression tests for the bounded, read-only feedback integrity report. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcFeedbackIntegrityAdapterTest {

  private static final Instant NOW = Instant.parse("2026-07-26T12:00:00Z");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"));

  private JdbcTemplate jdbc;
  private JdbcFeedbackIntegrityAdapter adapter;

  @BeforeEach
  void resetSchema() {
    jdbc = new JdbcTemplate(dataSource());
    jdbc.execute("drop schema public cascade");
    jdbc.execute("create schema public");
    executeV15();
    adapter = new JdbcFeedbackIntegrityAdapter(jdbc);
  }

  @AfterAll
  static void stopContainer() {
    POSTGRES.stop();
  }

  @Test
  void completeAggregateIsReportedWithoutMutation() {
    insertCompleteAggregate("complete", 0, "EVIDENCE", "ACTIVE");

    final FeedbackIntegrityReport report = adapter.inspect(command(), NOW.minus(Duration.ofDays(365)));

    assertThat(report).isEqualTo(new FeedbackIntegrityReport(1, 1, 0));
    assertThat(count("qdr_feedback_outcome_observation")).isEqualTo(1);
    assertThat(count("qdr_feedback_attribution")).isEqualTo(1);
    assertThat(count("qdr_feedback_attribution_contribution")).isEqualTo(1);
    assertThat(count("qdr_feedback_attribution_reference")).isEqualTo(1);
  }

  @Test
  void missingAttributionSortGapAndActiveHoldAreReportedAsBlocked() {
    insertObservationOnly("missing-attribution");
    insertCompleteAggregate("sort-gap", 1, "EVIDENCE", "ACTIVE");
    insertCompleteAggregate("active-audit", 0, "AUDIT", "ACTIVE");

    final FeedbackIntegrityReport report = adapter.inspect(command(), NOW.minus(Duration.ofDays(365)));

    assertThat(report).isEqualTo(new FeedbackIntegrityReport(3, 0, 3));
  }

  private static FeedbackRetentionCommand command() {
    return new FeedbackRetentionCommand(
        "tenant-a", FeedbackEnvironment.DEV, true, Duration.ofDays(365), 100);
  }

  private void insertCompleteAggregate(
      final String suffix, final int sortOrder, final String referenceType, final String referenceStatus) {
    final String observationId = "observation-" + suffix;
    final String attributionId = hash("attribution-" + suffix);
    final String canonicalHash = hash("canonical-" + suffix);
    insertObservation(observationId, canonicalHash);
    jdbc.update(
        "insert into qdr_feedback_attribution"
            + " (id,tenant_id,environment,observation_id,decision_id,trace_id,observed_at,attribution_id,"
            + " policy_id,policy_version,attribution_status,confidence,canonical_hash,error_code)"
            + " values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
        uuid("attribution-row-" + suffix),
        "tenant-a",
        "DEV",
        observationId,
        "decision-a",
        "trace-a",
        Timestamp.from(NOW.minus(Duration.ofDays(366))),
        attributionId,
        "policy-a",
        "v1",
        "ATTRIBUTED",
        new java.math.BigDecimal("0.5"),
        canonicalHash,
        "SUCCESS");
    jdbc.update(
        "insert into qdr_feedback_attribution_contribution"
            + " (id,tenant_id,environment,attribution_id,dimension,measurement,contribution,impact,confidence,"
            + " reason_code,evidence_ref,sort_order) values (?,?,?,?,?,?,?,?,?,?,?,?)",
        uuid("contribution-" + suffix),
        "tenant-a",
        "DEV",
        attributionId,
        "EVIDENCE_QUALITY",
        new java.math.BigDecimal("0.5"),
        new java.math.BigDecimal("0.5"),
        "POSITIVE",
        new java.math.BigDecimal("0.5"),
        "RETENTION_TEST",
        "evidence:test",
        sortOrder);
    jdbc.update(
        "insert into qdr_feedback_attribution_reference"
            + " (id,tenant_id,environment,attribution_id,reference_type,reference_value,reference_status)"
            + " values (?,?,?,?,?,?,?)",
        uuid("reference-" + suffix),
        "tenant-a",
        "DEV",
        attributionId,
        referenceType,
        referenceType.equals("AUDIT") ? "audit:audit-a" : "evidence:test",
        referenceStatus);
  }

  private void insertObservationOnly(final String suffix) {
    insertObservation("observation-" + suffix, hash("canonical-" + suffix));
  }

  private void insertObservation(final String observationId, final String canonicalHash) {
    jdbc.update(
        "insert into qdr_feedback_outcome_observation"
            + " (id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,outcome_source,"
            + " outcome_status,observed_at,evaluation_time,canonical_hash) values (?,?,?,?,?,?,?,?,?,?,?,?)",
        uuid("observation-row-" + observationId),
        "tenant-a",
        "DEV",
        "decision-a",
        "trace-a",
        observationId,
        hash("idempotency-" + observationId),
        "STRUCTURED_TEST_FIXTURE",
        "SUCCEEDED",
        Timestamp.from(NOW.minus(Duration.ofDays(366))),
        Timestamp.from(NOW),
        canonicalHash);
  }

  private int count(final String table) {
    return jdbc.queryForObject("select count(*) from " + table, Integer.class);
  }

  private void executeV15() {
    try {
      final Path path =
          Path.of(
              "..", "dh-app", "src", "main", "resources", "db", "migration",
              "V15__qdr9_structured_feedback_persistence.sql");
      ScriptUtils.executeSqlScript(
          jdbc.getDataSource().getConnection(),
          new ByteArrayResource(Files.readString(path).getBytes(StandardCharsets.UTF_8)));
    } catch (final Exception error) {
      throw new IllegalStateException("failed to execute V15 test migration", error);
    }
  }

  private static DriverManagerDataSource dataSource() {
    final DriverManagerDataSource source = new DriverManagerDataSource();
    source.setDriverClassName("org.postgresql.Driver");
    source.setUrl(POSTGRES.getJdbcUrl());
    source.setUsername(POSTGRES.getUsername());
    source.setPassword(POSTGRES.getPassword());
    return source;
  }

  private static UUID uuid(final String value) {
    return UUID.nameUUIDFromBytes(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String hash(final String value) {
    return String.format("%064x", value.hashCode() & 0xffffffffL);
  }
}
