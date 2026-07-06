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
            List.of(
                    "placeOrder", "cancelOrder", "submitOrder", "executeOrder", "bypassRisk",
                    "forceExecute");

    private static final List<String> FORBIDDEN_APPROVAL_MUTATION_TOKENS =
            List.of(
                    "placeOrder", "cancelOrder", "submitOrder", "executeOrder", "bypassRisk",
                    "forceExecute");

    private static final Pattern FORBIDDEN_API_PATH =
            Pattern.compile("\"\\s*/?(orders|trades|live)(/|\")", Pattern.CASE_INSENSITIVE);

    private static JavaClasses importMainClasses() {
        return new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    /**
     * 旧规则（GateA 起就有的）：domain 不允许直接依赖 infra。Stage1-CLOSE 保持。
     */
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

    /**
     * Stage1-CLOSE ① domain 不允许依赖 usecase / api / infra。
     */
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

    /**
     * Stage1-CLOSE ③ usecase.agent 不允许依赖 dh-providers 包内任何类。
     */
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

    /**
     * Stage1-CLOSE ④ api 控制器 @RequestMapping/@GetMapping/@PostMapping 不能命中 /orders|/trades|/live。
     */
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

    // ============================================================================
    // Stage2-PoC-B5：新增 5 条规则
    // ============================================================================

    /**
     * Stage2-B5 ⑥ connector.tools 不允许依赖 ..infra.. 。
     *
     * <p>tools 端口与 Fake 适配器应保持与 infra 完全解耦；JDBC 实现位于 dh-infra/jdbc，单向依赖 connector。
     */
    @Test
    void stage2B5_rule6_connectorToolsDoesNotDependOnInfra() {
        noClasses()
                .that()
                .resideInAPackage("..connector.tools..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..infra..")
                .check(importMainClasses());
    }

    /**
     * Stage2-B5 ⑦ connector.research 不允许依赖 ..infra.. 。
     *
     * <p>research 端口与 Fake 适配器应保持与 infra 完全解耦；JDBC 实现位于 dh-infra/jdbc，单向依赖 connector。
     */
    @Test
    void stage2B5_rule7_connectorResearchDoesNotDependOnInfra() {
        noClasses()
                .that()
                .resideInAPackage("..connector.research..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..infra..")
                .check(importMainClasses());
    }

    /**
     * Stage2-B5 ⑧ Stage2 新增的 domain 子包不允许依赖 connector。
     *
     * <p>覆盖 domain.forecast / domain.marketdata / domain.reflection / domain.checkpoint。 这些是值对象 +
     * 枚举，不应反向耦合到 adapter 端口。
     */
    @Test
    void stage2B5_rule8_stage2DomainDoesNotDependOnConnector() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "..domain.forecast..",
                        "..domain.marketdata..",
                        "..domain.reflection..",
                        "..domain.checkpoint..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..connector..")
                .check(importMainClasses());
    }

    /**
     * Stage2-B5 ⑨ usecase.agent.planner 不允许依赖 dh-providers。
     *
     * <p>动态 Planner 内部不得通过 provider 调 LLM；保持纯编排。
     */
    @Test
    void stage2B5_rule9_useCaseAgentPlannerDoesNotDependOnProviders() {
        noClasses()
                .that()
                .resideInAPackage("..usecase.agent.planner..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.guidinglight.decisionhub.providers..")
                .check(importMainClasses());
    }

    /**
     * Stage2-B5 ⑩ usecase.agent.feedback 不允许依赖 dh-providers。
     *
     * <p>NQ feedback ingestion 必须保持纯本地编排，不允许通过 provider 触达外部 LLM。
     */
    @Test
    void stage2B5_rule10_useCaseAgentFeedbackDoesNotDependOnProviders() {
        noClasses()
                .that()
                .resideInAPackage("..usecase.agent.feedback..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.guidinglight.decisionhub.providers..")
                .check(importMainClasses());
    }

    // ============================================================================
    // Stage3-B3：新增 2 条规则
    // ============================================================================

    /**
     * Stage3-B3 ⑪ 只有 connector.nq 与 dh-app config 允许直接引用 HTTP 客户端。
     *
     * <p>本规则收口"DH 不接真实 HTTP"硬边界（参见 STAGE3_DH_BACKTEST_ADAPTER_SPEC §3.5 / §7.3）：
     * 业务模块禁止直接使用 RestTemplate / WebClient / OkHttp / HttpURLConnection。
     * 当前未引入真实 HTTP 依赖；本规则确保未来不会被静默引入。
     */
    @Test
    void stage3B3_rule11_httpClientOnlyInsideConnectorNqOrAppConfig() {
        noClasses()
                .that()
                .resideOutsideOfPackages("..connector.nq..", "..config..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.web.client.RestTemplate")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("org.springframework.web.reactive.function.client.WebClient")
                .orShould()
                .dependOnClassesThat()
                .resideInAPackage("okhttp3..")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("java.net.HttpURLConnection")
                .check(importMainClasses());
    }

    /**
     * Stage3-B3 ⑫ usecase.agent.backtest 不允许直接依赖 RealNqBacktestClient。
     *
     * <p>本规则收口"通过端口依赖、不依赖具体实现"原则（参见 SPEC §3.2）。
     * 因 Stage3-B3 本轮未实现 RealNqBacktestClient 类，本规则当前为"占位 + 防御"：
     * 即使未来引入 RealNqBacktestClient，业务层仍必须通过 NqBacktestClient 端口依赖。
     *
     * <p>本规则同时禁止 usecase.agent.backtest 反向依赖 dh-providers。
     */
    @Test
    void stage3B3_rule12_useCaseBacktestDoesNotDependOnRealClientOrProviders() {
        noClasses()
                .that()
                .resideInAPackage("..usecase.agent.backtest..")
                .should()
                .dependOnClassesThat()
                .haveSimpleName("RealNqBacktestClient")
                .orShould()
                .dependOnClassesThat()
                .resideInAPackage("com.guidinglight.decisionhub.providers..")
                .check(importMainClasses());
    }

    // ============================================================================
    // stage-qdr-1：Decision Core baseline 边界
    // ============================================================================

    /**
     * stage-qdr-1：QDR domain 不允许依赖 api / infra / security。
     */
    @Test
    void stageQdr1_rule13_qdrDomainIsolatedFromApiInfraSecurity() {
        noClasses()
                .that()
                .resideInAPackage("..domain.qdr..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..api..", "..infra..", "..security..")
                .check(importMainClasses());
    }

    /**
     * stage-qdr-1：usecase 不允许直接依赖真实 provider SDK。
     */
    @Test
    void stageQdr1_rule14_usecaseDoesNotDependOnRealProviderSdk() {
        noClasses()
                .that()
                .resideInAPackage("..usecase..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.openai..",
                        "com.anthropic..",
                        "com.google.genai..",
                        "dev.langchain4j..",
                        "org.springframework.ai..")
                .check(importMainClasses());
    }

    // ============================================================================
    // stage-qdr-2 B1：Audit Trace Read Model contract 边界
    // ============================================================================

    /**
     * stage-qdr-2 B1：QDR readmodel 只允许作为 usecase-level DTO / query contract。
     *
     * <p>readmodel 包不得依赖 API、infra、Spring Web、JDBC/JPA、真实 provider SDK 或 Agent runtime。
     * B1 不实现 Controller、JDBC repository、external HTTP、LangGraph / AutoGen / CrewAI。
     */
    @Test
    void stageQdr2B1_rule15_qdrReadmodelDoesNotDependOnApiInfraWebJdbcProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..usecase.qdr.readmodel..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..",
                        "..infra..",
                        "org.springframework.web..",
                        "org.springframework.jdbc..",
                        "java.sql..",
                        "javax.sql..",
                        "jakarta.persistence..",
                        "javax.persistence..",
                        "org.hibernate..",
                        "com.openai..",
                        "com.anthropic..",
                        "com.google.genai..",
                        "dev.langchain4j..",
                        "org.springframework.ai..",
                        "langgraph..",
                        "autogen..",
                        "crewai..")
                .check(importMainClasses());
    }

    /**
     * stage-qdr-2 B2：QDR JDBC read adapter 只能是 DH-owned DB 只读 adapter。
     *
     * <p>infra.jdbc.qdr 允许依赖 Spring JDBC，但不得反向依赖 API/Spring Web、HTTP client、真实
     * provider SDK 或 Agent runtime。
     */
    @Test
    void stageQdr2B2_rule16_qdrJdbcReadAdapterDoesNotDependOnApiWebHttpProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..infra.jdbc.qdr..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..",
                        "org.springframework.web..",
                        "org.springframework.web.reactive..",
                        "okhttp3..",
                        "com.openai..",
                        "com.anthropic..",
                        "com.google.genai..",
                        "dev.langchain4j..",
                        "org.springframework.ai..",
                        "langgraph..",
                        "autogen..",
                        "crewai..")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("java.net.http.HttpClient")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("java.net.HttpURLConnection")
                .check(importMainClasses());
    }

    /**
     * stage-qdr-2 B2：API read controller 只能依赖 usecase contract，不得直连 repository/JDBC。
     */
    @Test
    void stageQdr2B2_rule17_qdrReadApiDoesNotDependOnInfraJdbcProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..api.decision..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..infra..",
                        "org.springframework.jdbc..",
                        "java.sql..",
                        "javax.sql..",
                        "com.openai..",
                        "com.anthropic..",
                        "com.google.genai..",
                        "dev.langchain4j..",
                        "org.springframework.ai..",
                        "langgraph..",
                        "autogen..",
                        "crewai..")
                .check(importMainClasses());
    }

    /**
     * stage-qdr-2 B3：approval domain 只允许依赖 domain / JDK，不允许依赖 API、infra、security、
     * Spring、JDBC/JPA、provider SDK 或 Agent runtime。
     */
    @Test
    void stageQdr2B3_rule18_approvalDomainDoesNotDependOnApiInfraSecuritySpringJdbcProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..domain.qdr.approval..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..",
                        "..infra..",
                        "..security..",
                        "..usecase..",
                        "org.springframework..",
                        "org.springframework.jdbc..",
                        "java.sql..",
                        "javax.sql..",
                        "jakarta.persistence..",
                        "javax.persistence..",
                        "org.hibernate..",
                        "com.openai..",
                        "com.anthropic..",
                        "com.google.genai..",
                        "dev.langchain4j..",
                        "org.springframework.ai..",
                        "langgraph..",
                        "autogen..",
                        "crewai..")
                .check(importMainClasses());
    }

    /**
     * stage-qdr-2 B3：approval usecase 只能依赖 domain contract，不允许依赖 API、infra、Spring
     * Web、JDBC/JPA、provider SDK 或 Agent runtime。
     */
    @Test
    void stageQdr2B3_rule19_approvalUsecaseDoesNotDependOnApiInfraWebJdbcProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..usecase.qdr.approval..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..",
                        "..infra..",
                        "org.springframework.web..",
                        "org.springframework.web.reactive..",
                        "org.springframework.jdbc..",
                        "java.sql..",
                        "javax.sql..",
                        "jakarta.persistence..",
                        "javax.persistence..",
                        "org.hibernate..",
                        "com.openai..",
                        "com.anthropic..",
                        "com.google.genai..",
                        "dev.langchain4j..",
                        "org.springframework.ai..",
                        "langgraph..",
                        "autogen..",
                        "crewai..")
                .check(importMainClasses());
    }

    /**
     * stage-qdr-2 B3：approval infra 只能作为 DH-owned JDBC adapter，不允许依赖 API/Web、
     * HTTP client、provider SDK 或 Agent runtime。
     */
    @Test
    void stageQdr2B3_rule20_approvalInfraDoesNotDependOnApiWebHttpProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..infra.jdbc.qdr..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..",
                        "org.springframework.web..",
                        "org.springframework.web.reactive..",
                        "okhttp3..",
                        "com.openai..",
                        "com.anthropic..",
                        "com.google.genai..",
                        "dev.langchain4j..",
                        "org.springframework.ai..",
                        "langgraph..",
                        "autogen..",
                        "crewai..")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("java.net.http.HttpClient")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("java.net.HttpURLConnection")
                .check(importMainClasses());
    }

    /**
     * stage-qdr-2 B3：approval 相关代码不允许出现订单执行、风控旁路或强制执行 token。
     */
    @Test
    void stageQdr2B3_rule21_approvalSourcesForbidOrderAndBypassTokens() {
        final List<Path> approvalPaths =
                List.of(
                        Path.of(
                                        "..",
                                        "dh-domain",
                                        "src",
                                        "main",
                                        "java",
                                        "com",
                                        "guidinglight",
                                        "decisionhub",
                                        "domain",
                                        "qdr",
                                        "approval")
                                .toAbsolutePath()
                                .normalize(),
                        Path.of(
                                        "..",
                                        "dh-usecase",
                                        "src",
                                        "main",
                                        "java",
                                        "com",
                                        "guidinglight",
                                        "decisionhub",
                                        "usecase",
                                        "qdr",
                                        "approval")
                                .toAbsolutePath()
                                .normalize(),
                        Path.of(
                                        "..",
                                        "dh-infra",
                                        "src",
                                        "main",
                                        "java",
                                        "com",
                                        "guidinglight",
                                        "decisionhub",
                                        "infra",
                                        "jdbc",
                                        "qdr",
                                        "JdbcHumanApprovalPacketRepository.java")
                                .toAbsolutePath()
                                .normalize());
        final List<String> violations = new ArrayList<>();
        for (Path approvalPath : approvalPaths) {
            collectForbiddenTokenViolations(
                    approvalPath, FORBIDDEN_APPROVAL_MUTATION_TOKENS, violations);
        }
        if (!violations.isEmpty()) {
            fail(
                    "stage-qdr-2 B3 approval source files must not contain order/risk mutation tokens:\n"
                            + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-2 B3：approval 相关代码不允许依赖 NQ client 或 connector.nq。
     */
    @Test
    void stageQdr2B3_rule22_approvalDoesNotDependOnNqClient() {
        noClasses()
                .that()
                .resideInAnyPackage(
                        "..domain.qdr.approval..", "..usecase.qdr.approval..", "..infra.jdbc.qdr..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("..connector.nq..")
                .orShould()
                .dependOnClassesThat()
                .haveSimpleName("NqBacktestClient")
                .orShould()
                .dependOnClassesThat()
                .haveSimpleName("RealNqBacktestClient")
                .check(importMainClasses());
    }

    private static void collectForbiddenTokenViolations(
            final Path rootOrFile, final List<String> tokens, final List<String> violations) {
        if (!Files.exists(rootOrFile)) {
            violations.add("missing approval source path: " + rootOrFile);
            return;
        }
        try (Stream<Path> walker =
                Files.isDirectory(rootOrFile) ? Files.walk(rootOrFile) : Stream.of(rootOrFile)) {
            walker
                    .filter(p -> p.toString().endsWith(".java"))
                    .forEach(
                            p -> {
                                try {
                                    final String body = Files.readString(p, StandardCharsets.UTF_8);
                                    for (String token : tokens) {
                                        if (body.contains(token)) {
                                            violations.add(p + " contains forbidden token: " + token);
                                        }
                                    }
                                } catch (IOException io) {
                                    violations.add("failed to read " + p + ": " + io.getMessage());
                                }
                            });
        } catch (IOException io) {
            violations.add("failed to walk " + rootOrFile + ": " + io.getMessage());
        }
    }
}
