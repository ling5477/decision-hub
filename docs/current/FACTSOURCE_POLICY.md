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
B2 schema errata implementation: DONE
B2 schema errata review: TECHNICAL_PASS / FACTSOURCE_FIX_PENDING
B2 milestone acceptance: NOT_YET
capacity acceptance: NOT_ALLOWED
B3: NOT_ALLOWED
current task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY
next task: DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY-2
real HTTP: NO
real provider: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

## 6. Archive Rule

`docs/gates/**` 是当前 QDR 阶段归档目录。`docs/archive/**` 不再作为本项目 QDR 阶段的新归档标准；若历史遗留目录存在，只能作为 historical reference。

归档文件可以说明过去某一轮任务当时的状态，但不能作为当前事实源。若归档文档与 current factsources 冲突，以 `STATUS.md` 与 `WORK_ORDER.md` 为准。

## 7. Current Alignment Rule

当前`DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-BLOCKER-FIX-RETRY`只修复仓库入口、authority hierarchy和current factsources。它不修改callback、Java、测试、migration、API、contracts、golden_cases或NQ。

factsources对齐并通过验证后，只允许进入独立`DH-STAGE-QDR-7-B2-SCHEMA-ERRATA-IMPLEMENTATION-REVIEW-RETRY-2`。该review通过前不得写B2 `ACCEPTED`，不得进入milestone retry-3、capacity acceptance或B3。
