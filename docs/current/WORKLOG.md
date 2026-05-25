# Decision Hub Worklog

## 2026-05-25 DH-REFIT-1-PLAN

完成 DH 文档结构重构的第一批落地。

### 已完成

```text
建立 docs/current/README.md
建立 docs/current/STATUS.md
建立 docs/current/ROADMAP.md
建立 docs/current/WORKFLOW.md
建立 docs/current/WORK_ORDER.md
更新 docs/README.md
保留 docs/codex 为历史辅助区
确认 DH/NQ 边界文档存在
确认 DH Stage1 重构工单存在
```

### 结论

DH 后续采用与 NQ 一致的工作流：

```text
docs/current = 当前事实源
docs/gates = 冻结快照
docs/codex = 历史计划与辅助执行区
PLAN -> WO -> IMPLEMENT -> VERIFY -> FREEZE -> NEXT PLAN
```

### 未做

```text
未写业务代码
未修改 NQ 仓库
未接真实模型
未接交易能力
未建设前端页面
```

---

## 2026-05-25 Stage1（Boundary Freeze + Agent Runtime Skeleton）

详细记录见 `docs/current/DH_REFACTOR_STAGE1_WORKLOG.md`。本节为顶层小结。

### 已完成

```text
dh-domain  新增 ResearchRun / AgentTask / TaskNode / AgentRole / AgentArtifact /
           StrategyCandidate / SignalProposal / RiskReview / JudgeDecision /
           DecisionRecommendation / ExperienceEntry / PheromoneEdge /
           NqFeedbackEvent + 5 个状态枚举
dh-memory  ExperienceStore / PheromoneStore / FailureCaseStore /
           MarketRegimeMemory / StrategyPatternMemory + InMemory 实现
dh-eval    CandidateScorer / RiskHeuristicScorer / EvidenceQualityScorer /
           BacktestResultScorer / JudgeAggregator + 规则实现
dh-connector NqBacktestClient / NqFeedbackClient / NqStrategyCandidateMapper /
             NqContractVerifier + Fake/Default 实现
dh-usecase agent runtime 8 个 service + 6 个 repository 端口 + 默认实现 + InMemory 仓储
dh-api     /api/ai/research-runs/...（POST/GET/start/tasks/candidates/judge-decision）
           /api/ai/feedback/nq
dh-app     AgentRuntimeWiringConfig
db/migration V2__dh_agent_runtime.sql（10 张表，全部带 trace_id / status / payload_json）
test       dh-usecase 新增 ResearchRunStage1ClosedLoopTest，覆盖
           create → start → candidate → judge → NQ feedback → experience 闭环
```

### 验收

```text
mvn -pl dh-domain,dh-memory,dh-eval,dh-connector,dh-usecase -am clean test  BUILD SUCCESS
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
仅 dh-app/PostgresContainerSmokeTest 因当前环境无 Docker 而跳过（与本次改动无关）
```

---

## 2026-05-25 Stage1-CLOSE（旧链路收敛 + 文档单源 + ArchUnit 兜底）

详细记录见 `docs/current/STAGE1_CLOSE_WORKLOG.md`。本节为顶层小结。

### 已完成

```text
旧"多模型平台"链路整体 @Deprecated(since="Stage1-CLOSE", forRemoval=true)：
  - domain.run.{Run,RunStatus,RunStep,StepType}
  - usecase.facade.* + impl + dto
  - usecase.run.* + support.*
  - usecase.gate.* + evaluator.*
  - usecase.contract.*
  - dh-providers.{ModelProvider,MockProvider,ModelOutput,ProviderRegistry}
  - dh-app/AppWiringConfig（旧 bean）

api.run.RunController 迁移到 api.legacy.run 子包，@RequestMapping 从 /runs 改为 /legacy/runs；
同步 CreateRunRequest / RunView 迁移。

contracts/openapi.yaml 中 /runs 路径迁到 /legacy/runs 并标 deprecated。

文档单源：根 README + docs/current/{README,STATUS,ROADMAP,WORKLOG,WORK_ORDER,TESTING}.md
全部更新为 "Stage1 completed / Next: Stage2-PoC"。

docs/codex/plans/_active/STATUS.json 切到 2026-05-25_Stage1_agent_runtime_skeleton；
老 M1 mock-provider 计划归档到 docs/codex/plans/_archive/2026-02-04_M1/。

ArchitectureTest 新增 4 条规则：
  ① ..domain.. 不依赖 ..usecase.. / ..api.. / ..infra..
  ② ..connector.nq.. 字段/方法名禁止出现 placeOrder|submitOrder|executeOrder|bypassRisk|forceExecute
     （DefaultNqContractVerifier 自身的黑名单豁免）
  ③ ..usecase.agent.. 不依赖 dh-providers
  ④ ..api.. 控制器 @RequestMapping 不能命中 /orders|/trades|/live

dh-eval/pom.xml parent 从 decision-hub 改回 dh-bom，保留 jackson-databind / slf4j-api 依赖。

dep-tree.txt 重新生成。
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
git grep "/runs" 在 dh-api 控制器里只命中 /legacy/runs 与 deprecation 注释。
README / docs/current/STATUS.md / docs/codex/plans/_active/STATUS.json 三处状态措辞一致。
```

### 下一步

进入 Stage2-PoC：NQ 真实事件回流接通 + Kronos/global-stock-data 工具接口预留。

---

## 2026-05-25 Stage1-FREEZE

### 已完成

```text
新建 docs/gates/dh-stage1/ 目录
将 docs/current/ 快照复制到 docs/gates/dh-stage1/
docs/gates/dh-stage1/README.md 声明 Stage1 completed / Stage1-CLOSE completed / mvn test passed / Next: Stage2-PoC
更新 docs/current/STATUS.md：Current stage → Stage1-FREEZE completed
更新 README.md：Current stage → Stage1-FREEZE completed
更新 AGENTS.md：Current stage → Stage1-FREEZE completed
三处状态措辞一致
```

### 未做

```text
未写业务代码
未修改 Stage1 runtime 代码
未接 NQ
未接 Kronos / global-stock-data / TradingAgents
未做前端
```

### 下一步

进入 Stage2-PoC WO：细化工单为可执行步骤。

---

## 2026-05-25 Stage2-PoC PLAN

### 已完成

```text
docs/current/STAGE2_POC_PLAN.md           总体规划（目标、范围、模块设计、风险、验收标准）
docs/current/STAGE2_POC_API_PLAN.md       API 草案（新增端点、请求/响应格式）
docs/current/STAGE2_POC_CONTRACT_PLAN.md  事件契约草案（NQ feedback 信封、Kronos/global-stock-data 接口）
docs/current/STAGE2_POC_DB_PLAN.md        DB 迁移计划（V3 新表 + InMemory->JDBC 替换清单）
docs/current/STAGE2_POC_TEST_PLAN.md      测试计划（6 个新测试 + ArchUnit 新规则）
docs/current/STAGE2_POC_WORK_ORDER.md     工单草案（5 个 Batch + Codex 开工提示词）
更新 docs/current/STATUS.md：Current stage → Stage2-PoC PLAN completed
更新 README.md：Current stage → Stage2-PoC PLAN completed
更新 AGENTS.md：Current stage → Stage2-PoC PLAN completed
```

### 未做

```text
未写业务代码
未修改任何 Java 文件
未接 NQ
未接 Kronos / global-stock-data / TradingAgents
未做前端
```

### 下一步

进入 Stage2-PoC WO：细化工单为可执行步骤，确认每个 Batch 的具体文件清单。

---

## 2026-05-25 Stage2-PoC WO

### 已完成

```text
docs/current/STAGE2_POC_WORK_ORDER.md 重写为可执行版本：
  Batch 1  Contract + Domain
           - 16 个 JSON Schema 清单（NQ envelope + 8 payload + DH backtest + Forecast/Research/Reflection/Checkpoint）
           - 33+ Java 类清单（值对象、枚举、payload）
           - 字段清单（envelope/8 payload/Forecast/Snapshot/Reflection/Checkpoint/DhBacktestRequest）
           - 14 个 enum 清单（NqFeedbackEventType、BacktestVerdict、ForecastHorizon 等）
           - 测试清单（域模型测试 + schema 自检）
           - 不做事项
  Batch 2  NQ Feedback Ingestion
           - Controller/Service/Repository 文件清单（含 Validator/Router/8 Handler）
           - DTO 清单（envelope/accepted/error）
           - 4 步校验规则（envelope schema -> traceId 关联 -> payload 结构 -> 持久化）
           - 幂等规则（eventId 唯一键 + 唯一冲突视为命中）
           - traceId/requestId/correlationId/sourceJobId 规则
           - 测试清单（ContractValidation/Idempotency/HandlerDispatch/WebMvc）
  Batch 3  Forecast / Research Adapter Interfaces
           - interface 清单（ForecastToolPort / ResearchDataAdapter / ResearchSnapshotStore）
           - Fake 实现清单 + 行为约束
           - artifact/snapshot 字段（复用 Batch 1）
           - rawPayloadJson 留档强制规则（6 条）
           - timeout/cache/retry 后置设计（接口不暴露超时参数；Status 枚举留口）
           - 测试清单
  Batch 4  Reflection / Checkpoint / Dynamic Planner
           - 领域模型清单（复用 Batch 1）
           - planner 接口清单（Resolver/Registry/4 个 StrategyHandler/ReflectionCheckpointService）
           - 默认 planner 行为（regime -> strategy 映射表）
           - reflection/checkpoint 字段规范 + 写入规则（JudgeDecision 仍是唯一最终出口）
           - 测试清单（5+4+1 个测试）
           - 禁止引入的 TradingAgents 组件清单（7 项 ❌ + 1 项 ✅）
  Batch 5  JDBC + Tests + Docs
           - V3__stage2_poc_tools.sql 表结构（4 新表 + 2 ALTER）
           - 6 个 InMemory→JDBC 替换 + 5 个新 JDBC 仓储
           - 单元测试矩阵（domain/contracts/connector/usecase/api/infra-jdbc）
           - 集成测试矩阵（Stage2ClosedLoopTest / ApplicationContextLoadsTest / PostgresContainerSmokeTest）
           - ArchUnit 5 条新规则（含旧 5 条共 10 条）
           - docs/current 更新清单 + WiringConfig 装配清单
           - 验收命令

docs/current/STATUS.md：Current stage -> Stage2-PoC WO completed / Next -> Stage2-PoC IMPLEMENT
README.md：Current stage 同步
AGENTS.md：Current stage 同步
```

### 未做

```text
未写业务代码
未修改任何 Java 文件
未修改任何 SQL / OpenAPI / Schema 文件
未接 NQ
未接 Kronos / global-stock-data / TradingAgents
未做前端
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
```

### 下一步

进入 Stage2-PoC IMPLEMENT：按 Batch 1 -> 5 顺序实现，每个 Batch 完成后跑一次 mvn test。

---

## 2026-05-25 Stage2-PoC-B1 IMPLEMENT

### 已完成

```text
dh-domain (新增 30 个文件)
  feedback/
    NqFeedbackEventType (8 值 enum)
    NqFeedbackEnvelope (Stage2 信封值对象，含 traceId/requestId/correlationId/sourceJobId/payloadJson)
  feedback/payload/
    AlertLevel (INFO/WARN/ERROR/CRITICAL)
    StabilityCheckResult (STABLE/UNSTABLE/INCONCLUSIVE)
    PaperRunCreatedPayload
    PaperRunStartedPayload
    PaperRunStoppedPayload
    PaperRunDailyReportGeneratedPayload
    PaperRunAlertRaisedPayload
    PaperRunRecoveryEventRecordedPayload
    PaperRunStabilityCheckCompletedPayload
    BacktestResultReadyPayload (持有 requestId 关联 DhBacktestRequest)
  backtest/
    BacktestVerdict (PASS/FAIL/MARGINAL)
    BacktestFrequency (DAILY/HOURLY/MINUTE)
    DhBacktestRequestStatus (DRAFT/QUEUED/ACCEPTED/REJECTED/RESULT_READY/FAILED)
    DhBacktestRequest (含 withStatus 不可变状态前进)
    DhBacktestResultSnapshot
  forecast/
    ForecastTarget (PRICE/VOLATILITY/DIRECTION/VOLUME)
    ForecastHorizon (D1/D5/D20/D60)
    ForecastArtifactStatus (COMPLETED/PENDING/FAILED/TIMEOUT)
    ForecastPoint (confidence [0,1])
    ForecastArtifact (含 rawPayloadJson)
  marketdata/
    MarketDataSource (GLOBAL_STOCK_DATA/INTERNAL_CACHE/FAKE)
    MarketSnapshotStatus (COMPLETED/PENDING/FAILED)
    ExternalMarketSnapshot (含 dataJson + rawPayloadJson)
  reflection/
    ReflectionType (STEP/AGENT/RUN)
    ReflectionEntry
  checkpoint/
    CheckpointType (5 值，含 CANDIDATE_FROZEN/JUDGE_DECISION/PIVOT/ABORT/BACKTEST_REQUESTED)
    CheckpointStatus (DRAFT/RECORDED/DISCARDED)
    CheckpointEntry

contracts/json-schema/ (新增 16 个 JSON Schema 文件)
  nq-feedback-envelope.schema.json
  nq-feedback-paper-run-{created,started,stopped,daily-report-generated,
                        alert-raised,recovery-event-recorded,stability-check-completed}.schema.json
  nq-feedback-backtest-result-ready.schema.json
  dh-backtest-{request,request-accepted,result-snapshot}.schema.json
  forecast-artifact.schema.json
  external-market-snapshot.schema.json
  reflection-entry.schema.json
  checkpoint-entry.schema.json

contracts/openapi.yaml
  追加 components/schemas 23 项（envelope + payload + Dh backtest + 工具产物 + reflection + checkpoint）
  不新增任何 path / endpoint

dh-domain/src/test (新增 8 个测试类，35 个测试用例全绿)
  feedback/NqFeedbackEnvelopeTest             4 cases
  feedback/NqFeedbackPayloadContractTest      8 cases (8 个 payload 各一)
  backtest/DhBacktestRequestContractTest      5 cases
  forecast/ForecastArtifactTest               4 cases
  marketdata/ExternalMarketSnapshotTest       3 cases
  reflection/ReflectionEntryTest              3 cases
  checkpoint/CheckpointEntryTest              3 cases
  contracts/JsonSchemaPresenceTest            5 cases (16 个 schema 存在/可解析/
                                                     additionalProperties=false/
                                                     无 placeOrder/submitOrder/executeOrder/bypassRisk/forceExecute/
                                                     envelope 枚举 8 值)

dh-domain/pom.xml  新增 JUnit Jupiter 测试依赖

docs/current/STATUS.md     Current stage -> Stage2-PoC-B1 IMPLEMENT completed
docs/current/TESTING.md    追加 Stage2-PoC-B1 验收结果
README.md                  同步
AGENTS.md                  同步
docs/current/README.md     同步
```

### 未做（Batch 1 严格边界）

```text
未写 Controller / Service / Repository / JDBC / WiringConfig
未接 NQ / Kronos / global-stock-data
未引入 TradingAgents Python 代码
未做前端
未修改 NQ 仓库
未引入新的 ArchUnit 规则（Batch 5 一次性处理）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
dh-domain 新增 35 个测试全部通过
Stage1 闭环测试保持通过
ArchitectureTest 5/5 保持通过
```

### 下一步

进入 Stage2-PoC-B2 IMPLEMENT：NQ feedback ingestion 正式契约（Controller/Service/Validator/Router/8 Handler）。

---

## 2026-05-25 Stage2-PoC-B2 IMPLEMENT

### 已完成

```text
dh-usecase (新增 17 个文件)
  agent/feedback/
    IngestionErrorCode (3 值 enum: UNKNOWN_EVENT_TYPE / INVALID_SCHEMA / UNKNOWN_TRACE)
    IngestionOutcome (3 值 enum: ACCEPTED / DUPLICATE / REJECTED)
    IngestionCommand (HTTP -> 用例层入参值对象，rawEventType 仍为字符串)
    IngestionResult (accepted/duplicate/rejected 工厂)
    ValidationResult (ok/fail 工厂，校验通过时携带 NqFeedbackEnvelope)
    NqFeedbackContractValidator        (接口)
    NqFeedbackEventHandler             (接口)
    NqFeedbackEventTypeRouter          (接口)
    NqFeedbackIngestionService         (接口)
  agent/feedback/impl/
    DefaultNqFeedbackContractValidator (envelope 字段 + sourceSystem + rawEventType -> enum +
                                        schemaVersion >= 1.0.0 + traceId 命中 ResearchRun +
                                        per-eventType 必填字段表)
    DefaultNqFeedbackEventTypeRouter   (EnumMap 全覆盖 8 个 handler，缺一或重复即抛 IllegalStateException)
    DefaultNqFeedbackIngestionService  (幂等优先 -> 校验 -> 保存 -> 派发)
  agent/feedback/handler/
    AbstractNqFeedbackEventHandler     (公共基类：解析 payload + 构造 Stage1 NqFeedbackEvent + append + apply)
    PaperRunCreatedHandler / PaperRunStartedHandler / PaperRunStoppedHandler /
    PaperRunDailyReportGeneratedHandler (PnL>=0 判 positive) /
    PaperRunAlertRaisedHandler (ERROR/CRITICAL 判 negative) /
    PaperRunRecoveryEventRecordedHandler /
    PaperRunStabilityCheckCompletedHandler (UNSTABLE 判 negative) /
    BacktestResultReadyHandler (FAIL 判 negative)

dh-usecase (修改 2 个文件)
  agent/NqFeedbackEventRepository.java                  +saveEnvelope/findEnvelopeByEventId
  agent/inmemory/InMemoryNqFeedbackEventRepository.java +putIfAbsent eventId 唯一键幂等

dh-api (新增 3 个文件 + 修改 2 个)
  feedback/NqFeedbackEnvelopeRequest.java        (Stage2 envelope 请求 DTO，全字段 @NotBlank/@NotNull)
  feedback/NqFeedbackAcceptedResponse.java       (202 响应)
  feedback/NqFeedbackErrorResponse.java          (400 响应)
  feedback/NqFeedbackController.java             升级到 envelope 契约；保留 /api/ai/feedback/nq 路径，无新增 path
  feedback/NqFeedbackRequest.java                @Deprecated(since="Stage2-PoC-B2", forRemoval=true)
  pom.xml                                        加 spring-boot-starter-test (test scope)

dh-app (修改 1 个文件)
  config/AgentRuntimeWiringConfig.java           装配 ObjectMapper / Validator / 8 个 Handler / Router /
                                                 NqFeedbackIngestionService；Stage1 DefaultNqIntegrationUseCase
                                                 与新链路并存（不删除）

dh-usecase 测试 (新增 4 个文件，15 个 cases 全绿)
  agent/feedback/B2TestFixtures.java                          (公共 fixtures)
  agent/feedback/NqFeedbackContractValidationTest.java        7 cases：8 eventType + 6 错误场景
  agent/feedback/NqFeedbackIdempotencyTest.java               3 cases：重放、不同 eventId、REJECTED 不入库
  agent/feedback/NqFeedbackHandlerDispatchTest.java           5 cases：8 全覆盖、独立派发、Stage1 append、raw 保留、重复注册

dh-api 测试 (新增 1 个文件，7 个 cases 全绿)
  feedback/NqFeedbackControllerWebMvcTest.java               202/400 路径 + outcome + 字段分离 + bean 校验

docs/current/STATUS.md / WORKLOG.md / TESTING.md / README.md / AGENTS.md / docs/current/README.md
  状态切到 Stage2-PoC-B2 IMPLEMENT completed / Next: Stage2-PoC-B3 IMPLEMENT
```

### 校验链与状态码（与 WO §Batch 2.4 一致）

```text
envelope 必填字段缺失 / sourceSystem != nexus-quant / payload 结构不符  -> 400 INVALID_SCHEMA
rawEventType 不在枚举内                                                  -> 400 UNKNOWN_EVENT_TYPE
schemaVersion < 1.0.0                                                    -> 400 INVALID_SCHEMA
traceId 未命中 ResearchRunRepository                                     -> 400 UNKNOWN_TRACE
eventId 已存在                                                           -> 202 outcome=DUPLICATE, status=RECEIVED
首次合法                                                                 -> 202 outcome=ACCEPTED, status=RECEIVED
```

### B2 follow-up（不在本批回改 Batch 1 领域模型，记录在此）

```text
1. NqFeedbackEnvelope 当前 schemaVersion 字段无 semver 校验；只在 service 层 (DefaultNqFeedbackContractValidator)
   做 >= 1.0.0 比较。若未来允许放宽 schemaVersion 字符串（含 -beta 等后缀），需要域内值对象或专门 SemVer 类型。
2. payload 强类型 value object（PaperRunCreatedPayload 等）当前未由 service 反序列化使用；handler 仅做
   最小 map 解析以触发经验链路。Batch 3+ 可考虑在 service 层增加类型化反序列化以做更细校验。
3. AbstractNqFeedbackEventHandler 用 traceId 作为 runId 走 Stage1 ExperienceFeedbackService 闭环；
   若 Stage2-PoC-B4 / B5 引入 ResearchRun -> runId 单独字段，需把这里改为通过仓库查 runId。
4. NqFeedbackController 仍使用 "t-default" 单 tenant；多租户回到 Stage3+ 再讨论。
```

### 未做（B2 严格边界）

```text
未写 JDBC（Batch 5 处理）
未接真实 NQ HTTP（Fake 保留）
未接 Kronos / global-stock-data
未修改 dh-domain（Batch 1 已固化）
未修改 OpenAPI components/schemas（Batch 1 已落地）
未引入 TradingAgents Python 代码
未做前端
未修改 NQ 仓库
未引入新的 ArchUnit 规则（Batch 5 一次性处理）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
dh-usecase B2 新增 15 个测试 + Stage1 闭环测试全部通过
dh-api WebMvc 7 个 cases 全部通过
dh-app ArchitectureTest 5/5 保持通过
```

### 下一步

进入 Stage2-PoC-B3 IMPLEMENT：dh-connector Forecast / Research Adapter 接口预留 + Fake 实现。
