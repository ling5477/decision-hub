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
stage-qdr-3 close review: YES / B5 ACCEPTED
stage-qdr-3 acceptance: ACCEPTED
stage-qdr-3 final close: CLOSED / ACCEPTED
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 B2: CLOSED / ACCEPTED
stage-qdr-4 B3: CLOSED / ACCEPTED
stage-qdr-4 B4: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG: PENDING
ALLOW_STAGE_QDR_4_TAG_AFTER_ARCHIVE_COMMIT: YES
ALLOW_STAGE_QDR_4_TAG_CLOSE: YES
ALLOW_STAGE_QDR_5_PLAN: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
current workspace: E:/Project/decision-hub
next action: DH-STAGE-QDR-4-TAG-CLOSE
```

## Stage-QDR-4 归档状态

```text
Stage-QDR-4 Replay / Evaluation / Regression Baseline: CLOSED / ACCEPTED / ARCHIVED
B1 Replay / Evaluation Domain Contracts: DONE
B2 Replay / Evaluation Persistence Baseline: CLOSED / ACCEPTED
B3 Mock Gateway Regression Integration: CLOSED / ACCEPTED
B4 Regression Report / Read Model Support: DONE
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4_TAG: PENDING
next tag: dh-stage-qdr-4-close
```

Stage-QDR-4 归档不授权 real HTTP、real provider、Provider SDK、Agent runtime、LangGraph runtime、LIVE、NQ mutation 或 trading execution。下一步仅允许 `DH-STAGE-QDR-4-TAG-CLOSE`；Stage-QDR-5 只能在 tag close 后 planning-first。

## Stage-qdr-4 当前事实源

这些文件可以作为 `DH-STAGE-QDR-4-ARCHIVE-CLOSE` 与后续 tag prep 的 current factsource：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/DH_STAGE_QDR_4_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
```

若以上文件与当前状态冲突，必须先修正 factsources，不得进入 tag 或 Stage-QDR-5 planning。

## Supporting Docs

以下文件默认不是 primary stage gate source，也不是 close review blocker。只有当它们出现硬错误时，才可升级为 blocker，规则见 `FACTSOURCE_POLICY.md`。

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

## 当前入口文件

```text
STATUS.md                  当前状态表
WORK_ORDER.md              下一步唯一入口
CODEX_PROJECT_INSTRUCTIONS.md 当前执行纪律
TESTING.md                 当前验证证据与工具风险
DH_STAGE_QDR_4_PLAN.md     stage-qdr-4 planning 结论
DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md B4 read model plan
DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md B4 implementation WO
FACTSOURCE_POLICY.md       事实源与 blocker 规则
ARCHIVE_INDEX.md           已归档文档索引
docs/gates/stage-qdr-4/README.md Stage-QDR-4 归档入口
WORKLOG.md                 本轮文档治理记录，supporting only
ROADMAP.md                 路线摘要，supporting only
API.md                     API 实现状态摘要，supporting only
DB_SCHEMA.md               DB schema 状态摘要，supporting only
```

## Archive Pointers

```text
docs/gates/stage-qdr-2/
docs/gates/stage-qdr-3/
docs/gates/stage-qdr-3/pre-close-current-snapshot-20260707/
docs/gates/stage-qdr-3/current-docs-historical-20260708/
docs/gates/stage-qdr-4/
```

Archive docs are historical records. They are not current factsources and must not override `STATUS.md` or `WORK_ORDER.md`. `docs/archive/**` is not the current QDR archive standard.
