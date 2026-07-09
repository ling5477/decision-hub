# Decision Hub 当前工单

## 1. 唯一下一步

```text
current task: DH-STAGE-QDR-5-PLAN
current task status: DONE / PLAN_ONLY
next action: DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER
mode: PLANNING_ONLY + STAGE_QDR_5_SCOPE_DESIGN + POST_QDR_REPLAY_EVALUATION_PLAN + MODEL_GATEWAY_OBSERVABILITY_REVIEW + PROVIDER_READINESS_BOUNDARY_REVIEW + SECURITY_BOUNDARY_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
```

## 2. 前置状态

```text
STAGE_QDR_4: CLOSED / ACCEPTED / ARCHIVED / TAGGED
STAGE_QDR_4_ARCHIVE: DONE
STAGE_QDR_4_TAG_CLOSE: DONE
STAGE_QDR_4_TAG: DONE / dh-stage-qdr-4-close
STAGE_QDR_4_TAG_TARGET: 62c8020 docs(workflow): repair documentation discipline and skill policy
STAGE_QDR_5_PLAN: DONE / PLAN_ONLY
STAGE_QDR_5_IMPLEMENTATION: NOT_STARTED
ALLOW_STAGE_QDR_5_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_5_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

Stage-QDR-4 已完成 B1 Replay / Evaluation Domain Contracts、B2 Replay / Evaluation Persistence Baseline、B3 Mock Gateway Regression Integration、B4 Regression Report / Read Model Support、final close review、archive close 和 tag close。Stage-QDR-5 只能从 implementation work order 开始，不允许跳过 work order 直接 implementation。

## 3. Stage-QDR-5 推荐主线

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

## 4. 计划批次

```text
B1: Model Gateway Observability Contracts
B2: Provider Health / Gateway Call Read Model
B3: Provider Readiness Guard / Policy Evaluation
B4: Observability Report / Current Docs / Acceptance Support
B5: Stage-QDR-5 Final Close Review / Archive Close / Tag Close
```

B1/B2/B3/B4 只是 Stage-QDR-5 batch，不创建 tag。Stage tag 只能在 B5 final close `PASS` 且 archive close commit 存在后，由独立 tag close 任务创建。

## 5. Review 触发规则

```text
ordinary batch: implementation + tests + boundary scan + minimal docs + commit
standalone review trigger: migration
standalone review trigger: API / Controller
standalone review trigger: security boundary
standalone review trigger: stage close
standalone review trigger: P0 / P1 blocker
stage final close PASS -> archive close -> tag close -> next stage planning
```

普通 batch 不做 standalone review。若 implementation work order 发现必须新增 migration、API / Controller 或安全边界变化，必须停止并输出对应 blocker，进入单独 review 任务。

## 6. 当前禁止范围

```text
禁止修改 Java 生产代码，除非后续 work order 明确授权。
禁止修改 Java 测试代码，除非后续 work order 明确授权。
禁止新增 migration，除非后续 migration review 明确授权。
禁止修改 V1-V9 migration。
禁止新增 V10。
禁止新增 API / Controller / REST endpoint。
禁止接真实 HTTP client。
禁止接真实 provider / Provider SDK。
禁止读取 credential / token / cookie / apiKey / apiSecret / passphrase。
禁止持久化 raw prompt。
禁止持久化 raw provider response。
禁止把 gateway-result、replay output 或 regression output 写成交易信号。
禁止启动 Agent runtime。
禁止接 LangGraph / AutoGen / CrewAI。
禁止修改 NQ。
禁止开启 LIVE。
禁止直接进入 Stage-QDR-5 implementation。
禁止创建新 tag。
禁止 push。
```

## 7. 下一任务

```text
DH-STAGE-QDR-5-IMPLEMENTATION-WORK-ORDER
```
