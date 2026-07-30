package com.guidinglight.decisionhub.security.nq;

import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackEnvironment;
import java.time.Instant;

/**
 * Integration-1 limited dry-run endpoint 的 HMAC 校验输入。
 *
 * <p>Why：dry-run endpoint 虽然只返回只读 snapshot，但它已经是 runtime inbound API，必须把 method、path、
 * tenant、source、requestId、traceId、timestamp、nonce、schemaVersion 和原始 body hash 绑定到签名材料里，避免
 * 调用方复用旧签名篡改上下文或跨 endpoint 重放。
 *
 * @param method HTTP method，当前必须是 POST。
 * @param path endpoint path，当前必须是 /api/ai/decision-dry-runs。
 * @param sourceHeader canonical X-NQ-DH-Source header。
 * @param sourceSystem body.source。
 * @param authenticatedTenantId API bearer token 认证后的 tenant。
 * @param tenantId body.tenantId。
 * @param authenticatedEnvironment bearer authentication 建立的显式 DEV/TEST authority。
 * @param environment body.environment；必须是 signed canonical DEV 或 TEST。
 * @param timestampHeader canonical X-NQ-DH-Timestamp header，必须是 UTC Z。
 * @param nonce canonical X-NQ-DH-Nonce header。
 * @param signature canonical X-NQ-DH-Signature header。
 * @param requestId body.requestId。
 * @param traceId body.traceId。
 * @param schemaVersion body.schemaVersion。
 * @param rawBody 原始请求 body；只用于计算 SHA-256 hash，不应写入日志。
 * @param contentLength HTTP content length；未知时可为 -1。
 * @param now 当前时间，用于 ±clockSkew 窗口判定。
 */
public record NqDryRunAuthRequest(
    String method,
    String path,
    String sourceHeader,
    String sourceSystem,
    String authenticatedTenantId,
    String tenantId,
    FeedbackEnvironment authenticatedEnvironment,
    String environment,
    String timestampHeader,
    String nonce,
    String signature,
    String requestId,
    String traceId,
    String schemaVersion,
    String rawBody,
    long contentLength,
    Instant now) {}
