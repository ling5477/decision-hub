package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Qdr7CapacityEvidenceFinalizerTest {

  private static final String SHA = "a".repeat(40);
  private static final String TREE = "b".repeat(40);
  private static final String HASH = "c".repeat(64);
  private static final List<String> SCENARIOS =
      java.util.stream.IntStream.rangeClosed(1, 15).mapToObj(index -> "scenario-" + index).toList();
  private static final Map<String, Qdr7CapacityEvidenceFinalizer.ThresholdExpectation> THRESHOLDS =
      thresholds();
  private static final byte[] PAYLOAD = "sealed-evidence".getBytes(StandardCharsets.UTF_8);
  private static final Set<String> MANDATORY_ARTIFACTS = Set.of("sealed.json", "source.json");

  private final ObjectMapper mapper = new ObjectMapper();
  private ObjectNode environment;
  private ObjectNode execution;
  private ObjectNode ledger;
  private ObjectNode comparisons;
  private ObjectNode inventory;
  private Map<String, byte[]> artifactBytes;

  @BeforeEach
  void setUp() throws Exception {
    execution = binding(mapper.createObjectNode());
    environment = binding(mapper.createObjectNode());
    environment.put("status", "QUALIFIED");
    environment.remove("environmentManifestHash");
    final String environmentHash = sha256(mapper.writeValueAsBytes(environment));
    environment.put("environmentManifestHash", environmentHash);
    execution.put("environmentManifestHash", environmentHash);

    ledger = binding(mapper.createObjectNode());
    ledger.put("environmentManifestHash", environmentHash);
    final ArrayNode scenarios = ledger.putArray("scenarios");
    SCENARIOS.forEach(id -> scenarios.addObject().put("scenarioId", id).put("verdict", "PASS"));

    comparisons = binding(mapper.createObjectNode());
    comparisons.put("environmentManifestHash", environmentHash);
    final ArrayNode rows = comparisons.putArray("comparisons");
    int criterion = 0;
    for (final Map.Entry<String, Qdr7CapacityEvidenceFinalizer.ThresholdExpectation> threshold :
        THRESHOLDS.entrySet()) {
      for (int executionIndex = 0;
          executionIndex < threshold.getValue().expectedExecutions();
          executionIndex++) {
        rows.addObject()
            .put("thresholdPath", threshold.getKey())
            .put("criterionId", "criterion-" + criterion++)
            .put("sourceArtifact", threshold.getValue().sourceArtifact())
            .put("observed", 10.0D)
            .put("operator", threshold.getValue().operator())
            .put("threshold", threshold.getValue().threshold())
            .put("unit", threshold.getValue().unit())
            .put("status", "PASS");
      }
    }

    inventory = binding(mapper.createObjectNode());
    inventory.put("environmentManifestHash", environmentHash);
    inventory
        .putArray("artifacts")
        .addObject()
        .put("path", "sealed.json")
        .put("sha256", sha256(PAYLOAD));
    inventory
        .withArray("artifacts")
        .addObject()
        .put("path", "source.json")
        .put("sha256", sha256(sourcePayload(10.0D)));
    artifactBytes = new LinkedHashMap<>();
    artifactBytes.put("sealed.json", PAYLOAD);
    artifactBytes.put("source.json", sourcePayload(10.0D));
  }

  @Test
  void completePacketPassesWithinFrozenProfile() {
    assertThat(finalizePacket().verdict()).isEqualTo("PASS_WITHIN_FROZEN_PROFILE");
  }

  @Test
  void missingScenarioOrThresholdIsInvalid() {
    ((ArrayNode) ledger.path("scenarios")).remove(0);
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");

    setUpUnchecked();
    ((ArrayNode) comparisons.path("comparisons")).remove(0);
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
  }

  @Test
  void duplicateThresholdResultIsInvalid() {
    ((ArrayNode) comparisons.path("comparisons"))
        .add(comparisons.path("comparisons").path(0).deepCopy());
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
    assertThat(finalizePacket().findings())
        .anyMatch(value -> value.startsWith("DUPLICATE_THRESHOLD_RESULT"));
  }

  @Test
  void duplicateScenarioOrOrphanThresholdIsInvalid() {
    ((ArrayNode) ledger.path("scenarios")).add(ledger.path("scenarios").path(0).deepCopy());
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");

    setUpUnchecked();
    ((ObjectNode) comparisons.withArray("comparisons").get(0))
        .put("thresholdPath", "numericThresholds.orphan");
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
  }

  @Test
  void environmentOrExecutionGapBlocksAndMeasuredFailureFails() {
    environment.put("status", "NOT_QUALIFIED");
    rebindEnvironmentHash();
    assertThat(finalizePacket().verdict()).isEqualTo("BLOCKED");

    setUpUnchecked();
    ((ObjectNode) ledger.withArray("scenarios").get(0)).put("verdict", "BLOCKED");
    assertThat(finalizePacket().verdict()).isEqualTo("BLOCKED");

    setUpUnchecked();
    for (final JsonNode row : comparisons.withArray("comparisons")) {
      ((ObjectNode) row).put("observed", 1.0D).put("status", "FAIL");
    }
    setSourcePayload(sourcePayload(1.0D));
    assertThat(finalizePacket().verdict()).isEqualTo("FAIL");
  }

  @Test
  void thresholdOperatorThresholdSourceAndUnitDriftAreInvalid() {
    final ObjectNode row = (ObjectNode) comparisons.withArray("comparisons").get(0);
    row.put("operator", "<=");
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");

    setUpUnchecked();
    ((ObjectNode) comparisons.withArray("comparisons").get(0)).put("threshold", 6.0D);
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");

    setUpUnchecked();
    ((ObjectNode) comparisons.withArray("comparisons").get(0)).put("sourceArtifact", "wrong.json");
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");

    setUpUnchecked();
    ((ObjectNode) comparisons.withArray("comparisons").get(0)).put("unit", "count");
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");

    setUpUnchecked();
    ((ObjectNode) comparisons.withArray("comparisons").get(0)).remove("observed");
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
  }

  @Test
  void thresholdStatusMustMatchIndependentComparison() {
    final ObjectNode row = (ObjectNode) comparisons.withArray("comparisons").get(0);
    row.put("observed", 1.0D);
    row.put("operator", ">=");
    row.put("threshold", 5.0D);
    row.put("unit", "operations/second");
    row.put("sourceArtifact", "rate-summary.json");
    row.put("status", "PASS");

    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
  }

  @Test
  void thresholdObservedMustMatchSealedSourceArtifact() {
    ((ObjectNode) comparisons.withArray("comparisons").get(0)).put("observed", 9.0D);
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
    assertThat(finalizePacket().findings())
        .anyMatch(value -> value.startsWith("THRESHOLD_SOURCE_OBSERVED_MISMATCH"));

    setUpUnchecked();
    setSourcePayload("{\"observed\":\"10\"}".getBytes(StandardCharsets.UTF_8));
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
    assertThat(finalizePacket().findings())
        .anyMatch(value -> value.startsWith("THRESHOLD_SOURCE_OBSERVED_UNREADABLE"));
  }

  @Test
  void rateSelectorBindsCriterionCoordinatesToTheSealedSummary() {
    final byte[] rateSource =
        "{\"rounds\":[{\"concurrency\":1,\"round\":1,\"throughput\":10.0}]}"
            .getBytes(StandardCharsets.UTF_8);
    final Map<String, Qdr7CapacityEvidenceFinalizer.ThresholdExpectation> expectations =
        Map.of(
            "numericThresholds.rate.1.throughputMin",
            new Qdr7CapacityEvidenceFinalizer.ThresholdExpectation(
                1,
                ">=",
                BigDecimal.valueOf(5),
                "operations/second",
                "rate-summary.json",
                "rate-summary.json#rounds[concurrency,round].throughput"));
    final ArrayNode rows = comparisons.withArray("comparisons");
    rows.removeAll();
    rows.addObject()
        .put("thresholdPath", "numericThresholds.rate.1.throughputMin")
        .put("criterionId", "rate.c1.r1.throughput")
        .put("sourceArtifact", "rate-summary.json")
        .put("observed", 10.0D)
        .put("operator", ">=")
        .put("threshold", 5.0D)
        .put("unit", "operations/second")
        .put("status", "PASS");
    inventory
        .withArray("artifacts")
        .addObject()
        .put("path", "rate-summary.json")
        .put("sha256", sha256(rateSource));
    final Set<String> mandatory = new java.util.HashSet<>(MANDATORY_ARTIFACTS);
    mandatory.add("rate-summary.json");
    artifactBytes.put("rate-summary.json", rateSource);

    assertThat(finalizePacket(expectations, mandatory).verdict())
        .isEqualTo("PASS_WITHIN_FROZEN_PROFILE");

    final byte[] tampered =
        "{\"rounds\":[{\"concurrency\":1,\"round\":1,\"throughput\":1.0}]}"
            .getBytes(StandardCharsets.UTF_8);
    artifactBytes.put("rate-summary.json", tampered);
    for (final JsonNode row : inventory.withArray("artifacts")) {
      if ("rate-summary.json".equals(row.path("path").asText())) {
        ((ObjectNode) row).put("sha256", sha256(tampered));
      }
    }
    assertThat(finalizePacket(expectations, mandatory).verdict()).isEqualTo("INVALID");
  }

  @Test
  void missingMandatoryArtifactIsInvalid() {
    inventory.withArray("artifacts").remove(0);
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
    assertThat(finalizePacket().findings()).contains("MANDATORY_ARTIFACT_MISSING:sealed.json");
  }

  @Test
  void hashProfileAndTreeMismatchesAreInvalid() {
    ((ObjectNode) inventory.withArray("artifacts").get(0)).put("sha256", HASH);
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");

    setUpUnchecked();
    ledger.put("profileVersion", "drifted-profile");
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");

    setUpUnchecked();
    comparisons.put("candidateTree", "d".repeat(40));
    assertThat(finalizePacket().verdict()).isEqualTo("INVALID");
  }

  private Qdr7CapacityEvidenceFinalizer.Result finalizePacket() {
    return finalizePacket(THRESHOLDS, MANDATORY_ARTIFACTS);
  }

  private Qdr7CapacityEvidenceFinalizer.Result finalizePacket(
      final Map<String, Qdr7CapacityEvidenceFinalizer.ThresholdExpectation> expectations,
      final Set<String> mandatoryArtifacts) {
    return Qdr7CapacityEvidenceFinalizer.finalizePacket(
        new Qdr7CapacityEvidenceFinalizer.Packet(
            environment,
            execution,
            ledger,
            comparisons,
            inventory,
            SCENARIOS,
            expectations,
            mandatoryArtifacts,
            artifactBytes),
        mapper);
  }

  private ObjectNode binding(final ObjectNode node) {
    return node.put("attemptId", "20260809T000000Z")
        .put("candidateSha", SHA)
        .put("candidateTree", TREE)
        .put("profileId", "qdr7-capacity-acceptance")
        .put("profileVersion", "qdr7-capacity-criteria-1")
        .put("scenarioSetHash", HASH)
        .put("thresholdSetHash", HASH)
        .put("environmentManifestHash", HASH)
        .put("harnessVersion", "qdr12-capacity-harness-1")
        .put("harnessHash", HASH)
        .put("generatedAt", "2026-08-09T00:00:00.000Z");
  }

  private static Map<String, Qdr7CapacityEvidenceFinalizer.ThresholdExpectation> thresholds() {
    final Map<String, Qdr7CapacityEvidenceFinalizer.ThresholdExpectation> result =
        new LinkedHashMap<>();
    for (int index = 1; index <= 41; index++) {
      result.put(
          "numericThresholds.path." + index,
          new Qdr7CapacityEvidenceFinalizer.ThresholdExpectation(
              index <= 29 ? 3 : 1,
              ">=",
              BigDecimal.valueOf(5.0D),
              "operations/second",
              "source.json",
              "source.json#observed"));
    }
    return Map.copyOf(result);
  }

  private static String sha256(final byte[] value) {
    try {
      return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
    } catch (final NoSuchAlgorithmException impossible) {
      throw new IllegalStateException(impossible);
    }
  }

  private void setUpUnchecked() {
    try {
      setUp();
    } catch (final Exception failure) {
      throw new IllegalStateException(failure);
    }
  }

  private static byte[] sourcePayload(final double observed) {
    return ("{\"observed\":" + observed + "}").getBytes(StandardCharsets.UTF_8);
  }

  private void setSourcePayload(final byte[] payload) {
    artifactBytes.put("source.json", payload);
    for (final JsonNode row : inventory.withArray("artifacts")) {
      if ("source.json".equals(row.path("path").asText())) {
        ((ObjectNode) row).put("sha256", sha256(payload));
      }
    }
  }

  private void rebindEnvironmentHash() {
    try {
      environment.remove("environmentManifestHash");
      final String environmentHash = sha256(mapper.writeValueAsBytes(environment));
      environment.put("environmentManifestHash", environmentHash);
      execution.put("environmentManifestHash", environmentHash);
      ledger.put("environmentManifestHash", environmentHash);
      comparisons.put("environmentManifestHash", environmentHash);
      inventory.put("environmentManifestHash", environmentHash);
    } catch (final Exception failure) {
      throw new IllegalStateException(failure);
    }
  }
}
