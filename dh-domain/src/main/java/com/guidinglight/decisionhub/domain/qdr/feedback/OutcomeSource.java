package com.guidinglight.decisionhub.domain.qdr.feedback;

/**
 * 结构化结果观察的封闭来源集合。
 *
 * <p>来源缺失或未知时不得默认可信；Stage-QDR-8 不接收真实 Provider、NQ runtime、Paper 或 LIVE 来源。
 */
public enum OutcomeSource {
  /** 受保护的 deterministic mock dry-run 结果。 */
  DRY_RUN_RESULT,
  /** 既有 deterministic replay 的安全结果引用。 */
  DETERMINISTIC_REPLAY,
  /** 测试使用的结构化 fixture。 */
  STRUCTURED_TEST_FIXTURE
}
