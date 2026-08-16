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
  private static final String INDEX_DIGEST =
      "sha256:5c855ad7b85e68e48a62f34662853f38b57c1c1d80f3a927ab58034fd6d31c5e";
  private static final String PLATFORM_MANIFEST_DIGEST =
      "sha256:9c1534cbf839ec70409508a874f4c02bf4739de2f32f77efe080eef1cfd34bf4";
  private static final String CONFIG_DIGEST =
      "sha256:07f76768a0c956d6e9bddbcdb3c2be7fd9fd45ee6174a26873f8219fccbad65d";
  private static final String DIFFERENT_DIGEST = "sha256:" + "c".repeat(64);
  private static final String REQUIRED_REFERENCE = "postgres@" + INDEX_DIGEST;
  private static final String INDEX_MEDIA_TYPE = "application/vnd.oci.image.index.v1+json";
  private static final String MANIFEST_MEDIA_TYPE =
      "application/vnd.oci.image.manifest.v1+json";
  private static final String CONFIG_MEDIA_TYPE = "application/vnd.oci.image.config.v1+json";

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
        valid.deepCopy().put("repoDigestMembership", "FAIL"),
        requirements,
        "POSTGRES_REPO_DIGEST_MEMBERSHIP_MISSING");
    final ObjectNode wrongExecutedDomain = valid.deepCopy();
    wrongExecutedDomain
        .withObject("executedObservedIdentity")
        .put("kind", "CONFIG_DIGEST")
        .put("digest", PLATFORM_MANIFEST_DIGEST)
        .put("mediaType", CONFIG_MEDIA_TYPE);
    assertBlocked(
        wrongExecutedDomain,
        requirements,
        "POSTGRES_EXECUTED_PLATFORM_MISMATCH");
    assertBlocked(
        valid.deepCopy().remove("requiredIndexDigest"),
        requirements,
        "POSTGRES_INDEX_DIGEST_INVALID");
    assertBlocked(
        valid.deepCopy().put("resolvedPlatformManifestDigest", DIFFERENT_DIGEST),
        requirements,
        "POSTGRES_PLATFORM_MANIFEST_MISMATCH");
    assertBlocked(
        valid.deepCopy().put("resolvedPlatformConfigDigest", DIFFERENT_DIGEST),
        requirements,
        "POSTGRES_CONFIG_DIGEST_MISMATCH");
    final ObjectNode unknownLocal = valid.deepCopy();
    unknownLocal.withObject("localObservedIdentity").put("kind", "UNKNOWN");
    assertBlocked(
        unknownLocal,
        requirements,
        "POSTGRES_LOCAL_IDENTITY_UNKNOWN");
    final ObjectNode unknownExecuted = valid.deepCopy();
    unknownExecuted.withObject("executedObservedIdentity").put("kind", "UNKNOWN");
    assertBlocked(
        unknownExecuted,
        requirements,
        "POSTGRES_EXECUTED_IDENTITY_UNKNOWN");
    assertBlocked(
        valid.deepCopy().put("requiredImageReference", "postgres:17"),
        requirements,
        "POSTGRES_INDEX_DIGEST_INVALID");
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
  void identityEqualityRequiresTheSameExplicitDomain() {
    final var index =
        new Qdr7CapacityEnvironmentAdmission.ImageIdentity(
            Qdr7CapacityEnvironmentAdmission.ImageIdentityKind.OCI_INDEX_DIGEST,
            INDEX_DIGEST,
            INDEX_MEDIA_TYPE);
    final var sameIndex =
        new Qdr7CapacityEnvironmentAdmission.ImageIdentity(
            Qdr7CapacityEnvironmentAdmission.ImageIdentityKind.OCI_INDEX_DIGEST,
            INDEX_DIGEST,
            INDEX_MEDIA_TYPE);
    final var sameValueDifferentDomain =
        new Qdr7CapacityEnvironmentAdmission.ImageIdentity(
            Qdr7CapacityEnvironmentAdmission.ImageIdentityKind.CONFIG_DIGEST,
            INDEX_DIGEST,
            CONFIG_MEDIA_TYPE);
    final var sameDomainAndValueDifferentMediaType =
        new Qdr7CapacityEnvironmentAdmission.ImageIdentity(
            Qdr7CapacityEnvironmentAdmission.ImageIdentityKind.OCI_INDEX_DIGEST,
            INDEX_DIGEST,
            MANIFEST_MEDIA_TYPE);

    assertThat(Qdr7CapacityEnvironmentAdmission.identitiesMatch(index, sameIndex)).isTrue();
    assertThat(Qdr7CapacityEnvironmentAdmission.identitiesMatch(index, sameValueDifferentDomain))
        .isFalse();
    assertThat(
            Qdr7CapacityEnvironmentAdmission.identitiesMatch(
                index, sameDomainAndValueDifferentMediaType))
        .isFalse();
  }

  @Test
  void ociIndexResolutionIsExactUnambiguousAndFailClosed() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode requirements = requirements(mapper);
    final ObjectNode validIndex = mapper.createObjectNode();
    validIndex.put("mediaType", INDEX_MEDIA_TYPE);
    final ObjectNode descriptor = validIndex.putArray("manifests").addObject();
    descriptor.put("mediaType", MANIFEST_MEDIA_TYPE);
    descriptor.put("digest", PLATFORM_MANIFEST_DIGEST);
    descriptor.putObject("platform").put("os", "linux").put("architecture", "amd64").put("variant", "");

    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolvePlatformDescriptor(validIndex, requirements)
                .passed())
        .isTrue();

    final ObjectNode missing = validIndex.deepCopy();
    ((ObjectNode) missing.path("manifests").get(0).path("platform")).put("architecture", "arm64");
    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolvePlatformDescriptor(missing, requirements)
                .blocker())
        .isEqualTo("POSTGRES_PLATFORM_NOT_FOUND");

    final ObjectNode ambiguous = validIndex.deepCopy();
    ambiguous.withArray("manifests").add(descriptor.deepCopy());
    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolvePlatformDescriptor(ambiguous, requirements)
                .blocker())
        .isEqualTo("POSTGRES_PLATFORM_AMBIGUOUS");

    final ObjectNode wrongDigest = validIndex.deepCopy();
    ((ObjectNode) wrongDigest.path("manifests").get(0)).put("digest", DIFFERENT_DIGEST);
    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolvePlatformDescriptor(wrongDigest, requirements)
                .blocker())
        .isEqualTo("POSTGRES_PLATFORM_MANIFEST_MISMATCH");

    final ObjectNode unsupported = validIndex.deepCopy();
    unsupported.put("mediaType", "application/octet-stream");
    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolvePlatformDescriptor(unsupported, requirements)
                .blocker())
        .isEqualTo("POSTGRES_INDEX_MEDIA_TYPE_INVALID");
  }

  @Test
  void platformManifestConfigResolutionRejectsWrongDigestAndMediaType() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode requirements = requirements(mapper);
    final ObjectNode manifest = mapper.createObjectNode();
    manifest.put("mediaType", MANIFEST_MEDIA_TYPE);
    manifest
        .putObject("config")
        .put("mediaType", CONFIG_MEDIA_TYPE)
        .put("digest", CONFIG_DIGEST);

    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolveConfigDescriptor(manifest, requirements)
                .passed())
        .isTrue();
    final ObjectNode wrongDigest = manifest.deepCopy();
    ((ObjectNode) wrongDigest.path("config")).put("digest", DIFFERENT_DIGEST);
    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolveConfigDescriptor(wrongDigest, requirements)
                .blocker())
        .isEqualTo("POSTGRES_CONFIG_DIGEST_MISMATCH");
    final ObjectNode wrongMedia = manifest.deepCopy();
    ((ObjectNode) wrongMedia.path("config")).put("mediaType", "application/octet-stream");
    assertThat(
            Qdr7CapacityEnvironmentAdmission.resolveConfigDescriptor(wrongMedia, requirements)
                .blocker())
        .isEqualTo("POSTGRES_CONFIG_DIGEST_MISMATCH");
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
  void deprecatedV1FieldsCannotInfluenceTheV2Verdict() throws IOException {
    final ObjectMapper mapper = new ObjectMapper();
    final ObjectNode snapshot = validSnapshot(mapper);
    snapshot.put("postgresImageId", DIFFERENT_DIGEST);
    snapshot.put("postgresImageIdentityDomain", "CONFIG_IMAGE_ID");
    snapshot.put("postgresExpectedCanonicalImageId", DIFFERENT_DIGEST);
    snapshot.put("postgresExecutedImageId", DIFFERENT_DIGEST);
    snapshot.put("postgresImageDigestVerified", false);

    assertThat(Qdr7CapacityEnvironmentAdmission.evaluate(snapshot, requirements(mapper)).status())
        .isEqualTo("QUALIFIED");
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
    assertThat(qualified.path("identityContractId").asText())
        .isEqualTo("POSTGRES_IMAGE_IDENTITY_CONTRACT_V2");
    assertThat(qualified.path("resolvedPlatformManifestDigest").asText())
        .isEqualTo(PLATFORM_MANIFEST_DIGEST);
    assertThat(qualified.path("resolvedPlatformConfigDigest").asText())
        .isEqualTo(CONFIG_DIGEST);
    assertThat(qualified.path("repoDigestMembership").asText()).isEqualTo("PASS");
    assertThat(qualified.path("executedObservedIdentity").path("kind").asText())
        .isIn("PLATFORM_MANIFEST_DIGEST", "CONFIG_DIGEST");
    assertThat(qualified.path("executedConfigDigest").asText()).isEqualTo(CONFIG_DIGEST);
    assertThat(qualified.path("immutableBindingResult").asText()).isEqualTo("PASS");
    assertThat(qualified.path("identityBlockers")).isEmpty();
    assertThat(qualified.path("postgresExecutedImageReference").asText()).isNotBlank();
    final JsonNode diagnostics = qualified.path("postgresImageIdentityDiagnostics");
    assertThat(diagnostics.path("requiredImageReference").asText())
        .isEqualTo(REQUIRED_REFERENCE);
    assertThat(diagnostics.path("executedIdentityProof").path("result").asText())
        .isEqualTo("PASS");
    assertThat(diagnostics.path("admission").path("blockerCount").asInt()).isZero();
    assertThat(diagnostics.path("admission").path("blockers")).isEmpty();
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
            (containerId, reportedImageId, manifest, contract, objectMapper) ->
                new Qdr7CapacityEnvironmentAdmission.ExecutedIdentityProof(
                    new Qdr7CapacityEnvironmentAdmission.ImageIdentity(
                        Qdr7CapacityEnvironmentAdmission.ImageIdentityKind.CONFIG_DIGEST,
                        DIFFERENT_DIGEST,
                        CONFIG_MEDIA_TYPE),
                    "",
                    DIFFERENT_DIGEST,
                    "NEGATIVE_TEST",
                    "POSTGRES_EXECUTED_PLATFORM_MISMATCH"));

    assertThat(evaluation.status()).isEqualTo("NOT_QUALIFIED");
    assertThat(evaluation.blockers()).contains("POSTGRES_EXECUTED_PLATFORM_MISMATCH");
    final JsonNode rejected = mapper.readTree(environment.toFile());
    assertThat(rejected.path("executedObservedIdentity").path("digest").asText())
        .isEqualTo(DIFFERENT_DIGEST);
    assertThat(rejected.path("immutableBindingResult").asText()).isEqualTo("FAIL");
    assertThat(rejected.path("repoDigestMembership").asText()).isEqualTo("PASS");
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
    snapshot.put("identityContractId", "POSTGRES_IMAGE_IDENTITY_CONTRACT_V2");
    snapshot.put("identityContractVersion", 2);
    snapshot.put("requiredImageReference", REQUIRED_REFERENCE);
    snapshot.put("requiredIndexDigest", INDEX_DIGEST);
    snapshot.put("requiredIndexMediaType", INDEX_MEDIA_TYPE);
    snapshot
        .putObject("targetPlatform")
        .put("os", "linux")
        .put("architecture", "amd64")
        .put("variant", "");
    snapshot.put("resolvedPlatformManifestDigest", PLATFORM_MANIFEST_DIGEST);
    snapshot.put("resolvedPlatformManifestMediaType", MANIFEST_MEDIA_TYPE);
    snapshot.put("resolvedPlatformConfigDigest", CONFIG_DIGEST);
    snapshot.put("repoDigestMembership", "PASS");
    snapshot
        .putObject("localObservedIdentity")
        .put("kind", "CONFIG_DIGEST")
        .put("digest", CONFIG_DIGEST)
        .put("mediaType", CONFIG_MEDIA_TYPE);
    snapshot
        .putObject("executedObservedIdentity")
        .put("kind", "PLATFORM_MANIFEST_DIGEST")
        .put("digest", PLATFORM_MANIFEST_DIGEST)
        .put("mediaType", MANIFEST_MEDIA_TYPE);
    snapshot.put("executedPlatformManifestDigest", PLATFORM_MANIFEST_DIGEST);
    snapshot.put("executedConfigDigest", CONFIG_DIGEST);
    snapshot.put("immutableBindingResult", "PASS");
    snapshot.putArray("identityBlockers");
    snapshot.put("postgresImageId", CONFIG_DIGEST);
    snapshot.put("postgresImageIdentityDomain", "CONFIG_DIGEST");
    snapshot.put("postgresImageReference", REQUIRED_REFERENCE);
    snapshot.put("postgresImageDigestVerified", true);
    snapshot.put("postgresExpectedCanonicalImageId", CONFIG_DIGEST);
    snapshot.put("postgresExecutedImageId", PLATFORM_MANIFEST_DIGEST);
    snapshot.putArray("postgresExpectedRepoDigests").add(REQUIRED_REFERENCE);
    snapshot
        .putObject("legacyPostgresIdentityFields")
        .put("status", "DEPRECATED_NOT_USED_FOR_V2_DECISION")
        .put("decisionContract", "POSTGRES_IMAGE_IDENTITY_CONTRACT_V2");
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
