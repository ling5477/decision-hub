# Stage3-PLAN Gate — Frozen Snapshot

> Stage: Stage3-PLAN
> Status: FREEZE completed
> Source snapshot: docs/current at Stage3-B4 End-to-End Contract Test PLAN completed
> Verification: mvn test passed, 151 tests / 0 failures / 0 errors / 0 skipped
> ArchUnit: 10/10 passed
> Scope: planning only, no Java implementation, no NQ repository changes
> Next: Stage3-B1 IMPLEMENT

## 冻结声明

本目录是 Stage3-PLAN 完成后的 `docs/current/` 快照，冻结于 2026-05-26。

冻结范围限于"Stage3 规划文档"。本快照**不代表**真实联调已完成；不代表 Stage3 任何
IMPLEMENT 工作已完成；不代表 NQ 仓库任何工作已完成。

本快照只意味着：
- Stage3 全部 10 份 PLAN 文档（STAGE3_*.md）落盘并互相口径一致；
- 截止 2026-05-26 23:59，DH 仓库 dev 分支上 Stage3 规划阶段验收通过；
- 后续任何 Stage3 推进（IMPLEMENT / VERIFY / FREEZE）必须在新工单中单独开工。

冻结后**不得修改本目录内容**。后续变更只能发生在 `docs/current/`。

### Stage3-B1 IMPLEMENT 已先行完成（特别说明）

Stage3-B1 Contract Alignment IMPLEMENT 已于 2026-05-26 完成（独立工单）；
其 IMPLEMENT 结果（4 份 contract 测试类 / 29 cases / 151 tests 基线）已落入 dh-domain
源码与本快照 docs/current/TESTING.md §10 验收记录。Stage3-PLAN-FREEZE 不再单独跑
Stage3-B1 IMPLEMENT，但其 PLAN 阶段（即 STAGE3_WORK_ORDER §1 与 STAGE3_CONTRACT_PLAN）
仍在本冻结范围。

### Stage3 后续不允许做的事

```text
不修改 NQ 仓库                           不接真实 NQ API
不接真实 Kronos                          不接真实 global-stock-data
不引入 TradingAgents Python              不实现真实下单
不绕过 NQ 风控                           不修改 NQ 订单状态机
不重写 NQ 回测核心                       不建设第二套完整前端
不接实盘                                 不自动发布策略
不让 DH 标记正式回测成功                 不让 DH 覆盖 NQ verdict
不让 NQ 强依赖 DH 才能启动                不让 DH 强依赖 NQ 才能完成 ResearchRun
不在联调使用 prod tenant / prod URL       不在联调使用真实账户 / 真实凭证
联调失败禁止"硬抹平"通过                  IMPLEMENT 必须逐 Batch 执行
```

### Stage3 后续允许进入的工单

```text
Stage3-B1 Contract Alignment IMPLEMENT             ✅ 已完成 (2026-05-26)
Stage3-B2 NQ Feedback Outbox IMPL                   NQ 仓库后续实施（按 STAGE3_NQ_OUTBOX_SPEC §8）
Stage3-B3 DH Backtest Request Adapter IMPL          DH 仓库后续实施（按 STAGE3_DH_BACKTEST_ADAPTER_SPEC §12）
Stage3-B4 End-to-End Contract Test IMPL/VERIFY      DH+NQ 联调（按 STAGE3_E2E_CONTRACT_TEST_SPEC §8）
Stage3-FREEZE                                       B4 VERIFY GO 后；拷贝 docs/current 到 docs/gates/dh-stage3/
DH-FREEZE                                           Stage3-FREEZE 后；Decision Hub Agent Decision Layer v1 长期维护态
```

## 验收命令与结果

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
BUILD SUCCESS
Total: 151 tests / 0 failures / 0 errors / 0 skipped
ArchUnit: 10/10 PASS
```

按模块分布（与 Stage2-PoC FREEZE 基线的 122 tests 相比，Stage3-B1 IMPLEMENT 落地后新增 29 tests）：

```text
dh-domain    64 tests   (Stage2 35 + Stage3-B1 contract 29)
              - JsonSchemaPresenceTest                                  5
              - NqFeedbackEnvelopeSchemaContractTest                    7  (Stage3-B1)
              - DhBacktestRequestSchemaContractTest                     7  (Stage3-B1)
              - BacktestResultSnapshotSchemaContractTest                6  (Stage3-B1)
              - OpenApiContractAlignmentTest                            9  (Stage3-B1)
              - 其它 Stage2-PoC-B1 domain 用例
dh-connector  9 tests   (Stage2-PoC-B3 fake adapter/store)
dh-usecase   47 tests   (Stage1 + Stage2-PoC-B2/B4/B5 + Stage2ClosedLoopTest / ResearchRunStage1ClosedLoopTest)
dh-infra      9 tests   (JdbcSqlFragments 5 / JdbcNqFeedback 4)
dh-api        7 tests   (NqFeedbackControllerWebMvcTest)
dh-app       15 tests   (ArchitectureTest 10 / V3MigrationPresenceTest 5)
```

## 冻结快照文件清单

```text
docs/gates/dh-stage3-plan/
├── README.md                                       本文件（冻结声明）
├── STATUS.md                                       Stage3-B4 PLAN completed / Next: Stage3-PLAN-FREEZE
├── ROADMAP.md / WORKFLOW.md / API.md / DB_SCHEMA.md / TESTING.md / WORKLOG.md
├── DH_NQ_INTEGRATION.md
├── DH_REFACTOR_STAGE1_*.md
├── STAGE1_CLOSE_WORKLOG.md
├── STAGE2_POC_*.md                                 Stage2-PoC PLAN/WO/VERIFY_REPORT 等
├── STAGE3_PLAN.md                                  Stage3 主索引
├── STAGE3_WORK_ORDER.md                            4 Batch 工单
├── STAGE3_BATCH_PLAN.md                            Batch 边界对照表
├── STAGE3_CONTRACT_PLAN.md                         端到端契约规则
├── STAGE3_TEST_PLAN.md                             测试策略
├── STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md                NQ → DH 出站事件链路
├── STAGE3_DH_TO_NQ_BACKTEST_PLAN.md                DH → NQ 入站请求链路
├── STAGE3_NQ_OUTBOX_SPEC.md                        Stage3-B2 NQ outbox 11 段规格
├── STAGE3_DH_BACKTEST_ADAPTER_SPEC.md              Stage3-B3 DH adapter 14 段规格
└── STAGE3_E2E_CONTRACT_TEST_SPEC.md                Stage3-B4 E2E contract test 11 段规格
```

## 一致性核查（10 份 STAGE3_*.md）

```text
原则                                                覆盖情况
NQ 与 DH 对接可插拔、默认关闭、非强依赖              ✅ B3 SPEC §2 / B4 SPEC §1.2 / WO §3 / NQ_OUTBOX_SPEC §1
NQ without DH 完整可运行                            ✅ B3 SPEC §2.1 / B4 SPEC §3.7-C / NQ_OUTBOX_SPEC §1.2
DH without NQ 走 fake / disabled 闭环               ✅ B3 SPEC §2.1 / B4 SPEC §3.5 / §3.7-D
NQ → DH feedback 走旁路 outbox，不阻塞交易主链路    ✅ NQ_OUTBOX_SPEC §1.3 / §9 / §7.3
DH → NQ backtest request 默认 disabled / fake       ✅ B3 SPEC §2 / §10.3 / B4 SPEC §3.5
NQ 仍然是唯一正式回测执行方                          ✅ B3 SPEC §1.1 / §6.4 / B4 SPEC §1.1 / DH_NQ_INTEGRATION §2/3
DH 不直接下单 / 不绕风控 / 不改订单状态 / 不重写回测核心 ✅ 全部 10 份均含；AGENTS §1/5
Stage3 只规划真实联调，不接实盘                      ✅ B4 SPEC §1.2 / §2.4 / 所有 PLAN 文档硬边界
后续 IMPLEMENT 必须逐 Batch 执行                     ✅ WO / BATCH_PLAN / B2 SPEC §8 / B3 SPEC §12 / B4 SPEC §8
```

无口径不一致；本次 FREEZE 未修改任一份 STAGE3_*.md（仅核查 + 冻结落盘）。

## Stage3-PLAN 交付物

```text
Stage3-PLAN 阶段（2026-05-26）：
  - 主索引 STAGE3_PLAN.md
  - 工单与拆批 STAGE3_WORK_ORDER.md + STAGE3_BATCH_PLAN.md
  - 端到端契约规则 STAGE3_CONTRACT_PLAN.md
  - 测试策略 STAGE3_TEST_PLAN.md
  - 入站 / 出站事件链路 STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md / STAGE3_DH_TO_NQ_BACKTEST_PLAN.md

Stage3-B1 IMPLEMENT 阶段（2026-05-26）：
  - 4 份 contract 测试类落地（dh-domain）；29 cases 全绿
  - contracts/json-schema 3 份 schema 描述补齐（required / enum 维持不变）
  - contracts/openapi.yaml info.description 加 Stage3-B1 硬边界声明（path / response 不变）

Stage3-B2 PLAN 阶段（2026-05-26）：
  - STAGE3_NQ_OUTBOX_SPEC.md 11 段（NQ 端 outbox 表 / 8 触发点 / 5 状态机 / 退避矩阵 /
    audit / NQ 后续 5 Batch / GateJ-FREEZE 防护）

Stage3-B3 PLAN 阶段（2026-05-26）：
  - STAGE3_DH_BACKTEST_ADAPTER_SPEC.md 14 段（可插拔 10 原则 / 三层 gate / Fake-Disabled-Real
    三 client / 9 状态机 / 24h 幂等 / B3-1..B3-5 五批 IMPL）

Stage3-B4 PLAN 阶段（2026-05-26）：
  - STAGE3_E2E_CONTRACT_TEST_SPEC.md 11 段（DH staging + NQ test cluster 环境 /
    T1-T7 联调用例 / 10 类 Contract Test / 5 字段对账 / 失败矩阵 /
    B4-1..B4-5 五批 IMPL / Stage3-PLAN-FREEZE 衔接）

零 Java 业务代码改动（除 Stage3-B1 IMPLEMENT 已落 contract test 类，纯测试代码）；
零 NQ 仓库改动；零 contracts/openapi.yaml path 新增；零 Flyway migration 新增；
零真实 HTTP / 真实联调 / 实盘 / Kronos / global-stock-data / TradingAgents Python 接入。
```

---

以下为冻结时的 docs/current/README.md 原始内容（供历史比对，**不得**修改）。

---

# Decision Hub Current Docs

> Current stage: Stage3-B4 End-to-End Contract Test PLAN completed
> Next stage:    Stage3-PLAN-FREEZE
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
```

下一步只允许进入：

```text
Stage3-PLAN-FREEZE：
  评审 10 份 STAGE3_*.md 文档口径一致性（PLAN / WO / BATCH_PLAN / CONTRACT_PLAN / TEST_PLAN /
  NQ_TO_DH_FEEDBACK_PLAN / DH_TO_NQ_BACKTEST_PLAN / NQ_OUTBOX_SPEC / DH_BACKTEST_ADAPTER_SPEC /
  E2E_CONTRACT_TEST_SPEC）；
  视需要在 docs/gates/dh-stage3-plan/ 落盘冻结快照；
  本批仅文档评审 + 状态推进，不写 Java 业务代码；不联调真实 NQ；不接真实 HTTP。
  严格禁止：修改 NQ 仓库 / 接实盘 / 自动下单 / 绕风控 / 重写 NQ 回测核心 /
           引入 TradingAgents Python / 接真实 Kronos / 接真实 global-stock-data。
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
