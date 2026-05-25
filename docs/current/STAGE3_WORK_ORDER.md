# Stage3 Work Order

> Created: 2026-05-26
> Parent: `docs/current/STAGE3_PLAN.md`
> Scope: 把 Stage3 IMPLEMENT 拆成 4 个可独立 ship 的 Batch，本文件只产出 WO 草案，不实施。

## 0. 通用守则

每个 Batch 都遵守 `STAGE3_PLAN.md` §3 禁止范围，且都要：

```text
开工前读：README.md / AGENTS.md / docs/current/{README,STATUS,WORKLOG,TESTING,ROADMAP,
            DH_NQ_INTEGRATION,STAGE3_PLAN,STAGE3_NQ_TO_DH_FEEDBACK_PLAN,
            STAGE3_DH_TO_NQ_BACKTEST_PLAN,STAGE3_CONTRACT_PLAN,STAGE3_TEST_PLAN}.md
结尾必须：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false 全绿
状态：    Stage3-IMPLEMENT-B{N} completed -> 进入 B{N+1} PLAN
禁止：    任何 NQ 仓库改动；任何真实下单 / 风控旁路 / 实盘 / 前端开发
```

## 1. Batch 1: Contract Alignment

### 1.1 目标

把 DH 已落地契约与 NQ 未来接入点对齐，把"经验沉淀"链路按 STAGE3_NQ_TO_DH_FEEDBACK_PLAN §6 在 DH 仓库内补齐。仅 DH 仓库内改动，不联调。

### 1.2 允许改动

```text
dh-domain                可补 ExperienceEntry / PheromoneEdge 写入辅助方法（不破坏现有签名）
dh-usecase.agent.feedback  在 8 个 Handler 中接入经验沉淀（InMemory 实现即可）
dh-memory                ExperienceStore / PheromoneStore / FailureCaseStore 增量行为（仍 InMemory）
dh-eval                  复用既有 scorer；如需新增 scorer 必须单测覆盖
contracts/openapi.yaml   仅新增 DH 端期望的 NQ endpoint 占位注释或 components；不破坏现有语义
contracts/json-schema    禁止新增事件类型；可补 description / examples
docs/current             更新 STATUS / WORKLOG / TESTING / STAGE3_*.md
test                     按 STAGE3_TEST_PLAN §1.1 / §1.3 新增 unit + ArchUnit 测试
```

### 1.3 禁止改动

```text
NQ 仓库的任何文件
实现真实 HTTP client
新增事件类型 / 新增 JudgeDecision 终态
修改 Flyway migration
新增 /api/ai/research/backtest-requests 真实 controller（Batch 3 才允许）
```

### 1.4 文件清单（待 IMPLEMENT 阶段确认）

```text
新增 / 修改：
  dh-usecase/.../agent/feedback/handler/*Handler.java   补 ExperienceEntry / PheromoneEdge 写入
  dh-memory/.../experience/InMemoryExperienceStore.java 复用现有；补必要查询方法
  dh-memory/.../pheromone/InMemoryPheromoneStore.java   同上
  dh-usecase/.../agent/feedback/NqFeedbackHandlerExperienceTest.java  新增 unit test
  dh-app/.../ArchitectureTest.java                       视情况新增规则（不破坏 10 条已落地）
  docs/current/STATUS.md / WORKLOG.md / TESTING.md      更新 Batch1 段
```

### 1.5 验收命令

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

### 1.6 Codex 开工提示词

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage3-Batch1-Contract-Alignment。
当前阶段：Stage3-WO completed / Next stage: Stage3-Batch1-IMPLEMENT。
目标：把 NqFeedbackEnvelope ingest 命中后的经验沉淀链路在 DH 仓库内补齐：
  - 8 个 NqFeedbackEventType Handler 接入 ExperienceEntry / PheromoneEdge / FailureCaseStore（仍走 InMemory）
  - 不引入真实 HTTP；不联调 NQ；不新增事件类型；不修改 migration / OpenAPI 语义
  - 按 docs/current/STAGE3_TEST_PLAN.md §1.1 / §1.3 新增 unit + ArchUnit 测试
请只改 dh-usecase / dh-memory / docs/current；不改 NQ 仓库；mvn test 全绿。
```

---

## 2. Batch 2: NQ Feedback Outbox PLAN/IMPLEMENT

### 2.1 目标

规划并由 NQ 团队在 NQ 仓库实现"最小 feedback outbox"。本仓库的工作仅限：契约草案、测试 fixture、字段映射文档；不实施 NQ 仓库代码。

### 2.2 允许改动（DH 仓库）

```text
docs/current/STAGE3_NQ_OUTBOX_SPEC.md   Batch2 PLAN 阶段新增（描述 NQ 端 outbox 表结构 / 重试矩阵 / 字段映射）
contracts/openapi.yaml                  仅注释 / examples 完善
contracts/json-schema                   description / examples 完善
docs/current                            STATUS / WORKLOG / TESTING 更新
```

### 2.3 禁止改动

```text
任何 Java 代码（DH 仓库 / NQ 仓库都不实施）
任何业务事件类型新增
DH 仓库内任何 Java 实现的 outbox 客户端（不属于本 Batch）
```

### 2.4 NQ 团队后续工作（不在本仓库进行）

```text
Paper Trading / Backtest / Risk / Daily Report / Alert / Recovery / Stability 7 个事件源接入 outbox
outbox 表结构落 NQ DB（参考 STAGE3_NQ_TO_DH_FEEDBACK_PLAN §5）
重试矩阵实施
dead-letter 表
对账作业
```

### 2.5 验收命令（DH 仓库）

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

DH 侧无业务代码变更，主要靠回归保持绿。

### 2.6 Codex 开工提示词

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage3-Batch2-NQ-Outbox-Spec。
当前阶段：Stage3-Batch1-IMPLEMENT completed / Next stage: Stage3-Batch2-PLAN。
目标：在 docs/current/STAGE3_NQ_OUTBOX_SPEC.md 中写下 NQ 侧 feedback outbox 的完整规格：
  - outbox 表结构 / 字段约束 / 索引
  - 重试矩阵（1s / 5s / 30s / 5m / 1h，attempt 上限 8）
  - dead-letter / audit / 对账作业
  - 与 NqFeedbackEnvelope 字段映射
  - schemaVersion 升级流程
不写任何 Java 代码；不修改 NQ 仓库；mvn test 全绿。
```

---

## 3. Batch 3: DH Backtest Request Adapter

### 3.1 目标

在 DH 仓库内实现 DhBacktestRequest 的生成 + 出站客户端骨架。允许引入 HTTP client，但默认仍走 Fake；只有显式 profile `decisionhub.stage3.nq.enabled=true` 才装真实客户端 bean。

### 3.2 允许改动

```text
dh-domain.backtest               必要时新增 builder util（保持既有不变）
dh-usecase.agent.backtest        新增 DhBacktestRequestService（从 ResearchRun + Candidate -> DhBacktestRequest）
dh-connector.nq                  新增 RealNqBacktestClient（HTTP 实现），@ConditionalOnProperty(...stage3.nq.enabled)
                                 默认仍是 FakeNqBacktestClient（@ConditionalOnMissingBean 兜底）
dh-app/WiringConfig              注册新 bean，但默认 disabled
dh-infra                         若需 outbox 表，可新增 V4__stage3_dh_outbox.sql（仅在 Batch3 IMPLEMENT 真正落地时）
contracts/openapi.yaml           可补 DH 端期望对端 endpoint 描述（不破坏已落地）
test                             按 STAGE3_TEST_PLAN §1.2 / §4 / §5 新增；Fake 端到端 + 幂等 + 重试
```

### 3.3 禁止改动

```text
默认 profile 下不得连接任何真实 HTTP
不得绕过 NQ 风控
不得在 DH 仓库实现回测撮合 / 数据加载 / 风控
不得修改已有 Flyway migration 语义（新加只允许 V4__*.sql）
ArchUnit 不允许放松 dh-connector.nq 之外的模块连接 HTTP
```

### 3.4 验收命令

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

### 3.5 Codex 开工提示词

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage3-Batch3-DH-Backtest-Adapter。
当前阶段：Stage3-Batch2-PLAN completed / Next stage: Stage3-Batch3-IMPLEMENT。
目标：
  - 新增 DhBacktestRequestService（dh-usecase）
  - 新增 RealNqBacktestClient（dh-connector.nq，@ConditionalOnProperty(decisionhub.stage3.nq.enabled)）
  - Fake 仍为默认；@ConditionalOnMissingBean 兜底
  - 单测覆盖 build / send / idempotent（24h 短路）/ 5xx 重试 / 409 视成功
  - ArchUnit 新规则：除 dh-connector.nq 外其他模块禁止引用 RestTemplate / WebClient / OkHttp
约束：默认 profile 不连真实 HTTP；不联调；mvn test 全绿。
```

---

## 4. Batch 4: End-to-End Contract Test

### 4.1 目标

在 NQ test cluster + DH staging 跑通 STAGE3_TEST_PLAN §3 的 7 个联调用例；产出 `docs/current/STAGE3_VERIFY_REPORT.md`。

### 4.2 允许改动

```text
docs/current/STAGE3_VERIFY_REPORT.md   联调验证报告（go/no-go FREEZE）
docs/current                          STATUS / WORKLOG / TESTING 更新
test                                  可新增端到端 smoke test（默认 @Disabled 或 @EnabledIfEnvironment）
配置                                  application-stage3.yml（不入 prod profile）
```

### 4.3 禁止改动

```text
不接实盘 / 不自动发布策略 / 不下单
DH 仓库不实现 NQ outbox / 不实现 NQ 端逻辑
不在 Batch4 IMPLEMENT 阶段修改契约 / migration 语义
```

### 4.4 出口标准（与 Stage2-PoC VERIFY 同体例）

```text
7 个联调用例全绿
mvn test 全绿（不含联调用例的默认 profile）
PostgresContainerSmokeTest 在 CI Docker 环境跑通
STAGE3_VERIFY_REPORT.md 给出 Verdict: GO / NO-GO
```

### 4.5 验收命令

```bash
# DH 默认 profile
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false

# CI Docker 环境
mvn test -Dtest='PostgresContainerSmokeTest'

# Staging 联调
ENABLED_STAGE3=true mvn -pl dh-app test -Dtest='Stage3*EndToEnd*'
```

### 4.6 Codex 开工提示词

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：Stage3-Batch4-E2E-Contract-Test。
当前阶段：Stage3-Batch3-IMPLEMENT completed / Next stage: Stage3-Batch4-IMPLEMENT。
目标：
  - 在 staging 跑 7 个联调用例（STAGE3_TEST_PLAN §3）
  - 默认 profile 仍走 Fake；联调用例以 @EnabledIfEnvironmentVariable(ENABLED_STAGE3=true) 隔离
  - 在 CI Docker 环境跑 PostgresContainerSmokeTest，回填 docs/current/TESTING.md
  - 写 docs/current/STAGE3_VERIFY_REPORT.md，给出 go/no-go FREEZE 判定
约束：禁实盘 / 禁自动发布 / 禁下单 / 禁风控旁路 / 不修改 NQ 仓库。
```

---

## 5. Stage3 整体下一步

```text
Stage3-WO completed   -> Stage3-Batch1 IMPLEMENT
Stage3-Batch1         -> Batch2 -> Batch3 -> Batch4
Stage3-Batch4 VERIFY  -> Stage3-FREEZE (docs/current -> docs/gates/dh-stage3/)
Stage3-FREEZE         -> DH-FREEZE（Decision Hub Agent Decision Layer v1）
```
