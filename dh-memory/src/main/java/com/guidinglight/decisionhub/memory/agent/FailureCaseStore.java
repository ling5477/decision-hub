package com.guidinglight.decisionhub.memory.agent;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Stage1：失败案例库读写端口。
 *
 * <p>对应工单 4.3：FailureCaseStore。第一阶段只保留结构化失败快照，不做语义索引。
 */
public interface FailureCaseStore {

  /**
   * 记录一次失败案例。
   *
   * @param tenantId 租户。
   * @param traceId 关联 traceId。
   * @param runId 关联 runId。
   * @param category 失败大类：RISK_REJECTED / BACKTEST_FAILED / RELEASE_REJECTED 等。
   * @param payloadJson 失败案例的结构化快照。
   * @param now 写入时间。
   */
  void record(
      String tenantId,
      String traceId,
      String runId,
      String category,
      Map<String, Object> payloadJson,
      Instant now);

  /** 列出某 run 下的所有失败案例。 */
  List<Map<String, Object>> listByRun(String tenantId, String runId);
}
