package com.guidinglight.decisionhub.security.nq;

import java.time.Instant;

/**
 * NQ feedback 来源认证请求。
 *
 * <p>Why：签名校验必须绑定 envelope 的关键字段，避免攻击者只伪造 sourceSystem 字符串就把 payload
 * 写入 DH 经验链路。该对象不保存密钥，只携带 header 与请求体摘要所需字段。
 */
public record NqFeedbackAuthRequest(
    String sourceHeader,
    String sourceSystem,
    String timestampHeader,
    String nonce,
    String signature,
    String eventId,
    String requestId,
    String traceId,
    String payloadJson,
    long contentLength,
    Instant now) {}
