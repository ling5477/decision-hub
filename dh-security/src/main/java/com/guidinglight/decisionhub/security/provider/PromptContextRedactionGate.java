package com.guidinglight.decisionhub.security.provider;

/**
 * Prompt / context 出站脱敏闸门。
 *
 * <p>真实 provider 接入前，调用方必须通过该接口确认目标 provider 是否可接收当前上下文。
 */
public interface PromptContextRedactionGate {

  /**
   * 校验并返回可发送上下文。
   *
   * @param context 待发送上下文。
   * @param decision provider trust 判定结果。
   * @return 可发送上下文；当前最小实现保持原文或拒绝。
   */
  String assertAllowed(String context, ProviderTrustDecision decision);
}
