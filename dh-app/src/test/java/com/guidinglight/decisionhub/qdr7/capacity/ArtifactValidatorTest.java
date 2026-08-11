package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ArtifactValidatorTest {

  private static final String RUN_ID = "20260715T120000Z";
  private static final String COMMIT = "a".repeat(40);

  @TempDir Path temporaryDirectory;

  @Test
  void validatesCommonJsonContractAndRejectsMissingField() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final Qdr7CapacityContracts.RunContext context =
        Qdr7CapacityContracts.parseRunContext(RUN_ID, "7", temporaryDirectory, COMMIT);
    final Map<String, Object> artifact =
        Qdr7CapacityContracts.commonArtifact(
            context,
            "test",
            Qdr7CapacityContracts.HarnessStatus.PASS,
            Instant.parse("2026-07-15T12:00:00Z"),
            Instant.parse("2026-07-15T12:00:01Z"));
    addBinding(artifact);
    final Path valid = context.evidenceRoot().resolve("valid.json");
    Qdr7CapacityArtifactSupport.writeJson(valid, artifact, mapper);

    assertThat(
            Qdr7CapacityArtifactSupport.validateJson(
                    valid, RUN_ID, COMMIT, Qdr7CapacityContracts.CRITERIA_VERSION, mapper)
                .valid())
        .isTrue();

    artifact.remove("status");
    final Path invalid = context.evidenceRoot().resolve("invalid.json");
    Qdr7CapacityArtifactSupport.writeJson(invalid, artifact, mapper);
    assertThat(
            Qdr7CapacityArtifactSupport.validateJson(
                    invalid, RUN_ID, COMMIT, Qdr7CapacityContracts.CRITERIA_VERSION, mapper)
                .findings())
        .anyMatch(finding -> finding.contains("missing status"));
  }

  @Test
  void validatesCsvCommonPrefixAndRejectsWrongOrder() throws IOException {
    final Path valid = temporaryDirectory.resolve("valid.csv");
    Qdr7CapacityArtifactSupport.writeCsvHeader(valid, List.of("latencyMs"));
    final List<String> required =
        List.of("schemaVersion", "runId", "commitSha", "scenario", "timestampUtc", "elapsedMs");

    assertThat(Qdr7CapacityArtifactSupport.validateCsvHeader(valid, required).valid()).isTrue();

    final Path invalid = temporaryDirectory.resolve("invalid.csv");
    Files.writeString(invalid, "runId,schemaVersion\n", StandardCharsets.UTF_8);
    assertThat(Qdr7CapacityArtifactSupport.validateCsvHeader(invalid, required).valid()).isFalse();
  }

  @Test
  void readsMavenLogMarkersWithoutAssumingUtf8() throws IOException {
    final Path log = temporaryDirectory.resolve("maven.log");
    Files.write(
        log,
        new byte[] {
          (byte) 0xC4,
          (byte) 0xE3,
          (byte) '\n',
          (byte) '[',
          (byte) 'I',
          (byte) 'N',
          (byte) 'F',
          (byte) 'O',
          (byte) ']',
          (byte) ' ',
          (byte) 'B',
          (byte) 'U',
          (byte) 'I',
          (byte) 'L',
          (byte) 'D',
          (byte) ' ',
          (byte) 'S',
          (byte) 'U',
          (byte) 'C',
          (byte) 'C',
          (byte) 'E',
          (byte) 'S',
          (byte) 'S',
          (byte) '\n'
        });

    assertThat(Qdr7CapacityArtifactSupport.readAsciiCompatibleLog(log))
        .contains("[INFO] BUILD SUCCESS");
  }

  @Test
  void manifestIsStableSelfExcludingAndDetectsMutationOrTraversal() throws IOException {
    Files.writeString(temporaryDirectory.resolve("b.txt"), "b", StandardCharsets.UTF_8);
    Files.writeString(temporaryDirectory.resolve("a.txt"), "a", StandardCharsets.UTF_8);

    final Map<String, String> manifest =
        Qdr7CapacityArtifactSupport.writeManifest(temporaryDirectory);

    assertThat(manifest.keySet()).containsExactly("a.txt", "b.txt");
    assertThat(Qdr7CapacityArtifactSupport.validateManifest(temporaryDirectory).valid()).isTrue();

    Files.writeString(temporaryDirectory.resolve("a.txt"), "changed", StandardCharsets.UTF_8);
    assertThat(Qdr7CapacityArtifactSupport.validateManifest(temporaryDirectory).findings())
        .contains("MANIFEST_MISMATCH:a.txt");

    Files.writeString(
        temporaryDirectory.resolve("sha256-manifest.txt"),
        "a".repeat(64) + "  ../outside.txt\n",
        StandardCharsets.UTF_8);
    assertThat(Qdr7CapacityArtifactSupport.validateManifest(temporaryDirectory).findings())
        .contains("MANIFEST_PATH_INVALID");
  }

  @Test
  void manifestRejectsOmittedDuplicateAndUnexpectedFiles() throws IOException {
    Files.writeString(temporaryDirectory.resolve("a.txt"), "a", StandardCharsets.UTF_8);
    Files.writeString(temporaryDirectory.resolve("b.txt"), "b", StandardCharsets.UTF_8);
    Qdr7CapacityArtifactSupport.writeManifest(temporaryDirectory);
    final Path manifest = temporaryDirectory.resolve("sha256-manifest.txt");
    final List<String> original = Files.readAllLines(manifest, StandardCharsets.UTF_8);

    Files.write(manifest, List.of(original.get(0)), StandardCharsets.UTF_8);
    assertThat(Qdr7CapacityArtifactSupport.validateManifest(temporaryDirectory).findings())
        .contains("MANIFEST_PATH_SET_MISMATCH");

    Files.write(
        manifest, List.of(original.get(0), original.get(0), original.get(1)), StandardCharsets.UTF_8);
    assertThat(Qdr7CapacityArtifactSupport.validateManifest(temporaryDirectory).findings())
        .contains("MANIFEST_DUPLICATE_PATH:a.txt");

    Qdr7CapacityArtifactSupport.writeManifest(temporaryDirectory);
    Files.writeString(temporaryDirectory.resolve("c.txt"), "c", StandardCharsets.UTF_8);
    assertThat(Qdr7CapacityArtifactSupport.validateManifest(temporaryDirectory).findings())
        .contains("MANIFEST_PATH_SET_MISMATCH");
  }

  @Test
  void mandatoryRegistryRejectsMissingArtifactAndPathTraversal() throws IOException {
    Files.writeString(temporaryDirectory.resolve("present.json"), "{}", StandardCharsets.UTF_8);

    final Qdr7CapacityArtifactSupport.ValidationResult result =
        Qdr7CapacityArtifactSupport.validateMandatoryArtifacts(
            temporaryDirectory, List.of("present.json", "missing.json", "../outside.json"));

    assertThat(result.valid()).isFalse();
    assertThat(result.findings())
        .contains(
            "MANDATORY_ARTIFACT_MISSING:missing.json", "MANDATORY_PATH_INVALID:../outside.json");
  }

  @Test
  void frozenBlockedSummaryAndThresholdContractsAreRegistered() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final Path root = repositoryRoot();
    final JsonNode registry =
        mapper.readTree(
            root.resolve("config/qdr7-capacity/qdr7-capacity-artifact-registry.json").toFile());
    final JsonNode schema =
        mapper.readTree(
            root.resolve("config/qdr7-capacity/qdr7-capacity-artifacts.schema.json").toFile());
    final List<String> mandatory =
        mapper.convertValue(
            registry.path("mandatory"),
            mapper.getTypeFactory().constructCollectionType(List.class, String.class));
    final List<String> summaryRequired =
        mapper.convertValue(
            registry.path("summaryRequired"),
            mapper.getTypeFactory().constructCollectionType(List.class, String.class));

    assertThat(mandatory)
        .contains(
            "scenario-ledger.json",
            "threshold-comparison.json",
            "capacity-acceptance-summary.json");
    assertThat(summaryRequired)
        .containsExactly(
            "schemaVersion",
            "runId",
            "commitSha",
            "criteriaVersion",
            "startedAt",
            "completedAt",
            "status",
            "internalExitCode",
            "mandatoryScenarioCount",
            "startedScenarioCount",
            "partialScenarioCount",
            "completedScenarioCount",
            "passedScenarioCount",
            "failedScenarioCount",
            "blockedScenarioCount",
            "notStartedScenarioCount",
            "executedScenarioCount",
            "correctnessVerdict",
            "thresholdVerdict",
            "regressionVerdict",
            "qualityVerdict",
            "artifactVerdict",
            "secretVerdict",
            "teardownVerdict",
            "reasonCode",
            "capacityAcceptanceExecuted",
            "formalAcceptanceVerdict",
            "qualificationVerdict");
    assertThat(schema.path("properties").path("mandatoryScenarioCount").path("const").asInt())
        .isEqualTo(15);
    assertThat(schema.path("properties").has("executedScenarioCount")).isTrue();
    assertThat(schema.path("properties").has("reasonCode")).isTrue();
    assertThat(schema.path("properties").path("executionState").path("enum"))
        .extracting(JsonNode::asText)
        .containsExactly("NOT_STARTED", "STARTED", "PARTIAL", "COMPLETED");
    assertThat(schema.path("properties").path("verdict").path("enum"))
        .extracting(JsonNode::asText)
        .containsExactly(
            "NOT_EVALUATED", "PASS_WITHIN_FROZEN_PROFILE", "PASS", "FAIL", "BLOCKED", "INVALID");
  }

  private static void addBinding(final Map<String, Object> artifact) {
    artifact.put("attemptId", RUN_ID);
    artifact.put("candidateSha", COMMIT);
    artifact.put("candidateTree", COMMIT);
    artifact.put("profileId", "qdr7-capacity-acceptance");
    artifact.put("profileVersion", Qdr7CapacityContracts.CRITERIA_VERSION);
    artifact.put("scenarioSetHash", "b".repeat(64));
    artifact.put("thresholdSetHash", "c".repeat(64));
    artifact.put("environmentManifestHash", "d".repeat(64));
    artifact.put("harnessVersion", "qdr12-capacity-harness-1");
    artifact.put("harnessHash", "e".repeat(64));
    artifact.put("generatedAt", "2026-07-15T12:00:01.000Z");
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
