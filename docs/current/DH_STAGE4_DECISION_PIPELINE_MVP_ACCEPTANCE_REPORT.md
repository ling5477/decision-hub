# DH Stage4 Decision Pipeline MVP Acceptance Report

> 任务: DH-STAGE4-DECISION-PIPELINE-MVP-K8-ACCEPTANCE-FREEZE
> 日期: 2026-07-02  
> 结论: DH Stage4 Decision Pipeline MVP `ACCEPTED / CLOSED`
> 下一步: `NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE / CLOSED / ACCEPTED`（后续进入 `NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / NOT STARTED`）

## 1. 任务范围

本轮只执行 DH Stage4 Decision Pipeline MVP 的最终验收、回归验证、安全边界复核和文档冻结。

本轮未修改生产代码、测试代码、contracts、golden_cases、API path、Controller、migration、Repository、Service、Client 或 provider 实现；未接真实 NQ、真实 provider、HTTP、LLM、LangGraph 或 LIVE。

## 2. K1-K7 验收清单

| 批次 | 结论 | 验收证据 |
| --- | --- | --- |
| K1 Contract Freeze | `CLOSED / ACCEPTED` | `DecisionType` 仅 `READ_ONLY_RECOMMENDATION`；`DecisionAction` 仅 `ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`；`forbiddenActions` 固定五项；request/output schema 不含交易执行、账户或凭证字段；contract tests 通过。 |
| K2 Orchestrator Skeleton | `CLOSED` | `DefaultDecisionOrchestrator` 只使用 deterministic mock provider；无 agent runtime、真实 provider、NQ runtime、HTTP 或 LangGraph；policy/provider/persistence failure 均 fail-closed 到 structured `DecisionOutput`。 |
| K3 Audit / Snapshot / Trace Persistence | `CLOSED / ACCEPTED` | Flyway V5 只新增 DH-owned decision request、context snapshot、trace step、provider call log、output、audit event 六张表；migration 注释和 usecase 脱敏逻辑禁止 secret/token/credential/NQ DB 内容；persistence failure fail-closed。 |
| K4 Replay Read Model | `CLOSED` | replay read model 只读 K3 六类 DH-owned 表；不新增 API、Controller、migration 或 replay endpoint；不重跑 provider/orchestrator；tenant mismatch、corrupted/incomplete data 均 fail-closed。 |
| K5 Provider Health / Budget / Latency | `CLOSED` | provider guard 只作用于 mock provider；disabled/unhealthy/timeout/budget exceeded 均 fail-closed；provider call summary 写入 K3 provider log 并可经 K4 replay 读取；无真实 provider。 |
| K6 Mock NQ Dry-run Contract Tests | `CLOSED` | mock NQ request factory、fixture、no-live-trade guard、persistence-to-replay contract tests 均通过；不修改 NQ 仓库、不真实 HTTP、不执行 NQ mutation。 |
| K7 Golden Cases / Eval | `CLOSED` | `golden_cases/decision` 共 12 个 deterministic cases；action 白名单、固定 forbiddenActions、credential/account/order/execution 禁止字段扫描和 eval baseline tests 均通过。 |

## 3. 验收结论

```text
DH Stage4 Decision Pipeline MVP: ACCEPTED / CLOSED
K1-K7: CLOSED
K8 Acceptance / Freeze: CLOSED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

K1-K7 已形成稳定闭环：只读合同、mock-only orchestrator、audit/snapshot/trace persistence、internal replay read model、mock provider guard、mock NQ dry-run contract tests、golden cases/eval baseline 均可由代码、测试和文档证据复核。

## 4. 测试结果

```text
git status --short
  结果: 开工前为空；最终仅文档和冻结快照变更。

git diff --check
  结果: 通过；无 whitespace error。

git diff --stat
  结果: 仅文档同步和 docs/gates 冻结快照。

mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test
  结果: BUILD SUCCESS；reactor 15/15 SUCCESS；Total time 18.980 s；Finished at 2026-07-02T19:39:15+08:00。
  说明: 当前 sandbox 下 Testcontainers 无可用 Docker 环境，PostgresContainerSmokeTest skipped 1。

mvn -ntp test
  结果: BUILD SUCCESS；reactor 19/19 SUCCESS；Total time 18.355 s；Finished at 2026-07-02T19:39:53+08:00。
  说明: 当前 sandbox 下 Testcontainers 无可用 Docker 环境，PostgresContainerSmokeTest skipped 1。

mvn -ntp -Pquality validate
  结果: BUILD SUCCESS；reactor 19/19 SUCCESS；0 Checkstyle violations；Spotless check passed；Finished at 2026-07-02T19:40:04+08:00。
```

Docker 探测：

```text
docker info --format '{{.ServerVersion}}'
  结果: failed to connect to the docker API at npipe:////./pipe/dockerDesktopLinuxEngine；
        named pipe 不存在，说明当前 Docker daemon 未运行或未暴露该管道。
  判定: 环境未覆盖项，不是 Stage4 代码回归失败。
```

## 5. 质量门结果

`mvn -ntp -Pquality validate` 已通过。Checkstyle 聚合结果为 `0 violations`，Spotless check passed。各子模块 `unable to find checkstyle:checkstyle outputFile` 仍为既有非阻断信息，最终 reactor status 为 `SUCCESS`。

## 6. 安全边界确认

关键词扫描覆盖：

```text
RealClient / LangGraph / OpenAI / Claude / Gemini / WebClient / RestTemplate /
HttpClient / placeOrder / cancelOrder / BUY / SELL / apiSecret / passphrase /
accountId / Controller / NqClient / Exchange / Broker / live / LIVE /
endpoint / Endpoint / PostMapping / GetMapping / RequestMapping
```

扫描结论：

- 生产范围命中仅来自禁止说明、migration comment、脱敏/denylist 和配置注释。
- 完整允许范围命中集中在文档说明、负向测试断言、denylist、historical/deferred 说明、migration comment 与固定 `forbiddenActions`。
- 未发现 DH Stage4 Decision Pipeline 新增 API、Controller、mapping annotation、HTTP client、RealClient、real provider、NQ runtime、LangGraph runtime、LIVE 或 BUY/SELL action 生产实现。

## 7. 禁止项确认

```text
未修改生产代码
未修改测试代码
未新增 API path
未新增 Controller
未新增 migration
未新增 Repository / Service / Client 实现
未真实 HTTP
未真实 NQ 调用
未真实 DH runtime integration
未真实交易所调用
未新增 RealClient
未新增真实 Provider
未接 OpenAI / Claude / Gemini / 本地模型
未接 LangGraph
未接 MCP 写能力
未读取或输出 credential / token / cookie / API secret / passphrase
未启动 Integration-1 runtime
未把 DH 写成 integrated
未把 Runtime integration 写成 started
未把 AI / Agent runtime 写成 started
未开启 LIVE
未修改 NQ 仓库
未把 Stage4 完成写成允许真实交易
未把 mock NQ dry-run 写成真实 NQ runtime
未把 replay read model 写成 replay API
未把 golden cases 写成真实交易验证
```

## 8. Freeze

冻结路径：

```text
docs/gates/dh-stage4-decision-pipeline-mvp/
```

冻结范围：`docs/current` 当前事实源全量快照，含本验收报告。冻结快照为历史记录；后续若发现措辞或事实需要修正，只能在 `docs/current` 写 errata 或 clarification，不得直接修改历史快照冒充原始事实。

## 9. 剩余风险

- Integration-1 仍需基于 NQ GateN 重新规划。
- Integration-1 runtime 仍未开始。
- LangGraph 后置。
- Agent phase 后置。
- 真实 provider 后置。
- LIVE 仍禁用。
- 当前 sandbox 未运行 Docker-gated `PostgresContainerSmokeTest`；该项需要 Docker daemon 可用后由 CI 或本地 Docker 环境复跑。

## 10. Readiness decision

```text
ALLOW_STAGE4_CLOSE: YES
ALLOW_INTEGRATION1_DRYRUN_PLAN_REBASE_N: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步只允许进入：

```text
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN
```
