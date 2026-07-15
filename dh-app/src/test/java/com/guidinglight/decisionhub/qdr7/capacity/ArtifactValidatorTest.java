package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

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
}
