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
STAGE_QDR_5_B4_OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_WO: DONE / WORK_ORDER_ONLY
STAGE_QDR_5_B4_IMPLEMENTATION: DONE / OBSERVABILITY_REPORT_ACCEPTANCE_SUPPORT_IMPLEMENTED
STAGE_QDR_5_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_5: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_5_ARCHIVE: DONE
STAGE_QDR_5_TAG: DONE / dh-stage-qdr-5-close
STAGE_QDR_6: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_6_PLAN: DONE / PLAN_ONLY
STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: DONE / WORK_ORDER_ONLY
STAGE_QDR_6_IMPLEMENTATION: COMPLETE / B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE_COMMITTED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_WORK_ORDER: YES / CONSUMED
ALLOW_STAGE_QDR_6_IMPLEMENTATION_NOW: NO
STAGE_QDR_6_FINAL_CLOSE_REVIEW: PREVIOUS_BLOCKED / HISTORICAL_PRESERVED
STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: PASS / 5961164
CURRENT_FACTSOURCE_CONFLICT: CLEARED
STAGE_QDR_6_ARCHIVE: DONE / docs/gates/stage-qdr-6/
STAGE_QDR_6_TAG: DONE / dh-stage-qdr-6-close
STAGE_QDR_6_TAG_TARGET: b9b68b3c4ea35813959ac5bf5a4566e5393e20be
STAGE_QDR_6_CURRENT_PROCESS_SOURCES: PRUNED
STAGE_QDR_6_POST_TAG_CURRENT_CLEANUP: DONE
STAGE_QDR_7_PLAN: NOT_STARTED / PLANNING_FIRST_ONLY
ALLOW_STAGE_QDR_6_FINAL_CLOSE_REVIEW_RETRY: YES / CONSUMED / PASS
ALLOW_STAGE_QDR_6_ARCHIVE_PACKET_NOW: YES / CONSUMED
ALLOW_STAGE_QDR_6_TAG_CLOSE_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_7_PLAN: YES / PLANNING_FIRST_ONLY
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO
ARCHIVE_POLICY: REPAIRED
ARCHIVE_PACKET_POLICY: REQUIRED_FOR_ALL_FUTURE_STAGES
STAGE_QDR_5_IMPLEMENTATION: B1_DONE / B2_DONE / B3_CLOSED_ACCEPTED / B4_DONE / FINAL_CLOSE_PASS
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
ALLOW_STAGE_QDR_5_B4_PLAN_OR_WO: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_5_B4_IMPLEMENTATION_NOW: NO / CONSUMED
ALLOW_STAGE_QDR_5_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_5_ARCHIVE_CLOSE: YES / CONSUMED
STAGE_QDR_5_TAG_CLOSE: DONE / dh-stage-qdr-5-close
STAGE_QDR_5_TAG_NOW: NO / ALREADY_TAGGED
ALLOW_STAGE_QDR_6_PLAN: YES / CONSUMED
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
current task: DH-STAGE-QDR-6-POST-TAG-CURRENT-CLEANUP
current task status: DONE / CURRENT_PROCESS_SOURCES_PRUNED
next action: DH-STAGE-QDR-7-PLAN
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

Stage-QDR-4、Stage-QDR-5 与 Stage-QDR-6 均已 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`。Stage-QDR-6 tag 为 `dh-stage-qdr-6-close`，指向 archive commit `b9b68b3`；11 个 current process sources 已清理且archive副本继续保留。下一步只允许 `DH-STAGE-QDR-7-PLAN` planning-first，不授权 implementation/runtime。

## 当前事实源

Stage-QDR-5 已完成 archive close、tag close 与 current cleanup。当前事实源只保留全局状态、下一步入口、验证证据和归档索引；已关闭阶段的 process source docs 只作为 historical archive evidence。

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/TESTING.md
docs/current/ARCHIVE_INDEX.md
```

Stage-QDR-4 和 Stage-QDR-5 的详细 plan / work order / implementation work order 已归档到 `docs/gates/stage-qdr-4/` 与 `docs/gates/stage-qdr-5/`，不再保留在 `docs/current`。Stage-QDR-5 historical source docs 使用 `SOURCE_` 前缀，旧状态只作为 historical record。
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
docs/gates/stage-qdr-5/
docs/gates/stage-qdr-6/
```

Archive docs are historical records. They are not current factsources and must not override `STATUS.md` or `WORK_ORDER.md`. `docs/archive/**` is not the current QDR archive standard.
