package com.guidinglight.decisionhub.usecase.agent.backtest;

import com.guidinglight.decisionhub.domain.backtest.BacktestFrequency;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Stage3-B3：DH 端 backtest request 入参 Command（service 边界 DTO）。
 *
 * <p>与 wire-level {@code DhBacktestRequest} 解耦：
 *
 * <ul>
 *   <li>Command 是 DH 业务上下文输入；wire-level 是 NQ HTTP body schema；
 *   <li>service 内由 IdGenerator 生成 requestId，不接受外部传入 requestId（保证幂等键一致性）；
 *   <li>sourceJobId 不在 Command 内（由 NQ 同步响应给出）；
 *   <li>correlationId 在 Command 内持有，但不落 wire-level body（仅在 HTTP header / feedback envelope 携带）。
 * </ul>
 *
 * <p>禁止字段（参见 STAGE3_DH_BACKTEST_ADAPTER_SPEC §5.4）：
 * 不允许携带 API key / secret / token / 实盘账户凭证 / 下单指令 / 绕风控标记。
 */
public final class DhBacktestRequestCommand {

  private final String traceId;
  private final String correlationId;
  private final String candidateId;
  private final String strategyName;
  private final String strategyVersion;
  private final String strategyParametersJson;
  private final LocalDate startDate;
  private final LocalDate endDate;
  private final double initialCapital;
  private final List<String> symbols;
  private final BacktestFrequency frequency;
  private final String requestedBy;

  private DhBacktestRequestCommand(final Builder b) {
    this.traceId = Objects.requireNonNull(b.traceId, "traceId");
    this.correlationId = Objects.requireNonNull(b.correlationId, "correlationId");
    this.candidateId = Objects.requireNonNull(b.candidateId, "candidateId");
    this.strategyName = Objects.requireNonNull(b.strategyName, "strategyName");
    this.strategyVersion = Objects.requireNonNull(b.strategyVersion, "strategyVersion");
    this.strategyParametersJson =
        Objects.requireNonNull(b.strategyParametersJson, "strategyParametersJson");
    this.startDate = Objects.requireNonNull(b.startDate, "startDate");
    this.endDate = Objects.requireNonNull(b.endDate, "endDate");
    this.initialCapital = b.initialCapital;
    this.symbols = b.symbols == null ? List.of() : List.copyOf(b.symbols);
    this.frequency = Objects.requireNonNull(b.frequency, "frequency");
    this.requestedBy = Objects.requireNonNull(b.requestedBy, "requestedBy");
  }

  public static Builder builder() {
    return new Builder();
  }

  public String getTraceId() {
    return traceId;
  }

  public String getCorrelationId() {
    return correlationId;
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

  public String getStrategyParametersJson() {
    return strategyParametersJson;
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

  /** Builder：减少长参数列表使用错位风险。 */
  public static final class Builder {
    private String traceId;
    private String correlationId;
    private String candidateId;
    private String strategyName;
    private String strategyVersion;
    private String strategyParametersJson;
    private LocalDate startDate;
    private LocalDate endDate;
    private double initialCapital;
    private List<String> symbols;
    private BacktestFrequency frequency;
    private String requestedBy;

    public Builder traceId(final String v) {
      this.traceId = v;
      return this;
    }

    public Builder correlationId(final String v) {
      this.correlationId = v;
      return this;
    }

    public Builder candidateId(final String v) {
      this.candidateId = v;
      return this;
    }

    public Builder strategyName(final String v) {
      this.strategyName = v;
      return this;
    }

    public Builder strategyVersion(final String v) {
      this.strategyVersion = v;
      return this;
    }

    public Builder strategyParametersJson(final String v) {
      this.strategyParametersJson = v;
      return this;
    }

    public Builder startDate(final LocalDate v) {
      this.startDate = v;
      return this;
    }

    public Builder endDate(final LocalDate v) {
      this.endDate = v;
      return this;
    }

    public Builder initialCapital(final double v) {
      this.initialCapital = v;
      return this;
    }

    public Builder symbols(final List<String> v) {
      this.symbols = v;
      return this;
    }

    public Builder frequency(final BacktestFrequency v) {
      this.frequency = v;
      return this;
    }

    public Builder requestedBy(final String v) {
      this.requestedBy = v;
      return this;
    }

    public DhBacktestRequestCommand build() {
      return new DhBacktestRequestCommand(this);
    }
  }
}
