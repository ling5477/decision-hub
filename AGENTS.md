# Decision Hub Agent Guidelines

本仓库是 Decision Hub。任何 Agent、Codex、人工改动都必须按本文件执行。

## 1. 项目定位

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层，不是交易执行系统。

DH 负责：

```text
Agent 编排
候选方案生成
多路径探索
历史反馈强化
策略评分
冲突仲裁
报告生成
辅助决策
```

NQ 负责：

```text
交易核心
账户与资产
订单状态机
风控链路
正式回测
模拟盘/实盘执行
审计与复盘
```

## 2. 当前事实源

开工前必须优先读取：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
```

涉及 DH/NQ 集成时，还必须读取：

```text
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_REFACTOR_STAGE1_WORK_ORDER.md
```

`docs/current` 是唯一当前事实源。

`docs/codex` 只保留历史计划与辅助执行区，不得覆盖 `docs/current` 的当前结论。

## 3. 标准工作流

DH 采用与 NQ 一致的阶段化流程：

```text
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

禁止跳过 VERIFY 标记完成。

禁止把临时补丁说明写入根 README。

阶段推进后必须更新：

```text
docs/current/STATUS.md
docs/current/WORKLOG.md
docs/current/TESTING.md
```

## 4. 当前阶段

```text
Current stage: Stage3-B3 DH Backtest Request Adapter IMPL completed
Next stage:    Stage3-B2 NQ Feedback Outbox IMPL, blocked until NQ GateJ-FREEZE or isolated branch approval
Source of truth: docs/current
```

Stage3-B3 已于 2026-05-26 完成：DH 端 backtest adapter 可插拔骨架（dh-usecase service + DTO + Repository / dh-connector Fake + Disabled client / dh-app Stage3NqBacktestWiringConfig 三层 gate / ArchUnit 扩到 12 条）；190 tests 全绿；无真实 HTTP；无 RealNqBacktestClient。下一步进入 Stage3-B2，但 B2 触及 NQ 仓库，必须等待 NQ GateJ-FREEZE 完工或在隔离分支上获得显式批准后才能启动。每个 Batch 都必须保证：不修改 NQ 仓库（B2 启动前）、不接实盘、不自动下单、不绕过 NQ 风控、不重写 NQ 回测核心、不引入 TradingAgents Python、mvn test 全绿。Stage3 规划冻结快照位于 docs/gates/dh-stage3-plan/。

## 5. 硬边界

```text
DH 不迁入 NQ
DH 不直接下单
DH 不绕过 NQ 风控
DH 不替代 NQ 订单状态机
DH 不重写 NQ 回测核心
DH 不建设完整第二套前端
DH 不成为交易事实源
```

## 6. 允许改动范围

DH-REFIT-1-WO 允许改：

```text
dh-domain
dh-usecase
dh-memory
dh-eval
dh-connector
dh-api
dh-app
dh-infra
docs/current
contracts
golden_cases
```

当前阶段不允许改：

```text
NQ 仓库
实盘执行链路
订单状态机
风控核心
正式回测核心
NQ Console 正式页面
```

## 7. 构建与验证

最低验证：

```bash
mvn test
```

质量检查：

```bash
mvn -Pquality validate
```

应用启动：

```bash
mvn -pl dh-app -am spring-boot:run
```

验证结果必须写入：

```text
docs/current/TESTING.md
```

实现记录必须写入：

```text
docs/current/WORKLOG.md
```

## 8. 代码与命名规范

```text
Java 21
Spring Boot 3.5.x
包名前缀 com.guidinglight.decisionhub
测试类以 *Test 结尾
Flyway 迁移命名 V{版本}__{描述}.sql
```

public/protected 的类、接口、枚举、字段、方法必须有清晰注释。

关键 private 方法如果承载业务规则，也必须注释。

变量、方法、类、枚举命名必须贴近业务原意、可读、可搜索。

## 9. Agent 输出要求

```text
所有 Agent 输出必须结构化
所有关键对象必须有 traceId
所有 NQ feedback 必须保存原始 payload
最终策略建议必须经过 JudgeDecision
```

禁止单个 Agent 直接输出最终交易决策。

## 10. 安全与配置

禁止提交密钥。

敏感值必须通过环境变量或安全配置注入。

所有外部输入必须校验，避免 SQL、路径、命令注入。
