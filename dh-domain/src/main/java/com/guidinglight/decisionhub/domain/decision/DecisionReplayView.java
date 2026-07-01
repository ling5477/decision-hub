package com.guidinglight.decisionhub.domain.decision;

import java.util.List;
import java.util.Objects;

/**
 * K4 Decision Replay Read Model 的聚合只读视图。
 *
 * <p>该视图只读取 K3 已落库的 DH-owned audit / snapshot / trace / output 数据，并给出 replay 查询状态。
 * 它不是 replay API，不会重跑 provider、orchestrator、LLM、NQ runtime 或任何交易执行。
 */
public record DecisionReplayView(
    DecisionReplayStatus replayStatus,
    String tenantId,
    String decisionId,
    String traceId,
    String requestId,
    DecisionReplayRequestView request,
    DecisionReplayContextView context,
    DecisionReplayOutputView output,
    DecisionReplayTimelineView timeline,
    List<String> reasonCodes) {

  /** 校验 replay 归属字段；非 FOUND 状态允许不携带明细，但必须携带 tenantId / decisionId。 */
  public DecisionReplayView {
    replayStatus = Objects.requireNonNull(replayStatus, "replayStatus");
    tenantId = DecisionReplaySafety.requireText(tenantId, "tenantId");
    decisionId = DecisionReplaySafety.requireText(decisionId, "decisionId");
    timeline = timeline == null ? DecisionReplayTimelineView.empty() : timeline;
    reasonCodes = DecisionReplaySafety.copyStrings(reasonCodes);
  }

  /** 返回完整 replay view。 */
  public static DecisionReplayView found(
      final DecisionReplayRequestView request,
      final DecisionReplayContextView context,
      final DecisionReplayOutputView output,
      final DecisionReplayTimelineView timeline) {
    final DecisionReplayRequestView checkedRequest = Objects.requireNonNull(request, "request");
    final DecisionReplayOutputView checkedOutput = Objects.requireNonNull(output, "output");
    return new DecisionReplayView(
        DecisionReplayStatus.FOUND,
        checkedRequest.tenantId(),
        checkedRequest.decisionId(),
        checkedRequest.traceId(),
        checkedOutput.requestId(),
        checkedRequest,
        Objects.requireNonNull(context, "context"),
        checkedOutput,
        Objects.requireNonNull(timeline, "timeline"),
        List.of());
  }

  /** decisionId 在当前 tenant 下没有任何 K3 记录。 */
  public static DecisionReplayView notFound(final String tenantId, final String decisionId) {
    return empty(DecisionReplayStatus.NOT_FOUND, tenantId, decisionId, "REPLAY_NOT_FOUND");
  }

  /** tenant 归属不一致，必须 fail-closed 且不返回任何明细。 */
  public static DecisionReplayView tenantMismatch(final String tenantId, final String decisionId) {
    return empty(DecisionReplayStatus.TENANT_MISMATCH, tenantId, decisionId, "TENANT_MISMATCH");
  }

  /** 查询到部分记录但不足以组成完整 replay。 */
  public static DecisionReplayView incomplete(
      final String tenantId,
      final String decisionId,
      final String traceId,
      final String requestId,
      final List<String> reasonCodes) {
    return new DecisionReplayView(
        DecisionReplayStatus.INCOMPLETE,
        tenantId,
        decisionId,
        traceId,
        requestId,
        null,
        null,
        null,
        DecisionReplayTimelineView.empty(),
        reasonCodes);
  }

  /** 已落库 JSON、枚举或时间字段不可安全解析。 */
  public static DecisionReplayView corrupted(
      final String tenantId, final String decisionId, final String reasonCode) {
    return empty(DecisionReplayStatus.CORRUPTED, tenantId, decisionId, reasonCode);
  }

  /** 查询输入非法或底层读取失败。 */
  public static DecisionReplayView blocked(
      final String tenantId, final String decisionId, final String reasonCode) {
    return empty(DecisionReplayStatus.BLOCKED, tenantId, decisionId, reasonCode);
  }

  /** 是否为可展示的完整 replay。 */
  public boolean isComplete() {
    return replayStatus == DecisionReplayStatus.FOUND;
  }

  private static DecisionReplayView empty(
      final DecisionReplayStatus status,
      final String tenantId,
      final String decisionId,
      final String reasonCode) {
    return new DecisionReplayView(
        status,
        tenantId,
        decisionId,
        null,
        null,
        null,
        null,
        null,
        DecisionReplayTimelineView.empty(),
        List.of(reasonCode));
  }
}
