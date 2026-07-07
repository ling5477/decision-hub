package com.guidinglight.decisionhub.usecase.qdr.gateway;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * MockModelProvider deterministic / no-outbound 回归。
 */
final class MockModelProviderTest {

    @Test
    void mockProviderReturnsDeterministicOutput() {
        final MockModelProvider provider = new MockModelProvider();
        final ModelGatewayRequest request = request();

        final MockModelProviderResult first = provider.invoke(request, "rendered prompt");
        final MockModelProviderResult second = provider.invoke(request, "rendered prompt");

        assertEquals(first, second);
        assertEquals(MockModelProviderMode.NORMAL, first.mode());
    }

    @Test
    void mockProviderUnavailableModeFailsClosed() {
        final MockModelProvider provider = new MockModelProvider(MockModelProviderMode.UNAVAILABLE);

        assertThrows(ModelProviderUnavailableException.class, () -> provider.invoke(request(), "rendered prompt"));
    }

    @Test
    void mockProviderMalformedModeReturnsMalformedResult() {
        final MockModelProvider provider = new MockModelProvider(MockModelProviderMode.MALFORMED);

        final MockModelProviderResult result = provider.invoke(request(), "rendered prompt");

        assertEquals(MockModelProviderMode.MALFORMED, result.mode());
    }

    @Test
    void mockProviderSourceDoesNotContainHttpOrCredentialRead() throws IOException {
        final String source =
                Files.readString(
                        Path.of(
                                "src",
                                "main",
                                "java",
                                "com",
                                "guidinglight",
                                "decisionhub",
                                "usecase",
                                "qdr",
                                "gateway",
                                "MockModelProvider.java"),
                        StandardCharsets.UTF_8);

        assertFalse(source.contains("HttpClient"));
        assertFalse(source.contains("WebClient"));
        assertFalse(source.contains("RestTemplate"));
        assertFalse(source.contains("OkHttp"));
        assertFalse(source.contains("System.getenv"));
        assertFalse(source.contains("System.getProperty"));
    }

    @Test
    void mockProviderResultDoesNotContainExecutableTradingInstruction() {
        final MockModelProviderResult result =
                new MockModelProvider().invoke(request(), "rendered prompt");

        assertEquals(ModelGatewayDecisionAction.OBSERVE, result.decision().action());
        assertFalse(result.redactedSummary().contains("BUY"));
        assertFalse(result.redactedSummary().contains("SELL"));
        assertFalse(result.redactedSummary().contains("PLACE_ORDER"));
        assertFalse(result.redactedSummary().contains("CANCEL_ORDER"));
        assertTrue(result.safeProviderRef().startsWith("mock-provider:"));
    }

    private static ModelGatewayRequest request() {
        return new ModelGatewayRequest(
                new ModelCallContext(
                        "tenant-a",
                        "trace-a",
                        "request-a",
                        "decision-run-a",
                        "prompt-v1",
                        "model-v1",
                        "provider-mock"),
                "qdr-template",
                "v1",
                "0000000000000000000000000000000000000000000000000000000000000000",
                "0000000000000000000000000000000000000000000000000000000000000000",
                java.util.Map.of("symbol", "BTCUSDT"),
                java.util.List.of("risk LOW"),
                new ModelCallPolicy("policy", true, true, true),
                new ModelCallBudget(200, 200, 200, 200, 3),
                ModelCallRedactionPolicy.strictDefault());
    }
}
