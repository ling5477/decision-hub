# Decision Hub Factsource Policy

## 1. 目的

本文件定义Decision Hub current factsource权威层级、blocker规则和历史文档边界。目标是防止旧阶段文档、过期work order、历史review记录和阶段中间产物继续覆盖当前状态。

## 2. CURRENT_FACTSOURCE_CAN_BLOCK_CLOSE

当前状态与下一任务的主权威只有：

```text
docs/current/STATUS.md
docs/current/WORK_ORDER.md
```

以下入口和执行指导必须与主权威一致，但不得覆盖主权威：

```text
README.md
CLAUDE.md
AGENTS.md
docs/current/README.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/TESTING.md
```

若入口或执行指导与主权威冲突，review/close可以阻断；修复时以`STATUS.md`和`WORK_ORDER.md`为准。

权威层级固定为：

```text
Primary current-state authority:
docs/current/STATUS.md
docs/current/WORK_ORDER.md

Policy authority:
docs/current/FACTSOURCE_POLICY.md

Execution guidance:
AGENTS.md
CLAUDE.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md

Entry/index documents:
README.md
docs/current/README.md
```

## 2.1 TASK_SCOPE_DESIGN_VALIDATION

所有任务在实施前必须满足：

```text
VALIDATION_SCOPE ⊆ READ_SCOPE
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST
```

任一包含关系不成立时，不得开始实施，必须输出`TASK_SCOPE_DESIGN_INVALID`。

## 2.2 REVIEW_CLOSEOUT_RULE

1. 技术验收已通过且唯一阻断为current docs漂移时，必须在同一任务内修复文档并完成最终验收。
2. 禁止创建`docs fix -> review -> docs fix -> review`循环。
3. `STATUS.md`和`WORK_ORDER.md`冲突可以阻断阶段close。
4. 其他入口文档漂移必须修复，但不得自动降级为migration、事务或安全实现失败。
5. 只有新的真实P0/P1代码、安全、tenant、事务、migration或API问题，才能阻断技术acceptance。
6. 要求`current conflict count = 0`的文件必须全部位于当前任务`WRITE_ALLOWLIST`。

## 3. SUPPORTING_DOCS_NOT_BLOCKERS_BY_DEFAULT

以下文件默认不是 primary stage gate source，也不是 close review blocker：

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/ARCHIVE_INDEX.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

这些文件可以提供背景、验证证据、变更记录、API / DB 摘要或历史复盘，但不得覆盖 `STATUS.md` 与 `WORK_ORDER.md` 的当前结论。其内部旧`next action`、旧`current task`或旧阶段状态一律按historical/consumed解释，除非主权威在当前块中显式引用。

## 3.1 ARCHIVED_DOCS_NOT_BLOCKERS

以下目录是 historical records，默认不作为 close review blocker：

```text
docs/gates/**
docs/archive/** 仅当历史遗留目录存在时使用；QDR 当前归档标准不是 docs/archive
```

`docs/gates/**` 是当前 QDR 阶段归档目录。归档文件保留历史内容和复盘价值，但不得覆盖 `STATUS.md`、`WORK_ORDER.md`、`CODEX_PROJECT_INSTRUCTIONS.md` 或 `TESTING.md` 的当前结论。

## 4. Supporting Docs 升级为 Blocker 的硬错误

只有出现以下硬错误时，supporting docs 才能升级为 blocker：

```text
real provider started
real HTTP started
Provider SDK introduced
LangGraph started
Agent runtime started
LIVE enabled
已关闭或历史阶段被重新写成当前implementation
stage-qdr-3 acceptance/final close 与 current factsources 冲突
gateway result can trade
raw prompt 或 raw provider response 被写成可保存
credential storage allowed
NQ mutation allowed
```

## 5. 当前状态

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED / ACCEPTED
Schema errata implementation: ACCEPTED
Persistent guards implementation: ACCEPTED
Guard hard-ceiling contract: CLOSED / ACCEPTED
Guard configuration bypass: CLOSED
Normative hard ceilings: rate window <= 3600s / rate quota <= 100000 / idempotency lease <= 900s
Capacity acceptance criteria: BLOCKED / NOT_FROZEN
Capacity harness: NOT_IMPLEMENTED / BLOCKED_BY_CRITERIA
Post-B2 capacity acceptance: BLOCKED
Capacity calibration path blocker: CLOSED
Repeatable protected 2xx: PASS
PromptVersion atomic bootstrap: CLOSED / ACCEPTED
Cleanup tenant-scoped contract: CLOSED / ACCEPTED
Rate matrix: PASS / 15 OF 15 MEASURED ROUNDS
Quota atomicity: PASS / COLD_START 3 OF 3
Cleanup protected-row safety: PASS / TENANT_SCOPED
Cleanup capacity evidence: PASS / 10 + 100 + 1000
PostgreSQL same-pool recovery: CLOSED / ACCEPTED / 3 OF 3
PostgreSQL contention evidence: PASS / SAME_POOL_RECOVERY_AND_SERIES_COMPLETE
Restart reproducibility: PASS / SPRING_CONTEXT 3 OF 3 / POSTGRESQL_SAME_CONTAINER 3 OF 3
Capacity threshold evidence: CLOSED / SUFFICIENT
Candidate threshold evidence: SUFFICIENT
Allow capacity criteria freeze retry: YES / NEXT_TASK_ONLY
Full regression: PASS / 1114 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Full regression resource baseline: PASS / 380 SUREFIRE ROWS / 7 PIDS
Quality gate: PASS
Stage-QDR-7 B3: NOT_ALLOWED
current task: DH-STAGE-QDR-7-B2-POSTGRESQL-SAME-POOL-RECOVERY-BLOCKER
current task status: CLOSED / ACCEPTED
next task: DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY
real HTTP: NO
real provider: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

## 5.1 Previous attempt / Superseded current state / Consumed evidence

以下状态只记录上一轮任务当时的真实结果，属于`Historical / Previous attempt / Superseded current state / Consumed evidence`，不得覆盖第5节active current state：

```text
previous current task: DH-STAGE-QDR-7-B2-CAPACITY-CALIBRATION-PATH-BLOCKER
previous next task: DH-STAGE-QDR-7-B2-CAPACITY-THRESHOLD-EVIDENCE-RETRY-2
previous full regression: PASS / 1101 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
previous same-pool attempt full regression: PASS / 1110 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
previous same-pool result: BLOCKED / RECOVERY_PROBE_INVALID
```

## 6. Archive Rule

`docs/gates/**` 是当前 QDR 阶段归档目录。`docs/archive/**` 不再作为本项目 QDR 阶段的新归档标准；若历史遗留目录存在，只能作为 historical reference。

归档文件可以说明过去某一轮任务当时的状态，但不能作为当前事实源。若归档文档与 current factsources 冲突，以 `STATUS.md` 与 `WORK_ORDER.md` 为准。

## 7. Current Alignment Rule

`DH-STAGE-QDR-7-B2-FACTSOURCE-ALIGNMENT-AND-FINAL-ACCEPTANCE`已在不修改callback、Java、测试、V1–V14、API、contracts、golden_cases或NQ的前提下完成8-file对齐，并保留初始`BLOCKED / CURRENT_FACTSOURCE_SCOPE_CONFLICT`审计记录；其旧capacity acceptance路线已被后续任务消费，只作为historical record。

B2已`CLOSED / ACCEPTED`。Calibration path blocker、PromptVersion atomic bootstrap、cold-start quota、tenant-scoped cleanup、same-pool recovery、两类restart与Surefire资源采样缺口均已关闭，candidate threshold evidence为`SUFFICIENT`。当前任务仍登记为`DH-STAGE-QDR-7-B2-POSTGRESQL-SAME-POOL-RECOVERY-BLOCKER / CLOSED / ACCEPTED`，下一任务只允许`DH-STAGE-QDR-7-B2-CAPACITY-CRITERIA-FREEZE-RETRY`。Criteria尚未冻结，Post-B2 capacity acceptance继续`BLOCKED`，B3继续`NOT_ALLOWED`；不得推导正式harness、capacity acceptance、API、外部HTTP/provider、NQ、Agent/LangGraph或LIVE授权。
