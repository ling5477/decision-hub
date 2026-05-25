package com.guidinglight.decisionhub.memory.agent;

import com.guidinglight.decisionhub.domain.experience.ExperienceEntry;
import java.util.List;
import java.util.Optional;

/**
 * Stage1：经验沉淀的读写端口。
 *
 * <p>对应工单 4.3：ExperienceStore。第一阶段做轻量分数读写，不做重型蚁群优化。
 */
public interface ExperienceStore {

  /** 按业务复合 key 查找。 */
  Optional<ExperienceEntry> findByKey(String tenantId, String experienceKey);

  /** Upsert：不存在则新增，存在则替换为提供的对象。 */
  void save(ExperienceEntry entry);

  /** 按 strategyPattern 检索 top N，按 score 倒序。 */
  List<ExperienceEntry> topByPattern(String tenantId, String strategyPattern, int limit);

  /** 列出全部（Stage1 调试用，禁止用于生产排序）。 */
  List<ExperienceEntry> listAll(String tenantId);
}
