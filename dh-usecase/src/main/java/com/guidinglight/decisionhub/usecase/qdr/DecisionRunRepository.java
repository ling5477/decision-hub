package com.guidinglight.decisionhub.usecase.qdr;

import com.guidinglight.decisionhub.domain.qdr.DecisionRun;
import com.guidinglight.decisionhub.domain.qdr.DecisionRunStatus;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * `decision_run` 主线 repository port。
 *
 * <p>run 必须可按 `decision_request_id` 查询；完成或失败更新必须 fail-closed。
 */
public interface DecisionRunRepository {

  /**
   * 保存 run 初始记录。
   *
   * @param run run 记录。
   */
  void save(DecisionRun run);

  /**
   * 完成 run 状态。
   *
   * @param id run ID。
   * @param status 结束状态。
   * @param finishedAt 结束时间。
   * @param latencyMs 耗时。
   * @param errorCode 错误码。
   * @param errorMessage 脱敏错误摘要。
   */
  void complete(
      UUID id,
      DecisionRunStatus status,
      Instant finishedAt,
      Long latencyMs,
      String errorCode,
      String errorMessage);

  /**
   * 按 request ID 查询 runs。
   *
   * @param decisionRequestId 主线请求 ID。
   * @return run 列表。
   */
  List<DecisionRun> findByDecisionRequestId(UUID decisionRequestId);

  /**
   * 按 run ID 查询。
   *
   * @param id run ID。
   * @return run。
   */
  Optional<DecisionRun> findById(UUID id);
}
