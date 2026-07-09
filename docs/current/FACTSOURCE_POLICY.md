# Decision Hub Factsource Policy

## 1. 目的

本文件定义 Stage-QDR-4 归档后的 current factsource、tag close 前的 blocker 规则和归档文档边界。目标是防止旧阶段文档、过期 work order、历史 review 记录和阶段中间产物继续覆盖当前状态。

## 2. CURRENT_FACTSOURCE_CAN_BLOCK_CLOSE

以下文件可以作为 close review blocker source：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/ARCHIVE_INDEX.md
```

若这些文件与当前状态冲突，tag close 可以阻断。

## 3. SUPPORTING_DOCS_NOT_BLOCKERS_BY_DEFAULT

以下文件默认不是 primary stage gate source，也不是 close review blocker：

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

这些文件可以提供背景、验证证据、变更记录、API / DB 摘要或历史复盘，但不得覆盖 `STATUS.md` 与 `WORK_ORDER.md` 的当前结论。

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
stage-qdr-4 implementation 被写成已启动
stage-qdr-3 acceptance/final close 与 current factsources 冲突
gateway result can trade
raw prompt 或 raw provider response 被写成可保存
credential storage allowed
NQ mutation allowed
```

## 5. 当前状态

```text
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 implementation: DONE
stage-qdr-3 B1: DONE / COMMITTED
stage-qdr-3 B2: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B3: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 B4: DONE / FREEZE ACCEPTED / COMMITTED
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4: CLOSED / ACCEPTED / ARCHIVED
stage-qdr-4 tag: PENDING
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
current workspace: use Get-Location per run
current task: DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION
next action after cleanup: DH-STAGE-QDR-4-TAG-CLOSE
```

## 6. Archive Rule

`docs/gates/**` 是当前 QDR 阶段归档目录。`docs/archive/**` 不再作为本项目 QDR 阶段的新归档标准；若历史遗留目录存在，只能作为 historical reference。

归档文件可以说明过去某一轮任务当时的状态，但不能作为当前事实源。若归档文档与 current factsources 冲突，以 `STATUS.md` 与 `WORK_ORDER.md` 为准。

## 7. Documentation Discipline Cleanup Rule

当前 `DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION` 只修复文档纪律、workflow authority、skill policy、archive policy 和 current factsource。它不创建 tag，不进入 Stage-QDR-5，不修改 Java、测试、migration、API、contracts、golden_cases 或 NQ。

cleanup 完成且工作区 clean 后，才允许另起独立 `DH-STAGE-QDR-4-TAG-CLOSE`。Stage-QDR-5 只能在 tag close 后 planning-first，不得从 cleanup、archive 或 tag 任务直接进入 implementation。
