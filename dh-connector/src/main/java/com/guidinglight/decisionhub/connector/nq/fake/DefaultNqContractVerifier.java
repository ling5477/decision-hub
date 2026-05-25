package com.guidinglight.decisionhub.connector.nq.fake;

import com.guidinglight.decisionhub.connector.nq.NqContractVerifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stage1：默认契约校验器。
 *
 * <p>规则：必须包含 traceId、runId、candidateId、requestType；不允许包含 placeOrder/forceExecute 等危险字段。
 */
public final class DefaultNqContractVerifier implements NqContractVerifier {

  private static final List<String> REQUIRED_FIELDS =
      List.of("traceId", "runId", "candidateId", "requestType");

  private static final List<String> FORBIDDEN_FIELDS =
      List.of("placeOrder", "forceExecute", "bypassRisk");

  @Override
  public List<String> verify(final Map<String, Object> request) {
    final List<String> issues = new ArrayList<>();
    if (request == null) {
      issues.add("request is null");
      return issues;
    }
    for (String field : REQUIRED_FIELDS) {
      if (!request.containsKey(field)) {
        issues.add("missing required field: " + field);
      }
    }
    for (String field : FORBIDDEN_FIELDS) {
      if (request.containsKey(field)) {
        issues.add("forbidden field present: " + field);
      }
    }
    return issues;
  }
}
