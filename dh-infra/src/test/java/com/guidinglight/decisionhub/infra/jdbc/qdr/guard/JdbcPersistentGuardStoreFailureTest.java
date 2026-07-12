package com.guidinglight.decisionhub.infra.jdbc.qdr.guard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.IdempotencyAdmissionStatus;
import com.guidinglight.decisionhub.usecase.qdr.guard.PersistentGuardIdentity;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionCommand;
import com.guidinglight.decisionhub.usecase.qdr.guard.RateLimitAdmissionStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.core.JdbcTemplate;

/** PostgreSQL失败必须保持独立store分类，禁止伪装成rate limited或duplicate。 */
@ExtendWith(MockitoExtension.class)
class JdbcPersistentGuardStoreFailureTest {

  @Mock private JdbcTemplate jdbcTemplate;

  @Test
  void rateStoreFailureFailsClosedWithoutRateLimitedClassification() {
    when(jdbcTemplate.queryForMap(anyString(), any(Object[].class)))
        .thenThrow(new DataAccessResourceFailureException("unavailable"));

    final var result =
        new JdbcRateLimitAdmissionAdapter(jdbcTemplate)
            .tryAcquire(new RateLimitAdmissionCommand(identity(), 60, 10));

    assertThat(result.status()).isEqualTo(RateLimitAdmissionStatus.STORE_UNAVAILABLE);
  }

  @Test
  void idempotencyStoreFailureFailsClosedWithoutConflictClassification() {
    when(jdbcTemplate.update(anyString(), any(Object[].class)))
        .thenThrow(new DataAccessResourceFailureException("unavailable"));
    final Instant now = Instant.parse("2026-07-12T00:00:00Z");

    final var result =
        new JdbcIdempotencyGuardAdapter(jdbcTemplate)
            .admit(
                new IdempotencyAdmissionCommand(
                    identity(),
                    "request-1",
                    "a".repeat(64),
                    IdempotencyAdmissionCommand.HASH_VERSION,
                    now.plusSeconds(600),
                    now.plusSeconds(3600)));

    assertThat(result.status()).isEqualTo(IdempotencyAdmissionStatus.STORE_UNAVAILABLE);
  }

  private static PersistentGuardIdentity identity() {
    return new PersistentGuardIdentity(
        "test",
        PersistentGuardIdentity.DECISION_DRY_RUN_ENDPOINT,
        PersistentGuardIdentity.NQ_DRYRUN_SOURCE,
        "tenant-a");
  }
}
