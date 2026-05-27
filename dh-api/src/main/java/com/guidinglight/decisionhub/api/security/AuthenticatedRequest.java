package com.guidinglight.decisionhub.api.security;

import com.guidinglight.decisionhub.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponseException;

/**
 * API 认证上下文读取工具。
 *
 * <p>Why：controller 只能从已校验的 request attribute 读取 tenant，禁止继续使用未认证 header 或硬编码
 * default tenant 作为正式请求上下文。
 */
public final class AuthenticatedRequest {

  public static final String AUTH_CONTEXT_ATTR = "decisionhub.auth.context";
  public static final String TENANT_ID_ATTR = "decisionhub.auth.tenantId";
  public static final String TENANT_HEADER = "X-DH-Tenant-Id";

  private AuthenticatedRequest() {}

  /**
   * 读取已认证 tenant。
   *
   * @param request 当前 HTTP 请求。
   * @return 由认证 filter 写入的 tenantId。
   * @throws ErrorResponseException 未认证时抛出 401。
   */
  public static String requireTenantId(final HttpServletRequest request) {
    final Object value = request.getAttribute(TENANT_ID_ATTR);
    if (value == null || value.toString().isBlank()) {
      throw new ErrorResponseException(HttpStatus.UNAUTHORIZED);
    }
    return value.toString();
  }

  /**
   * 校验 run tenant 与调用方 tenant 一致。
   *
   * @param expectedTenant 调用方 tenant。
   * @param actualTenant 资源所属 tenant。
   * @throws ErrorResponseException tenant 不匹配时抛出 403。
   */
  public static void requireTenantMatch(final String expectedTenant, final String actualTenant) {
    if (expectedTenant == null || actualTenant == null || !expectedTenant.equals(actualTenant)) {
      throw new ErrorResponseException(HttpStatus.FORBIDDEN);
    }
  }

  /** 读取完整认证上下文；仅测试或审计需要。 */
  public static AuthContext authContext(final HttpServletRequest request) {
    final Object value = request.getAttribute(AUTH_CONTEXT_ATTR);
    return value instanceof AuthContext ctx ? ctx : null;
  }
}
