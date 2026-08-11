package com.guidinglight.decisionhub.qdr7.capacity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 将 host sampler 的 raw CSV 归一化为冻结 full-regression 资源指标。 */
final class Qdr7CapacityResourceEvidence {

  private static final String BASELINE_PHASE = "HARNESS_BASELINE";
  private static final String REGRESSION_PHASE = "FULL_REGRESSION";
  private static final String POST_REGRESSION_PHASE = "POST_REGRESSION";
  private static final long MAX_SAMPLE_GAP_MS = 5_000L;
  private static final long DURATION_TOLERANCE_MS = 5_000L;

  private Qdr7CapacityResourceEvidence() {}

  static Snapshot read(final Path evidenceRoot, final long durationMs, final Binding binding)
      throws IOException {
    if (durationMs < 0L) {
      throw new IllegalArgumentException("duration must be non-negative");
    }
    final List<Row> jvm =
        rows(evidenceRoot.resolve("jvm-series.csv"), "jvm-resource-sampling", binding);
    final List<Row> docker =
        rows(evidenceRoot.resolve("docker-series.csv"), "docker-host-resource-sampling", binding);
    requireCompleteJvmSampling(jvm);
    requireCompleteDockerSampling(docker);
    requireRegressionCoverage(jvm, durationMs, "jvm");
    requireRegressionCoverage(docker, durationMs, "docker");
    requireRoleRegressionCoverage(jvm, "MAVEN");
    requireRoleRegressionCoverage(jvm, "SUREFIRE");
    final long baselineMaven = maximum(jvm, BASELINE_PHASE, "MAVEN", "workingSetBytes", false);
    final long peakMaven = maximum(jvm, REGRESSION_PHASE, "MAVEN", "workingSetBytes", false);
    final long peakSurefire = aggregateMaximum(jvm, REGRESSION_PHASE, "SUREFIRE");
    final long peakDocker = maximum(docker, REGRESSION_PHASE, null, "memoryBytes", false);
    final long minimumHost = minimum(docker, REGRESSION_PHASE, "hostAvailableBytes");
    return new Snapshot(
        durationMs,
        baselineMaven,
        peakMaven,
        Math.max(0L, peakMaven - baselineMaven),
        peakSurefire,
        peakDocker,
        minimumHost,
        Qdr7CapacityContracts.sha256(evidenceRoot.resolve("jvm-series.csv")),
        Qdr7CapacityContracts.sha256(evidenceRoot.resolve("docker-series.csv")));
  }

  private static long maximum(
      final List<Row> rows,
      final String phase,
      final String role,
      final String field,
      final boolean allowZero) {
    final List<Long> values = values(rows, phase, role, field);
    final long value =
        values.stream()
            .mapToLong(Long::longValue)
            .max()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "resource sample unavailable: " + phase + "/" + field));
    if (!allowZero && value <= 0L) {
      throw new IllegalStateException("resource sample invalid: " + phase + "/" + field);
    }
    return value;
  }

  private static long minimum(final List<Row> rows, final String phase, final String field) {
    final long value =
        values(rows, phase, null, field).stream()
            .mapToLong(Long::longValue)
            .min()
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "resource sample unavailable: " + phase + "/" + field));
    if (value <= 0L) {
      throw new IllegalStateException("resource sample invalid: " + phase + "/" + field);
    }
    return value;
  }

  private static long aggregateMaximum(
      final List<Row> rows, final String phase, final String role) {
    final Map<String, Long> byTimestamp = new HashMap<>();
    for (final Row row : rows) {
      if (phase.equals(row.value("phase")) && role.equals(row.value("processRole"))) {
        final long workingSet = positiveLong(row, "workingSetBytes");
        try {
          byTimestamp.merge(row.value("timestampUtc"), workingSet, Math::addExact);
        } catch (final ArithmeticException overflow) {
          throw new IllegalStateException(
              "resource aggregate overflow: " + phase + "/" + role, overflow);
        }
      }
    }
    return byTimestamp.values().stream()
        .mapToLong(Long::longValue)
        .max()
        .orElseThrow(
            () -> new IllegalStateException("resource sample unavailable: " + phase + "/" + role));
  }

  private static List<Long> values(
      final List<Row> rows, final String phase, final String role, final String field) {
    final List<Long> values = new ArrayList<>();
    for (final Row row : rows) {
      if (phase.equals(row.value("phase"))
          && (role == null || role.equals(row.value("processRole")))) {
        values.add(positiveLong(row, field));
      }
    }
    return values;
  }

  private static long positiveLong(final Row row, final String field) {
    final String raw = row.value(field);
    if (raw.isBlank()) {
      throw new IllegalStateException("resource field missing: " + field);
    }
    try {
      final long value = Long.parseLong(raw);
      if (value <= 0L) {
        throw new IllegalStateException("resource field must be positive: " + field);
      }
      return value;
    } catch (final NumberFormatException invalid) {
      throw new IllegalStateException("resource field invalid: " + field, invalid);
    }
  }

  private static void requireCompleteJvmSampling(final List<Row> rows) {
    for (final Row row : rows) {
      final String phase = row.value("phase");
      if ((BASELINE_PHASE.equals(phase)
              || REGRESSION_PHASE.equals(phase)
              || POST_REGRESSION_PHASE.equals(phase))
          && !row.value("missingReason").isBlank()) {
        throw new IllegalStateException(
            "resource sampler failure: " + phase + "/" + row.value("missingReason"));
      }
    }
  }

  private static void requireCompleteDockerSampling(final List<Row> rows) {
    for (final Row row : rows) {
      final String phase = row.value("phase");
      if ((REGRESSION_PHASE.equals(phase) || POST_REGRESSION_PHASE.equals(phase))
          && !row.value("missingReason").isBlank()) {
        throw new IllegalStateException(
            "resource sampler failure: " + phase + "/" + row.value("missingReason"));
      }
    }
  }

  private static void requireRegressionCoverage(
      final List<Row> rows, final long durationMs, final String series) {
    final List<Long> regressionElapsed = distinctElapsed(rows, REGRESSION_PHASE, null);
    final List<Long> postElapsed = distinctElapsed(rows, POST_REGRESSION_PHASE, null);
    if (regressionElapsed.size() < 2) {
      throw new IllegalStateException("resource sampling coverage incomplete: " + series);
    }
    requireBoundedGaps(regressionElapsed, series);
    final long requiredSpan = Math.max(0L, durationMs - DURATION_TOLERANCE_MS);
    if (regressionElapsed.getLast() - regressionElapsed.getFirst() < requiredSpan) {
      throw new IllegalStateException("resource sampling duration incomplete: " + series);
    }
    if (postElapsed.isEmpty()
        || postElapsed.getFirst() < regressionElapsed.getLast()
        || postElapsed.getFirst() - regressionElapsed.getLast() > MAX_SAMPLE_GAP_MS) {
      throw new IllegalStateException("resource sampling tail missing or stale: " + series);
    }
  }

  private static void requireRoleRegressionCoverage(final List<Row> rows, final String role) {
    final List<Long> elapsed = distinctElapsed(rows, REGRESSION_PHASE, role);
    if (elapsed.size() < 2) {
      throw new IllegalStateException("resource role sampling coverage incomplete: " + role);
    }
    requireBoundedGaps(elapsed, "jvm/" + role);
  }

  private static List<Long> distinctElapsed(
      final List<Row> rows, final String phase, final String role) {
    final List<Long> elapsed = new ArrayList<>();
    Long previousRaw = null;
    for (final Row row : rows) {
      final long current = nonNegativeLong(row, "elapsedMs");
      if (previousRaw != null && current < previousRaw) {
        throw new IllegalStateException("resource sampling elapsed order regressed");
      }
      previousRaw = current;
      if (phase.equals(row.value("phase"))
          && (role == null || role.equals(row.value("processRole")))
          && (elapsed.isEmpty() || elapsed.getLast() != current)) {
        elapsed.add(current);
      }
    }
    return elapsed;
  }

  private static void requireBoundedGaps(final List<Long> elapsedValues, final String series) {
    long previous = elapsedValues.getFirst();
    for (int index = 1; index < elapsedValues.size(); index++) {
      final long elapsed = elapsedValues.get(index);
      if (elapsed - previous > MAX_SAMPLE_GAP_MS) {
        throw new IllegalStateException("resource sampling gap exceeded: " + series);
      }
      previous = elapsed;
    }
  }

  private static List<Row> rows(
      final Path path, final String expectedScenario, final Binding binding) throws IOException {
    if (!Files.isRegularFile(path)) {
      throw new IllegalStateException("resource sampler artifact missing: " + path.getFileName());
    }
    final List<String> lines = Files.readAllLines(path);
    if (lines.size() < 2) {
      throw new IllegalStateException(
          "resource sampler artifact has no samples: " + path.getFileName());
    }
    final String[] headers = lines.getFirst().split(",", -1);
    final Map<String, Integer> indexes = new LinkedHashMap<>();
    for (int index = 0; index < headers.length; index++) {
      indexes.put(headers[index], index);
    }
    final List<Row> rows = new ArrayList<>();
    for (int line = 1; line < lines.size(); line++) {
      final String[] values = lines.get(line).split(",", -1);
      if (values.length != headers.length) {
        throw new IllegalStateException("resource sampler row shape mismatch");
      }
      final Row row = new Row(indexes, values);
      if (!binding.runId().equals(row.value("runId"))) {
        throw new IllegalStateException("resource sampler run binding mismatch");
      }
      if (!binding.commitSha().equals(row.value("commitSha"))) {
        throw new IllegalStateException("resource sampler commit binding mismatch");
      }
      if (!expectedScenario.equals(row.value("scenario"))) {
        throw new IllegalStateException("resource sampler scenario binding mismatch");
      }
      rows.add(row);
    }
    return rows;
  }

  private static long nonNegativeLong(final Row row, final String field) {
    final String raw = row.value(field);
    try {
      final long value = Long.parseLong(raw);
      if (value < 0L) {
        throw new IllegalStateException("resource field must be non-negative: " + field);
      }
      return value;
    } catch (final NumberFormatException invalid) {
      throw new IllegalStateException("resource field invalid: " + field, invalid);
    }
  }

  record Binding(String runId, String commitSha) {
    Binding {
      if (runId == null
          || runId.isBlank()
          || commitSha == null
          || !commitSha.matches("^[a-f0-9]{40}$")) {
        throw new IllegalArgumentException("resource sampler binding is invalid");
      }
    }
  }

  record Snapshot(
      long durationMs,
      long mavenBaselineWorkingSetBytes,
      long mavenPeakWorkingSetBytes,
      long mavenWorkingSetDeltaBytes,
      long surefirePeakAggregateWorkingSetBytes,
      long dockerPeakMemoryBytes,
      long minimumHostAvailableBytes,
      String jvmSeriesSha256,
      String dockerSeriesSha256) {

    Map<String, Object> fields() {
      final Map<String, Object> fields = new LinkedHashMap<>();
      fields.put("durationMs", durationMs);
      fields.put("mavenBaselineWorkingSetBytes", mavenBaselineWorkingSetBytes);
      fields.put("mavenPeakWorkingSetBytes", mavenPeakWorkingSetBytes);
      fields.put("mavenWorkingSetDeltaBytes", mavenWorkingSetDeltaBytes);
      fields.put(
          "mavenWorkingSetRegressionRatio",
          (double) mavenWorkingSetDeltaBytes / (double) mavenBaselineWorkingSetBytes);
      fields.put("surefirePeakAggregateWorkingSetBytes", surefirePeakAggregateWorkingSetBytes);
      fields.put("dockerPeakMemoryBytes", dockerPeakMemoryBytes);
      fields.put("minimumHostAvailableBytes", minimumHostAvailableBytes);
      fields.put("jvmSeriesSha256", jvmSeriesSha256);
      fields.put("dockerSeriesSha256", dockerSeriesSha256);
      fields.put("samplingPhase", REGRESSION_PHASE);
      fields.put("unit", "bytes");
      return fields;
    }
  }

  private record Row(Map<String, Integer> indexes, String[] values) {
    private String value(final String field) {
      final Integer index = indexes.get(field);
      if (index == null) {
        throw new IllegalStateException("resource sampler column missing: " + field);
      }
      return values[index];
    }
  }
}
