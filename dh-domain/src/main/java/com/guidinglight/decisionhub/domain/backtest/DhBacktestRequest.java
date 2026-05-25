package com.guidinglight.decisionhub.domain.backtest;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Stage2-PoC-B1：DH -> NQ 回测请求契约。
 *
 * <p>DH 发往 NQ {@code POST /api/ai/backtest-requests} 的最小信息集合。
 *
 * <p>注意：本对象不代表交易指令；DH 只是“请求 NQ 正式回测能力”。 任何把它当作下单 / 实盘提交 / 风控绕行的使用都违反 Stage2 边界。
 *
 * <p>策略参数以 {@code strategyParametersJson} 字符串承载，由 NQ 侧解析；DH 不在领域里强解析。
 */
public final class DhBacktestRequest {

  private final String requestId;
  private final String traceId;
  private final String candidateId;
  private final String strategyName;
  private final String strategyVersion;
  private final String strategyParametersJson;
  private final String entryRulesRef;
  private final String exitRulesRef;
  private final LocalDate startDate;
  private final LocalDate endDate;
  private final double initialCapital;
  private final List<String> symbols;
  private final BacktestFrequency frequency;
  private final String requestedBy;
  private final Instant requestedAt;
  private final DhBacktestRequestStatus status;

  private DhBacktestRequest(
      final String requestId,
      final String traceId,
      final String candidateId,
      final String strategyName,
      final String strategyVersion,
      final String strategyParametersJson,
      final String entryRulesRef,
      final String exitRulesRef,
      final LocalDate startDate,
      final LocalDate endDate,
      final double initialCapital,
      final List<String> symbols,
      final BacktestFrequency frequency,
      final String requestedBy,
      final Instant requestedAt,
      final DhBacktestRequestStatus status) {
    this.requestId = Objects.requireNonNull(requestId, "requestId");
    this.traceId = Objects.requireNonNull(traceId, "traceId");
    this.candidateId = Objects.requireNonNull(candidateId, "candidateId");
    this.strategyName = Objects.requireNonNull(strategyName, "strategyName");
    this.strategyVersion = Objects.requireNonNull(strategyVersion, "strategyVersion");
    this.strategyParametersJson =
        Objects.requireNonNull(strategyParametersJson, "strategyParametersJson");
    this.entryRulesRef = entryRulesRef;
    this.exitRulesRef = exitRulesRef;
    this.startDate = Objects.requireNonNull(startDate, "startDate");
    this.endDate = Objects.requireNonNull(endDate, "endDate");
    if (endDate.isBefore(startDate)) {
      throw new IllegalArgumentException("endDate must not be before startDate");
    }
    if (initialCapital <= 0.0) {
      throw new IllegalArgumentException("initialCapital must be positive");
    }
    this.initialCapital = initialCapital;
    this.symbols = symbols == null ? List.of() : List.copyOf(symbols);
    if (this.symbols.isEmpty()) {
      throw new IllegalArgumentException("symbols must not be empty");
    }
    this.frequency = Objects.requireNonNull(frequency, "frequency");
    this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy");
    this.requestedAt = Objects.requireNonNull(requestedAt, "requestedAt");
    this.status = Objects.requireNonNull(status, "status");
  }

  /** 工厂：默认状态 {@link DhBacktestRequestStatus#DRAFT}。 */
  public static DhBacktestRequest draft(
      final String requestId,
      final String traceId,
      final String candidateId,
      final String strategyName,
      final String strategyVersion,
      final String strategyParametersJson,
      final String entryRulesRef,
      final String exitRulesRef,
      final LocalDate startDate,
      final LocalDate endDate,
      final double initialCapital,
      final List<String> symbols,
      final BacktestFrequency frequency,
      final String requestedBy,
      final Instant requestedAt) {
    return new DhBacktestRequest(
        requestId,
        traceId,
        candidateId,
        strategyName,
        strategyVersion,
        strategyParametersJson,
        entryRulesRef,
        exitRulesRef,
        startDate,
        endDate,
        initialCapital,
        symbols,
        frequency,
        requestedBy,
        requestedAt,
        DhBacktestRequestStatus.DRAFT);
  }

  /** 重建用：从持久层 rehydrate。 */
  public static DhBacktestRequest rehydrate(
      final String requestId,
      final String traceId,
      final String candidateId,
      final String strategyName,
      final String strategyVersion,
      final String strategyParametersJson,
      final String entryRulesRef,
      final String exitRulesRef,
      final LocalDate startDate,
      final LocalDate endDate,
      final double initialCapital,
      final List<String> symbols,
      final BacktestFrequency frequency,
      final String requestedBy,
      final Instant requestedAt,
      final DhBacktestRequestStatus status) {
    return new DhBacktestRequest(
        requestId,
        traceId,
        candidateId,
        strategyName,
        strategyVersion,
        strategyParametersJson,
        entryRulesRef,
        exitRulesRef,
        startDate,
        endDate,
        initialCapital,
        symbols,
        frequency,
        requestedBy,
        requestedAt,
        status);
  }

  /** 返回一个新对象（状态前进）；本类保持不可变。 */
  public DhBacktestRequest withStatus(final DhBacktestRequestStatus next) {
    return new DhBacktestRequest(
        requestId,
        traceId,
        candidateId,
        strategyName,
        strategyVersion,
        strategyParametersJson,
        entryRulesRef,
        exitRulesRef,
        startDate,
        endDate,
        initialCapital,
        symbols,
        frequency,
        requestedBy,
        requestedAt,
        next);
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

  public String getStrategyName() {
    return strategyName;
  }

  public String getStrategyVersion() {
    return strategyVersion;
  }

  /** 策略参数原始 JSON 字符串，禁止丢失。 */
  public String getStrategyParametersJson() {
    return strategyParametersJson;
  }

  /** entry 规则引用（DSL 名 / 文档 ID），可空。 */
  public String getEntryRulesRef() {
    return entryRulesRef;
  }

  /** exit 规则引用（DSL 名 / 文档 ID），可空。 */
  public String getExitRulesRef() {
    return exitRulesRef;
  }

  public LocalDate getStartDate() {
    return startDate;
  }

  public LocalDate getEndDate() {
    return endDate;
  }

  public double getInitialCapital() {
    return initialCapital;
  }

  public List<String> getSymbols() {
    return symbols;
  }

  public BacktestFrequency getFrequency() {
    return frequency;
  }

  public String getRequestedBy() {
    return requestedBy;
  }

  public Instant getRequestedAt() {
    return requestedAt;
  }

  public DhBacktestRequestStatus getStatus() {
    return status;
  }
}
