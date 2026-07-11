package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import java.util.Optional;

/**
 * Canonical replay snapshot 的 tenant-bound append-only persistence port。
 *
 * <p>所有入口都显式接收 {@code tenantId}，避免调用方从 record、UUID 或当前上下文推断租户。 该 port 不提供
 * update、delete、latest、list-all、fallback 或 tenantless 查询。
 */
public interface CanonicalReplaySnapshotPersistencePort {

  /**
   * 插入 immutable snapshot；相同 identity 的完全一致内容可幂等返回既有记录。
   *
   * @param tenantId 第一安全边界，必须与 record identity 完全一致。
   * @param record 已由上游构造并校验的 structured safe snapshot。
   * @return 新插入或幂等命中的既有 record。
   * @throws CanonicalReplaySnapshotPersistenceException 数据源失败或 identity/content 冲突时抛出。
   */
  CanonicalReplaySnapshotRecord insert(String tenantId, CanonicalReplaySnapshotRecord record);

  /**
   * 按 tenant + snapshot business ID 精确读取。
   *
   * @param tenantId 第一安全边界。
   * @param snapshotId tenant 内 snapshot business ID。
   * @return 精确命中的 record；跨 tenant 返回 empty。
   * @throws CanonicalReplaySnapshotPersistenceException 数据源失败或返回行 identity 不一致时抛出。
   */
  Optional<CanonicalReplaySnapshotRecord> findByTenantAndSnapshotId(
      String tenantId, String snapshotId);

  /**
   * 按完整 tenant-bound identity 与 snapshot schema version 精确读取。
   *
   * @param tenantId 第一安全边界。
   * @param identity V5/V6/V8/V9 physical/business identity 集合。
   * @param snapshotSchemaVersion frozen snapshot schema version。
   * @return 全部 identity 均精确匹配的 record；无匹配返回 empty。
   * @throws CanonicalReplaySnapshotPersistenceException 数据源失败或返回行 identity 不一致时抛出。
   */
  Optional<CanonicalReplaySnapshotRecord> findByTenantAndIdentity(
      String tenantId, CanonicalReplaySnapshotIdentity identity, String snapshotSchemaVersion);
}
