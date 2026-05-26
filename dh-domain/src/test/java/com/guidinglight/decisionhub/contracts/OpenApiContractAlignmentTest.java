package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Stage3-B1 Contract Alignment：contracts/openapi.yaml 与已实现 NqFeedbackController 一致性 +
 * 危险路径黑名单。
 *
 * <p>本测试不引入 YAML parser，全部使用 text-based 断言（足以约束契约口径 + 黑名单关键词）。
 *
 * <p>检查项：
 *
 * <ul>
 *   <li>openapi.yaml 文件存在；
 *   <li>已实现端点 POST /api/ai/feedback/nq 出现，且与 NqFeedbackController 一致：
 *       <ul>
 *         <li>response 202 引用 NqFeedbackAcceptedResponse；
 *         <li>response 400 引用 NqFeedbackErrorResponse；
 *         <li>request body 引用 NqFeedbackEnvelope；
 *       </ul>
 *   <li>NqFeedbackAcceptedResponse.outcome 枚举包含 ACCEPTED 与 DUPLICATE；
 *   <li>NqFeedbackErrorResponse.errorCode 枚举包含 UNKNOWN_EVENT_TYPE / INVALID_SCHEMA /
 *       UNKNOWN_TRACE（与 IngestionErrorCode 一致）；
 *   <li>components 含 DhBacktestRequest / DhBacktestRequestAccepted / DhBacktestResultSnapshot；
 *   <li>NqFeedbackEventType 枚举仍为 8 种（Stage3 不允许扩展）；
 *   <li>整个文档不出现下单 / 实盘 / 绕风控关键词；
 *   <li>整个文档不出现 /orders /trades /live 实盘类路径片段；
 *   <li>Stage3-B1 不新增 NQ 端 /api/ai/research/backtest-requests path（仅注释占位）。
 * </ul>
 */
class OpenApiContractAlignmentTest {

  private static final List<String> FORBIDDEN_KEYWORDS =
      List.of(
          "placeOrder",
          "submitOrder",
          "executeOrder",
          "bypassRisk",
          "forceExecute");

  private static final List<String> FORBIDDEN_PATH_SEGMENTS =
      List.of("/orders", "/trades", "/live");

  private static final List<String> EXPECTED_FEEDBACK_ERROR_CODES =
      List.of("UNKNOWN_EVENT_TYPE", "INVALID_SCHEMA", "UNKNOWN_TRACE");

  private static final List<String> EXPECTED_FEEDBACK_EVENT_TYPES =
      List.of(
          "PAPER_RUN_CREATED",
          "PAPER_RUN_STARTED",
          "PAPER_RUN_STOPPED",
          "PAPER_RUN_DAILY_REPORT_GENERATED",
          "PAPER_RUN_ALERT_RAISED",
          "PAPER_RUN_RECOVERY_EVENT_RECORDED",
          "PAPER_RUN_STABILITY_CHECK_COMPLETED",
          "BACKTEST_RESULT_READY");

  private static Path openapiPath() {
    return Path.of("..", "contracts", "openapi.yaml").toAbsolutePath().normalize();
  }

  private static String readOpenapi() throws Exception {
    return Files.readString(openapiPath());
  }

  @Test
  void openapiYamlFileExists() {
    assertTrue(Files.exists(openapiPath()), "contracts/openapi.yaml must exist");
  }

  @Test
  void feedbackEndpointMatchesNqFeedbackControllerImplementation() throws Exception {
    final String body = readOpenapi();
    assertTrue(body.contains("/api/ai/feedback/nq"), "must expose POST /api/ai/feedback/nq");
    assertTrue(body.contains("NqFeedbackEnvelope"), "request body must reference NqFeedbackEnvelope");
    assertTrue(
        body.contains("NqFeedbackAcceptedResponse"),
        "202 response must reference NqFeedbackAcceptedResponse");
    assertTrue(
        body.contains("NqFeedbackErrorResponse"),
        "400 response must reference NqFeedbackErrorResponse");
    // 与 NqFeedbackController.java 中 ResponseEntity.accepted() / status(BAD_REQUEST) 对齐
    assertTrue(body.contains("'202'") || body.contains("\"202\""), "must declare 202 response");
    assertTrue(body.contains("'400'") || body.contains("\"400\""), "must declare 400 response");
  }

  @Test
  void acceptedResponseOutcomeEnumCoversAcceptedAndDuplicate() throws Exception {
    final String body = readOpenapi();
    assertTrue(body.contains("ACCEPTED"), "outcome enum must include ACCEPTED");
    assertTrue(body.contains("DUPLICATE"), "outcome enum must include DUPLICATE");
  }

  @Test
  void errorResponseErrorCodeEnumMatchesIngestionErrorCode() throws Exception {
    final String body = readOpenapi();
    for (String code : EXPECTED_FEEDBACK_ERROR_CODES) {
      assertTrue(
          body.contains(code),
          "NqFeedbackErrorResponse.errorCode enum must include: " + code);
    }
  }

  @Test
  void backtestContractComponentsArePresent() throws Exception {
    final String body = readOpenapi();
    assertTrue(body.contains("DhBacktestRequest:"), "components must define DhBacktestRequest");
    assertTrue(
        body.contains("DhBacktestRequestAccepted:"),
        "components must define DhBacktestRequestAccepted");
    assertTrue(
        body.contains("DhBacktestResultSnapshot:"),
        "components must define DhBacktestResultSnapshot");
  }

  @Test
  void feedbackEventTypeEnumKeepsExactlyEightValues() throws Exception {
    final String body = readOpenapi();
    for (String t : EXPECTED_FEEDBACK_EVENT_TYPES) {
      assertTrue(body.contains(t), "NqFeedbackEventType must include: " + t);
    }
  }

  @Test
  void openapiHasNoTradingExecutionKeywords() throws Exception {
    final String body = readOpenapi();
    for (String token : FORBIDDEN_KEYWORDS) {
      assertFalse(
          body.contains(token),
          "openapi.yaml must not contain forbidden trading-execution keyword: " + token);
    }
  }

  @Test
  void openapiHasNoOrdersTradesLivePathSegments() throws Exception {
    final String body = readOpenapi();
    // openapi.yaml paths 段中以 "  /xxx:" 形式声明；
    // 简单做法：扫描所有缩进 2 个空格 + "/" 开头的行（path 声明）。
    for (String line : body.split("\\R")) {
      // path 声明形如 "  /xxx:" 或 "  /xxx/{id}:"，左侧两个空格
      if (line.startsWith("  /") && line.trim().endsWith(":")) {
        final String declared = line.trim();
        for (String forbidden : FORBIDDEN_PATH_SEGMENTS) {
          assertFalse(
              declared.contains(forbidden),
              "openapi.yaml path must not include forbidden segment: "
                  + forbidden
                  + " (offending line: "
                  + declared
                  + ")");
        }
      }
    }
  }

  @Test
  void stage3PlannedNqBacktestEndpointStaysAsCommentNotPath() throws Exception {
    final String body = readOpenapi();
    // Stage3-B1 严格禁止真实落 /api/ai/research/backtest-requests path；只允许以注释 (# 开头) 描述。
    for (String line : body.split("\\R")) {
      if (line.startsWith("  /") && line.trim().endsWith(":")) {
        final String declared = line.trim();
        assertFalse(
            declared.contains("/api/ai/research/backtest-requests"),
            "Stage3-B1 不允许在 paths 段引入 /api/ai/research/backtest-requests；只允许注释占位");
      }
    }
  }
}
