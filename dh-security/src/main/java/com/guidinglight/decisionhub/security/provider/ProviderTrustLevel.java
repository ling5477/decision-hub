package com.guidinglight.decisionhub.security.provider;

/**
 * Provider / relay 信任等级。
 *
 * <p>UNKNOWN 与 UNTRUSTED_RELAY 必须默认拒绝，避免未来 OpenAI-compatible 中转站在未评审时接收完整上下文。
 */
public enum ProviderTrustLevel {
  /** 官方 API，且 baseURL 明确在 allowlist 中。 */
  OFFICIAL_API,
  /** 自建网关，且由 DH/NQ 团队控制。 */
  SELF_HOSTED_GATEWAY,
  /** 受控 relay，必须有额外审计和脱敏。 */
  CONTROLLED_RELAY,
  /** 不可信第三方 relay。 */
  UNTRUSTED_RELAY,
  /** 未知或未配置 provider。 */
  UNKNOWN
}
