package com.guidinglight.decisionhub.security.provider;

/**
 * Provider 出站信任闸门。
 *
 * <p>所有未来真实 LLM provider 或 OpenAI-compatible client 在发出上下文前必须先通过该策略。
 */
public interface ProviderTrustPolicy {

  /**
   * 判断 provider/baseURL 是否允许接收上下文。
   *
   * @param provider provider 标识，仅用于审计。
   * @param baseUrl provider 或 gateway 的 baseURL。
   * @return trust decision；调用方必须拒绝 allowed=false 的结果。
   */
  ProviderTrustDecision evaluate(String provider, String baseUrl);
}
