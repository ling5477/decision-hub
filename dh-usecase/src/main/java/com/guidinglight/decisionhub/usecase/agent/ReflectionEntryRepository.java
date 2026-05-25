package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.reflection.ReflectionEntry;
import java.util.List;

/**
 * Stage2-PoC-B4：ReflectionEntry 仓储端口。
 *
 * <p>Batch 4 仅落 InMemory 实现；JDBC 实现留待 Batch 5。
 */
public interface ReflectionEntryRepository {

  /** 持久化一条 reflection。 */
  void save(ReflectionEntry entry);

  /** 按 runId 查询，按 stepIndex 升序返回。 */
  List<ReflectionEntry> listByRun(String runId);
}
