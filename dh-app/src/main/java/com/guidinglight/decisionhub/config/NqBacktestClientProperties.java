package com.guidinglight.decisionhub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Stage3-B3：DH 端 backtest request adapter 配置项。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §10.1。
 *
 * <p>位置说明：本类位于 dh-app 而非 dh-connector，避免给 dh-connector 引入 Spring Boot 依赖。
 * SPEC §3.2 列出的"建议路径"是 dh-connector；此处依据 SPEC 末段"路径仅供参考"调整到 dh-app。
 *
 * <p>装配三层 gate（参见 SPEC §2.2 装配真值表）：
 *
 * <pre>
 *   stage3.nq.enabled | backtest-request.enabled | fake-mode | 装配结果
 *   ----------------- + ------------------------ + --------- + --------------------
 *   false             | (any)                    | (any)     | FakeNqBacktestClient（默认兜底）
 *   true              | false                    | (any)     | DisabledNqBacktestClient
 *   true              | true                     | true      | FakeNqBacktestClient
 *   true              | true                     | false     | RealNqBacktestClient（Stage3-B3 本轮不实现）
 * </pre>
 *
 * <p>Stage3-B3 本轮硬约束：RealNqBacktestClient 不实现；fake-mode=false 时仍走 Fake 兜底，
 * 等同 fake-mode=true 行为；联调启动 RealClient 推迟到 Stage3-B4 IMPL Batch。
 */
@ConfigurationProperties(prefix = "decisionhub.stage3.nq")
public class NqBacktestClientProperties {

  /** Stage3 NQ 集成总开关；默认 false。 */
  private boolean enabled = false;

  /** NQ test cluster base url；仅在 enabled=true 且 RealClient 装配时使用。 */
  private String baseUrl = "";

  /** 子能力配置：backtest-request。 */
  private BacktestRequest backtestRequest = new BacktestRequest();

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
  }

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(final String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public BacktestRequest getBacktestRequest() {
    return backtestRequest;
  }

  public void setBacktestRequest(final BacktestRequest backtestRequest) {
    this.backtestRequest = backtestRequest;
  }

  /** backtest-request 子配置。 */
  public static class BacktestRequest {

    /** 子能力开关；默认 false。 */
    private boolean enabled = false;

    /** Fake 模式（不发 HTTP；deterministic）；默认 true。 */
    private boolean fakeMode = true;

    /** NQ 端 endpoint 路径（建议 /api/ai/backtest-requests；本 Batch 不直连）。 */
    private String endpointPath = "/api/ai/backtest-requests";

    /** HTTP 客户端读取超时（ms）；本 Batch 仅占位。 */
    private int timeoutMs = 30000;

    /** HTTP 客户端连接超时（ms）。 */
    private int connectTimeoutMs = 10000;

    /** 重试上限；默认 0（不重试）。 */
    private int maxRetries = 0;

    /** 幂等窗口（小时）；默认 24。 */
    private int idempotencyWindowHours = 24;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(final boolean enabled) {
      this.enabled = enabled;
    }

    public boolean isFakeMode() {
      return fakeMode;
    }

    public void setFakeMode(final boolean fakeMode) {
      this.fakeMode = fakeMode;
    }

    public String getEndpointPath() {
      return endpointPath;
    }

    public void setEndpointPath(final String endpointPath) {
      this.endpointPath = endpointPath;
    }

    public int getTimeoutMs() {
      return timeoutMs;
    }

    public void setTimeoutMs(final int timeoutMs) {
      this.timeoutMs = timeoutMs;
    }

    public int getConnectTimeoutMs() {
      return connectTimeoutMs;
    }

    public void setConnectTimeoutMs(final int connectTimeoutMs) {
      this.connectTimeoutMs = connectTimeoutMs;
    }

    public int getMaxRetries() {
      return maxRetries;
    }

    public void setMaxRetries(final int maxRetries) {
      this.maxRetries = maxRetries;
    }

    public int getIdempotencyWindowHours() {
      return idempotencyWindowHours;
    }

    public void setIdempotencyWindowHours(final int idempotencyWindowHours) {
      this.idempotencyWindowHours = idempotencyWindowHours;
    }
  }
}
