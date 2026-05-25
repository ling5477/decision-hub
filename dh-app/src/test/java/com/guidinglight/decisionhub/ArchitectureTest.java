package com.guidinglight.decisionhub;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.junit.jupiter.api.Assertions.fail;

import com.guidinglight.decisionhub.connector.nq.fake.DefaultNqContractVerifier;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Stage1-CLOSE：在保留原有 domain→infra 约束之上新增 4 条架构规则，兜底新 Agent runtime 边界。
 *
 * <p>规则编号对应工单：① domain 不依赖 usecase/api/infra；② connector.nq 禁止出现 placeOrder /
 * submitOrder / executeOrder / bypassRisk / forceExecute；③ usecase.agent 不依赖 dh-providers；
 * ④ api 控制器 @RequestMapping 不命中 /orders|/trades|/live。
 */
public class ArchitectureTest {

  private static final String BASE_PACKAGE = "com.guidinglight.decisionhub";

  private static final List<String> FORBIDDEN_NQ_TOKENS =
      List.of("placeOrder", "submitOrder", "executeOrder", "bypassRisk", "forceExecute");

  private static final Pattern FORBIDDEN_API_PATH =
      Pattern.compile("\"\\s*/?(orders|trades|live)(/|\")", Pattern.CASE_INSENSITIVE);

  private static JavaClasses importMainClasses() {
    return new ClassFileImporter()
        .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
        .importPackages(BASE_PACKAGE);
  }

  /** 旧规则（GateA 起就有的）：domain 不允许直接依赖 infra。Stage1-CLOSE 保持。 */
  @Test
  void domain_should_not_depend_on_infra() {
    noClasses()
        .that()
        .resideInAPackage("..domain..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..infra..")
        .check(importMainClasses());
  }

  /** Stage1-CLOSE ① domain 不允许依赖 usecase / api / infra。 */
  @Test
  void stage1Close_rule1_domainIsolatedFromUseCaseApiInfra() {
    final ArchRule rule =
        noClasses()
            .that()
            .resideInAPackage("..domain..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("..usecase..", "..api..", "..infra..");
    rule.check(importMainClasses());
  }

  /**
   * Stage1-CLOSE ② connector.nq 包内（除 {@link DefaultNqContractVerifier} 外）不允许出现
   * placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute 字样。
   *
   * <p>用源文件扫描覆盖类名、方法名、字段名、字符串字面量、注释中的引用。
   * 唯一豁免：{@code DefaultNqContractVerifier.java}，它本身要把禁字作为黑名单。
   */
  @Test
  void stage1Close_rule2_connectorNqForbidsOrderAndBypassTokens() {
    // 静态触达，保证未来如果 DefaultNqContractVerifier 改名也能让编译期暴露问题。
    final Class<?> verifierClass = DefaultNqContractVerifier.class;
    final String verifierFileName = verifierClass.getSimpleName() + ".java";

    final Path connectorRoot =
        Path.of("..", "dh-connector", "src", "main", "java").toAbsolutePath().normalize();
    final List<String> violations = new ArrayList<>();
    try (Stream<Path> walker = Files.walk(connectorRoot)) {
      walker
          .filter(p -> p.toString().endsWith(".java"))
          .filter(p -> !p.getFileName().toString().equals(verifierFileName))
          .forEach(
              p -> {
                try {
                  final String body = Files.readString(p, StandardCharsets.UTF_8);
                  for (String token : FORBIDDEN_NQ_TOKENS) {
                    if (body.contains(token)) {
                      violations.add(p + " contains forbidden token: " + token);
                    }
                  }
                } catch (IOException io) {
                  violations.add("failed to read " + p + ": " + io.getMessage());
                }
              });
    } catch (IOException io) {
      fail("failed to walk dh-connector sources: " + io.getMessage());
    }

    if (!violations.isEmpty()) {
      fail(
          "Stage1-CLOSE rule② violations in dh-connector source files:\n"
              + String.join("\n", violations));
    }
  }

  /** Stage1-CLOSE ③ usecase.agent 不允许依赖 dh-providers 包内任何类。 */
  @Test
  void stage1Close_rule3_useCaseAgentDoesNotDependOnProviders() {
    noClasses()
        .that()
        .resideInAPackage("..usecase.agent..")
        .should()
        .dependOnClassesThat()
        .resideInAPackage("com.guidinglight.decisionhub.providers..")
        .check(importMainClasses());
  }

  /** Stage1-CLOSE ④ api 控制器 @RequestMapping/@GetMapping/@PostMapping 不能命中 /orders|/trades|/live。 */
  @Test
  void stage1Close_rule4_apiControllersForbidOrderTradeLivePaths() {
    final Path apiRoot =
        Path.of("..", "dh-api", "src", "main", "java").toAbsolutePath().normalize();
    final Pattern mappingPattern =
        Pattern.compile(
            "@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)\\s*\\(([^)]*)\\)",
            Pattern.DOTALL);

    final List<String> violations = new ArrayList<>();
    try (Stream<Path> walker = Files.walk(apiRoot)) {
      walker
          .filter(p -> p.toString().endsWith(".java"))
          .forEach(
              p -> {
                try {
                  final String body = Files.readString(p, StandardCharsets.UTF_8);
                  final Matcher annotation = mappingPattern.matcher(body);
                  while (annotation.find()) {
                    final String args = annotation.group(2);
                    final Matcher pathMatch = FORBIDDEN_API_PATH.matcher(args);
                    if (pathMatch.find()) {
                      violations.add(
                          p
                              + " has forbidden mapping path token '"
                              + pathMatch.group(1)
                              + "' in "
                              + annotation.group(0));
                    }
                  }
                } catch (IOException io) {
                  violations.add("failed to read " + p + ": " + io.getMessage());
                }
              });
    } catch (IOException io) {
      fail("failed to walk dh-api sources: " + io.getMessage());
    }

    if (!violations.isEmpty()) {
      fail(
          "Stage1-CLOSE rule④ violations: api controllers must not map /orders|/trades|/live:\n"
              + String.join("\n", violations));
    }
  }
}
