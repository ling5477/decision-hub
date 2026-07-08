# Decision Hub Roadmap

> supporting document
> not primary stage gate source
> old history must not override `docs/current/STATUS.md` or `docs/current/WORK_ORDER.md`

## 1. 当前路线

DH 的目标不是成为交易系统，而是成为 NQ 的 AI Agent 决策能力层。当前主线为 Quant Decision Review。

```text
stage-qdr-1: CLOSED / ACCEPTED
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
B4 implementation work order: DONE / WORK_ORDER_ONLY
STAGE_QDR_4_B4_IMPLEMENTATION_WO: DONE
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES / CONSUMED
B4 implementation: DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED
stage-qdr-4 final close: NOT_STARTED
stage-qdr-4 tag: NOT_CREATED
ALLOW_STAGE_QDR_4_FINAL_CLOSE_REVIEW: YES
ALLOW_STAGE_QDR_4_TAG_NOW: NO
```

## 2. 当前下一步

```text
DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW
```

stage-qdr-4 plan 已选择唯一主线：

```text
stage-qdr-4 = QDR Replay / Evaluation / Regression Baseline
```

推荐顺序：

```text
1. QDR replay / evaluation / regression baseline
2. Model gateway observability / provider readiness hardening
3. real provider dry-run readiness plan
4. Agent / LangGraph preparation
```

stage-qdr-4 B1 已完成 domain/usecase contracts。B2 persistence baseline plan 已完成，B2 freeze review 已 `PASS`，B2 implementation work order 已完成，B2 implementation 已新增 V9 migration、tenant-bound repository ports、JDBC adapters 和配套测试。V9 PostgreSQL/Flyway load test 已通过真实 Testcontainers PostgreSQL 验证。B2 close review 已 `PASS`，B2 当前状态为 `CLOSED / ACCEPTED`。B3 mock gateway regression integration plan 已 `DONE / PLAN_ONLY`，B3 mock gateway regression integration work order 已 `DONE / WORK_ORDER_ONLY`。B3 implementation 已实现 deterministic mock gateway regression flow：existing dry-run / mock gateway safe refs -> replay case -> evaluation case -> expected/actual summary -> regression comparison -> verdict -> finding list；实现范围为 MockGatewayRegressionCaseBuilder、QdrRegressionEvaluationService、QdrRegressionComparator、RegressionBaselinePolicy、RegressionEvidenceRef 与 B2 repository port 复用，不新增 migration、API、Controller、真实 HTTP、真实 provider、Provider SDK、Agent / LangGraph runtime 或 LIVE。B3 close review 已 `PASS`，B3 当前状态为 `CLOSED / ACCEPTED`。B4 regression report / read model support plan 已 `DONE / PLAN_ONLY`，B4 implementation work order 已 `DONE / WORK_ORDER_ONLY`。B4 implementation 已完成 tenant-bound internal read model / report service implementation，不新增 API / Controller / migration / V9 修改 / provider / HTTP / Agent / LangGraph / LIVE。下一步只允许进入 `DH-STAGE-QDR-4-FINAL-CLOSE-REVIEW`，不得直接 tag。

## 3. 后续阶段边界

stage-qdr-4 planning 已 `DONE / PLAN_ACCEPTED`。stage-qdr-4 B1 已 `DONE / DOMAIN_CONTRACTS_ONLY`。B2 plan 已 `DONE / PERSISTENCE_BASELINE_PLAN_ONLY`，B2 freeze review 已 `PASS`，B2 implementation work order 已 `DONE / WORK_ORDER_ONLY`，B2 blocker fix 已 `DONE / TESTCONTAINERS_VERIFIED`，B2 implementation 已 `DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED`，B2 close review 已 `PASS`，B2 已 `CLOSED / ACCEPTED`。B3 plan 已 `DONE / PLAN_ONLY`，B3 WO 已 `DONE / WORK_ORDER_ONLY`，B3 implementation 已 `DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED`，B3 close review 已 `PASS`，B3 已 `CLOSED / ACCEPTED`。B4 plan 已 `DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY`，B4 WO 已 `DONE / WORK_ORDER_ONLY`，B4 implementation 已 `DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED`。固定顺序为 B4 plan -> B4 WO -> B4 implementation -> Stage-QDR-4 final close review；B4 implementation 只允许 usecase/internal read model，不得新增 API / Controller，不得新增 migration，不得修改 V9，不得接 provider / HTTP / Agent / LangGraph。B4 read model / report 仍必须保持只读，Agent / LangGraph 必须后置，不进入 stage-qdr-4。stage tag 只允许在 Stage-QDR-4 final close PASS 后处理，B4 不打 tag。

## 4. 持续禁止项

```text
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
NQ mutation: NO
NQ DB read/write: NO
trading execution: NO
```

历史路线、旧 work order、旧 review / freeze 记录、blocker fix 过程和 pre-close snapshots 已归入 `docs/gates/**` 或由 `ARCHIVE_INDEX.md` 索引。它们保留复盘价值，但不能作为当前 next action 或 B5 blocker，除非触发 `FACTSOURCE_POLICY.md` 定义的硬错误。`docs/archive/**` 不再作为 QDR 当前归档标准。
