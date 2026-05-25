package com.guidinglight.decisionhub.domain.backtest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Stage2-PoC-B1：NQ 回测结果在 DH 侧的领域快照。
 *
 * <p>不同于 {@link
 * com.guidinglight.decisionhub.domain.feedback.payload.BacktestResultReadyPayload}（事件传输 payload），本对象 是 DH
 * 内部沉淀下来的领域快照：使用 {@code requestId} 关联回发起的 {@link DhBacktestRequest}，{@code rawPayloadJson} 保留 NQ 原始响应。
 *
 * <p>本对象不参与交易执行；仅作为 ExperienceEntry / reflection 的强化依据。
 */
public final class DhBacktestResultSnapshot {

  private final String resultId;
  private final String requestId;
  private final String traceId;
  private final String candidateId;
  private final Double sharpeRatio;
  private final Double maxDrawdown;
  private final Double annualReturn;
  private final Double winRate;
  private final Double profitFactor;
  private final LocalDate periodStart;
  private final LocalDate periodEnd;
  private final BacktestVerdict verdict;
  private final Instant recordedAt;
  private final String rawPayloadJson;

  private DhBacktestResultSnapshot(
      final String resultId,
      final String requestId,
      final String traceId,
      final String candidateId,
      final Double sharpeRatio,
      final Double maxDrawdown,
      final Double annualReturn,
      final Double winRate,
      final Double profitFactor,
      final LocalDate periodStart,
      final LocalDate periodEnd,
      final BacktestVerdict verdict,
      final Instant recordedAt,
      final String rawPayloadJson) {
    this.resultId = Objects.requireNonNull(resultId, "resultId");
    this.requestId = Objects.requireNonNull(requestId, "requestId");
    this.traceId = Objects.requireNonNull(traceId, "traceId");
    this.candidateId = Objects.requireNonNull(candidateId, "candidateId");
    this.sharpeRatio = sharpeRatio;
    this.maxDrawdown = maxDrawdown;
    this.annualReturn = annualReturn;
    this.winRate = winRate;
    this.profitFactor = profitFactor;
    this.periodStart = Objects.requireNonNull(periodStart, "periodStart");
    this.periodEnd = Objects.requireNonNull(periodEnd, "periodEnd");
    this.verdict = Objects.requireNonNull(verdict, "verdict");
    this.recordedAt = Objects.requireNonNull(recordedAt, "recordedAt");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static DhBacktestResultSnapshot of(
      final String resultId,
      final String requestId,
      final String traceId,
      final String candidateId,
      final Double sharpeRatio,
      final Double maxDrawdown,
      final Double annualReturn,
      final Double winRate,
      final Double profitFactor,
      final LocalDate periodStart,
      final LocalDate periodEnd,
      final BacktestVerdict verdict,
      final Instant recordedAt,
      final String rawPayloadJson) {
    return new DhBacktestResultSnapshot(
        resultId,
        requestId,
        traceId,
        candidateId,
        sharpeRatio,
        maxDrawdown,
        annualReturn,
        winRate,
        profitFactor,
        periodStart,
        periodEnd,
        verdict,
        recordedAt,
        rawPayloadJson);
  }

  public String getResultId() {
    return resultId;
  }

  public String getRequestId() {
    return requestId;
  }

  public String getTraceId() {
    return traceId;
  }

  public String getCandidateId() {
    return candidateId;
  }

  public Double getSharpeRatio() {
    return sharpeRatio;
  }

  public Double getMaxDrawdown() {
    return maxDrawdown;
  }

  public Double getAnnualReturn() {
    return annualReturn;
  }

  public Double getWinRate() {
    return winRate;
  }

  public Double getProfitFactor() {
    return profitFactor;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public BacktestVerdict getVerdict() {
    return verdict;
  }

  public Instant getRecordedAt() {
    return recordedAt;
  }

  /** 原始 NQ 响应 JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
