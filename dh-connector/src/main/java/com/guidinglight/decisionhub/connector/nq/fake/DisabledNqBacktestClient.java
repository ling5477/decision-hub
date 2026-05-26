package com.guidinglight.decisionhub.connector.nq.fake;

import com.guidinglight.decisionhub.connector.nq.NqBacktestClient;
import com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitResult;
import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import java.util.Map;

/**
 * Stage3-B3：DH 端"backtest-request 能力关闭"模式的 {@link NqBacktestClient} 实现。
 *
 * <p>装配条件（参见 STAGE3_DH_BACKTEST_ADAPTER_SPEC §7.2）：
 *
 * <pre>
 *   decisionhub.stage3.nq.enabled=true
 *   decisionhub.stage3.nq.backtest-request.enabled=false
 * </pre>
 *
 * <p>与默认 profile（stage3.nq.enabled=false → FakeNqBacktestClient 兜底）的差别：本类用于运维显式
 * 关闭 backtest-request 子能力的场景（例如 NQ 联调暂停 / NQ test cluster 不可用）。返回
 * {@link com.guidinglight.decisionhub.connector.nq.NqBacktestSubmitStatus#DISABLED}，
 * 由 caller 决定是否走 Fake 兜底或人工降级。
 *
 * <p>行为契约：
 *
 * <ul>
 *   <li>不抛 {@link RuntimeException}（不让 ResearchRun 主流程因能力关闭而失败）；
 *   <li>不发 HTTP；不读 token / API key；不消耗外部资源；
 *   <li>typed submit 返回 outcome=DISABLED + errorCode=DH_DISABLED；
 *   <li>Map 风格 submit / getJob 同样返回 DISABLED 占位 map，确保 caller 不会拿到 null。
 * </ul>
 */
public final class DisabledNqBacktestClient implements NqBacktestClient {

  /** DH 端 disabled 时的错误码（参见 SPEC §6.3 错误码映射表）。 */
  public static final String ERROR_CODE_DH_DISABLED = "DH_DISABLED";

  @Override
  public Map<String, Object> submit(final Map<String, Object> request) {
    return Map.of(
        "status", "DISABLED",
        "errorCode", ERROR_CODE_DH_DISABLED,
        "message", "backtest-request capability is disabled");
  }

  @Override
  public Map<String, Object> getJob(final String jobId) {
    return Map.of(
        "jobId", jobId == null ? "" : jobId,
        "status", "DISABLED",
        "errorCode", ERROR_CODE_DH_DISABLED);
  }

  @Override
  public NqBacktestSubmitResult submit(final DhBacktestRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request must not be null");
    }
    return NqBacktestSubmitResult.disabled(request.getRequestId(), ERROR_CODE_DH_DISABLED);
  }
}
