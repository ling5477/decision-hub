package com.guidinglight.decisionhub.domain.feedback.payload;

/**
 * Stage2-PoC-B1：paper run 告警级别。
 *
 * <p>语义与 SRE 通用告警分级保持一致，由 NQ 侧上报；DH 仅作经验权重与展示，不据此自动下单。
 */
public enum AlertLevel {
  /** 信息级，仅用于审计。 */
  INFO,
  /** 警告级，需关注但不一定立即处理。 */
  WARN,
  /** 错误级，需要排查。 */
  ERROR,
  /** 严重级，可能影响 paper run 继续运行。 */
  CRITICAL
}
