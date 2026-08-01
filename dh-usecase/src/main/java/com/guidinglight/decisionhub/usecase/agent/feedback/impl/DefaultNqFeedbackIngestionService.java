package com.guidinglight.decisionhub.usecase.agent.feedback.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEvent;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionTransactionException;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionUnitOfWork;
import com.guidinglight.decisionhub.usecase.agent.feedback.ValidationResult;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Stage2-PoC-B2：默认 NQ feedback ingestion 服务。
 *
 * <p>编排 §Batch 2.4 - §Batch 2.5：
 *
 * <ol>
 *   <li>幂等优先：若 {@code findEnvelopeByEventId} 命中，则直接返回 DUPLICATE，不再校验、不再派发。
 *   <li>调用 {@link NqFeedbackContractValidator}；失败返回 REJECTED。
 *   <li>调用 {@link NqFeedbackEventRepository#saveEnvelope}；返回 {@code false}（并发竞态）视为 DUPLICATE。
 *   <li>调用 {@link NqFeedbackEventTypeRouter#route} 派发 handler。
 *   <li>返回 ACCEPTED。
 * </ol>
 */
public final class DefaultNqFeedbackIngestionService implements NqFeedbackIngestionService {

  private static final ObjectMapper PAYLOAD_MAPPER = new ObjectMapper();

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
    return unitOfWork.required(() -> ingestAtomic(command));
  }

  private IngestionResult ingestAtomic(final IngestionCommand command) {
    // 1. 幂等优先：eventId 为空时不查；非空命中即返回 DUPLICATE。
    if (command.getEventId() != null && !command.getEventId().isBlank()) {
      final Optional<NqFeedbackEnvelope> existing =
          repository.findEnvelopeByEventId(command.getEventId());
      if (existing.isPresent()) {
        return completeDuplicateOrFail(existing.get(), command.getTenantId());
      }
    }

    // 2. 契约校验。
    final ValidationResult validation = validator.validate(command);
    if (!validation.isValid()) {
      return IngestionResult.rejected(
          command.getEventId(), validation.getErrorCode(), validation.getMessage());
    }

    final NqFeedbackEnvelope envelope = validation.getEnvelope();

    // 3. 保存 envelope；并发竞态下重复键视为 DUPLICATE。
    final boolean firstWrite = repository.saveEnvelope(envelope);
    if (!firstWrite) {
      final NqFeedbackEnvelope existing =
          repository
              .findEnvelopeByEventId(envelope.getEventId())
              .orElseThrow(
                  () ->
                      persistenceFailure(
                          "feedback envelope conflict could not be reconciled"));
      return completeDuplicateOrFail(existing, command.getTenantId());
    }

    // 4. 派发 append-only handler；append 或未知异常直接向上失败关闭，不返回 false success。
    router.route(envelope, command.getTenantId());

    return IngestionResult.accepted(envelope.getEventId());
  }

  private IngestionResult completeDuplicateOrFail(
      final NqFeedbackEnvelope envelope, final String tenantId) {
    final boolean complete =
        repository.listByRun(tenantId, envelope.getTraceId()).stream()
            .anyMatch(event -> matchesEnvelope(event, envelope));
    if (!complete) {
      throw persistenceFailure("feedback ingestion state is incomplete or inconsistent");
    }
    return IngestionResult.duplicate(envelope.getEventId());
  }

  private static boolean matchesEnvelope(
      final NqFeedbackEvent event, final NqFeedbackEnvelope envelope) {
    final Map<String, Object> eventPayload = event.getPayloadJson();
    final Object rawPayload = eventPayload.get("rawPayloadJson");
    return envelope.getEventType().name().equals(event.getEventType())
        && Objects.equals(envelope.getOccurredAt(), event.getOccurredAt())
        && rawPayload instanceof String raw
        && sameJsonPayload(envelope.getPayloadJson(), raw);
  }

  private static boolean sameJsonPayload(final String left, final String right) {
    try {
      final JsonNode leftTree = PAYLOAD_MAPPER.readTree(left);
      final JsonNode rightTree = PAYLOAD_MAPPER.readTree(right);
      return Objects.equals(leftTree, rightTree);
    } catch (final JsonProcessingException ignored) {
      // Legacy invalid JSON can only match by its exact non-string whitespace-normalized form.
      return normalizeJsonWhitespace(left).equals(normalizeJsonWhitespace(right));
    }
  }

  /** JSONB 回读会规范对象空白；只移除字符串字面量外的空白，避免改变字符串字段值。 */
  private static String normalizeJsonWhitespace(final String value) {
    if (value == null) {
      return "";
    }
    final StringBuilder normalized = new StringBuilder(value.length());
    boolean quoted = false;
    boolean escaped = false;
    for (int index = 0; index < value.length(); index++) {
      final char current = value.charAt(index);
      if (quoted) {
        normalized.append(current);
        if (escaped) {
          escaped = false;
        } else if (current == '\\') {
          escaped = true;
        } else if (current == '"') {
          quoted = false;
        }
      } else if (current == '"') {
        quoted = true;
        normalized.append(current);
      } else if (!Character.isWhitespace(current)) {
        normalized.append(current);
      }
    }
    return normalized.toString();
  }

  private static NqFeedbackIngestionTransactionException persistenceFailure(
      final String safeMessage) {
    return new NqFeedbackIngestionTransactionException(
        NqFeedbackIngestionTransactionException.ErrorCode.PERSISTENCE_FAILURE, safeMessage);
  }
}
