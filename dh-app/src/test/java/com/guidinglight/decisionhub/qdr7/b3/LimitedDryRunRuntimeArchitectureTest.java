package com.guidinglight.decisionhub.qdr7.b3;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** B3 runtime 可达实现的 no-outbound、mock-only、bounded-concurrency 静态边界证据。 */
class LimitedDryRunRuntimeArchitectureTest {

  private static final List<String> FORBIDDEN_RUNTIME_SNIPPETS =
      List.of(
          "Executors.newCachedThreadPool",
          "new LinkedBlockingQueue",
          "CallerRunsPolicy",
          "import org.springframework.web.reactive.function.client.WebClient",
          "import org.springframework.web.client.RestClient",
          "import org.springframework.web.client.RestTemplate",
          "import okhttp3.",
          "import org.apache.http.",
          "import java.net.http.HttpClient");

  @Test
  void dryRunRuntimeUsesBoundedExecutorAndIntroducesNoHttpOrClientFallback() throws IOException {
    final Path root = repositoryRoot();
    final Path dryRun =
        root.resolve(
            "dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision/dryrun");
    final String sources = readJavaSources(dryRun);
    final String wiring =
        Files.readString(
            root.resolve(
                "dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java"));

    for (final String forbidden : FORBIDDEN_RUNTIME_SNIPPETS) {
      assertThat(sources).doesNotContain(forbidden);
      assertThat(wiring).doesNotContain(forbidden);
    }
    assertThat(sources).contains("new ArrayBlockingQueue<>");
    assertThat(sources).contains("new SynchronousQueue<>");
    assertThat(sources).contains("new ThreadPoolExecutor.AbortPolicy()");
    assertThat(sources).contains("NO_SIDE_EFFECT_VIOLATION");
  }

  @Test
  void applicationProviderWiringRemainsDeterministicMockOnly() throws IOException {
    final Path root = repositoryRoot();
    final String pipelineWiring =
        Files.readString(
            root.resolve(
                "dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionPipelineWiringConfig.java"));
    final String runtimeWiring =
        Files.readString(
            root.resolve(
                "dh-app/src/main/java/com/guidinglight/decisionhub/config/DecisionDryRunRuntimeWiringConfig.java"));

    assertThat(pipelineWiring).contains("return new MockModelProvider();");
    assertThat(runtimeWiring)
        .contains("LimitedDryRunRuntimePolicy.MOCK_PROVIDER_KIND")
        .doesNotContain("WebClient", "RestClient", "RestTemplate", "HttpClient");
  }

  private static String readJavaSources(final Path directory) throws IOException {
    final StringBuilder content = new StringBuilder();
    try (var files = Files.list(directory)) {
      for (final Path file : files.filter(path -> path.toString().endsWith(".java")).toList()) {
        content.append(Files.readString(file)).append('\n');
      }
    }
    return content.toString();
  }

  private static Path repositoryRoot() {
    Path current = Path.of("").toAbsolutePath().normalize();
    while (current != null
        && !(Files.exists(current.resolve("pom.xml"))
            && Files.isDirectory(current.resolve("dh-usecase"))
            && Files.isDirectory(current.resolve("dh-app")))) {
      current = current.getParent();
    }
    if (current == null) {
      throw new IllegalStateException("decision-hub repository root not found");
    }
    return current;
  }
}
