package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.util.Set;

/**
 * Controller 传入 dry-run usecase 的命令对象。
 *
 * <p>命令只包含已认证/已解析的安全摘要，不携带 raw body、signature、credential 或 header 原文。source、tenant、
 * requestId、traceId 的安全绑定在 API/security 层完成，本 usecase 继续做 feature/policy/audit 闸门。
 *
 * @param requestId request id。
 * @param traceId trace id。
 * @param tenantId body tenant id。
 * @param source body source。
 * @param timestamp body timestamp，必须与 canonical timestamp header 一致。
 * @param nonce body nonce，必须与 canonical nonce header 一致。
 * @param schemaVersion schema version。
 * @param dryRun 是否 dryRun；必须为 true。
 * @param forbiddenCapabilities 调用方显式声明禁止能力。
 * @param context 只读 decision context。
 * @param forbiddenMaterialDetected API 层 recursive scan 是否发现执行/凭证/敏感字段。
 */
public record DecisionDryRunCommand(
    String requestId,
    String traceId,
    String tenantId,
    String source,
    String timestamp,
    String nonce,
    String schemaVersion,
    boolean dryRun,
    Set<String> forbiddenCapabilities,
    DecisionDryRunContext context,
    boolean forbiddenMaterialDetected) {

  /** 归一化 forbiddenCapabilities，避免可变集合逃逸。 */
  public DecisionDryRunCommand {
    forbiddenCapabilities =
        forbiddenCapabilities == null ? Set.of() : Set.copyOf(forbiddenCapabilities);
  }
}
