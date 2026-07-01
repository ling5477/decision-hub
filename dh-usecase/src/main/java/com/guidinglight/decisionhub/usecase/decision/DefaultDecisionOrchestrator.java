package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyResult;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskReview;
import java.time.Clock;
import java.util.Objects;

/**
 * K2 默认 orchestrator 骨架。
 *
 * <p>流程固定为 policy -> context -> mock signal -> risk -> output assembler。该实现无 Spring 装配、无 DB、
 * 无 HTTP、无 NQ runtime、无真实 provider、无审计持久化；异常统一 fail-closed。
 */
public final class DefaultDecisionOrchestrator implements DecisionOrchestrator {

  private final DecisionContextBuilder contextBuilder;
  private final DecisionPolicyChecker policyChecker;
  private final DecisionSignalProvider signalProvider;
  private final DecisionRiskReviewer riskReviewer;
  private final DecisionOutputAssembler outputAssembler;
  private final Clock clock;

  /** 使用 K2 默认 mock-only 组件创建 orchestrator。 */
  public DefaultDecisionOrchestrator() {
    this(
        new DefaultDecisionContextBuilder(),
        new DefaultDecisionPolicyChecker(),
        new MockDecisionSignalProvider(),
        new DefaultDecisionRiskReviewer(),
        new DecisionOutputAssembler(),
        Clock.systemUTC());
  }

  /**
   * 创建可测试的 orchestrator。
   *
   * @param contextBuilder 只读上下文构造器
   * @param policyChecker read-only policy checker
   * @param signalProvider deterministic mock provider
   * @param riskReviewer deterministic risk reviewer
   * @param outputAssembler fail-closed output assembler
   * @param clock 输出时间源
   */
  public DefaultDecisionOrchestrator(
      final DecisionContextBuilder contextBuilder,
      final DecisionPolicyChecker policyChecker,
      final DecisionSignalProvider signalProvider,
      final DecisionRiskReviewer riskReviewer,
      final DecisionOutputAssembler outputAssembler,
      final Clock clock) {
    this.contextBuilder = Objects.requireNonNull(contextBuilder, "contextBuilder");
    this.policyChecker = Objects.requireNonNull(policyChecker, "policyChecker");
    this.signalProvider = Objects.requireNonNull(signalProvider, "signalProvider");
    this.riskReviewer = Objects.requireNonNull(riskReviewer, "riskReviewer");
    this.outputAssembler = Objects.requireNonNull(outputAssembler, "outputAssembler");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /**
   * 执行 K2 只读编排，并保证异常路径也返回结构化 output。
   *
   * @param request K1 冻结请求合同；null 会被 policy 拒绝
   * @return read-only structured output
   */
  @Override
  public DecisionOutput decide(final DecisionRequest request) {
    try {
      final DecisionPolicyResult policy = policyChecker.check(request);
      if (!policy.isAllowed()) {
        return outputAssembler.policyDenied(request, policy, clock.instant());
      }
      final DecisionContext context = contextBuilder.build(request);
      if (!context.hasEvidence()) {
        return outputAssembler.noEvidence(request, clock.instant());
      }
      final DecisionSignalResult signal = signalProvider.signal(context);
      final DecisionRiskReview risk = riskReviewer.review(context, signal);
      return outputAssembler.assemble(context, policy, signal, risk, clock.instant());
    } catch (final RuntimeException error) {
      return outputAssembler.unexpectedFailure(request, error, clock.instant());
    }
  }
}
