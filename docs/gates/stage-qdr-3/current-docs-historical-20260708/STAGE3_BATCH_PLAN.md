# Stage3 Batch Plan

> Created: 2026-05-26
> Parent: `docs/current/STAGE3_PLAN.md` / `docs/current/STAGE3_WORK_ORDER.md`
> Scope: 集中列出 Stage3-WO 拆出的 4 个 Batch 的边界、依赖与执行顺序。
> 本文件不写业务代码，不修改 NQ 仓库，不实现真实联调。

## 1. 总览

```text
Batch 1  Contract Alignment             仅 DH 仓库内对齐契约 + 经验沉淀（不联调）
Batch 2  NQ Feedback Outbox             仅 SPEC 文档（NQ 仓库由 NQ 团队后续实施）
Batch 3  DH Backtest Request Adapter    DH 侧 adapter + Fake；默认不连真实 HTTP
Batch 4  End-to-End Contract Test       staging + CI Docker 联调；不接实盘
```

依赖关系：

```text
Batch 1  --(契约对齐)-->  Batch 2  --(SPEC 落地 + 字段映射)-->  Batch 3  --(adapter 与 Fake 通路)-->  Batch 4
                              \
                               +--(NQ 团队按 SPEC 实施 outbox，不阻塞 DH 仓库；Batch 4 联调时校验)
```

## 2. Batch 边界对照表

| 维度 | Batch 1 | Batch 2 | Batch 3 | Batch 4 |
| --- | --- | --- | --- | --- |
| 仓库范围 | DH only | DH 文档 only | DH only | DH + NQ test cluster |
| 是否写 Java | 是（dh-usecase / dh-memory 增量） | 否 | 是（dh-connector.nq + dh-usecase + WiringConfig） | 仅测试代码 |
| 是否动 OpenAPI | 仅 description / examples | 仅 description / examples | 仅 description / examples（不破坏语义） | 不动 |
| 是否动 Flyway | 否 | 否 | 视情况新增 V4__stage3_dh_outbox.sql（可选） | 否 |
| 是否新增事件类型 | 否 | 否 | 否 | 否 |
| 是否动 NQ 仓库 | 否 | 否（仅写 SPEC） | 否 | 否（NQ 由 NQ 团队部署，DH 只发请求） |
| 是否接真实 HTTP | 否 | 否 | 默认否，profile 开启才装 bean | 联调时是，默认 profile 仍 Fake |
| 是否接实盘 | 否 | 否 | 否 | 否 |
| 是否新增 ArchUnit | 视情况新增 | 否 | 视情况新增（如 RestTemplate / WebClient 限制） | 否 |

## 3. 执行顺序与里程碑

```text
M1 Stage3-Batch1-IMPLEMENT  完成后：DH 8 个 Handler 命中后能写 ExperienceEntry / PheromoneEdge / FailureCaseStore；
                                    单测 + ArchUnit 全绿；状态切到 "Stage3-B1 completed / Next: Stage3-B2 PLAN"。
M2 Stage3-Batch2-PLAN       完成后：docs/current/STAGE3_NQ_OUTBOX_SPEC.md 落地；零 Java 代码改动；
                                    状态切到 "Stage3-B2 completed / Next: Stage3-B3 PLAN"。
M3 Stage3-Batch3-IMPLEMENT  完成后：DH 侧 DhBacktestRequestService + RealNqBacktestClient（默认 disabled）+
                                    Fake 仍兜底；24h 幂等短路 + 5xx 重试 + 409 视为成功有单测覆盖；
                                    状态切到 "Stage3-B3 completed / Next: Stage3-B4 PLAN"。
M4 Stage3-Batch4-VERIFY     完成后：7 个联调用例全绿 + STAGE3_VERIFY_REPORT.md（Verdict: GO/NO-GO）；
                                    状态切到 "Stage3-Batch4 VERIFY completed / Next: Stage3-FREEZE"。
```

每个里程碑必须保持以下不变量：

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
ArchUnit 全绿（默认 10 条规则；Batch 3 视情况新增不破坏既有）
零 NQ 仓库改动；零真实下单 / 实盘 / 风控旁路；零 Kronos / global-stock-data / TradingAgents Python 接入
```

## 4. 全 Stage3 共同硬边界

```text
不修改 Java 业务代码（仅 Batch 1 / Batch 3 在 PLAN/WO 完成后允许，且仅限 dh-usecase / dh-connector / dh-memory / dh-app）
不修改 NQ 仓库
不接真实 NQ API（默认 profile 始终 Fake；Batch 4 联调走 NQ test cluster）
不接真实 Kronos / global-stock-data
不引入 TradingAgents Python / graph scheduler / 复杂 agent graph runtime
不实现真实下单 / 不绕过 NQ 风控 / 不重写 NQ 回测核心
不建设前端
不新增 NqFeedbackEventType（保持 8 种）
不修改 Flyway migration 已落地语义（仅允许 Batch 3 IMPL 时新增 V4__stage3_dh_outbox.sql）
不修改 contracts/openapi.yaml 已落地端点的语义（仅允许 description / examples 完善）
```

## 5. 与现有冻结物的关系

```text
Stage1 冻结快照     docs/gates/dh-stage1/         保留不动，作为 Agent Runtime 基线
Stage2-PoC 冻结快照 docs/gates/dh-stage2-poc/     保留不动，作为契约 + JDBC + ArchUnit 基线
Stage2 已落地资产   contracts/openapi.yaml         仅 description / examples 完善
                    contracts/json-schema/*       16 份 schema 数量保持
                    Flyway V1/V2/V3                语义不变
                    ArchUnit 10 条规则              全部保留；Batch 3 可加严不可放松
                    122 tests                       Stage3 任一 Batch 完成后必须保持全绿
```

## 6. Stage3 之后

```text
Stage3-Batch4 VERIFY 通过 -> Stage3-FREEZE：
  docs/current/* 完整拷贝到 docs/gates/dh-stage3/
  README.md / AGENTS.md / docs/current/STATUS.md 切换到
    "Stage3 FREEZE completed / Next: DH-FREEZE"

Stage3-Batch4 VERIFY 不通过 -> 回到对应 Batch 的 PLAN，禁止跳跃。
```

## 7. 与 Stage3 其他文档的衔接

```text
Stage3 主索引          -> docs/current/STAGE3_PLAN.md
NQ -> DH 出站事件链路  -> docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md
DH -> NQ 入站请求链路  -> docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md
端到端契约规则         -> docs/current/STAGE3_CONTRACT_PLAN.md
测试策略               -> docs/current/STAGE3_TEST_PLAN.md
4 批工单细化           -> docs/current/STAGE3_WORK_ORDER.md
```
