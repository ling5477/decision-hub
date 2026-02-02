package com.guidinglight.decisionhub.api;

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

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class IdempotencyFilter extends OncePerRequestFilter {

  private final IdempotencyStore store;

  public IdempotencyFilter(IdempotencyStore store) {
    this.store = store;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String key = request.getHeader("Idempotency-Key");
    if (key != null && !key.isBlank() && "POST".equalsIgnoreCase(request.getMethod())) {
      String tenantId = "t-default";
      boolean ok = store.tryPut(tenantId, key, Duration.ofMinutes(10));
      if (!ok) {
        String traceId = request.getAttribute(TraceIdFilter.TRACE_HEADER) == null ? null :
            String.valueOf(request.getAttribute(TraceIdFilter.TRACE_HEADER));
        ApiResponse<Object> body = ApiResponse.fail(CommonErrorCodes.CONFLICT.code(), "Duplicate request", traceId, null);
        response.setStatus(CommonErrorCodes.CONFLICT.httpStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(body));
        return;
      }
    }
    filterChain.doFilter(request, response);
  }
}
