package com.guidinglight.decisionhub.api.decision;

import java.math.BigDecimal;
import java.util.List;

/**
 * limited dry-run endpoint 的成功响应 envelope。
 *
 * <p>字段只表达 read-only decision snapshot，不包含 BUY/SELL、PLACE_ORDER/CANCEL_ORDER、quantity、price、
 * leverage 或任何 executable order instruction。
 *
 * @param decisionId decision id。
 * @param dryRun 固定 true。
 * @param action OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS。
 * @param confidence 0..1 置信度。
 * @param riskLevel 风险等级。
 * @param reasons 安全 reason code。
 * @param traceSummary 脱敏 trace summary。
 * @param replayRef replay 引用。
 * @param auditRef audit 引用。
 * @param schemaVersion schema version。
 */
public record DecisionDryRunSuccessResponse(
    String decisionId,
    boolean dryRun,
    String action,
    BigDecimal confidence,
    String riskLevel,
    List<String> reasons,
    List<String> traceSummary,
    String replayRef,
    String auditRef,
    String schemaVersion) {}
