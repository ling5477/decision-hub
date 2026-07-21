package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionResult;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackAuditReference;
import java.time.Instant;
import java.util.Objects;

/**
 * 默认结构化归因编排。
 *
 * <p>执行顺序固定为 observation/scope 校验、canonicalization、原子幂等、纯策略归因、audit、不可变结果。
 * 本类不注册 Spring bean，不调用 Experience、Prompt、Provider、NQ、Agent、LangGraph 或任何交易写路径。
 */
public final class DefaultFeedbackAttributionService implements FeedbackAttributionService {

  private final FeedbackCanonicalizer canonicalizer;
  private final FeedbackAttributionPolicyEvaluator policyEvaluator;
  private final FeedbackAttributionIdempotencyPort idempotencyPort;
  private final FeedbackAttributionAuditPort auditPort;

  /** 注入纯计算器与两个 fail-closed ports；Stage-QDR-8 不提供 production adapter。 */
  public DefaultFeedbackAttributionService(
      final FeedbackCanonicalizer canonicalizer,
      final FeedbackAttributionPolicyEvaluator policyEvaluator,
      final FeedbackAttributionIdempotencyPort idempotencyPort,
      final FeedbackAttributionAuditPort auditPort) {
    this.canonicalizer = Objects.requireNonNull(canonicalizer, "canonicalizer");
    this.policyEvaluator = Objects.requireNonNull(policyEvaluator, "policyEvaluator");
    this.idempotencyPort = Objects.requireNonNull(idempotencyPort, "idempotencyPort");
    this.auditPort = Objects.requireNonNull(auditPort, "auditPort");
  }

  /** 执行 deterministic attribution；未知异常统一脱敏为 {@code ATTRIBUTION_FAILED}。 */
  @Override
  public FeedbackAttributionResult attribute(final FeedbackAttributionCommand command) {
    if (command == null) {
      return FeedbackAttributionResult.rejected(
          FeedbackAttributionErrorCode.INVALID_FEEDBACK_SUBJECT,
          "feedback attribution command is required");
    }
    final FeedbackAttributionResult validation = validate(command);
    if (validation != null) {
      return validation;
    }
    try {
      final FeedbackCanonicalHash canonicalHash = canonicalizer.canonicalize(command);
      return idempotencyPort.execute(
          canonicalHash.idempotencyKey(),
          canonicalHash.value(),
          () -> executeFirst(command, canonicalHash));
    } catch (FeedbackAttributionIdempotencyException error) {
      final FeedbackAttributionErrorCode code =
          error.kind() == FeedbackAttributionIdempotencyException.Kind.CONFLICT
              ? FeedbackAttributionErrorCode.IDEMPOTENCY_CONFLICT
              : FeedbackAttributionErrorCode.IDEMPOTENCY_UNAVAILABLE;
      return FeedbackAttributionResult.rejected(code, "feedback attribution idempotency rejected");
    } catch (FeedbackAttributionAuditException error) {
      return FeedbackAttributionResult.rejected(
          FeedbackAttributionErrorCode.AUDIT_FAILED,
          "feedback attribution audit reference unavailable");
    } catch (RuntimeException error) {
      return FeedbackAttributionResult.rejected(
          FeedbackAttributionErrorCode.ATTRIBUTION_FAILED,
          "feedback attribution failed closed");
    }
  }

  private FeedbackAttributionResult executeFirst(
      final FeedbackAttributionCommand command, final FeedbackCanonicalHash canonicalHash) {
    final FeedbackAttributionPolicyEvaluator.Evaluation evaluation =
        policyEvaluator.evaluate(command);
    final String resultIdentity = canonicalizer.resultIdentity(canonicalHash, evaluation);
    final FeedbackAttributionAuditRecord auditRecord =
        new FeedbackAttributionAuditRecord(
            command,
            canonicalHash,
            resultIdentity,
            evaluation.status(),
            evaluation.errorCode(),
            evaluation.contributions(),
            evaluation.confidence());
    final FeedbackAuditReference auditReference;
    try {
      auditReference = Objects.requireNonNull(auditPort.write(auditRecord), "auditReference");
      validateAuditReference(auditRecord, auditReference);
    } catch (FeedbackAttributionAuditException error) {
      throw error;
    } catch (RuntimeException error) {
      throw new FeedbackAttributionAuditException("audit write failed closed", error);
    }
    final AttributionResult domainResult =
        new AttributionResult(
            command.subject(),
            command.observation().observationId(),
            evaluation.status(),
            evaluation.contributions(),
            evaluation.confidence(),
            command.policy().policyId(),
            command.policy().policyVersion(),
            canonicalHash.value(),
            auditReference);
    return FeedbackAttributionResult.completed(
        domainResult, evaluation.errorCode(), evaluation.safeMessage());
  }

  private static FeedbackAttributionResult validate(final FeedbackAttributionCommand command) {
    final Instant observedAt = command.observation().observedAt();
    final Instant evaluationTime = command.policy().evaluationTime();
    if (observedAt.isAfter(evaluationTime)
        || observedAt.isBefore(evaluationTime.minus(command.policy().maxObservationAge()))) {
      return FeedbackAttributionResult.rejected(
          FeedbackAttributionErrorCode.INVALID_OBSERVATION,
          "observation time is outside the explicit policy window");
    }
    if (!command.subject().tenantId().equals(command.observation().tenantId())) {
      return FeedbackAttributionResult.rejected(
          FeedbackAttributionErrorCode.TENANT_SCOPE_MISMATCH,
          "feedback subject and observation tenant mismatch");
    }
    if (command.subject().environment() != command.observation().environment()) {
      return FeedbackAttributionResult.rejected(
          FeedbackAttributionErrorCode.ENVIRONMENT_SCOPE_MISMATCH,
          "feedback subject and observation environment mismatch");
    }
    if (!command.subject().decisionId().equals(command.observation().decisionId())) {
      return FeedbackAttributionResult.rejected(
          FeedbackAttributionErrorCode.DECISION_REFERENCE_INVALID,
          "feedback subject and observation decision mismatch");
    }
    if (!command.subject().traceId().equals(command.observation().traceId())) {
      return FeedbackAttributionResult.rejected(
          FeedbackAttributionErrorCode.TRACE_REFERENCE_INVALID,
          "feedback subject and observation trace mismatch");
    }
    return null;
  }

  private static void validateAuditReference(
      final FeedbackAttributionAuditRecord record,
      final FeedbackAuditReference reference) {
    if (!record.command().subject().tenantId().equals(reference.tenantId())
        || record.command().subject().environment() != reference.environment()
        || !record.command().subject().decisionId().equals(reference.decisionId())
        || !record.command().subject().traceId().equals(reference.traceId())
        || !record.command().observation().observationId().equals(reference.observationId())
        || !record.resultIdentity().equals(reference.resultIdentity())
        || !record.command().policy().policyId().equals(reference.policyId())
        || !record.command().policy().policyVersion().equals(reference.policyVersion())
        || !record.canonicalHash().value().equals(reference.canonicalHash())) {
      throw new FeedbackAttributionAuditException("audit reference scope mismatch");
    }
  }
}
