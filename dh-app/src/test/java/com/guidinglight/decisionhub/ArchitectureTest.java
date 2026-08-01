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

    private static final List<String> FORBIDDEN_QDR_MODEL_MUTATION_TOKENS =
            List.of(
                    "placeOrder", "cancelOrder", "submitOrder", "executeOrder", "bypassRisk",
                    "forceExecute");

    private static final List<String> FORBIDDEN_QDR_GATEWAY_MUTATION_TOKENS =
            List.of(
                    "placeOrder", "cancelOrder", "submitOrder", "executeOrder", "bypassRisk",
                    "forceExecute", "mutateLedger", "mutateRisk", "paperRunStart", "liveRunStart");

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

    /** Feedback ingestion usecase 保持 framework-neutral，事务能力只能由 adapter 实现。 */
    @Test
    void feedbackIngestAtomicity_usecaseDoesNotDependOnSpringTransaction() {
        noClasses()
                .that()
                .resideInAPackage("..usecase.agent.feedback..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage("org.springframework.transaction..", "org.springframework.jdbc..")
                .check(importMainClasses());
    }

    /** 原子 ingress 禁止异步、REQUIRES_NEW、动态 bean lookup 或 transaction callback 补写。 */
    @Test
    void feedbackIngestAtomicity_forbidsBypassAndSplitTransactionTokens() {
        final List<Path> sources =
                List.of(
                        Path.of("..", "dh-usecase", "src", "main", "java", "com", "guidinglight",
                                "decisionhub", "usecase", "agent", "feedback"),
                        Path.of("..", "dh-infra", "src", "main", "java", "com", "guidinglight",
                                "decisionhub", "infra", "jdbc", "JdbcNqFeedbackEventRepository.java"),
                        Path.of("src", "main", "java", "com", "guidinglight", "decisionhub", "config",
                                "FeedbackIngestionWiringConfig.java"),
                        Path.of("src", "main", "java", "com", "guidinglight", "decisionhub", "config",
                                "Stage2JdbcWiringConfig.java"));
        final List<String> forbidden =
                List.of(
                        "REQUIRES_NEW",
                        "@Async",
                        "ApplicationEventPublisher",
                        "TransactionSynchronization",
                        "ApplicationContext#getBean",
                        "ObjectProvider",
                        "CompletableFuture",
                        "new Thread(");
        final List<String> violations = new ArrayList<>();
        for (Path source : sources) {
            final Path absolute = source.toAbsolutePath().normalize();
            if (!Files.exists(absolute)) {
                violations.add("missing feedback atomicity source: " + absolute);
                continue;
            }
            try (Stream<Path> walker =
                         Files.isDirectory(absolute) ? Files.walk(absolute) : Stream.of(absolute)) {
                walker.filter(path -> path.toString().endsWith(".java"))
                        .forEach(
                                path -> {
                                    try {
                                        final String body = Files.readString(path, StandardCharsets.UTF_8);
                                        forbidden.stream()
                                                .filter(body::contains)
                                                .forEach(token -> violations.add(path + " contains " + token));
                                    } catch (IOException error) {
                                        violations.add("failed to read " + path + ": " + error.getMessage());
                                    }
                                });
            } catch (IOException error) {
                violations.add("failed to scan " + absolute + ": " + error.getMessage());
            }
        }
        if (!violations.isEmpty()) {
            fail("feedback ingest atomicity bypass violations:\n" + String.join("\n", violations));
        }
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
     * stage-qdr-1：QDR domain 不允许依赖 api / infra / DH security module。
     */
    @Test
    void stageQdr1_rule13_qdrDomainIsolatedFromApiInfraSecurity() {
        noClasses()
                .that()
                .resideInAPackage("..domain.qdr..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..", "..infra..", "com.guidinglight.decisionhub.security..")
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
                                .normalize(),
                        Path.of(
                                        "..",
                                        "dh-api",
                                        "src",
                                        "main",
                                        "java",
                                        "com",
                                        "guidinglight",
                                        "decisionhub",
                                        "api",
                                        "decision",
                                        "HumanApprovalPacketController.java")
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

    /**
     * stage-qdr-2 B4：approval API 路径必须受 DhApiAuthenticationFilter 保护。
     *
     * <p>该静态守卫防止新增 decision-runs approval 子路径或 approval-packets 子路径后遗漏认证
     * filter，导致匿名 approval create/query/decision
     * endpoint。运行态 401/403 仍由 WebMvc tests 覆盖。
     */
    @Test
    void stageQdr2B4_rule23_approvalApiIsInsideAuthenticationFilter() {
        final Path filterPath =
                Path.of(
                                "..",
                                "dh-api",
                                "src",
                                "main",
                                "java",
                                "com",
                                "guidinglight",
                                "decisionhub",
                                "api",
                                "security",
                                "DhApiAuthenticationFilter.java")
                        .toAbsolutePath()
                        .normalize();
        try {
            final String body = Files.readString(filterPath, StandardCharsets.UTF_8);
            if (!body.contains("APPROVAL_PACKET_PATH")
                    || !body.contains("DECISION_RUN_READ_PATH")
                    || !body.contains("\"/api/ai/approval-packets\"")
                    || !body.contains("\"/api/ai/decision-runs\"")
                    || !body.contains("path.startsWith(DECISION_RUN_READ_PATH + \"/\")")
                    || !body.contains("path.startsWith(APPROVAL_PACKET_PATH + \"/\")")) {
                fail("approval packet API path must be protected by DhApiAuthenticationFilter");
            }
        } catch (IOException io) {
            fail("failed to read DhApiAuthenticationFilter.java: " + io.getMessage());
        }
    }

    /**
     * stage-qdr-2 B4：approval API / usecase 不允许依赖 NQ client。
     */
    @Test
    void stageQdr2B4_rule24_approvalApiAndUsecaseDoNotDependOnNqClient() {
        noClasses()
                .that()
                .resideInAnyPackage("..api.decision..", "..usecase.qdr.approval..")
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

    /**
     * stage-qdr-3 B1：qdr model domain 只允许依赖 domain/JDK，不允许依赖 API、infra、Spring、
     * JDBC/JPA、provider SDK、HTTP client 或 Agent runtime。
     */
    @Test
    void stageQdr3B1_rule25_qdrModelDomainDoesNotDependOnApiInfraWebJdbcProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..domain.qdr.model..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..",
                        "..infra..",
                        "..usecase..",
                        "org.springframework..",
                        "org.springframework.web..",
                        "org.springframework.web.reactive..",
                        "org.springframework.jdbc..",
                        "java.sql..",
                        "javax.sql..",
                        "jakarta.persistence..",
                        "javax.persistence..",
                        "org.hibernate..",
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
     * stage-qdr-3 B1：qdr model usecase 只能依赖 domain/usecase/JDK，不允许依赖 API、infra、
     * Spring Web、JDBC/JPA、provider SDK、HTTP client 或 Agent runtime。
     */
    @Test
    void stageQdr3B1_rule26_qdrModelUsecaseDoesNotDependOnApiInfraWebJdbcProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..usecase.qdr.model..")
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
     * stage-qdr-3 B1：qdr model production sources 不允许出现订单执行、风控旁路或强制执行 token。
     *
     * <p>PromptInjectionGuard 可以把大写风险词作为 denylist/enum constraint；这里专门拦截会被误用为
     * Java 方法或执行 hook 的 camelCase mutation token。
     */
    @Test
    void stageQdr3B1_rule27_qdrModelSourcesForbidOrderAndBypassMutationTokens() {
        final List<Path> qdrModelPaths =
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
                                        "model")
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
                                        "model")
                                .toAbsolutePath()
                                .normalize());
        final List<String> violations = new ArrayList<>();
        for (Path qdrModelPath : qdrModelPaths) {
            collectForbiddenTokenViolations(
                    qdrModelPath, FORBIDDEN_QDR_MODEL_MUTATION_TOKENS, violations);
        }
        if (!violations.isEmpty()) {
            fail(
                    "stage-qdr-3 B1 qdr model source files must not contain mutation tokens:\n"
                            + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B1：不得新增 Prompt / Model Gateway API endpoint。
     *
     * <p>V8 migration 已由 B3 独立守卫接管；B1 guard 继续阻断 API 提前扩散。
     */
    @Test
    void stageQdr3B1_rule28_noPromptModelApiEndpointOrMigrationArtifact() {
        final List<String> violations = new ArrayList<>();
        collectPatternViolations(
                Path.of("..", "dh-api", "src", "main", "java").toAbsolutePath().normalize(),
                Pattern.compile(
                        "@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)"
                                + "[\\s\\S]{0,240}(prompt|model-gateway|provider-profile)",
                        Pattern.CASE_INSENSITIVE),
                "B1 must not add prompt/model API endpoint",
                violations);
        if (!violations.isEmpty()) {
            fail("stage-qdr-3 B1 API/migration boundary violations:\n" + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B2：qdr gateway usecase 只能依赖 domain/usecase/JDK，不允许依赖 API、infra、
     * Spring Web、JDBC/JPA、provider SDK、HTTP client 或 Agent runtime。
     */
    @Test
    void stageQdr3B2_rule29_qdrGatewayUsecaseDoesNotDependOnApiInfraWebJdbcProviderOrAgentRuntime() {
        noClasses()
                .that()
                .resideInAPackage("..usecase.qdr.gateway..")
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
     * stage-qdr-3 B2：gateway production source 不允许出现真实 provider SDK、HTTP outbound setup、
     * Agent framework 或交易 mutation token。
     */
    @Test
    void stageQdr3B2_rule30_qdrGatewaySourcesForbidProviderSdkHttpAgentAndMutationTokens() {
        final Path gatewayRoot =
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
                                "gateway")
                        .toAbsolutePath()
                        .normalize();
        final List<String> violations = new ArrayList<>();
        collectForbiddenTokenViolations(
                gatewayRoot, FORBIDDEN_QDR_GATEWAY_MUTATION_TOKENS, violations);
        collectPatternViolations(
                gatewayRoot,
                Pattern.compile(
                        "import\\s+.*(openai|anthropic|genai|ollama|langgraph|autogen|crewai|"
                                + "WebClient|RestTemplate|OkHttp|HttpClient)",
                        Pattern.CASE_INSENSITIVE),
                "B2 gateway must not import provider SDK, HTTP client, or Agent framework",
                violations);
        collectPatternViolations(
                gatewayRoot,
                Pattern.compile(
                        "(WebClient\\.builder|new\\s+RestTemplate|OkHttpClient|"
                                + "HttpClient\\.new|URI\\.create\\(|URL\\()",
                        Pattern.CASE_INSENSITIVE),
                "B2 gateway must not create outbound HTTP setup",
                violations);
        if (!violations.isEmpty()) {
            fail(
                    "stage-qdr-3 B2 gateway source boundary violations:\n"
                            + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B2：不得新增 Prompt / Model Gateway API endpoint。
     *
     * <p>V8 migration 已由 B3 独立守卫接管；B2 guard 继续阻断 API 提前扩散。
     */
    @Test
    void stageQdr3B2_rule31_noGatewayApiEndpointOrMigrationArtifact() {
        final List<String> violations = new ArrayList<>();
        collectPatternViolations(
                Path.of("..", "dh-api", "src", "main", "java").toAbsolutePath().normalize(),
                Pattern.compile(
                        "@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)"
                                + "[\\s\\S]{0,240}(prompt|model-gateway|provider-profile|model-version)",
                        Pattern.CASE_INSENSITIVE),
                "B2 must not add prompt/model gateway API endpoint",
                violations);
        if (!violations.isEmpty()) {
            fail("stage-qdr-3 B2 API/migration boundary violations:\n" + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B2：QDR business code 不得绕过 ModelGatewayPort 直连 provider。
     */
    @Test
    void stageQdr3B2_rule32_qdrBusinessCodeDoesNotBypassModelGatewayPort() {
        final Path qdrRoot =
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
                                "qdr")
                        .toAbsolutePath()
                        .normalize();
        final Path gatewayRoot = qdrRoot.resolve("gateway");
        final List<String> violations = new ArrayList<>();
        collectGatewayBypassViolations(qdrRoot, gatewayRoot, violations);
        if (!violations.isEmpty()) {
            fail(
                    "stage-qdr-3 B2 qdr business code must call through ModelGatewayPort:\n"
                            + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B3：QDR model JDBC repository 不允许依赖 dh-api、provider SDK、HTTP client、Agent
     * framework 或 NQ client。
     */
    @Test
    void stageQdr3B3_rule33_qdrModelJdbcRepositoryDoesNotDependOnApiProviderHttpAgentOrNqClient() {
        noClasses()
                .that()
                .resideInAPackage("..infra.jdbc.qdr.model..")
                .should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "..api..",
                        "..connector.nq..",
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
                .orShould()
                .dependOnClassesThat()
                .haveSimpleName("NqBacktestClient")
                .orShould()
                .dependOnClassesThat()
                .haveSimpleName("RealNqBacktestClient")
                .check(importMainClasses());
    }

    /**
     * stage-qdr-3 B3：QDR model persistence production source 不得出现 provider SDK、HTTP outbound、
     * Agent framework、raw storage 字段或交易 mutation token。
     */
    @Test
    void stageQdr3B3_rule34_qdrModelPersistenceSourcesForbidProviderHttpAgentRawStorageAndMutation() {
        final List<Path> roots =
                List.of(
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
                                        "model")
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
                                        "gateway")
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
                                        "model")
                                .toAbsolutePath()
                                .normalize(),
                        Path.of(
                                        "src",
                                        "main",
                                        "resources",
                                        "db",
                                        "migration",
                                        "V8__qdr_model_gateway_persistence_baseline.sql")
                                .toAbsolutePath()
                                .normalize());
        final List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            collectForbiddenTokenViolations(root, FORBIDDEN_QDR_GATEWAY_MUTATION_TOKENS, violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "\\b(raw_prompt|raw_provider_response)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "B3 persistence must not declare raw prompt/provider response storage fields",
                    violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "import\\s+.*(openai|anthropic|genai|ollama|langgraph|autogen|crewai|"
                                    + "WebClient|RestTemplate|OkHttp|HttpClient|connector\\.nq)",
                            Pattern.CASE_INSENSITIVE),
                    "B3 persistence must not import provider SDK, HTTP client, Agent framework, or NQ client",
                    violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "(WebClient\\.builder|new\\s+RestTemplate|OkHttpClient|"
                                    + "HttpClient\\.new|URI\\.create\\(|URL\\()",
                            Pattern.CASE_INSENSITIVE),
                    "B3 persistence must not create outbound HTTP setup",
                    violations);
        }
        if (!violations.isEmpty()) {
            fail(
                    "stage-qdr-3 B3 persistence source boundary violations:\n"
                            + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B3：不得新增 Prompt / Model Gateway API endpoint 或 Controller。
     */
    @Test
    void stageQdr3B3_rule35_noPromptModelGatewayApiEndpointOrController() {
        final List<String> violations = new ArrayList<>();
        collectPatternViolations(
                Path.of("..", "dh-api", "src", "main", "java").toAbsolutePath().normalize(),
                Pattern.compile(
                        "@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)"
                                + "[\\s\\S]{0,240}(prompt|model-gateway|provider-profile|model-version)",
                        Pattern.CASE_INSENSITIVE),
                "B3 must not add prompt/model gateway API endpoint",
                violations);
        collectPatternViolations(
                Path.of("..", "dh-api", "src", "main", "java").toAbsolutePath().normalize(),
                Pattern.compile(
                        "class\\s+\\w*(Prompt|ModelGateway|ProviderProfile|ModelVersion)\\w*Controller",
                        Pattern.CASE_INSENSITIVE),
                "B3 must not add prompt/model gateway Controller",
                violations);
        if (!violations.isEmpty()) {
            fail("stage-qdr-3 B3 API boundary violations:\n" + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B3：必须存在唯一 V8 migration；Stage4 B2 之后只允许已授权的 V9。
     */
    @Test
    void stageQdr3B3_rule36_v8MigrationExistsAndUnexpectedV9DoesNotExist() {
        final Path migrationRoot =
                Path.of("src", "main", "resources", "db", "migration").toAbsolutePath().normalize();
        final Path v8 = migrationRoot.resolve("V8__qdr_model_gateway_persistence_baseline.sql");
        final List<String> violations = new ArrayList<>();
        if (!Files.exists(v8)) {
            violations.add("missing V8 migration: " + v8);
        }
        try (Stream<Path> walker = Files.walk(migrationRoot)) {
            final long v8Count =
                    walker.filter(p -> p.getFileName().toString().startsWith("V8__")).count();
            if (v8Count != 1) {
                violations.add("expected exactly one V8 migration, found " + v8Count);
            }
        } catch (IOException io) {
            violations.add("failed to walk " + migrationRoot + ": " + io.getMessage());
        }
        collectUnexpectedV9MigrationViolations(migrationRoot, violations);
        if (!violations.isEmpty()) {
            fail("stage-qdr-3 B3 migration version boundary violations:\n" + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B4：pipeline mock gateway integration 不得新增 API、Controller 或未授权 migration。
     *
     * <p>B4 只接入既有 dry-run / QDR pipeline；provider/model gateway API、Controller 与 DB migration 均保持
     * NOT STARTED；Stage4 B2 已授权的 replay/evaluation V9 不属于 B4 mock gateway 扩展。
     */
    @Test
    void stageQdr3B4_rule37_noApiControllerOrMigrationExpansion() {
        final List<String> violations = new ArrayList<>();
        final Path apiRoot = Path.of("..", "dh-api", "src", "main", "java").toAbsolutePath().normalize();
        collectPatternViolations(
                apiRoot,
                Pattern.compile(
                        "@(RequestMapping|GetMapping|PostMapping|PutMapping|DeleteMapping|PatchMapping)"
                                + "[\\s\\S]{0,240}(prompt|model-gateway|provider-profile|model-version)",
                        Pattern.CASE_INSENSITIVE),
                "B4 must not add prompt/model gateway API endpoint",
                violations);
        collectPatternViolations(
                apiRoot,
                Pattern.compile(
                        "class\\s+\\w*(Prompt|ModelGateway|ProviderProfile|ModelVersion)\\w*Controller",
                        Pattern.CASE_INSENSITIVE),
                "B4 must not add prompt/model gateway Controller",
                violations);
        final Path migrationRoot =
                Path.of("src", "main", "resources", "db", "migration").toAbsolutePath().normalize();
        collectUnexpectedV9MigrationViolations(migrationRoot, violations);
        if (!violations.isEmpty()) {
            fail("stage-qdr-3 B4 API/migration boundary violations:\n" + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B4：dry-run pipeline 不允许绕过 gateway integration port。
     *
     * <p>QDR dry-run pipeline 只能依赖 QdrModelGatewayIntegrationPort；ProviderTrustPolicy 与 provider 调用必须留在
     * qdr.gateway 包内部，不能被 dry-run service 直接触达。
     */
    @Test
    void stageQdr3B4_rule38_pipelineUsesGatewayIntegrationPortOnly() {
        final Path dryRunService =
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
                                "decision",
                                "dryrun",
                                "DefaultDecisionDryRunService.java")
                        .toAbsolutePath()
                        .normalize();
        final List<String> violations = new ArrayList<>();
        try {
            final String body = Files.readString(dryRunService, StandardCharsets.UTF_8);
            if (!body.contains("QdrModelGatewayIntegrationPort")) {
                violations.add(dryRunService + ": dry-run pipeline must depend on QdrModelGatewayIntegrationPort");
            }
            if (body.contains("ModelProviderPort")
                    || body.contains("MockModelProvider")
                    || body.contains("ProviderTrustPolicy")) {
                violations.add(dryRunService + ": dry-run pipeline must not bypass gateway/trust boundary");
            }
        } catch (IOException io) {
            violations.add("failed to read " + dryRunService + ": " + io.getMessage());
        }
        if (!violations.isEmpty()) {
            fail("stage-qdr-3 B4 gateway-only pipeline boundary violations:\n" + String.join("\n", violations));
        }
    }

    /**
     * stage-qdr-3 B4：pipeline / gateway / app mock wiring 不允许出现真实 provider、HTTP、Agent、NQ、交易
     * mutation 或 raw storage 字段。
     */
    @Test
    void stageQdr3B4_rule39_sourcesForbidRealProviderHttpAgentNqTradingAndRawStorage() {
        final List<Path> roots =
                List.of(
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
                                        "decision",
                                        "dryrun")
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
                                        "gateway")
                                .toAbsolutePath()
                                .normalize(),
                        Path.of(
                                        "src",
                                        "main",
                                        "java",
                                        "com",
                                        "guidinglight",
                                        "decisionhub",
                                        "config",
                                        "DecisionPipelineWiringConfig.java")
                                .toAbsolutePath()
                                .normalize());
        final List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            collectForbiddenTokenViolations(root, FORBIDDEN_QDR_GATEWAY_MUTATION_TOKENS, violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "\\b(raw_prompt|raw_provider_response)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "B4 must not declare raw prompt/provider response storage fields",
                    violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "import\\s+.*(openai|anthropic|genai|ollama|langgraph|autogen|crewai|"
                                    + "WebClient|RestTemplate|OkHttp|HttpClient|connector\\.nq)",
                            Pattern.CASE_INSENSITIVE),
                    "B4 must not import provider SDK, HTTP client, Agent framework, or NQ client",
                    violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "(WebClient\\.builder|new\\s+RestTemplate|OkHttpClient|"
                                    + "HttpClient\\.new|URI\\.create\\(|URL\\()",
                            Pattern.CASE_INSENSITIVE),
                    "B4 must not create outbound HTTP setup",
                    violations);
        }
        if (!violations.isEmpty()) {
            fail(
                    "stage-qdr-3 B4 source boundary violations:\n"
                            + String.join("\n", violations));
        }
    }

    /**
     * Stage-QDR-8：feedback attribution foundation 必须保持纯确定性、无 runtime wiring 与无副作用。
     *
     * <p>该规则只扫描冻结的两个 production 新包，拒绝 HTTP/Provider/NQ/Agent/LangGraph、Spring
     * wiring、Repository/JDBC、系统时钟/随机数/线程池，以及 Experience/Pheromone/Prompt/Judge mutation。
     */
    @Test
    void stageQdr8_rule40_feedbackAttributionFoundationHasNoExternalOrMutationPath() {
        final List<Path> roots =
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
                                        "feedback")
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
                                        "feedback")
                                .toAbsolutePath()
                                .normalize());
        final List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            if (!Files.isDirectory(root)) {
                violations.add("missing Stage-QDR-8 feedback source root: " + root);
                continue;
            }
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "import\\s+.*(springframework|WebClient|RestTemplate|OkHttp|HttpClient|"
                                    + "connector\\.nq|providers|usecase\\.agent|langgraph|autogen|crewai|"
                                    + "memory|infra\\.jdbc)",
                            Pattern.CASE_INSENSITIVE),
                    "Stage-QDR-8 feedback source imports forbidden runtime dependency",
                    violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "@(Component|Service|Repository|Configuration|Bean)\\b|"
                                    + "\\bclass\\s+\\w*(Repository|Jdbc|Controller)\\b|"
                                    + "\\binterface\\s+\\w*(Jdbc|Controller)\\b",
                            Pattern.CASE_INSENSITIVE),
                    "Stage-QDR-8 feedback source declares runtime wiring, Repository implementation,"
                            + " JDBC, or Controller",
                    violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "System\\.(currentTimeMillis|nanoTime)|Instant\\.now\\(|Clock\\.system|"
                                    + "UUID\\.randomUUID|ThreadLocalRandom|new\\s+Random|"
                                    + "Executors\\.new|ExecutorService"),
                    "Stage-QDR-8 feedback source uses nondeterministic time, random, or executor",
                    violations);
            collectPatternViolations(
                    root,
                    Pattern.compile(
                            "ExperienceFeedbackService|Pheromone(Store|Edge)?|PromptVersion|JudgeDecision|"
                                    + "\\.(reinforce|penalize|decay|apply)\\s*\\("),
                    "Stage-QDR-8 feedback source reaches forbidden learning or state mutation",
                    violations);
        }
        if (!violations.isEmpty()) {
            fail("Stage-QDR-8 feedback attribution boundary violations:\n"
                    + String.join("\n", violations));
        }
    }

    /** Feedback ingress 必须保持 append-only，永久禁止重新依赖 mutable learning capability。 */
    @Test
    void feedbackContainment_rule41_ingressPackagesDoNotDependOnLearningServiceOrStores() {
        final JavaClasses classes = importMainClasses();
        noClasses()
                .that()
                .resideInAPackage("..usecase.agent.feedback..")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService")
                .check(classes);
        noClasses()
                .that()
                .resideInAPackage("..usecase.agent.feedback..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..memory.agent..")
                .check(classes);
        noClasses()
                .that()
                .resideInAPackage("..api.feedback..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..memory.agent..")
                .check(classes);
        noClasses()
                .that()
                .haveSimpleName("FeedbackIngestionWiringConfig")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("..memory.agent..")
                .check(classes);
        noClasses()
                .that()
                .haveSimpleName("FeedbackIngestionWiringConfig")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService")
                .check(classes);
        noClasses()
                .that()
                .haveSimpleName("DefaultNqIntegrationUseCase")
                .should()
                .dependOnClassesThat()
                .haveFullyQualifiedName(
                        "com.guidinglight.decisionhub.usecase.agent.ExperienceFeedbackService")
                .check(classes);
    }

    /** Source guard 覆盖全部 handler、router、ingestion、Controller、wiring 与 compatibility bean。 */
    @Test
    void feedbackContainment_rule42_ingressSourcesForbidHiddenLearningMutationPaths() {
        final List<Path> roots =
                List.of(
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
                                        "agent",
                                        "feedback")
                                .toAbsolutePath()
                                .normalize(),
                        Path.of(
                                        "..",
                                        "dh-api",
                                        "src",
                                        "main",
                                        "java",
                                        "com",
                                        "guidinglight",
                                        "decisionhub",
                                        "api",
                                        "feedback")
                                .toAbsolutePath()
                                .normalize(),
                        Path.of(
                                        "src",
                                        "main",
                                        "java",
                                        "com",
                                        "guidinglight",
                                        "decisionhub",
                                        "config",
                                        "FeedbackIngestionWiringConfig.java")
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
                                        "agent",
                                        "impl",
                                        "DefaultNqIntegrationUseCase.java")
                                .toAbsolutePath()
                                .normalize());
        final Pattern hiddenMutation =
                Pattern.compile(
                        "ExperienceFeedbackService|ExperienceStore|PheromoneStore|FailureCaseStore|"
                                + "ObjectProvider\\s*<\\s*ExperienceFeedbackService|"
                                + "ApplicationContext\\s*\\.\\s*getBean|"
                                + "@(EventListener|TransactionalEventListener|Scheduled|Async)\\b|"
                                + "TransactionSynchronization|afterCommit|Class\\.forName|Method\\.invoke");
        final List<String> violations = new ArrayList<>();
        for (Path root : roots) {
            collectPatternViolations(
                    root,
                    hiddenMutation,
                    "feedback ingress contains forbidden learning dependency or hidden mutation path",
                    violations);
        }
        if (!violations.isEmpty()) {
            fail("feedback side-effect containment violations:\n" + String.join("\n", violations));
        }
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

    private static void collectPatternViolations(
            final Path rootOrFile,
            final Pattern pattern,
            final String violationMessage,
            final List<String> violations) {
        if (!Files.exists(rootOrFile)) {
            return;
        }
        try (Stream<Path> walker =
                     Files.isDirectory(rootOrFile) ? Files.walk(rootOrFile) : Stream.of(rootOrFile)) {
            walker
                    .filter(p -> p.toString().endsWith(".java") || p.toString().endsWith(".sql"))
                    .forEach(
                            p -> {
                                try {
                                    final String body = Files.readString(p, StandardCharsets.UTF_8);
                                    final Matcher matcher = pattern.matcher(body);
                                    if (matcher.find()) {
                                        violations.add(p + ": " + violationMessage);
                                    }
                                } catch (IOException io) {
                                    violations.add("failed to read " + p + ": " + io.getMessage());
                                }
                            });
        } catch (IOException io) {
            violations.add("failed to walk " + rootOrFile + ": " + io.getMessage());
        }
    }

    private static void collectV8MigrationFileViolations(
            final Path migrationRoot, final List<String> violations) {
        if (!Files.exists(migrationRoot)) {
            return;
        }
        try (Stream<Path> walker = Files.walk(migrationRoot)) {
            walker
                    .filter(p -> p.getFileName().toString().startsWith("V8__"))
                    .forEach(p -> violations.add(p + ": B1 must not add V8 migration artifact"));
        } catch (IOException io) {
            violations.add("failed to walk " + migrationRoot + ": " + io.getMessage());
        }
    }

    private static void collectUnexpectedV9MigrationViolations(
            final Path migrationRoot, final List<String> violations) {
        if (!Files.exists(migrationRoot)) {
            return;
        }
        try (Stream<Path> walker = Files.walk(migrationRoot)) {
            walker
                    .filter(p -> p.getFileName().toString().startsWith("V9__"))
                    .filter(
                            p ->
                                    !"V9__qdr_replay_evaluation_baseline.sql"
                                            .equals(p.getFileName().toString()))
                    .forEach(p -> violations.add(p + ": unexpected V9 migration artifact"));
        } catch (IOException io) {
            violations.add("failed to walk " + migrationRoot + ": " + io.getMessage());
        }
    }

    private static void collectGatewayBypassViolations(
            final Path qdrRoot, final Path gatewayRoot, final List<String> violations) {
        if (!Files.exists(qdrRoot)) {
            return;
        }
        try (Stream<Path> walker = Files.walk(qdrRoot)) {
            walker
                    .filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.normalize().startsWith(gatewayRoot.normalize()))
                    .forEach(
                            p -> {
                                try {
                                    final String body = Files.readString(p, StandardCharsets.UTF_8);
                                    if (body.contains("MockModelProvider")
                                            || body.contains("ModelProviderPort")) {
                                        violations.add(p + ": direct provider dependency outside gateway");
                                    }
                                } catch (IOException io) {
                                    violations.add("failed to read " + p + ": " + io.getMessage());
                                }
                            });
        } catch (IOException io) {
            violations.add("failed to walk " + qdrRoot + ": " + io.getMessage());
        }
    }
}
