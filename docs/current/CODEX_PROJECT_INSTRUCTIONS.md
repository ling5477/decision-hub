# Codex Project Instructions

> 项目: Decision Hub
> 必需前置 skill: `nq-dh-workflow-router`
> 必需文档 skill: `dh-docs-writer`
> 当前事实源: `docs/current`
> 当前工作区: `E:/Project/decision-hub`

## 1. 当前状态锁定

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
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES
stage-qdr-4 B4 implementation: NOT_STARTED
stage-qdr-4 final close: NOT_STARTED
ALLOW_STAGE_QDR_4_B3_IMPLEMENTATION: YES / CONSUMED
ALLOW_STAGE_QDR_4_B3_CLOSE_REVIEW: YES / CONSUMED
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION_NOW: NO / DOCS_ONLY_WO_TURN
ALLOW_STAGE_QDR_4_B5_FINAL_CLOSE_NOW: NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
next action: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION
```

## 2. 前置分类规则

每个 Codex 任务必须先使用 `nq-dh-workflow-router` 分类，再决定 skill、scope、文件范围和验证命令。DH 文档治理、`docs/current`、archive、work order、acceptance、freeze、close review、WORKLOG、TESTING、STATUS、ROADMAP、API 与 DB_SCHEMA 同步必须使用 `dh-docs-writer`。

固定输出字段：

```text
Task classification:
Plugins selected:
Scope:
Files inspected:
Files changed:
Findings:
Validation:
Risks:
Next concrete action:
```

`Summary` 不是必填字段。

## 3. 当前事实源规则

stage-qdr-4 planning 的当前事实源文件只包括：

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

以下文件默认 supporting only，不作为 primary stage gate source：

```text
docs/current/WORKLOG.md
docs/current/ROADMAP.md
docs/current/API.md
docs/current/DB_SCHEMA.md
```

归档历史默认不作为 blocker：

```text
docs/gates/**
docs/archive/** 仅当历史遗留目录存在时使用；QDR 当前归档标准不是 docs/archive
```

只有 `FACTSOURCE_POLICY.md` 定义的硬错误可让 supporting docs 升级为 blocker。stage-qdr-4 B1 已按用户授权完成，B2 persistence baseline plan 已完成，B2 freeze review 已 PASS，B2 implementation work order 已完成，B2 implementation 已完成并通过真实 PostgreSQL/Testcontainers Flyway load 验证。B2 close review 已 `PASS`，B2 当前状态为 `CLOSED / ACCEPTED`。B3 mock gateway regression integration plan 已 `DONE / PLAN_ONLY`，B3 mock gateway regression integration work order 已 `DONE / WORK_ORDER_ONLY`，B3 implementation 已 `DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED`，B3 close review 已 `PASS`，B3 当前状态为 `CLOSED / ACCEPTED`。B4 regression report / read model support plan 已 `DONE / PLAN_ONLY`，B4 implementation work order 已 `DONE / WORK_ORDER_ONLY`。下一步只能进入 `DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION`。不得把 B4 与 B5 合并实施，不得在 B4 implementation 中直接新增 API / Controller，不得在 B4 打 tag。

## 4. 安全边界

```text
DH 不直接下单
DH 不绕过 NQ 风控
DH 不修改 NQ 订单状态
DH 不读写 NQ DB
DH 不访问交易所密钥
DH 不启动 Paper Run
DH 不接真实 HTTP
DH 不接真实 provider
DH 不引入 Provider SDK
DH 不启动 Agent / LangGraph runtime
DH 不启用 LIVE
```

`LONG_BIAS / SHORT_BIAS` 只是 bias，不得映射成 `BUY / SELL`。`APPROVED` 不是 `BUY`，`REJECTED` 不是 `SELL`。Gateway result 只能作为只读 evidence / reasoning summary，不得触发交易、approval mutation、NQ mutation、risk mutation、ledger mutation、paper 或 live mutation。

## 5. 文档语言与路径规则

正文使用简体中文。类名、字段名、状态枚举、HTTP header、命令、路径和外部技术名保留英文原样。当前路径统一为 `E:/Project/decision-hub`。若历史文档出现旧路径，只能作为 historical record，不得覆盖当前工作区。

## 6. 验证纪律

docs-only 治理任务至少运行：

```powershell
git status --short
git diff --check
git diff --stat
```

本轮 QDR B1 / B2 / B3 边界治理还必须运行：

```powershell
git diff --name-only
git diff --cached --name-only
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

`mvnw.cmd` 当前仍不可写成可用。Docker/Testcontainers skip 只能写成环境型 skip，不得写成 PASS。

## 7. B4 implementation 当前入口

```text
previous task: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION-WO / DONE
plan source: docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_PLAN.md
B4 implementation WO source: docs/current/DH_STAGE_QDR_4_B4_REGRESSION_REPORT_READ_MODEL_SUPPORT_IMPLEMENTATION_WO.md
previous B3 plan source: docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_PLAN.md
previous B3 work order source: docs/current/DH_STAGE_QDR_4_B3_MOCK_GATEWAY_REGRESSION_INTEGRATION_WO.md
B2 freeze/review: PASS
B2 blocker fix: DONE / TESTCONTAINERS_VERIFIED
B2 implementation: DONE / IMPLEMENTED / POSTGRES_FLYWAY_VERIFIED
B2 close review: PASS
B2 status: CLOSED / ACCEPTED
B3 plan: DONE / PLAN_ONLY
B3 implementation work order: DONE / WORK_ORDER_ONLY
B3 implementation: DONE / MOCK_GATEWAY_REGRESSION_INTEGRATED
B3 close review: PASS
B3 status: CLOSED / ACCEPTED
ALLOW_STAGE_QDR_4_B4_PLAN: YES / CONSUMED
B4 plan: DONE / REGRESSION_REPORT_READ_MODEL_PLAN_ONLY
B4 implementation work order: DONE / WORK_ORDER_ONLY
ALLOW_STAGE_QDR_4_B4_IMPLEMENTATION: YES
B4 implementation: NOT_STARTED
Stage-QDR-4 final close: NOT_STARTED
V9 migration: CREATED / V9__qdr_replay_evaluation_baseline.sql / POSTGRES_LOAD_VERIFIED
Repository / JDBC implementation: DONE / TENANT_BOUND
API / Controller: NO
real HTTP: NO
real provider: NO
Provider SDK: NO
Agent / LangGraph: NO
LIVE: DISABLED
next action: DH-STAGE-QDR-4-B4-REGRESSION-REPORT-READ-MODEL-SUPPORT-IMPLEMENTATION
```
