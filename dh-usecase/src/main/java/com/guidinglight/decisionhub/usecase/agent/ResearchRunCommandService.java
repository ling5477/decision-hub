package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.research.ResearchRun;
import java.util.Map;

/**
 * Stage1：ResearchRun 命令端用例。
 *
 * <p>对应工单 4.2：ResearchRunCommandService。
 */
public interface ResearchRunCommandService {

  /**
   * 创建一个新的 ResearchRun（CREATED 状态）。
   *
   * @param tenantId 租户。
   * @param topic 研究课题（自然语言或结构化主题 key）。
   * @param payloadJson 入参快照。
   * @return 新 run。
   */
  ResearchRun create(String tenantId, String topic, Map<String, Object> payloadJson);

  /**
   * 启动一个已存在的 ResearchRun。
   *
   * <p>Stage1：在同一调用内串行执行 planning -> exploring -> reviewing -> judging。
   *
   * @param runId run id。
   * @return 完成后的 run（典型为 COMPLETED 或 FAILED）。
   */
  ResearchRun start(String runId);
}
