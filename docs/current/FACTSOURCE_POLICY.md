# Decision Hub Factsource Policy

## 1. 目的

本文件定义 `DH-STAGE-QDR-3-B5-CLOSE-REVIEW` 前的 current factsource 与 blocker 规则。目标是防止旧阶段文档、过期 work order、历史 review 记录和阶段中间产物继续覆盖当前状态。

## 2. CURRENT_FACTSOURCE_CAN_BLOCK_CLOSE

以下文件可以作为 close review blocker source：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
```

若这些文件与当前状态冲突，B5 close review 可以阻断。

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
stage-qdr-4 started
stage-qdr-3 accepted/final closed 的错误提前声明
gateway result can trade
raw prompt/raw provider response allowed
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
stage-qdr-3 B5: READY FOR RETRY
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
current workspace: F:/Project/decision-hub
next action: DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```

## 6. Archive Rule

`docs/gates/**` 是当前 QDR 阶段归档目录。`docs/archive/**` 不再作为本项目 QDR 阶段的新归档标准；若历史遗留目录存在，只能作为 historical reference。

归档文件可以说明过去某一轮任务当时的状态，但不能作为当前事实源。若归档文档与 current factsources 冲突，以 `STATUS.md` 与 `WORK_ORDER.md` 为准。
