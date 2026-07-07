package com.guidinglight.decisionhub.usecase.decision;

/**
 * K3 decision trace 步骤名。
 *
 * <p>这些步骤只描述 DH 内部只读编排，不代表真实 provider、NQ runtime、交易或 LangGraph runtime 已启动。
 */
public enum DecisionTraceStepName {
  /** Policy 与只读边界校验。 */
  POLICY_CHECK,

  /** 从 request 的 contextSnapshot 复制只读 evidence 引用。 */
  CONTEXT_BUILD,

  /** Deterministic mock provider signal 生成。 */
  MOCK_PROVIDER_SIGNAL,

  /** stage-qdr-3 B4 mock ModelGateway 调用；不代表真实 provider 或 HTTP 已接入。 */
  MODEL_GATEWAY_MOCK_CALL,

  /** 输出前风险审查。 */
  RISK_REVIEW,

  /** DecisionOutput 组装与落库。 */
  OUTPUT_WRITE,

  /** Audit event 组装与落库。 */
  AUDIT_WRITE
}
