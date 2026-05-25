package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.agent.AgentArtifact;
import java.util.List;

/** Stage1：AgentArtifact 持久化端口。 */
public interface AgentArtifactRepository {

  /** 写入。 */
  void save(AgentArtifact artifact);

  /** 按 runId 列出全部产物。 */
  List<AgentArtifact> listByRun(String runId);
}
