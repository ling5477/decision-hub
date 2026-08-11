package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class Qdr7CapacityResourceEvidenceTest {

  private static final String RUN_ID = "20260811T000000Z";
  private static final String SHA = "a".repeat(40);
  private static final String JVM_HEADER =
      "schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,phase,processRole,workingSetBytes,missingReason\n";
  private static final String DOCKER_HEADER =
      "schemaVersion,runId,commitSha,scenario,timestampUtc,elapsedMs,phase,memoryBytes,hostAvailableBytes,missingReason\n";

  @TempDir Path temporaryDirectory;

  @Test
  void normalizesBaselinePeakAggregateAndMinimumSamples() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "300", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "SUREFIRE", "250", "")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));
    Files.writeString(
        temporaryDirectory.resolve("docker-series.csv"),
        DOCKER_HEADER
            + docker("t1", 1000, "FULL_REGRESSION", "50", "1000", "")
            + docker("t2", 2000, "FULL_REGRESSION", "60", "900", "")
            + docker("t3", 3000, "POST_REGRESSION", "55", "950", ""));

    final Qdr7CapacityResourceEvidence.Snapshot snapshot = read(123L);

    assertThat(snapshot.durationMs()).isEqualTo(123L);
    assertThat(snapshot.mavenBaselineWorkingSetBytes()).isEqualTo(100L);
    assertThat(snapshot.mavenPeakWorkingSetBytes()).isEqualTo(160L);
    assertThat(snapshot.mavenWorkingSetDeltaBytes()).isEqualTo(60L);
    assertThat(snapshot.surefirePeakAggregateWorkingSetBytes()).isEqualTo(500L);
    assertThat(snapshot.dockerPeakMemoryBytes()).isEqualTo(60L);
    assertThat(snapshot.minimumHostAvailableBytes()).isEqualTo(900L);
    assertThat(snapshot.fields())
        .containsEntry("mavenWorkingSetRegressionRatio", 0.6D)
        .containsEntry("samplingPhase", "FULL_REGRESSION")
        .containsEntry("unit", "bytes");
    assertThat(snapshot.jvmSeriesSha256()).matches("^[a-f0-9]{64}$");
    assertThat(snapshot.dockerSeriesSha256()).matches("^[a-f0-9]{64}$");
  }

  @Test
  void missingOrInvalidRequiredMetricBlocksInsteadOfDefaultingToZero() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));
    Files.writeString(
        temporaryDirectory.resolve("docker-series.csv"),
        DOCKER_HEADER
            + docker("t1", 1000, "FULL_REGRESSION", "50", "1000", "")
            + docker("t2", 2000, "FULL_REGRESSION", "50", "1000", "")
            + docker("t3", 3000, "POST_REGRESSION", "50", "1000", ""));

    assertThatThrownBy(() -> read(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("SUREFIRE");
  }

  @Test
  void invalidNumericValueOrCsvShapeBlocksInsteadOfProducingEvidence() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));
    Files.writeString(
        temporaryDirectory.resolve("docker-series.csv"),
        DOCKER_HEADER
            + docker("t1", 1000, "FULL_REGRESSION", "not-a-number", "1000", "")
            + docker("t2", 2000, "FULL_REGRESSION", "50", "1000", "")
            + docker("t3", 3000, "POST_REGRESSION", "50", "1000", ""));

    assertThatThrownBy(() -> read(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("memoryBytes");

    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER + "qdr7-capacity-1," + RUN_ID + "," + SHA + ",bad,row,shape\n");
    Files.writeString(
        temporaryDirectory.resolve("docker-series.csv"),
        DOCKER_HEADER + docker("t1", 1000, "FULL_REGRESSION", "50", "1000", ""));

    assertThatThrownBy(() -> read(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("row shape mismatch");
  }

  @Test
  void intermittentJvmSamplerFailureBlocksEvenWhenValidSamplesExist() throws IOException {
    writeDockerEvidence();
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "UNAVAILABLE", "", "JVM_SAMPLER_FAILURE")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));

    assertThatThrownBy(() -> read(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("JVM_SAMPLER_FAILURE");
  }

  @Test
  void nonPositiveOrOverflowedSurefireAggregateBlocks() throws IOException {
    writeDockerEvidence();
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "-1", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "SUREFIRE", "1", "")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));
    assertThatThrownBy(() -> read(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("workingSetBytes");

    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", Long.toString(Long.MAX_VALUE), "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "1", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "SUREFIRE", "1", "")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));
    assertThatThrownBy(() -> read(1L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("overflow");
  }

  @Test
  void sparseRegressionSamplesCannotProduceCapacityEvidence() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "200", ""));
    Files.writeString(
        temporaryDirectory.resolve("docker-series.csv"),
        DOCKER_HEADER + docker("t1", 1000, "FULL_REGRESSION", "50", "1000", ""));

    assertThatThrownBy(() -> read(60_000L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("sampling");
  }

  @Test
  void runCommitAndScenarioBindingDriftCannotProduceEvidence() throws IOException {
    writeValidEvidence();
    final Path jvmPath = temporaryDirectory.resolve("jvm-series.csv");
    Files.writeString(jvmPath, Files.readString(jvmPath).replace(RUN_ID, "wrong-run"));
    assertThatThrownBy(() -> read(1_000L)).hasMessageContaining("run binding mismatch");

    writeValidEvidence();
    Files.writeString(jvmPath, Files.readString(jvmPath).replace(SHA, "b".repeat(40)));
    assertThatThrownBy(() -> read(1_000L)).hasMessageContaining("commit binding mismatch");

    writeValidEvidence();
    Files.writeString(
        jvmPath,
        Files.readString(jvmPath)
            .replace("jvm-resource-sampling", "docker-host-resource-sampling"));
    assertThatThrownBy(() -> read(1_000L)).hasMessageContaining("scenario binding mismatch");
  }

  @Test
  void oversizedGapAndMissingPostRegressionTailCannotProduceEvidence() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t2", 7001, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t2", 7001, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t3", 8000, "POST_REGRESSION", "MAVEN", "140", ""));
    Files.writeString(
        temporaryDirectory.resolve("docker-series.csv"),
        DOCKER_HEADER
            + docker("t1", 1000, "FULL_REGRESSION", "50", "1000", "")
            + docker("t2", 7001, "FULL_REGRESSION", "50", "1000", "")
            + docker("t3", 8000, "POST_REGRESSION", "50", "1000", ""));
    assertThatThrownBy(() -> read(6_000L)).hasMessageContaining("gap exceeded");

    writeValidEvidence();
    final Path jvmPath = temporaryDirectory.resolve("jvm-series.csv");
    Files.writeString(
        jvmPath,
        Files.readString(jvmPath)
            .lines()
            .filter(line -> !line.contains("POST_REGRESSION"))
            .collect(java.util.stream.Collectors.joining("\n", "", "\n")));
    assertThatThrownBy(() -> read(1_000L)).hasMessageContaining("tail missing or stale");
  }

  @Test
  void elapsedRollbackAndSingleSurefireSampleCannotProduceEvidence() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));
    writeDockerEvidence();
    assertThatThrownBy(() -> read(1L)).hasMessageContaining("elapsed order regressed");

    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));
    assertThatThrownBy(() -> read(1L)).hasMessageContaining("SUREFIRE");
  }

  private void writeDockerEvidence() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("docker-series.csv"),
        DOCKER_HEADER
            + docker("t1", 1000, "FULL_REGRESSION", "50", "1000", "")
            + docker("t2", 2000, "FULL_REGRESSION", "50", "1000", "")
            + docker("t3", 3000, "POST_REGRESSION", "50", "1000", ""));
  }

  private void writeValidEvidence() throws IOException {
    Files.writeString(
        temporaryDirectory.resolve("jvm-series.csv"),
        JVM_HEADER
            + jvm("t0", 0, "HARNESS_BASELINE", "MAVEN", "100", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "MAVEN", "160", "")
            + jvm("t1", 1000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "MAVEN", "150", "")
            + jvm("t2", 2000, "FULL_REGRESSION", "SUREFIRE", "200", "")
            + jvm("t3", 3000, "POST_REGRESSION", "MAVEN", "140", ""));
    writeDockerEvidence();
  }

  private Qdr7CapacityResourceEvidence.Snapshot read(final long durationMs) throws IOException {
    return Qdr7CapacityResourceEvidence.read(
        temporaryDirectory, durationMs, new Qdr7CapacityResourceEvidence.Binding(RUN_ID, SHA));
  }

  private static String jvm(
      final String timestamp,
      final long elapsed,
      final String phase,
      final String role,
      final String workingSet,
      final String missingReason) {
    return String.join(
            ",",
            "qdr7-capacity-1",
            RUN_ID,
            SHA,
            "jvm-resource-sampling",
            timestamp,
            Long.toString(elapsed),
            phase,
            role,
            workingSet,
            missingReason)
        + "\n";
  }

  private static String docker(
      final String timestamp,
      final long elapsed,
      final String phase,
      final String memory,
      final String available,
      final String missingReason) {
    return String.join(
            ",",
            "qdr7-capacity-1",
            RUN_ID,
            SHA,
            "docker-host-resource-sampling",
            timestamp,
            Long.toString(elapsed),
            phase,
            memory,
            available,
            missingReason)
        + "\n";
  }
}
