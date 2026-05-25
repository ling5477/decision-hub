# Decision Hub

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层。

它负责 Agent 编排、候选方案生成、多路径探索、历史反馈强化、策略评分、冲突仲裁、报告生成和辅助决策。

它不负责交易核心、订单状态机、风控执行、正式回测内核、模拟盘/实盘执行、账本审计和交易事实沉淀。这些能力由 NexusQuant 承担。

## 当前阶段

```text
Current stage: Stage2-PoC-B3 IMPLEMENT completed
Next stage:    Stage2-PoC-B4 IMPLEMENT
Source of truth: docs/current
```

## 文档入口

当前事实源：

```text
docs/current/README.md
```

开工前必须读取：

```text
AGENTS.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_REFACTOR_STAGE1_STATUS.md
docs/current/STAGE1_CLOSE_WORKLOG.md
```

## 文档结构

```text
docs/current/      当前事实源
docs/gates/        历史阶段快照（Stage1 冻结后归档）
docs/codex/        当前活跃计划（plans/_active/） + 历史归档（plans/_archive/）
contracts/         对外协议、Schema、事件契约
golden_cases/      黄金样例与回归用例
```

## 当前工程边界

```text
DH 不迁入 NQ
DH 不直接下单
DH 不绕过 NQ 风控
DH 不替代 NQ 订单状态机
DH 不重写 NQ 回测核心
DH 不建设完整第二套前端
```

## 标准工作流

```text
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

当前下一步只能进入：

```text
Stage2-PoC-B3 IMPLEMENT：dh-connector Forecast / Research Adapter 接口预留 + Fake 实现
```

## 构建与验证

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

质量检查：

```bash
mvn -Pquality validate
```

应用启动：

```bash
mvn -pl dh-app -am spring-boot:run
```

## DH 与 NQ 的关系

```text
NQ Console -> DH API -> Agent Runtime / Judge / Memory / NQ Adapter
NQ Console -> NQ API -> Backtest / Risk / Paper / Live / Audit
NQ -> DH Feedback -> Experience / Pheromone
```

DH 输出结构化建议和报告，NQ 执行正式交易能力。
