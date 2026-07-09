# Stage-QDR-4 Plan

## Task Classification

```text
DOCUMENTATION_ARCHIVE_PACKET_BACKFILL
STAGE_ARCHIVE_HISTORY
NO_CODE_CHANGE
NO_TEST_CHANGE
NO_DB_MIGRATION
NO_API_CHANGE
NO_REAL_PROVIDER
NO_REAL_HTTP
NO_AGENT
NO_LANGGRAPH
NO_LIVE
```

## Stage Scope

```text
Stage name: Stage-QDR-4
Stage title: QDR Replay / Evaluation / Regression Baseline
Final state: CLOSED / ACCEPTED / ARCHIVED / TAGGED
Tag: dh-stage-qdr-4-close
Tag target: 62c8020 docs(workflow): repair documentation discipline and skill policy
```

Stage-QDR-4 的计划目标是把 QDR replay、evaluation、persistence baseline、mock gateway regression 和 regression report read model 收口为可审计的 replay / evaluation baseline。该阶段不授权真实 provider、真实 HTTP、Provider SDK、Agent / LangGraph runtime、LIVE、NQ mutation 或 trading execution。

## Source Evidence

```text
Primary archived plan: docs/gates/stage-qdr-4/DH_STAGE_QDR_4_PLAN.md
B2 plan: docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B2_PERSISTENCE_BASELINE_PLAN.md
B3 plan: docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
B4 plan: docs/gates/stage-qdr-4/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
Current status snapshot: docs/gates/stage-qdr-4/STATUS_SNAPSHOT.md
```

## Batch Plan

```text
B1: Replay / Evaluation Domain Contracts
B2: Replay / Evaluation Persistence Baseline
B3: Mock Gateway Regression Integration
B4: Regression Report / Read Model Support
B5: Final close review -> archive close -> tag close
```

## Boundary Confirmation

```text
no real provider
no real HTTP
no Provider SDK
no Agent / LangGraph runtime
no LIVE
no NQ mutation
no trading signal
no raw prompt / raw provider response / credential storage
```

## Readiness Decision

```text
Stage-QDR-4 plan: DONE / PLAN_ACCEPTED
Final close: PASS
Archive close: DONE
Tag close: DONE
Next concrete action from this archive: historical reference only
```
