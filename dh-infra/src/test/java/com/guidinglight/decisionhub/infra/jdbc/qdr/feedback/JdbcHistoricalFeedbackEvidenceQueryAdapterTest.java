package com.guidinglight.decisionhub.infra.jdbc.qdr.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceErrorCode;
import com.guidinglight.decisionhub.usecase.qdr.feedback.FeedbackPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidencePage;
import com.guidinglight.decisionhub.usecase.qdr.feedback.HistoricalFeedbackEvidenceQuery;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/** B3 keyset, isolation, aggregate assembly and reconnect coverage on PostgreSQL 17.10 Testcontainers. */
@Testcontainers(disabledWithoutDocker = true)
class JdbcHistoricalFeedbackEvidenceQueryAdapterTest {

  private static final Instant FROM = Instant.parse("2026-07-01T00:00:00Z");
  private static final Instant TO = Instant.parse("2026-07-31T00:00:00Z");
  @Container
  static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:17"));
  private JdbcTemplate jdbc;
  private JdbcHistoricalFeedbackEvidenceQueryAdapter adapter;

  @BeforeEach
  void reset() {
    final DataSource source = source();
    jdbc = new JdbcTemplate(source);
    jdbc.execute("drop table if exists qdr_feedback_attribution_reference cascade");
    jdbc.execute("drop table if exists qdr_feedback_attribution_contribution cascade");
    jdbc.execute("drop table if exists qdr_feedback_attribution cascade");
    jdbc.execute("drop table if exists qdr_feedback_outcome_observation cascade");
    migration();
    adapter = new JdbcHistoricalFeedbackEvidenceQueryAdapter(jdbc, new DataSourceTransactionManager(source));
  }

  @Test
  void readsMoreThanTwoHundredCompleteAggregatesWithStableKeysetOrderAndNoDuplicates() {
    for (int index = 0; index < 201; index++) { insert("tenant-a", "DEV", index, FROM.plusSeconds(index)); }
    final List<String> received = new ArrayList<>();
    HistoricalFeedbackEvidenceQuery.Cursor cursor = null;
    do {
      final HistoricalFeedbackEvidencePage page = adapter.query(query("tenant-a", FeedbackEnvironment.DEV, 50, cursor));
      received.addAll(page.items().stream().map(item -> item.attributionId()).toList());
      cursor = page.nextCursor().orElse(null);
    } while (cursor != null);
    assertThat(received).hasSize(201).doesNotHaveDuplicates();
    assertThat(received).isSortedAccordingTo(java.util.Comparator.reverseOrder());
    assertThat(new HashSet<>(received)).hasSize(201);
  }

  @Test
  void isolatesTenantAndEnvironmentRejectsForeignCursorAndKeepsNewerInsertOutOfContinuation() {
    insert("tenant-a", "DEV", 1, FROM.plusSeconds(1));
    insert("tenant-a", "DEV", 2, FROM.plusSeconds(2));
    insert("tenant-b", "DEV", 2, FROM.plusSeconds(2));
    insert("tenant-a", "TEST", 2, FROM.plusSeconds(2));
    final HistoricalFeedbackEvidencePage first = adapter.query(query("tenant-a", FeedbackEnvironment.DEV, 1, null));
    insert("tenant-a", "DEV", 9, TO.minusSeconds(1));
    final HistoricalFeedbackEvidencePage next = adapter.query(query("tenant-a", FeedbackEnvironment.DEV, 1, first.nextCursor().orElseThrow()));
    assertThat(first.items()).hasSize(1);
    assertThat(next.items()).hasSize(1);
    assertThat(next.items().getFirst().attributionId()).isNotEqualTo(first.items().getFirst().attributionId());
    assertThat(next.items().getFirst().attributionId()).isNotEqualTo(hash(9));
    assertThatThrownBy(() -> query("tenant-b", FeedbackEnvironment.DEV, 1, first.nextCursor().orElseThrow()))
        .isInstanceOf(HistoricalFeedbackEvidenceQuery.ValidationException.class);
  }

  @Test
  void failsClosedWhenARequiredChildIsMissingAndWorksAfterAdapterReconnect() {
    insert("tenant-a", "DEV", 1, FROM.plusSeconds(1));
    jdbc.update("delete from qdr_feedback_attribution_reference where tenant_id=? and environment=? and attribution_id=?", "tenant-a", "DEV", hash(1));
    assertThatThrownBy(() -> adapter.query(query("tenant-a", FeedbackEnvironment.DEV, 10, null)))
        .isInstanceOf(FeedbackPersistenceException.class)
        .extracting(error -> ((FeedbackPersistenceException) error).errorCode())
        .isEqualTo(FeedbackPersistenceErrorCode.AGGREGATE_INCOMPLETE);
    jdbc.update("insert into qdr_feedback_attribution_reference (id,tenant_id,environment,attribution_id,reference_type,reference_value,reference_status) values (?,?,?,?,?,?,?)", UUID.randomUUID(), "tenant-a", "DEV", hash(1), "EVIDENCE", "evidence:item-1", "ACTIVE");
    insert("tenant-a", "DEV", 2, FROM.plusSeconds(2));
    final DataSource reconnected = source();
    final var reconnectedAdapter = new JdbcHistoricalFeedbackEvidenceQueryAdapter(new JdbcTemplate(reconnected), new DataSourceTransactionManager(reconnected));
    assertThat(reconnectedAdapter.query(query("tenant-a", FeedbackEnvironment.DEV, 10, null)).items()).hasSize(2);
  }

  private HistoricalFeedbackEvidenceQuery query(final String tenant, final FeedbackEnvironment environment, final int size, final HistoricalFeedbackEvidenceQuery.Cursor cursor) {
    return new HistoricalFeedbackEvidenceQuery(tenant, environment, FROM, TO, size, cursor, null, null, null, null, null, null, null);
  }

  private void insert(final String tenant, final String environment, final int index, final Instant observedAt) {
    final String value = hash(index);
    final String observation = "observation-" + index;
    jdbc.update("insert into qdr_feedback_outcome_observation (id,tenant_id,environment,decision_id,trace_id,observation_id,idempotency_key,outcome_source,outcome_status,observed_at,evaluation_time,canonical_hash) values (?,?,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), tenant, environment, "decision-" + index, "trace-" + index, observation, hash(index + 1000), "STRUCTURED_TEST_FIXTURE", "SUCCEEDED", java.sql.Timestamp.from(observedAt), java.sql.Timestamp.from(observedAt.plusSeconds(1)), hash(index + 2000));
    jdbc.update("insert into qdr_feedback_attribution (id,tenant_id,environment,observation_id,decision_id,trace_id,observed_at,attribution_id,policy_id,policy_version,attribution_status,confidence,canonical_hash,error_code) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), tenant, environment, observation, "decision-" + index, "trace-" + index, java.sql.Timestamp.from(observedAt), value, "policy-a", "v1", "ATTRIBUTED", java.math.BigDecimal.ONE, hash(index + 2000), "NONE");
    jdbc.update("insert into qdr_feedback_attribution_contribution (id,tenant_id,environment,attribution_id,dimension,measurement,contribution,impact,confidence,reason_code,evidence_ref,sort_order) values (?,?,?,?,?,?,?,?,?,?,?,?)", UUID.randomUUID(), tenant, environment, value, "EVIDENCE_QUALITY", java.math.BigDecimal.ONE, java.math.BigDecimal.ONE, "POSITIVE", java.math.BigDecimal.ONE, "TEST", "evidence:item-" + index, 0);
    jdbc.update("insert into qdr_feedback_attribution_reference (id,tenant_id,environment,attribution_id,reference_type,reference_value,reference_status) values (?,?,?,?,?,?,?)", UUID.randomUUID(), tenant, environment, value, "EVIDENCE", "evidence:item-" + index, "ACTIVE");
  }

  private DataSource source() { final DriverManagerDataSource dataSource = new DriverManagerDataSource(); dataSource.setUrl(POSTGRES.getJdbcUrl()); dataSource.setUsername(POSTGRES.getUsername()); dataSource.setPassword(POSTGRES.getPassword()); return dataSource; }
  private void migration() { try { final Path path = Path.of("..", "dh-app", "src", "main", "resources", "db", "migration", "V15__qdr9_structured_feedback_persistence.sql"); ScriptUtils.executeSqlScript(jdbc.getDataSource().getConnection(), new ByteArrayResource(Files.readString(path, StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8))); } catch (IOException | java.sql.SQLException error) { throw new IllegalStateException(error); } }
  private static String hash(final int value) { return String.format("%064x", value); }
}
