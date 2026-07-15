package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class Qdr7CapacityPowerShellContractTest {

  @Test
  void contractTestAcceptsFrozenParametersAndRejectsInvalidRunId()
      throws IOException, InterruptedException {
    final Path root = repositoryRoot();
    final Path script = root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1");

    assertThat(invoke(script, root, "20260715T120000Z", "7")).isZero();
    assertThat(invoke(script, root, "../unsafe", "7")).isEqualTo(10);
    assertThat(invoke(script, root, "20260715T120000Z", "8")).isEqualTo(10);
  }

  @Test
  void teardownUsesExactRegistryNamesAndForbidsGlobalCleanup() throws IOException {
    final String script =
        Files.readString(
            repositoryRoot().resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));

    assertThat(script)
        .contains("$Registry.containerName -ne $expected")
        .contains("docker container rm --force $Registry.containerName")
        .contains("docker volume rm $Registry.volumeName")
        .contains("finally")
        .doesNotContain("docker system prune", "docker volume prune", "git reset", "git clean");
  }

  @Test
  void testcontainersCannotSilentlySkipFormalIt() throws IOException {
    final String formalIt =
        Files.readString(
            repositoryRoot()
                .resolve(
                    "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));

    assertThat(formalIt)
        .contains("@Testcontainers")
        .doesNotContain("disabledWithoutDocker = true", "disabledWithoutDocker=true");
  }

  private static int invoke(
      final Path script, final Path root, final String runId, final String seed)
      throws IOException, InterruptedException {
    final Process process =
        new ProcessBuilder(
                "powershell.exe",
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                script.toString(),
                "-Phase",
                "ContractTest",
                "-RunId",
                runId,
                "-Seed",
                seed,
                "-ProjectRoot",
                root.toString())
            .redirectErrorStream(true)
            .start();
    final boolean finished =
        process.waitFor(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS);
    final String output =
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertThat(finished).withFailMessage("PowerShell contract test timed out: %s", output).isTrue();
    return process.exitValue();
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    while (current != null) {
      if (Files.isRegularFile(current.resolve("pom.xml"))
          && Files.isDirectory(current.resolve("dh-app"))) {
        return current;
      }
      current = current.getParent();
    }
    throw new IllegalStateException("repository root not found");
  }
}
