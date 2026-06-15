package com.guidinglight.decisionhub.api.feedback;

import com.guidinglight.decisionhub.api.TraceIdFilter;
import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.security.nq.NormalizedNqDhHeaders;
import com.guidinglight.decisionhub.security.nq.NqDhHeaderParser;
import com.guidinglight.decisionhub.security.nq.NqDhHeaderValidationResult;
import com.guidinglight.decisionhub.security.nq.NqDhHeaderValidator;
import com.guidinglight.decisionhub.security.nq.NqFeedbackAuthRequest;
import com.guidinglight.decisionhub.security.nq.NqFeedbackAuthResult;
import com.guidinglight.decisionhub.security.nq.NqFeedbackAuthenticator;
import com.guidinglight.decisionhub.security.nq.RateLimitResult;
import com.guidinglight.decisionhub.security.nq.RateLimiter;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage2-PoC-B2：NQ 反馈事件入口（正式 envelope 契约）。
 *
 * <p>路径与 Stage1 保持一致：{@code POST /api/ai/feedback/nq}；不新增任何 feedback 路径。
 *
 * <p>响应：
 *
 * <ul>
 *   <li>202 Accepted + {@link NqFeedbackAcceptedResponse}（首次接收或幂等重放）。
 *   <li>400 Bad Request + {@link NqFeedbackErrorResponse}（envelope schema / eventType / schemaVersion /
 *       traceId / payload 校验失败）。
 * </ul>
 */
@RestController
@RequestMapping("/api/ai/feedback")
public final class NqFeedbackController {

  private static final Logger log = LoggerFactory.getLogger(NqFeedbackController.class);

  /** 固定逻辑路由标识，作为限流 key 的 route 维度；不读取 raw path / query。 */
  private static final String NQ_FEEDBACK_ROUTE = "NQ_FEEDBACK";

  /**
   * header 解析器（DH-NQ-HEADER-ALIGNMENT Batch 2）：集中 header 名 + 归一化模型。
   * Batch 2 起 <b>canonical-only</b>：仅按 canonical {@code X-NQ-DH-*} 读取，不接收 legacy {@code X-DH-NQ-*}、
   * 不做双接收。无状态可复用。
   */
  private final NqDhHeaderParser headerParser = new NqDhHeaderParser();

  /**
   * canonical header binding 校验器（DH-NQ-HEADER-ALIGNMENT Batch 3）：校验 canonical
   * {@code X-NQ-DH-Tenant-Id/Request-Id/Trace-Id} 与权威来源（tenant=认证上下文，requestId/traceId=body）一致；
   * 若提供且不一致则 fail-closed（403 {@code HEADER_BINDING_MISMATCH}）。<b>header 绝不覆盖权威来源</b>。无状态可复用。
   */
  private final NqDhHeaderValidator headerValidator = new NqDhHeaderValidator();

  private final NqFeedbackIngestionService ingestionService;
  private final NqFeedbackAuthenticator feedbackAuthenticator;
  private final RateLimiter rateLimiter;

  /** 构造 NQ feedback controller。 */
  public NqFeedbackController(
      final NqFeedbackIngestionService ingestionService,
      final NqFeedbackAuthenticator feedbackAuthenticator,
      final RateLimiter rateLimiter) {
    this.ingestionService = ingestionService;
    this.feedbackAuthenticator = feedbackAuthenticator;
    this.rateLimiter = rateLimiter;
  }

  /** 接收一条 NQ envelope 事件。 */
  @PostMapping("/nq")
  public ResponseEntity<Object> receive(
      @Valid @RequestBody final NqFeedbackEnvelopeRequest req,
      final HttpServletRequest httpRequest) {

    final String tenantId = AuthenticatedRequest.requireTenantId(httpRequest);
    final String httpTraceId = resolveHttpTraceId(httpRequest);

    // Batch 2：canonical-only 读取 canonical X-NQ-DH-* header 族；不再读取 legacy X-DH-NQ-*、不做双接收。
    // 仅 legacy header 存在时 source/timestamp/nonce/signature 均为 null，等同缺失 canonical -> 由 authenticator fail-closed 拒绝。
    final NormalizedNqDhHeaders nqHeaders = headerParser.parseCanonical(httpRequest::getHeader);

    // 限流必须前置于 HMAC authenticator：超限请求在进入签名 / 重放校验 / 入库前即被拒，降低被刷成本。
    // key = source + tenant + route（租户 / 来源隔离）；超限映射 429 RATE_LIMITED，且不暴露阈值 / 窗口 / 计数。
    final String nqSource = nqHeaders.source();
    final RateLimitResult rateLimit =
        rateLimiter.check(nqSource, tenantId, NQ_FEEDBACK_ROUTE, TimeProvider.now());
    if (!rateLimit.allowed()) {
      // 审计可观测：记录 RATE_LIMITED 分支被触发；不输出阈值 / 窗口 / 计数 / 密钥 / 签名材料。
      log.warn(
          "nq feedback rate limited, auditCode={}, tenantId={}, source={}, route={}, traceId={}",
          rateLimit.auditCode(),
          tenantId,
          nonNull(nqSource, "absent"),
          NQ_FEEDBACK_ROUTE,
          nonNull(httpTraceId, req.getTraceId()));
      return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
          .header(TraceIdFilter.TRACE_HEADER, nonNull(httpTraceId, req.getTraceId()))
          .body(
              (Object)
                  new NqFeedbackErrorResponse(
                      "RATE_LIMITED",
                      rateLimit.reason(),
                      "NQ feedback request rate limit exceeded",
                      req.getEventId(),
                      req.getTraceId(),
                      req.getCorrelationId()));
    }

    final NqFeedbackAuthResult authResult = authenticateNqSource(req, nqHeaders, httpRequest);
    if (!authResult.allowed()) {
      return ResponseEntity.status(authResult.status())
          .header(TraceIdFilter.TRACE_HEADER, nonNull(httpTraceId, req.getTraceId()))
          .body(
              (Object)
                  new NqFeedbackErrorResponse(
                      "UNAUTHORIZED_NQ_FEEDBACK",
                      authResult.reason(),
                      "NQ feedback source authentication failed",
                      req.getEventId(),
                      req.getTraceId(),
                      req.getCorrelationId()));
    }
    // Batch 3：canonical Tenant/Request/Trace 与权威来源（tenant=认证上下文，requestId/traceId=body）binding 一致性校验。
    // 三个 header 可选；若提供则必须一致，否则 fail-closed（403 HEADER_BINDING_MISMATCH）。绝不以 header 覆盖权威来源。
    final NqDhHeaderValidationResult headerBinding =
        headerValidator.validate(nqHeaders, tenantId, req.getRequestId(), req.getTraceId());
    if (!headerBinding.valid()) {
      // 审计：仅记 auditCode + 安全字段；不记 header 原值 / 权威值 / signature / secret / full body。
      log.warn(
          "nq feedback header binding mismatch, auditCode={}, tenantId={}, source={}, route={}, traceId={}",
          headerBinding.auditCode(),
          tenantId,
          nonNull(nqHeaders.source(), "absent"),
          NQ_FEEDBACK_ROUTE,
          nonNull(httpTraceId, req.getTraceId()));
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .header(TraceIdFilter.TRACE_HEADER, nonNull(httpTraceId, req.getTraceId()))
          .body(
              (Object)
                  new NqFeedbackErrorResponse(
                      "HEADER_BINDING_MISMATCH",
                      headerBinding.auditCode(),
                      "NQ feedback header binding mismatch",
                      req.getEventId(),
                      req.getTraceId(),
                      req.getCorrelationId()));
    }

    final Instant occurredAt = req.getOccurredAt() == null ? TimeProvider.now() : req.getOccurredAt();
    final Instant receivedAt = TimeProvider.now();

    final IngestionCommand command =
        IngestionCommand.of(
            tenantId,
            req.getEventId(),
            req.getEventType(),
            occurredAt,
            req.getSourceSystem(),
            req.getSourceJobId(),
            req.getTraceId(),
            req.getRequestId(),
            req.getCorrelationId(),
            req.getSchemaVersion(),
            req.getPayloadJson(),
            receivedAt);

    final IngestionResult result = ingestionService.ingest(command);

    return switch (result.getOutcome()) {
      case ACCEPTED ->
          ResponseEntity.accepted()
              .header(TraceIdFilter.TRACE_HEADER, nonNull(httpTraceId, req.getTraceId()))
              .body(
                  (Object)
                      new NqFeedbackAcceptedResponse(
                          result.getEventId(),
                          result.getStatus(),
                          "ACCEPTED",
                          req.getTraceId(),
                          req.getCorrelationId()));
      case DUPLICATE ->
          ResponseEntity.accepted()
              .header(TraceIdFilter.TRACE_HEADER, nonNull(httpTraceId, req.getTraceId()))
              .body(
                  (Object)
                      new NqFeedbackAcceptedResponse(
                          result.getEventId(),
                          result.getStatus(),
                          "DUPLICATE",
                          req.getTraceId(),
                          req.getCorrelationId()));
      case REJECTED ->
          ResponseEntity.status(HttpStatus.BAD_REQUEST)
              .header(TraceIdFilter.TRACE_HEADER, nonNull(httpTraceId, req.getTraceId()))
              .body(
                  (Object)
                      new NqFeedbackErrorResponse(
                          "INVALID_REQUEST",
                          result.getErrorCode() == null ? null : result.getErrorCode().name(),
                          result.getErrorMessage(),
                          req.getEventId(),
                          req.getTraceId(),
                          req.getCorrelationId()));
    };
  }

  private static String resolveHttpTraceId(final HttpServletRequest httpRequest) {
    final Object attr = httpRequest.getAttribute(TraceIdFilter.TRACE_HEADER);
    return attr == null ? null : attr.toString();
  }

  private static String nonNull(final String a, final String fallback) {
    return a == null || a.isBlank() ? fallback : a;
  }

  /**
   * 构造认证请求并校验。Batch 2：header 值取自归一化模型（canonical 族 {@code X-NQ-DH-*}）；
   * 权威 tenant 仍来自认证上下文、requestId/traceId/eventId/sourceSystem/payload 仍来自 body，未改变。
   * canonical Tenant/Request/Trace 即使出现在模型中也不参与认证、不覆盖权威来源（binding 校验见 Batch 3）。
   * 不改变 HMAC signatureMaterial 语义（仍 value-based，不含 header name）。
   *
   * @param req envelope 请求体
   * @param nqHeaders 归一化 header 模型
   * @param httpRequest 仅用于读取 contentLength
   * @return 认证结果
   */
  private NqFeedbackAuthResult authenticateNqSource(
      final NqFeedbackEnvelopeRequest req,
      final NormalizedNqDhHeaders nqHeaders,
      final HttpServletRequest httpRequest) {
    return feedbackAuthenticator.authenticate(
        new NqFeedbackAuthRequest(
            nqHeaders.source(),
            req.getSourceSystem(),
            nqHeaders.timestamp(),
            nqHeaders.nonce(),
            nqHeaders.signature(),
            req.getEventId(),
            req.getRequestId(),
            req.getTraceId(),
            req.getPayloadJson(),
            httpRequest.getContentLengthLong(),
            TimeProvider.now()));
  }
}
