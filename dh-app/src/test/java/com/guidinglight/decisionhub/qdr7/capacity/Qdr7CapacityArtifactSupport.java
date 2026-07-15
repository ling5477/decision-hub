package com.guidinglight.decisionhub.qdr7.capacity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/** Capacity evidence 的写入、manifest、schema 和 secret scan 支持。 */
final class Qdr7CapacityArtifactSupport {

  private static final List<String> COMMON_FIELDS =
      List.of(
          "schemaVersion",
          "runId",
          "commitSha",
          "scenario",
          "status",
          "startedAtUtc",
          "finishedAtUtc",
          "durationMs",
          "seed",
          "unitSystem",
          "missingValues");
  private static final List<String> VALID_STATUSES = List.of("PASS", "FAIL", "BLOCKED", "NOT_RUN");

  private Qdr7CapacityArtifactSupport() {}

  static void writeJson(final Path path, final Map<String, ?> value, final ObjectMapper mapper)
      throws IOException {
    Files.createDirectories(path.getParent());
    mapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
  }

  static void writeCsvHeader(final Path path, final List<String> additionalColumns)
      throws IOException {
    final List<String> columns = new ArrayList<>();
    columns.addAll(
        List.of("schemaVersion", "runId", "commitSha", "scenario", "timestampUtc", "elapsedMs"));
    columns.addAll(additionalColumns);
    Files.createDirectories(path.getParent());
    Files.writeString(
        path, String.join(",", columns) + System.lineSeparator(), StandardCharsets.UTF_8);
  }

  static void appendCsv(final Path path, final List<?> values) throws IOException {
    final String line =
        values.stream()
            .map(Qdr7CapacityArtifactSupport::csvValue)
            .reduce((a, b) -> a + "," + b)
            .orElse("");
    Files.writeString(
        path,
        line + System.lineSeparator(),
        StandardCharsets.UTF_8,
        StandardOpenOption.CREATE,
        StandardOpenOption.APPEND);
  }

  private static String csvValue(final Object value) {
    if (value == null) {
      return "";
    }
    final String text = String.valueOf(value);
    if (text.contains(",") || text.contains("\"") || text.contains("\n") || text.contains("\r")) {
      return "\"" + text.replace("\"", "\"\"") + "\"";
    }
    return text;
  }

  static ValidationResult validateJson(
      final Path path,
      final String runId,
      final String commitSha,
      final String criteriaVersion,
      final ObjectMapper mapper) {
    final List<String> findings = new ArrayList<>();
    try {
      final JsonNode root = mapper.readTree(path.toFile());
      for (final String field : COMMON_FIELDS) {
        if (!root.has(field)) {
          findings.add(path.getFileName() + ": missing " + field);
        }
      }
      if (!Qdr7CapacityContracts.SCHEMA_VERSION.equals(root.path("schemaVersion").asText())) {
        findings.add(path.getFileName() + ": schemaVersion mismatch");
      }
      if (!runId.equals(root.path("runId").asText())) {
        findings.add(path.getFileName() + ": runId mismatch");
      }
      if (!commitSha.equals(root.path("commitSha").asText())) {
        findings.add(path.getFileName() + ": commitSha mismatch");
      }
      if (!criteriaVersion.equals(root.path("criteriaVersion").asText())) {
        findings.add(path.getFileName() + ": criteriaVersion mismatch");
      }
      if (!VALID_STATUSES.contains(root.path("status").asText())) {
        findings.add(path.getFileName() + ": invalid status");
      }
      if (root.path("seed").asInt(Integer.MIN_VALUE) != Qdr7CapacityContracts.REQUIRED_SEED) {
        findings.add(path.getFileName() + ": seed mismatch");
      }
      if (!root.path("missingValues").isArray()) {
        findings.add(path.getFileName() + ": missingValues must be an array");
      }
    } catch (final Exception invalidJson) {
      findings.add(path.getFileName() + ": JSON_PARSE_FAILED");
    }
    return new ValidationResult(findings.isEmpty(), findings);
  }

  static ValidationResult validateCsvHeader(final Path path, final List<String> requiredHeader) {
    final List<String> findings = new ArrayList<>();
    try {
      final String header =
          Files.readAllLines(path, StandardCharsets.UTF_8).stream().findFirst().orElse("");
      final List<String> actual = List.of(header.split(",", -1));
      if (actual.size() < requiredHeader.size()
          || !actual.subList(0, requiredHeader.size()).equals(requiredHeader)) {
        findings.add(path.getFileName() + ": CSV_HEADER_MISMATCH");
      }
    } catch (final IOException unreadable) {
      findings.add(path.getFileName() + ": CSV_UNREADABLE");
    }
    return new ValidationResult(findings.isEmpty(), findings);
  }

  static ValidationResult validateMandatoryArtifacts(
      final Path evidenceRoot, final List<String> mandatoryArtifacts) {
    final List<String> findings = new ArrayList<>();
    for (final String name : mandatoryArtifacts) {
      final Path target = evidenceRoot.resolve(name).normalize();
      if (!target.startsWith(evidenceRoot.normalize())) {
        findings.add("MANDATORY_PATH_INVALID:" + name);
      } else if (!Files.isRegularFile(target)) {
        findings.add("MANDATORY_ARTIFACT_MISSING:" + name);
      }
    }
    return new ValidationResult(findings.isEmpty(), findings);
  }

  static Map<String, String> writeManifest(final Path evidenceRoot) throws IOException {
    final Map<String, String> entries = new LinkedHashMap<>();
    try (Stream<Path> paths = Files.walk(evidenceRoot)) {
      paths
          .filter(Files::isRegularFile)
          .filter(path -> !path.getFileName().toString().equals("sha256-manifest.txt"))
          .sorted(Comparator.comparing(path -> toRelativePath(evidenceRoot, path)))
          .forEach(
              path -> {
                try {
                  entries.put(
                      toRelativePath(evidenceRoot, path), Qdr7CapacityContracts.sha256(path));
                } catch (final IOException failure) {
                  throw new ManifestFailure(failure);
                }
              });
    } catch (final ManifestFailure failure) {
      throw (IOException) failure.getCause();
    }
    final StringBuilder content = new StringBuilder();
    entries.forEach((path, hash) -> content.append(hash).append("  ").append(path).append('\n'));
    Files.writeString(
        evidenceRoot.resolve("sha256-manifest.txt"), content.toString(), StandardCharsets.UTF_8);
    return entries;
  }

  static ValidationResult validateManifest(final Path evidenceRoot) {
    final List<String> findings = new ArrayList<>();
    final Path manifest = evidenceRoot.resolve("sha256-manifest.txt");
    try {
      final List<String> lines = Files.readAllLines(manifest, StandardCharsets.UTF_8);
      final List<String> pathsInManifest =
          lines.stream()
              .map(line -> line.split("  ", 2))
              .filter(parts -> parts.length == 2)
              .map(parts -> parts[1])
              .toList();
      final List<String> sortedPaths = new ArrayList<>(pathsInManifest);
      sortedPaths.sort(Comparator.naturalOrder());
      if (!pathsInManifest.equals(sortedPaths)) {
        findings.add("MANIFEST_ORDER_MISMATCH");
      }
      for (final String line : lines) {
        final String[] parts = line.split("  ", 2);
        if (parts.length != 2 || !parts[0].matches("^[a-f0-9]{64}$")) {
          findings.add("MANIFEST_LINE_INVALID");
          continue;
        }
        final Path target = evidenceRoot.resolve(parts[1]).normalize();
        if (!target.startsWith(evidenceRoot.normalize()) || target.equals(manifest)) {
          findings.add("MANIFEST_PATH_INVALID");
        } else if (!Files.isRegularFile(target)) {
          findings.add("MANIFEST_FILE_MISSING:" + parts[1]);
        } else if (!parts[0].equals(Qdr7CapacityContracts.sha256(target))) {
          findings.add("MANIFEST_MISMATCH:" + parts[1]);
        }
      }
    } catch (final IOException failure) {
      findings.add("MANIFEST_UNREADABLE");
    }
    return new ValidationResult(findings.isEmpty(), findings);
  }

  static SecretScanResult scanSecrets(
      final Path evidenceRoot,
      final List<SecretPattern> patterns,
      final List<String> exactForbiddenValues)
      throws IOException {
    final List<SecretFinding> findings = new ArrayList<>();
    int scannedFiles = 0;
    try (Stream<Path> paths = Files.walk(evidenceRoot)) {
      final List<Path> files =
          paths
              .filter(Files::isRegularFile)
              .filter(path -> !path.getFileName().toString().equals("secret-scan.json"))
              .toList();
      scannedFiles = files.size();
      for (final Path file : files) {
        if (!isTextArtifact(file)) {
          continue;
        }
        final List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int index = 0; index < lines.size(); index++) {
          final String line = lines.get(index);
          for (final SecretPattern pattern : patterns) {
            if (pattern.pattern().matcher(line).find()) {
              findings.add(
                  new SecretFinding(pattern.id(), toRelativePath(evidenceRoot, file), index + 1));
            }
          }
          for (final String exact : exactForbiddenValues) {
            if (exact != null && !exact.isBlank() && line.contains(exact)) {
              findings.add(
                  new SecretFinding(
                      "temporary-secret-exact", toRelativePath(evidenceRoot, file), index + 1));
            }
          }
        }
      }
    }
    return new SecretScanResult(scannedFiles, List.copyOf(findings));
  }

  static List<SecretPattern> loadSecretPatterns(final Path projectRoot, final ObjectMapper mapper)
      throws IOException {
    final JsonNode root =
        mapper.readTree(
            projectRoot
                .resolve("config/qdr7-capacity/qdr7-capacity-secret-patterns.json")
                .toFile());
    final List<SecretPattern> result = new ArrayList<>();
    for (final JsonNode pattern : root.path("patterns")) {
      result.add(
          new SecretPattern(
              pattern.path("id").asText(), Pattern.compile(pattern.path("regex").asText())));
    }
    return List.copyOf(result);
  }

  static Map<String, Object> secretScanArtifact(
      final Qdr7CapacityContracts.RunContext context,
      final SecretScanResult result,
      final Instant started,
      final Instant finished) {
    final Map<String, Object> artifact =
        Qdr7CapacityContracts.commonArtifact(
            context,
            "secret-scan",
            result.findings().isEmpty()
                ? Qdr7CapacityContracts.HarnessStatus.PASS
                : Qdr7CapacityContracts.HarnessStatus.FAIL,
            started,
            finished);
    artifact.put("patternsVersion", "qdr7-capacity-secret-patterns-1");
    artifact.put("scannedFiles", result.scannedFiles());
    artifact.put("findingCount", result.findings().size());
    artifact.put("findings", result.findings());
    return artifact;
  }

  private static boolean isTextArtifact(final Path path) {
    final String name = path.getFileName().toString().toLowerCase(java.util.Locale.ROOT);
    return name.endsWith(".json")
        || name.endsWith(".csv")
        || name.endsWith(".txt")
        || name.endsWith(".log");
  }

  private static String toRelativePath(final Path root, final Path path) {
    return root.relativize(path).toString().replace('\\', '/');
  }

  record ValidationResult(boolean valid, List<String> findings) {}

  record SecretPattern(String id, Pattern pattern) {}

  record SecretFinding(String patternId, String path, int line) {}

  record SecretScanResult(int scannedFiles, List<SecretFinding> findings) {}

  private static final class ManifestFailure extends RuntimeException {
    private static final long serialVersionUID = 1L;

    ManifestFailure(final IOException cause) {
      super(cause);
    }
  }
}
