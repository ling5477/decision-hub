# Decision Hub Current Docs

> 当前阶段: DH-GATEK-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY / IMPLEMENTED / READY FOR NEXT
> 下一阶段: DH-GATEK-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS / NOT STARTED
> 事实源: docs/current
>
> NQ / DH 三轮只读审计已完成；Integration-0 safety gate CLOSED / ACCEPTED；DH not integrated；runtime integration NOT STARTED；Integration-1 NOT STARTED；no RealClient；no real provider；no trading ability；LIVE DISABLED。
> Security baseline: FULL；fail-closed state: FULL；P1-4 residual CLOSED；header alignment CLOSED；timestamp alignment CLOSED；code reality audit blockers fixed。
> Current contract artifacts: `contracts/json-schema/dh-decision-request.schema.json` and `contracts/json-schema/dh-decision-output.schema.json`。`DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md` 与 `DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md` 已 `ACCEPTED / CLOSED`；K1 contract freeze review 已 `PASS / CLOSED / ACCEPTED`；K2 DecisionOrchestrator Skeleton 已实现；K3 Audit / Snapshot / Trace Persistence 已经 M1 readiness review 关闭；K4 Replay Read Model 已 `IMPLEMENTED / READY FOR NEXT`；K5 Provider Health / Budget / Latency 已 `IMPLEMENTED / READY FOR NEXT`。K6 Mock NQ Dry-run Contract Tests 仍 `NOT STARTED`。旧 `NQ-DH-GATEK-INTEGRATION1-PLAN-PACK` 为 `SUPERSEDED / REBASE_REQUIRED`；NQ 已进入 GateN，后续 Integration-1 必须基于 GateN rebase 重新规划。当前不允许接 LangGraph、LLM、真实 NQ runtime、真实 provider、真实 HTTP 或 LIVE。

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
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/CODEX_WORKFLOW_INDEX.md
docs/current/STATUS.md
docs/current/ROADMAP.md
docs/current/WORKFLOW.md
docs/current/WORK_ORDER.md
docs/current/DH_NQ_INTEGRATION.md
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md
docs/current/DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md
docs/current/DH_REFACTOR_STAGE1_STATUS.md
docs/current/STAGE1_CLOSE_WORKLOG.md
```

DH-NQ Integration-0 契约冻结入口（contract / mock / docs，未实现集成）：

```text
docs/current/NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md
docs/current/DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md
docs/current/DH_NQ_INTEGRATION0_SECURITY_POLICY.md
docs/current/DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md
```

`DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md` 已含 15 项 × 16 字段详细测试矩阵 + 共享 fixture + forbidden side-effect checklist + Integration-0/1 blocker + 代码任务草案（mock/contract test 设计，只写计划不写测试代码）。

DH-NQ Integration-0 contract test 代码已实现（仅 `dh-domain/src/test/**`，test-only，INT0-T01..T15 共 16 用例，全部 mock/stub/内存校验，无真实集成、无 RealClient、无真实 NQ）；`mvn test` 通过，全仓回归全绿。

DH-NQ Integration-0 safety gate：**CLOSED / ACCEPTED**（见 `DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md`）。Runtime integration / Integration-1 / AI 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED；Integration-1 前置项中 DH P1-4 residual、header alignment、timestamp alignment 已 CLOSED；真实通道安全审查与独立 PLAN 仍未开始。

DH P1-4 residual 修复方案见 `DH_P1_4_RESIDUAL_FIX_PLAN.md`：限流加在 dh-api 层（key=source+tenant+route，429 RATE_LIMITED）、memory cap（TTL + tenant/全局上限 + fail-closed）、replay nonce persistence（PostgreSQL-backed JdbcNonceReplayGuard，real channel 必须集中式）。三项均已实现：**replay nonce persistence**（BATCH-1，2026-06-12：`JdbcNonceReplayGuard` + Flyway V4 `dh_nq_replay_nonce` + `SecurityWiringConfig` 条件装配，dev/test 允许 in-memory、非 dev/test 默认 jdbc、fail-closed）；**bounded memory cap**（BATCH-2，2026-06-12：有界 + TTL + fail-closed）；**inbound rate limit**（BATCH-3，2026-06-13：dh-security `RateLimiter` 端口 + 独立 `RateLimitResult` + bounded fail-closed `InMemoryRateLimiter`，`NqFeedbackController` 前置限流、超限 429 RATE_LIMITED，in-memory 仅 dev/test/单实例，真实多实例集中式 limiter 另起任务）。三项均已 review 通过，并于 2026-06-13 完成整体回归收口（DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE）：**P1-4 residual: CLOSED**（见 `STATUS.md` §1.3）。P1-4 CLOSED 不等于允许真实联调——Integration-1 仍 NOT STARTED，禁止直接进入 Integration-1 runtime；下一步仅允许 DH-CI-PERSISTENT-NONCE-IT-ENABLE 或 DH-NQ-HEADER-ALIGNMENT-PLAN。header `X-DH-NQ-*` -> canonical `X-NQ-DH-*` 对齐方案已输出（planning-only，见 `DH_NQ_HEADER_ALIGNMENT_PLAN.md`，2026-06-14）：DH 生产 controller 现用 legacy（Source/Timestamp/Nonce/Signature 4 个），canonical 为 `X-NQ-DH-*`；**策略已定为 canonical-only（无兼容期、无 legacy/canonical 双接收）**：缺必需 canonical header -> fail-closed（由 authenticator 既有码 403/401；`MISSING_CANONICAL_HEADER` 为预留码、不单独发码）；签名 value-based（改名不漂移）；Tenant·Request·Trace 保权威来源、header 仅一致性校验不覆盖。PLAN 已 ACCEPTED（PLAN-REVIEW）。**Batch 1 DONE / ACCEPTED（2026-06-14）**：内部结构 skeleton（集中常量 `NqDhHeaderNames` + 归一化模型 + parser + validator skeleton）。**Batch 2 DONE / ACCEPTED（2026-06-15）**：生产入站切到 **canonical-only** —— controller 改用 `parseCanonical` 读 canonical `X-NQ-DH-*`，**不再接受 legacy `X-DH-NQ-*`**（仅 legacy / 缺必需 canonical -> fail-closed，Source→403 / Timestamp·Nonce·Signature→401，状态码语义不变）。**Batch 3 DONE / ACCEPTED（2026-06-15）**：接入 `NqDhHeaderValidator`，canonical `X-NQ-DH-Tenant-Id/Request-Id/Trace-Id` 与权威来源（tenant=认证上下文，requestId/traceId=body）binding 一致性校验，不一致 fail-closed（403 `HEADER_BINDING_MISMATCH`），**header 绝不覆盖权威来源**；HMAC value-based、payload 64KiB / nonce replay / rate-limit key 不变。**Batch 4 DONE（2026-06-15）**：docs/fixtures 收口（统一 `MISSING_CANONICAL_HEADER` 预留码措辞、核对 WebMvc/INT0 fixtures 均 canonical）。**header alignment 整体：CLOSED（2026-06-15，CLOSE-REVIEW-RERUN 通过）** —— Batch 1–4 全部落地并验收；quality gate 曾因 checkstyle 离线 DTD 联网超时 BLOCKED，经 `DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE`（修正 `checkstyle-suppressions.xml` DOCTYPE PUBLIC id 为官方串、离线解析）解除阻断后，`mvn test` + `mvn -Pquality validate` 稳定 BUILD SUCCESS / 0 violations、INT0 16/16。**CLOSED 不放开 runtime**：Integration-1 / Runtime integration 仍 NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。下一步 `DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT` 或 GateK-PLAN。`DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT` 已出 planning-only 方案（见 `DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md`，2026-06-15）：核查发现 `X-NQ-DH-Timestamp` 三方分歧（生产 RFC3339 / SECURITY_POLICY epoch 毫秒 / INT0 fixture epoch 秒），推荐 canonical = **RFC3339 / ISO-8601 UTC**（生产零改动），分 T1 docs / T2 INT0 测试 / T4 NQ companion 实施。**T1 docs ACCEPTED；T2 INT0 测试对齐 DONE（2026-06-15，待 T2-REVIEW）**：`SECURITY_POLICY` / `CONTRACT_FREEZE` / `CONTRACT_TEST_PLAN` 与 DH INT0 contract test（`Int0RequestFactory`/`Int0ContractValidator`）的 `X-NQ-DH-Timestamp` 均统一为 RFC3339 UTC `Z`（窗口 ±300s 不变、HMAC value-based 不改、epoch 秒/毫秒被拒、INT0 16/16）。timestamp alignment **整体 CLOSED（2026-06-28 finalization）**：T1 docs、T2 DH INT0、T3 DH production / INT0 UTC-Z-only hardening、T4 NQ companion 均 ACCEPTED；canonical=RFC3339 / ISO-8601 UTC `Z`，拒绝 epoch seconds / epoch milliseconds / 数字时区偏移，±300s window 不变。CLOSED 仅表示 timestamp 契约收口完成，不授权 Integration-1 runtime；Integration-1 仍 NOT STARTED。

2026-06-28 timestamp finalization：`DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT` 已 **CLOSED / ACCEPTED**。T1 docs、T2 DH INT0、T3 DH production / INT0 UTC-Z-only hardening、T4 NQ companion 均已 ACCEPTED；canonical timestamp 固定为 RFC3339 / ISO-8601 UTC `Z`（例 `2026-06-15T12:34:56Z`），DH production `HmacNqFeedbackAuthenticator.parseTimestamp` 已强制 `endsWith("Z") + Instant.parse`，DH / NQ INT0 均拒绝 epoch seconds、epoch milliseconds、数字时区偏移（如 `+08:00`）。±300s replay window、HMAC value-based signatureMaterial、header name 不入签、nonce / source / tenant / requestId / traceId / payload 语义均不变。timestamp CLOSED 仅表示 timestamp 契约收口完成，不授权 Integration-1 runtime；Integration-1 / Runtime integration 仍 NOT STARTED，DH NOT INTEGRATED，LIVE DISABLED。

Codex workflow 入口：

```text
docs/current/CODEX_WORKFLOW_INDEX.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/DH_CODEX_PLUGIN_WORKFLOW.md
docs/current/DH_WORKFLOW_ROUTER_SKILL.md
docs/current/DH_CODEX_TASK_TEMPLATES.md
.agents/skills/nq-dh-workflow-router/SKILL.md
.agents/skills/dh-docs-writer/SKILL.md
```

标准输出字段：

```text
Task classification:
Plugins selected:
Scope:
Files inspected:
Files changed:
Findings:
Validation:
Risks:
Next concrete action:
```

`Summary` 不作为必填字段。

语言治理规则：

```text
DH 文档正文默认中文为主。
DH 架构说明、阶段计划、WORKLOG、TESTING、ROADMAP、STATUS 默认中文为主。
DH 代码注释 / Javadoc 默认中文为主。
类名、方法名、包名、enum、JSON Schema 字段、OpenAPI 字段、HTTP header、状态枚举、命令和外部技术名保留英文原样。
固定输出字段可保留英文，但字段内容必须中文为主。
从 NQ skill 或历史文档同步规则时，必须改写为 DH 中文主语言风格。
如果 Codex 输出英文大段内容，必须在同轮改为中文。
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

当前下一步只允许进入：

```text
DH-GATEK-DECISION-PIPELINE-MVP-K6-MOCK-NQ-DRYRUN-CONTRACT-TESTS（NOT STARTED）。

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
  - DH-GATEK-DECISION-PIPELINE-MVP-PLAN 已 ACCEPTED / CLOSED。
  - DH-GATEK-DECISION-PIPELINE-MVP-WO 已 ACCEPTED / CLOSED。
  - K1 Contract Freeze 已 PASS / CLOSED / ACCEPTED。
  - K2 已实现 dh-usecase 内 DecisionOrchestrator skeleton、context builder、policy checker、
    deterministic mock signal provider、risk reviewer、output assembler 与 fail-closed tests。
  - K3 已实现 DH-owned audit / snapshot / trace persistence：Flyway V5 六张表、
    dh-usecase persistence port、dh-infra JDBC adapter、dh-app wiring 与 fail-closed tests，并已通过 M1 readiness review 关闭。
  - K3 未新增 API、Controller、replay API、真实 provider、NQ runtime、真实 HTTP、LangGraph、
    AI / Agent runtime 或 LIVE。
  - K4 Replay Read Model 已实现内部 usecase / repository read model；只读取 K3 六张 DH-owned 表，不新增 API、Controller、migration 或 replay endpoint。
  - K5 Provider Health / Budget / Latency 已实现 mock-only health / budget / latency guard；K6 仍 NOT STARTED，不得连续进入 K7-K8。
  - 不接 NQ；不新增 NQ client / RealClient / real provider；不触碰 LIVE trading；
    不修改 NQ 状态；不读取或写入 NQ DB；不启动 Paper Run。
  - Stage3-B2 / NQ Feedback Outbox / 真实 HTTP / event / NQ client / RealClient / real provider
    均为 historical / superseded / deferred / gated，不是当前 next，不允许作为当前实现任务。

严格禁止：
  接 NQ / 修改 NQ 仓库 / 接实盘 / 自动下单 / 绕风控 / 重写 NQ 回测核心 /
  引入 TradingAgents Python / 接真实 Kronos / 接真实 global-stock-data /
  新增 API / migration / provider / NQ client / RealClient / 交易路径。
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
