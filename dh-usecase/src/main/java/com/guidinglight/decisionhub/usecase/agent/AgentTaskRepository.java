package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.agent.AgentTask;
import java.util.Optional;

/** Stage1：AgentTask 持久化端口。 */
public interface AgentTaskRepository {

  /** Upsert。 */
  void save(AgentTask task);

  /** 按 runId 查找当前 run 关联的任务图（Stage1：每个 run 一个 AgentTask）。 */
  Optional<AgentTask> findByRun(String runId);
}
