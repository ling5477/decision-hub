# Decision Hub Current Work Order

> Current stage: DH-REFIT-1-PLAN completed
> Next stage: DH-REFIT-1-WO

## 1. 当前目标

重构 DH 文档结构，使其与 NQ 的工作流保持一致。

## 2. 本轮范围

```text
更新根 README.md
更新 AGENTS.md
建立 docs/current 当前事实源
建立 docs/gates 冻结快照入口
保留 docs/codex 作为历史辅助区
同步 STATUS / ROADMAP / WORKFLOW / WORKLOG / TESTING / API / DB_SCHEMA / ARCHITECTURE
```

## 3. 不做事项

```text
不写业务代码
不改 NQ 仓库
不做真实 Agent Runtime 实现
不接真实模型
不接真实交易
不动 NQ 交易核心
```

## 4. 下一轮 Codex 开工提示词

```text
你在 ling5477/decision-hub 仓库工作。

任务：执行 DH-REFIT-1-WO，按照 docs/current 的工作流完成 DH Agent Runtime Skeleton 第一批代码实现。

开始前必须读取：
- README.md
- AGENTS.md
- docs/current/README.md
- docs/current/STATUS.md
- docs/current/ROADMAP.md
- docs/current/WORKFLOW.md
- docs/current/DH_NQ_INTEGRATION.md
- docs/current/DH_REFACTOR_STAGE1_WORK_ORDER.md

严格边界：
- 不修改 NQ 仓库。
- 不实现真实下单。
- 不绕过 NQ 风控。
- 不重写 NQ 回测核心。
- 不建设第二套完整前端。
- 不引入重型群体智能数学优化器。

实现目标：
1. 定义 ResearchRun / AgentTask / TaskNode / StrategyCandidate / JudgeDecision / ExperienceEntry / PheromoneEdge / NqFeedbackEvent。
2. 定义最小 usecase 接口与 fake implementation。
3. 定义 NQ Adapter 接口与 fake implementation。
4. 增加最小 REST API。
5. 增加测试，验证创建 run -> 启动 -> 生成候选 -> 仲裁 -> 接收 feedback -> 更新经验 的最小闭环。
6. 执行 mvn test 并更新 docs/current/TESTING.md 与 docs/current/WORKLOG.md。
```
