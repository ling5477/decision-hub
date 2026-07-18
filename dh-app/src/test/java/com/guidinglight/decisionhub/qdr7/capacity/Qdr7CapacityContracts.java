package com.guidinglight.decisionhub.qdr7.capacity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.sql.SQLException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/** Stage-QDR-7 capacity harness 的不可变合同、统计和分类工具。 */
final class Qdr7CapacityContracts {

  static final String SCHEMA_VERSION = "qdr7-capacity-1";
  static final String CRITERIA_VERSION = "qdr7-capacity-criteria-1";
  static final int REQUIRED_SEED = 7;
  static final String UNIT_SYSTEM = "milliseconds-bytes-requestsPerSecond-percent-rfc3339-utc";
  static final Pattern RUN_ID_PATTERN = Pattern.compile("^[0-9]{8}T[0-9]{6}Z$");
  static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
          .withLocale(Locale.ROOT)
          .withZone(ZoneOffset.UTC);

  private Qdr7CapacityContracts() {}

  static RunContext parseRunContext(
      final String runId, final String seedValue, final Path projectRoot, final String commitSha) {
    return parseRunContext(runId, seedValue, projectRoot, commitSha, false);
  }

  /**
   * 解析formal或qualification运行上下文，并把两类证据写入互不覆盖的根目录。
   *
   * @param qualificationOnly {@code true}时只产生qualification证据，不形成formal acceptance。
   */
  static RunContext parseRunContext(
      final String runId,
      final String seedValue,
      final Path projectRoot,
      final String commitSha,
      final boolean qualificationOnly) {
    if (runId == null || !RUN_ID_PATTERN.matcher(runId).matches()) {
      throw new IllegalArgumentException("runId must use UTC yyyyMMddTHHmmssZ format");
    }
    final Instant parsed;
    try {
      parsed =
          Instant.from(
              DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                  .withZone(ZoneOffset.UTC)
                  .parse(runId));
    } catch (final RuntimeException invalidTimestamp) {
      throw new IllegalArgumentException(
          "runId contains an invalid UTC timestamp", invalidTimestamp);
    }
    final int seed;
    try {
      seed = Integer.parseInt(Objects.requireNonNull(seedValue, "seed"));
    } catch (final RuntimeException invalidSeed) {
      throw new IllegalArgumentException("seed must be integer 7", invalidSeed);
    }
    if (seed != REQUIRED_SEED) {
      throw new IllegalArgumentException("seed must be integer 7");
    }
    if (commitSha == null || !commitSha.matches("^[a-f0-9]{40}$")) {
      throw new IllegalArgumentException("commitSha must be a lowercase 40-character SHA-1");
    }
    final Path normalizedRoot =
        Objects.requireNonNull(projectRoot, "projectRoot").toAbsolutePath().normalize();
    final Path evidenceBase =
        normalizedRoot.resolve(
            qualificationOnly
                ? "target/qdr7-capacity-qualification"
                : "target/qdr7-capacity-acceptance");
    final Path evidenceRoot = evidenceBase.resolve(runId).normalize();
    if (!evidenceRoot.startsWith(evidenceBase)) {
      throw new IllegalArgumentException("runId resolved outside the evidence root");
    }
    return new RunContext(
        runId, seed, parsed, commitSha, normalizedRoot, evidenceRoot, qualificationOnly);
  }

  static CriteriaSnapshot loadCriteria(final Path projectRoot, final ObjectMapper mapper)
      throws IOException {
    final Path config = projectRoot.resolve("config/qdr7-capacity/qdr7-capacity-thresholds.json");
    final JsonNode root = mapper.readTree(config.toFile());
    final String version = requiredText(root, "criteriaVersion");
    if (!CRITERIA_VERSION.equals(version)) {
      throw new IllegalStateException("unsupported criteriaVersion: " + version);
    }
    final Path source = projectRoot.resolve(requiredText(root, "sourceDocument")).normalize();
    if (!source.startsWith(projectRoot.normalize()) || !Files.isRegularFile(source)) {
      throw new IllegalStateException("criteria source document is unavailable");
    }
    final String expectedHash = requiredText(root, "sourceDocumentSha256");
    final String actualHash = sha256(source);
    if (!expectedHash.equals(actualHash)) {
      throw new IllegalStateException(
          "criteria source hash drift: expected=" + expectedHash + ", actual=" + actualHash);
    }
    final List<String> scenarios = new ArrayList<>();
    root.path("scenarioRegistry").forEach(node -> scenarios.add(node.asText()));
    if (scenarios.size() != ScenarioRegistry.mandatory().size()
        || !scenarios.equals(ScenarioRegistry.mandatory())) {
      throw new IllegalStateException("criteria scenario registry does not match the frozen order");
    }
    return new CriteriaSnapshot(version, source, expectedHash, root);
  }

  static String sha256(final Path path) throws IOException {
    return sha256(Files.readAllBytes(path));
  }

  static String sha256(final String value) {
    return sha256(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String sha256(final byte[] value) {
    try {
      final byte[] digest = MessageDigest.getInstance("SHA-256").digest(value);
      final StringBuilder result = new StringBuilder(digest.length * 2);
      for (final byte current : digest) {
        result.append(String.format(Locale.ROOT, "%02x", current));
      }
      return result.toString();
    } catch (final java.security.NoSuchAlgorithmException impossible) {
      throw new IllegalStateException("SHA-256 is unavailable", impossible);
    }
  }

  static String timestamp(final Instant instant) {
    return TIMESTAMP_FORMATTER.format(instant);
  }

  static Map<String, Object> commonArtifact(
      final RunContext context,
      final String scenario,
      final HarnessStatus status,
      final Instant startedAt,
      final Instant finishedAt) {
    final Map<String, Object> artifact = new LinkedHashMap<>();
    artifact.put("schemaVersion", SCHEMA_VERSION);
    artifact.put("runId", context.runId());
    artifact.put("commitSha", context.commitSha());
    artifact.put("scenario", scenario);
    artifact.put("status", status.name());
    artifact.put("startedAtUtc", timestamp(startedAt));
    artifact.put("finishedAtUtc", timestamp(finishedAt));
    artifact.put("durationMs", Math.max(0L, finishedAt.toEpochMilli() - startedAt.toEpochMilli()));
    artifact.put("seed", context.seed());
    artifact.put("unitSystem", UNIT_SYSTEM);
    artifact.put("missingValues", new ArrayList<String>());
    artifact.put("criteriaVersion", CRITERIA_VERSION);
    return artifact;
  }

  private static String requiredText(final JsonNode node, final String field) {
    final String value = node.path(field).asText();
    if (value.isBlank()) {
      throw new IllegalStateException("missing required field: " + field);
    }
    return value;
  }

  record RunContext(
      String runId,
      int seed,
      Instant runTimestamp,
      String commitSha,
      Path projectRoot,
      Path evidenceRoot,
      boolean qualificationOnly) {}

  record CriteriaSnapshot(
      String criteriaVersion, Path sourceDocument, String sourceDocumentSha256, JsonNode root) {}

  enum HarnessStatus {
    PASS,
    FAIL,
    BLOCKED,
    NOT_RUN,
    NOT_FORMAL
  }

  enum ExecutionState {
    NOT_STARTED,
    STARTED,
    PARTIAL,
    COMPLETED
  }

  enum ScenarioVerdict {
    NOT_EVALUATED,
    PASS,
    FAIL,
    BLOCKED
  }

  enum SemanticExit {
    PASS(0, HarnessStatus.PASS),
    ENVIRONMENT_PREFLIGHT_BLOCKED(10, HarnessStatus.BLOCKED),
    ACTUAL_WIRING_BLOCKED(20, HarnessStatus.BLOCKED),
    MANDATORY_SCENARIO_MISSING(30, HarnessStatus.BLOCKED),
    CORRECTNESS_INVARIANT_FAILED(40, HarnessStatus.FAIL),
    NUMERIC_THRESHOLD_FAILED(50, HarnessStatus.FAIL),
    FULL_REGRESSION_FAILED(60, HarnessStatus.FAIL),
    QUALITY_GATE_FAILED(70, HarnessStatus.FAIL),
    ARTIFACT_VALIDATION_BLOCKED(80, HarnessStatus.BLOCKED),
    SECRET_SCAN_FAILED(90, HarnessStatus.FAIL),
    UNEXPECTED_HARNESS_FAILURE(100, HarnessStatus.BLOCKED);

    private static final List<Integer> PRIORITY =
        List.of(90, 80, 70, 60, 40, 50, 30, 20, 10, 100, 0);

    private final int code;
    private final HarnessStatus status;

    SemanticExit(final int code, final HarnessStatus status) {
      this.code = code;
      this.status = status;
    }

    int code() {
      return code;
    }

    HarnessStatus status() {
      return status;
    }

    static SemanticExit fromCode(final int code) {
      for (final SemanticExit value : values()) {
        if (value.code == code) {
          return value;
        }
      }
      throw new IllegalArgumentException("unsupported semantic exit code: " + code);
    }

    static SemanticExit select(final List<SemanticExit> findings) {
      if (findings == null || findings.isEmpty()) {
        return PASS;
      }
      return findings.stream()
          .min(Comparator.comparingInt(value -> PRIORITY.indexOf(value.code)))
          .orElse(PASS);
    }
  }

  /** Mandatory driver 的统一生命周期；teardown 必须由调用者放入 finally。 */
  interface ScenarioDriver {
    String scenarioId();

    String scenarioVersion();

    boolean mandatory();

    void setup(ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException;

    void execute(ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException;

    void sample(ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException;

    void assertCorrectness(ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException;

    void compareThreshold(ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException;

    void writeArtifacts(ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException;

    void teardown(ScenarioExecution execution)
        throws IOException, InterruptedException, SQLException, ExecutionException;
  }

  /** Driver 共享的受控状态；只存聚合指标和结构化 findings。 */
  static final class ScenarioExecution {
    private final RunContext context;
    private final CriteriaSnapshot criteria;
    private final Map<String, Object> values = new LinkedHashMap<>();
    private final List<SemanticExit> findings = new ArrayList<>();

    ScenarioExecution(final RunContext context, final CriteriaSnapshot criteria) {
      this.context = Objects.requireNonNull(context);
      this.criteria = Objects.requireNonNull(criteria);
    }

    RunContext context() {
      return context;
    }

    CriteriaSnapshot criteria() {
      return criteria;
    }

    Map<String, Object> values() {
      return values;
    }

    List<SemanticExit> findings() {
      return findings;
    }
  }

  static final class ScenarioRegistry {
    private static final List<String> MANDATORY =
        List.of(
            "actual-wiring",
            "rate-matrix",
            "cold-start-quota",
            "tenant-environment-isolation",
            "canonical-source-fail-closed",
            "nonce-race",
            "idempotency-lifecycle",
            "tenant-scoped-cleanup",
            "postgres-hikari-contention",
            "postgres-same-pool-recovery",
            "spring-context-restart",
            "postgres-persistent-volume-restart",
            "post-recovery-concurrency",
            "full-regression",
            "quality-gate");

    private ScenarioRegistry() {}

    static List<String> mandatory() {
      return MANDATORY;
    }

    static Optional<String> validate(final List<? extends ScenarioDriver> drivers) {
      final List<String> actual =
          drivers.stream()
              .filter(ScenarioDriver::mandatory)
              .map(ScenarioDriver::scenarioId)
              .toList();
      if (!MANDATORY.equals(actual)) {
        return Optional.of("mandatory scenario order mismatch: " + actual);
      }
      if (drivers.stream()
          .anyMatch(driver -> !"qdr7-capacity-scenario-1".equals(driver.scenarioVersion()))) {
        return Optional.of("scenario version mismatch");
      }
      return Optional.empty();
    }
  }

  record Statistics(
      long count,
      long min,
      long median,
      long max,
      double mean,
      double sampleVariance,
      long p50,
      long p95,
      long p99,
      double throughput) {}

  static Statistics statistics(final List<Long> samples, final long elapsedMs) {
    if (samples == null || samples.isEmpty()) {
      throw new IllegalArgumentException("mandatory metric is NOT_CAPTURED");
    }
    final List<Long> sorted = samples.stream().sorted().toList();
    final BigDecimal sum =
        sorted.stream().map(BigDecimal::valueOf).reduce(BigDecimal.ZERO, BigDecimal::add);
    final double mean =
        sum.divide(BigDecimal.valueOf(sorted.size()), 12, RoundingMode.HALF_UP).doubleValue();
    double squared = 0D;
    for (final long sample : sorted) {
      final double delta = sample - mean;
      squared += delta * delta;
    }
    final double variance = sorted.size() > 1 ? squared / (sorted.size() - 1D) : 0D;
    final double throughput = elapsedMs > 0 ? sorted.size() * 1000D / elapsedMs : 0D;
    return new Statistics(
        sorted.size(),
        sorted.get(0),
        nearestRank(sorted, 0.50D),
        sorted.get(sorted.size() - 1),
        mean,
        variance,
        nearestRank(sorted, 0.50D),
        nearestRank(sorted, 0.95D),
        nearestRank(sorted, 0.99D),
        throughput);
  }

  static long nearestRank(final List<Long> sortedSamples, final double percentile) {
    if (sortedSamples == null || sortedSamples.isEmpty()) {
      throw new IllegalArgumentException("percentile input is empty");
    }
    if (percentile <= 0D || percentile > 1D) {
      throw new IllegalArgumentException("percentile must be in (0, 1]");
    }
    final int rank = (int) Math.ceil(percentile * sortedSamples.size());
    return sortedSamples.get(Math.max(0, rank - 1));
  }

  record ThresholdResult(
      String criterionId,
      String operator,
      BigDecimal observed,
      BigDecimal threshold,
      String unit,
      HarnessStatus status,
      String reason) {}

  static ThresholdResult compare(
      final String criterionId,
      final BigDecimal observed,
      final String operator,
      final BigDecimal threshold,
      final String observedUnit,
      final String thresholdUnit) {
    if (observed == null) {
      return new ThresholdResult(
          criterionId,
          operator,
          null,
          threshold,
          thresholdUnit,
          HarnessStatus.BLOCKED,
          "NOT_CAPTURED");
    }
    if (!Objects.equals(observedUnit, thresholdUnit)) {
      return new ThresholdResult(
          criterionId,
          operator,
          observed,
          threshold,
          thresholdUnit,
          HarnessStatus.BLOCKED,
          "UNIT_MISMATCH");
    }
    final int relation = observed.compareTo(threshold);
    final boolean passed =
        switch (operator) {
          case "<" -> relation < 0;
          case "<=" -> relation <= 0;
          case "=" -> relation == 0;
          case ">=" -> relation >= 0;
          case ">" -> relation > 0;
          default -> throw new IllegalArgumentException("unsupported operator: " + operator);
        };
    return new ThresholdResult(
        criterionId,
        operator,
        observed,
        threshold,
        thresholdUnit,
        passed ? HarnessStatus.PASS : HarnessStatus.FAIL,
        passed ? "MATCH" : "THRESHOLD_MISS");
  }
}
