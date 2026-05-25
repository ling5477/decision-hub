package com.guidinglight.decisionhub.api.feedback;

import com.guidinglight.decisionhub.api.TraceIdFilter;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
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

  private static final String DEFAULT_TENANT = "t-default";

  private final NqFeedbackIngestionService ingestionService;

  public NqFeedbackController(final NqFeedbackIngestionService ingestionService) {
    this.ingestionService = ingestionService;
  }

  /** 接收一条 NQ envelope 事件。 */
  @PostMapping("/nq")
  public ResponseEntity<Object> receive(
      @Valid @RequestBody final NqFeedbackEnvelopeRequest req,
      final HttpServletRequest httpRequest) {

    final String httpTraceId = resolveHttpTraceId(httpRequest);
    final Instant occurredAt = req.getOccurredAt() == null ? TimeProvider.now() : req.getOccurredAt();
    final Instant receivedAt = TimeProvider.now();

    final IngestionCommand command =
        IngestionCommand.of(
            DEFAULT_TENANT,
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
}
