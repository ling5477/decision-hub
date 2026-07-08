# Decision Hub Status

## 1. 当前状态表

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
stage-qdr-4 implementation: NOT_STARTED / NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
current workspace: F:/project/decision-hub
next action: DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
```

## 2. 当前事实源集合

`CURRENT_FACTSOURCE_CAN_BLOCK_CLOSE`：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/DH_STAGE_QDR_4_PLAN.md
```

`SUPPORTING_DOCS_NOT_BLOCKERS_BY_DEFAULT`：

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

`ARCHIVED_DOCS_NOT_BLOCKERS`：

```text
docs/gates/**
docs/archive/** 仅当历史遗留目录存在时使用；QDR 当前归档标准不是 docs/archive
```

完整 blocker 升级规则见 `docs/current/FACTSOURCE_POLICY.md`。

## 3. 当前治理结论

```text
DH-DOCS-GOVERNANCE-ARCHIVE-STAGE-QDR-3-PRE-CLOSE: DONE
DH-DOCS-GOVERNANCE-GATES-ARCHIVE-FIX: DONE / VALIDATED
current factsources: CONSOLIDATED
archive policy: DOCS_GATES
historical QDR docs: ARCHIVED_OR_INDEXED
docs/archive: NOT_CURRENT_ARCHIVE_STANDARD
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 implementation: NOT_STARTED / NO
B5 close review retry: CLOSED / ACCEPTED
stage-qdr-4 recommended direction: QDR Replay / Evaluation / Regression Baseline
```

B5 close review 的 ACCEPTED 结论已由用户提供并写回 current factsources。`DH-STAGE-QDR-4-PLAN` 已完成，下一步只允许进入 `DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER`；stage-qdr-4 implementation、real HTTP、real provider、Provider SDK、Agent / LangGraph runtime 与 LIVE 仍未启动。

## 4. 禁止项

```text
ALLOW_STAGE_QDR_3_FINAL_CLOSE: YES / CONSUMED
ALLOW_STAGE_QDR_4_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_4_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_4_IMPLEMENTATION: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
ALLOW_NQ_MUTATION: NO
```

## 5. 历史文档处理

已完成阶段的详细 work order、阶段中间产物、旧 review / freeze 记录、blocker fix 过程文档和 pre-close 快照均按历史记录处理。QDR 阶段归档目录统一为 `docs/gates/**`。归档文档不得作为当前状态 blocker，除非出现 `FACTSOURCE_POLICY.md` 定义的硬错误。
