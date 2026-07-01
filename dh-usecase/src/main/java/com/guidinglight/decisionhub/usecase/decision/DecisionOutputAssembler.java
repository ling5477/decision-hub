package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionOutput;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyResult;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskLevel;
import com.guidinglight.decisionhub.domain.decision.DecisionRiskReview;
import com.guidinglight.decisionhub.domain.decision.ProviderSignalStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * K2 output assembler。
 *
 * <p>该类是 fail-closed 输出的唯一组装点：policy 拒绝、缺失 evidence、provider 失败、高风险和内部异常都转换为
 * ABSTAIN/BLOCKED，不抛出业务异常给调用方，不生成交易执行字段。
 */
public final class DecisionOutputAssembler {

  private static final String UNKNOWN_REQUEST = "unknown-request";
  private static final String UNKNOWN_TRACE = "unknown-trace";
  private static final String UNKNOWN_TENANT = "unknown-tenant";

  /**
   * 组装正常 policy 通过后的 K2 输出。
   *
   * @param context 只读上下文
   * @param policy policy 结果；非 allowed 会 fail-closed
   * @param signal mock provider signal
   * @param risk risk review 结果
   * @param createdAt 输出创建时间
   * @return K1 合同格式的 structured output
   */
  public DecisionOutput assemble(
      final DecisionContext context,
      final DecisionPolicyResult policy,
      final DecisionSignalResult signal,
      final DecisionRiskReview risk,
      final Instant createdAt) {
    final DecisionRequest request = context == null ? null : context.request();
    final List<String> evidenceRefs = context == null ? List.of() : context.evidenceRefs();
    if (policy == null || !policy.isAllowed()) {
      return policyDenied(request, policy, createdAt);
    }
    if (context == null || !context.hasEvidence()) {
      return noEvidence(request, createdAt);
    }
    if (signal == null) {
      return DecisionOutput.abstainForProviderFailure(
          requestId(request),
          traceId(request),
          tenantId(request),
          ProviderSignalStatus.FAILED,
          safeTime(createdAt));
    }
    if (signal.requiresAbstain()) {
      return DecisionOutput.abstainForProviderFailure(
          requestId(request), traceId(request), tenantId(request), signal.status(), safeTime(createdAt));
    }
    if (risk == null || risk.level() == DecisionRiskLevel.UNKNOWN) {
      return DecisionOutput.abstainForRisk(
          requestId(request),
          traceId(request),
          tenantId(request),
          DecisionRiskLevel.UNKNOWN,
          mergedReasonCodes(signal.reasonCodes(), risk, "UNKNOWN_RISK"),
          evidenceRefs,
          safeTime(createdAt));
    }
    if (risk.forbidsDirectionalBias()) {
      return DecisionOutput.abstainForRisk(
          requestId(request),
          traceId(request),
          tenantId(request),
          risk.level(),
          mergedReasonCodes(signal.reasonCodes(), risk, "RISK_BLOCKED"),
          evidenceRefs,
          safeTime(createdAt));
    }
    if (signal.action() == DecisionAction.LONG_BIAS || signal.action() == DecisionAction.SHORT_BIAS) {
      return DecisionOutput.directionalBias(
          requestId(request),
          traceId(request),
          tenantId(request),
          signal.action(),
          risk.level(),
          mergedReasonCodes(signal.reasonCodes(), risk, "DIRECTIONAL_BIAS"),
          evidenceRefs,
          safeTime(createdAt));
    }
    if (signal.action() == DecisionAction.OBSERVE || signal.action() == DecisionAction.NO_TRADE) {
      return DecisionOutput.observation(
          requestId(request),
          traceId(request),
          tenantId(request),
          signal.action(),
          risk.level(),
          mergedReasonCodes(signal.reasonCodes(), risk, "OBSERVATION_ONLY"),
          evidenceRefs,
          safeTime(createdAt));
    }
    return DecisionOutput.abstainForRisk(
        requestId(request),
        traceId(request),
        tenantId(request),
        risk.level(),
        mergedReasonCodes(signal.reasonCodes(), risk, "SIGNAL_ABSTAIN"),
        evidenceRefs,
        safeTime(createdAt));
  }

  /** 返回 policy 拒绝输出。 */
  public DecisionOutput policyDenied(
      final DecisionRequest request, final DecisionPolicyResult policy, final Instant createdAt) {
    final DecisionPolicyStatus status =
        policy == null ? DecisionPolicyStatus.BLOCKED : policy.status();
    final DecisionPolicyStatus safeStatus =
        status == DecisionPolicyStatus.ALLOWED ? DecisionPolicyStatus.BLOCKED : status;
    return DecisionOutput.blockedByPolicy(
        requestId(request), traceId(request), tenantId(request), safeStatus, safeTime(createdAt));
  }

  /** 返回缺失 evidence 的 fail-closed 输出。 */
  public DecisionOutput noEvidence(final DecisionRequest request, final Instant createdAt) {
    return DecisionOutput.abstainForNoEvidence(
        requestId(request), traceId(request), tenantId(request), safeTime(createdAt));
  }

  /** 将编排内部异常转换为结构化 ABSTAIN，避免调用方收到裸异常。 */
  public DecisionOutput unexpectedFailure(
      final DecisionRequest request, final RuntimeException error, final Instant createdAt) {
    return DecisionOutput.abstainForRisk(
        requestId(request),
        traceId(request),
        tenantId(request),
        DecisionRiskLevel.UNKNOWN,
        List.of("UNEXPECTED_FAILURE"),
        List.of(),
        safeTime(createdAt));
  }

  private static List<String> mergedReasonCodes(
      final List<String> signalReasons, final DecisionRiskReview risk, final String fallback) {
    final List<String> merged = new ArrayList<>();
    if (signalReasons != null) {
      merged.addAll(signalReasons);
    }
    if (risk != null && risk.reasonCodes() != null) {
      merged.addAll(risk.reasonCodes());
    }
    if (merged.isEmpty()) {
      merged.add(fallback);
    }
    return List.copyOf(merged);
  }

  private static String requestId(final DecisionRequest request) {
    return request == null ? UNKNOWN_REQUEST : request.getRequestId();
  }

  private static String traceId(final DecisionRequest request) {
    return request == null ? UNKNOWN_TRACE : request.getTraceId();
  }

  private static String tenantId(final DecisionRequest request) {
    return request == null ? UNKNOWN_TENANT : request.getTenantId();
  }

  private static Instant safeTime(final Instant createdAt) {
    return createdAt == null ? Instant.EPOCH : createdAt;
  }
}
