package com.guidinglight.decisionhub.usecase.decision;

import com.guidinglight.decisionhub.domain.decision.DecisionPolicyResult;
import com.guidinglight.decisionhub.domain.decision.DecisionPolicyStatus;
import com.guidinglight.decisionhub.domain.decision.DecisionRequest;
import com.guidinglight.decisionhub.domain.decision.DecisionSubject;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import java.util.List;
import java.util.Locale;

/**
 * K2 默认 policy checker。
 *
 * <p>策略只做 deterministic guard：请求必须是 READ_ONLY_RECOMMENDATION，且可自由文本引用不得携带执行、账户、凭证或订单
 * 意图。该类不扫描 symbol，避免把普通交易对名称误判为执行指令。
 */
public final class DefaultDecisionPolicyChecker implements DecisionPolicyChecker {

  private static final List<String> FORBIDDEN_TOKENS =
      List.of(
          "buy",
          "sell",
          "place_order",
          "cancel_order",
          "market_order",
          "limit_order",
          "placeorder",
          "cancelorder",
          "orderid",
          "accountid",
          "apisecret",
          "api_key",
          "secret",
          "passphrase");

  /**
   * 对 request 做只读边界检查。
   *
   * @param request K1 冻结请求；null 或非法请求返回 INVALID / DENIED
   * @return policy 检查结果
   */
  @Override
  public DecisionPolicyResult check(final DecisionRequest request) {
    if (request == null) {
      return new DecisionPolicyResult(DecisionPolicyStatus.INVALID, List.of("MISSING_REQUEST"));
    }
    if (request.getDecisionType() != DecisionType.READ_ONLY_RECOMMENDATION) {
      return new DecisionPolicyResult(
          DecisionPolicyStatus.DENIED, List.of("NON_READ_ONLY_DECISION_TYPE"));
    }
    if (isBlank(request.getRequestId())
        || isBlank(request.getTraceId())
        || isBlank(request.getTenantId())
        || isBlank(request.getSource())
        || request.getSubject() == null) {
      return new DecisionPolicyResult(
          DecisionPolicyStatus.INVALID, List.of("MISSING_REQUIRED_FIELD"));
    }
    if (containsForbiddenToken(request.getContextRef())
        || containsForbiddenSubjectReference(request.getSubject())) {
      return new DecisionPolicyResult(
          DecisionPolicyStatus.DENIED, List.of("FORBIDDEN_EXECUTION_INTENT"));
    }
    return new DecisionPolicyResult(DecisionPolicyStatus.ALLOWED, List.of("READ_ONLY_POLICY_OK"));
  }

  private static boolean containsForbiddenSubjectReference(final DecisionSubject subject) {
    return containsForbiddenToken(subject.strategyRef()) || containsForbiddenToken(subject.researchRef());
  }

  private static boolean containsForbiddenToken(final String value) {
    if (isBlank(value)) {
      return false;
    }
    final String normalized = value.toLowerCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
    return FORBIDDEN_TOKENS.stream().anyMatch(normalized::contains);
  }

  private static boolean isBlank(final String value) {
    return value == null || value.trim().isEmpty();
  }
}
