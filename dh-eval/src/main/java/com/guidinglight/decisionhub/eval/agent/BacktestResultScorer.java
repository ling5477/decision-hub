package com.guidinglight.decisionhub.eval.agent;

import java.util.Map;

/**
 * Stage1：回测结果评分器接口。
 *
 * <p>对应工单 4.4：BacktestResultScorer。Stage1 不调 NQ 真实回测核心，回测结果以快照形式注入。
 */
public interface BacktestResultScorer {

  /**
   * 对回测快照评分。
   *
   * @param backtestSnapshot NQ 回测的结构化结果快照。
   * @return 包含 {"backtestScore": double} 的结构化快照。
   */
  Map<String, Object> score(Map<String, Object> backtestSnapshot);
}
