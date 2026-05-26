# Decision Hub Current Docs

> Current stage: Stage3-B3 DH Backtest Request Adapter IMPL completed
> Next stage:    Stage3-B2 NQ Feedback Outbox IMPL, blocked until NQ GateJ-FREEZE or isolated branch approval
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
Stage3-NEXT-STATUS-FIX（2026-05-26，修正 PLAN-FREEZE 后 Next 指向）：
           - Stage3-B1 已完成（2026-05-26），Next 不应再指向 B1
           - 修正为 Stage3-B2 NQ Feedback Outbox IMPL；blocked until NQ GateJ-FREEZE
           - STATUS.md §4 新增 3 条执行口径
Stage3-B3 DH Backtest Request Adapter IMPL（2026-05-26，DH 端可插拔骨架落地）：
           - dh-usecase 新增 backtest 包：DhBacktestRequestService 端口 + Default 实现 +
             Command/Result/Outcome/ErrorCode + Repository 端口 + InMemory 实现
             （24h paramsHash 幂等短路 + 9 状态机 + 错误码映射 + 不抛 RuntimeException 中断 caller）
           - dh-connector 扩展 NqBacktestClient typed submit (DhBacktestRequest)；
             新增 NqBacktestSubmitResult / NqBacktestSubmitStatus（4 状态）；
             新增 DisabledNqBacktestClient（DH gate 关闭时返回 DISABLED 不抛异常）；
             FakeNqBacktestClient 扩展 deterministic typed submit（jobId = sha256(requestId).take(16)）
           - dh-app 新增 NqBacktestClientProperties (@ConfigurationProperties) +
             Stage3NqBacktestWiringConfig（互斥 SpEL 三层 gate；默认 Fake 兜底；
             stage3.nq.enabled=true && backtest-request.enabled=false → Disabled；
             fake-mode=false 仍走 Fake 兜底，无 RealNqBacktestClient）
           - 修改 AgentRuntimeWiringConfig：移除 nqBacktestClient bean（由 Stage3 装配接管），
             避免多 config @ConditionalOnMissingBean 评估冲突
           - dh-app ArchUnit 扩到 12 条：新增 R11 HTTP 客户端仅允许 connector.nq / config /
             R12 usecase.agent.backtest 不依赖 RealClient 或 providers
           - 8 个 B3 测试类（dh-connector 2 + dh-usecase 3 + dh-app 2 + dh-domain 1）共 39 cases 全绿
           - mvn test BUILD SUCCESS / 190 tests（151 → 190，+39）/ 0 failures / 0 errors / 0 skipped
           - 零 NQ 仓库改动；零真实 HTTP；零 contracts/openapi.yaml 修改；
             零 contracts/json-schema 修改；零 Flyway migration 新增；零 OpenAPI path 新增；
             零下单 / 绕风控 / 重写回测核心；零 TradingAgents Python / Kronos / global-stock-data
```

下一步只允许进入：

```text
Stage3-B2 NQ Feedback Outbox IMPL（blocked until NQ GateJ-FREEZE 或隔离分支批准）。

执行口径：
  - Stage3-B3 DH Backtest Request Adapter IMPL 已于 2026-05-26 完成：
    * dh-usecase 新增 backtest 包（DhBacktestRequestService + Default + Command + Result +
      Outcome + ErrorCode + Repository + InMemory），共 9 个生产类；
    * dh-connector 扩展 NqBacktestClient typed submit + 新增 DisabledNqBacktestClient +
      NqBacktestSubmitResult/Status，共 4 个生产类；
    * dh-app 新增 NqBacktestClientProperties + Stage3NqBacktestWiringConfig（三层 gate
      互斥 SpEL 条件，默认 Fake 兜底；fake-mode=false 仍走 Fake 兜底，无 RealClient）；
    * dh-app ArchUnit 扩到 12 条（新增 R11 HTTP 客户端隔离 + R12 backtest 端口隔离）；
    * 8 个 B3 测试类共 39 cases 全绿；190 tests 全绿；
    * 零真实 HTTP；零 NQ 仓库改动；零下单 / 风控旁路 / 实盘 / 前端。
  - Stage3-B2 是 NQ 仓库工作（按 STAGE3_NQ_OUTBOX_SPEC §8 / NQ-1..NQ-5）；
    NQ GateJ-FREEZE 未完工前 B2 不允许启动；即便有隔离分支启动也必须遵守 STAGE3_NQ_OUTBOX_SPEC
    §1.3 / §9 全部硬边界（不影响 GateJ-FREEZE / 不进入交易同步链路 / 不阻塞订单/风控/账本/回测）。
  - 后续 Stage3-B4 联调（按 STAGE3_E2E_CONTRACT_TEST_SPEC §8 / B4-1..B4-5）+ Stage3-VERIFY/FREEZE
    按 STAGE3_WORK_ORDER 推进。

严格禁止：
  修改 NQ 仓库（B2 启动前）/ 接实盘 / 自动下单 / 绕风控 / 重写 NQ 回测核心 /
  引入 TradingAgents Python / 接真实 Kronos / 接真实 global-stock-data。
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
