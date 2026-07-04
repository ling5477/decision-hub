package com.guidinglight.decisionhub.usecase.decision.dryrun;

/**
 * Integration-1 limited dry-run endpoint 的 usecase 边界。
 *
 * <p>实现必须只返回 read-only decision snapshot，不调用 NQ、不发起 HTTP、不接真实 provider、不触发
 * Agent/LangGraph runtime、不读写交易状态。所有安全或审计失败必须 fail-closed。
 */
public interface DecisionDryRunService {

  /**
   * 执行一次 limited dry-run decision。
   *
   * @param command 已认证并脱敏提取后的 dry-run 命令。
   * @return 成功 snapshot 或 fail-closed error。
   */
  DecisionDryRunResult execute(DecisionDryRunCommand command);

  /**
   * 记录 controller/security 层已经判定的 fail-closed 拒绝。
   *
   * @param command 已解析出的 dry-run 命令；解析失败时可为 null。
   * @param status HTTP 状态建议。
   * @param errorCode canonical error code。
   * @param message 脱敏错误摘要。
   * @return 带 auditRef 的失败结果；audit 写失败时返回 UNKNOWN_ERROR。
   */
  DecisionDryRunResult reject(
      DecisionDryRunCommand command,
      int status,
      DecisionDryRunErrorCode errorCode,
      String message);
}
