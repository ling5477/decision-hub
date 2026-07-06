package com.guidinglight.decisionhub.usecase.qdr.approval;

import com.guidinglight.decisionhub.domain.qdr.QuantDecisionAction;
import com.guidinglight.decisionhub.domain.qdr.RiskLevel;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalChecklist;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalDecision;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalEvidenceRefs;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalKey;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalReviewer;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatus;
import com.guidinglight.decisionhub.domain.qdr.approval.ApprovalStatusTransitionException;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacket;
import com.guidinglight.decisionhub.domain.qdr.approval.HumanApprovalPacketId;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventStatus;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditEventType;
import com.guidinglight.decisionhub.usecase.decision.DecisionAuditRepository;
import com.guidinglight.decisionhub.usecase.decision.DecisionPersistenceRecords;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionReadModelService;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunReadQuery;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * stage-qdr-2 B4 Human Approval Packet command service。
 *
 * <p>该 service 负责 tenant-bound create / get / decision submit，并把所有 write 记录到既有
 * `dh_decision_audit_event` 审计端口。它只依赖 usecase port / read model service / B3 状态机 service；
 * 不依赖 API、infra、Spring Web、JDBC/JPA、provider SDK、LangGraph、NQ client 或任何外部 HTTP。
 */
public final class HumanApprovalPacketCommandService {

    private final HumanApprovalPacketRepository repository;
    private final HumanApprovalPacketService packetService;
    private final DecisionReadModelService readModelService;
    private final DecisionAuditRepository auditRepository;
    private final Clock clock;
    private final Supplier<UUID> idSupplier;
    private final ApprovalWriteBoundary writeBoundary;

    /**
     * 创建 command service。
     *
     * @param repository       B3 approval repository port，必须 tenant-bound。
     * @param packetService    B3 approval domain service，内部使用状态机。
     * @param readModelService B2 read model service，用于验证 decision_run 属于当前 tenant。
     * @param auditRepository  K3/V5 审计端口；写失败必须 fail-closed。
     * @param clock            UTC 时钟。
     */
    public HumanApprovalPacketCommandService(
            final HumanApprovalPacketRepository repository,
            final HumanApprovalPacketService packetService,
            final DecisionReadModelService readModelService,
            final DecisionAuditRepository auditRepository,
            final Clock clock) {
        this(
                repository,
                packetService,
                readModelService,
                auditRepository,
                clock,
                UUID::randomUUID,
                ApprovalWriteBoundary.direct());
    }

    /**
     * 创建带外部写入边界的 command service。
     *
     * @param repository       B3 approval repository port。
     * @param packetService    B3 approval domain service。
     * @param readModelService B2 read model service。
     * @param auditRepository  审计端口。
     * @param clock            UTC 时钟。
     * @param writeBoundary    写入边界；应用层用它接入事务，保证 audit 失败回滚 approval write。
     */
    public HumanApprovalPacketCommandService(
            final HumanApprovalPacketRepository repository,
            final HumanApprovalPacketService packetService,
            final DecisionReadModelService readModelService,
            final DecisionAuditRepository auditRepository,
            final Clock clock,
            final ApprovalWriteBoundary writeBoundary) {
        this(
                repository,
                packetService,
                readModelService,
                auditRepository,
                clock,
                UUID::randomUUID,
                writeBoundary);
    }

    /**
     * 创建可注入 ID 生成器的 command service，供 deterministic tests 使用。
     *
     * @param repository       B3 approval repository port。
     * @param packetService    B3 approval domain service。
     * @param readModelService B2 read model service。
     * @param auditRepository  审计端口。
     * @param clock            UTC 时钟。
     * @param idSupplier       approval packet / audit event ID 生成器。
     */
    public HumanApprovalPacketCommandService(
            final HumanApprovalPacketRepository repository,
            final HumanApprovalPacketService packetService,
            final DecisionReadModelService readModelService,
            final DecisionAuditRepository auditRepository,
            final Clock clock,
            final Supplier<UUID> idSupplier) {
        this(
                repository,
                packetService,
                readModelService,
                auditRepository,
                clock,
                idSupplier,
                ApprovalWriteBoundary.direct());
    }

    /**
     * 创建可注入 ID 生成器和写入边界的 command service，供 deterministic tests / app wiring 使用。
     *
     * @param repository       B3 approval repository port。
     * @param packetService    B3 approval domain service。
     * @param readModelService B2 read model service。
     * @param auditRepository  审计端口。
     * @param clock            UTC 时钟。
     * @param idSupplier       approval packet / audit event ID 生成器。
     * @param writeBoundary    写入边界；必须覆盖 repository write + audit write。
     */
    public HumanApprovalPacketCommandService(
            final HumanApprovalPacketRepository repository,
            final HumanApprovalPacketService packetService,
            final DecisionReadModelService readModelService,
            final DecisionAuditRepository auditRepository,
            final Clock clock,
            final Supplier<UUID> idSupplier,
            final ApprovalWriteBoundary writeBoundary) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.packetService = Objects.requireNonNull(packetService, "packetService");
        this.readModelService = Objects.requireNonNull(readModelService, "readModelService");
        this.auditRepository = Objects.requireNonNull(auditRepository, "auditRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idSupplier = Objects.requireNonNull(idSupplier, "idSupplier");
        this.writeBoundary = Objects.requireNonNull(writeBoundary, "writeBoundary");
    }

    /**
     * 基于当前 tenant 下已存在的 decision_run 创建 PENDING approval packet。
     *
     * @param command API command；tenantId 必须来自认证上下文。
     * @return 创建后的 approval packet view。
     * @throws HumanApprovalPacketNotFoundException decision_run 不存在或不属于 tenant。
     * @throws HumanApprovalPacketDuplicateException approval_key 已存在。
     * @throws HumanApprovalPacketAuditException audit 写入失败。
     */
    public ApprovalPacketView create(final CreateApprovalPacketCommand command) {
        final CreateApprovalPacketCommand checked = Objects.requireNonNull(command, "command");
        return writeBoundary.execute(() -> createWithinBoundary(checked));
    }

    private ApprovalPacketView createWithinBoundary(final CreateApprovalPacketCommand checked) {
        final DecisionRunDetailView detail = requireDecisionRun(checked);
        final DecisionSnapshot snapshot = DecisionSnapshot.from(detail);
        final ApprovalKey approvalKey = approvalKey(checked.decisionRunId(), checked.approvalType().name());
        if (repository.findByTenantAndApprovalKey(checked.tenantId(), approvalKey).isPresent()) {
            throw new HumanApprovalPacketDuplicateException(
                    "human approval packet duplicate approval key",
                    new IllegalStateException("duplicate approval key"));
        }

        final HumanApprovalPacketResult result =
                packetService.create(
                        new CreateHumanApprovalPacketCommand(
                                new HumanApprovalPacketId(nextUuid()),
                                UUID.fromString(checked.decisionRunId()),
                                checked.tenantId(),
                                detail.traceId(),
                                detail.requestId(),
                                approvalKey,
                                checked.approvalType(),
                                snapshot.riskLevel(),
                                snapshot.decisionAction(),
                                snapshot.confidenceScore(),
                                checked.summary(),
                                new ApprovalChecklist(checklist(checked, detail)),
                                new ApprovalEvidenceRefs(evidenceRefs(checked, detail))));
        saveAuditEvent(
                result.packet(),
                null,
                result.packet().approvalStatus(),
                DecisionAuditEventType.HUMAN_APPROVAL_PACKET_CREATED,
                DecisionAuditEventStatus.SUCCESS,
                null,
                actorId(checked.requesterId(), null),
                null,
                Instant.now(clock));
        return ApprovalPacketView.from(result.packet());
    }

    /**
     * 查询当前 tenant 下的 approval packet。
     *
     * @param tenantId         认证上下文 tenant。
     * @param approvalPacketId approval packet UUID。
     * @return API-safe approval packet view。
     */
    public ApprovalPacketView get(final String tenantId, final String approvalPacketId) {
        final String checkedTenant = ApprovalCommandValidation.requireText(tenantId, "tenantId");
        final HumanApprovalPacketId id =
                new HumanApprovalPacketId(
                        UUID.fromString(
                                ApprovalCommandValidation.requireUuidText(
                                        approvalPacketId, "approvalPacketId")));
        return repository
                .findByTenantAndId(checkedTenant, id)
                .map(ApprovalPacketView::from)
                .orElseThrow(() -> new HumanApprovalPacketNotFoundException("approval packet not found"));
    }

    /**
     * 提交人工审批决定，仅改变 `human_approval_packet.approval_status`。
     *
     * @param command submit decision command；tenantId / requesterId 来自认证上下文。
     * @return 更新后的 approval packet view。
     * @throws ApprovalStatusTransitionException 状态机拒绝转移。
     * @throws HumanApprovalPacketAuditException audit 写入失败。
     */
    public ApprovalPacketView submitDecision(final SubmitApprovalDecisionCommand command) {
        final SubmitApprovalDecisionCommand checked = Objects.requireNonNull(command, "command");
        return writeBoundary.execute(() -> submitDecisionWithinBoundary(checked));
    }

    private ApprovalPacketView submitDecisionWithinBoundary(final SubmitApprovalDecisionCommand checked) {
        final HumanApprovalPacketId id =
                new HumanApprovalPacketId(UUID.fromString(checked.approvalPacketId()));
        final HumanApprovalPacket current =
                repository
                        .findByTenantAndId(checked.tenantId(), id)
                        .orElseThrow(
                                () ->
                                        new HumanApprovalPacketNotFoundException(
                                                "approval packet not found"));
        final ApprovalReviewer reviewer =
                new ApprovalReviewer(
                        actorId(checked.requesterId(), checked.reviewerId()), checked.reviewerNote());
        try {
            final HumanApprovalPacketResult result =
                    packetService.updateStatus(
                            new ApprovalStatusUpdateCommand(
                                    checked.tenantId(),
                                    id,
                                    checked.decision().targetStatus(),
                                    reviewer,
                                    Instant.now(clock)));
            saveAuditEvent(
                    result.packet(),
                    current.approvalStatus(),
                    result.packet().approvalStatus(),
                    decisionEventType(checked.decision()),
                    DecisionAuditEventStatus.SUCCESS,
                    null,
                    reviewer.reviewerId(),
                    reviewer.reviewerNote(),
                    Instant.now(clock));
            return ApprovalPacketView.from(result.packet());
        } catch (final ApprovalStatusTransitionException error) {
            saveAuditEvent(
                    current,
                    current.approvalStatus(),
                    checked.decision().targetStatus(),
                    DecisionAuditEventType.HUMAN_APPROVAL_TRANSITION_DENIED,
                    DecisionAuditEventStatus.FAILED,
                    "APPROVAL_TRANSITION_DENIED",
                    reviewer.reviewerId(),
                    reviewer.reviewerNote(),
                    Instant.now(clock));
            throw error;
        }
    }

    private DecisionRunDetailView requireDecisionRun(final CreateApprovalPacketCommand command) {
        return readModelService
                .findDecisionRunDetail(
                        new DecisionRunReadQuery(
                                command.tenantId(),
                                command.decisionRunId(),
                                command.requesterId(),
                                command.traceId()))
                .orElseThrow(() -> new HumanApprovalPacketNotFoundException("decision run not found"));
    }

    private void saveAuditEvent(
            final HumanApprovalPacket packet,
            final ApprovalStatus oldStatus,
            final ApprovalStatus newStatus,
            final DecisionAuditEventType eventType,
            final DecisionAuditEventStatus eventStatus,
            final String errorCode,
            final String actorId,
            final String reason,
            final Instant occurredAt) {
        try {
            auditRepository.saveAuditEvent(
                    new DecisionPersistenceRecords.AuditEventRecord(
                            auditEventId(packet, eventType, occurredAt),
                            packet.requestId(),
                            packet.tenantId(),
                            packet.traceId(),
                            eventType,
                            eventStatus,
                            auditPayload(packet, oldStatus, newStatus, actorId, reason, occurredAt),
                            errorCode,
                            occurredAt));
        } catch (final RuntimeException error) {
            throw new HumanApprovalPacketAuditException("human approval audit write failed", error);
        }
    }

    private Map<String, Object> auditPayload(
            final HumanApprovalPacket packet,
            final ApprovalStatus oldStatus,
            final ApprovalStatus newStatus,
            final String actorId,
            final String reason,
            final Instant occurredAt) {
        final Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("tenantId", packet.tenantId());
        payload.put("traceId", packet.traceId());
        payload.put("requestId", packet.requestId());
        payload.put("decisionRunId", packet.decisionRunId().toString());
        payload.put("approvalPacketId", packet.id().value().toString());
        payload.put("approvalKey", packet.approvalKey().value());
        putIfNotNull(payload, "oldStatus", oldStatus == null ? null : oldStatus.name());
        payload.put("newStatus", newStatus.name());
        putIfNotNull(payload, "reviewerId", actorId);
        payload.put("occurredAt", occurredAt.toString());
        putIfNotNull(
                payload,
                "reason",
                ApprovalCommandValidation.optionalSafeText(reason, "auditReason"));
        return payload;
    }

    private static void putIfNotNull(
            final Map<String, Object> payload, final String key, final Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private static DecisionAuditEventType decisionEventType(final ApprovalDecision decision) {
        return switch (decision) {
            case APPROVED -> DecisionAuditEventType.HUMAN_APPROVAL_DECISION_SUBMITTED;
            case REJECTED -> DecisionAuditEventType.HUMAN_APPROVAL_DECISION_REJECTED;
            case NEEDS_REVIEW -> DecisionAuditEventType.HUMAN_APPROVAL_DECISION_NEEDS_REVIEW;
        };
    }

    private static Map<String, Object> checklist(
            final CreateApprovalPacketCommand command, final DecisionRunDetailView detail) {
        final Map<String, Object> checklist = new LinkedHashMap<>(command.checklistJson());
        checklist.putIfAbsent("decisionRunId", detail.decisionRunId());
        checklist.putIfAbsent("approvalType", command.approvalType().name());
        checklist.putIfAbsent("tenantBound", true);
        checklist.putIfAbsent("noExternalSideEffect", true);
        return checklist;
    }

    private static Map<String, Object> evidenceRefs(
            final CreateApprovalPacketCommand command, final DecisionRunDetailView detail) {
        final Map<String, Object> refs = new LinkedHashMap<>(command.evidenceRefsJson());
        refs.putIfAbsent("decisionRun", "decision-run://" + detail.decisionRunId());
        refs.putIfAbsent("trace", "trace://" + detail.traceId());
        refs.putIfAbsent("request", "request://" + detail.requestId());
        return refs;
    }

    private static ApprovalKey approvalKey(final String decisionRunId, final String approvalType) {
        return new ApprovalKey("qdr:" + decisionRunId + ":" + approvalType);
    }

    private UUID nextUuid() {
        return Objects.requireNonNull(idSupplier.get(), "idSupplier returned null");
    }

    private String auditEventId(
            final HumanApprovalPacket packet,
            final DecisionAuditEventType eventType,
            final Instant occurredAt) {
        return "human-approval-"
                + packet.id().value()
                + "-"
                + eventType.name()
                + "-"
                + occurredAt.toEpochMilli()
                + "-"
                + nextUuid();
    }

    private static String actorId(final String requesterId, final String fallbackReviewerId) {
        if (requesterId != null && !requesterId.isBlank()) {
            return requesterId;
        }
        return fallbackReviewerId;
    }

    private record DecisionSnapshot(
            RiskLevel riskLevel, QuantDecisionAction decisionAction, BigDecimal confidenceScore) {

        private static DecisionSnapshot from(final DecisionRunDetailView detail) {
            final String summary = detail.quantDecisionSummary();
            if (summary == null || summary.isBlank()) {
                throw new HumanApprovalPacketPersistenceException(
                        "decision run quant decision summary unavailable",
                        new IllegalStateException("missing quant decision summary"));
            }
            final Map<String, String> fields = summaryFields(summary);
            try {
                final String action = requiredField(fields, "action");
                final String risk = requiredField(fields, "risk");
                final String confidence = fields.get("confidence");
                return new DecisionSnapshot(
                        RiskLevel.valueOf(risk),
                        HumanApprovalPacket.decisionActionFrom(action),
                        confidence == null || "n/a".equalsIgnoreCase(confidence)
                                ? null
                                : new BigDecimal(confidence));
            } catch (final RuntimeException error) {
                throw new HumanApprovalPacketPersistenceException(
                        "decision run quant decision summary rejected", error);
            }
        }

        private static Map<String, String> summaryFields(final String summary) {
            final Map<String, String> fields = new LinkedHashMap<>();
            for (String segment : summary.split(";")) {
                final int separator = segment.indexOf('=');
                if (separator > 0 && separator < segment.length() - 1) {
                    fields.put(segment.substring(0, separator), segment.substring(separator + 1));
                }
            }
            return fields;
        }

        private static String requiredField(final Map<String, String> fields, final String key) {
            final String value = fields.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("quant decision summary missing " + key);
            }
            return value;
        }
    }
}
