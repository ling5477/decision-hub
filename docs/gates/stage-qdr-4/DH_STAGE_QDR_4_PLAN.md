# DH stage-qdr-4 Plan

> current factsource for `DH-STAGE-QDR-4-PLAN`
> planning only; implementation is not started

```text
Task: DH-STAGE-QDR-4-PLAN
Task type: PLANNING_ONLY + STAGE_QDR_4_SCOPE_DESIGN + POST_MODEL_GATEWAY_HARDENING_PLAN + REPLAY_EVAL_PROVIDER_READINESS_REVIEW + SECURITY_BOUNDARY_DESIGN + NO_CODE_CHANGE + NO_TEST_CHANGE + NO_DB_MIGRATION + NO_API_CHANGE + NO_REAL_PROVIDER + NO_REAL_HTTP + NO_AGENT + NO_LANGGRAPH + NO_LIVE
Status: DONE / PLAN_ACCEPTED
Date: 2026-07-08
Repository: F:/project/decision-hub
Branch: dev
HEAD requirement: docs(qdr): record stage-qdr-3 acceptance
```

## 1. 前置门禁

```text
stage-qdr-2: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3: FINAL CLOSE CLOSED / ACCEPTED
stage-qdr-3 B1-B4: DONE / COMMITTED
stage-qdr-3 final close docs sync commit: docs(qdr): record stage-qdr-3 acceptance
stage-qdr-4 planning: DONE / PLAN_ACCEPTED
stage-qdr-4 implementation: NOT_STARTED / NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

本轮门禁结果：工作区开工前 clean，当前分支为 `dev`，`HEAD` 为 `b3fa637 docs(qdr): record stage-qdr-3 acceptance`。本计划只允许进入后续 implementation work order，不允许直接启动 stage-qdr-4 implementation。

## 2. 当前技术基线

stage-qdr-1 到 stage-qdr-3 已形成以下 QDR 基线：

```text
stage-qdr-1: QDR decision core baseline
stage-qdr-2: read model + human approval packet + approval API
stage-qdr-3: Prompt / Model Version / Mock Model Gateway / V8 persistence / QDR pipeline integration
```

已存在事实：

```text
QDR dry-run 只输出 read-only snapshot
ModelGatewayService 先执行 registry / prompt-injection / redaction / budget / ProviderTrustPolicy，再调用 mock provider
MockModelProvider deterministic / in-memory / no outbound
DefaultQdrModelGatewayIntegrationService 将 mock gateway refs 写入 V8 gateway call、V5 trace 与 audit
QdrModelGatewayIntegrationResult 只暴露 safe refs，不承载 raw prompt 或 raw provider response
V8 qdr_model_gateway_call 只保存 redacted summary / hash / ref / metadata
gateway result 只能作为 reasoning / evidence summary
```

当前缺口：

```text
缺少 stage-qdr-1 到 stage-qdr-3 的统一 replay / evaluation / regression baseline
缺少可冻结的 golden regression verdict
缺少面向 mock gateway output 的回归 case 固化
provider readiness 仍停留在 mock-only 边界，尚未进入 real dry-run work order
Agent / LangGraph 仍后置
```

## 3. 候选方向评估

| 方向 | 目标 | 优点 | 风险 | 结论 |
| --- | --- | --- | --- | --- |
| A: QDR replay / evaluation / regression baseline | 让 stage-qdr-1 到 stage-qdr-3 的决策链路可回放、可评估、可做 golden regression | 直接补齐当前最关键的可验证性缺口；不需要真实 provider；能约束 mock gateway 结果和 QDR output 语义 | 可能触碰 replay/eval contracts 和 persistence；若需要 migration 必须单独 review/freeze | 推荐作为 stage-qdr-4 唯一主线 |
| B: Model gateway observability / provider readiness hardening | 加强 model_gateway_call metrics、failure classification、budget reporting、trust decision report | 能提升后续 provider readiness，但依赖 A 的 replay/eval baseline 才能稳定验收 | 容易过早转向 provider readiness，而不是先锁定回归基线 | 第二顺位，作为 A 完成后的 hardening 方向 |
| C: Real provider dry-run readiness plan | 只规划 real provider dry-run 的 feature flag、kill switch、credential boundary、network isolation、no-live policy | 有助于后续真实 provider planning | 当前过早；若先做 C，缺少 replay/eval regression 作为安全回归网 | 第三顺位，仅 planning，不能实现 SDK/HTTP |
| D: Agent / LangGraph preparation | 仅保留为后置方向 | 为未来 Agent orchestration 提前留入口 | 当前启动会绕开 QDR 可审计链路，且违反本阶段边界 | 后置，不进入 stage-qdr-4 |

推荐顺序：

```text
1. A: QDR replay / evaluation / regression baseline
2. B: Model gateway observability / provider readiness hardening
3. C: real provider dry-run readiness plan
4. D: Agent / LangGraph preparation
```

## 4. 推荐主线

```text
stage-qdr-4 = QDR Replay / Evaluation / Regression Baseline
```

stage-qdr-4 的唯一主线是把已完成的 Prompt / Model Version / Mock Model Gateway / V8 persistence / QDR pipeline integration 固化为可审计、可回放、可评估、可回归的 QDR baseline。

stage-qdr-4 明确不是：

```text
不是真实 provider 阶段
不是真实 HTTP 阶段
不是 Provider SDK 阶段
不是 Agent / LangGraph 阶段
不是 LIVE 阶段
不是 NQ runtime integration 阶段
不是 replay execution API 阶段
```

## 5. 批次规划

### B1: Replay / evaluation domain contracts

目标：

```text
replay case contract
evaluation case contract
expected decision summary
regression verdict
mock gateway expected output summary
```

边界：

```text
no API
no Controller
no migration if contracts can stay in code/test-support/golden case form
no replay execution API
no real provider
no NQ
```

Review：若只新增 domain/usecase contracts 与测试，不触碰 API/migration/security runtime，可走 validation + docs sync；如改变 replay/eval 语义边界，必须 review。

### B2: Persistence baseline

目标：

```text
replay/eval case persistence baseline
golden case identity / checksum / verdict persistence if needed
tenant-bound and trace-bound lookup shape
```

边界：

```text
如果需要 migration，必须新开 migration review/freeze
不得修改 V1-V8 migration
不得把 V9 写成已存在
不得保存 raw prompt
不得保存 raw provider response
不得保存 credential
```

Review：必须 review/freeze if touching migration、table、index、constraint、field comment、repository persistence 或 data-retention boundary。

### B3: QDR mock gateway regression integration

目标：

```text
使用 existing dry-run + mock gateway result 生成 regression case
固定 promptVersionRef / modelVersionRef / providerProfileRef / gatewayCallRef / trust / redaction / budget 的 expected summary
验证 gateway result 只进入 evidence / reasoning summary
验证 forbidden action 和 no-side-effect boundary
```

边界：

```text
no real provider
no real HTTP
no Provider SDK
no NQ
no replay execution API
no approval mutation as execution
no trading signal
```

Review：必须 review/freeze if touching QDR pipeline integration、audit trace mapping、mock gateway output contract、fail-closed behavior 或 no-side-effect guard。

### B4: Report / read model / docs-only acceptance support

目标：

```text
提供 replay/eval regression report shape
提供只读 summary / verdict / known residuals
同步 docs/current acceptance support
```

边界：

```text
不新增 execution API
只读 reporting 优先
如需 API，必须另起 API / contract / security review
不把 report 写成 trading instruction
```

Review：如仅 docs/report/read-model contract 可走 validation；如新增 API、Controller、OpenAPI、auth/tenant 行为或 persistence query，必须 review。

### B5: stage-qdr-4 close review

目标：

```text
冻结 B1-B4 evidence
确认 replay/eval/regression baseline 可复核
确认 no real provider / no real HTTP / no Provider SDK
确认 no Agent / no LangGraph / no LIVE
确认 gateway 结果仍不得作为交易信号
```

边界：

```text
review only
no new feature
no code change unless blocker fix is explicitly authorized
no implementation mixed into close review
```

## 6. Review 触发规则

以下情况必须单独 review/freeze：

```text
migration / 表结构 / index / constraint / field comment 变化
API / Controller / OpenAPI / endpoint 变化
auth / tenant / HMAC / nonce / source allowlist 变化
replay/eval persistence boundary 变化
QDR pipeline integration 变化
ModelGateway failure classification / budget / redaction / ProviderTrustPolicy 变化
audit fail-closed / trace mapping 变化
P0/P1 blocker fix
stage close / acceptance
```

普通 docs sync、非安全边界的 contract-only 小改动，不再默认拆成长 review，但仍必须运行相关验证。

## 7. 安全边界

stage-qdr-4 必须继续保持：

```text
no real HTTP
no real provider
no Provider SDK
no API key
no LangGraph
no Agent runtime
no LIVE
no NQ mutation
no trading mutation
no approval-as-execution
no gateway-result-as-trading-signal
no raw prompt storage
no raw provider response storage
no credential storage
all failures fail-closed
all current factsources remain in docs/current
archived docs under docs/gates are historical only
```

gateway result 只允许作为 replay/eval evidence、reasoning summary、audit/trace/read-model ref，不得映射为 `BUY`、`SELL`、`PLACE_ORDER`、`CANCEL_ORDER`、approval execution 或 NQ mutation。

## 8. Work order follow-up

下一步只能输出 implementation work order，不得直接 implementation：

```text
DH-STAGE-QDR-4-IMPLEMENTATION-WORK-ORDER
```

该 work order 必须再次确认：

```text
repository: F:/project/decision-hub
module scope
target files
excluded files
expected output
review triggers
validation commands
rollback plan
no real HTTP / provider / SDK / Agent / LangGraph / LIVE
```

## 9. Acceptance criteria

```text
stage-qdr-3 final close 已提交
本轮只改 docs
stage-qdr-4 recommended direction 明确
stage-qdr-4 implementation 未启动
real provider 未启动
real HTTP 未启动
Agent / LangGraph 未启动
LIVE 仍 DISABLED
批次规划不过细
review 触发规则明确
docs/current current factsources 同步
quality validate PASS
mvnw.cmd 风险如实记录
工作区无 staged
```

## 10. Readiness decision

```text
STAGE_QDR_4_PLAN: DONE
ALLOW_STAGE_QDR_4_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_4_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```
