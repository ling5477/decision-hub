package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.util.List;
import java.util.Objects;

/**
 * K2 deterministic mock signal provider。
 *
 * <p>默认只返回 MOCKED + NO_TRADE，用于验证 orchestrator 骨架与 fail-closed 边界。构造器保留给测试注入不同 mock 状态，
 * 但明确拒绝 SUCCESS，避免把 K2 误写成真实 provider 成功路径。
 */
public final class MockDecisionSignalProvider implements DecisionSignalProvider {

  private final ProviderSignalStatus status;
  private final DecisionAction action;
  private final List<String> reasonCodes;

  /** 创建默认 mock provider：不表达方向性偏好。 */
  public MockDecisionSignalProvider() {
    this(ProviderSignalStatus.MOCKED, DecisionAction.NO_TRADE, List.of("MOCK_NO_TRADE"));
  }

  /**
   * 创建可测试的 deterministic mock provider。
   *
   * @param status provider 状态；K2 不允许 SUCCESS
   * @param action signal action；失败态必须为 ABSTAIN
   * @param reasonCodes signal 原因码
   */
  public MockDecisionSignalProvider(
      final ProviderSignalStatus status,
      final DecisionAction action,
      final List<String> reasonCodes) {
    this.status = Objects.requireNonNull(status, "status");
    if (this.status == ProviderSignalStatus.SUCCESS) {
      throw new IllegalArgumentException("K2 mock provider must not use SUCCESS status");
    }
    this.action = Objects.requireNonNull(action, "action");
    this.reasonCodes = reasonCodes == null ? List.of() : List.copyOf(reasonCodes);
  }

  /**
   * 返回预设 mock signal。
   *
   * @param context 只读上下文；K2 provider 不读取外部资源
   * @return deterministic provider signal
   */
  @Override
  public DecisionSignalResult signal(final DecisionContext context) {
    Objects.requireNonNull(context, "context");
    return new DecisionSignalResult(status, action, reasonCodes);
  }
}
