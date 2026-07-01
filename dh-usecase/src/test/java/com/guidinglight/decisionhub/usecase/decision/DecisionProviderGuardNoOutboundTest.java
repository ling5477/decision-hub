package com.guidinglight.decisionhub.usecase.decision;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * K5 provider guard 源码边界测试，防止 guard 层引入外部 provider、HTTP、NQ 或模型 SDK 名称。
 */
final class DecisionProviderGuardNoOutboundTest {

    @Test
    void providerGuardSourcesDoNotReferenceOutboundRuntimeDependencies() throws IOException {
        final Path sourceDir = sourceDir();
        final List<String> sources;
        try (var stream = Files.walk(sourceDir)) {
            sources =
                    stream
                            .filter(Files::isRegularFile)
                            .filter(path -> path.getFileName().toString().contains("DecisionProvider"))
                            .map(this::read)
                            .toList();
        }

        final String joined = String.join("\n", sources);
        for (String forbidden :
                List.of(
                        "WebClient",
                        "RestTemplate",
                        "HttpClient",
                        "OpenAI",
                        "Claude",
                        "Gemini",
                        "NqClient",
                        "RealClient",
                        "Exchange",
                        "Broker",
                        "PostMapping",
                        "GetMapping",
                        "RequestMapping")) {
            assertFalse(joined.contains(forbidden), forbidden + " must not appear in K5 guard code");
        }
    }

    private static Path sourceDir() {
        final Path moduleDir =
                Path.of("src/main/java/com/guidinglight/decisionhub/usecase/decision");
        if (Files.exists(moduleDir)) {
            return moduleDir;
        }
        return Path.of(
                "dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/decision");
    }

    private String read(final Path path) {
        try {
            return Files.readString(path);
        } catch (final IOException error) {
            throw new IllegalStateException("failed to read " + path, error);
        }
    }
}
