package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class Qdr7CapacityPowerShellContractTest {

  @Test
  void contractTestAcceptsFrozenParametersAndRejectsInvalidRunId()
      throws IOException, InterruptedException {
    final Path root = repositoryRoot();
    final Path script = root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1");

    for (final String executable : List.of("powershell.exe", "pwsh.exe")) {
      assertThat(invoke(executable, script, root, "20260715T120000Z", "7")).isZero();
      assertThat(invoke(executable, script, root, "../unsafe", "7")).isEqualTo(10);
      assertThat(invoke(executable, script, root, "20260715T120000Z", "8")).isEqualTo(10);
    }
  }

  @Test
  void runtimeBindingContractPassesOnWindowsPowerShellAndPwsh()
      throws IOException, InterruptedException {
    final Path root = repositoryRoot();
    final Path contract = root.resolve("scripts/qdr7-capacity/Test-Qdr7CapacityRuntimeBinding.ps1");

    for (final String executable : List.of("powershell.exe", "pwsh.exe")) {
      assertThat(invokeContract(executable, contract, root)).isZero();
    }
  }

  @Test
  void teardownUsesExactRegistryNamesAndForbidsGlobalCleanup() throws IOException {
    final String script =
        Files.readString(
            repositoryRoot().resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));

    assertThat(script)
        .contains("$candidates.Add('pwsh.exe')")
        .contains("$candidates.Add('powershell.exe')")
        .contains("$ResolvedPowerShellIdentity")
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
      final String executable,
      final Path script,
      final Path root,
      final String runId,
      final String seed)
      throws IOException, InterruptedException {
    final Process process =
        new ProcessBuilder(
                executable,
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
                root.toString(),
                "-PowerShellExecutable",
                executable)
            .redirectErrorStream(true)
            .start();
    final boolean finished =
        process.waitFor(Duration.ofSeconds(20).toMillis(), TimeUnit.MILLISECONDS);
    final String output =
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertThat(finished).withFailMessage("PowerShell contract test timed out: %s", output).isTrue();
    return process.exitValue();
  }

  private static int invokeContract(
      final String executable, final Path contract, final Path root)
      throws IOException, InterruptedException {
    final Process process =
        new ProcessBuilder(
                executable,
                "-NoProfile",
                "-NonInteractive",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                contract.toString())
            .directory(root.toFile())
            .redirectErrorStream(true)
            .start();
    final boolean finished =
        process.waitFor(Duration.ofSeconds(60).toMillis(), TimeUnit.MILLISECONDS);
    final String output =
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    assertThat(finished).withFailMessage("PowerShell binding contract timed out: %s", output).isTrue();
    assertThat(output).contains("QDR7_CAPACITY_RUNTIME_BINDING_CONTRACT=PASS");
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
