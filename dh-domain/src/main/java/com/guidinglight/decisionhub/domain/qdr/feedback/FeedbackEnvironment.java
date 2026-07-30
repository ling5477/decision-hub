package com.guidinglight.decisionhub.domain.qdr.feedback;

import java.util.Locale;

/**
 * 结构化反馈允许使用的隔离环境。
 *
 * <p>Stage-QDR-8 仅允许开发与测试环境，其他环境必须在枚举解析或构造阶段 fail-closed。
 */
public enum FeedbackEnvironment {
  /** 开发环境。 */
  DEV,
  /** 测试环境。 */
  TEST;

  /**
   * 解析 signed wire value；不接受 alias、空白归一化或默认环境。
   *
   * @param wireValue 调用方签名覆盖的精确环境值
   * @return canonical DEV/TEST 环境
   * @throws FeedbackExecutionScopeException 环境缺失或不受支持
   */
  public static FeedbackEnvironment fromWire(final String wireValue) {
    if (wireValue == null || wireValue.isBlank()) {
      throw new FeedbackExecutionScopeException("feedback environment is required");
    }
    try {
      return FeedbackEnvironment.valueOf(wireValue);
    } catch (final IllegalArgumentException error) {
      throw new FeedbackExecutionScopeException("feedback environment is not supported", error);
    }
  }

  /**
   * 解析既有 persistent guard 配置；仅允许显式 dev/test，staging/prod 不得映射。
   *
   * @param configuredValue guard environment 配置值
   * @return canonical 环境；配置不属于 dev/test 时返回 null 以保持 fail-closed
   */
  public static FeedbackEnvironment fromGuardConfiguration(final String configuredValue) {
    if (configuredValue == null) {
      return null;
    }
    return switch (configuredValue.toLowerCase(Locale.ROOT)) {
      case "dev" -> DEV;
      case "test" -> TEST;
      default -> null;
    };
  }

  /** 返回既有 V12 persistent identity 使用的小写稳定值。 */
  public String persistentValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
