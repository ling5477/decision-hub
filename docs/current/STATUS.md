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
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B2_PERSISTENCE_BASELINE / DONE
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
stage-qdr-4 B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
STAGE_QDR_4_B2_CLOSE_REVIEW: PASS
STAGE_QDR_4_B2: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B3_PLAN: YES
stage-qdr-4 B3 plan: DONE / PLAN_ONLY
STAGE_QDR_4_B3_PLAN: DONE
stage-qdr-4 B3 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B3_IMPLEMENTATION_WO: DONE
stage-qdr-4 B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
STAGE_QDR_4_B3_CLOSE_REVIEW: PASS
STAGE_QDR_4_B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES / CONSUMED
stage-qdr-4 B4 plan: DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY
STAGE_QDR_4_B4_PLAN: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES / CONSUMED
stage-qdr-4 B4 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES / CONSUMED
stage-qdr-4 B4 implementation: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG: PENDING
ALLOW_STAGE_QDR_4_TAG_AFTER_ARCHIVE_COMMIT: YES
ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO
ALLOW_STAGE_QDR_4_TAG_CLOSE_AFTER_CLEANUP: YES
ALLOW_STAGE_QDR_5_PLAN_NOW: NO
ALLOW_STAGE_QDR_5_PLAN_AFTER_TAG: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_NOW: NO
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
current task: DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION
next action after cleanup: DH-STAGE-QDR-4-TAG-CLOSE
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
docs/current/ARCHIVE_INDEX.md
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
stage-qdr-4 B1: DONE / DOMAIN_CONTRACTS_ONLY
stage-qdr-4 implementation: B2_PERSISTENCE_BASELINE / DONE
stage-qdr-4 B2 plan: DONE / PERSISTENCE_BASELINE_PLAN_ONLY
stage-qdr-4 B2 freeze/review: PASS
stage-qdr-4 B2 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
stage-qdr-4 B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
stage-qdr-4 B2 close review: PASS
stage-qdr-4 B2: CLOSED / ACCEPTED
stage-qdr-4 B3 plan: DONE / PLAN_ONLY
stage-qdr-4 B3 implementation work order: DONE / WORK_ORDER_ONLY
stage-qdr-4 B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
stage-qdr-4 B3 close review: PASS
stage-qdr-4 B3: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES / CONSUMED
stage-qdr-4 B4 plan: DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY
STAGE_QDR_4_B4_PLAN: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES / CONSUMED
stage-qdr-4 B4 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES / CONSUMED
stage-qdr-4 B4 implementation: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG: PENDING
ALLOW_STAGE_QDR_4_TAG_AFTER_ARCHIVE_COMMIT: YES
ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO
ALLOW_STAGE_QDR_4_TAG_CLOSE_AFTER_CLEANUP: YES
ALLOW_STAGE_QDR_5_PLAN_NOW: NO
ALLOW_STAGE_QDR_5_PLAN_AFTER_TAG: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_NOW: NO
B5 close review retry: CLOSED / ACCEPTED
stage-qdr-4 recommended direction: QDR Replay / Evaluation / Regression Baseline
```

B5 close review 的 ACCEPTED 结论已由用户提供并写回 current factsources。`DH-STAGE-QDR-4-PLAN` 已完成，B1 已按用户授权完成 replay / evaluation domain contracts；B2 persistence baseline plan 已完成，B2 freeze review 已 `PASS`，B2 implementation work order 已完成。B2 implementation 本轮新增 V9 migration、tenant-bound repository ports、JDBC adapters、migration / repository / redaction / tenant isolation tests，并保持 no API / Controller、no real HTTP、no real provider、no Provider SDK、no Agent / LangGraph runtime、no LIVE。B2 blocker fix 已通过 Docker/Testcontainers 真实 PostgreSQL 验证：`V9QdrReplayEvaluationFlywayPostgresTest` 在 PostgreSQL 17 Testcontainer 中执行，Flyway validated 9 migrations，并成功迁移到 version v9，结果为 1 test / 0 failures / 0 errors / 0 skipped。`DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW` 已完成并判定 `PASS`：V9 migration、tenant-bound repository ports/JDBC adapters、redaction guard、trading-term guard、scoped tests、quality validate 与 safety scan 均满足 close 条件。B2 当前状态为 `CLOSED / ACCEPTED`。`DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN` 已完成为 `DONE / PLAN_ONLY`，B3 implementation work order 已完成为 `DONE / WORK_ORDER_ONLY`。B3 implementation 本轮实现 deterministic mock gateway regression flow：existing dry-run / QDR decision artifact -> mock model gateway summary -> replay case -> evaluation case -> expected/actual summary -> regression comparison -> regression verdict -> finding list；复用 B2/V9 repository ports，不新增 schema/API/Controller，不接真实 provider/HTTP/Agent/LangGraph，不进入 LIVE。`DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-CLOSE-REVIEW` 已完成并判定 `PASS`，B3 当前状态为 `CLOSED / ACCEPTED`。`DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-PLAN` 已完成为 `DONE / PLAN_ONLY`；`DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO` 已完成为 `DONE / WORK_ORDER_ONLY`。B4 implementation 本轮已完成 tenant-bound internal regression report / read model support：新增 `RegressionReportQuery`、`RegressionReportView`、`RegressionReportFindingView`、`RegressionDriftSummary` 与 `RegressionReadModelService`，复用 B2/V9 repository ports，不新增 migration、不修改 V9、不新增 API / Controller、不接 provider / HTTP / Agent / LangGraph / LIVE，不把 report/verdict/drift 写成 trading signal。`DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW` 已完成并判定 `PASS`：B1/B2/B3/B4 evidence、scoped tests、quality validate、safety scan 与跨阶段边界均满足 close 条件。Stage-QDR-4 当前状态为 `CLOSED / ACCEPTED / ARCHIVED`，stage tag 为 `PENDING`。当前任务为 `DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION`；本轮不得创建 tag，不得进入 Stage-QDR-5。cleanup 完成并保持 clean 后，下一步才允许进入独立 `DH-STAGE-QDR-4-TAG-CLOSE`。Stage-QDR-5 只能在 tag close 后 planning-first。

## 4. 禁止项

```text
ALLOW_STAGE_QDR_3_FINAL_CLOSE: YES / CONSUMED
ALLOW_STAGE_QDR_4_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_4_B1_DOMAIN_CONTRACTS: YES / CONSUMED
ALLOW_STAGE_QDR_4_B2_PERSISTENCE_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_4_B2_FREEZE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_B2_IMPLEMENTATION_WO: YES / CONSUMED
ALLOW_STAGE_QDR_4_B2_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_4_B2_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_WO: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_B4_PLAN: YES / CONSUMED
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_WO: YES / CONSUMED
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_4_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_AFTER_ARCHIVE_COMMIT: YES
ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO
ALLOW_STAGE_QDR_4_TAG_CLOSE_AFTER_CLEANUP: YES
ALLOW_STAGE_QDR_5_PLAN_NOW: NO
ALLOW_STAGE_QDR_5_PLAN_AFTER_TAG: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_TAG_NOW: NO
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
