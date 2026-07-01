package com.guidinglight.decisionhub.usecase.decision;

import static com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunAssertionSupport.assertFixtureContainsNoCredentialOrExecutionIntent;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.usecase.decision.support.MockNqDryRunFixtures;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * K6 no-live-trade 合同测试，验证 dry-run 没有引入出站 runtime、交易动作或 API 边界。
 */
final class MockNqDecisionNoLiveTradeContractTest {

    private static final Set<String> ALLOWED_ACTIONS =
            Set.of("ABSTAIN", "OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS");

    private static final List<String> FORBIDDEN_CODE_TOKENS =
            List.of(
                    "RealClient",
                    "LangGraph",
                    "OpenAI",
                    "Claude",
                    "Gemini",
                    "WebClient",
                    "RestTemplate",
                    "HttpClient",
                    "placeOrder",
                    "cancelOrder",
                    "NqClient",
                    "Exchange",
                    "Broker",
                    "Controller",
                    "PostMapping",
                    "GetMapping",
                    "RequestMapping");

    private static final List<Path> DECISION_PIPELINE_PRODUCTION_PATHS =
            List.of(
                    Path.of("src", "main", "java", "com", "guidinglight", "decisionhub", "usecase", "decision"),
                    Path.of("..", "dh-domain", "src", "main", "java", "com", "guidinglight", "decisionhub", "domain", "decision"),
                    Path.of("..", "dh-infra", "src", "main", "java", "com", "guidinglight", "decisionhub", "infra", "jdbc", "decision"),
                    Path.of("..", "dh-app", "src", "main", "java", "com", "guidinglight", "decisionhub", "config", "DecisionPipelineWiringConfig.java"));

    @Test
    void decisionActionVocabularyContainsNoTradingCommands() {
        final Set<String> actual = Stream.of(DecisionAction.values()).map(Enum::name).collect(java.util.stream.Collectors.toSet());

        assertEquals(ALLOWED_ACTIONS, actual);
        assertFalse(actual.contains("BUY"));
        assertFalse(actual.contains("SELL"));
        assertFalse(actual.contains("PLACE_ORDER"));
        assertFalse(actual.contains("CANCEL_ORDER"));
        assertFalse(actual.contains("MARKET_ORDER"));
        assertFalse(actual.contains("LIMIT_ORDER"));
    }

    @Test
    void decisionPipelineProductionCodeContainsNoOutboundRuntimeDependencies() throws Exception {
        for (Path path : DECISION_PIPELINE_PRODUCTION_PATHS) {
            if (!Files.exists(path)) {
                continue;
            }
            if (Files.isDirectory(path)) {
                try (Stream<Path> files = Files.walk(path)) {
                    for (Path file : files.filter(Files::isRegularFile).filter(this::isJavaFile).toList()) {
                        assertNoForbiddenCodeTokens(file);
                    }
                }
            } else {
                assertNoForbiddenCodeTokens(path);
            }
        }
    }

    @Test
    void mockDryRunFixturesContainNoExecutionIntentOrCredentialFields() {
        for (String filename : MockNqDryRunFixtures.fixtureFilenames()) {
            assertFixtureContainsNoCredentialOrExecutionIntent(MockNqDryRunFixtures.readFixture(filename));
        }
    }

    private boolean isJavaFile(final Path path) {
        return path.getFileName().toString().endsWith(".java");
    }

    private static void assertNoForbiddenCodeTokens(final Path file) throws IOException {
        final String codeOnly = stripCommentsAndStringLiterals(Files.readString(file));
        for (String token : FORBIDDEN_CODE_TOKENS) {
            assertFalse(
                    codeOnly.contains(token),
                    "decision pipeline production code must not contain token " + token + " in " + file);
        }
        assertTrue(codeOnly.contains("class") || codeOnly.contains("interface") || codeOnly.contains("enum") || codeOnly.contains("record"));
    }

    private static String stripCommentsAndStringLiterals(final String source) {
        return source
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("(?m)//.*$", " ")
                .replaceAll("\"(?:\\\\.|[^\"\\\\])*\"", "\"\"")
                .replaceAll("'(?:\\\\.|[^'\\\\])*'", "''");
    }
}
