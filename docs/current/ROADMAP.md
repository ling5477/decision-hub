# Decision Hub Roadmap

## 1. 总路线

DH 的目标不是成为交易系统，而是成为 NQ 的 AI Agent 决策能力层。

路线：

```text
DH-REFIT-1:   文档结构与边界统一                          [completed]
Stage1:       Boundary Freeze + Agent Runtime Skeleton    [completed]
Stage1-CLOSE: 旧链路 @Deprecated + 文档单源 + ArchUnit    [completed]
Integration-0-PLAN: 只读边界、契约冻结、权限模型、审计模型  [next]
Stage2-PoC:   NQ 真实事件回流 + 工具接口预留              [historical / superseded / deferred]
Stage3:       NQ Console AI 页面接入                      [later / gated]
DH-FREEZE:    冻结 DH Agent Decision Layer v1             [later]
```

## 2. Stage1（已完成）

目标：建立 Agent Runtime Skeleton。

交付：

```text
domain.research.ResearchRun (+Status)
domain.agent.AgentTask / TaskNode / AgentRole / AgentArtifact
domain.candidate.StrategyCandidate / SignalProposal
domain.judge.JudgeDecision / RiskReview / DecisionRecommendation
domain.experience.ExperienceEntry / PheromoneEdge
domain.feedback.NqFeedbackEvent / FeedbackSource
usecase.agent.* + impl + InMemory repositories
memory.agent.* + InMemory stores
eval.agent.* + rule scorers
connector.nq.* + fake adapters
api.research.ResearchRunController（POST/GET /api/ai/research-runs/...）
api.feedback.NqFeedbackController（POST /api/ai/feedback/nq）
dh-app/AgentRuntimeWiringConfig + V2__dh_agent_runtime.sql
ResearchRunStage1ClosedLoopTest（create→start→candidate→judge→feedback→experience）
```

验收：`mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false` BUILD SUCCESS。

## 3. Stage1-CLOSE（已完成）

目标：让 Stage1 成为仓库唯一主链路，文档单源，ArchUnit 兜底新边界。

交付：

```text
domain.run.* / api.legacy.run.* / usecase.facade / usecase.run / usecase.gate /
usecase.contract / dh-providers 全部 @Deprecated(since="Stage1-CLOSE", forRemoval=true)
RunController @RequestMapping 从 /runs 改为 /legacy/runs
contracts/openapi.yaml 中 /runs 路径标 deprecated 并迁到 /legacy/runs
docs/current 全套文档同步到 Stage1 完成态
docs/codex/plans/_active/STATUS.json 切到 2026-05-25_Stage1_agent_runtime_skeleton
docs/codex/plans/_archive/2026-02-04_M1/ 归档老 M1 mock-provider 计划
ArchitectureTest 新增 4 条规则（domain 独立 / connector.nq 禁字 / usecase.agent 禁 providers / api 禁 order-trade-live 路径）
dh-eval/pom.xml parent 修回 dh-bom
dep-tree.txt 重新生成
```

## 4. Integration-0-PLAN（唯一下一步）

目标：只做 DH -> NQ 未来接入前的只读边界、契约冻结、权限模型和审计模型规划。

Integration-0-PLAN 当前状态：

```text
NQ integration not started
Integration-0 not started / plan only
RealClient forbidden
real provider forbidden
LIVE trading forbidden
NQ mutation forbidden
```

NQ / DH 三轮只读审计（NQ 全仓 / DH 全仓 / NQ-DH 联合边界 + 汇总）已完成。Integration-0 是 contract / mock / documentation 工作线，不是 runtime integration。
DH P1-1 / P1-2 / P1-3 已关闭；P1-4 残留（rate limit / memory cap / replay nonce 持久化）**已于 2026-06-13 CLOSED**（DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE，见 `STATUS.md` §1.3）。P1-4 CLOSED 仅表示 Integration-1 的前置安全缺口关闭，不等于允许真实联调；Integration-1 仍 NOT STARTED，不阻塞 Integration-0。

DH-NQ Integration-0 契约冻结已完成（contract / mock / docs，未实现集成），见
`DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md` / `DH_NQ_INTEGRATION0_SECURITY_POLICY.md` /
`DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md`。下一步只允许 mock / contract test 设计或安全文档固化，
禁止真实联调；真实通道必须等 Integration-1 并先修复 P1-4 残留。

DH-NQ Integration-0 mock / contract test 详细矩阵（15 项）已设计完成（docs-only，未写测试代码）。
下一步可进入 contract test 代码实现（草案 `NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL`，DH 侧只加测试与
fixture、走 Fake/Disabled、不接真实通道），仍禁止真实联调。

DH-NQ Integration-0 contract test 代码已实现并通过 implementation review；**Integration-0 safety gate
CLOSED / ACCEPTED**（见 `DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md`）。下一步只允许 Integration-1
planning-only audit / DH P1-4 residual fix planning / NQ GateK-PLAN 文档规划；禁止直接 Integration-1
实现、真实只读通道、真实 HTTP、RealClient、Provider、LIVE、AI 自动交易。

DH P1-4 residual 三项修复（replay nonce persistence / memory cap / rate limit）已分别实现并 review 通过，
并于 2026-06-13 完成整体回归收口（DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE）：`mvn test` / `mvn -Pquality validate`
BUILD SUCCESS，INT0-T01..T15 16/16 未破坏，既有 HMAC/timestamp/nonce/replay/payload/source/tenant 语义保持，
**P1-4 residual: CLOSED**（见 `STATUS.md` §1.3 / `DH_P1_4_RESIDUAL_FIX_PLAN.md`）。下一步只允许：
① DH-CI-PERSISTENT-NONCE-IT-ENABLE（Docker CI 跑通持久化 nonce IT）或 ② DH-NQ-HEADER-ALIGNMENT-PLAN
（header `X-DH-NQ-*`/`X-NQ-DH-*` 对齐规划）；**不得直接进入 Integration-1 runtime**。Integration-1 仍 NOT STARTED。

范围：

```text
DH -> NQ 只读边界梳理
契约草案
scope token / permission model
audit trail
replay protection
tenant binding
request signing
timestamp / nonce
payload size limit
source allowlist
验收清单
风险清单
```

Integration-0-PLAN 必须明确禁止：

```text
不实现真实 NQ client
不实现 RealClient
不接真实 HTTP / event 到 NQ
不调用 NQ /api/ai/research/backtest-requests
不启动 Paper Run
不修改 NQ 交易状态
不访问交易所密钥
不触碰 LIVE trading
不读取 NQ DB
不写 NQ DB
```

验收：

```text
输出 Integration-0-PLAN 文档
只读边界、契约草案、权限模型、审计模型和 replay protection 规则可审查
明确 payload size limit、source allowlist、tenant binding、request signing、timestamp / nonce
列出 forbidden 能力清单和回滚 / 停止条件
git diff --check 通过
本轮不要求 mvn test，除非后续任务修改业务代码
```

## 5. Stage2-PoC（historical / superseded / deferred）

以下内容是历史规划背景，不是当前 next，不允许作为当前实现任务：

```text
NqFeedbackClient 接通真实 HTTP/事件（deferred / forbidden until Integration-0 approved）
NqBacktestClient 接通真实 NQ /api/ai/research/backtest-requests（deferred / forbidden until Integration-0 approved）
RealNqBacktestClient / RealClient（forbidden）
real provider（forbidden）
真实 HTTP / event 到 NQ（forbidden）
DH 主动发起真实 NQ backtest request（forbidden）
```

后续如需恢复这些方向，必须先通过 Integration-0-PLAN 的安全审查、契约冻结和人工确认。

## 6. Stage3：NQ Console 接入

目标：NQ Console 接入 DH AI 页面。

交付页面（落在 NQ frontend，不在 DH）：

```text
/ai/tasks
/ai/tasks/:id
/ai/candidates
/ai/experiences
/ai/reports
```

验收：NQ Console 统一前端入口，不建设 DH 完整业务前端。

## 7. DH-FREEZE

目标：冻结 DH Agent Decision Layer v1。

验收：

```text
文档快照进入 docs/gates/dh-agent-v1
回归测试通过
边界检查通过
无 AI 直接交易能力
无 NQ 核心污染
```
