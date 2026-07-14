package com.guidinglight.decisionhub.infra.jdbc.qdr.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.guidinglight.decisionhub.domain.qdr.model.PromptVersionStatus;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionChecksumConflictException;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.SavePromptVersionCommand;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * {@link JdbcPromptVersionRepository} 的真实 PostgreSQL 原子 bootstrap 回归。
 *
 * <p>每个保存操作都在独立事务中执行，覆盖 fresh tenant 的并发唯一键竞争、immutable semantic conflict 与 tenant
 * isolation。测试只使用一次性 Testcontainers PostgreSQL，不访问外部 HTTP、provider、NQ 或 LIVE。
 */
@Testcontainers(disabledWithoutDocker = true)
final class JdbcPromptVersionRepositoryPostgresTest {

  private static final String HASH_A = "a".repeat(64);
  private static final String HASH_B = "b".repeat(64);
  private static final Instant CREATED_AT = Instant.parse("2026-07-14T00:00:00Z");

  @Container
  static final PostgreSQLContainer<?> POSTGRES =
      new PostgreSQLContainer<>("postgres:17")
          .withDatabaseName("prompt_bootstrap")
          .withUsername("prompt_bootstrap")
          .withPassword("prompt_bootstrap");

  private static HikariDataSource dataSource;
  private static JdbcTemplate jdbc;
  private static TransactionTemplate transaction;
  private static JdbcPromptVersionRepository repository;

  @BeforeAll
  static void createSchema() {
    final HikariConfig hikari = new HikariConfig();
    hikari.setJdbcUrl(POSTGRES.getJdbcUrl());
    hikari.setUsername(POSTGRES.getUsername());
    hikari.setPassword(POSTGRES.getPassword());
    hikari.setMaximumPoolSize(8);
    hikari.setMinimumIdle(1);
    hikari.setConnectionTimeout(TimeUnit.SECONDS.toMillis(10));
    hikari.setPoolName("prompt-version-bootstrap-test");
    dataSource = new HikariDataSource(hikari);
    jdbc = new JdbcTemplate(dataSource);
    transaction = new TransactionTemplate(new DataSourceTransactionManager(dataSource));
    repository = new JdbcPromptVersionRepository(jdbc);
    jdbc.execute(
        "create table qdr_prompt_template ("
            + "id uuid primary key, tenant_id varchar not null, template_key varchar not null,"
            + "display_name varchar not null, current_version_id uuid, status varchar not null,"
            + "created_at timestamptz not null, updated_at timestamptz not null,"
            + "constraint ux_prompt_template_tenant_key unique (tenant_id,template_key))");
    jdbc.execute(
        "create table qdr_prompt_version ("
            + "id uuid primary key, tenant_id varchar not null, prompt_template_id uuid not null,"
            + "version varchar not null, render_policy_key varchar not null, template_ref varchar not null,"
            + "template_hash varchar(64) not null, redacted_summary text not null, status varchar not null,"
            + "checksum varchar(64) not null, created_at timestamptz not null, created_by varchar not null,"
            + "constraint ux_prompt_version_tenant_template_version"
            + " unique (tenant_id,prompt_template_id,version),"
            + "constraint ux_prompt_version_tenant_id unique (tenant_id,id),"
            + "constraint fk_prompt_version_template foreign key (prompt_template_id)"
            + " references qdr_prompt_template(id))");
  }

  @AfterAll
  static void closeDataSource() {
    if (dataSource != null) {
      dataSource.close();
    }
  }

  @BeforeEach
  void clearRows() {
    jdbc.execute("truncate table qdr_prompt_version, qdr_prompt_template");
  }

  @Test
  void freshTenantConcurrentBootstrapReturnsOneCanonicalDefinitionForThreeRounds()
      throws Exception {
    for (int round = 1; round <= 3; round++) {
      final SavePromptVersionCommand command = command("tenant-concurrent-" + round, "render-v1");
      final List<PromptVersionRecord> results = runConcurrent(command, 8, 40);

      assertThat(results).hasSize(40);
      assertThat(results)
          .allSatisfy(
              result -> {
                assertThat(result.promptTemplateId()).isEqualTo(command.promptTemplateId());
                assertThat(result.promptVersionId()).isEqualTo(command.promptVersionId());
                assertThat(result.tenantId()).isEqualTo(command.tenantId());
                assertThat(result.renderPolicyKey()).isEqualTo(command.renderPolicyKey());
                assertThat(result.templateHash()).isEqualTo(command.templateHash());
                assertThat(result.checksum()).isEqualTo(command.checksum());
                assertThat(result.createdAt()).isEqualTo(CREATED_AT);
              });
      assertThat(
              jdbc.queryForObject(
                  "select count(*) from qdr_prompt_version where tenant_id=?",
                  Integer.class,
                  command.tenantId()))
          .isEqualTo(1);
    }
  }

  @Test
  void sameStableIdentityWithDifferentSemanticContentFailsClosedWithoutAbortingTransaction() {
    final SavePromptVersionCommand canonical = command("tenant-conflict", "render-v1");
    final SavePromptVersionCommand conflicting =
        new SavePromptVersionCommand(
            canonical.promptTemplateId(),
            canonical.promptVersionId(),
            canonical.tenantId(),
            canonical.templateKey(),
            canonical.templateDisplayName(),
            canonical.version(),
            "render-v2",
            canonical.templateRef(),
            HASH_B,
            canonical.redactedSummary(),
            canonical.status(),
            canonical.checksum(),
            CREATED_AT.plusSeconds(1),
            "other-system");

    transaction.executeWithoutResult(
        status -> {
          repository.save(canonical);
          assertThatThrownBy(() -> repository.save(conflicting))
              .isInstanceOf(PromptVersionChecksumConflictException.class)
              .hasMessageContaining("semantic/checksum conflict");
          assertThat(jdbc.queryForObject("select 1", Integer.class)).isEqualTo(1);
        });

    final PromptVersionRecord stored =
        repository
            .findByTenantAndTemplateVersion(
                canonical.tenantId(), canonical.promptTemplateId(), canonical.version())
            .orElseThrow();
    assertThat(stored.renderPolicyKey()).isEqualTo(canonical.renderPolicyKey());
    assertThat(stored.templateHash()).isEqualTo(canonical.templateHash());
    assertThat(stored.createdAt()).isEqualTo(CREATED_AT);
  }

  @Test
  void distinctTenantsRemainIsolated() {
    final SavePromptVersionCommand tenantA = command("tenant-a", "render-v1");
    final SavePromptVersionCommand tenantB = command("tenant-b", "render-v1");

    transaction.executeWithoutResult(status -> repository.save(tenantA));
    transaction.executeWithoutResult(status -> repository.save(tenantB));

    assertThat(
            jdbc.queryForObject(
                "select count(*) from qdr_prompt_version where tenant_id in (?,?)",
                Integer.class,
                tenantA.tenantId(),
                tenantB.tenantId()))
        .isEqualTo(2);
    assertThat(
            repository.findByTenantAndTemplateVersion(
                tenantA.tenantId(), tenantB.promptTemplateId(), tenantB.version()))
        .isEmpty();
  }

  private static List<PromptVersionRecord> runConcurrent(
      final SavePromptVersionCommand command, final int threads, final int attempts)
      throws Exception {
    final ExecutorService executor = Executors.newFixedThreadPool(threads);
    final CountDownLatch ready = new CountDownLatch(threads);
    final CountDownLatch start = new CountDownLatch(1);
    try {
      final List<Future<PromptVersionRecord>> futures = new ArrayList<>();
      for (int attempt = 0; attempt < attempts; attempt++) {
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  if (!start.await(10, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("prompt bootstrap start gate timed out");
                  }
                  return transaction.execute(status -> repository.save(command));
                }));
      }
      assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      final List<PromptVersionRecord> results = new ArrayList<>();
      for (final Future<PromptVersionRecord> future : futures) {
        results.add(future.get(30, TimeUnit.SECONDS));
      }
      return results;
    } finally {
      executor.shutdownNow();
      assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
    }
  }

  private static SavePromptVersionCommand command(
      final String tenantId, final String renderPolicyKey) {
    final UUID templateId = stableUuid(tenantId, "template");
    final UUID versionId = stableUuid(tenantId, "version");
    return new SavePromptVersionCommand(
        templateId,
        versionId,
        tenantId,
        "qdr-review",
        "QDR review",
        "v1",
        renderPolicyKey,
        "prompt-template-ref-a",
        HASH_A,
        "redacted prompt summary",
        PromptVersionStatus.ACTIVE,
        HASH_A,
        CREATED_AT,
        "system");
  }

  private static UUID stableUuid(final String tenantId, final String part) {
    return UUID.nameUUIDFromBytes(
        ("prompt-bootstrap|" + tenantId + "|" + part).getBytes(StandardCharsets.UTF_8));
  }
}
