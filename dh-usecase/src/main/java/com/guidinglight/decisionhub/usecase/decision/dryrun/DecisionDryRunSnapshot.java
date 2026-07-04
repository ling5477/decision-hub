package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.math.BigDecimal;
import java.util.List;

/**
 * limited dry-run endpoint 的成功响应 snapshot。
 *
 * <p>该 snapshot 是 read-only decision view；action 只能为 OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS。
 * 它不包含 quantity、price、leverage、order side、PLACE_ORDER、CANCEL_ORDER 或任何 executable instruction。
 *
 * @param decisionId decision id。
 * @param dryRun 固定 true。
 * @param action 对外 runtime action。
 * @param confidence 0..1 置信度。
 * @param riskLevel 风险级别。
 * @param reasons 安全 reason code 列表。
 * @param traceSummary 脱敏 trace 摘要。
 * @param replayRef replay 引用，不包含 raw payload。
 * @param auditRef audit 引用，不包含 raw payload。
 * @param schemaVersion schema version。
 */
public record DecisionDryRunSnapshot(
    String decisionId,
    boolean dryRun,
    String action,
    BigDecimal confidence,
    String riskLevel,
    List<String> reasons,
    List<String> traceSummary,
    String replayRef,
    String auditRef,
    String schemaVersion) {

  /** 归一化列表字段，避免响应后被调用方修改。 */
  public DecisionDryRunSnapshot {
    reasons = reasons == null ? List.of() : List.copyOf(reasons);
    traceSummary = traceSummary == null ? List.of() : List.copyOf(traceSummary);
  }
}
