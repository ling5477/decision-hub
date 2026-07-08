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
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_WO: YES
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
current workspace: F:/project/decision-hub
next action: DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO
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
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
docs/current/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_IMPLEMENTATION_WO.md
docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
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
B5 close review retry: CLOSED / ACCEPTED
stage-qdr-4 recommended direction: QDR Replay / Evaluation / Regression Baseline
```

B5 close review 的 ACCEPTED 结论已由用户提供并写回 current factsources。`DH-STAGE-QDR-4-PLAN` 已完成，B1 已按用户授权完成 replay / evaluation domain contracts；B2 persistence baseline plan 已完成，B2 freeze review 已 `PASS`，B2 implementation work order 已完成。B2 implementation 本轮新增 V9 migration、tenant-bound repository ports、JDBC adapters、migration / repository / redaction / tenant isolation tests，并保持 no API / Controller、no real HTTP、no real provider、no Provider SDK、no Agent / LangGraph runtime、no LIVE。B2 blocker fix 已通过 Docker/Testcontainers 真实 PostgreSQL 验证：`V9QdrReplayEvaluationFlywayPostgresTest` 在 PostgreSQL 17 Testcontainer 中执行，Flyway validated 9 migrations，并成功迁移到 version v9，结果为 1 test / 0 failures / 0 errors / 0 skipped。`DH-STAGE-QDR-4-B2-PERSISTENCE-BASELINE-CLOSE-REVIEW` 已完成并判定 `PASS`：V9 migration、tenant-bound repository ports/JDBC adapters、redaction guard、trading-term guard、scoped tests、quality validate 与 safety scan 均满足 close 条件。B2 当前状态为 `CLOSED / ACCEPTED`。`DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-PLAN` 已完成为 `DONE / PLAN_ONLY`：B3 只规划 mock gateway safe refs 到 replay/evaluation/regression persistence 的 future implementation flow、comparison rules、persistence reuse、redaction/trading guard 与测试矩阵。下一步只允许进入 `DH-STAGE-QDR-4-B3-MOCK-GATEWAY-REGRESSION-INTEGRATION-WO`；不允许直接进入 B3 implementation 或 B4。

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
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_WO: YES
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO
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
