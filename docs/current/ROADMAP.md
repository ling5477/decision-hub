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
STAGE_QDR_4_FINAL_CLOSE_REVIEW: PASS
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
ALLOW_STAGE_QDR_4_TAG_AFTER_ARCHIVE_COMMIT: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_CLOSE_NOW: NO / ALREADY_DONE
ALLOW_STAGE_QDR_4_TAG_CLOSE_AFTER_CLEANUP: YES / CONSUMED
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
ALLOW_STAGE_QDR_4_FINAL_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_TAG_NOW: NO / ALREADY_TAGGED
```

## 2. 当前下一步

```text
DH-STAGE-QDR-5-PLAN
Then: DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER
Then: Stage-QDR-5 implementation only after accepted work order
```

Stage-QDR-5 plan 已选择推荐主线：

```text
Stage-QDR-5 = Model Gateway Observability / Provider Readiness Hardening
```

推荐顺序：

```text
1. Model Gateway Observability / Provider Readiness Hardening
2. QDR Regression Baseline Hardening
3. Real Provider Dry-run Readiness Plan
4. Agent / LangGraph Preparation
```

Stage-QDR-5 批次规划：

```text
B1: Model Gateway Observability Contracts
B2: Provider Health / Gateway Call Read Model
B3: Provider Readiness Guard / Policy Evaluation
B4: Observability Report / Current Docs / Acceptance Support
B5: Stage-QDR-5 Final Close Review / Archive Close / Tag Close
```

B1-B4 只是 Stage-QDR-5 普通 batch，不打 tag。普通 batch 沿用 implementation + tests + boundary scan + minimal docs + commit；只有 migration、API / Controller、security boundary、stage close 或 P0/P1 blocker 才触发 standalone review。Stage tag 只能在 B5 final close `PASS` 且 archive close commit 存在后，由独立 tag close 任务创建。Real provider dry-run 与 Agent / LangGraph preparation 均后置，不在 Stage-QDR-5 implementation 中启动。

## 3. 后续阶段边界

stage-qdr-4 planning 已 `DONE / PLAN_ACCEPTED`。stage-qdr-4 B1 已 `DONE / DOMAIN_CONTRACTS_ONLY`。B2 plan 已 `DONE / PERSISTENCE_BASELINE_PLAN_ONLY`，B2 freeze review 已 `PASS`，B2 implementation work order 已 `DONE / WORK_ORDER_ONLY`，B2 blocker fix 已 `DONE / TESTCONTAINERS_VERIFIED`，B2 implementation 已 `DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED`，B2 close review 已 `PASS`，B2 已 `CLOSED / ACCEPTED`。B3 plan 已 `DONE / PLAN_ONLY`，B3 WO 已 `DONE / WORK_ORDER_ONLY`，B3 implementation 已 `DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED`，B3 close review 已 `PASS`，B3 已 `CLOSED / ACCEPTED`。B4 plan 已 `DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY`，B4 WO 已 `DONE / WORK_ORDER_ONLY`，B4 implementation 已 `DONE / INTERNAL_REGRESSION_REPORT_READ_MODEL_IMPLEMENTED`。Stage-QDR-4 final close review 已 `PASS`，整体 `CLOSED / ACCEPTED / ARCHIVED / TAGGED`，tag close 已 `DONE`。Stage-QDR-5 plan 已 `DONE / PLAN_ONLY`，implementation 仍为 `NOT_STARTED`；real HTTP、real provider、Provider SDK、Agent / LangGraph runtime 和 LIVE 仍然后置且禁止。

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
