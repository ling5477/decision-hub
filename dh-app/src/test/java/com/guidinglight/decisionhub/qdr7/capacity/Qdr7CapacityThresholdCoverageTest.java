package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 冻结 numeric threshold 与 executable consumer 的一一覆盖合同。 */
class Qdr7CapacityThresholdCoverageTest {

  @Test
  void allFrozenNumericLeavesHaveOneExecutableConsumerAndExpectedExecutions() throws IOException {
    final Path root = repositoryRoot();
    final ObjectMapper mapper = new ObjectMapper();
    final JsonNode thresholds =
        mapper.readTree(
            root.resolve("config/qdr7-capacity/qdr7-capacity-thresholds.json").toFile());
    final JsonNode registry =
        mapper.readTree(
            root.resolve("config/qdr7-capacity/qdr7-capacity-threshold-consumers.json").toFile());
    final List<String> declared = new ArrayList<>();
    collectNumericLeaves(thresholds.path("numericThresholds"), "numericThresholds", declared);
    final List<String> consumed = new ArrayList<>();
    int expectedExecutions = 0;
    for (final JsonNode consumer : registry.path("consumers")) {
      consumed.add(consumer.path("thresholdPath").asText());
      expectedExecutions += consumer.path("expectedExecutions").asInt();
      assertThat(consumer.path("scenario").asText()).isNotBlank();
      assertThat(consumer.path("producer").asText()).isNotBlank();
      assertThat(consumer.path("sampler").asText()).isNotBlank();
      assertThat(consumer.path("comparator").asText()).isEqualTo("addThreshold");
      assertThat(consumer.path("artifactField").asText())
          .contains("#")
          .doesNotContain("threshold-comparison.json");
      assertThat(consumer.path("status").asText()).isEqualTo("EXECUTABLE");
    }

    assertThat(declared).hasSize(41).doesNotHaveDuplicates();
    assertThat(consumed).hasSize(41).doesNotHaveDuplicates();
    assertThat(new HashSet<>(consumed)).containsExactlyInAnyOrderElementsOf(declared);
    assertThat(registry.path("expectedDeclaredLeaves").asInt()).isEqualTo(41);
    assertThat(expectedExecutions).isEqualTo(99);
    assertThat(registry.path("expectedComparisonExecutions").asInt()).isEqualTo(99);
  }

  private static void collectNumericLeaves(
      final JsonNode node, final String path, final List<String> leaves) {
    if (node.isNumber()) {
      leaves.add(path);
      return;
    }
    node.fields()
        .forEachRemaining(
            entry -> collectNumericLeaves(entry.getValue(), path + "." + entry.getKey(), leaves));
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
