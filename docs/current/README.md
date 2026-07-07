# Decision Hub Current Docs

本目录是 Decision Hub 当前事实源入口。当前阶段的 close review 不应再从旧 work order、旧 freeze 记录、旧 blocker fix 过程文档或归档文档推导当前状态。

## 当前状态

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
current workspace: E:/Project/decision-hub
next action: DH-STAGE-QDR-3-B5-CLOSE-REVIEW
```

## Close Review 可阻断事实源

这些文件可以作为 `DH-STAGE-QDR-3-B5-CLOSE-REVIEW` 的 blocker source：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
```

若以上文件与当前状态冲突，B5 close review 可以阻断。

## Supporting Docs

以下文件默认不是 primary stage gate source，也不是 close review blocker。只有当它们出现硬错误时，才可升级为 blocker，规则见 `FACTSOURCE_POLICY.md`。

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
docs/current/DH_STAGE_QDR_*.md
docs/archive/**
```

## 当前入口文件

```text
STATUS.md                  当前状态表
WORK_ORDER.md              下一步唯一入口
CODEX_PROJECT_INSTRUCTIONS.md 当前执行纪律
TESTING.md                 当前验证证据与工具风险
FACTSOURCE_POLICY.md       事实源与 blocker 规则
ARCHIVE_INDEX.md           已归档文档索引
WORKLOG.md                 本轮文档治理记录，supporting only
ROADMAP.md                 路线摘要，supporting only
API.md                     API 实现状态摘要，supporting only
DB_SCHEMA.md               DB schema 状态摘要，supporting only
```

## Archive Pointers

```text
docs/archive/stage-qdr-2/
docs/archive/stage-qdr-3/
docs/archive/stage-qdr-3/pre-close-current-snapshot-20260707/
```

Archive docs are historical records. They are not current factsources and must not override `STATUS.md` or `WORK_ORDER.md`.
