package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionReplayStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import java.util.Objects;

/**
 * K4 replay read model 默认实现。
 *
 * <p>该实现只依赖 repository port，保持 usecase 层不依赖 JDBC。任何输入非法、repository 异常、跨租户数据或可选
 * trace/request 一致性失败都会返回结构化 fail-closed 结果，而不是向调用方抛普通 RuntimeException。
 */
public final class DefaultDecisionReplayQueryService implements DecisionReplayQueryService {

  private final DecisionReplayQueryRepository repository;

  /**
   * 创建 replay 查询服务。
   *
   * @param repository K4 replay read repository port；实现必须只读。
   */
  public DefaultDecisionReplayQueryService(final DecisionReplayQueryRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  @Override
  public DecisionReplayView replay(
      final String tenantId,
      final String decisionId,
      final String traceId,
      final String requestId) {
    final DecisionReplayQuery query;
    try {
      query = new DecisionReplayQuery(tenantId, decisionId, traceId, requestId);
    } catch (final RuntimeException error) {
      return DecisionReplayView.blocked(
          safeText(tenantId), safeText(decisionId), "INVALID_REPLAY_QUERY");
    }

    try {
      final DecisionReplayView view = repository.findReplay(query);
      return normalize(query, view);
    } catch (final RuntimeException error) {
      return DecisionReplayView.blocked(query.tenantId(), query.decisionId(), "REPLAY_QUERY_FAILED");
    }
  }

  private static DecisionReplayView normalize(
      final DecisionReplayQuery query, final DecisionReplayView view) {
    if (view == null) {
      return DecisionReplayView.blocked(query.tenantId(), query.decisionId(), "REPLAY_QUERY_NULL");
    }
    if (view.replayStatus() == DecisionReplayStatus.FOUND
        || view.replayStatus() == DecisionReplayStatus.INCOMPLETE) {
      if (!query.tenantId().equals(view.tenantId())) {
        return DecisionReplayView.tenantMismatch(query.tenantId(), query.decisionId());
      }
      if (query.traceId() != null && view.traceId() != null && !query.traceId().equals(view.traceId())) {
        return DecisionReplayView.blocked(
            query.tenantId(), query.decisionId(), "TRACE_ID_MISMATCH");
      }
      if (query.requestId() != null
          && view.requestId() != null
          && !query.requestId().equals(view.requestId())) {
        return DecisionReplayView.blocked(
            query.tenantId(), query.decisionId(), "REQUEST_ID_MISMATCH");
      }
    }
    return view;
  }

  private static String safeText(final String value) {
    if (value == null || value.trim().isEmpty()) {
      return "UNKNOWN";
    }
    return value.trim();
  }
}
