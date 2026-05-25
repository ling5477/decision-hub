package com.guidinglight.decisionhub.domain.feedback.payload;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Stage2-PoC-B1：{@code PAPER_RUN_DAILY_REPORT_GENERATED} 事件 payload。
 *
 * <p>表示 NQ 侧 paper run 为某一交易日生成了日报；DH 用于经验权重和后续 reflection。
 */
public final class PaperRunDailyReportGeneratedPayload {

  private final String paperRunId;
  private final String reportId;
  private final LocalDate reportDate;
  private final Double realizedPnl;
  private final Double maxDrawdown;
  private final Double winRate;
  private final String rawPayloadJson;

  private PaperRunDailyReportGeneratedPayload(
      final String paperRunId,
      final String reportId,
      final LocalDate reportDate,
      final Double realizedPnl,
      final Double maxDrawdown,
      final Double winRate,
      final String rawPayloadJson) {
    this.paperRunId = Objects.requireNonNull(paperRunId, "paperRunId");
    this.reportId = Objects.requireNonNull(reportId, "reportId");
    this.reportDate = Objects.requireNonNull(reportDate, "reportDate");
    this.realizedPnl = realizedPnl;
    this.maxDrawdown = maxDrawdown;
    this.winRate = winRate;
    this.rawPayloadJson = Objects.requireNonNull(rawPayloadJson, "rawPayloadJson");
  }

  /** 工厂方法。 */
  public static PaperRunDailyReportGeneratedPayload of(
      final String paperRunId,
      final String reportId,
      final LocalDate reportDate,
      final Double realizedPnl,
      final Double maxDrawdown,
      final Double winRate,
      final String rawPayloadJson) {
    return new PaperRunDailyReportGeneratedPayload(
        paperRunId, reportId, reportDate, realizedPnl, maxDrawdown, winRate, rawPayloadJson);
  }

  public String getPaperRunId() {
    return paperRunId;
  }

  public String getReportId() {
    return reportId;
  }

  public LocalDate getReportDate() {
    return reportDate;
  }

  /** 已实现盈亏，可空。 */
  public Double getRealizedPnl() {
    return realizedPnl;
  }

  /** 最大回撤，可空（负值或绝对值由 NQ 侧约定）。 */
  public Double getMaxDrawdown() {
    return maxDrawdown;
  }

  /** 胜率，可空，范围 [0.0, 1.0]。 */
  public Double getWinRate() {
    return winRate;
  }

  /** 原始 payload JSON，禁止丢失。 */
  public String getRawPayloadJson() {
    return rawPayloadJson;
  }
}
