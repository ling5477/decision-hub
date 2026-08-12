package com.guidinglight.decisionhub.qdr7.capacity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Function;
import org.testcontainers.DockerClientFactory;
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
    require(
        requiredBoolean(
            snapshot,
            "postgresImageAvailable",
            "POSTGRES_IMAGE_AVAILABILITY_OBSERVATION_INVALID",
            blockers),
        "POSTGRES_IMAGE_UNAVAILABLE",
        blockers);
    final String postgresImageId =
        requiredText(
            snapshot, "postgresImageId", "POSTGRES_IMAGE_ID_OBSERVATION_INVALID", blockers);
    final String expectedCanonicalImageId =
        requiredText(
            snapshot,
            "postgresExpectedCanonicalImageId",
            "POSTGRES_EXPECTED_IMAGE_OBSERVATION_INVALID",
            blockers);
    require(
        !canonicalImageId(postgresImageId).isBlank(),
        "POSTGRES_IMAGE_IDENTITY_MISSING",
        blockers);
    require(
        canonicalImageIdsMatch(postgresImageId, expectedCanonicalImageId),
        "POSTGRES_EXPECTED_IMAGE_MISMATCH",
        blockers);
    final String requiredPostgresImage =
        requiredText(
            requirements, "postgresImage", "POSTGRES_IMAGE_REQUIREMENT_INVALID", blockers);
    final String observedPostgresImage =
        requiredText(
            snapshot,
            "postgresImageReference",
            "POSTGRES_IMAGE_REFERENCE_OBSERVATION_INVALID",
            blockers);
    require(
        requiredPostgresImage.matches("^postgres@sha256:[a-f0-9]{64}$")
            && requiredPostgresImage.equals(observedPostgresImage),
        "POSTGRES_IMAGE_REFERENCE_MISMATCH",
        blockers);
    require(
        requiredBoolean(
            snapshot,
            "postgresImageDigestVerified",
            "POSTGRES_IMAGE_DIGEST_OBSERVATION_INVALID",
            blockers),
        "POSTGRES_IMAGE_DIGEST_MISMATCH",
        blockers);
    final String executedImageId =
        requiredText(
            snapshot,
            "postgresExecutedImageId",
            "POSTGRES_EXECUTED_IMAGE_OBSERVATION_INVALID",
            blockers);
    require(
        canonicalImageIdsMatch(expectedCanonicalImageId, executedImageId),
        "POSTGRES_EXECUTED_IMAGE_MISMATCH",
        blockers);
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

  static Evaluation runPostgresSmokeAndQualify(
      final Path environmentManifestPath,
      final Path registryPath,
      final Path requirementsPath,
      final ObjectMapper mapper)
      throws IOException {
    return runPostgresSmokeAndQualify(
        environmentManifestPath,
        registryPath,
        requirementsPath,
        mapper,
        Qdr7CapacityEnvironmentAdmission::inspectCanonicalImageId);
  }

  static Evaluation runPostgresSmokeAndQualify(
      final Path environmentManifestPath,
      final Path registryPath,
      final Path requirementsPath,
      final ObjectMapper mapper,
      final Function<String, String> canonicalImageResolver)
      throws IOException {
    final ObjectNode manifest = (ObjectNode) mapper.readTree(environmentManifestPath.toFile());
    final ObjectNode registry = (ObjectNode) mapper.readTree(registryPath.toFile());
    final JsonNode requirements = mapper.readTree(requirementsPath.toFile());
    String postgresVersion = "";
    try (PostgreSQLContainer<?> smoke =
        new PostgreSQLContainer<>(
                DockerImageName.parse(requirements.path("postgresImage").asText()))
            .withDatabaseName("qdr12_environment_admission")
            .withUsername("qdr12_admission")
            .withPassword("qdr12-admission-test-only")) {
      smoke.start();
      final String executedImageReference = smoke.getContainerInfo().getImageId();
      manifest.put("postgresExecutedImageReference", executedImageReference);
      manifest.put(
          "postgresExpectedCanonicalImageId",
          canonicalImageResolver.apply(requirements.path("postgresImage").asText()));
      manifest.put("postgresExecutedImageId", canonicalImageResolver.apply(executedImageReference));
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
    } catch (final RuntimeException | java.sql.SQLException failure) {
      manifest.put("testcontainersViable", false);
      manifest.put("postgresMajor", 0);
      manifest.put("postgresExpectedCanonicalImageId", "");
      manifest.put("postgresExecutedImageReference", "");
      manifest.put("postgresExecutedImageId", "");
      manifest.put("testcontainersFailure", failure.getClass().getSimpleName());
    }
    final Evaluation evaluation = evaluate(manifest, requirements);
    manifest.put("status", evaluation.status());
    final ArrayNode blockers = manifest.putArray("blockers");
    evaluation.blockers().forEach(blockers::add);
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

  /** Resolve a Docker reference to the daemon's immutable image config digest. */
  static String inspectCanonicalImageId(final String imageReference) {
    return resolveCanonicalImageId(
        imageReference,
        reference ->
            DockerClientFactory.instance()
                .client()
                .inspectImageCmd(reference)
                .exec()
                .getId());
  }

  static String resolveCanonicalImageId(
      final String imageReference, final Function<String, String> authoritativeInspector) {
    if (imageReference == null || imageReference.isBlank() || authoritativeInspector == null) {
      return "";
    }
    try {
      return canonicalImageId(authoritativeInspector.apply(imageReference.trim()));
    } catch (final RuntimeException inspectionFailure) {
      return "";
    }
  }

  static String canonicalImageId(final String imageId) {
    if (imageId == null) {
      return "";
    }
    final String trimmed = imageId.trim();
    final String hex = trimmed.startsWith("sha256:") ? trimmed.substring(7) : trimmed;
    return hex.matches("^[a-f0-9]{64}$") ? "sha256:" + hex : "";
  }

  static boolean canonicalImageIdsMatch(final String expected, final String executed) {
    final String canonicalExpected = canonicalImageId(expected);
    final String canonicalExecuted = canonicalImageId(executed);
    return !canonicalExpected.isBlank() && canonicalExpected.equals(canonicalExecuted);
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
}
