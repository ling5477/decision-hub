# Decision Hub Current Docs

> Current stage: Stage3-PLAN-FREEZE completed
> Next stage:    Stage3-B2 NQ Feedback Outbox IMPL
> Source of truth: docs/current

## 1. 当前定位

Decision Hub 是 NexusQuant 的 AI Agent 决策能力层，不是交易核心系统。

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

## 2. 当前文档入口

必须优先读取：

```text
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_REFACTOR_STAGE1_STATUS.md
docs/current/STAGE1_CLOSE_WORKLOG.md
```

## 3. 当前工作流

DH 后续采用与 NQ 一致的 Gate/Stage 流程：

```text
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

当前已完成：

```text
DH-REFIT-1-PLAN（文档结构对齐）
Stage1 (Boundary Freeze + Agent Runtime Skeleton)
Stage1-CLOSE（旧链路 @Deprecated + 文档单源 + ArchUnit 兜底）
Stage1-FREEZE（docs/current 快照冻结到 docs/gates/dh-stage1/）
Stage2-PoC-B1 / B2 / B3 / B4 / B5 IMPLEMENT
Stage2-PoC VERIFY（2026-05-26，BUILD SUCCESS / 122 tests / ArchUnit 10/10；Verdict: GO）
Stage2-PoC FREEZE（2026-05-26，docs/current 快照冻结到 docs/gates/dh-stage2-poc/）
Stage3-PLAN（2026-05-26，新增 6 份 STAGE3_*.md 规划文档：PLAN / NQ→DH feedback /
             DH→NQ backtest / Contract / Test / Work Order）
Stage3-WO（2026-05-26，重写 STAGE3_WORK_ORDER.md 为 4 Batch 可直接开工工单；
           新增 STAGE3_BATCH_PLAN.md 边界对照表）
Stage3-B1 Contract Alignment IMPLEMENT（2026-05-26，DH 仓库内对齐 16 份 JSON Schema、
           openapi.yaml 与 dh-domain 枚举/字段；新增 4 份 contract 测试类共 29 cases；
           mvn test 151 全绿 / ArchUnit 10/10；零 NQ 仓库改动）
Stage3-B2 NQ Feedback Outbox PLAN（2026-05-26，DH 仓库内落 docs/current/STAGE3_NQ_OUTBOX_SPEC.md：
           NQ 侧模块清单 / 表结构（nq_ai_feedback_outbox + 死信表 + COMMENT + 索引）/
           8 触发点 / 5 状态机 + 8 attempt 退避矩阵 / audit / 5 字段语义 / HTTP 矩阵 /
           NQ 后续 5 个 Batch（NQ-1..NQ-5）/ GateJ-FREEZE 防护 / schema 演进；
           零 Java 业务代码改动；零 NQ 仓库改动；mvn test 仍 151 全绿）
Stage3-B3 DH Backtest Request Adapter PLAN（2026-05-26，DH 仓库内落
           docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md（14 段完整规格）：
           可插拔原则 10 条 + 三层 gate + 三 client 策略（Fake / Disabled / Real）+
           9 状态机 + 错误码映射 + 24h 幂等 + 退避矩阵 + 三段配置建议 + 8 个测试类规划 +
           B3-1..B3-5 五批 IMPL 拆解 + 与 NQ outbox / contracts 衔接；
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration 修改；
           mvn test 仍 151 全绿 / ArchUnit 10/10）
Stage3-B4 End-to-End Contract Test PLAN（2026-05-26，DH 仓库内落
           docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md（11 段完整规格）：
           DH staging + NQ test cluster 测试环境规划 / 网络隔离强约束 /
           7 个端到端联调用例 T1-T7（PAPER_RUN_CREATED / ALERT 幂等 /
           BACKTEST_RESULT_READY 消费 / backtest accepted / disabled mode /
           outbox retry+dead-letter / 安全边界）/ 10 类 Contract Test 类型 /
           5 字段端到端对账 + deterministic 数据 / DH+NQ+联调三段验收命令 /
           失败处理矩阵 + 联调回滚预案 / B4-1..B4-5 五批 IMPL 拆解 /
           Stage3-PLAN-FREEZE 衔接；
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration 修改；
           mvn test 仍 151 全绿 / ArchUnit 10/10）
Stage3-PLAN-FREEZE（2026-05-26，Stage3 规划成果落盘冻结）：
           - 一致性核查 10 份 STAGE3_*.md（9 条核心原则口径一致；无修订）
           - docs/current/* 33 个文件完整复制到 docs/gates/dh-stage3-plan/
           - docs/gates/dh-stage3-plan/README.md 顶部加冻结声明 + 一致性核查表 +
             Stage3-PLAN 交付物清单
           - 6 份状态文档同步到 "Stage3-PLAN-FREEZE completed / Next: Stage3-B1 IMPLEMENT"
           - 零 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration / OpenAPI 修改；
             mvn test 仍 151 全绿 / ArchUnit 10/10
```

下一步只允许进入：

```text
Stage3-B2 NQ Feedback Outbox IMPL：
  - Stage3-B1 Contract Alignment IMPLEMENT 已于 2026-05-26 完成，不要重复开工
  - B2 触及 NQ 仓库；必须等待 NQ GateJ-FREEZE 完工或在隔离分支上获得显式批准后才能启动
  - NQ 团队按 STAGE3_NQ_OUTBOX_SPEC §8（NQ-1..NQ-5）实施；DH 仓库不动
  - 若 NQ 端 B2 工作被阻塞，可优先推进 DH 端 Stage3-B3 DH Backtest Request Adapter IMPL
    （按 STAGE3_DH_BACKTEST_ADAPTER_SPEC §12 在 fake / disabled 模式下落地 B3-1..B3-5；
     不接真实 NQ；不真实联调；fake-mode=true 默认装配 FakeNqBacktestClient）

严格禁止：
  修改 NQ 仓库（B2 启动前）/ 接实盘 / 自动下单 / 绕风控 / 重写 NQ 回测核心 /
  引入 TradingAgents Python / 接真实 Kronos / 接真实 global-stock-data
```

Stage3 规划冻结快照（不得修改）：

```text
docs/gates/dh-stage3-plan/
```

## 4. 当前不允许做

```text
不迁移 DH 到 NQ
不修改 NQ 交易核心
不直接下单
不绕过 NQ 风控
不替代 NQ 订单状态机
不重写 NQ 回测核心
不建设完整第二套前端
不引入重型群体智能数学优化器
```

## 5. 文档目录规则

```text
docs/current/      当前事实源
docs/gates/        历史冻结快照
docs/codex/        当前活跃计划 + 历史归档
contracts/         外部协议、OpenAPI、JSON Schema、事件协议
golden_cases/      回归用例与黄金样例
```

## 6. 冻结规则

每个阶段完成后，必须复制 `docs/current` 到：

```text
docs/gates/<stage-id>/
```

冻结后不得直接修改历史快照。
