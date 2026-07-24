# DH Stage-QDR-9 B1 Scope Erratum

## 状态

```text
TASK: DH-STAGE-QDR-9-B1-MILESTONE-REVIEW-BLOCKER-FIX
CLASSIFICATION: DOCUMENTATION + SCOPE_CONTRACT_ERRATUM + CURRENT_FACTSOURCE_ALIGNMENT
TECHNICAL IMPLEMENTATION COMMIT: 80b21f31bdb5dc7a15f327aa9617b0640fd1f22b
SCOPE ERRATUM: DONE
EFFECTIVE SCOPE INVARIANTS: PASS / 9 OF 9
```

## 审计轨迹与根因

原始冻结工单为 [DH Stage-QDR-9 Implementation Work Order](DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md)。其原始 scope invariants 为 `8 / 8 PASS`，该记录保留为：

```text
ORIGINAL / SUPERSEDED FOR B1 FINAL ACCEPTANCE
```

首次 B1 milestone review 不覆盖或删除该记录，结论固定为：

```text
INITIAL REVIEW: BLOCKED
INITIAL BLOCKER: STAGE_QDR_9_B1_REVIEW_SCOPE_VIOLATION
TASK_SCOPE_DESIGN_INVALID
INITIAL TECHNICAL P0 FINDINGS: 0
```

根因是原始 published `WRITE_ALLOWLIST` 未包含以下两个实际被 `80b21f31…` 修改的 legacy migration compatibility tests：

```text
dh-app/src/test/java/com/guidinglight/decisionhub/V12PersistentRuntimeGuardsFlywayPostgresTest.java
dh-app/src/test/java/com/guidinglight/decisionhub/V13TransactionalCompatibilityCallbackFlywayPostgresTest.java
```

当 target 为 `null` 时，这两个历史测试会迁移到最新版本；V15 加入后，原本只验证 V14 historical state 的断言失效。提交中的修复仅将默认 target 显式固定为 `14`。

本 erratum 是透明的 post-implementation governance repair；不得改写为“原工单从一开始就包含这两个文件”。

## 批准的精确 scope extension

```text
APPROVED FILES: the two exact V12/V13 test files above
APPROVED MODIFICATION SEMANTICS: explicit historical migration target = 14 only
ORIGINAL B1 TECHNICAL SUBSET: 13 files
LEGACY MIGRATION TEST FIX SCOPE: 2 files
EFFECTIVE B1 TECHNICAL SUBSET: 15 files
```

明确禁止：

```text
assertion weakening or deletion
expected version changed to 15
test disablement
production callback changes
V1–V15 migration changes
additional legacy test modifications
```

本 blocker-fix task 的文档 `WRITE_ALLOWLIST` 仅为下列 15 个批准文档：

```text
README.md
AGENTS.md
CLAUDE.md
docs/current/DH_STAGE_QDR_9_IMPLEMENTATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_9_B1_SCOPE_ERRATUM.md
docs/current/DH_STAGE_QDR_9_B1_MILESTONE_REVIEW.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/ARCHIVE_INDEX.md
```

技术 `WRITE_ALLOWLIST` 则为原始 B1 13-file subset 加上批准的两个 exact legacy tests；本轮没有修改其中任何技术文件。

## Effective scope invariants

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
MIGRATION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
REPOSITORY_SCOPE ⊆ WRITE_ALLOWLIST: PASS
READ_MODEL_SCOPE ⊆ WRITE_ALLOWLIST: PASS
RETENTION_SCOPE ⊆ WRITE_ALLOWLIST: PASS
ARCHITECTURE_GUARD_SCOPE ⊆ VALIDATION_SCOPE: PASS
LEGACY_MIGRATION_TEST_FIX_SCOPE ⊆ WRITE_ALLOWLIST: PASS

TOTAL: 9 OF 9 PASS
```

`LEGACY_MIGRATION_TEST_FIX_SCOPE` 仅含上述两份 test。任何新的 technical blocker、API、migration、transaction、tenant 或 security finding 都必须转为新任务，不得用此 erratum 扩大范围。

## 复核结论

`735fe9c730c97c0e1a4a137c90d49883408191dc..80b21f31bdb5dc7a15f327aa9617b0640fd1f22b` 的 changed files 为 27：12 个 terminal factsources、原始 B1 technical subset 13 个文件及本 erratum 覆盖的 2 个 legacy tests。两个 legacy diff 均只新增 `HISTORICAL_MIGRATION_TARGET = "14"` 与 null-target fallback；未见 expected version 迁移至 V15、断言删除、test disablement、production callback 或 V1–V15 migration 修改。

最终 revalidation 与 milestone review 结果见 [B1 Milestone Review](DH_STAGE_QDR_9_B1_MILESTONE_REVIEW.md)。
