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
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B1: DONE / MODEL_GATEWAY_OBSERVABILITY_CONTRACTS_ONLY
STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B2_IMPLEMENTATION: DONE / INTERNAL_PROVIDER_HEALTH_READ_MODEL_IMPLEMENTED
STAGE_QDR_5_B2_CI_BLOCKER_FIX: DONE
STAGE_QDR_5_B3_IMPLEMENTATION_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B3_IMPLEMENTATION: DONE / PROVIDER_READINESS_GUARD_POLICY_EVALUATION_IMPLEMENTED
STAGE_QDR_5_B3_CLOSE_REVIEW: PASS
STAGE_QDR_5_B3: CLOSED / ACCEPTED
STAGE_QDR_5_B4: READY_FOR_PLAN_OR_WO
STAGE_QDR_5_B4_IMPLEMENTATION: NOT_STARTED
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_WO_READY / B4_IMPLEMENTATION_NOT_STARTED
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO / ALL_IN_ONE_FORBIDDEN
ALLOW_STAGE_QDR_5_B1_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B2_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B3_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_PLAN_OR_WO: YES
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: NO
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
current workspace: use Get-Location per run
current task: DH-STAGE-QDR-5-B3-PROVIDER-READINESS-GUARD-POLICY-EVALUATION-CLOSE-REVIEW
next action: DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-WO
```

## Stage-QDR-4 归档状态

```text
Stage-QDR-4 Replay / Evaluation / Regression Baseline: CLOSED / ACCEPTED / ARCHIVED
B1 Replay / Evaluation Domain Contracts: DONE
B2 Replay / Evaluation Persistence Baseline: CLOSED / ACCEPTED
B3 Mock Gateway Regression Integration: CLOSED / ACCEPTED
B4 Regression Report / Read Model Support: DONE
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
```

Stage-QDR-4 归档和 tag close 不授权 real HTTP、real provider、Provider SDK、Agent runtime、LangGraph runtime、LIVE、NQ mutation 或 trading execution。`DH-STAGE-QDR-5-PLAN`、`DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER`、B1、B2、B3 implementation work order、B3 implementation 与 B3 close review 已完成；B3 close review 结论为 `PASS`，B3 状态为 `CLOSED / ACCEPTED`。下一步只能进入 `DH-STAGE-QDR-5-B4-OBSERVABILITY-REPORT-ACCEPTANCE-SUPPORT-WO`；B4 implementation 仍为 `NOT_STARTED`，不得 all-in-one implementation。

## Stage-qdr-4 当前事实源

这些文件可以作为 `DH-STAGE-QDR-4-ARCHIVE-CLOSE` 与后续 tag prep 的 current factsource：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/ARCHIVE_INDEX.md
docs/current/DH_STAGE_QDR_5_PLAN.md
docs/current/DH_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO.md
docs/current/DH_STAGE_QDR_5_B3_PROVIDER_READINESS_GUARD_POLICY_EVALUATION_WO.md
```

Stage-QDR-4 的详细 plan / work order / implementation work order 已归档到 `docs/gates/stage-qdr-4/`，不再保留在 `docs/current`。Stage-QDR-5 的 planning 入口是 `docs/current/DH_STAGE_QDR_5_PLAN.md`；implementation work order 入口是 `docs/current/DH_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER.md`；B2 Provider Health / Gateway Call Read Model work order 入口是 `docs/current/DH_STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO.md`；B3 Provider Readiness Guard / Policy Evaluation work order 入口是 `docs/current/DH_STAGE_QDR_5_B3_PROVIDER_READINESS_GUARD_POLICY_EVALUATION_WO.md`。

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
FACTSOURCE_POLICY.md       事实源与 blocker 规则
ARCHIVE_INDEX.md           已归档文档索引
DH_STAGE_QDR_5_PLAN.md     Stage-QDR-5 plan-only 记录
DH_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER.md Stage-QDR-5 implementation batch 边界与测试矩阵
DH_STAGE_QDR_5_B2_PROVIDER_HEALTH_GATEWAY_CALL_READ_MODEL_WO.md Stage-QDR-5 B2 internal read model implementation work order
DH_STAGE_QDR_5_B3_PROVIDER_READINESS_GUARD_POLICY_EVALUATION_WO.md Stage-QDR-5 B3 provider readiness guard / policy evaluation implementation work order
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
