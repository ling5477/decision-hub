package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Qdr7CapacityProfileIsolationTest {

  @Test
  void profileIsExplicitAndFailsafeDiscoversOnlyFormalCapacityIt() throws IOException {
    final Path root = repositoryRoot();
    final String rootPom = Files.readString(root.resolve("pom.xml"));
    final String appPom = Files.readString(root.resolve("dh-app/pom.xml"));

    assertThat(rootPom).contains("<id>qdr7-capacity-acceptance</id>");
    assertThat(rootPom).doesNotContain("<activeByDefault>true</activeByDefault>");
    assertThat(appPom)
        .contains("<id>qdr7-capacity-acceptance</id>")
        .contains("<exclude>**/*IT.java</exclude>")
        .contains("**/Qdr7CapacityAcceptanceIT.java")
        .contains("<goal>integration-test</goal>")
        .contains("<goal>verify</goal>")
        .contains("Invoke-Qdr7CapacityAcceptance.ps1");
  }

  @Test
  void scriptBindingsPassRunIdSeedAndUsePreAndPostIntegrationPhases() throws IOException {
    final String appPom = Files.readString(repositoryRoot().resolve("dh-app/pom.xml"));

    assertThat(appPom)
        .contains("<phase>pre-integration-test</phase>")
        .contains("<phase>post-integration-test</phase>")
        .contains("<argument>${qdr7.runId}</argument>")
        .contains("<argument>${qdr7.seed}</argument>");
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
