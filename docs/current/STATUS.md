# Decision Hub Status

## 1. 当前状态表

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

## 2. 当前事实源集合

`CURRENT_FACTSOURCE_CAN_BLOCK_CLOSE`：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
```

`SUPPORTING_DOCS_NOT_BLOCKERS_BY_DEFAULT`：

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
docs/current/DH_STAGE_QDR_*.md
docs/archive/**
```

完整 blocker 升级规则见 `docs/current/FACTSOURCE_POLICY.md`。

## 3. 当前治理结论

```text
DH-DOCS-GOVERNANCE-ARCHIVE-STAGE-QDR-3-PRE-CLOSE: DONE
current factsources: CONSOLIDATED
archive policy: DEFINED
historical QDR docs: ARCHIVED_OR_INDEXED
stage-qdr-3 acceptance: NOT_ACCEPTED_YET
stage-qdr-3 final close: NOT_CLOSED
stage-qdr-4: NOT_STARTED
B5 close review retry: ALLOWED_AFTER_VALIDATION
```

B5 close review 可以重新执行，但本文件不把 B5 写成 accepted，不把 stage-qdr-3 final close 写成 closed，也不启动 stage-qdr-4。

## 4. 禁止项

```text
ALLOW_STAGE_QDR_3_FINAL_CLOSE: NO
ALLOW_STAGE_QDR_4_PLAN: NO
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

已完成阶段的详细 work order、阶段中间产物、旧 review / freeze 记录、blocker fix 过程文档和 pre-close 快照均按历史记录处理。归档文档不得作为当前状态 blocker，除非出现 `FACTSOURCE_POLICY.md` 定义的硬错误。
