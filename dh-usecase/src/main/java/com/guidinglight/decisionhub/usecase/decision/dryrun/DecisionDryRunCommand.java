package com.guidinglight.decisionhub.usecase.decision.dryrun;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackExecutionScope;
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
 * @param environment signed wire environment；在 executionScope 附加前不是业务 authority。
 * @param timestamp body timestamp，必须与 canonical timestamp header 一致。
 * @param nonce body nonce，必须与 canonical nonce header 一致。
 * @param schemaVersion schema version。
 * @param dryRun 是否 dryRun；必须为 true。
 * @param forbiddenCapabilities 调用方显式声明禁止能力。
 * @param context 只读 decision context。
 * @param forbiddenMaterialDetected API 层 recursive scan 是否发现执行/凭证/敏感字段。
 * @param executionScope 认证根在联合授权成功后建立的 verified scope。
 */
public record DecisionDryRunCommand(
    String requestId,
    String traceId,
    String tenantId,
    String source,
    String environment,
    String timestamp,
    String nonce,
    String schemaVersion,
    boolean dryRun,
    Set<String> forbiddenCapabilities,
    DecisionDryRunContext context,
    boolean forbiddenMaterialDetected,
    FeedbackExecutionScope executionScope) {

  /** 归一化 forbiddenCapabilities，避免可变集合逃逸。 */
  public DecisionDryRunCommand {
    forbiddenCapabilities =
        forbiddenCapabilities == null ? Set.of() : Set.copyOf(forbiddenCapabilities);
  }

  /** 保留无 environment 的旧 internal/test 构造；persistent/runtime root 会 fail-closed 拒绝。 */
  public DecisionDryRunCommand(
      final String requestId,
      final String traceId,
      final String tenantId,
      final String source,
      final String timestamp,
      final String nonce,
      final String schemaVersion,
      final boolean dryRun,
      final Set<String> forbiddenCapabilities,
      final DecisionDryRunContext context,
      final boolean forbiddenMaterialDetected) {
    this(
        requestId,
        traceId,
        tenantId,
        source,
        null,
        timestamp,
        nonce,
        schemaVersion,
        dryRun,
        forbiddenCapabilities,
        context,
        forbiddenMaterialDetected,
        null);
  }

  /**
   * 在认证根附加 immutable verified scope；tenant/environment drift 立即拒绝。
   *
   * @param verifiedScope HMAC 与联合授权成功后建立的 scope
   * @return 携带 scope 的新 command
   */
  public DecisionDryRunCommand withExecutionScope(final FeedbackExecutionScope verifiedScope) {
    if (verifiedScope == null
        || tenantId == null
        || !tenantId.equals(verifiedScope.tenantId())
        || environment == null
        || !environment.equals(verifiedScope.environment().name())) {
      throw new IllegalArgumentException(
          "verified feedback execution scope does not match dry-run command");
    }
    return new DecisionDryRunCommand(
        requestId,
        traceId,
        tenantId,
        source,
        environment,
        timestamp,
        nonce,
        schemaVersion,
        dryRun,
        forbiddenCapabilities,
        context,
        forbiddenMaterialDetected,
        verifiedScope);
  }

  /** 判断 command 是否仍与 immutable verified scope 精确一致。 */
  public boolean hasVerifiedExecutionScope() {
    return executionScope != null
        && tenantId != null
        && tenantId.equals(executionScope.tenantId())
        && environment != null
        && environment.equals(executionScope.environment().name());
  }
}
