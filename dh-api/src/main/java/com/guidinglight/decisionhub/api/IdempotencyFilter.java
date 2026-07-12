package com.guidinglight.decisionhub.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.common.api.ApiResponse;
import com.guidinglight.decisionhub.common.error.CommonErrorCodes;
import com.guidinglight.decisionhub.usecase.idempotency.IdempotencyStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

/**
 * HTTP 幂等键 filter。
 *
 * <p>Why：历史实现早于认证 filter 运行，并用固定 {@code t-default} 参与幂等 key，导致多租户公开入口可能互相
 * 污染幂等空间。当前实现排在 DH API 认证之后，只使用认证上下文 tenant；未认证请求直接交给后续认证层拒绝，不再写入
 * 固定默认 tenant 的幂等记录。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 30)
public class IdempotencyFilter extends OncePerRequestFilter {

  private static final String PERSISTENT_GUARD_ROUTE = "/api/ai/decision-dry-runs";

  private final IdempotencyStore store;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public IdempotencyFilter(IdempotencyStore store) {
    this.store = store;
  }

  /**
   * Stage-QDR-7 protected route由认证后的persistent state machine负责，禁止generic key-only filter提前占位。
   */
  @Override
  protected boolean shouldNotFilter(final HttpServletRequest request) {
    return PERSISTENT_GUARD_ROUTE.equals(request.getRequestURI());
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String key = request.getHeader("Idempotency-Key");
    if (key != null && !key.isBlank() && "POST".equalsIgnoreCase(request.getMethod())) {
      String tenantId = authenticatedTenantId(request);
      if (tenantId == null) {
        filterChain.doFilter(request, response);
        return;
      }
      boolean ok = store.tryPut(tenantId, key, Duration.ofMinutes(10));
      if (!ok) {
        String traceId = request.getAttribute(TraceIdFilter.TRACE_HEADER) == null ? null :
            String.valueOf(request.getAttribute(TraceIdFilter.TRACE_HEADER));
        ApiResponse<Object> body = ApiResponse.fail(CommonErrorCodes.CONFLICT.code(), "Duplicate request", traceId, null);
        response.setStatus(CommonErrorCodes.CONFLICT.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(body));
        return;
      }
    }
    filterChain.doFilter(request, response);
  }

  /**
   * 从认证 filter 写入的 request attribute 读取 tenant。
   *
   * <p>未认证时返回 {@code null}，让认证 filter 继续 fail-closed；这里不抛异常，避免幂等层抢先改写认证错误合同。
   */
  private static String authenticatedTenantId(final HttpServletRequest request) {
    final Object value = request.getAttribute(AuthenticatedRequest.TENANT_ID_ATTR);
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return value.toString();
  }
}
