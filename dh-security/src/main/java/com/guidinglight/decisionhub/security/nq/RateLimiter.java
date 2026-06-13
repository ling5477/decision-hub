package com.guidinglight.decisionhub.security.nq;

import java.time.Instant;

/**
 * NQ feedback 入站限流端口（DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3）。
 *
 * <p>Why：在本批之前，{@code NqFeedbackController -> HmacNqFeedbackAuthenticator} 链路完全没有限流，
 * 持有合法 API token 的调用方可无限刷入站请求，放大签名/重放校验与入库成本。本端口提供一个独立于
 * HMAC 认证的限流闸门，作用点必须位于认证之前，使超限请求在进入 HMAC / ingestion 前被拒。
 *
 * <p>关键约束：
 *
 * <ol>
 *   <li>限流 key 至少包含 {@code source + tenantId + route}，保证租户 / 来源隔离，避免单租户拖垮其它租户。
 *   <li>实现只接收 source / tenant / route / now，<b>不读取 raw body、不读取 secret、不接触签名材料</b>。
 *   <li>限流是认证之外的独立结果（{@link RateLimitResult}），不得伪装成 HMAC 失败，也不得污染 {@link
 *       NqFeedbackAuthResult}。
 *   <li>实现必须有界且 fail-closed：容量耗尽或配置非法时拒绝 / 启动失败，绝不 fail-open。
 * </ol>
 */
public interface RateLimiter {

  /**
   * 校验给定 (source, tenant, route) 在当前时刻是否允许通过。
   *
   * @param source 调用方来源标识（如 NQ source header）；可为 null/blank（实现按 fail-closed 语义归一化，不得合并成无界 key）。
   * @param tenantId 已认证租户 id；限流桶必须按租户隔离。
   * @param route 逻辑路由标识（如 {@code NQ_FEEDBACK}）；固定值，不读取 raw path query。
   * @param now 当前时刻；不可为 null（实现可在 null 时回退自身时钟，但调用方应显式传入以便测试）。
   * @return 限流结果；{@link RateLimitResult#allowed()} 为 false 时 controller 映射 HTTP 429 RATE_LIMITED。
   */
  RateLimitResult check(String source, String tenantId, String route, Instant now);
}
