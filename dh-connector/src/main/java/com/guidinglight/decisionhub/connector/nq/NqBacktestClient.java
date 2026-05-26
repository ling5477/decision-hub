package com.guidinglight.decisionhub.connector.nq;

import com.guidinglight.decisionhub.domain.backtest.DhBacktestRequest;
import java.util.Map;

/**
 * DH -&gt; NQ 回测请求出站客户端端口（Stage1 落地 Map 风格；Stage3-B3 扩展 typed 风格）。
 *
 * <p>对应 STAGE3_DH_BACKTEST_ADAPTER_SPEC §3.2 与 §7。
 *
 * <p>硬约束（不可放松）：
 *
 * <ul>
 *   <li>DH 不直接下单 / 不绕风控 / 不重写 NQ 回测核心。本端口只表达"请求 NQ 执行正式回测"。
 *   <li>NQ 仍是唯一正式回测执行方；本端口仅产生 NQ 队列入队语义；不产生回测结果。
 *   <li>RESULT_READY 只能来自 NQ feedback {@code BACKTEST_RESULT_READY} 事件，
 *       不能来自本端口的同步响应。
 *   <li>Stage3-B3 本轮仅允许 Fake / Disabled 实现；Real HTTP 客户端推迟到后续 IMPL Batch。
 *       本接口默认实现必须满足"无 NQ 时 DH 仍可启动"。
 * </ul>
 */
public interface NqBacktestClient {

  /**
   * Stage1：Map 风格提交（保留向后兼容；新代码请用 {@link #submit(DhBacktestRequest)}）。
   *
   * @param request 已经经过 DH 内部 Judge 仲裁的结构化请求（任意键值结构）
   * @return NQ 返回的结构化响应（包含 jobId、status 等）
   */
  Map<String, Object> submit(Map<String, Object> request);

  /** 查询某个 NQ 回测作业的状态（Stage1 兼容方法）。 */
  Map<String, Object> getJob(String jobId);

  /**
   * Stage3-B3：typed 风格提交（推荐使用）。
   *
   * <p>语义参见 STAGE3_DH_BACKTEST_ADAPTER_SPEC §6.2。
   *
   * <p>本方法默认实现委托给 Map 风格 {@link #submit(Map)}，并把 jobId 与 status 映射到
   * {@link NqBacktestSubmitResult}。实现方可以覆盖以提供更精确的同步响应解析。
   *
   * <p>异常约定（强制）：
   *
   * <ul>
   *   <li>不允许抛 {@link RuntimeException} 中断 caller；任何错误必须映射到 {@link NqBacktestSubmitStatus#FAILED}。
   *   <li>能力关闭场景必须返回 {@link NqBacktestSubmitStatus#DISABLED}，由 caller 决定降级。
   * </ul>
   *
   * @param request DH 端 typed 请求；不允许为 {@code null}
   * @return 同步阶段结果；含 ACCEPTED / DUPLICATE / DISABLED / FAILED 之一
   */
  default NqBacktestSubmitResult submit(final DhBacktestRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("request must not be null");
    }
    try {
      final Map<String, Object> raw =
          submit(
              Map.of(
                  "requestId", request.getRequestId(),
                  "traceId", request.getTraceId(),
                  "candidateId", request.getCandidateId(),
                  "strategyName", request.getStrategyName(),
                  "strategyVersion", request.getStrategyVersion(),
                  "strategyParametersJson", request.getStrategyParametersJson(),
                  "startDate", request.getStartDate().toString(),
                  "endDate", request.getEndDate().toString(),
                  "initialCapital", request.getInitialCapital(),
                  "frequency", request.getFrequency().name()));
      if (raw == null) {
        return NqBacktestSubmitResult.failed(
            request.getRequestId(), "INVALID_RESPONSE", "null response");
      }
      final Object jobId = raw.get("jobId");
      if (jobId == null) {
        return NqBacktestSubmitResult.failed(
            request.getRequestId(), "INVALID_RESPONSE", "missing jobId");
      }
      return NqBacktestSubmitResult.accepted(
          request.getRequestId(), jobId.toString(), java.time.Instant.now());
    } catch (final RuntimeException e) {
      return NqBacktestSubmitResult.failed(
          request.getRequestId(), "ADAPTER_ERROR", e.getClass().getSimpleName());
    }
  }
}
