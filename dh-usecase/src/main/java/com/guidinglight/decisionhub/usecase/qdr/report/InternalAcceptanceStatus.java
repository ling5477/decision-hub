package com.guidinglight.decisionhub.usecase.qdr.report;

/**
 * Stage-QDR-6 B4 internal evidence acceptance 的冻结状态。
 *
 * <p>所有状态都只用于内部 evidence/replay 验收，不授权 Provider、NQ、交易、执行、Paper 或 LIVE。
 */
public enum InternalAcceptanceStatus {
  /** 所有必需结构化证据均满足冻结规则。 */
  ACCEPTED,
  /** 输入完整但 replay、regression、readiness 或 observability 未通过。 */
  REJECTED,
  /** 缺少 mandatory evidence 或必需结构化输入。 */
  INCOMPLETE,
  /** tenant、correlation、安全合同或 replay input 无效。 */
  INVALID,
  /** replay schema、canonicalization 或 executor version 不受支持。 */
  UNSUPPORTED,
  /** source、转换或 replay execution 失败。 */
  FAILED;

  /**
   * Internal acceptance 永远不授权 Provider。
   *
   * @return 固定为 {@code false}。
   */
  public boolean authorizesProvider() {
    return false;
  }

  /**
   * Internal acceptance 永远不授权 NQ integration。
   *
   * @return 固定为 {@code false}。
   */
  public boolean authorizesNqIntegration() {
    return false;
  }

  /**
   * Internal acceptance 永远不授予交易或执行权限。
   *
   * @return 固定为 {@code false}。
   */
  public boolean allowsTradingOrExecution() {
    return false;
  }

  /**
   * Internal acceptance 永远不启用 Paper 或 LIVE。
   *
   * @return 固定为 {@code false}。
   */
  public boolean enablesPaperOrLive() {
    return false;
  }
}
