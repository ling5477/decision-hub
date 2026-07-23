package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Qdr7CapacityContractsTest {

  private static final String COMMIT = "a".repeat(40);

  @TempDir Path temporaryDirectory;

  @Test
  void runContextRequiresFrozenRunIdSeedAndSafeArtifactPath() {
    final Qdr7CapacityContracts.RunContext context =
        Qdr7CapacityContracts.parseRunContext("20260715T120000Z", "7", temporaryDirectory, COMMIT);

    assertThat(context.runId()).isEqualTo("20260715T120000Z");
    assertThat(context.seed()).isEqualTo(7);
    assertThat(context.evidenceRoot())
        .isEqualTo(
            temporaryDirectory
                .toAbsolutePath()
                .normalize()
                .resolve("target/qdr7-capacity-acceptance/20260715T120000Z"));

    assertThatThrownBy(
            () ->
                Qdr7CapacityContracts.parseRunContext("../unsafe", "7", temporaryDirectory, COMMIT))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(
            () ->
                Qdr7CapacityContracts.parseRunContext(
                    "20260715T120000Z", "8", temporaryDirectory, COMMIT))
        .isInstanceOf(IllegalArgumentException.class);

    final Qdr7CapacityContracts.RunContext qualification =
        Qdr7CapacityContracts.parseRunContext(
            "20260715T120001Z", "7", temporaryDirectory, COMMIT, true);
    assertThat(qualification.qualificationOnly()).isTrue();
    assertThat(qualification.evidenceRoot())
        .isEqualTo(
            temporaryDirectory
                .toAbsolutePath()
                .normalize()
                .resolve("target/qdr7-capacity-qualification/20260715T120001Z"));
  }

  @Test
  void criteriaHashMatchesFrozenSourceAndDriftFailsClosed() throws IOException {
    final Path repositoryRoot = repositoryRoot();
    final ObjectMapper mapper = new ObjectMapper();

    final Qdr7CapacityContracts.CriteriaSnapshot snapshot =
        Qdr7CapacityContracts.loadCriteria(repositoryRoot, mapper);
    final Path stableContractRoot =
        repositoryRoot.resolve("config/qdr7-capacity").toAbsolutePath().normalize();
    final Path currentDocsRoot = repositoryRoot.resolve("docs/current").toAbsolutePath().normalize();

    assertThat(snapshot.criteriaVersion()).isEqualTo(Qdr7CapacityContracts.CRITERIA_VERSION);
    assertThat(Files.isRegularFile(snapshot.sourceDocument())).isTrue();
    assertThat(snapshot.sourceDocument().toAbsolutePath().normalize()).startsWith(stableContractRoot);
    assertThat(snapshot.sourceDocument().toAbsolutePath().normalize().startsWith(currentDocsRoot))
        .isFalse();
    assertThat(Files.size(snapshot.sourceDocument())).isPositive();
    assertThat(snapshot.sourceDocumentSha256())
        .isEqualTo(Qdr7CapacityContracts.sha256(snapshot.sourceDocument()));
    assertThat(snapshot.root().path("scenarioRegistry")).hasSize(15);
    assertThat(
            snapshot
                .root()
                .path("numericThresholds")
                .path("rate")
                .path("16")
                .path("throughputMin")
                .asDouble())
        .isEqualTo(38.7D);
    assertThat(
            snapshot
                .root()
                .path("numericThresholds")
                .path("regressionMax")
                .path("durationMs")
                .asLong())
        .isEqualTo(660000L);

    final Path copiedRoot = temporaryDirectory.resolve("drift");
    final Path copiedConfig =
        copiedRoot.resolve("config/qdr7-capacity/qdr7-capacity-thresholds.json");
    final Path copiedSource =
        copiedRoot.resolve("config/qdr7-capacity/qdr7-capacity-acceptance-criteria.md");
    Files.createDirectories(copiedConfig.getParent());
    Files.createDirectories(copiedSource.getParent());
    Files.copy(
        repositoryRoot.resolve("config/qdr7-capacity/qdr7-capacity-thresholds.json"), copiedConfig);
    Files.writeString(copiedSource, "drift");

    assertThatThrownBy(() -> Qdr7CapacityContracts.loadCriteria(copiedRoot, mapper))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("criteria source hash drift");
  }

  @Test
  void nonceDriverUsesTheReplayKeyColumnFromTheFrozenSchema() throws IOException {
    final Path root = repositoryRoot();
    final String formalHarness =
        Files.readString(
            root.resolve(
                "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));
    final String nonceMigration =
        Files.readString(
            root.resolve(
                "dh-app/src/main/resources/db/migration/V4__nq_feedback_replay_nonce.sql"));

    assertThat(nonceMigration)
        .contains("replay_key varchar(512) primary key")
        .doesNotContain(" nonce varchar", "nonce varchar");
    assertThat(formalHarness)
        .contains("select count(*) from dh_nq_replay_nonce where replay_key=?")
        .doesNotContain("select count(*) from dh_nq_replay_nonce where nonce=?");
  }

  @Test
  void nearestRankStatisticsAreDeterministicAndVarianceUsesSampleDenominator() {
    final Qdr7CapacityContracts.Statistics statistics =
        Qdr7CapacityContracts.statistics(List.of(4L, 1L, 3L, 2L), 1000L);

    assertThat(statistics.count()).isEqualTo(4);
    assertThat(statistics.min()).isEqualTo(1);
    assertThat(statistics.median()).isEqualTo(2);
    assertThat(statistics.p50()).isEqualTo(2);
    assertThat(statistics.p95()).isEqualTo(4);
    assertThat(statistics.p99()).isEqualTo(4);
    assertThat(statistics.max()).isEqualTo(4);
    assertThat(statistics.mean()).isEqualTo(2.5D);
    assertThat(statistics.sampleVariance()).isCloseTo(1.6666666667D, within(0.000000001D));
    assertThat(statistics.throughput()).isEqualTo(4D);

    assertThatThrownBy(() -> Qdr7CapacityContracts.statistics(List.of(), 1L))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("NOT_CAPTURED");
  }

  @Test
  void scenarioRegistryRejectsMissingOrReorderedMandatoryDriver() {
    final List<Qdr7CapacityContracts.ScenarioDriver> complete =
        Qdr7CapacityContracts.ScenarioRegistry.mandatory().stream()
            .map(ContractDriver::new)
            .map(Qdr7CapacityContracts.ScenarioDriver.class::cast)
            .toList();

    assertThat(Qdr7CapacityContracts.ScenarioRegistry.validate(complete)).isEmpty();
    assertThat(Qdr7CapacityContracts.ScenarioRegistry.validate(complete.subList(0, 14)))
        .hasValueSatisfying(message -> assertThat(message).contains("order mismatch"));
  }

  @Test
  void semanticExitMappingPreservesFrozenStatusAndPriority() {
    for (final Qdr7CapacityContracts.SemanticExit value :
        Qdr7CapacityContracts.SemanticExit.values()) {
      assertThat(Qdr7CapacityContracts.SemanticExit.fromCode(value.code())).isEqualTo(value);
    }
    assertThat(
            Qdr7CapacityContracts.SemanticExit.select(
                List.of(
                    Qdr7CapacityContracts.SemanticExit.NUMERIC_THRESHOLD_FAILED,
                    Qdr7CapacityContracts.SemanticExit.ARTIFACT_VALIDATION_BLOCKED,
                    Qdr7CapacityContracts.SemanticExit.SECRET_SCAN_FAILED)))
        .isEqualTo(Qdr7CapacityContracts.SemanticExit.SECRET_SCAN_FAILED);
    assertThat(Qdr7CapacityContracts.SemanticExit.ENVIRONMENT_PREFLIGHT_BLOCKED.status())
        .isEqualTo(Qdr7CapacityContracts.HarnessStatus.BLOCKED);
    assertThat(Qdr7CapacityContracts.SemanticExit.CORRECTNESS_INVARIANT_FAILED.status())
        .isEqualTo(Qdr7CapacityContracts.HarnessStatus.FAIL);
  }

  private static org.assertj.core.data.Offset<Double> within(final double value) {
    return org.assertj.core.data.Offset.offset(value);
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

  private static final class ContractDriver implements Qdr7CapacityContracts.ScenarioDriver {
    private final String scenarioId;

    private ContractDriver(final String scenarioId) {
      this.scenarioId = scenarioId;
    }

    @Override
    public String scenarioId() {
      return scenarioId;
    }

    @Override
    public String scenarioVersion() {
      return "qdr7-capacity-scenario-1";
    }

    @Override
    public boolean mandatory() {
      return true;
    }

    @Override
    public void setup(final Qdr7CapacityContracts.ScenarioExecution execution) {}

    @Override
    public void execute(final Qdr7CapacityContracts.ScenarioExecution execution) {}

    @Override
    public void sample(final Qdr7CapacityContracts.ScenarioExecution execution) {}

    @Override
    public void assertCorrectness(final Qdr7CapacityContracts.ScenarioExecution execution) {}

    @Override
    public void compareThreshold(final Qdr7CapacityContracts.ScenarioExecution execution) {}

    @Override
    public void writeArtifacts(final Qdr7CapacityContracts.ScenarioExecution execution) {}

    @Override
    public void teardown(final Qdr7CapacityContracts.ScenarioExecution execution) {}
  }
}
