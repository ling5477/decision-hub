package com.guidinglight.decisionhub.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.api.security.DhApiAuthenticationFilter;
import com.guidinglight.decisionhub.usecase.idempotency.IdempotencyStore;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.annotation.Order;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** DH-CODE-REALITY-AUDIT-FIX-PACK：IdempotencyFilter 认证顺序与 tenant key 回归。 */
final class IdempotencyFilterSecurityTest {

  @Test
  void idempotencyFilterRunsAfterDhApiAuthenticationFilter() {
    final Order authOrder = AnnotationUtils.findAnnotation(DhApiAuthenticationFilter.class, Order.class);
    final Order idempotencyOrder = AnnotationUtils.findAnnotation(IdempotencyFilter.class, Order.class);

    assertTrue(authOrder != null && idempotencyOrder != null, "both filters must declare order");
    assertTrue(
        idempotencyOrder.value() > authOrder.value(),
        "idempotency must see authenticated tenant written by DhApiAuthenticationFilter");
  }

  @Test
  void postWithAuthenticatedTenantUsesRequestTenantForIdempotencyKey() throws Exception {
    final RecordingStore store = new RecordingStore(true);
    final IdempotencyFilter filter = new IdempotencyFilter(store);
    final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/ai/research-runs");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Idempotency-Key", "idem-1");
    request.setAttribute(AuthenticatedRequest.TENANT_ID_ATTR, "tenant-a");

    filter.doFilter(request, response, new MockFilterChain());

    assertEquals("tenant-a", store.tenantId);
    assertEquals("idem-1", store.key);
    assertEquals(200, response.getStatus());
  }

  @Test
  void postWithoutAuthenticatedTenantDoesNotWriteDefaultTenantKey() throws Exception {
    final RecordingStore store = new RecordingStore(true);
    final IdempotencyFilter filter = new IdempotencyFilter(store);
    final MockHttpServletRequest request = new MockHttpServletRequest("POST", "/legacy/runs");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Idempotency-Key", "idem-2");

    filter.doFilter(request, response, new MockFilterChain());

    assertNull(store.tenantId, "unauthenticated request must not use t-default or any synthetic tenant");
    assertNull(store.key);
    assertEquals(200, response.getStatus());
  }

  @Test
  void protectedDryRunRouteSkipsGenericKeyOnlyIdempotencyStore() throws Exception {
    final RecordingStore store = new RecordingStore(true);
    final IdempotencyFilter filter = new IdempotencyFilter(store);
    final MockHttpServletRequest request =
        new MockHttpServletRequest("POST", "/api/ai/decision-dry-runs");
    final MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader("Idempotency-Key", "must-not-be-consumed");
    request.setAttribute(AuthenticatedRequest.TENANT_ID_ATTR, "tenant-a");

    filter.doFilter(request, response, new MockFilterChain());

    assertNull(store.tenantId);
    assertNull(store.key);
    assertEquals(200, response.getStatus());
  }

  /** 记录幂等写入参数，避免测试依赖真实 store 的时间清理行为。 */
  private static final class RecordingStore implements IdempotencyStore {
    private final boolean result;
    private String tenantId;
    private String key;

    RecordingStore(final boolean result) {
      this.result = result;
    }

    @Override
    public boolean tryPut(final String tenantId, final String key, final Duration ttl) {
      this.tenantId = tenantId;
      this.key = key;
      return result;
    }
  }
}
