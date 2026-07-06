package com.guidinglight.decisionhub.usecase.qdr;

import com.guidinglight.decisionhub.domain.qdr.QuantSignal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * `quant_signal` 主线 repository port。
 *
 * <p>实现只保存只读 signal 摘要，不保存凭证、可执行订单 payload 或 provider 原始响应。
 */
public interface QuantSignalRepository {

  /**
   * 保存 quant signal。
   *
   * @param signal signal 摘要。
   */
  void save(QuantSignal signal);

  /**
   * 按 request ID 查询 signal。
   *
   * @param decisionRequestId 主线请求 ID。
   * @return signal 列表。
   */
  List<QuantSignal> findSignalsByDecisionRequestId(UUID decisionRequestId);

  /**
   * 按 signal ID 查询。
   *
   * @param id signal ID。
   * @return signal。
   */
  Optional<QuantSignal> findSignalById(UUID id);
}
