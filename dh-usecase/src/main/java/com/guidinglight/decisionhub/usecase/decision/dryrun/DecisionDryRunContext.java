package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.time.Instant;
import java.util.List;

/**
 * limited dry-run request 中的只读 decisionContext 摘要。
 *
 * <p>该对象只承载脱敏后的分析上下文，不包含账户、订单、数量、杠杆、价格、credential、raw prompt 或 provider
 * raw response。API 层负责从 JSON 中提取这些字段并先执行 forbidden material 扫描。
 *
 * @param symbol 分析标的。
 * @param market 市场。
 * @param timeframe 时间周期。
 * @param strategyRef 可选策略引用。
 * @param researchRef 可选研究引用。
 * @param contextRef 可选上下文引用。
 * @param snapshotId 只读 snapshot id。
 * @param capturedAt snapshot 捕获时间。
 * @param evidenceRefs evidence 引用列表。
 * @param approxBytes decisionContext 近似字节数，用于 memory cap。
 */
public record DecisionDryRunContext(
    String symbol,
    String market,
    String timeframe,
    String strategyRef,
    String researchRef,
    String contextRef,
    String snapshotId,
    Instant capturedAt,
    List<String> evidenceRefs,
    int approxBytes) {

  /** 归一化 evidence refs，避免可变集合逃逸。 */
  public DecisionDryRunContext {
    evidenceRefs = evidenceRefs == null ? List.of() : List.copyOf(evidenceRefs);
  }
}
