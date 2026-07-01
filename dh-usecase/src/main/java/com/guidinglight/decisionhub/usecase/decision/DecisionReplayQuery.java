package com.guidinglight.decisionhub.usecase.decision;

import java.util.Objects;

/**
 * K4 replay read model 查询条件。
 *
 * <p>tenantId 与 decisionId 是强制条件；traceId / requestId 只作为额外一致性校验，不允许扩大查询范围或执行跨租户读取。
 */
public record DecisionReplayQuery(
    String tenantId, String decisionId, String traceId, String requestId) {

  /** 校验主查询条件，保证 repository 只能按 tenant + decision 读取 K3 记录。 */
  public DecisionReplayQuery {
    tenantId = requireText(tenantId, "tenantId");
    decisionId = requireText(decisionId, "decisionId");
    traceId = optionalText(traceId);
    requestId = optionalText(requestId);
  }

  private static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }

  private static String optionalText(final String value) {
    if (value == null) {
      return null;
    }
    final String checked = value.trim();
    return checked.isEmpty() ? null : checked;
  }
}
