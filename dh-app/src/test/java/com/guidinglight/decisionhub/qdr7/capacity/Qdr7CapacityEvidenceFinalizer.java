package com.guidinglight.decisionhub.qdr7.capacity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Stage-QDR-12 capacity evidence packet 的 fail-closed 完整性裁决器。
 *
 * <p>完整性或 binding 漂移一律为 {@code INVALID}；环境/执行能力不足为 {@code BLOCKED}；真实场景或阈值失败为 {@code
 * FAIL}。只有完整且全部通过的 packet 才能得到 {@code PASS_WITHIN_FROZEN_PROFILE}。
 */
final class Qdr7CapacityEvidenceFinalizer {

  private static final List<String> BINDING_FIELDS =
      List.of(
          "attemptId",
          "candidateSha",
          "candidateTree",
          "profileId",
          "profileVersion",
          "scenarioSetHash",
          "thresholdSetHash",
          "environmentManifestHash",
          "harnessVersion",
          "harnessHash");

  private Qdr7CapacityEvidenceFinalizer() {}

  static Result finalizePacket(final Packet packet, final ObjectMapper mapper) {
    final List<String> invalid = new ArrayList<>();
    final List<String> blocked = new ArrayList<>();
    final List<String> failed = new ArrayList<>();
    validateBinding(packet, mapper, invalid);
    validateInventory(packet, invalid);
    validateScenarios(packet, invalid, blocked, failed);
    validateThresholds(packet, mapper, invalid, blocked, failed);
    if (!invalid.isEmpty()) {
      return new Result("INVALID", List.copyOf(invalid));
    }
    if (!"QUALIFIED".equals(packet.environmentManifest().path("status").asText())) {
      blocked.add("ENVIRONMENT_NOT_QUALIFIED");
    }
    if (!blocked.isEmpty()) {
      return new Result("BLOCKED", List.copyOf(blocked));
    }
    if (!failed.isEmpty()) {
      return new Result("FAIL", List.copyOf(failed));
    }
    return new Result("PASS_WITHIN_FROZEN_PROFILE", List.of());
  }

  private static void validateBinding(
      final Packet packet, final ObjectMapper mapper, final List<String> invalid) {
    final JsonNode execution = packet.executionManifest();
    require(
        execution.path("attemptId").asText().matches("^[A-Za-z0-9._:-]+$"),
        "ATTEMPT_ID_INVALID",
        invalid);
    require(
        execution.path("candidateSha").asText().matches("^[a-f0-9]{40}$"),
        "CANDIDATE_SHA_INVALID",
        invalid);
    require(
        execution.path("candidateTree").asText().matches("^[a-f0-9]{40}$"),
        "CANDIDATE_TREE_INVALID",
        invalid);
    for (final String field :
        List.of("scenarioSetHash", "thresholdSetHash", "environmentManifestHash", "harnessHash")) {
      require(
          execution.path(field).asText().matches("^[a-f0-9]{64}$"),
          "BINDING_HASH_INVALID:" + field,
          invalid);
    }
    for (final JsonNode artifact :
        List.of(
            packet.environmentManifest(),
            packet.scenarioLedger(),
            packet.thresholdResults(),
            packet.artifactInventory())) {
      for (final String field : BINDING_FIELDS) {
        require(
            execution.path(field).asText().equals(artifact.path(field).asText()),
            "BINDING_MISMATCH:" + field,
            invalid);
      }
    }
    final ObjectNode environment = packet.environmentManifest().deepCopy();
    environment.remove("environmentManifestHash");
    try {
      require(
          execution
              .path("environmentManifestHash")
              .asText()
              .equals(sha256(mapper.writeValueAsBytes(environment))),
          "ENVIRONMENT_MANIFEST_HASH_MISMATCH",
          invalid);
    } catch (final IOException failure) {
      invalid.add("ENVIRONMENT_MANIFEST_HASH_UNREADABLE");
    }
  }

  private static void validateInventory(final Packet packet, final List<String> invalid) {
    final Map<String, String> declared = new LinkedHashMap<>();
    for (final JsonNode row : packet.artifactInventory().path("artifacts")) {
      final String path = row.path("path").asText();
      if (path.isBlank() || declared.put(path, row.path("sha256").asText()) != null) {
        invalid.add("DUPLICATE_ARTIFACT:" + path);
      }
    }
    for (final String mandatory : packet.mandatoryArtifacts()) {
      if (!declared.containsKey(mandatory) || !packet.artifactBytes().containsKey(mandatory)) {
        invalid.add("MANDATORY_ARTIFACT_MISSING:" + mandatory);
        continue;
      }
      if (!sha256(packet.artifactBytes().get(mandatory)).equals(declared.get(mandatory))) {
        invalid.add("ARTIFACT_HASH_MISMATCH:" + mandatory);
      }
    }
  }

  private static void validateScenarios(
      final Packet packet,
      final List<String> invalid,
      final List<String> blocked,
      final List<String> failed) {
    final Map<String, JsonNode> actual = new HashMap<>();
    for (final JsonNode row : packet.scenarioLedger().path("scenarios")) {
      final String id = row.path("scenarioId").asText();
      if (id.isBlank() || actual.put(id, row) != null) {
        invalid.add("DUPLICATE_SCENARIO:" + id);
      }
    }
    for (final String expected : packet.mandatoryScenarios()) {
      final JsonNode row = actual.get(expected);
      if (row == null) {
        invalid.add("MANDATORY_SCENARIO_MISSING:" + expected);
        continue;
      }
      final String verdict = row.path("verdict").asText();
      if ("FAIL".equals(verdict)) {
        failed.add("SCENARIO_FAILED:" + expected);
      } else if (!"PASS".equals(verdict)) {
        blocked.add("SCENARIO_NOT_EXECUTABLE:" + expected);
      }
    }
    for (final String actualId : actual.keySet()) {
      if (!packet.mandatoryScenarios().contains(actualId)) {
        invalid.add("ORPHAN_SCENARIO:" + actualId);
      }
    }
  }

  private static void validateThresholds(
      final Packet packet,
      final ObjectMapper mapper,
      final List<String> invalid,
      final List<String> blocked,
      final List<String> failed) {
    final Map<String, Integer> actualExecutions = new HashMap<>();
    final Set<String> criterionIds = new HashSet<>();
    int comparisonCount = 0;
    for (final JsonNode row : packet.thresholdResults().path("comparisons")) {
      comparisonCount++;
      final String path = row.path("thresholdPath").asText();
      final String criterionId = row.path("criterionId").asText();
      final ThresholdExpectation expectation = packet.thresholdExpectations().get(path);
      if (path.isBlank() || expectation == null) {
        invalid.add("ORPHAN_THRESHOLD:" + path);
      }
      if (criterionId.isBlank() || !criterionIds.add(criterionId)) {
        invalid.add("DUPLICATE_THRESHOLD_RESULT:" + criterionId);
      }
      actualExecutions.merge(path, 1, Integer::sum);
      if (expectation == null || !validateThresholdSemantics(row, path, expectation, invalid)) {
        continue;
      }
      final BigDecimal sourceObserved;
      try {
        sourceObserved = sourceObserved(packet, row, expectation, mapper);
      } catch (final RuntimeException | IOException unreadable) {
        invalid.add("THRESHOLD_SOURCE_OBSERVED_UNREADABLE:" + path);
        continue;
      }
      if (sourceObserved.compareTo(row.path("observed").decimalValue()) != 0) {
        invalid.add("THRESHOLD_SOURCE_OBSERVED_MISMATCH:" + path);
        continue;
      }
      final String independentlyEvaluated =
          compare(sourceObserved, expectation.operator(), expectation.threshold())
              ? "PASS"
              : "FAIL";
      if (!independentlyEvaluated.equals(row.path("status").asText())) {
        invalid.add("THRESHOLD_STATUS_SEMANTICS_MISMATCH:" + path);
      } else if ("FAIL".equals(independentlyEvaluated)) {
        failed.add("THRESHOLD_FAILED:" + path);
      }
    }
    for (final Map.Entry<String, ThresholdExpectation> expected :
        packet.thresholdExpectations().entrySet()) {
      if (expected.getValue().expectedExecutions()
          != actualExecutions.getOrDefault(expected.getKey(), 0)) {
        invalid.add("THRESHOLD_EXECUTION_COUNT_MISMATCH:" + expected.getKey());
      }
    }
    final int expectedTotal =
        packet.thresholdExpectations().values().stream()
            .mapToInt(ThresholdExpectation::expectedExecutions)
            .sum();
    if (comparisonCount != expectedTotal) {
      invalid.add("THRESHOLD_TOTAL_MISMATCH");
    }
  }

  private static boolean validateThresholdSemantics(
      final JsonNode row,
      final String path,
      final ThresholdExpectation expectation,
      final List<String> invalid) {
    boolean valid = true;
    if (!row.path("observed").isNumber()) {
      invalid.add("THRESHOLD_OBSERVED_INVALID:" + path);
      valid = false;
    }
    if (!expectation.operator().equals(row.path("operator").asText())) {
      invalid.add("THRESHOLD_OPERATOR_MISMATCH:" + path);
      valid = false;
    }
    if (!row.path("threshold").isNumber()
        || row.path("threshold").decimalValue().compareTo(expectation.threshold()) != 0) {
      invalid.add("THRESHOLD_VALUE_MISMATCH:" + path);
      valid = false;
    }
    if (!expectation.unit().equals(row.path("unit").asText())) {
      invalid.add("THRESHOLD_UNIT_MISMATCH:" + path);
      valid = false;
    }
    if (!expectation.sourceArtifact().equals(row.path("sourceArtifact").asText())) {
      invalid.add("THRESHOLD_SOURCE_ARTIFACT_MISMATCH:" + path);
      valid = false;
    }
    if (!expectation.artifactField().startsWith(expectation.sourceArtifact() + "#")) {
      invalid.add("THRESHOLD_ARTIFACT_FIELD_INVALID:" + path);
      valid = false;
    }
    return valid;
  }

  private static BigDecimal sourceObserved(
      final Packet packet,
      final JsonNode row,
      final ThresholdExpectation expectation,
      final ObjectMapper mapper)
      throws IOException {
    final String source = expectation.sourceArtifact();
    if (!packet.mandatoryArtifacts().contains(source)) {
      throw new IllegalStateException("threshold source artifact is not sealed");
    }
    final byte[] bytes = packet.artifactBytes().get(source);
    if (bytes == null) {
      throw new IllegalStateException("threshold source artifact is missing");
    }
    return switch (source) {
      case "rate-summary.json" -> rateObserved(mapper.readTree(bytes), row, expectation);
      case "cleanup-summary.json" -> cleanupObserved(mapper.readTree(bytes), row, expectation);
      case "recovery-timeline.csv" -> recoveryObserved(bytes, row, expectation);
      case "postgres-hikari-series.csv" -> contentionObserved(bytes, row, expectation);
      case "resource-summary.json" -> directJsonObserved(mapper.readTree(bytes), expectation);
      default -> directJsonObserved(mapper.readTree(bytes), expectation);
    };
  }

  private static BigDecimal rateObserved(
      final JsonNode source, final JsonNode thresholdRow, final ThresholdExpectation expectation) {
    final Matcher matcher =
        Pattern.compile("^rate\\.c(\\d+)\\.r(\\d+)\\.(throughput|p50|p95|p99|max)$")
            .matcher(thresholdRow.path("criterionId").asText());
    if (!matcher.matches()) {
      throw new IllegalStateException("rate criterion id is invalid");
    }
    final int concurrency = Integer.parseInt(matcher.group(1));
    final int round = Integer.parseInt(matcher.group(2));
    final String field =
        switch (matcher.group(3)) {
          case "throughput" -> "throughput";
          case "p50" -> "p50Ms";
          case "p95" -> "p95Ms";
          case "p99" -> "p99Ms";
          case "max" -> "maxMs";
          default -> throw new IllegalStateException("rate field is invalid");
        };
    requireArtifactField(expectation, "rate-summary.json#rounds[concurrency,round]." + field);
    JsonNode match = null;
    for (final JsonNode candidate : source.path("rounds")) {
      if (candidate.path("concurrency").asInt(-1) == concurrency
          && candidate.path("round").asInt(-1) == round) {
        if (match != null) {
          throw new IllegalStateException("duplicate rate source row");
        }
        match = candidate;
      }
    }
    return numeric(match == null ? null : match.get(field));
  }

  private static BigDecimal cleanupObserved(
      final JsonNode source, final JsonNode thresholdRow, final ThresholdExpectation expectation) {
    final Matcher matcher =
        Pattern.compile("^cleanup\\.(\\d+)\\.duration$")
            .matcher(thresholdRow.path("criterionId").asText());
    if (!matcher.matches()) {
      throw new IllegalStateException("cleanup criterion id is invalid");
    }
    requireArtifactField(expectation, "cleanup-summary.json#scales[scale].durationMs");
    final int scale = Integer.parseInt(matcher.group(1));
    JsonNode match = null;
    for (final JsonNode candidate : source.path("scales")) {
      if (candidate.path("scale").asInt(-1) == scale) {
        if (match != null) {
          throw new IllegalStateException("duplicate cleanup source row");
        }
        match = candidate;
      }
    }
    return numeric(match == null ? null : match.get("durationMs"));
  }

  private static BigDecimal recoveryObserved(
      final byte[] source, final JsonNode thresholdRow, final ThresholdExpectation expectation) {
    final Matcher matcher =
        Pattern.compile("^recovery\\.(database|hikari|request|samplingGap)\\.r(\\d+)$")
            .matcher(thresholdRow.path("criterionId").asText());
    if (!matcher.matches()) {
      throw new IllegalStateException("recovery criterion id is invalid");
    }
    final String field =
        switch (matcher.group(1)) {
          case "database" -> "databaseReadyMs";
          case "hikari" -> "hikariReadyMs";
          case "request" -> "requestReadyMs";
          case "samplingGap" -> "samplingGapMs";
          default -> throw new IllegalStateException("recovery field is invalid");
        };
    requireArtifactField(expectation, "recovery-timeline.csv#rows[round]." + field);
    final String round = matcher.group(2);
    final List<Map<String, String>> rows = csvRows(source);
    final List<Map<String, String>> matches =
        rows.stream().filter(candidate -> round.equals(candidate.get("round"))).toList();
    if (matches.size() != 1) {
      throw new IllegalStateException("recovery source row cardinality invalid");
    }
    return numeric(matches.getFirst().get(field));
  }

  private static BigDecimal contentionObserved(
      final byte[] source, final JsonNode thresholdRow, final ThresholdExpectation expectation) {
    final Matcher matcher =
        Pattern.compile("^contention\\.(hikariPending|acquire|postgresWaiting|lockWaiting)$")
            .matcher(thresholdRow.path("criterionId").asText());
    if (!matcher.matches()) {
      throw new IllegalStateException("contention criterion id is invalid");
    }
    final String field =
        switch (matcher.group(1)) {
          case "hikariPending" -> "hikariPending";
          case "acquire" -> "acquireMs";
          case "postgresWaiting" -> "postgresWaiting";
          case "lockWaiting" -> "postgresLockWaiting";
          default -> throw new IllegalStateException("contention field is invalid");
        };
    requireArtifactField(expectation, "postgres-hikari-series.csv#max(" + field + ")");
    return csvRows(source).stream()
        .map(candidate -> numeric(candidate.get(field)))
        .max(BigDecimal::compareTo)
        .orElseThrow(() -> new IllegalStateException("contention source is empty"));
  }

  private static BigDecimal directJsonObserved(
      final JsonNode source, final ThresholdExpectation expectation) {
    final String prefix = expectation.sourceArtifact() + "#";
    if (!expectation.artifactField().startsWith(prefix)) {
      throw new IllegalStateException("direct artifact field is invalid");
    }
    final String field = expectation.artifactField().substring(prefix.length());
    if (field.isBlank() || field.contains(".") || field.contains("[") || field.contains("(")) {
      throw new IllegalStateException("direct artifact field is unsupported");
    }
    return numeric(source.get(field));
  }

  private static List<Map<String, String>> csvRows(final byte[] source) {
    final List<String> lines =
        new String(source, StandardCharsets.UTF_8).lines().filter(line -> !line.isBlank()).toList();
    if (lines.size() < 2) {
      throw new IllegalStateException("CSV source has no rows");
    }
    final String[] headers = lines.getFirst().split(",", -1);
    final List<Map<String, String>> rows = new ArrayList<>();
    for (int line = 1; line < lines.size(); line++) {
      final String[] values = lines.get(line).split(",", -1);
      if (values.length != headers.length) {
        throw new IllegalStateException("CSV source row shape mismatch");
      }
      final Map<String, String> row = new HashMap<>();
      for (int index = 0; index < headers.length; index++) {
        row.put(headers[index], values[index]);
      }
      rows.add(row);
    }
    return rows;
  }

  private static void requireArtifactField(
      final ThresholdExpectation expectation, final String expected) {
    if (!expected.equals(expectation.artifactField())) {
      throw new IllegalStateException("threshold artifact field drifted");
    }
  }

  private static BigDecimal numeric(final JsonNode value) {
    if (value == null || !value.isNumber()) {
      throw new IllegalStateException("JSON source value is not numeric");
    }
    return value.decimalValue();
  }

  private static BigDecimal numeric(final String value) {
    if (value == null || value.isBlank()) {
      throw new IllegalStateException("CSV source value is not numeric");
    }
    try {
      return new BigDecimal(value);
    } catch (final NumberFormatException invalid) {
      throw new IllegalStateException("CSV source value is not numeric", invalid);
    }
  }

  private static boolean compare(
      final BigDecimal observed, final String operator, final BigDecimal threshold) {
    final int comparison = observed.compareTo(threshold);
    return switch (operator) {
      case ">=" -> comparison >= 0;
      case "<=" -> comparison <= 0;
      default -> false;
    };
  }

  private static String sha256(final byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (final NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 unavailable", impossible);
    }
  }

  private static void require(
      final boolean condition, final String finding, final List<String> findings) {
    if (!condition) {
      findings.add(finding);
    }
  }

  record Packet(
      JsonNode environmentManifest,
      JsonNode executionManifest,
      JsonNode scenarioLedger,
      JsonNode thresholdResults,
      JsonNode artifactInventory,
      List<String> mandatoryScenarios,
      Map<String, ThresholdExpectation> thresholdExpectations,
      Set<String> mandatoryArtifacts,
      Map<String, byte[]> artifactBytes) {}

  record ThresholdExpectation(
      int expectedExecutions,
      String operator,
      BigDecimal threshold,
      String unit,
      String sourceArtifact,
      String artifactField) {}

  record Result(String verdict, List<String> findings) {}
}
