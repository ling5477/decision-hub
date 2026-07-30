package com.guidinglight.decisionhub.domain.qdr.feedback;

/**
 * 结构化反馈允许使用的隔离环境。
 *
 * <p>Stage-QDR-8 仅允许开发与测试环境，其他环境必须在枚举解析或构造阶段 fail-closed。
 */
public enum FeedbackEnvironment {
  /** 开发环境。 */
  DEV,
  /** 测试环境。 */
  TEST
}
