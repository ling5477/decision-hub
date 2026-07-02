# Decision Hub Architecture

## 1. 定位

Decision Hub 是 AI Agent 决策能力层，不是交易执行系统。

目标：

```text
候选生成 -> 多路径探索 -> 反馈强化 -> 组织仲裁 -> 结构化决策输出 -> 复盘进化
```

## 2. 总体架构

```text
NQ Console
  -> DH API
      -> Agent Orchestrator
      -> Candidate Search
      -> Evaluation / Judge
      -> Experience Memory
      -> NQ Adapter
  -> NQ API
      -> Backtest
      -> Risk
      -> Paper Trading
      -> Publish
      -> Audit / Review
```

## 3. 后端模块边界

```text
dh-domain         领域模型、状态、决策对象
dh-usecase        应用用例、任务编排、决策流程
dh-providers      模型供应商与模型路由
dh-memory         记忆、经验库、轻量信息素分数
dh-knowledge      知识库、资料检索、上下文构建
dh-eval           候选评分、证据质量评分、Judge 聚合
dh-connector      外部系统连接器，包含 NQ Adapter
dh-api            REST API
dh-app            Spring Boot 入口
dh-infra          持久化、Repository、JDBC 实现
dh-security       认证鉴权
dh-observability  日志、指标、trace
dh-sdk            对外 SDK / client 契约
```

## 4. 群体智能落点

### 蜂群机制

用于候选生成和并行探索。

```text
多个 Agent / 多模型 / 多数据源 / 多参数空间 并行生成候选
候选进入过滤、排序、淘汰
```

### 蚁群机制

用于历史反馈强化。

```text
NQ 回测、风控、模拟盘、复盘结果回流 DH
更新 ExperienceEntry 与 PheromoneEdge
后续任务优先参考高分经验路径
```

### 狼群机制

用于组织调度和仲裁。

```text
Leader 规划任务
Scout/Analyst/Strategy 生成证据和候选
RiskReviewer/StrategyReviewer 审查
Judge 统一裁决
```

## 5. 硬边界

```text
DH 不直接下单
DH 不绕过 NQ 风控
DH 不复制 NQ 订单状态机
DH 不重写 NQ 回测核心
DH 不成为交易事实源
```
