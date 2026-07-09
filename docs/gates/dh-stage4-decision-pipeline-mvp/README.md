# DH Stage4 Decision Pipeline MVP Freeze Snapshot

> 冻结时间: 2026-07-02T19:45+08:00  
> 冻结阶段: DH-STAGE4-DECISION-PIPELINE-MVP-K8-ACCEPTANCE-FREEZE
> 冻结结论: DH Stage4 Decision Pipeline MVP `ACCEPTED / CLOSED`
> 来源: `docs/current`  
> 快照路径: `docs/gates/dh-stage4-decision-pipeline-mvp/`

## 0. 命名修正声明

本目录原错误目录名为 `dh-gatek-decision-pipeline-mvp`，正确目录名为 `dh-stage4-decision-pipeline-mvp`。本次 `DH-STAGE4-NAMING-REBASE-FIX` 只修正 DH 自身阶段命名和冻结目录命名，不改变原冻结快照的验收事实。

冻结快照原本验收结论仍为 `ACCEPTED / CLOSED`。目录内部分历史文件名、历史命令输出和冻结时刻记录仍可能保留 `DH_GATEK`、`DH-GATEK`、`GateK` 或 `dh-gatek` 字样；这些只代表原始历史记录或旧错误命名，不得作为当前事实源。当前事实源以 `docs/current` 中的 `DH-STAGE4-DECISION-PIPELINE-MVP` 和 `NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED` 为准。

## 0.1 文档纪律勘误索引（2026-07-09）

本目录是冻结快照，不做大规模历史正文重写。`DH-DOCS-DISCIPLINE-CLEANUP-IMPLEMENTATION` 仅确认以下索引规则：

```text
legacy GateK wording: historical-only / naming residual
current authority: docs/current
current Stage4 canonical name: DH-STAGE4-DECISION-PIPELINE-MVP
do not treat archived GateK wording as current task name
do not use this archive to start Stage-QDR-5 implementation
do not use this archive to authorize provider / HTTP / Agent / LangGraph / LIVE
```

## 1. 冻结范围

本目录是 `docs/current` 在 DH Stage4 Decision Pipeline MVP K8 acceptance / freeze 关闭时的历史快照，包含当前事实源文档、Stage4 plan/work order、acceptance report、测试记录、工作日志、API 事实说明、Integration-0 安全文档和 Stage 历史上下文。

本次冻结只归档文档事实源，不表示新增 runtime 能力；不授权真实 NQ、真实 provider、HTTP、LLM、LangGraph、Integration-1 runtime 或 LIVE。

## 2. 验收结论

```text
DH Stage4 Decision Pipeline MVP: ACCEPTED / CLOSED
K1 Contract Freeze: CLOSED
K2 Orchestrator Skeleton: CLOSED
K3 Audit / Snapshot / Trace Persistence: CLOSED
M1 Readiness Review: CLOSED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED
Next: NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

## 3. 文件清单

```text
API.md
ARCHITECTURE.md
CODEX_PROJECT_INSTRUCTIONS.md
CODEX_WORKFLOW_INDEX.md
DB_SCHEMA.md
DH_AUDIT_FIX_REPORT.md
DH_CODEX_PLUGIN_WORKFLOW.md
DH_CODEX_TASK_TEMPLATES.md
DH_FULL_SECURITY_AUDIT_REPORT.md
DH_GATEK_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md
DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md
DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md
DH_NQ_HEADER_ALIGNMENT_PLAN.md
DH_NQ_INTEGRATION.md
DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md
DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md
DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md
DH_NQ_INTEGRATION0_SECURITY_POLICY.md
DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md
DH_P1_4_RESIDUAL_FIX_PLAN.md
DH_REFACTOR_STAGE1_STATUS.md
DH_REFACTOR_STAGE1_WORKLOG.md
DH_REFACTOR_STAGE1_WORK_ORDER.md
DH_WORKFLOW_ROUTER_SKILL.md
DOCS_STRUCTURE.md
NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md
README.md
ROADMAP.md
STAGE1_CLOSE_WORKLOG.md
STAGE2_POC_API_PLAN.md
STAGE2_POC_CONTRACT_PLAN.md
STAGE2_POC_DB_PLAN.md
STAGE2_POC_PLAN.md
STAGE2_POC_TEST_PLAN.md
STAGE2_POC_VERIFY_REPORT.md
STAGE2_POC_WORK_ORDER.md
STAGE3_BATCH_PLAN.md
STAGE3_CONTRACT_PLAN.md
STAGE3_DH_BACKTEST_ADAPTER_SPEC.md
STAGE3_DH_TO_NQ_BACKTEST_PLAN.md
STAGE3_E2E_CONTRACT_TEST_SPEC.md
STAGE3_NQ_OUTBOX_SPEC.md
STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md
STAGE3_PLAN.md
STAGE3_TEST_PLAN.md
STAGE3_WORK_ORDER.md
STATUS.md
TESTING.md
WORKFLOW.md
WORKLOG.md
WORK_ORDER.md
```

## 4. 验证摘要

```text
mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
  BUILD SUCCESS；reactor 15/15 SUCCESS。

mvn -ntp test
  BUILD SUCCESS；reactor 19/19 SUCCESS。

mvn -ntp -Pquality validate
  BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed。

Boundary scan
  未发现 DH Stage4 production 越界实现；命中均为禁止说明、negative tests、denylist、
  migration comment、historical/deferred docs 或固定 forbiddenActions。
```

当前本地 sandbox 中 Docker daemon 管道不可用，`PostgresContainerSmokeTest` 在 Maven 测试中按 Testcontainers 机制 skipped 1；该项属于环境未覆盖项，不是 Stage4 代码失败。

## 5. 不可修改历史快照声明

本目录为历史冻结快照。冻结后不得直接修改本目录内容来改写历史事实。

如后续发现需要修正的措辞、状态或补充证据，只能在 `docs/current` 新增 errata、clarification 或后续阶段文档，并在新的 gate/freeze 中再次归档；不得直接改写本快照冒充原始冻结事实。

## 6. 后续边界

```text
ALLOW_STAGE4_CLOSE: YES
ALLOW_INTEGRATION1_DRYRUN_PLAN_REBASE_N: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步只允许 `NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / NOT STARTED`，且只能做基于 NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED 的 factsource rebase；仍不得启动 Integration-1 runtime、真实 NQ runtime、真实 provider、LangGraph runtime、AI / Agent runtime 或 LIVE。
