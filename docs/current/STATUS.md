# Decision Hub Status

> Current stage: DH-REFIT-1-PLAN completed
> Next stage: DH-REFIT-1-WO
> AI trading execution: not allowed
> NQ core changes: not allowed in this stage

## 1. 当前结论

DH 文档结构开始切换为与 NQ 一致的工作流：

```text
docs/current 作为当前事实源
docs/gates   作为冻结快照
docs/codex   作为历史计划与辅助执行区
```

## 2. 当前已完成

```text
DH_NQ_INTEGRATION.md 已建立 DH/NQ 边界
aDH_REFACTOR_STAGE1_WORK_ORDER.md 已建立 Stage1 代码重构工单
docs/current/README.md 已建立当前入口
docs/README.md 已改为 NQ 风格文档入口
```

## 3. 当前阶段边界

DH-REFIT-1-PLAN 只做文档结构、边界、计划和工作流统一。

本阶段不做：

```text
不写业务代码
不改 NQ 仓库
不做真实模型调用
不接实盘交易
不迁移 DH 到 NQ
不建设第二套完整前端
```

## 4. 下一阶段

下一阶段为 DH-REFIT-1-WO。

范围：

```text
Agent Runtime Skeleton
ResearchRun 最小状态机
StrategyCandidate 结构化候选
JudgeDecision 仲裁结果
ExperienceEntry / PheromoneEdge 轻量反馈
NQ Adapter fake implementation
最小 API 闭环
mvn test 验收
```

## 5. 当前风险

```text
旧 docs/codex 流程仍存在，后续只作为历史辅助区
根 README 原本是历史补丁说明，需要改为项目入口
AGENTS.md 需要同步当前事实源规则
历史 docs 尚未形成 gates 冻结快照
```
