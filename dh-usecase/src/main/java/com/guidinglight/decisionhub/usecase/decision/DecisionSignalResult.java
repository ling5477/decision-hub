package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.ProviderDecisionSignal;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.util.List;
import java.util.Objects;

/**
 * K2 provider signal 的 usecase 层值对象。
 *
 * <p>该结果只表达推荐姿态和 provider 状态，不包含模型输出原文、HTTP 响应、凭证、账户或订单字段。不可变设计保证单次编排内
 * 可安全传递。
 */
public record DecisionSignalResult(
    ProviderSignalStatus status, DecisionAction action, List<String> reasonCodes) {

  /** 规范化 signal，并强制失败态只允许 ABSTAIN。 */
  public DecisionSignalResult {
    status = Objects.requireNonNull(status, "status");
    action = Objects.requireNonNull(action, "action");
    reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
    if (requiresAbstain(status) && action != DecisionAction.ABSTAIN) {
      throw new IllegalArgumentException("provider failure status must use ABSTAIN action");
    }
  }

  /** 创建 deterministic mock signal；K2 禁止 SUCCESS provider runtime。 */
  public static DecisionSignalResult mock(
      final DecisionAction action, final List<String> reasonCodes) {
    return new DecisionSignalResult(ProviderSignalStatus.MOCKED, action, reasonCodes);
  }

  /** 创建 provider 失败 signal；输出层必须 fail-closed 为 ABSTAIN。 */
  public static DecisionSignalResult failure(
      final ProviderSignalStatus status, final String reasonCode) {
    return new DecisionSignalResult(status, DecisionAction.ABSTAIN, List.of(reasonCode));
  }

  /** 返回该 provider 状态是否要求 fail-closed。 */
  public boolean requiresAbstain() {
    return requiresAbstain(status);
  }

  private static boolean requiresAbstain(final ProviderSignalStatus status) {
    return new ProviderDecisionSignal(status, List.of()).requiresAbstain();
  }
}
