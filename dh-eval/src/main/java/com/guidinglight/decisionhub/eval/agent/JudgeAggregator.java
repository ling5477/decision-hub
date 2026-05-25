package com.guidinglight.decisionhub.eval.agent;

import java.util.List;
import java.util.Map;

/**
 * Stage1：Judge 综合聚合器接口。
 *
 * <p>对应工单 4.4：JudgeAggregator。将多个 scorer 输出聚合为 Judge 的综合分数与选择结果。
 */
public interface JudgeAggregator {

  /**
   * 聚合一个候选的多维评分快照为最终 Judge 视角的分数。
   *
   * @param scoreSnapshots 各 scorer 的输出列表。
   * @return 包含 {"finalScore": double, "selected": boolean, "reasons": List&lt;String&gt;} 的结构化快照。
   */
  Map<String, Object> aggregate(List<Map<String, Object>> scoreSnapshots);
}
