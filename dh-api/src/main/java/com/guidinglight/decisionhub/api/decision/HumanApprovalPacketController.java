package com.guidinglight.decisionhub.api.decision;

import com.guidinglight.decisionhub.api.TraceIdFilter;
import com.guidinglight.decisionhub.api.security.AuthenticatedRequest;
import com.guidinglight.decisionhub.common.api.ApiResponse;
import com.guidinglight.decisionhub.common.error.BizException;
import com.guidinglight.decisionhub.common.error.CommonErrorCodes;
import com.guidinglight.decisionhub.common.error.ErrorCode;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalDecision;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionException;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalType;
import com.guidinglight.decisionhub.usecase.qdr.approval.ApprovalDecisionParser;
import com.guidinglight.decisionhub.usecase.qdr.approval.ApprovalPacketView;
import com.guidinglight.decisionhub.usecase.qdr.approval.CreateApprovalPacketCommand;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketAuditException;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketCommandService;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketDuplicateException;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketNotFoundException;
import com.guidinglight.decisionhub.usecase.qdr.approval.HumanApprovalPacketPersistenceException;
import com.guidinglight.decisionhub.usecase.qdr.approval.InvalidApprovalDecisionException;
import com.guidinglight.decisionhub.usecase.qdr.approval.SubmitApprovalDecisionCommand;

import jakarta.servlet.http.HttpServletRequest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * stage-qdr-2 B4 Human Approval Packet API。
 *
 * <p>所有 endpoint 都通过 `DhApiAuthenticationFilter` 认证，tenantId 只从可信 request attribute 读取。
 * API 只创建/查询/更新 DH 内部 approval packet 与审计事件，不触发 NQ、交易、provider、外部 HTTP、
 * replay execution、Agent runtime 或 LIVE。
 */
@RestController
@RequestMapping("/api/ai")
public final class HumanApprovalPacketController {

    private static final String APPROVAL_DUPLICATE = "APPROVAL_PACKET_DUPLICATE";
    private static final String APPROVAL_WRITE_FAILED = "QDR_APPROVAL_WRITE_FAILED";
    private static final String APPROVAL_AUDIT_FAILED = "QDR_APPROVAL_AUDIT_FAILED";
    private static final String APPROVAL_TRANSITION_DENIED = "APPROVAL_TRANSITION_DENIED";
    private static final String INVALID_APPROVAL_DECISION_MESSAGE = "Invalid approval decision.";

    private final HumanApprovalPacketCommandService commandService;

    /**
     * 创建 approval API controller。
     *
     * @param commandService B4 approval command service。
     */
    public HumanApprovalPacketController(final HumanApprovalPacketCommandService commandService) {
        this.commandService = Objects.requireNonNull(commandService, "commandService");
    }

    /**
     * 创建当前 tenant 下指定 decision run 的 approval packet。
     *
     * @param decisionRunId decision_run.id。
     * @param body          创建请求；不接受 tenantId、traceId、approvalStatus 或 executable action。
     * @param request       HTTP request，tenant/requester/trace 只从可信上下文读取。
     * @return 创建后的 PENDING approval packet。
     */
    @PostMapping("/decision-runs/{decisionRunId}/approval-packets")
    public ApiResponse<ApprovalPacketResponse> createApprovalPacket(
            @PathVariable final String decisionRunId,
            @RequestBody final CreateApprovalPacketRequest body,
            final HttpServletRequest request) {
        final String traceId = traceId(request);
        try {
            final ApprovalPacketView view =
                    commandService.create(
                            new CreateApprovalPacketCommand(
                                    AuthenticatedRequest.requireTenantId(request),
                                    decisionRunId,
                                    approvalType(body),
                                    body == null ? null : body.summary(),
                                    body == null ? Map.of() : body.checklistJson(),
                                    body == null ? Map.of() : body.evidenceRefsJson(),
                                    requesterId(request),
                                    traceId));
            return ApiResponse.ok(ApprovalPacketResponse.from(view), traceId);
        } catch (final HumanApprovalPacketNotFoundException error) {
            throw notFound(traceId, "decision run not found");
        } catch (final HumanApprovalPacketDuplicateException error) {
            throw conflict(traceId, "approval packet duplicate", APPROVAL_DUPLICATE);
        } catch (final HumanApprovalPacketAuditException error) {
            throw unavailable(traceId, "approval audit unavailable", APPROVAL_AUDIT_FAILED);
        } catch (final HumanApprovalPacketPersistenceException error) {
            throw unavailable(traceId, "approval write unavailable", APPROVAL_WRITE_FAILED);
        }
    }

    /**
     * 查询当前 tenant 下的 approval packet。
     *
     * @param approvalPacketId approval packet UUID。
     * @param request          HTTP request，tenant 只从可信上下文读取。
     * @return approval packet 详情。
     */
    @GetMapping("/approval-packets/{approvalPacketId}")
    public ApiResponse<ApprovalPacketResponse> getApprovalPacket(
            @PathVariable final String approvalPacketId, final HttpServletRequest request) {
        final String traceId = traceId(request);
        try {
            final ApprovalPacketView view =
                    commandService.get(
                            AuthenticatedRequest.requireTenantId(request), approvalPacketId);
            return ApiResponse.ok(ApprovalPacketResponse.from(view), traceId);
        } catch (final HumanApprovalPacketNotFoundException error) {
            throw notFound(traceId, "approval packet not found");
        } catch (final HumanApprovalPacketPersistenceException error) {
            throw unavailable(traceId, "approval read unavailable", APPROVAL_WRITE_FAILED);
        }
    }

    /**
     * 提交 approval decision，只改变 DH 内部 approval status。
     *
     * @param approvalPacketId approval packet UUID。
     * @param body             decision 请求；只允许 APPROVED / REJECTED / NEEDS_REVIEW。
     * @param request          HTTP request，tenant/requester/trace 只从可信上下文读取。
     * @return 更新后的 approval packet。
     */
    @PostMapping("/approval-packets/{approvalPacketId}/decision")
    public ApiResponse<ApprovalDecisionResponse> submitApprovalDecision(
            @PathVariable final String approvalPacketId,
            @RequestBody final SubmitApprovalDecisionRequest body,
            final HttpServletRequest request) {
        final String traceId = traceId(request);
        try {
            final ApprovalPacketView view =
                    commandService.submitDecision(
                            new SubmitApprovalDecisionCommand(
                                    AuthenticatedRequest.requireTenantId(request),
                                    approvalPacketId,
                                    approvalDecision(body),
                                    body == null ? null : body.reviewerId(),
                                    body == null ? null : body.reviewerNote(),
                                    requesterId(request),
                                    traceId));
            return ApiResponse.ok(ApprovalDecisionResponse.from(view), traceId);
        } catch (final InvalidApprovalDecisionException error) {
            throw invalidApprovalDecision(traceId);
        } catch (final HumanApprovalPacketNotFoundException error) {
            throw notFound(traceId, "approval packet not found");
        } catch (final ApprovalStatusTransitionException error) {
            throw conflict(traceId, "approval transition denied", APPROVAL_TRANSITION_DENIED);
        } catch (final HumanApprovalPacketAuditException error) {
            throw unavailable(traceId, "approval audit unavailable", APPROVAL_AUDIT_FAILED);
        } catch (final HumanApprovalPacketPersistenceException error) {
            throw unavailable(traceId, "approval write unavailable", APPROVAL_WRITE_FAILED);
        }
    }

    private static ApprovalType approvalType(final CreateApprovalPacketRequest body) {
        if (body == null || body.approvalType() == null || body.approvalType().isBlank()) {
            throw new IllegalArgumentException("approvalType must not be blank");
        }
        return ApprovalType.valueOf(body.approvalType());
    }

    private static ApprovalDecision approvalDecision(final SubmitApprovalDecisionRequest body) {
        return ApprovalDecisionParser.parseSafe(body == null ? null : body.decision());
    }

    private static BizException notFound(final String traceId, final String message) {
        return new BizException(CommonErrorCodes.NOT_FOUND, message, null, traceId);
    }

    private static BizException conflict(
            final String traceId, final String message, final String errorCode) {
        return new BizException(
                CommonErrorCodes.CONFLICT, message, Map.of("errorCode", errorCode), traceId);
    }

    private static BizException unavailable(
            final String traceId, final String message, final String errorCode) {
        return new BizException(
                CommonErrorCodes.INTERNAL_ERROR, message, Map.of("errorCode", errorCode), traceId);
    }

    private static BizException invalidApprovalDecision(final String traceId) {
        return new BizException(
                ApprovalApiErrorCode.APPROVAL_DECISION_INVALID,
                INVALID_APPROVAL_DECISION_MESSAGE,
                null,
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
     * 创建 approval packet 的 API request DTO。
     *
     * <p>不包含 tenantId、approvalStatus、decisionAction 或 execution action；这些字段只能由服务端可信上下文
     * 和 decision read model 派生。
     */
    public record CreateApprovalPacketRequest(
            String approvalType,
            String summary,
            Map<String, Object> checklistJson,
            Map<String, Object> evidenceRefsJson,
            String reviewerId,
            String reviewerNote) {
    }

    /**
     * 提交 approval decision 的 API request DTO。
     */
    public record SubmitApprovalDecisionRequest(
            String decision, String reviewerNote, String reviewerId) {
    }

    /**
     * approval packet API response DTO，不直接暴露 persistence entity。
     */
    public record ApprovalPacketResponse(
            String approvalPacketId,
            String decisionRunId,
            String approvalKey,
            String approvalType,
            String approvalStatus,
            String riskLevel,
            String decisionAction,
            BigDecimal confidenceScore,
            String summary,
            Map<String, Object> checklistJson,
            Map<String, Object> evidenceRefsJson,
            String reviewerId,
            String reviewerNote,
            Instant decidedAt,
            Instant createdAt,
            Instant updatedAt) {

        private static ApprovalPacketResponse from(final ApprovalPacketView view) {
            return new ApprovalPacketResponse(
                    view.approvalPacketId(),
                    view.decisionRunId(),
                    view.approvalKey(),
                    view.approvalType(),
                    view.approvalStatus(),
                    view.riskLevel(),
                    view.decisionAction(),
                    view.confidenceScore(),
                    view.summary(),
                    view.checklistJson(),
                    view.evidenceRefsJson(),
                    view.reviewerId(),
                    view.reviewerNote(),
                    view.decidedAt(),
                    view.createdAt(),
                    view.updatedAt());
        }
    }

    /**
     * approval decision API response DTO；不包含任何 executable instruction。
     */
    public record ApprovalDecisionResponse(
            String approvalPacketId,
            String decisionRunId,
            String approvalStatus,
            String reviewerId,
            String reviewerNote,
            Instant decidedAt,
            Instant updatedAt) {

        private static ApprovalDecisionResponse from(final ApprovalPacketView view) {
            return new ApprovalDecisionResponse(
                    view.approvalPacketId(),
                    view.decisionRunId(),
                    view.approvalStatus(),
                    view.reviewerId(),
                    view.reviewerNote(),
                    view.decidedAt(),
                    view.updatedAt());
        }
    }

    private enum ApprovalApiErrorCode implements ErrorCode {
        APPROVAL_DECISION_INVALID(INVALID_APPROVAL_DECISION_MESSAGE, 400);

        private final String message;
        private final int httpStatus;

        ApprovalApiErrorCode(final String message, final int httpStatus) {
            this.message = message;
            this.httpStatus = httpStatus;
        }

        @Override
        public String code() {
            return name();
        }

        @Override
        public String message() {
            return message;
        }

        @Override
        public int httpStatus() {
            return httpStatus;
        }
    }
}
