package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.research.ResearchRun;
import java.util.List;
import java.util.Optional;

/**
 * Stage1：ResearchRun 持久化端口。
 *
 * <p>对应工单 4.7：Repository 接口预留位置。Stage1 由 dh-memory/dh-app 提供内存实现。
 */
public interface ResearchRunRepository {

  /** Upsert。 */
  void save(ResearchRun run);

  /** 查找。 */
  Optional<ResearchRun> find(String runId);

  /** 列出某租户的全部 run（按 createdAt 倒序）。 */
  List<ResearchRun> listByTenant(String tenantId);
}
