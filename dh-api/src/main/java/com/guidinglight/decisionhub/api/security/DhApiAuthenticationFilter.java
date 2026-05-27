package com.guidinglight.decisionhub.api.security;

import com.guidinglight.decisionhub.security.AuthContext;
import com.guidinglight.decisionhub.security.TokenVerifier;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * DH AI API 最小认证 filter。
 *
 * <p>保护范围只覆盖本轮 P1 指定端点：ResearchRun API 与 NQ feedback API。认证结果写入 request
 * attribute，controller 只能从该可信上下文解析 tenant。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public final class DhApiAuthenticationFilter extends OncePerRequestFilter {

  private static final String AUTHORIZATION = "Authorization";
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String RESEARCH_PATH = "/api/ai/research-runs";
  private static final String NQ_FEEDBACK_PATH = "/api/ai/feedback/nq";

  private final TokenVerifier tokenVerifier;

  /** 构造认证 filter。 */
  public DhApiAuthenticationFilter(final TokenVerifier tokenVerifier) {
    this.tokenVerifier = Objects.requireNonNull(tokenVerifier, "tokenVerifier");
  }

  @Override
  protected boolean shouldNotFilter(final HttpServletRequest request) {
    final String path = request.getRequestURI();
    return !(path.equals(RESEARCH_PATH)
        || path.startsWith(RESEARCH_PATH + "/")
        || path.equals(NQ_FEEDBACK_PATH));
  }

  @Override
  protected void doFilterInternal(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final FilterChain filterChain)
      throws ServletException, IOException {
    final String token = extractBearerToken(request);
    final AuthContext authContext = tokenVerifier.verify(token);
    if (authContext == null || isBlank(authContext.tenantId())) {
      response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
      return;
    }
    final String requestedTenant = request.getHeader(AuthenticatedRequest.TENANT_HEADER);
    if (!isBlank(requestedTenant) && !authContext.tenantId().equals(requestedTenant)) {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
      return;
    }
    request.setAttribute(AuthenticatedRequest.AUTH_CONTEXT_ATTR, authContext);
    request.setAttribute(AuthenticatedRequest.TENANT_ID_ATTR, authContext.tenantId());
    filterChain.doFilter(request, response);
  }

  private static String extractBearerToken(final HttpServletRequest request) {
    final String header = request.getHeader(AUTHORIZATION);
    if (header == null || !header.startsWith(BEARER_PREFIX)) {
      return null;
    }
    return header.substring(BEARER_PREFIX.length()).trim();
  }

  private static boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }
}
