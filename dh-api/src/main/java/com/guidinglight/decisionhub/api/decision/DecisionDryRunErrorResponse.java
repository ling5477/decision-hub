package com.guidinglight.decisionhub.api.decision;

/**
 * limited dry-run endpoint 的统一 error envelope。
 *
 * <p>响应只包含 canonical error code、requestId、traceId 与 auditRef，不回显 signature、secret、raw body、
 * raw prompt、provider raw response 或 credential。
 *
 * @param error 固定为 DRY_RUN_REJECTED。
 * @param errorCode canonical error code。
 * @param message 脱敏摘要。
 * @param requestId requestId；安全上下文允许时返回。
 * @param traceId traceId；安全上下文允许时返回。
 * @param auditRef auditRef；audit 写失败时为空。
 */
public record DecisionDryRunErrorResponse(
    String error,
    String errorCode,
    String message,
    String requestId,
    String traceId,
    String auditRef) {}
