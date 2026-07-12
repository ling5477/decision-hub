package com.guidinglight.decisionhub.qdr7;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 防止persistent guard生命周期重新依赖JVM绝对时钟或caller absolute timestamp。 */
class PersistentGuardLifecycleClockArchitectureTest {

  @Test
  void productionLifecycleSourcesForbidJvmAbsoluteClockAndAbsoluteCommandInputs()
      throws IOException {
    final List<Path> lifecycleSources =
        List.of(
            Path.of(
                "src/main/java/com/guidinglight/decisionhub/qdr7/PersistentDecisionDryRunRateLimiter.java"),
            Path.of(
                "../dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/"
                    + "JdbcRateLimitAdmissionAdapter.java"),
            Path.of(
                "../dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/"
                    + "JdbcIdempotencyGuardAdapter.java"),
            Path.of(
                "../dh-infra/src/main/java/com/guidinglight/decisionhub/infra/jdbc/qdr/guard/"
                    + "JdbcGuardCleanupAdapter.java"),
            Path.of(
                "../dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/"
                    + "IdempotencyAdmissionCommand.java"),
            Path.of(
                "../dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/"
                    + "IdempotencyTransitionCommand.java"),
            Path.of(
                "../dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/guard/"
                    + "GuardCleanupCommand.java"));

    for (final Path source : lifecycleSources) {
      final String body = Files.readString(source.toAbsolutePath().normalize(), StandardCharsets.UTF_8);
      assertThat(body)
          .doesNotContain("Instant.now(")
          .doesNotContain("OffsetDateTime.now(")
          .doesNotContain("System.currentTimeMillis(")
          .doesNotContain("Clock.system");
    }

    final String admission = Files.readString(lifecycleSources.get(4), StandardCharsets.UTF_8);
    final String transition = Files.readString(lifecycleSources.get(5), StandardCharsets.UTF_8);
    final String cleanup = Files.readString(lifecycleSources.get(6), StandardCharsets.UTF_8);
    assertThat(admission).contains("Duration timeToLive", "Duration retentionPeriod").doesNotContain("Instant");
    assertThat(transition).contains("Duration leaseDuration").doesNotContain("Instant");
    assertThat(cleanup).contains("Duration safetyGrace").doesNotContain("Instant");
  }
}
