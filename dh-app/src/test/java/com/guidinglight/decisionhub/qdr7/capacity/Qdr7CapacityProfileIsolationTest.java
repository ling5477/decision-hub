package com.guidinglight.decisionhub.qdr7.capacity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Qdr7CapacityProfileIsolationTest {

  @Test
  void profileIsExplicitAndFailsafeDiscoversOnlyFormalCapacityIt() throws IOException {
    final Path root = repositoryRoot();
    final String rootPom = Files.readString(root.resolve("pom.xml"));
    final String bomPom = Files.readString(root.resolve("dh-bom/pom.xml"));
    final String appPom = Files.readString(root.resolve("dh-app/pom.xml"));

    assertThat(rootPom).contains("<id>qdr7-capacity-acceptance</id>");
    assertThat(rootPom)
        .contains("<qdr7.powershell.executable>pwsh</qdr7.powershell.executable>")
        .contains("<id>qdr7-capacity-windows-powershell</id>")
        .contains("<family>Windows</family>")
        .contains("<qdr7.seed>7</qdr7.seed>")
        .contains("<qdr7.qualificationOnly>false</qdr7.qualificationOnly>");
    assertThat(rootPom).doesNotContain("<activeByDefault>true</activeByDefault>");
    assertThat(bomPom)
        .contains("<qdr7.capacity.outerSurefire.skip>false</qdr7.capacity.outerSurefire.skip>")
        .contains("<id>qdr7-capacity-acceptance</id>")
        .contains("<qdr7.capacity.outerSurefire.skip>true</qdr7.capacity.outerSurefire.skip>")
        .contains("<skipTests>${qdr7.capacity.outerSurefire.skip}</skipTests>");
    assertThat(appPom)
        .contains("<id>qdr7-capacity-acceptance</id>")
        .contains("<exclude>**/*IT.java</exclude>")
        .contains("外层普通Surefire隔离由dh-bom同名profile统一应用到全部子模块")
        .contains("**/Qdr7CapacityAcceptanceIT.java")
        .contains("<goal>integration-test</goal>")
        .contains("<goal>verify</goal>")
        .contains("<qdr7.qualificationOnly>${qdr7.qualificationOnly}</qdr7.qualificationOnly>")
        .contains("Invoke-Qdr7CapacityAcceptance.ps1")
        .contains("<executable>${qdr7.powershell.executable}</executable>")
        .doesNotContain("<executable>powershell.exe</executable>", "<skipTests>true</skipTests>");
    assertThat(rootPom + bomPom + appPom).doesNotContain("<maven.test.skip>");
  }

  @Test
  void profileIsolationPreservesInternalRegressionQualityAndFrozenCriteria() throws IOException {
    final Path root = repositoryRoot();
    final String formalHarness =
        Files.readString(
            root.resolve(
                "dh-app/src/test/java/com/guidinglight/decisionhub/qdr7/capacity/Qdr7CapacityAcceptanceIT.java"));
    final Qdr7CapacityContracts.CriteriaSnapshot criteria =
        Qdr7CapacityContracts.loadCriteria(root, new ObjectMapper());

    assertThat(formalHarness)
        .contains("mavenCommand(List.of(\"-o\", \"-B\", \"-ntp\", \"test\"))")
        .contains("mavenCommand(List.of(\"-o\", \"-B\", \"-ntp\", \"-Pquality\"")
        .contains("new HarnessDriver(\"full-regression\", this::fullRegression)")
        .contains("new HarnessDriver(\"quality-gate\", this::qualityGate)");
    assertThat(criteria.root().path("scenarioRegistry")).hasSize(15);
    assertThat(criteria.sourceDocumentSha256())
        .isEqualTo(Qdr7CapacityContracts.sha256(criteria.sourceDocument()));
  }

  @Test
  void scriptBindingsPassRunIdSeedAndUsePreAndPostIntegrationPhases() throws IOException {
    final String appPom = Files.readString(repositoryRoot().resolve("dh-app/pom.xml"));

    assertThat(appPom)
        .contains("<phase>pre-integration-test</phase>")
        .contains("<phase>post-integration-test</phase>")
        .contains("<argument>${qdr7.runId}</argument>")
        .contains("<argument>${qdr7.seed}</argument>")
        .contains("<argument>-PowerShellExecutable</argument>")
        .contains("<argument>${qdr7.powershell.executable}</argument>")
        .contains("<argument>-QualificationOnly</argument>")
        .contains("<argument>${qdr7.qualificationOnly}</argument>");

    assertThat(appPom.indexOf("<argument>-RunId</argument>"))
        .isLessThan(appPom.indexOf("<argument>${qdr7.runId}</argument>"));
    assertThat(appPom.indexOf("<argument>-Seed</argument>"))
        .isLessThan(appPom.indexOf("<argument>${qdr7.seed}</argument>"));
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
