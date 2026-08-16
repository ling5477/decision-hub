package com.guidinglight.decisionhub.qdr7.capacity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Stage-QDR-12 formal run 前的 machine-readable environment admission。 */
final class Qdr7CapacityEnvironmentAdmission {

  private static final String QUALIFIED = "QUALIFIED";
  private static final String NOT_QUALIFIED = "NOT_QUALIFIED";

  private Qdr7CapacityEnvironmentAdmission() {}

  static Evaluation evaluate(final JsonNode snapshot, final JsonNode requirements) {
    final List<String> blockers = new ArrayList<>();
    final String candidateSha =
        requiredText(snapshot, "candidateSha", "CANDIDATE_SHA_OBSERVATION_INVALID", blockers);
    final String originSha =
        requiredText(snapshot, "originSha", "ORIGIN_SHA_OBSERVATION_INVALID", blockers);
    final String advertisedSha =
        requiredText(snapshot, "advertisedSha", "ADVERTISED_SHA_OBSERVATION_INVALID", blockers);
    final long minimumDockerMemoryBytes =
        positiveIntegralRequirement(
            requirements,
            "minimumDockerMemoryBytes",
            "DOCKER_MEMORY_REQUIREMENT_INVALID",
            blockers);
    final long minimumLogicalCpu =
        positiveIntegralRequirement(
            requirements, "minimumLogicalCpu", "CPU_REQUIREMENT_INVALID", blockers);
    final long minimumAvailableMemoryBytes =
        positiveIntegralRequirement(
            requirements,
            "minimumAvailableMemoryBytes",
            "HOST_MEMORY_REQUIREMENT_INVALID",
            blockers);
    final long minimumDiskFreeBytes =
        positiveIntegralRequirement(
            requirements, "minimumDiskFreeBytes", "DISK_REQUIREMENT_INVALID", blockers);
    final double maximumClockOffsetMilliseconds =
        finiteRequirement(
            requirements,
            "maximumClockOffsetMilliseconds",
            0.0d,
            Double.MAX_VALUE,
            "CLOCK_OFFSET_REQUIREMENT_INVALID",
            blockers);
    final double maximumBackgroundAverageCpuPercent =
        finiteRequirement(
            requirements,
            "maximumBackgroundAverageCpuPercent",
            0.0d,
            100.0d,
            "BACKGROUND_AVERAGE_CPU_REQUIREMENT_INVALID",
            blockers);
    final double maximumBackgroundPeakCpuPercent =
        finiteRequirement(
            requirements,
            "maximumBackgroundPeakCpuPercent",
            0.0d,
            100.0d,
            "BACKGROUND_PEAK_CPU_REQUIREMENT_INVALID",
            blockers);
    require(
        sha(candidateSha) && candidateSha.equals(originSha) && candidateSha.equals(advertisedSha),
        "CANDIDATE_SHA_MISMATCH",
        blockers);
    require(
        sha(requiredText(snapshot, "candidateTree", "CANDIDATE_TREE_OBSERVATION_INVALID", blockers)),
        "CANDIDATE_TREE_INVALID",
        blockers);
    require(
        requiredBoolean(snapshot, "worktreeClean", "WORKTREE_CLEAN_OBSERVATION_INVALID", blockers),
        "WORKTREE_DIRTY",
        blockers);
    require(
        requiredBoolean(snapshot, "stagedEmpty", "STAGED_EMPTY_OBSERVATION_INVALID", blockers),
        "STAGED_NOT_EMPTY",
        blockers);
    require(
        nonNegativeIntegral(
                snapshot,
                "untrackedTechnicalCount",
                "UNTRACKED_TECHNICAL_COUNT_OBSERVATION_INVALID",
                blockers)
            == 0L,
        "UNTRACKED_TECHNICAL_FILES",
        blockers);
    require(
        positiveIntegral(snapshot, "javaMajor", "JAVA_MAJOR_OBSERVATION_INVALID", blockers) == 21L,
        "JAVA_VERSION_UNQUALIFIED",
        blockers);
    require(
        requiredText(snapshot, "mavenVersion", "MAVEN_VERSION_OBSERVATION_INVALID", blockers)
            .matches("^3\\.9\\..+$"),
        "MAVEN_VERSION_UNQUALIFIED",
        blockers);
    require(
        sha256(
            requiredText(
                snapshot,
                "powerShellPathHash",
                "POWERSHELL_IDENTITY_OBSERVATION_INVALID",
                blockers)),
        "POWERSHELL_IDENTITY_UNQUALIFIED",
        blockers);
    require(
        requiredBoolean(
            snapshot,
            "dockerDaemonAvailable",
            "DOCKER_DAEMON_OBSERVATION_INVALID",
            blockers),
        "DOCKER_UNAVAILABLE",
        blockers);
    require(
        requiredBoolean(snapshot, "dockerHealthy", "DOCKER_HEALTH_OBSERVATION_INVALID", blockers),
        "DOCKER_UNHEALTHY",
        blockers);
    require(
        containsRequiredText(
            requirements,
            "requiredDockerStorageDrivers",
            requiredText(
                snapshot,
                "dockerStorageDriver",
                "DOCKER_STORAGE_OBSERVATION_INVALID",
                blockers),
            "DOCKER_STORAGE_REQUIREMENT_INVALID",
            blockers),
        "DOCKER_STORAGE_UNQUALIFIED",
        blockers);
    requirePositiveObservedAtLeast(
        snapshot,
        "dockerMemoryBytes",
        minimumDockerMemoryBytes,
        "DOCKER_MEMORY_OBSERVATION_INVALID",
        "DOCKER_MEMORY_INSUFFICIENT",
        blockers);
    evaluatePostgresIdentityV2(snapshot, requirements, blockers);
    require(
        requiredBoolean(
            snapshot,
            "testcontainersViable",
            "TESTCONTAINERS_VIABILITY_OBSERVATION_INVALID",
            blockers),
        "TESTCONTAINERS_POSTGRES_SMOKE_FAILED",
        blockers);
    require(
        positiveIntegral(snapshot, "postgresMajor", "POSTGRES_MAJOR_OBSERVATION_INVALID", blockers)
            == positiveIntegralRequirement(
                requirements,
                "postgresMajor",
                "POSTGRES_MAJOR_REQUIREMENT_INVALID",
                blockers),
        "POSTGRES_MAJOR_MISMATCH",
        blockers);
    requirePositiveObservedAtLeast(
        snapshot,
        "logicalCpu",
        minimumLogicalCpu,
        "CPU_OBSERVATION_INVALID",
        "CPU_INSUFFICIENT",
        blockers);
    requirePositiveObservedAtLeast(
        snapshot,
        "availableMemoryBytes",
        minimumAvailableMemoryBytes,
        "HOST_MEMORY_OBSERVATION_INVALID",
        "HOST_MEMORY_INSUFFICIENT",
        blockers);
    requirePositiveObservedAtLeast(
        snapshot,
        "diskFreeBytes",
        minimumDiskFreeBytes,
        "DISK_OBSERVATION_INVALID",
        "DISK_INSUFFICIENT",
        blockers);
    require(
        requiredBoolean(
            snapshot,
            "filesystemWritable",
            "FILESYSTEM_WRITABLE_OBSERVATION_INVALID",
            blockers),
        "FILESYSTEM_NOT_WRITABLE",
        blockers);
    require(
        containsRequiredText(
            requirements,
            "requiredFilesystemTypes",
            requiredText(
                snapshot, "filesystemType", "FILESYSTEM_TYPE_OBSERVATION_INVALID", blockers),
            "FILESYSTEM_TYPE_REQUIREMENT_INVALID",
            blockers),
        "FILESYSTEM_TYPE_UNQUALIFIED",
        blockers);
    require(
        requiredBoolean(
            snapshot,
            "clockSynchronized",
            "CLOCK_SYNCHRONIZATION_OBSERVATION_INVALID",
            blockers),
        "CLOCK_NOT_SYNCHRONIZED",
        blockers);
    final double clockOffsetMilliseconds =
        finiteObservation(
            snapshot,
            "clockOffsetMilliseconds",
            0.0d,
            Double.MAX_VALUE,
            "CLOCK_OFFSET_OBSERVATION_INVALID",
            blockers);
    require(
        clockOffsetMilliseconds <= maximumClockOffsetMilliseconds,
        "CLOCK_OFFSET_EXCEEDED",
        blockers);
    require(
        requiredBoolean(
            snapshot,
            "loopbackPortAvailable",
            "LOOPBACK_PORT_OBSERVATION_INVALID",
            blockers),
        "LOOPBACK_PORT_UNAVAILABLE",
        blockers);
    final String requiredNetworkPolicy =
        requiredText(requirements, "networkPolicy", "NETWORK_POLICY_REQUIREMENT_INVALID", blockers);
    require(
        requiredNetworkPolicy.equals(
            requiredText(
                snapshot, "networkPolicy", "NETWORK_POLICY_OBSERVATION_INVALID", blockers)),
        "NETWORK_POLICY_MISMATCH",
        blockers);
    final String requiredNetworkPolicyEvidenceType =
        requiredText(
            requirements,
            "networkPolicyEvidenceType",
            "NETWORK_POLICY_EVIDENCE_REQUIREMENT_INVALID",
            blockers);
    require(
        requiredNetworkPolicyEvidenceType.equals(
            requiredText(
                snapshot,
                "networkPolicyEvidenceType",
                "NETWORK_POLICY_EVIDENCE_OBSERVATION_INVALID",
                blockers)),
        "NETWORK_POLICY_EVIDENCE_MISMATCH",
        blockers);
    require(
        requiredBoolean(
            snapshot,
            "networkIsolationVerified",
            "NETWORK_ISOLATION_OBSERVATION_INVALID",
            blockers),
        "NETWORK_ISOLATION_UNVERIFIED",
        blockers);
    require(
        nonNegativeIntegral(
                snapshot,
                "disallowedNetworkAdapterCount",
                "NETWORK_ADAPTER_COUNT_OBSERVATION_INVALID",
                blockers)
            == 0L,
        "NETWORK_ADAPTER_OUTSIDE_ALLOWLIST",
        blockers);
    require(
        sha256(
            requiredText(
                snapshot,
                "activeNetworkAdapterSetHash",
                "NETWORK_ADAPTER_EVIDENCE_OBSERVATION_INVALID",
                blockers)),
        "NETWORK_ADAPTER_EVIDENCE_INVALID",
        blockers);
    require(
        requiredEmptyTextArray(
            snapshot,
            "credentialVariablesPresent",
            "CREDENTIAL_ENVIRONMENT_OBSERVATION_INVALID",
            blockers),
        "CREDENTIAL_ENVIRONMENT_PRESENT",
        blockers);
    require(
        requiredEmptyTextArray(
            snapshot,
            "proxyVariablesPresent",
            "PROXY_ENVIRONMENT_OBSERVATION_INVALID",
            blockers),
        "EXTERNAL_PROXY_PRESENT",
        blockers);
    final double backgroundAverageCpuPercent =
        finiteObservation(
            snapshot,
            "backgroundAverageCpuPercent",
            0.0d,
            100.0d,
            "BACKGROUND_AVERAGE_CPU_OBSERVATION_INVALID",
            blockers);
    require(
        backgroundAverageCpuPercent <= maximumBackgroundAverageCpuPercent,
        "BACKGROUND_AVERAGE_CPU_EXCEEDED",
        blockers);
    final double backgroundPeakCpuPercent =
        finiteObservation(
            snapshot,
            "backgroundPeakCpuPercent",
            0.0d,
            100.0d,
            "BACKGROUND_PEAK_CPU_OBSERVATION_INVALID",
            blockers);
    require(
        backgroundPeakCpuPercent <= maximumBackgroundPeakCpuPercent,
        "BACKGROUND_PEAK_CPU_EXCEEDED",
        blockers);
    require(
        requiredBoolean(
            snapshot,
            "attemptContainerAbsent",
            "ATTEMPT_CONTAINER_ABSENCE_OBSERVATION_INVALID",
            blockers),
        "ATTEMPT_CONTAINER_ALREADY_EXISTS",
        blockers);
    require(
        requiredBoolean(
            snapshot,
            "attemptVolumeAbsent",
            "ATTEMPT_VOLUME_ABSENCE_OBSERVATION_INVALID",
            blockers),
        "ATTEMPT_VOLUME_ALREADY_EXISTS",
        blockers);
    return new Evaluation(blockers.isEmpty() ? QUALIFIED : NOT_QUALIFIED, List.copyOf(blockers));
  }

  private static void evaluatePostgresIdentityV2(
      final JsonNode snapshot, final JsonNode requirements, final List<String> blockers) {
    require(
        requiredBoolean(
            snapshot,
            "postgresImageAvailable",
            "POSTGRES_IMAGE_AVAILABILITY_OBSERVATION_INVALID",
            blockers),
        "POSTGRES_IMAGE_UNAVAILABLE",
        blockers);
    final String contractId =
        requiredText(
            requirements,
            "identityContractId",
            "POSTGRES_IDENTITY_DOMAIN_MISMATCH",
            blockers);
    final JsonNode requiredVersion = requirements.path("identityContractVersion");
    final boolean contractVersionValid =
        requiredVersion.isIntegralNumber() && requiredVersion.asInt() == 2;
    require(
        "POSTGRES_IMAGE_IDENTITY_CONTRACT_V2".equals(contractId) && contractVersionValid,
        "POSTGRES_IDENTITY_DOMAIN_MISMATCH",
        blockers);
    require(
        contractId.equals(
                requiredText(
                    snapshot,
                    "identityContractId",
                    "POSTGRES_IDENTITY_DOMAIN_MISMATCH",
                    blockers))
            && snapshot.path("identityContractVersion").isIntegralNumber()
            && snapshot.path("identityContractVersion").asInt() == 2,
        "POSTGRES_IDENTITY_DOMAIN_MISMATCH",
        blockers);

    final String requiredReference =
        requiredText(
            requirements,
            "requiredImageReference",
            "POSTGRES_INDEX_DIGEST_INVALID",
            blockers);
    final String requiredIndexDigest =
        requiredText(
            requirements,
            "requiredIndexDigest",
            "POSTGRES_INDEX_DIGEST_INVALID",
            blockers);
    final String requiredIndexMediaType =
        requiredText(
            requirements,
            "requiredIndexMediaType",
            "POSTGRES_INDEX_MEDIA_TYPE_INVALID",
            blockers);
    require(
        digest(requiredIndexDigest) && requiredReference.equals("postgres@" + requiredIndexDigest),
        "POSTGRES_INDEX_DIGEST_INVALID",
        blockers);
    require(
        "application/vnd.oci.image.index.v1+json".equals(requiredIndexMediaType),
        "POSTGRES_INDEX_MEDIA_TYPE_INVALID",
        blockers);
    require(
        requiredReference.equals(
                requiredText(
                    snapshot,
                    "requiredImageReference",
                    "POSTGRES_INDEX_DIGEST_INVALID",
                    blockers))
            && requiredIndexDigest.equals(
                requiredText(
                    snapshot,
                    "requiredIndexDigest",
                    "POSTGRES_INDEX_DIGEST_INVALID",
                    blockers))
            && requiredIndexMediaType.equals(
                requiredText(
                    snapshot,
                    "requiredIndexMediaType",
                    "POSTGRES_INDEX_MEDIA_TYPE_INVALID",
                    blockers)),
        "POSTGRES_INDEX_DIGEST_INVALID",
        blockers);

    final JsonNode requiredPlatform = requirements.path("targetPlatform");
    final String targetOs =
        requiredText(
            requiredPlatform, "os", "POSTGRES_PLATFORM_NOT_FOUND", blockers);
    final String targetArchitecture =
        requiredText(
            requiredPlatform, "architecture", "POSTGRES_PLATFORM_NOT_FOUND", blockers);
    final JsonNode variantNode = requiredPlatform.path("variant");
    final String targetVariant = variantNode.isTextual() ? variantNode.textValue() : null;
    require(
        "linux".equals(targetOs)
            && "amd64".equals(targetArchitecture)
            && "".equals(targetVariant),
        "POSTGRES_PLATFORM_NOT_FOUND",
        blockers);
    final JsonNode observedPlatform = snapshot.path("targetPlatform");
    require(
        observedPlatform.isObject()
            && targetOs.equals(observedPlatform.path("os").asText())
            && targetArchitecture.equals(observedPlatform.path("architecture").asText())
            && targetVariant != null
            && targetVariant.equals(observedPlatform.path("variant").asText(null)),
        "POSTGRES_PLATFORM_NOT_FOUND",
        blockers);

    final String expectedManifestDigest =
        requiredText(
            requirements,
            "expectedPlatformManifestDigest",
            "POSTGRES_PLATFORM_MANIFEST_MISMATCH",
            blockers);
    final String expectedManifestMediaType =
        requiredText(
            requirements,
            "expectedPlatformManifestMediaType",
            "POSTGRES_PLATFORM_MANIFEST_MISMATCH",
            blockers);
    final String expectedConfigDigest =
        requiredText(
            requirements,
            "expectedPlatformConfigDigest",
            "POSTGRES_CONFIG_DIGEST_MISMATCH",
            blockers);
    require(
        digest(expectedManifestDigest)
            && "application/vnd.oci.image.manifest.v1+json".equals(expectedManifestMediaType)
            && expectedManifestDigest.equals(
                requiredText(
                    snapshot,
                    "resolvedPlatformManifestDigest",
                    "POSTGRES_PLATFORM_MANIFEST_MISMATCH",
                    blockers))
            && expectedManifestMediaType.equals(
                requiredText(
                    snapshot,
                    "resolvedPlatformManifestMediaType",
                    "POSTGRES_PLATFORM_MANIFEST_MISMATCH",
                    blockers)),
        "POSTGRES_PLATFORM_MANIFEST_MISMATCH",
        blockers);
    require(
        digest(expectedConfigDigest)
            && expectedConfigDigest.equals(
                requiredText(
                    snapshot,
                    "resolvedPlatformConfigDigest",
                    "POSTGRES_CONFIG_DIGEST_MISMATCH",
                    blockers)),
        "POSTGRES_CONFIG_DIGEST_MISMATCH",
        blockers);
    require(
        "PASS"
            .equals(
                requiredText(
                    snapshot,
                    "repoDigestMembership",
                    "POSTGRES_REPO_DIGEST_MEMBERSHIP_MISSING",
                    blockers)),
        "POSTGRES_REPO_DIGEST_MEMBERSHIP_MISSING",
        blockers);

    final ImageIdentity localIdentity =
        readIdentity(
            snapshot.path("localObservedIdentity"),
            "POSTGRES_LOCAL_IDENTITY_UNKNOWN",
            blockers);
    final boolean localIndex =
        localIdentity.kind() == ImageIdentityKind.OCI_INDEX_DIGEST
            && requiredIndexDigest.equals(localIdentity.digest())
            && requiredIndexMediaType.equals(localIdentity.mediaType());
    final boolean localConfig =
        localIdentity.kind() == ImageIdentityKind.CONFIG_DIGEST
            && expectedConfigDigest.equals(localIdentity.digest())
            && "application/vnd.oci.image.config.v1+json".equals(localIdentity.mediaType());
    require(localIndex || localConfig, "POSTGRES_LOCAL_IDENTITY_UNKNOWN", blockers);

    final ImageIdentity executedIdentity =
        readIdentity(
            snapshot.path("executedObservedIdentity"),
            "POSTGRES_EXECUTED_IDENTITY_UNKNOWN",
            blockers);
    final boolean executedManifest =
        executedIdentity.kind() == ImageIdentityKind.PLATFORM_MANIFEST_DIGEST
            && expectedManifestDigest.equals(executedIdentity.digest())
            && expectedManifestMediaType.equals(executedIdentity.mediaType());
    final boolean executedConfig =
        executedIdentity.kind() == ImageIdentityKind.CONFIG_DIGEST
            && expectedConfigDigest.equals(executedIdentity.digest())
            && "application/vnd.oci.image.config.v1+json".equals(executedIdentity.mediaType());
    if (executedIdentity.kind() == ImageIdentityKind.UNKNOWN) {
      blockers.add("POSTGRES_EXECUTED_IDENTITY_UNKNOWN");
    } else {
      require(
          executedManifest || executedConfig,
          "POSTGRES_EXECUTED_PLATFORM_MISMATCH",
          blockers);
    }
    if (executedManifest) {
      require(
          expectedManifestDigest.equals(
              requiredText(
                  snapshot,
                  "executedPlatformManifestDigest",
                  "POSTGRES_EXECUTED_PLATFORM_MISMATCH",
                  blockers)),
          "POSTGRES_EXECUTED_PLATFORM_MISMATCH",
          blockers);
    }
    require(
        expectedConfigDigest.equals(
            requiredText(
                snapshot,
                "executedConfigDigest",
                "POSTGRES_CONFIG_DIGEST_MISMATCH",
                blockers)),
        "POSTGRES_CONFIG_DIGEST_MISMATCH",
        blockers);
    require(
        "PASS"
            .equals(
                requiredText(
                    snapshot,
                    "immutableBindingResult",
                    "POSTGRES_EXECUTED_PLATFORM_MISMATCH",
                    blockers)),
        "POSTGRES_EXECUTED_PLATFORM_MISMATCH",
        blockers);
    final JsonNode identityBlockers = snapshot.path("identityBlockers");
    if (!identityBlockers.isArray() || !identityBlockers.isEmpty()) {
      blockers.add("POSTGRES_IDENTITY_RESOLUTION_FAILED");
    }
  }

  static boolean identitiesMatch(final ImageIdentity left, final ImageIdentity right) {
    return left != null
        && right != null
        && left.kind() != ImageIdentityKind.UNKNOWN
        && left.kind() == right.kind()
        && digest(left.digest())
        && left.digest().equals(right.digest())
        && left.mediaType() != null
        && left.mediaType().equals(right.mediaType());
  }

  static DescriptorResolution resolvePlatformDescriptor(
      final JsonNode index, final JsonNode requirements) {
    if (!requirements.path("requiredIndexMediaType").asText().equals(index.path("mediaType").asText())) {
      return DescriptorResolution.failure("POSTGRES_INDEX_MEDIA_TYPE_INVALID");
    }
    final JsonNode target = requirements.path("targetPlatform");
    final List<JsonNode> eligible = new ArrayList<>();
    final JsonNode manifests = index.path("manifests");
    if (!manifests.isArray()) {
      return DescriptorResolution.failure("POSTGRES_IDENTITY_RESOLUTION_FAILED");
    }
    for (final JsonNode descriptor : manifests) {
      final JsonNode platform = descriptor.path("platform");
      if (target.path("os").asText().equals(platform.path("os").asText())
          && target.path("architecture").asText().equals(platform.path("architecture").asText())
          && target.path("variant").asText().equals(platform.path("variant").asText())) {
        eligible.add(descriptor);
      }
    }
    if (eligible.isEmpty()) {
      return DescriptorResolution.failure("POSTGRES_PLATFORM_NOT_FOUND");
    }
    if (eligible.size() != 1) {
      return DescriptorResolution.failure("POSTGRES_PLATFORM_AMBIGUOUS");
    }
    final JsonNode selected = eligible.getFirst();
    final String digestValue = selected.path("digest").asText();
    final String mediaType = selected.path("mediaType").asText();
    if (!requirements.path("expectedPlatformManifestDigest").asText().equals(digestValue)
        || !requirements.path("expectedPlatformManifestMediaType").asText().equals(mediaType)) {
      return DescriptorResolution.failure("POSTGRES_PLATFORM_MANIFEST_MISMATCH");
    }
    return new DescriptorResolution(digestValue, mediaType, "");
  }

  static ConfigResolution resolveConfigDescriptor(
      final JsonNode platformManifest, final JsonNode requirements) {
    if (!requirements
        .path("expectedPlatformManifestMediaType")
        .asText()
        .equals(platformManifest.path("mediaType").asText())) {
      return ConfigResolution.failure("POSTGRES_PLATFORM_MANIFEST_MISMATCH");
    }
    final JsonNode config = platformManifest.path("config");
    final String digestValue = config.path("digest").asText();
    if (!"application/vnd.oci.image.config.v1+json".equals(config.path("mediaType").asText())
        || !requirements.path("expectedPlatformConfigDigest").asText().equals(digestValue)) {
      return ConfigResolution.failure("POSTGRES_CONFIG_DIGEST_MISMATCH");
    }
    return new ConfigResolution(digestValue, config.path("mediaType").asText(), "");
  }

  private static ImageIdentity readIdentity(
      final JsonNode value, final String blocker, final List<String> blockers) {
    if (!value.isObject()) {
      blockers.add(blocker);
      return ImageIdentity.unknown();
    }
    final ImageIdentityKind kind;
    try {
      kind = ImageIdentityKind.valueOf(value.path("kind").asText("UNKNOWN"));
    } catch (final IllegalArgumentException invalidKind) {
      blockers.add(blocker);
      return ImageIdentity.unknown();
    }
    final String observedDigest = value.path("digest").asText();
    final String mediaType = value.path("mediaType").asText();
    if (kind == ImageIdentityKind.UNKNOWN || !digest(observedDigest) || mediaType.isBlank()) {
      blockers.add(blocker);
      return ImageIdentity.unknown();
    }
    return new ImageIdentity(kind, observedDigest, mediaType);
  }

  static Evaluation runPostgresSmokeAndQualify(
      final Path environmentManifestPath,
      final Path registryPath,
      final Path requirementsPath,
      final ObjectMapper mapper)
      throws IOException {
    return runPostgresSmokeAndQualifyWithInspector(
        environmentManifestPath,
        registryPath,
        requirementsPath,
        mapper,
        Qdr7CapacityEnvironmentAdmission::inspectExecutedIdentity);
  }

  static Evaluation runPostgresSmokeAndQualify(
      final Path environmentManifestPath,
      final Path registryPath,
      final Path requirementsPath,
      final ObjectMapper mapper,
      final ExecutedIdentityInspector executedIdentityInspector)
      throws IOException {
    return runPostgresSmokeAndQualifyWithInspector(
        environmentManifestPath,
        registryPath,
        requirementsPath,
        mapper,
        executedIdentityInspector);
  }

  private static Evaluation runPostgresSmokeAndQualifyWithInspector(
      final Path environmentManifestPath,
      final Path registryPath,
      final Path requirementsPath,
      final ObjectMapper mapper,
      final ExecutedIdentityInspector executedIdentityInspector)
      throws IOException {
    final ObjectNode manifest = (ObjectNode) mapper.readTree(environmentManifestPath.toFile());
    final ObjectNode registry = (ObjectNode) mapper.readTree(registryPath.toFile());
    final JsonNode requirements = mapper.readTree(requirementsPath.toFile());
    final String requiredImageReference = requirements.path("requiredImageReference").asText();
    final ObjectNode diagnostics = manifest.putObject("postgresImageIdentityDiagnostics");
    diagnostics.put("requiredImageReference", requiredImageReference);
    diagnostics.put("identityContractId", requirements.path("identityContractId").asText());
    diagnostics.put("identityContractVersion", requirements.path("identityContractVersion").asInt());
    diagnostics.set("localObservedIdentity", manifest.path("localObservedIdentity").deepCopy());
    final List<String> identityFindings = new ArrayList<>();
    final JsonNode existingIdentityFindings = manifest.path("identityBlockers");
    if (existingIdentityFindings.isArray()) {
      existingIdentityFindings.forEach(node -> identityFindings.add(node.asText()));
    } else {
      identityFindings.add("POSTGRES_IDENTITY_RESOLUTION_FAILED");
    }
    String postgresVersion = "";
    try (PostgreSQLContainer<?> smoke =
        new PostgreSQLContainer<>(
                DockerImageName.parse(requiredImageReference))
            .withDatabaseName("qdr12_environment_admission")
            .withUsername("qdr12_admission")
            .withPassword("qdr12-admission-test-only")) {
      smoke.start();
      final var containerInfo = smoke.getContainerInfo();
      final String executedImageReference = containerInfo.getImageId();
      final String containerConfiguredImage =
          containerInfo.getConfig() == null ? "" : containerInfo.getConfig().getImage();
      final ExecutedIdentityProof executedProof =
          executedIdentityInspector.inspect(
              smoke.getContainerId(), executedImageReference, manifest, requirements, mapper);
      final ObjectNode containerDiagnostics = diagnostics.putObject("container");
      containerDiagnostics.put("testcontainersImageReference", smoke.getDockerImageName());
      containerDiagnostics.put("containerConfiguredImage", nullToEmpty(containerConfiguredImage));
      containerDiagnostics.put("containerReportedImageIdentity", nullToEmpty(executedImageReference));
      containerDiagnostics.put("containerImmutableImageField", nullToEmpty(containerInfo.getImageId()));
      putExecutedProof(diagnostics.putObject("executedIdentityProof"), executedProof);
      putIdentity(manifest.putObject("executedObservedIdentity"), executedProof.identity());
      manifest.put("executedPlatformManifestDigest", executedProof.platformManifestDigest());
      manifest.put("executedConfigDigest", executedProof.configDigest());
      manifest.put("immutableBindingResult", executedProof.passed() ? "PASS" : "FAIL");
      if (!executedProof.passed()) {
        identityFindings.add(executedProof.blocker());
      }
      final ArrayNode identityBlockers = manifest.putArray("identityBlockers");
      identityFindings.stream().distinct().forEach(identityBlockers::add);
      manifest.put("postgresExecutedImageReference", executedImageReference);
      manifest.put(
          "postgresExpectedCanonicalImageId",
          requirements.path("expectedPlatformConfigDigest").asText());
      manifest.put(
          "postgresImageDigestVerified",
          "PASS".equals(manifest.path("repoDigestMembership").asText()));
      manifest.put("postgresExecutedImageId", executedProof.identity().digest());
      manifest.putObject("legacyPostgresIdentityFields")
          .put("status", "DEPRECATED_NOT_USED_FOR_V2_DECISION")
          .put("decisionContract", "POSTGRES_IMAGE_IDENTITY_CONTRACT_V2");
      try (var connection =
              DriverManager.getConnection(
                  smoke.getJdbcUrl(), smoke.getUsername(), smoke.getPassword());
          var statement = connection.createStatement();
          var result = statement.executeQuery("select current_setting('server_version_num')")) {
        if (!result.next()) {
          throw new IllegalStateException("postgres smoke returned no version row");
        }
        postgresVersion = result.getString(1);
      }
      manifest.put("testcontainersViable", true);
      manifest.put("postgresMajor", Integer.parseInt(postgresVersion) / 10000);
      manifest.put("testcontainersContainerRemoved", true);
    } catch (final RuntimeException | java.sql.SQLException | IOException failure) {
      manifest.put("testcontainersViable", false);
      manifest.put("postgresMajor", 0);
      manifest.put("postgresExecutedImageReference", "");
      manifest.put("postgresExecutedImageId", "");
      putIdentity(manifest.putObject("executedObservedIdentity"), ImageIdentity.unknown());
      manifest.put("executedPlatformManifestDigest", "");
      manifest.put("executedConfigDigest", "");
      manifest.put("immutableBindingResult", "FAIL");
      identityFindings.add("POSTGRES_EXECUTED_IDENTITY_UNKNOWN");
      final ArrayNode identityBlockers = manifest.putArray("identityBlockers");
      identityFindings.stream().distinct().forEach(identityBlockers::add);
      manifest.put("testcontainersFailure", failure.getClass().getSimpleName());
      diagnostics.put("smokeFailure", failure.getClass().getSimpleName());
    }
    final Evaluation evaluation = evaluate(manifest, requirements);
    manifest.put("status", evaluation.status());
    final ArrayNode blockers = manifest.putArray("blockers");
    evaluation.blockers().forEach(blockers::add);
    final ObjectNode admissionDiagnostics = diagnostics.putObject("admission");
    admissionDiagnostics.put("finalStatus", evaluation.status());
    admissionDiagnostics.put("blockerCount", evaluation.blockers().size());
    final ArrayNode diagnosticBlockers = admissionDiagnostics.putArray("blockers");
    evaluation.blockers().forEach(diagnosticBlockers::add);
    System.out.println("QDR12_IMAGE_IDENTITY_DIAGNOSTICS=" + mapper.writeValueAsString(diagnostics));
    manifest.put(
        "postgresVersionMajorEvidence", postgresVersion.isBlank() ? "NOT_CAPTURED" : "17.x");
    manifest.remove("environmentManifestHash");
    final String manifestHash = sha256(mapper.writeValueAsBytes(manifest));
    manifest.put("environmentManifestHash", manifestHash);
    Qdr7CapacityArtifactSupport.writeJson(environmentManifestPath, manifest, mapper);
    registry.put("environmentManifestHash", manifestHash);
    Qdr7CapacityArtifactSupport.writeJson(registryPath, registry, mapper);
    propagateEnvironmentHash(environmentManifestPath.getParent(), manifestHash, mapper);
    return evaluation;
  }

  static ExecutedIdentityProof inspectStartedContainerIdentity(
      final String containerId,
      final String reportedImageId,
      final JsonNode localIdentityEvidence,
      final JsonNode requirements,
      final ObjectMapper mapper)
      throws IOException {
    return inspectExecutedIdentity(
        containerId, reportedImageId, localIdentityEvidence, requirements, mapper);
  }

  private static ExecutedIdentityProof inspectExecutedIdentity(
      final String containerId,
      final String reportedImageId,
      final JsonNode manifest,
      final JsonNode requirements,
      final ObjectMapper mapper)
      throws IOException {
    final String expectedManifest = requirements.path("expectedPlatformManifestDigest").asText();
    final String expectedManifestMediaType =
        requirements.path("expectedPlatformManifestMediaType").asText();
    final String expectedConfig = requirements.path("expectedPlatformConfigDigest").asText();
    final JsonNode target = requirements.path("targetPlatform");
    try {
      final NativeResult descriptorResult =
          invokeNative(
              List.of(
                  "docker",
                  "container",
                  "inspect",
                  "--format",
                  "{{json .ImageManifestDescriptor}}",
                  containerId));
      if (descriptorResult.exitCode() == 0
          && !descriptorResult.output().isBlank()
          && !"null".equals(descriptorResult.output())
          && !"<no value>".equals(descriptorResult.output())) {
        final JsonNode descriptor = mapper.readTree(descriptorResult.output());
        final ImageIdentity identity =
            new ImageIdentity(
                ImageIdentityKind.PLATFORM_MANIFEST_DIGEST,
                descriptor.path("digest").asText(),
                descriptor.path("mediaType").asText());
        final JsonNode platform = descriptor.path("platform");
        final boolean passed =
            expectedManifest.equals(identity.digest())
                && expectedManifestMediaType.equals(identity.mediaType())
                && target.path("os").asText().equals(platform.path("os").asText())
                && target.path("architecture").asText().equals(platform.path("architecture").asText())
                && target.path("variant").asText().equals(platform.path("variant").asText());
        return new ExecutedIdentityProof(
            identity,
            identity.digest(),
            expectedConfig,
            "PATH_B_PLATFORM_MANIFEST_DESCRIPTOR",
            passed ? "" : "POSTGRES_EXECUTED_PLATFORM_MISMATCH");
      }
      final JsonNode localIdentity = manifest.path("localObservedIdentity");
      if (ImageIdentityKind.CONFIG_DIGEST.name().equals(localIdentity.path("kind").asText())
          && expectedConfig.equals(localIdentity.path("digest").asText())
          && expectedConfig.equals(reportedImageId)) {
        return new ExecutedIdentityProof(
            new ImageIdentity(
                ImageIdentityKind.CONFIG_DIGEST,
                reportedImageId,
                "application/vnd.oci.image.config.v1+json"),
            "",
            expectedConfig,
            "PATH_A_EXECUTED_CONFIG_DIGEST",
            "");
      }
      return ExecutedIdentityProof.failure("POSTGRES_EXECUTED_IDENTITY_UNKNOWN");
    } catch (final RuntimeException inspectionFailure) {
      return ExecutedIdentityProof.failure("POSTGRES_IDENTITY_RESOLUTION_FAILED");
    }
  }

  private static NativeResult invokeNative(final List<String> command) throws IOException {
    final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    final boolean finished;
    try {
      finished = process.waitFor(30, TimeUnit.SECONDS);
    } catch (final InterruptedException interrupted) {
      Thread.currentThread().interrupt();
      throw new IOException("Docker identity inspection interrupted", interrupted);
    }
    if (!finished) {
      process.destroyForcibly();
      return new NativeResult(-1, "");
    }
    return new NativeResult(
        process.exitValue(),
        new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim());
  }

  private static void putIdentity(final ObjectNode target, final ImageIdentity identity) {
    target.put("digest", identity.digest());
    target.put("kind", identity.kind().name());
    target.put("mediaType", identity.mediaType());
  }

  private static void putExecutedProof(
      final ObjectNode target, final ExecutedIdentityProof proof) {
    putIdentity(target.putObject("identity"), proof.identity());
    target.put("platformManifestDigest", proof.platformManifestDigest());
    target.put("configDigest", proof.configDigest());
    target.put("proofPath", proof.proofPath());
    target.put("result", proof.passed() ? "PASS" : "FAIL");
    target.put("blocker", proof.blocker());
  }

  private static String nullToEmpty(final String value) {
    return value == null ? "" : value;
  }

  private static void propagateEnvironmentHash(
      final Path evidenceRoot, final String manifestHash, final ObjectMapper mapper)
      throws IOException {
    try (var paths = Files.list(evidenceRoot)) {
      for (final Path path :
          paths
              .filter(Files::isRegularFile)
              .filter(candidate -> candidate.getFileName().toString().endsWith(".json"))
              .toList()) {
        final JsonNode value = mapper.readTree(path.toFile());
        if (value instanceof ObjectNode object && object.has("environmentManifestHash")) {
          object.put("environmentManifestHash", manifestHash);
          Qdr7CapacityArtifactSupport.writeJson(path, object, mapper);
        }
      }
    }
  }

  private static boolean containsRequiredText(
      final JsonNode root,
      final String field,
      final String value,
      final String blocker,
      final List<String> blockers) {
    final JsonNode array = root.path(field);
    if (!array.isArray() || array.isEmpty()) {
      blockers.add(blocker);
      return false;
    }
    boolean found = false;
    for (final JsonNode candidate : array) {
      if (!candidate.isTextual() || candidate.textValue().isBlank()) {
        blockers.add(blocker);
        return false;
      }
      if (candidate.textValue().equals(value)) {
        found = true;
      }
    }
    return found;
  }

  private static String requiredText(
      final JsonNode root,
      final String field,
      final String blocker,
      final List<String> blockers) {
    final JsonNode value = root.path(field);
    if (!value.isTextual() || value.textValue().isBlank()) {
      blockers.add(blocker);
      return "";
    }
    return value.textValue();
  }

  private static boolean requiredBoolean(
      final JsonNode root,
      final String field,
      final String blocker,
      final List<String> blockers) {
    final JsonNode value = root.path(field);
    if (!value.isBoolean()) {
      blockers.add(blocker);
      return false;
    }
    return value.booleanValue();
  }

  private static boolean requiredEmptyTextArray(
      final JsonNode root,
      final String field,
      final String blocker,
      final List<String> blockers) {
    final JsonNode value = root.path(field);
    if (!value.isArray()) {
      blockers.add(blocker);
      return false;
    }
    for (final JsonNode element : value) {
      if (!element.isTextual() || element.textValue().isBlank()) {
        blockers.add(blocker);
        return false;
      }
    }
    return value.isEmpty();
  }

  private static boolean sha(final String value) {
    return value != null && value.matches("^[a-f0-9]{40}$");
  }

  private static boolean sha256(final String value) {
    return value != null && value.matches("^[a-f0-9]{64}$");
  }

  private static boolean digest(final String value) {
    return value != null && value.matches("^sha256:[a-f0-9]{64}$");
  }

  private static long positiveIntegralRequirement(
      final JsonNode requirements,
      final String field,
      final String blocker,
      final List<String> blockers) {
    final JsonNode value = requirements.path(field);
    if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() <= 0L) {
      blockers.add(blocker);
      return Long.MAX_VALUE;
    }
    return value.asLong();
  }

  private static long positiveIntegral(
      final JsonNode root,
      final String field,
      final String blocker,
      final List<String> blockers) {
    final JsonNode value = root.path(field);
    if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() <= 0L) {
      blockers.add(blocker);
      return -1L;
    }
    return value.asLong();
  }

  private static long nonNegativeIntegral(
      final JsonNode root,
      final String field,
      final String blocker,
      final List<String> blockers) {
    final JsonNode value = root.path(field);
    if (!value.isIntegralNumber() || !value.canConvertToLong() || value.asLong() < 0L) {
      blockers.add(blocker);
      return -1L;
    }
    return value.asLong();
  }

  private static double finiteRequirement(
      final JsonNode requirements,
      final String field,
      final double minimum,
      final double maximum,
      final String blocker,
      final List<String> blockers) {
    return finiteNumber(requirements, field, minimum, maximum, blocker, blockers);
  }

  private static double finiteObservation(
      final JsonNode snapshot,
      final String field,
      final double minimum,
      final double maximum,
      final String blocker,
      final List<String> blockers) {
    return finiteNumber(snapshot, field, minimum, maximum, blocker, blockers);
  }

  private static double finiteNumber(
      final JsonNode root,
      final String field,
      final double minimum,
      final double maximum,
      final String blocker,
      final List<String> blockers) {
    final JsonNode value = root.path(field);
    final double observed = value.isNumber() ? value.asDouble() : Double.NaN;
    if (!Double.isFinite(observed) || observed < minimum || observed > maximum) {
      blockers.add(blocker);
      return Double.MAX_VALUE;
    }
    return observed;
  }

  private static void requirePositiveObservedAtLeast(
      final JsonNode snapshot,
      final String field,
      final long minimum,
      final String invalidBlocker,
      final String insufficientBlocker,
      final List<String> blockers) {
    final JsonNode observed = snapshot.path(field);
    if (!observed.isIntegralNumber()
        || !observed.canConvertToLong()
        || observed.asLong() <= 0L) {
      blockers.add(invalidBlocker);
      return;
    }
    require(observed.asLong() >= minimum, insufficientBlocker, blockers);
  }

  private static String sha256(final byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (final NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static void require(
      final boolean condition, final String blocker, final List<String> blockers) {
    if (!condition) {
      blockers.add(blocker);
    }
  }

  record Evaluation(String status, List<String> blockers) {}

  @FunctionalInterface
  interface ExecutedIdentityInspector {
    ExecutedIdentityProof inspect(
        String containerId,
        String reportedImageId,
        JsonNode manifest,
        JsonNode requirements,
        ObjectMapper mapper)
        throws IOException;
  }

  enum ImageIdentityKind {
    OCI_INDEX_DIGEST,
    PLATFORM_MANIFEST_DIGEST,
    CONFIG_DIGEST,
    REPO_DIGEST,
    UNKNOWN
  }

  record ImageIdentity(ImageIdentityKind kind, String digest, String mediaType) {
    private static ImageIdentity unknown() {
      return new ImageIdentity(ImageIdentityKind.UNKNOWN, "", "");
    }
  }

  record ExecutedIdentityProof(
      ImageIdentity identity,
      String platformManifestDigest,
      String configDigest,
      String proofPath,
      String blocker) {

    boolean passed() {
      return blocker == null || blocker.isBlank();
    }

    static ExecutedIdentityProof failure(final String blocker) {
      return new ExecutedIdentityProof(ImageIdentity.unknown(), "", "", "UNRESOLVED", blocker);
    }
  }

  record DescriptorResolution(String digest, String mediaType, String blocker) {
    static DescriptorResolution failure(final String blocker) {
      return new DescriptorResolution("", "", blocker);
    }

    boolean passed() {
      return blocker.isBlank();
    }
  }

  record ConfigResolution(String digest, String mediaType, String blocker) {
    static ConfigResolution failure(final String blocker) {
      return new ConfigResolution("", "", blocker);
    }

    boolean passed() {
      return blocker.isBlank();
    }
  }

  private record NativeResult(int exitCode, String output) {}
}
