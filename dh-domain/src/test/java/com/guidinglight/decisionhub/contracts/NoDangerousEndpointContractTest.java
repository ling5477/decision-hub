package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequestStatus;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEventType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B3：DH 仓库内"无危险端点 / 无危险关键词 / 无危险枚举"契约测试。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §11.1 / §11.3 + STAGE3_E2E_CONTRACT_TEST_SPEC §3.7 / §4.8。
 *
 * <p>本测试与 dh-domain {@code OpenApiContractAlignmentTest} 互补：
 *
 * <ul>
 *   <li>OpenApiContractAlignmentTest 只扫 contracts/openapi.yaml；
 *   <li>本测试扫 contracts/openapi.yaml + contracts/json-schema/*.schema.json，
 *       并断言核心 enum（NqFeedbackEventType / DhBacktestRequestStatus）不含危险前缀。
 * </ul>
 *
 * <p>注：源码层关键词由 dh-app ArchitectureTest 的 ②/⑪/⑫ 三条规则覆盖；本测试不重复扫描源码。
 */
final class NoDangerousEndpointContractTest {

  private static final List<String> FORBIDDEN_KEYWORDS =
      List.of("placeOrder", "submitOrder", "executeOrder", "bypassRisk", "forceExecute");

  private static final Pattern FORBIDDEN_PATH_SEGMENT =
      Pattern.compile("(?im)^\\s*(/orders|/trades|/live)(/|\\b)");

  private static final List<String> FORBIDDEN_STATUS_PREFIXES =
      List.of("PLACE_", "SUBMIT_", "EXECUTE_", "LIVE_", "RISK_BYPASS", "FORCE_");

  @Test
  void openapi_hasNoForbiddenKeywords() throws IOException {
    final Path openapi = findContractsRoot().resolve("openapi.yaml");
    final String content = Files.readString(openapi, StandardCharsets.UTF_8);
    for (final String token : FORBIDDEN_KEYWORDS) {
      // 允许在描述类硬边界声明里出现"必须不存在 X"形式；但本仓库现状是 contracts 不含该 token
      assertTrue(
          !content.contains(token),
          "contracts/openapi.yaml must not contain dangerous keyword: " + token);
    }
  }

  @Test
  void openapi_hasNoForbiddenPathSegments() throws IOException {
    final Path openapi = findContractsRoot().resolve("openapi.yaml");
    final String content = Files.readString(openapi, StandardCharsets.UTF_8);
    final var matcher = FORBIDDEN_PATH_SEGMENT.matcher(content);
    if (matcher.find()) {
      fail("contracts/openapi.yaml must not contain dangerous path segment: " + matcher.group());
    }
  }

  @Test
  void allJsonSchemas_haveNoForbiddenKeywords() throws IOException {
    final Path schemaRoot = findContractsRoot().resolve("json-schema");
    try (Stream<Path> stream = Files.list(schemaRoot)) {
      stream
          .filter(p -> p.toString().endsWith(".schema.json"))
          .forEach(
              p -> {
                try {
                  final String content = Files.readString(p, StandardCharsets.UTF_8);
                  for (final String token : FORBIDDEN_KEYWORDS) {
                    assertTrue(
                        !content.contains(token),
                        "schema "
                            + p.getFileName()
                            + " must not contain dangerous keyword: "
                            + token);
                  }
                } catch (final IOException e) {
                  fail("failed to read schema: " + p + " - " + e.getMessage());
                }
              });
    }
  }

  @Test
  void dhBacktestRequestStatus_enum_hasNoDangerousPrefixes() {
    for (final DhBacktestRequestStatus status : DhBacktestRequestStatus.values()) {
      for (final String prefix : FORBIDDEN_STATUS_PREFIXES) {
        assertTrue(
            !status.name().startsWith(prefix),
            "DhBacktestRequestStatus." + status.name() + " must not start with " + prefix);
      }
    }
  }

  @Test
  void nqFeedbackEventType_enum_hasNoDangerousPrefixes() {
    for (final NqFeedbackEventType type : NqFeedbackEventType.values()) {
      for (final String prefix : FORBIDDEN_STATUS_PREFIXES) {
        assertTrue(
            !type.name().startsWith(prefix),
            "NqFeedbackEventType." + type.name() + " must not start with " + prefix);
      }
    }
  }

  @Test
  void nqFeedbackEventType_enum_count_remainsEightValues() {
    // Stage3 硬约束：不允许扩展 NqFeedbackEventType 之外的事件类型。
    assertEquals(
        8, NqFeedbackEventType.values().length, "NqFeedbackEventType must remain exactly 8 values");
  }

  /** 沿用 OpenApiContractAlignmentTest 的相对路径回溯逻辑。 */
  private static Path findContractsRoot() {
    Path probe = Paths.get("").toAbsolutePath();
    for (int i = 0; i < 6; i++) {
      final Path candidate = probe.resolve("contracts");
      if (Files.isDirectory(candidate)) {
        return candidate;
      }
      probe = probe.getParent();
      if (probe == null) {
        break;
      }
    }
    fail("contracts/ directory not found from working dir; checked up to 6 levels up");
    throw new IllegalStateException("unreachable");
  }
}
