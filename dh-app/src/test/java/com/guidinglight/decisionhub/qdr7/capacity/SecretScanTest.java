package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SecretScanTest {

  @TempDir Path temporaryDirectory;

  @Test
  void reportsPatternAndExactTemporarySecretWithoutEchoingMatchedValue() throws IOException {
    final String exactSecret = "synthetic-secret-value";
    Files.writeString(
        temporaryDirectory.resolve("artifact.log"),
        "Authorization: Bearer abcdefghijklmnop\nvalue=" + exactSecret + "\n",
        StandardCharsets.UTF_8);

    final Qdr7CapacityArtifactSupport.SecretScanResult result =
        Qdr7CapacityArtifactSupport.scanSecrets(
            temporaryDirectory,
            List.of(
                new Qdr7CapacityArtifactSupport.SecretPattern(
                    "bearer", Pattern.compile("(?i)authorization: bearer [a-z]+"))),
            List.of(exactSecret));

    assertThat(result.findings()).hasSize(2);
    assertThat(result.findings().stream().map(Object::toString).toList())
        .noneMatch(value -> value.contains(exactSecret));
    assertThat(result.findings().stream().map(Qdr7CapacityArtifactSupport.SecretFinding::patternId))
        .containsExactly("bearer", "temporary-secret-exact");
  }

  @Test
  void safeHashesAndStructuredCodesDoNotProduceFindings() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("safe.json"),
        "{\"tenantHash\":\"" + "a".repeat(64) + "\",\"errorCode\":\"RATE_LIMITED\"}",
        StandardCharsets.UTF_8);

    final Qdr7CapacityArtifactSupport.SecretScanResult result =
        Qdr7CapacityArtifactSupport.scanSecrets(
            temporaryDirectory,
            List.of(
                new Qdr7CapacityArtifactSupport.SecretPattern(
                    "bearer", Pattern.compile("(?i)authorization: bearer [a-z]+"))),
            List.of());

    assertThat(result.findings()).isEmpty();
  }
}
