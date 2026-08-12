package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Qdr7CapacityEnvironmentAdmissionTest {

  private static final String SHA = "a".repeat(40);
  private static final String HASH = "b".repeat(64);
  private static final String POSTGRES_IMAGE_ID =
      "sha256:5c855ad7b85e68e48a62f34662853f38b57c1c1d80f3a927ab58034fd6d31c5e";
  private static final String DIFFERENT_IMAGE_ID = "sha256:" + "c".repeat(64);

  @TempDir Path temporaryDirectory;

  @Test
  void qualifiedSnapshotPassesAndEveryRequiredNegativeCaseBlocks() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode requirements = requirements(mapper);
    final ObjectNode valid = validSnapshot(mapper);

    assertThat(Qdr7CapacityEnvironmentAdmission.evaluate(valid, requirements).status())
        .isEqualTo("QUALIFIED");
    assertBlocked(
        valid.deepCopy().put("dockerDaemonAvailable", false), requirements, "DOCKER_UNAVAILABLE");
    assertBlocked(
        valid.deepCopy().put("postgresImageAvailable", false),
        requirements,
        "POSTGRES_IMAGE_UNAVAILABLE");
    assertBlocked(
        valid.deepCopy().put("testcontainersViable", false),
        requirements,
        "TESTCONTAINERS_POSTGRES_SMOKE_FAILED");
    assertBlocked(
        valid.deepCopy().put("availableMemoryBytes", 1L), requirements, "HOST_MEMORY_INSUFFICIENT");
    assertBlocked(
        valid.deepCopy().remove("availableMemoryBytes"),
        requirements,
        "HOST_MEMORY_OBSERVATION_INVALID");
    assertBlocked(valid.deepCopy().put("diskFreeBytes", 1L), requirements, "DISK_INSUFFICIENT");
    assertBlocked(valid.deepCopy().put("worktreeClean", false), requirements, "WORKTREE_DIRTY");
    assertBlocked(
        valid.deepCopy().put("networkIsolationVerified", false),
        requirements,
        "NETWORK_ISOLATION_UNVERIFIED");
    assertBlocked(
        valid.deepCopy().put("disallowedNetworkAdapterCount", 1),
        requirements,
        "NETWORK_ADAPTER_OUTSIDE_ALLOWLIST");
    assertBlocked(
        valid.deepCopy().put("postgresImageDigestVerified", false),
        requirements,
        "POSTGRES_IMAGE_DIGEST_MISMATCH");
    assertBlocked(
        valid.deepCopy().put("postgresExecutedImageId", DIFFERENT_IMAGE_ID),
        requirements,
        "POSTGRES_EXECUTED_IMAGE_MISMATCH");
    assertBlocked(
        valid.deepCopy().remove("postgresExpectedCanonicalImageId"),
        requirements,
        "POSTGRES_EXPECTED_IMAGE_OBSERVATION_INVALID");
    assertBlocked(
        valid.deepCopy().put("postgresExpectedCanonicalImageId", "sha256:abc"),
        requirements,
        "POSTGRES_EXPECTED_IMAGE_MISMATCH");
    assertBlocked(
        valid.deepCopy().remove("postgresExecutedImageId"),
        requirements,
        "POSTGRES_EXECUTED_IMAGE_OBSERVATION_INVALID");
    assertBlocked(
        valid.deepCopy().put("postgresExecutedImageId", "sha256:abc"),
        requirements,
        "POSTGRES_EXECUTED_IMAGE_MISMATCH");
    assertBlocked(
        valid.deepCopy().put("postgresImageReference", "postgres:17"),
        requirements,
        "POSTGRES_IMAGE_REFERENCE_MISMATCH");
    final ObjectNode credential = valid.deepCopy();
    credential.withArray("credentialVariablesPresent").add("OPENAI_API_KEY");
    assertBlocked(credential, requirements, "CREDENTIAL_ENVIRONMENT_PRESENT");
  }

  @Test
  void immutableRequirementsCannotBeSuppliedByTheObservedSnapshot() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode requirements = (ObjectNode) requirements(mapper);
    final ObjectNode valid = validSnapshot(mapper);

    valid.put("minimumLogicalCpu", 1);
    valid.put("minimumAvailableMemoryBytes", 1L);
    valid.put("minimumDockerMemoryBytes", 1L);
    assertThat(Qdr7CapacityEnvironmentAdmission.evaluate(valid, requirements).status())
        .isEqualTo("QUALIFIED");

    final ObjectNode missingCpuRequirement = requirements.deepCopy();
    missingCpuRequirement.remove("minimumLogicalCpu");
    assertBlocked(valid, missingCpuRequirement, "CPU_REQUIREMENT_INVALID");
    assertBlocked(
        valid,
        requirements.deepCopy().put("minimumAvailableMemoryBytes", "17179869184"),
        "HOST_MEMORY_REQUIREMENT_INVALID");
    assertBlocked(
        valid,
        requirements.deepCopy().put("minimumDockerMemoryBytes", 64L * 1024L * 1024L * 1024L),
        "DOCKER_MEMORY_INSUFFICIENT");
  }

  @Test
  void canonicalImageIdentityIsStrictContentAddressedAndFailClosed() {
    assertThat(Qdr7CapacityEnvironmentAdmission.canonicalImageId(POSTGRES_IMAGE_ID))
        .isEqualTo(POSTGRES_IMAGE_ID);
    assertThat(Qdr7CapacityEnvironmentAdmission.canonicalImageId(POSTGRES_IMAGE_ID.substring(7)))
        .isEqualTo(POSTGRES_IMAGE_ID);
    assertThat(
            Qdr7CapacityEnvironmentAdmission.canonicalImageId(
                "  " + POSTGRES_IMAGE_ID + "  "))
        .isEqualTo(POSTGRES_IMAGE_ID);
    assertThat(
            Qdr7CapacityEnvironmentAdmission.canonicalImageIdsMatch(
                POSTGRES_IMAGE_ID, POSTGRES_IMAGE_ID.substring(7)))
        .isTrue();
    assertThat(
            Qdr7CapacityEnvironmentAdmission.canonicalImageIdsMatch(
                POSTGRES_IMAGE_ID, DIFFERENT_IMAGE_ID))
        .isFalse();
    assertThat(Qdr7CapacityEnvironmentAdmission.canonicalImageId("sha256:abc")).isEmpty();
    assertThat(Qdr7CapacityEnvironmentAdmission.canonicalImageId("postgres:17")).isEmpty();
    assertThat(Qdr7CapacityEnvironmentAdmission.canonicalImageId("SHA256:" + "a".repeat(64)))
        .isEmpty();
    assertThat(Qdr7CapacityEnvironmentAdmission.canonicalImageId(" ")).isEmpty();
  }

  @Test
  void authoritativeDockerResolutionRejectsTagMutationAndInspectFailure() {
    final String expected =
        Qdr7CapacityEnvironmentAdmission.resolveCanonicalImageId(
            "postgres:17", ignored -> POSTGRES_IMAGE_ID);
    final String executedSameContent =
        Qdr7CapacityEnvironmentAdmission.resolveCanonicalImageId(
            "sha256:runtime-display", ignored -> POSTGRES_IMAGE_ID.substring(7));
    final String sameTagMutated =
        Qdr7CapacityEnvironmentAdmission.resolveCanonicalImageId(
            "postgres:17", ignored -> DIFFERENT_IMAGE_ID);

    assertThat(Qdr7CapacityEnvironmentAdmission.canonicalImageIdsMatch(expected, executedSameContent))
        .isTrue();
    assertThat(Qdr7CapacityEnvironmentAdmission.canonicalImageIdsMatch(expected, sameTagMutated))
        .isFalse();
    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolveCanonicalImageId(
                "postgres:17", ignored -> null))
        .isEmpty();
    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolveCanonicalImageId(
                "postgres:17",
                ignored -> {
                  throw new IllegalStateException("inspect unavailable");
                }))
        .isEmpty();
  }

  @Test
  void malformedTypesMissingRequirementsAndNegativeMeasurementsFailClosed() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode requirements = (ObjectNode) requirements(mapper);
    final ObjectNode valid = validSnapshot(mapper);

    assertBlocked(
        valid.deepCopy().put("worktreeClean", "true"),
        requirements,
        "WORKTREE_CLEAN_OBSERVATION_INVALID");
    assertBlocked(
        valid.deepCopy().put("javaMajor", "21"),
        requirements,
        "JAVA_MAJOR_OBSERVATION_INVALID");
    assertBlocked(
        valid,
        requirements.deepCopy().remove("minimumDiskFreeBytes"),
        "DISK_REQUIREMENT_INVALID");
    assertBlocked(
        valid.deepCopy().put("clockOffsetMilliseconds", -1),
        requirements,
        "CLOCK_OFFSET_OBSERVATION_INVALID");
    assertBlocked(
        valid.deepCopy().put("backgroundAverageCpuPercent", -1),
        requirements,
        "BACKGROUND_AVERAGE_CPU_OBSERVATION_INVALID");
    assertBlocked(
        valid.deepCopy().put("attemptContainerAbsent", "true"),
        requirements,
        "ATTEMPT_CONTAINER_ABSENCE_OBSERVATION_INVALID");
  }

  @Test
  void dedicatedTestcontainersPostgres17SmokeQualifiesAndBindsManifest() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final Path environment = temporaryDirectory.resolve("capacity-environment-manifest.json");
    final Path registry = temporaryDirectory.resolve("resource-registry.json");
    final Path requirements = temporaryDirectory.resolve("environment-admission.json");
    final ObjectNode snapshot = validSnapshot(mapper);
    snapshot.put("testcontainersViable", false);
    snapshot.put("postgresMajor", 0);
    mapper.writeValue(environment.toFile(), snapshot);
    mapper.writeValue(
        registry.toFile(), mapper.createObjectNode().put("runId", "20260809T000000Z"));
    Files.copy(
        repositoryRoot().resolve("config/qdr7-capacity/qdr7-capacity-environment-admission.json"),
        requirements);

    final Qdr7CapacityEnvironmentAdmission.Evaluation evaluation =
        Qdr7CapacityEnvironmentAdmission.runPostgresSmokeAndQualify(
            environment, registry, requirements, mapper);

    assertThat(evaluation.status()).isEqualTo("QUALIFIED");
    final JsonNode qualified = mapper.readTree(environment.toFile());
    assertThat(qualified.path("testcontainersViable").asBoolean()).isTrue();
    assertThat(qualified.path("postgresMajor").asInt()).isEqualTo(17);
    assertThat(qualified.path("postgresExpectedCanonicalImageId").asText())
        .isEqualTo(qualified.path("postgresExecutedImageId").asText())
        .matches("^sha256:[a-f0-9]{64}$");
    assertThat(qualified.path("postgresExecutedImageReference").asText()).isNotBlank();
    assertThat(qualified.path("environmentManifestHash").asText()).matches("^[a-f0-9]{64}$");
    assertThat(mapper.readTree(registry.toFile()).path("environmentManifestHash").asText())
        .isEqualTo(qualified.path("environmentManifestHash").asText());
  }

  @Test
  void dedicatedPostgresSmokeRejectsDifferentExecutedImmutableIdentity() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final Path environment = temporaryDirectory.resolve("negative-capacity-environment.json");
    final Path registry = temporaryDirectory.resolve("negative-resource-registry.json");
    final Path requirements = temporaryDirectory.resolve("negative-environment-admission.json");
    mapper.writeValue(environment.toFile(), validSnapshot(mapper));
    mapper.writeValue(
        registry.toFile(), mapper.createObjectNode().put("runId", "20260809T000001Z"));
    Files.copy(
        repositoryRoot().resolve("config/qdr7-capacity/qdr7-capacity-environment-admission.json"),
        requirements);

    final Qdr7CapacityEnvironmentAdmission.Evaluation evaluation =
        Qdr7CapacityEnvironmentAdmission.runPostgresSmokeAndQualify(
            environment,
            registry,
            requirements,
            mapper,
            reference ->
                reference.startsWith("postgres@") ? POSTGRES_IMAGE_ID : DIFFERENT_IMAGE_ID);

    assertThat(evaluation.status()).isEqualTo("NOT_QUALIFIED");
    assertThat(evaluation.blockers()).contains("POSTGRES_EXECUTED_IMAGE_MISMATCH");
    final JsonNode rejected = mapper.readTree(environment.toFile());
    assertThat(rejected.path("postgresExpectedCanonicalImageId").asText())
        .isEqualTo(POSTGRES_IMAGE_ID);
    assertThat(rejected.path("postgresExecutedImageId").asText())
        .isEqualTo(DIFFERENT_IMAGE_ID);
  }

  private static void assertBlocked(
      final JsonNode snapshot, final JsonNode requirements, final String blocker) {
    final Qdr7CapacityEnvironmentAdmission.Evaluation evaluation =
        Qdr7CapacityEnvironmentAdmission.evaluate(snapshot, requirements);
    assertThat(evaluation.status()).isEqualTo("NOT_QUALIFIED");
    assertThat(evaluation.blockers()).contains(blocker);
  }

  private static ObjectNode validSnapshot(final ObjectMapper mapper) {
    final ObjectNode snapshot = mapper.createObjectNode();
    snapshot.put("candidateSha", SHA);
    snapshot.put("originSha", SHA);
    snapshot.put("advertisedSha", SHA);
    snapshot.put("candidateTree", SHA);
    snapshot.put("worktreeClean", true);
    snapshot.put("stagedEmpty", true);
    snapshot.put("untrackedTechnicalCount", 0);
    snapshot.put("javaMajor", 21);
    snapshot.put("mavenVersion", "3.9.12");
    snapshot.put("powerShellPathHash", HASH);
    snapshot.put("dockerDaemonAvailable", true);
    snapshot.put("dockerHealthy", true);
    snapshot.put("dockerStorageDriver", "overlayfs");
    snapshot.put("dockerMemoryBytes", 24L * 1024L * 1024L * 1024L);
    snapshot.put("minimumDockerMemoryBytes", 16L * 1024L * 1024L * 1024L);
    snapshot.put("postgresImageAvailable", true);
    snapshot.put("postgresImageId", POSTGRES_IMAGE_ID);
    snapshot.put(
        "postgresImageReference",
        "postgres@sha256:5c855ad7b85e68e48a62f34662853f38b57c1c1d80f3a927ab58034fd6d31c5e");
    snapshot.put("postgresImageDigestVerified", true);
    snapshot.put("postgresExpectedCanonicalImageId", POSTGRES_IMAGE_ID);
    snapshot.put("postgresExecutedImageId", POSTGRES_IMAGE_ID);
    snapshot.put("testcontainersViable", true);
    snapshot.put("postgresMajor", 17);
    snapshot.put("logicalCpu", 32);
    snapshot.put("minimumLogicalCpu", 16);
    snapshot.put("availableMemoryBytes", 32L * 1024L * 1024L * 1024L);
    snapshot.put("minimumAvailableMemoryBytes", 16L * 1024L * 1024L * 1024L);
    snapshot.put("diskFreeBytes", 100L * 1024L * 1024L * 1024L);
    snapshot.put("filesystemWritable", true);
    snapshot.put("filesystemType", "NTFS");
    snapshot.put("clockSynchronized", true);
    snapshot.put("clockOffsetMilliseconds", 100);
    snapshot.put("loopbackPortAvailable", true);
    snapshot.put("networkPolicy", "LOOPBACK_AND_LOCAL_DOCKER_ONLY");
    snapshot.put("networkPolicyEvidenceType", "WINDOWS_ACTIVE_ADAPTER_ALLOWLIST_V1");
    snapshot.put("networkIsolationVerified", true);
    snapshot.put("disallowedNetworkAdapterCount", 0);
    snapshot.put("activeNetworkAdapterSetHash", HASH);
    snapshot.putArray("credentialVariablesPresent");
    snapshot.putArray("proxyVariablesPresent");
    snapshot.put("backgroundAverageCpuPercent", 10);
    snapshot.put("backgroundPeakCpuPercent", 20);
    snapshot.put("attemptContainerAbsent", true);
    snapshot.put("attemptVolumeAbsent", true);
    return snapshot;
  }

  private static JsonNode requirements(final ObjectMapper mapper) throws IOException {
    return mapper.readTree(
        repositoryRoot()
            .resolve("config/qdr7-capacity/qdr7-capacity-environment-admission.json")
            .toFile());
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
