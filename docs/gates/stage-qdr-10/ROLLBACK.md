# Rollback

## Before tag

- 文档/归档问题：停止 publication，修复后创建新的普通 commit；不 amend 已发布 implementation。
- close exact-SHA CI 失败：保持 tag absent，进入 `DH-STAGE-QDR-10-PUBLICATION-BLOCKER`。
- archive/hash 失败：停止 close，进入 `DH-STAGE-QDR-10-ARCHIVE-BLOCKER`。
- security tree mismatch：停止全部 publication，进入 `DH-STAGE-QDR-10-SECURITY-EVIDENCE-INVALIDATION-BLOCKER`。

## After tag

Annotated tag `dh-stage-qdr-10-close` 禁止移动、删除、覆盖或重建。任何后续修正只能用新的普通 commit，并保留 tag 指向的 close snapshot。

本 stage 没有 API、migration 或 schema 变化，无数据库 rollback。production capacity 未证明，不存在 production rollout rollback。
