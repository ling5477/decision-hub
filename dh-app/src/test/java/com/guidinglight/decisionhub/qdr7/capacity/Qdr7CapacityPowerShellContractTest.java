package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class Qdr7CapacityPowerShellContractTest {

  @Test
  void contractTestAcceptsFrozenParametersAndRejectsInvalidRunId()
      throws IOException, InterruptedException {
    final Path root = repositoryRoot();
    final Path script = root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1");

    for (final String executable : requiredPowerShellExecutables()) {
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

    for (final String executable : requiredPowerShellExecutables()) {
      assertThat(invokeContract(executable, contract, root)).isZero();
    }
  }

  @Test
  void selectsRequiredPowerShellExecutablesByOperatingSystem() {
    assertThat(requiredPowerShellExecutables("Windows 11"))
        .containsExactly("powershell.exe", "pwsh.exe");
    assertThat(requiredPowerShellExecutables("Linux")).containsExactly("pwsh");
  }

  @Test
  void mavenExecUsesPwshByDefaultAndWindowsPowerShellOnlyThroughOsActivation() throws IOException {
    final Path root = repositoryRoot();
    final String rootPom = Files.readString(root.resolve("pom.xml"));
    final String appPom = Files.readString(root.resolve("dh-app/pom.xml"));

    assertThat(rootPom)
        .contains("<qdr7.powershell.executable>pwsh</qdr7.powershell.executable>")
        .contains("<id>qdr7-capacity-windows-powershell</id>")
        .contains("<family>Windows</family>")
        .contains("<qdr7.powershell.executable>powershell.exe</qdr7.powershell.executable>");
    assertThat(appPom)
        .contains("<qdr7.powershell.executable>pwsh</qdr7.powershell.executable>")
        .contains("<id>qdr7-capacity-windows-powershell</id>")
        .contains("<family>Windows</family>")
        .contains("<qdr7.powershell.executable>powershell.exe</qdr7.powershell.executable>")
        .doesNotContain("<executable>powershell.exe</executable>");
    assertThat(occurrences(appPom, "<executable>${qdr7.powershell.executable}</executable>"))
        .isEqualTo(2);
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
        .contains("@('container', 'rm', '--force', [string]$Registry.containerName)")
        .contains("@('volume', 'rm', [string]$Registry.volumeName)")
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

  @Test
  void staticContainerStartsBeforeSpringDynamicPropertyResolutionAndHasSingleOwner()
      throws IOException {
    final Path root = repositoryRoot();
    final String formalIt =
        Files.readString(
            root.resolve(
                "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));
    final String script =
        Files.readString(root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));

    final int containerDeclaration = formalIt.indexOf("@Container");
    final int explicitStart = formalIt.indexOf("startPostgresBeforeSpringPropertyResolution();");
    final int dynamicPropertySource = formalIt.indexOf("@DynamicPropertySource");
    assertThat(containerDeclaration).isGreaterThanOrEqualTo(0);
    assertThat(explicitStart).isGreaterThan(containerDeclaration).isLessThan(dynamicPropertySource);
    assertThat(formalIt.substring(dynamicPropertySource, formalIt.indexOf("@LocalServerPort")))
        .contains("FROZEN_JDBC_URL", "FROZEN_DATABASE_USERNAME", "FROZEN_DATABASE_PASSWORD")
        .doesNotContain("POSTGRES::getJdbcUrl", "POSTGRES::getMappedPort");
    assertThat(script)
        .contains("containerOwnership = 'JUNIT_TESTCONTAINERS'")
        .doesNotContain("docker run", "docker container create", "docker create");
  }

  @Test
  void implementationValidationIsWiredAndCannotExecuteMandatoryScenarios() throws IOException {
    final Path root = repositoryRoot();
    final String rootPom = Files.readString(root.resolve("pom.xml"));
    final String appPom = Files.readString(root.resolve("dh-app/pom.xml"));
    final String harnessScript =
        Files.readString(root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));
    final String formalIt =
        Files.readString(
            root.resolve(
                "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));

    assertThat(rootPom)
        .contains("<qdr7.implementationValidation>false</qdr7.implementationValidation>");
    assertThat(appPom)
        .contains(
            "<qdr7.implementationValidation>",
            "${qdr7.implementationValidation}",
            "</qdr7.implementationValidation>")
        .contains("<argument>-ImplementationValidation</argument>");
    assertThat(harnessScript)
        .contains("'.gitattributes'")
        .contains("'dh-bom/pom.xml'")
        .contains("$stagedWriteScopeValid")
        .contains("qualification write allowlist only")
        .contains("empty or harness write allowlist only");
    assertThat(formalIt)
        .contains("IMPLEMENTATION_VALIDATION_ONLY")
        .contains("tenantIsolationStartupProbe", "contextRestartStartupProbe")
        .contains("nonceDriverStartupProbe")
        .contains("new ProcessBuilder(fullRegressionCommand())")
        .contains("mavenCommand(List.of(\"-B\", \"-ntp\", \"test\"))")
        .contains(
            "@EnabledIfSystemProperty(named = \"qdr7.implementationValidation\", matches = \"true\")")
        .contains(
            "@DisabledIfSystemProperty(named = \"qdr7.implementationValidation\", matches = \"true\")")
        .contains("artifact.put(\"executedScenarioCount\", 0)");
  }

  @Test
  void qualificationAndPartialFinalizerContractsAreDurableAndNonFormal() throws IOException {
    final Path root = repositoryRoot();
    final String rootPom = Files.readString(root.resolve("pom.xml"));
    final String appPom = Files.readString(root.resolve("dh-app/pom.xml"));
    final String script =
        Files.readString(root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));
    final String binding =
        Files.readString(root.resolve("scripts/qdr7-capacity/Test-Qdr7CapacityRuntimeBinding.ps1"));

    assertThat(rootPom).contains("<qdr7.qualificationOnly>false</qdr7.qualificationOnly>");
    assertThat(appPom)
        .contains("<argument>-QualificationOnly</argument>")
        .contains("<argument>${qdr7.qualificationOnly}</argument>")
        .contains("<qdr7.qualificationOnly>${qdr7.qualificationOnly}</qdr7.qualificationOnly>");
    assertThat(script)
        .contains("qdr7-capacity-qualification")
        .contains("QUALIFICATION_ONLY")
        .contains("HARNESS_INTERRUPTED_AFTER_SCENARIO_START")
        .contains("Get-NormalizedScenarioLedger")
        .contains("Set-SummaryScenarioCounts")
        .contains("PARTIAL_THRESHOLD_EVIDENCE")
        .contains("capacityAcceptanceExecuted");
    assertThat(binding)
        .contains("QualificationOnly 'true'")
        .contains("partial comparisons preserved")
        .contains("started to partial normalization")
            .contains("qualification formal verdict isolation");
  }

  @Test
  void qualificationDriversUseIsolatedSessionsStableRestartAndRepositoryMaven() throws IOException {
    final String formalIt =
        Files.readString(
            repositoryRoot()
                .resolve(
                    "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));

    assertThat(formalIt)
        .contains("config.addDataSourceProperty(\"ApplicationName\", pressureApplicationName)")
        .contains("and application_name=?")
        .contains("POSTGRES.stopWithoutRemoval()")
        .contains("POSTGRES.startExistingContainer()")
        .contains(".run(commandLineProperties(properties))")
        .contains("seedCanonicalPromptVersion(contextJdbc, tenant, round)")
        .contains("System.getenv(\"MAVEN_HOME\")")
        .contains("PROJECT_ROOT.resolve(windows ? \"mvnw.cmd\" : \"mvnw\")");
  }

  @Test
  void artifactContractsRestoreFixedMillisecondUtcStringsAfterPowerShellJsonParsing()
      throws IOException {
    final String script =
        Files.readString(
            repositoryRoot().resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));

    assertThat(script)
        .contains("[IO.File]::ReadAllText($Path, $Utf8NoBom)")
        .contains(
            "Set-ObjectProperty -Target $Summary -Name 'startedAtUtc' -Value"
                + " (ConvertTo-UtcTimestamp -Value $StartedAt)")
        .contains(
            "Set-ObjectProperty -Target $Summary -Name 'finishedAtUtc' -Value"
                + " (ConvertTo-UtcTimestamp -Value $CompletedAt)")
        .contains("$Registry.startedAtUtc = ConvertTo-UtcTimestamp -Value $Registry.startedAtUtc")
        .contains(
            "$Registry.samplerStartedAtUtc = ConvertTo-UtcTimestamp"
                + " -Value $Registry.samplerStartedAtUtc");
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

  /** Windows同时覆盖5.1与7；Linux CI只具备跨平台pwsh，不虚构Windows PowerShell。 */
  private static List<String> requiredPowerShellExecutables() {
    return requiredPowerShellExecutables(System.getProperty("os.name", ""));
  }

  private static List<String> requiredPowerShellExecutables(final String operatingSystem) {
    return operatingSystem.toLowerCase(Locale.ROOT).contains("win")
        ? List.of("powershell.exe", "pwsh.exe")
        : List.of("pwsh");
  }

  private static int invokeContract(final String executable, final Path contract, final Path root)
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
    assertThat(finished)
        .withFailMessage("PowerShell binding contract timed out: %s", output)
        .isTrue();
    assertThat(output).contains("QDR7_CAPACITY_RUNTIME_BINDING_CONTRACT=PASS");
    return process.exitValue();
  }

  private static int occurrences(final String value, final String token) {
    int count = 0;
    int offset = 0;
    while ((offset = value.indexOf(token, offset)) >= 0) {
      count++;
      offset += token.length();
    }
    return count;
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
