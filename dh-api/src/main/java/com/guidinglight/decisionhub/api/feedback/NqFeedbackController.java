package com.guidinglight.decisionhub.api.feedback;

import com.guidinglight.decisionhub.api.TraceIdFilter;
import com.guidinglight.decisionhub.common.api.ApiResponse;
import com.guidinglight.decisionhub.common.util.TimeProvider;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.usecase.agent.NqIntegrationUseCase;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Stage1：NQ 反馈事件入口。
 *
 * <p>对应工单 4.6：POST /api/ai/feedback/nq。
 */
@RestController
@RequestMapping("/api/ai/feedback")
public final class NqFeedbackController {

  private static final String DEFAULT_TENANT = "t-default";

  private final NqIntegrationUseCase nqIntegrationUseCase;

  /** 构造。 */
  public NqFeedbackController(final NqIntegrationUseCase nqIntegrationUseCase) {
    this.nqIntegrationUseCase = nqIntegrationUseCase;
  }

  /** 接收一条 NQ 事件。 */
  @PostMapping("/nq")
  public ApiResponse<String> receive(
      @Valid @RequestBody final NqFeedbackRequest req, final HttpServletRequest httpRequest) {
    final String traceId = resolveTraceId(httpRequest);
    final Instant occurredAt = req.getOccurredAt() == null ? TimeProvider.now() : req.getOccurredAt();
    final Map<String, Object> payload =
        req.getPayloadJson() == null ? Map.of() : Map.copyOf(req.getPayloadJson());
    final NqFeedbackEvent event =
        NqFeedbackEvent.create(
            DEFAULT_TENANT,
            req.getRunId(),
            req.getCandidateId(),
            traceId,
            req.getSource(),
            req.getEventType(),
            req.isPositive(),
            payload,
            occurredAt,
            TimeProvider.now());
    nqIntegrationUseCase.onFeedback(event);
    return ApiResponse.ok(event.getEventId(), traceId);
  }

  private static String resolveTraceId(final HttpServletRequest httpRequest) {
    final Object attr = httpRequest.getAttribute(TraceIdFilter.TRACE_HEADER);
    return attr == null ? null : attr.toString();
  }
}
