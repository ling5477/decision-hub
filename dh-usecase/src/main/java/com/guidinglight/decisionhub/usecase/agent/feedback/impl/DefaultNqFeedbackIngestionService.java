package com.guidinglight.decisionhub.usecase.agent.feedback.impl;

import com.guidinglight.decisionhub.domain.feedback.NqFeedbackEnvelope;
import com.guidinglight.decisionhub.usecase.agent.NqFeedbackEventRepository;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionCommand;
import com.guidinglight.decisionhub.usecase.agent.feedback.IngestionResult;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackContractValidator;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackEventTypeRouter;
import com.guidinglight.decisionhub.usecase.agent.feedback.NqFeedbackIngestionService;
import com.guidinglight.decisionhub.usecase.agent.feedback.ValidationResult;
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

  private final NqFeedbackContractValidator validator;
  private final NqFeedbackEventRepository repository;
  private final NqFeedbackEventTypeRouter router;

  public DefaultNqFeedbackIngestionService(
      final NqFeedbackContractValidator validator,
      final NqFeedbackEventRepository repository,
      final NqFeedbackEventTypeRouter router) {
    this.validator = validator;
    this.repository = repository;
    this.router = router;
  }

  @Override
  public IngestionResult ingest(final IngestionCommand command) {
    // 1. 幂等优先：eventId 为空时不查；非空命中即返回 DUPLICATE。
    if (command.getEventId() != null && !command.getEventId().isBlank()) {
      final Optional<NqFeedbackEnvelope> existing =
          repository.findEnvelopeByEventId(command.getEventId());
      if (existing.isPresent()) {
        return IngestionResult.duplicate(command.getEventId());
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
      return IngestionResult.duplicate(envelope.getEventId());
    }

    // 4. 派发 handler；handler 内部失败由 handler 自身降级，不影响 ACCEPTED 响应。
    router.route(envelope);

    return IngestionResult.accepted(envelope.getEventId());
  }
}
