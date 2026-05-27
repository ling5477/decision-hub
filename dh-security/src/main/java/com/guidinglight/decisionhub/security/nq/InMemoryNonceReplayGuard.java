package com.guidinglight.decisionhub.security.nq;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 进程内 nonce 防重放实现。
 *
 * <p>Why：DH-AUDIT-FIX 只需要最小可验证防护，不引入 Redis 或数据库依赖。生产多实例部署时应替换为
 * 共享存储实现，否则只能防同一实例内的重放。当前实现不做主动清理，避免因本机时钟与请求时间基准不一致导致刚登记的 replay key 被误删。
 */
public final class InMemoryNonceReplayGuard implements NonceReplayGuard {

  private final Map<String, Instant> seen = new ConcurrentHashMap<>();

  @Override
  public boolean markIfAbsent(final String replayKey, final Instant expiresAt) {
    return seen.putIfAbsent(replayKey, expiresAt) == null;
  }
}
