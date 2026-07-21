package com.guidinglight.decisionhub.usecase.qdr.feedback;

import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionContribution;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionDimension;
import com.guidinglight.decisionhub.domain.qdr.feedback.AttributionImpact;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackConfidence;
import com.guidinglight.decisionhub.domain.qdr.feedback.FeedbackStatus;
import com.guidinglight.decisionhub.domain.qdr.feedback.ObservedDecisionOutcome;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 纯确定性、可解释且无副作用的 attribution policy evaluator。 */
public final class FeedbackAttributionPolicyEvaluator {

  private static final int CALCULATION_SCALE = 12;

  /**
   * 执行策略允许性、证据完整性、贡献与矛盾校验。
   *
   * <p>相同 command 始终产生逐字段相同的 evaluation，不读取时钟或随机数。
   */
  public Evaluation evaluate(final FeedbackAttributionCommand command) {
    final FeedbackAttributionCommand checked = Objects.requireNonNull(command, "command");
    if (!checked.policy().allowedSources().contains(checked.observation().source())) {
      return new Evaluation(
          FeedbackStatus.REJECTED,
          FeedbackAttributionErrorCode.OUTCOME_SOURCE_DENIED,
          List.of(),
          FeedbackConfidence.zero(),
          "outcome source denied by policy");
    }

    final List<AttributionContribution> contributions = new ArrayList<>();
    boolean incomplete = false;
    for (AttributionDimension dimension : checked.policy().requiredDimensions()) {
      final BigDecimal measurement = checked.observation().measurements().get(dimension);
      final String evidence = checked.observation().evidenceReferences().get(dimension);
      if (measurement == null || evidence == null) {
        incomplete = true;
        continue;
      }
      final BigDecimal weighted =
          measurement
              .multiply(checked.policy().dimensionWeights().get(dimension))
              .setScale(CALCULATION_SCALE, RoundingMode.HALF_EVEN)
              .stripTrailingZeros();
      final AttributionImpact impact =
          weighted.signum() > 0
              ? AttributionImpact.POSITIVE
              : weighted.signum() < 0
                  ? AttributionImpact.NEGATIVE
                  : AttributionImpact.NEUTRAL;
      final String reasonCode =
          weighted.signum() > 0
              ? "MEASUREMENT_POSITIVE"
              : weighted.signum() < 0 ? "MEASUREMENT_NEGATIVE" : "MEASUREMENT_NEUTRAL";
      contributions.add(
          new AttributionContribution(
              dimension,
              weighted,
              impact,
              new FeedbackConfidence(measurement.abs().doubleValue()),
              reasonCode,
              evidence));
    }
    contributions.sort(null);
    final FeedbackConfidence confidence = confidence(contributions);
    if (incomplete) {
      return new Evaluation(
          FeedbackStatus.INCONCLUSIVE,
          FeedbackAttributionErrorCode.EVIDENCE_INCOMPLETE,
          contributions,
          confidence,
          "mandatory structured evidence is incomplete");
    }

    final int aggregateSign =
        contributions.stream()
            .map(AttributionContribution::contribution)
            .reduce(BigDecimal.ZERO, BigDecimal::add)
            .signum();
    if (contradicts(checked.observation().outcome(), aggregateSign)) {
      return new Evaluation(
          FeedbackStatus.REJECTED,
          FeedbackAttributionErrorCode.ATTRIBUTION_CONTRADICTION,
          contributions,
          confidence,
          "structured outcome contradicts aggregate contribution");
    }
    return new Evaluation(
        FeedbackStatus.ATTRIBUTED,
        FeedbackAttributionErrorCode.NONE,
        contributions,
        confidence,
        "deterministic attribution completed");
  }

  private static boolean contradicts(
      final ObservedDecisionOutcome outcome, final int aggregateSign) {
    return (outcome == ObservedDecisionOutcome.SUCCEEDED && aggregateSign < 0)
        || (outcome == ObservedDecisionOutcome.FAILED && aggregateSign > 0);
  }

  private static FeedbackConfidence confidence(
      final List<AttributionContribution> contributions) {
    if (contributions.isEmpty()) {
      return FeedbackConfidence.zero();
    }
    final double average =
        contributions.stream()
                .mapToDouble(value -> value.confidence().value())
                .sum()
            / contributions.size();
    return new FeedbackConfidence(average);
  }

  /**
   * Audit 前的不可变确定性 evaluation。
   *
   * @param status 归因状态
   * @param errorCode 稳定错误码
   * @param contributions 稳定排序贡献
   * @param confidence 整体置信度
   * @param safeMessage 不含原始异常或 payload 的说明
   */
  public record Evaluation(
      FeedbackStatus status,
      FeedbackAttributionErrorCode errorCode,
      List<AttributionContribution> contributions,
      FeedbackConfidence confidence,
      String safeMessage) {

    /** 冻结 evaluation 并拒绝状态字段缺失。 */
    public Evaluation {
      status = Objects.requireNonNull(status, "status");
      errorCode = Objects.requireNonNull(errorCode, "errorCode");
      contributions = List.copyOf(Objects.requireNonNull(contributions, "contributions"));
      confidence = Objects.requireNonNull(confidence, "confidence");
      safeMessage = Objects.requireNonNull(safeMessage, "safeMessage");
    }
  }
}
