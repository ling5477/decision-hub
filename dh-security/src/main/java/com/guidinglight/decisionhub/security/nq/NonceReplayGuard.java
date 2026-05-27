package com.guidinglight.decisionhub.security.nq;

import java.time.Instant;

/**
 * NQ feedback 防重放端口。
 *
 * <p>实现必须以原子方式登记 nonce/requestId 组合；返回 false 表示该组合已经出现过，应拒绝重放。
 */
public interface NonceReplayGuard {

  /**
   * 原子登记 replay key。
   *
   * @param replayKey 来源系统、nonce 与 requestId 组合后的稳定 key。
   * @param expiresAt 该 key 的过期时间；内存实现可用它做懒清理。
   * @return true 表示首次出现；false 表示重放。
   */
  boolean markIfAbsent(String replayKey, Instant expiresAt);
}
