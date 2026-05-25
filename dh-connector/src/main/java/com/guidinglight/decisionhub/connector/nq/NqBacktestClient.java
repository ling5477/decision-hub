package com.guidinglight.decisionhub.connector.nq;

import java.util.Map;

/**
 * Stage1：NQ 回测请求/状态查询客户端接口。
 *
 * <p>对应工单 4.5：NqBacktestClient。
 *
 * <p>硬约束：DH 不直接下单、不重写 NQ 回测核心；这里只表达“请求 NQ 执行正式回测”。
 */
public interface NqBacktestClient {

  /**
   * 提交一次正式回测请求。
   *
   * @param request 已经经过 DH 内部 Judge 仲裁的结构化请求。
   * @return NQ 返回的结构化响应（包含 jobId、status 等）。
   */
  Map<String, Object> submit(Map<String, Object> request);

  /** 查询某个 NQ 回测作业的状态。 */
  Map<String, Object> getJob(String jobId);
}
