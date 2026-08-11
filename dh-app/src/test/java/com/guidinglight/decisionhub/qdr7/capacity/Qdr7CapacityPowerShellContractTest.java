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
  void manifestContractRejectsOmissionDuplicateAndPostManifestFile()
      throws IOException, InterruptedException {
    final Path root = repositoryRoot();
    final Path script = root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1");

    for (final String executable : requiredPowerShellExecutables()) {
      assertThat(invokeManifestContract(executable, script, root)).isZero();
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
        .contains("empty or harness write allowlist only")
        .contains(
            "$executionBlockers",
            "'IMPLEMENTATION_VALIDATION_FORMAL_ENVIRONMENT_NOT_QUALIFIED'",
            "$_ -notin @('clock-synchronized', 'clock-offset', 'network-isolation')")
        .contains("$imageIdentity.text -eq $expectedImageId")
        .contains("$repoDigestJsonContainsReference")
        .contains(
            "Get-ThresholdAggregateFindings -Threshold $threshold -AllowNotRun"
                + " $implementationValidationPassed",
            "$notEvaluatedCount -eq 99",
            "'FORMAL_SCENARIO_NOT_EXECUTED'");
    assertThat(formalIt)
        .contains("IMPLEMENTATION_VALIDATION_ONLY")
        .contains("tenantIsolationStartupProbe", "contextRestartStartupProbe")
        .contains("nonceDriverStartupProbe")
        .contains("runRuntimeSafetyProbe(HARNESS_STARTED)")
        .contains("new ProcessBuilder(fullRegressionCommand())")
        .contains("mavenCommand(List.of(\"-o\", \"-B\", \"-ntp\", \"test\"))")
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
        .contains("$nonFormalMode = $ImplementationValidationEnabled -or $QualificationOnlyEnabled")
        .contains("if ($nonFormalMode) { 'NOT_EVALUATED' } else { 'BLOCKED' }")
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
  void formalEvidenceBindingAdmissionAndFinalizerRemainFailClosed() throws IOException {
    final Path root = repositoryRoot();
    final String script =
        Files.readString(root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));
    final String formalIt =
        Files.readString(
            root.resolve(
                "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));

    final int admission = formalIt.indexOf("qualifyEnvironmentBeforeScenarioDispatch();");
    final int dispatch = formalIt.indexOf("for (final HarnessDriver driver : drivers)");
    assertThat(admission).isGreaterThanOrEqualTo(0).isLessThan(dispatch);
    assertThat(formalIt)
        .contains("Qdr7CapacityEnvironmentAdmission.runPostgresSmokeAndQualify(")
        .contains("EXPECTED_THRESHOLD_COMPARISONS = 99")
        .contains("artifact.put(\"declaredThresholdLeaves\", 41)")
        .contains("assertThat(dispatchers).hasSize(15)")
        .contains("\"scenarioSetHash\"", "\"thresholdSetHash\"", "\"environmentManifestHash\"");

    assertThat(script)
        .contains("capacity-environment-manifest.json", "capacity-execution-manifest.json")
        .contains(
            "$bindingFields = @('attemptId', 'candidateSha', 'candidateTree', 'profileId',"
                + " 'profileVersion', 'scenarioSetHash', 'thresholdSetHash',"
                + " 'environmentManifestHash', 'harnessVersion', 'harnessHash')")
        .contains(
            "$manifest.expectedMandatoryScenarios = 15",
            "$manifest.expectedThresholdLeaves = 41",
            "$manifest.expectedThresholdComparisons = 99")
        .contains("artifact-inventory.json", "capacity-final-verdict.json")
        .contains("$findings.Add('THRESHOLD_SET_INCOMPLETE')")
        .contains(
            "$manifest.executionMode = Get-ExecutionMode",
            "$manifest.formalPacketPrepared = (Get-ExecutionMode) -eq 'FORMAL'",
            "$modeBindingFindings.Add('EXECUTION_MODE_BINDING_MISMATCH')",
            "$modeBindingFindings.Add('EXECUTION_MANIFEST_MODE_MISMATCH')")
        .contains(
            "$integrityFindings.Add(\"SCENARIO_CARDINALITY_INVALID:$scenarioId\")",
            "$integrityFindings.Add(\"ORPHAN_SCENARIO_RESULT:$($row.scenarioId)\")")
        .contains(
            "$findings.Add('THRESHOLD_STATUS_AGGREGATE_MISMATCH')",
            "$findings.Add('THRESHOLD_TOP_LEVEL_STATUS_MISMATCH')",
            "Get-ObjectPropertyValue -Target $Threshold -Name 'comparisons' -DefaultValue @()",
            "Get-ObjectPropertyValue -Target $Threshold -Name 'status' -DefaultValue ''")
        .contains("$findings.Add(\"BINDING_MISMATCH:${name}:$field\")")
        .contains("ENVIRONMENT_MANIFEST_HASH_MISMATCH")
        .contains("$artifactRegistry.mandatory")
        .contains("$finalExit = 80", "elseif ($finalExit -eq 80) { 'INVALID' }");
  }

  @Test
  void finalizerRebindsCurrentTreeInputsThresholdSemanticsAndSamplerCompletion()
      throws IOException {
    final Path root = repositoryRoot();
    final String script =
        Files.readString(root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));
    final String watcher =
        Files.readString(root.resolve("scripts/qdr7-capacity/Watch-Qdr7CapacityResources.ps1"));

    assertThat(script)
        .contains(
            "FINALIZER_HEAD_MISMATCH",
            "FINALIZER_WORKTREE_DIRTY",
            "FINALIZER_STAGED_DIRTY",
            "FINALIZER_SCENARIO_SET_HASH_MISMATCH",
            "FINALIZER_THRESHOLD_SET_HASH_MISMATCH",
            "FINALIZER_HARNESS_HASH_MISMATCH",
            "THRESHOLD_OPERATOR_MISMATCH",
            "THRESHOLD_VALUE_MISMATCH",
            "THRESHOLD_SOURCE_ARTIFACT_MISMATCH",
            "THRESHOLD_SOURCE_OBSERVED_MISMATCH",
            "THRESHOLD_STATUS_SEMANTICS_MISMATCH",
            "RESOURCE_SAMPLER_COMPLETION_INVALID",
            "RESOURCE_SAMPLER_RAW_HASH_MISMATCH",
            "RESOURCE_SUMMARY_RAW_BINDING_MISMATCH");
    assertThat(script)
        .contains(
            "if ($Phase -eq 'Finalize' -and (Get-ExecutionMode) -eq 'FORMAL')",
            "Test-FormalPacketPreparationGate",
            "FORMAL_PACKET_PREPARED_MISSING",
            "FORMAL_PACKET_PREPARED_TYPE_INVALID",
            "FORMAL_PACKET_NOT_PREPARED");
    assertThat(watcher)
        .contains(
            "resource-sampler-completion.json",
            "COMPLETED",
            "jvmSeriesSha256",
            "dockerSeriesSha256");
  }

  @Test
  void acceptanceUsesSpringRuntimeBeansAndFactsourcesDoNotSelfAttestSecurityPass()
      throws IOException {
    final Path root = repositoryRoot();
    final String formalIt =
        Files.readString(
            root.resolve(
                "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));
    assertThat(formalIt)
        .contains(
            "@Autowired private LimitedDryRunRuntimePolicy runtimePolicy",
            "@Autowired private HmacNqDryRunAuthenticator dryRunAuthenticator",
            "@Autowired private DecisionDryRunService decisionDryRunService",
            "@Autowired private DecisionDryRunRuntimeProperties runtimeProperties")
        .doesNotContain("Qdr7CapacityRuntimeSafetyProbe.run()");

    for (final String path :
        List.of(
            "README.md",
            "docs/current/README.md",
            "docs/current/STATUS.md",
            "docs/current/WORK_ORDER.md",
            "docs/current/ROADMAP.md",
            "docs/current/TESTING.md",
            "docs/current/WORKLOG.md",
            "docs/current/CODEX_PROJECT_INSTRUCTIONS.md",
            "docs/current/DH_POST_STAGE_QDR_11_NEXT_STAGE_PLAN.md",
            "docs/current/DH_STAGE_QDR_12_FORMAL_CAPACITY_RESOURCE_SAFETY_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md")) {
      final String factsource = Files.readString(root.resolve(path));
      final int nextAuthority = factsource.indexOf("## Terminal current authority", 40);
      final String currentAuthority =
          nextAuthority < 0 ? factsource : factsource.substring(0, nextAuthority);
      if (currentAuthority.contains("CURRENT TREE NOT YET SEALED")) {
        assertThat(currentAuthority)
            .as(path)
            .doesNotContain(
                "HARNESS_REMEDIATED / LOCAL_ACCEPTED",
                "安全 exact-diff 已通过",
                "SYNCHRONIZED / 0 CURRENT CONFLICTS");
      }
    }
  }

  @Test
  void resourceWatcherDeclaresStableByteBasedCsvContracts() throws IOException {
    final String watcher =
        Files.readString(
            repositoryRoot().resolve("scripts/qdr7-capacity/Watch-Qdr7CapacityResources.ps1"));

    assertThat(watcher)
        .contains(
            "schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,phase,processRole,pid,parentPidHash,cpuPercent,heapUsedBytes,heapCommittedBytes,heapMaxBytes,nonHeapBytes,nativeMemoryBytes,workingSetBytes,threadCount,missingReason")
        .contains(
            "schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,phase,containerHash,cpuPercent,memoryBytes,hostAvailableBytes,missingReason")
        .contains("ConvertTo-ByteCount", "FreePhysicalMemory * 1024L")
        .contains("REQUIRED_JVM_PROCESS_UNAVAILABLE", "DOCKER_OR_HOST_MEMORY_UNAVAILABLE")
        .doesNotContain("sampler failure => metric=0");
  }

  @Test
  void environmentAdmissionUsesObservedIsolationPinnedPostgresAndOfflineMaven() throws IOException {
    final Path root = repositoryRoot();
    final String script =
        Files.readString(root.resolve("scripts/qdr7-capacity/Invoke-Qdr7CapacityAcceptance.ps1"));
    final String formalIt =
        Files.readString(
            root.resolve(
                "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));
    final String admission =
        Files.readString(
            root.resolve(
                "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityEnvironmentAdmission.java"));
    final String admissionConfig =
        Files.readString(
            root.resolve("config/qdr7-capacity/qdr7-capacity-environment-admission.json"));

    assertThat(script)
        .contains("Get-NetAdapter")
        .contains("networkIsolationVerified")
        .contains("disallowedNetworkAdapterCount")
        .contains("postgresImageDigestVerified")
        .doesNotContain("'postgres:17'");
    assertThat(formalIt)
        .contains("DockerImageName.parse(POSTGRES_IMAGE)")
        .contains("ENVIRONMENT_REQUIREMENTS.path(\"postgresImage\")")
        .contains("List.of(\"-o\", \"-B\", \"-ntp\", \"test\")");
    assertThat(admissionConfig).contains("postgres@sha256:");
    assertThat(admission)
        .contains("NETWORK_ISOLATION_UNVERIFIED")
        .contains("POSTGRES_IMAGE_DIGEST_MISMATCH")
        .contains("requirements.path(\"postgresImage\")");
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

  private static int invokeManifestContract(
      final String executable, final Path script, final Path root)
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
                "ManifestContractTest",
                "-RunId",
                "20991231T235959Z",
                "-Seed",
                "7",
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
    assertThat(finished)
        .withFailMessage("PowerShell manifest contract timed out: %s", output)
        .isTrue();
    assertThat(output).contains("QDR7_CAPACITY_MANIFEST_CONTRACT=PASS");
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
