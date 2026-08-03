package com.guidinglight.decisionhub.usecase.agent.feedback.impl;

import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository.FeedbackIngestionPersistence;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionErrorCode;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionTransactionException;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionUnitOfWork;
import com.guidinglight.decisionhub.usecase.agent.feedback.ValidationResult;
import java.util.Objects;

/**
 * Stage2-PoC-B2：默认 NQ feedback ingestion 服务。
 *
 * <p>编排 §Batch 2.4 - §Batch 2.5：
 *
 * <ol>
 *   <li>先完成契约校验与 canonical payload 计算；失败返回 REJECTED，且不进入写边界。
 *   <li>在原子边界内按 exact envelope-event correlation 分类持久化状态。
 *   <li>仅 COMPLETE_MATCH 返回 DUPLICATE；冲突、orphan 或歧义状态统一 fail-closed。
 *   <li>ABSENT 时保存 envelope，并在显式 eventId 关联上下文内同步派发 handler。
 *   <li>返回 ACCEPTED。
 * </ol>
 */
public final class DefaultNqFeedbackIngestionService implements NqFeedbackIngestionService {

  private static final ObjectMapper PAYLOAD_MAPPER =
      JsonMapper.builder().enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION).build();

  private final NqFeedbackContractValidator validator;
  private final NqFeedbackEventRepository repository;
  private final NqFeedbackEventTypeRouter router;
  private final NqFeedbackIngestionUnitOfWork unitOfWork;

  public DefaultNqFeedbackIngestionService(
      final NqFeedbackContractValidator validator,
      final NqFeedbackEventRepository repository,
      final NqFeedbackEventTypeRouter router,
      final NqFeedbackIngestionUnitOfWork unitOfWork) {
    this.validator = Objects.requireNonNull(validator, "validator");
    this.repository = Objects.requireNonNull(repository, "repository");
    this.router = Objects.requireNonNull(router, "router");
    this.unitOfWork = Objects.requireNonNull(unitOfWork, "unitOfWork");
  }

  @Override
  public IngestionResult ingest(final IngestionCommand command) {
    Objects.requireNonNull(command, "command");
    // Validation/canonicalization is deliberately side-effect free and precedes duplicate lookup.
    final ValidationResult validation = validator.validate(command);
    if (!validation.isValid()) {
      return IngestionResult.rejected(
          command.getEventId(), validation.getErrorCode(), validation.getMessage());
    }
    final NqFeedbackEnvelope envelope = validation.getEnvelope();
    if (!hasUnambiguousCanonicalPayload(envelope.getPayloadJson())) {
      return IngestionResult.rejected(
          command.getEventId(),
          IngestionErrorCode.INVALID_SCHEMA,
          "payload must contain exactly one unambiguous JSON object");
    }
    return unitOfWork.required(() -> ingestValidatedAtomic(envelope, command.getTenantId()));
  }

  private IngestionResult ingestValidatedAtomic(
      final NqFeedbackEnvelope envelope, final String tenantId) {
    final Resolution initial = resolve(envelope, tenantId);
    if (initial.state() != ResolutionState.ABSENT) {
      return duplicateOrFail(initial, envelope.getEventId());
    }

    final boolean firstWrite = repository.saveEnvelope(envelope);
    if (!firstWrite) {
      // Concurrent winner must be reconciled in the same transaction using exact correlation.
      return duplicateOrFail(resolve(envelope, tenantId), envelope.getEventId());
    }

    repository.beginEventCorrelation(envelope, tenantId);
    try {
      router.route(envelope, tenantId);
    } finally {
      repository.endEventCorrelation();
    }

    final Resolution completed = resolve(envelope, tenantId);
    if (completed.state() != ResolutionState.COMPLETE_MATCH) {
      throw persistenceFailure("feedback ingestion did not produce one exact correlated event");
    }

    return IngestionResult.accepted(envelope.getEventId());
  }

  private Resolution resolve(final NqFeedbackEnvelope expected, final String tenantId) {
    final FeedbackIngestionPersistence persistence =
        repository.findIngestionPersistence(
            expected.getEventId(), tenantId, expected.getTraceId(), expected.getEventType());
    if (persistence.envelope().isPresent()
        && !sameCanonicalEnvelope(persistence.envelope().get(), expected)) {
      return new Resolution(ResolutionState.ENVELOPE_CONFLICT);
    }
    return new Resolution(ResolutionState.valueOf(persistence.state().name()));
  }

  private static IngestionResult duplicateOrFail(
      final Resolution resolution, final String eventId) {
    if (resolution.state() == ResolutionState.COMPLETE_MATCH) {
      return IngestionResult.duplicate(eventId);
    }
    final String safeMessage =
        switch (resolution.state()) {
          case ENVELOPE_CONFLICT -> "feedback eventId conflicts with canonical envelope";
          case ENVELOPE_ONLY -> "feedback ingestion has an orphan envelope";
          case EVENT_ONLY -> "feedback ingestion has an orphan routed event";
          case EVENT_CONFLICT -> "feedback routed event correlation conflicts";
          case AMBIGUOUS_CORRELATION -> "feedback routed event correlation is ambiguous";
          case ABSENT -> "feedback ingestion conflict could not be reconciled";
          case COMPLETE_MATCH -> throw new AssertionError("handled above");
        };
    throw persistenceFailure(safeMessage);
  }

  private static boolean sameCanonicalEnvelope(
      final NqFeedbackEnvelope left, final NqFeedbackEnvelope right) {
    return Objects.equals(left.getEventId(), right.getEventId())
        && left.getEventType() == right.getEventType()
        && Objects.equals(left.getOccurredAt(), right.getOccurredAt())
        && Objects.equals(left.getSourceSystem(), right.getSourceSystem())
        && Objects.equals(left.getSourceJobId(), right.getSourceJobId())
        && Objects.equals(left.getTraceId(), right.getTraceId())
        && Objects.equals(left.getRequestId(), right.getRequestId())
        && Objects.equals(left.getCorrelationId(), right.getCorrelationId())
        && Objects.equals(left.getSchemaVersion(), right.getSchemaVersion())
        && sameJsonPayload(left.getPayloadJson(), right.getPayloadJson());
  }

  private static boolean sameJsonPayload(final String left, final String right) {
    final JsonNode leftTree =
        DefaultNqFeedbackContractValidator.parseStrictSingleRootObject(PAYLOAD_MAPPER, left);
    final JsonNode rightTree =
        DefaultNqFeedbackContractValidator.parseStrictSingleRootObject(PAYLOAD_MAPPER, right);
    if (leftTree == null || rightTree == null) {
      throw persistenceFailure("validated feedback payload cannot be canonicalized");
    }
    return Objects.equals(leftTree, rightTree);
  }

  private static boolean hasUnambiguousCanonicalPayload(final String payload) {
    return DefaultNqFeedbackContractValidator.parseStrictSingleRootObject(
            PAYLOAD_MAPPER, payload)
        != null;
  }

  private enum ResolutionState {
    ABSENT,
    COMPLETE_MATCH,
    ENVELOPE_ONLY,
    EVENT_ONLY,
    ENVELOPE_CONFLICT,
    EVENT_CONFLICT,
    AMBIGUOUS_CORRELATION
  }

  private record Resolution(ResolutionState state) {}

  private static NqFeedbackIngestionTransactionException persistenceFailure(
      final String safeMessage) {
    return new NqFeedbackIngestionTransactionException(
        NqFeedbackIngestionTransactionException.ErrorCode.PERSISTENCE_FAILURE, safeMessage);
  }
}
