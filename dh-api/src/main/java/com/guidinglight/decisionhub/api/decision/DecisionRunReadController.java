package com.guidinglight.decisionhub.api.decision;

import com.guidinglight.decisionhub.api.TraceIdFilter;
import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.common.api.ApiResponse;
import com.guidinglight.decisionhub.common.error.BizException;
import com.guidinglight.decisionhub.common.error.CommonErrorCodes;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelService;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelUnavailableException;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceReadQuery;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceStepView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionTraceTimelineView;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * stage-qdr-2 B2 decision run 只读查询 API。
 *
 * <p>Controller 只从认证 filter 写入的可信上下文读取 tenant；不接 JDBC、provider SDK、外部 HTTP、
 * replay execution、approval write 或 LIVE。响应只暴露 read model 摘要与引用，不返回 tenantId、
 * raw prompt、raw provider response、credential 或可执行交易指令。
 */
@RestController
@RequestMapping("/api/ai/decision-runs")
public final class DecisionRunReadController {

    private static final String READMODEL_UNAVAILABLE = "QDR_READMODEL_UNAVAILABLE";

    private final DecisionReadModelService readModelService;

    /**
     * 创建只读 decision run controller。
     *
     * @param readModelService stage-qdr-2 B2 read model 查询服务。
     */
    public DecisionRunReadController(final DecisionReadModelService readModelService) {
        this.readModelService = Objects.requireNonNull(readModelService, "readModelService");
    }

    /**
     * 查询当前 tenant 下的 decision run 详情。
     *
     * @param decisionRunId V6 decision_run.id。
     * @param request       HTTP request，tenant 与 trace 只从可信 attribute 读取。
     * @return 只读详情摘要。
     */
    @GetMapping("/{decisionRunId}")
    public ApiResponse<DecisionRunDetailResponse> findDecisionRunDetail(
            @PathVariable final String decisionRunId, final HttpServletRequest request) {
        final String tenantId = AuthenticatedRequest.requireTenantId(request);
        final String traceId = traceId(request);
        try {
            final DecisionRunDetailView view =
                    readModelService
                            .findDecisionRunDetail(
                                    new DecisionRunReadQuery(
                                            tenantId, decisionRunId, requesterId(request), traceId))
                            .orElseThrow(() -> notFound(traceId));
            return ApiResponse.ok(DecisionRunDetailResponse.from(view), traceId);
        } catch (final DecisionReadModelUnavailableException error) {
            throw unavailable(traceId);
        }
    }

    /**
     * 查询当前 tenant 下的 decision run trace timeline。
     *
     * @param decisionRunId V6 decision_run.id。
     * @param request       HTTP request，tenant 与 trace 只从可信 attribute 读取。
     * @return 只读 trace timeline；steps 允许为空。
     */
    @GetMapping("/{decisionRunId}/trace")
    public ApiResponse<DecisionTraceTimelineResponse> findDecisionTrace(
            @PathVariable final String decisionRunId, final HttpServletRequest request) {
        final String tenantId = AuthenticatedRequest.requireTenantId(request);
        final String traceId = traceId(request);
        try {
            final DecisionTraceTimelineView view =
                    readModelService
                            .findDecisionTrace(
                                    new DecisionTraceReadQuery(
                                            tenantId, decisionRunId, requesterId(request), traceId))
                            .orElseThrow(() -> notFound(traceId));
            return ApiResponse.ok(DecisionTraceTimelineResponse.from(view), traceId);
        } catch (final DecisionReadModelUnavailableException error) {
            throw unavailable(traceId);
        }
    }

    private static BizException notFound(final String traceId) {
        return new BizException(CommonErrorCodes.NOT_FOUND, "decision run not found", null, traceId);
    }

    private static BizException unavailable(final String traceId) {
        return new BizException(
                CommonErrorCodes.INTERNAL_ERROR,
                "decision read model unavailable",
                Map.of("errorCode", READMODEL_UNAVAILABLE),
                traceId);
    }

    private static String traceId(final HttpServletRequest request) {
        final Object value = request.getAttribute(TraceIdFilter.TRACE_HEADER);
        return value == null ? null : value.toString();
    }

    private static String requesterId(final HttpServletRequest request) {
        final var authContext = AuthenticatedRequest.authContext(request);
        return authContext == null ? null : authContext.userId();
    }

    /**
     * API-safe decision run detail response。
     *
     * <p>tenantId 故意不出现在外部响应中；tenant 只作为认证与查询边界。
     */
    public record DecisionRunDetailResponse(
            String decisionRequestId,
            String decisionRunId,
            String traceId,
            String requestId,
            String requestKey,
            String requestType,
            String sourceSystem,
            String sourceRefId,
            Integer runNo,
            String status,
            Instant startedAt,
            Instant finishedAt,
            Long latencyMs,
            String errorCode,
            String errorMessage,
            String quantSignalSummary,
            String quantDecisionSummary,
            String outputSummary,
            Instant createdAt) {

        private static DecisionRunDetailResponse from(final DecisionRunDetailView view) {
            return new DecisionRunDetailResponse(
                    view.decisionRequestId(),
                    view.decisionRunId(),
                    view.traceId(),
                    view.requestId(),
                    view.requestKey(),
                    view.requestType(),
                    view.sourceSystem(),
                    view.sourceRefId(),
                    view.runNo(),
                    view.status().name(),
                    view.startedAt(),
                    view.finishedAt(),
                    view.latencyMs(),
                    view.errorCode(),
                    view.errorMessage(),
                    view.quantSignalSummary(),
                    view.quantDecisionSummary(),
                    view.outputSummary(),
                    view.createdAt());
        }
    }

    /**
     * API-safe trace timeline response。
     */
    public record DecisionTraceTimelineResponse(
            String decisionRunId, String traceId, String requestId, List<DecisionTraceStepResponse> steps) {

        private static DecisionTraceTimelineResponse from(final DecisionTraceTimelineView view) {
            return new DecisionTraceTimelineResponse(
                    view.decisionRunId(),
                    view.traceId(),
                    view.requestId(),
                    view.steps().stream().map(DecisionTraceStepResponse::from).toList());
        }
    }

    /**
     * API-safe trace step response；仅包含摘要和引用字段。
     */
    public record DecisionTraceStepResponse(
            String stepId,
            Integer stepNo,
            String stepName,
            String stepType,
            String status,
            Instant startedAt,
            Instant finishedAt,
            Long latencyMs,
            String errorCode,
            String errorMessage,
            String inputSummaryJson,
            String outputSummaryJson,
            String providerCallRef,
            String auditRef) {

        private static DecisionTraceStepResponse from(final DecisionTraceStepView view) {
            return new DecisionTraceStepResponse(
                    view.stepId(),
                    view.stepNo(),
                    view.stepName(),
                    view.stepType(),
                    view.status(),
                    view.startedAt(),
                    view.finishedAt(),
                    view.latencyMs(),
                    view.errorCode(),
                    view.errorMessage(),
                    view.inputSummaryJson(),
                    view.outputSummaryJson(),
                    view.providerCallRef(),
                    view.auditRef());
        }
    }
}
