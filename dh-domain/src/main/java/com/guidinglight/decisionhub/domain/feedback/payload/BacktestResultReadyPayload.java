package com.guidinglight.decisionhub.domain.feedback.payload;

import com.guidinglight.decisionhub.domain.backtest.BacktestVerdict;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Stage2-PoC-B1：{@code BACKTEST_RESULT_READY} 事件 payload。
 *
 * <p>表示 NQ 侧某次 backtest 的结果已经准备就绪。 {@code requestId} 关联 DH 发起的 {@link
 * com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest}；DH 入口 必须根据 {@code requestId} 找到原请求并更新状态机。
 */
public final class BacktestResultReadyPayload {

  private final String backtestId;
  private final String requestId;
  private final String candidateId;
  private final Double sharpeRatio;
  private final Double maxDrawdown;
  private final Double annualReturn;
  private final Double winRate;
  private final Double profitFactor;
  private final LocalDate periodStart;
  private final LocalDate periodEnd;
  private final BacktestVerdict verdict;
  private final Instant readyAt;
  private final String rawPayloadJson;

  private BacktestResultReadyPayload(
      final String backtestId,
      final String requestId,
      final String candidateId,
      final Double sharpeRatio,
      final Double maxDrawdown,
      final Double annualReturn,
      final Double winRate,
      final Double profitFactor,
      final LocalDate periodStart,
      final LocalDate periodEnd,
      final BacktestVerdict verdict,
      final Instant readyAt,
      final String rawPayloadJson) {
    this.backtestId = Objects.requireNonNull(backtestId, "backtestId");
    this.requestId = Objects.requireNonNull(requestId, "requestId");
    this.candidateId = Objects.requireNonNull(candidateId, "candidateId");
    this.sharpeRatio = sharpeRatio;
    this.maxDrawdown = maxDrawdown;
    this.annualReturn = annualReturn;
    this.winRate = winRate;
    this.profitFactor = profitFactor;
    this.periodStart = Objects.requireNonNull(periodStart, "periodStart");
    this.periodEnd = Objects.requireNonNull(periodEnd, "periodEnd");
    this.verdict = Objects.requireNonNull(verdict, "verdict");
    this.readyAt = Objects.requireNonNull(readyAt, "readyAt");
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static BacktestResultReadyPayload of(
      final String backtestId,
      final String requestId,
      final String candidateId,
      final Double sharpeRatio,
      final Double maxDrawdown,
      final Double annualReturn,
      final Double winRate,
      final Double profitFactor,
      final LocalDate periodStart,
      final LocalDate periodEnd,
      final BacktestVerdict verdict,
      final Instant readyAt,
      final String rawPayloadJson) {
    return new BacktestResultReadyPayload(
        backtestId,
        requestId,
        candidateId,
        sharpeRatio,
        maxDrawdown,
        annualReturn,
        winRate,
        profitFactor,
        periodStart,
        periodEnd,
        verdict,
        readyAt,
        rawPayloadJson);
  }

  public String getBacktestId() {
    return backtestId;
  }

  /** 关联 DH 发起 backtest 时生成的 requestId（{@link
   * com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest#getRequestId()}）。 */
  public String getRequestId() {
    return requestId;
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

  public Instant getReadyAt() {
    return readyAt;
  }

  /** 原始 payload JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
