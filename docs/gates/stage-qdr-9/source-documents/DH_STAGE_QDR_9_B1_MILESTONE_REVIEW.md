# DH Stage-QDR-9 B1 Milestone Review

## Review decision

```text
TASK: DH-STAGE-QDR-9-B1-MILESTONE-REVIEW-BLOCKER-FIX
INITIAL REVIEW: BLOCKED / TASK_SCOPE_DESIGN_INVALID
SCOPE REPAIR: DONE / 2 EXACT LEGACY TESTS ADDED
EFFECTIVE CORRECTED SCOPE: PASS / 9 OF 9
TECHNICAL IMPLEMENTATION COMMIT: 80b21f31bdb5dc7a15f327aa9617b0640fd1f22b
TECHNICAL FILES MODIFIED DURING BLOCKER-FIX TASK: 0
FINAL VERDICT: PASS / B1 ACCEPTED
```

## 审计轨迹

首次 review 的 blocker 为 `STAGE_QDR_9_B1_REVIEW_SCOPE_VIOLATION`：已提交 B1 实际修改了两个 V12/V13 legacy migration compatibility tests，但原 published `WRITE_ALLOWLIST` 未列出它们，因此触发 `TASK_SCOPE_DESIGN_INVALID`。首次 review 的技术 P0 findings 为 `0`。

scope repair 没有篡改或 amend `80b21f31…`。它保留原 `8 / 8 PASS` 审计记录，并以 [B1 Scope Erratum](DH_STAGE_QDR_9_B1_SCOPE_ERRATUM.md) 透明追加两个 exact tests：默认 Flyway target 为 `14`。有效 scope 为 `9 / 9 PASS`，不扩大到任何其他 legacy test、production callback、V1–V15 migration、Repository、read model、retention 或 API。

## Code reality inspected

```text
IMPLEMENTATION RANGE: 735fe9c730c97c0e1a4a137c90d49883408191dc..80b21f31bdb5dc7a15f327aa9617b0640fd1f22b
CHANGED FILES: 27
TERMINAL FACTSOURCES: 12
ORIGINAL B1 TECHNICAL SUBSET: 13
LEGACY MIGRATION TEST FIXES: 2
V12 CHANGE: null target -> explicit target 14 only
V13 CHANGE: null target -> explicit target 14 only
PRODUCTION CALLBACK: unchanged
V1–V14 MIGRATIONS: unchanged
V15: added and validated as the sole new migration
UNEXPECTED FILES: 0
```

域持久化 contracts 提供 tenant/environment-bound records、错误码与 ports；V15 定义四张 feedback aggregate 表、tenant/environment leading unique/index、复合 FK、范围与 enum constraints。B1 architecture tests 保持 JDBC aggregate write、historical read model、retention、Controller/API、external HTTP、Provider、NQ、Agent/LangGraph、automatic learning、Paper/LIVE 为未实现或禁止状态。

## Revalidation evidence

```text
legacy targeted:
  PASS / 28 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
  V12 final target: 14
  V13 final target: 14
  PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10

B1 targeted:
  PASS / 51 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
  FeedbackPersistenceRecordsTest
  V15Qdr9FeedbackPersistenceFlywayPostgresTest
  V15Qdr9FeedbackPersistenceMigrationPresenceTest
  StageQdr9FeedbackArchitectureTest
  ArchitectureTest

module regression:
  dh-domain -am: PASS / 3 OF 3 REACTOR / 161 TESTS / 0 / 0 / 0
  dh-usecase -am: PASS / 9 OF 9 REACTOR / 590 TESTS / 0 / 0 / 0
  dh-infra,dh-app -am: PASS / 15 OF 15 REACTOR / 206 TESTS / 0 / 0 / 0

full regression:
  PASS / 19 OF 19 REACTOR / 1208 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
  PostgreSQL/Testcontainers: REAL EXECUTION / POSTGRESQL 17.10 / ZERO MANDATORY SKIPS
  ArchitectureTest: PASS

quality:
  PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
```

## Findings

### P0

无。

### P1

已解决：初始 `STAGE_QDR_9_B1_REVIEW_SCOPE_VIOLATION` 通过精确 scope erratum 修复；未修改技术文件。

### P2

无新增 finding。Maven/Mockito 的 JDK 动态 agent 兼容性警告来自测试工具链，不影响本轮 0 failures/errors/skipped 的验收结果，也未在本任务范围内改动。

## Current-fact and boundary decision

12 个 terminal factsources 已对齐为一个 authority block、`0 CONFLICTS`。remote exact-SHA CI 尚未执行，不得描述为 remote PASS。B1 仅 `CLOSED / ACCEPTED LOCALLY`；B2 仍为 `NOT_ALLOWED UNTIL B1 PUBLICATION AND EXACT-SHA CI PASS`。

```text
STAGE_QDR_9_B1_SCOPE_CONTRACT_REPAIR: PASS
TASK_SCOPE_DESIGN: PASS
EFFECTIVE_SCOPE_INVARIANTS: PASS
STAGE_QDR_9_B1_MILESTONE_REVIEW: PASS
B1_COMMIT_SCOPE: PASS
LEGACY_V12_COMPATIBILITY: PASS
LEGACY_V13_COMPATIBILITY: PASS
V15_SCHEMA: PASS
POSTGRESQL_TESTCONTAINERS: PASS
FULL_REGRESSION: PASS
QUALITY_GATE: PASS
CURRENT_FACTSOURCE_CONSISTENCY: PASS
P0_FINDINGS: 0
P1_FINDINGS: 0
ALLOW_B1_COMMITS_PUBLICATION: YES / EXPLICIT PUSH AUTHORIZATION REQUIRED
ALLOW_B2_IMPLEMENTATION_NOW: NO
```

## Risks and next action

风险保留为：post-implementation scope repair 需要随两个 local commits 一并发布；未来 blocker-fix 必须先将精确 test 文件列入 allowlist；B2 transaction/idempotency implementation 和 B2 capacity gate 仍未完成，production capacity 仍未证明。

下一精确动作：`DH-STAGE-QDR-9-B1-COMMITS-PUBLICATION-AND-EXACT-SHA-CI`。该后续任务需要单独的 push authorization；本任务没有 push、tag 或 B2 implementation 授权。
