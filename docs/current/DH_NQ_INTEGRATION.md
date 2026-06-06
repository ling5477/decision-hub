# DH 与 NexusQuant 集成边界

> Status: Stage1 Boundary Freeze
> Owner: Decision Hub / NexusQuant Integration
> Created: 2026-05-18

## 0. 当前状态锁定（2026-06-06）

```text
Current stage: Stage3-B3 DH Backtest Request Adapter IMPL completed
Next stage:    Integration-0-PLAN
DH-AUDIT-FIX completed
NQ integration not started
Integration-0 not started / plan only
RealClient forbidden
real provider forbidden
LIVE trading forbidden
NQ mutation forbidden
```

当前唯一允许阶段是 `Integration-0-PLAN`。本文件中的 DH -> NQ REST API 控制面、`POST /api/ai/backtest-requests`、真实 HTTP / event、NQ client、RealClient、real provider 等方向均为 historical / superseded / deferred / gated；它们不是当前 next，不是当前 implementation，不允许作为当前开发任务。任何 NQ 相关实现必须先通过 Integration-0-PLAN 的安全审查、契约冻结和人工确认。

Integration-0-PLAN 只能做：

```text
DH -> NQ 只读边界
契约草案
scope token / permission model
audit trail
replay protection
tenant binding
request signing
timestamp / nonce
payload size limit
source allowlist
risk checklist
acceptance checklist
```

Integration-0-PLAN 明确禁止：

```text
不调用 NQ /api/ai/backtest-requests
不接真实 DH -> NQ REST
不接真实 event
不实现 NQ client
不实现 RealClient
不接 real provider
不启动 Paper Run
不读写 NQ DB
不访问交易所密钥
不修改 NQ 交易状态
不触碰 LIVE trading
```

## 1. 结论

DH 不迁入 NQ。

DH 保持独立服务，定位为 NQ 上方的 AI Agent 决策能力层。NQ 保持独立交易平台，继续作为唯一交易事实源、唯一风控闸门、唯一订单状态机、唯一正式回测与执行内核。

前端不维护两套完整业务前端。正式入口统一放在 NQ Console，DH 只提供后端能力和必要的 admin/debug 能力。

## 2. 职责边界

### 2.1 DH 负责

- AI Agent 编排
- 任务拆解与研究流程组织
- 策略候选生成
- 多路径候选探索
- 证据收集与解释
- 候选方案评分
- 风险建议生成
- 冲突仲裁
- 决策报告生成
- 经验库与失败案例库沉淀
- 接收 NQ 回流结果并做轻量反馈强化

### 2.2 NQ 负责

- 账户、资产、仓位
- 订单状态机
- 风控链路
- 正式回测核心
- 模拟盘与实盘执行
- 成交、账本、审计
- 恢复、对账、复盘事实
- 策略发布与运行控制

## 3. 硬约束

1. DH 不直接下单。
2. DH 不绕过 NQ 风控。
3. DH 不替代 NQ 订单状态机。
4. DH 不重写 NQ 回测核心。
5. DH 不直接修改 NQ 的账户、订单、仓位、账本事实。
6. DH 只输出结构化建议、候选、报告、评审意见和任务请求。
7. NQ 的回测、风控、模拟盘、实盘、复盘结果必须回流 DH，作为经验强化输入。

## 4. 集成方式（historical / superseded / deferred / gated）

本节保留早期边界冻结阶段的未来集成方向，仅作为历史背景和后续 Integration-0-PLAN 的风险输入，不代表当前 next 或当前 implementation。当前不得据此实现真实 REST、真实 event、NQ client、RealClient 或 real provider。

早期规划采用双通道：

```text
控制面：DH -> NQ REST API（deferred / gated）
事实面：NQ -> DH Feedback Event（deferred / gated）
```

### 4.1 DH -> NQ 控制面（deferred / gated）

以下接口方向只作为历史规划背景。当前不允许调用这些接口，不允许接真实 DH -> NQ REST，不允许实现 NQ client / RealClient，也不允许把它们作为当前开发任务。

历史最小接口方向：

```text
POST /api/ai/backtest-requests
POST /api/ai/paper-trial-requests
POST /api/ai/release-review-requests
GET  /api/ai/jobs/{jobId}
```

这些接口只表示“请求 NQ 执行正式能力”，不是交易指令。

### 4.2 NQ -> DH 事实回流（deferred / gated）

以下事件方向只作为历史规划背景。当前不允许接真实 event，不允许实现真实 dispatcher / real provider，也不允许把它们作为当前开发任务。

历史最小事件方向：

```text
BacktestCompleted
BacktestRejected
RiskRejected
PaperTrialCompleted
ReleaseApproved
ReleaseRejected
TradeReviewCompleted
PostMortemCreated
```

这些事件进入 DH 的 ExperienceEntry / PheromoneEdge，用于后续任务排序和经验强化。

## 5. 数据流（historical / superseded / deferred / gated）

以下数据流只保留为历史背景，不代表当前系统已经开始集成，也不是当前 implementation。当前唯一下一步仍是 Integration-0-PLAN。

```text
User / NQ Console
  -> DH 创建 ResearchRun
  -> DH Leader 规划任务
  -> DH 多 Agent 生成 StrategyCandidate
  -> DH Judge 冻结候选
  -> DH 调用 NQ 正式回测（deferred / gated；当前禁止）
  -> NQ 返回 BacktestCompleted / RiskRejected
  -> DH 生成 DecisionRecommendation
  -> 人工审批
  -> NQ 继续 paper/release/live 链路
  -> NQ 回流事实结果
  -> DH 更新经验库
```

## 6. 前端入口

正式业务入口放在 NQ Console：

```text
/ai/tasks
/ai/tasks/:id
/ai/candidates
/ai/experiences
/ai/reports
/ai/reviews
```

DH 不建设完整业务前端，只保留：

```text
/admin/debug
/admin/provider-health
/admin/workflow-trace
```

## 7. 第一阶段范围

第一阶段只做边界冻结与运行时骨架，不接入真实交易，不接入实盘，不修改 NQ 核心。

第一阶段产物：

- ResearchRun 状态机定义
- TaskNode 流程节点定义
- StrategyCandidate 结构化候选定义
- JudgeDecision 结构化仲裁定义
- NQ Adapter 接口骨架
- Feedback Event 接口骨架
- 任务回放与审计字段

## 8. 第一阶段不做

- 不做 AI 下单
- 不做自动发布策略
- 不做实盘执行
- 不做重型 ACO / ABC / GWO 数学优化器
- 不做完整第二套前端
- 不做 NQ 回测核心重写
- 不做 NQ 风控逻辑迁移
