package com.guidinglight.decisionhub.infra.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockingDetails;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionTransactionException;
import java.sql.Connection;
import java.time.Instant;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.invocation.Invocation;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.DefaultTransactionStatus;

/** JDBC conflict SQL、required transaction、failure classification 与 resource identity 回归。 */
@ExtendWith(MockitoExtension.class)
class JdbcNqFeedbackEventRepositoryTest {

  @Mock private JdbcTemplate jdbcTemplate;
  @Mock private DataSource dataSource;
  @Mock private Connection connection;
  private DataSourceTransactionManager transactionManager;
  private JdbcNqFeedbackEventRepository repository;

  @BeforeEach
  void setUp() throws Exception {
    when(jdbcTemplate.getDataSource()).thenReturn(dataSource);
    lenient().when(dataSource.getConnection()).thenReturn(connection);
    lenient().when(connection.getAutoCommit()).thenReturn(true);
    lenient()
        .when(connection.getTransactionIsolation())
        .thenReturn(Connection.TRANSACTION_READ_COMMITTED);
    transactionManager = new DataSourceTransactionManager(dataSource);
    repository =
        new JdbcNqFeedbackEventRepository(
            jdbcTemplate, new ObjectMapper(), transactionManager);
  }

  @Test
  void saveEnvelopeFirstWriteUsesConflictSafeJsonbInsert() {
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(1);

    assertThat(repository.saveEnvelope(sampleEnvelope("evt-001"))).isTrue();

    assertThat(captureUpdateSql(jdbcTemplate))
        .contains("insert into dh_nq_feedback_events")
        .contains("CAST(? AS jsonb)")
        .contains("on conflict do nothing");
  }

  @Test
  void saveEnvelopeConflictReadsBackInSameCallAndReturnsDuplicate() {
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("evt-dup")))
        .thenReturn(List.of(sampleEnvelope("evt-dup")));

    assertThat(repository.saveEnvelope(sampleEnvelope("evt-dup"))).isFalse();
    assertThat(updateInvocations(jdbcTemplate)).isOne();
  }

  @Test
  void saveEnvelopeConflictWithoutReadbackFailsClosed() {
    when(jdbcTemplate.update(anyString(), any(Object[].class))).thenReturn(0);
    when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq("evt-missing")))
        .thenReturn(List.of());

    assertThatThrownBy(() -> repository.saveEnvelope(sampleEnvelope("evt-missing")))
        .isInstanceOfSatisfying(
            NqFeedbackIngestionTransactionException.class,
            failure ->
                assertThat(failure.errorCode())
                    .isEqualTo(
                        NqFeedbackIngestionTransactionException.ErrorCode.PERSISTENCE_FAILURE));
  }

  @Test
  void requiredUsesTransactionAndCommitsAfterAction() throws Exception {
    assertThat(repository.required(() -> "complete")).isEqualTo("complete");

    verify(connection).setAutoCommit(false);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
  }

  @Test
  void callbackFailurePreservesCauseAndRollsBack() throws Exception {
    final IllegalStateException original = new IllegalStateException("controlled action failure");

    assertThatThrownBy(
            () ->
                repository.required(
                    () -> {
                      throw original;
                    }))
        .isSameAs(original);
    verify(connection).rollback();
  }

  @Test
  void callbackTransactionSystemExceptionIsNotMisclassifiedAsCommitUnknown() {
    final TransactionSystemException original =
        new TransactionSystemException("controlled callback failure");

    assertThatThrownBy(
            () ->
                repository.required(
                    () -> {
                      throw original;
                    }))
        .isSameAs(original);
  }

  @Test
  void commitPhaseFailureIsClassifiedUnknownWithoutAutomaticRetry() {
    final JdbcNqFeedbackEventRepository uncertain =
        new JdbcNqFeedbackEventRepository(
            jdbcTemplate, new ObjectMapper(), new CommitUnknownManager(dataSource));
    final int[] attempts = {0};

    assertThatThrownBy(
            () ->
                uncertain.required(
                    () -> {
                      attempts[0]++;
                      return "complete";
                    }))
        .isInstanceOfSatisfying(
            NqFeedbackIngestionTransactionException.class,
            failure -> {
              assertThat(failure.errorCode())
                  .isEqualTo(
                      NqFeedbackIngestionTransactionException.ErrorCode.COMMIT_OUTCOME_UNKNOWN);
              assertThat(failure.getCause()).isInstanceOf(TransactionSystemException.class);
            });
    assertThat(attempts[0]).isOne();
  }

  @Test
  void dataSourceMismatchFailsFast() {
    final DataSource other = mock(DataSource.class);

    assertThatThrownBy(
            () ->
                new JdbcNqFeedbackEventRepository(
                    jdbcTemplate, new ObjectMapper(), new DataSourceTransactionManager(other)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("does not match");
  }

  @Test
  void findEnvelopeByEventIdReturnsEmptyForNull() {
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
        .filter(invocation -> "update".equals(invocation.getMethod().getName()))
        .map(Invocation::getArguments)
        .map(arguments -> (String) arguments[0])
        .findFirst()
        .orElseThrow(() -> new AssertionError("expected JdbcTemplate.update(...)"));
  }

  private static long updateInvocations(final JdbcTemplate template) {
    return mockingDetails(template).getInvocations().stream()
        .filter(invocation -> "update".equals(invocation.getMethod().getName()))
        .count();
  }

  private static final class CommitUnknownManager extends DataSourceTransactionManager {
    private CommitUnknownManager(final DataSource dataSource) {
      super(dataSource);
    }

    @Override
    protected void doCommit(final DefaultTransactionStatus status) {
      throw new TransactionSystemException("controlled commit outcome unknown");
    }

    @Override
    protected void doBegin(
        final Object transaction, final TransactionDefinition definition) {
      super.doBegin(transaction, definition);
    }
  }
}
