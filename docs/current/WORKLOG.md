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

---

## 2026-05-25 Stage2-PoC-B3 IMPLEMENT

### 已完成

```text
dh-connector (新增 8 个文件)
  tools/
    ForecastRequest                  (traceId/symbol/horizon/target；symbol 非空 + horizon/target 非空检查)
    ForecastToolPort                 (Stage2 同步占位端口；rawPayloadJson 非空契约)
  tools/fake/
    FakeForecastToolAdapter          (deterministic mock：固定 generatedAt / FAKE_MODEL_VERSION /
                                      2 个 ForecastPoint；rawPayloadJson 写 mock JSON；
                                      symbol/horizon 非法时 IllegalArgumentException)
  research/
    MarketSnapshotRequest            (traceId/symbols/source/rangeStart/rangeEnd/dataTypes；
                                      symbols 非空 + rangeEnd>=rangeStart 校验)
    ResearchDataAdapter              (Stage2 同步占位端口；rawPayloadJson 非空契约)
    ResearchSnapshotStore            (save / findById / findByTraceId / findBySymbolAndDateRange)
  research/fake/
    FakeResearchDataAdapter          (deterministic mock：固定 fetchedAt / FAKE_SOURCE_VERSION；
                                      dataTypes 空 -> dataJson="{}"；rawPayloadJson 写 mock JSON)
    InMemoryResearchSnapshotStore    (ConcurrentHashMap<snapshotId,…> +
                                      二级索引 traceId -> snapshotId set)

dh-connector (修改 1 个文件)
  pom.xml                            加 junit-jupiter (test scope)

dh-connector 测试 (新增 3 个文件，9 个 cases 全绿)
  tools/fake/FakeForecastToolAdapterTest          3 cases
    ①  happy path -> COMPLETED + 非空 rawPayloadJson + deterministic artifactId
    ②  symbol 为空 / blank -> IllegalArgumentException
    ③  horizon 为空 -> IllegalArgumentException
  research/fake/FakeResearchDataAdapterTest       4 cases
    ①  happy path -> COMPLETED + 非空 rawPayloadJson + deterministic snapshotId
    ②  symbols 为空 -> IllegalArgumentException
    ③  rangeStart > rangeEnd -> IllegalArgumentException
    ④  空 dataTypes -> dataJson="{}" + COMPLETED + 非空 rawPayloadJson
  research/fake/InMemoryResearchSnapshotStoreTest 2 cases
    ①  save -> findById / findByTraceId 命中；null/missing 返回 empty
    ②  findBySymbolAndDateRange 命中/未命中（多 symbol + 日期 overlap）+ start>end 抛错

docs/current/STATUS.md / WORKLOG.md / TESTING.md / README.md / AGENTS.md
  状态切到 Stage2-PoC-B3 IMPLEMENT completed / Next: Stage2-PoC-B4 IMPLEMENT
```

### Raw Payload 留档（强制 6 条）

```text
1. FakeForecastToolAdapter 返回的 ForecastArtifact.rawPayloadJson 必填，
   写 {"source":"fake-forecast","symbol":"…","horizon":"…","target":"…"}。
2. FakeResearchDataAdapter 返回的 ExternalMarketSnapshot.rawPayloadJson 必填，
   写 {"source":"fake-research","symbols":[…],"dataTypes":[…],"rangeStart":"…","rangeEnd":"…"}。
3. Fake 不允许空字符串；空 dataTypes 时 dataJson 退化为 "{}"。
4. 数据库列 raw_payload_json TEXT NOT NULL，由 Batch 5 落地。
5. 真实接入失败时，未来适配器 fallback 必须把异常摘要写入 rawPayloadJson。
6. 不允许把敏感凭据写入 rawPayloadJson（Fake 已遵守，无任何 token 字段）。
```

### Timeout / Cache / Retry 后置设计（与 WO §Batch 3.6 一致）

```text
Stage2 不实现 timeout / cache / retry，仅以 javadoc + status 枚举做接口预留：
  ForecastArtifactStatus { COMPLETED, PENDING, FAILED, TIMEOUT }
  MarketSnapshotStatus   { COMPLETED, PENDING, FAILED }

后置接入计划（不在本 WO 内实现）：
  - ForecastToolPort 真实化时，由适配器层通过 Resilience4j 实现 timeout/circuit-breaker/retry。
  - ResearchDataAdapter 真实化时，由适配器层加入磁盘缓存（snapshotId 命中复用）。
  - 接口签名不变，因此 Stage2 的 Fake 与未来真实实现可平滑替换。
```

### 未做（B3 严格边界）

```text
未修改 dh-domain（复用 Batch 1 ForecastArtifact / ExternalMarketSnapshot）
未修改 NQ 仓库
未接真实 Kronos / global-stock-data / NQ API
未引入 Resilience4j / Caffeine / HTTP 客户端
未引入 JDBC（Batch 5 才落地 JDBC ResearchSnapshotStore）
未写 WiringConfig
未引入 TradingAgents Python 代码
未做前端
未引入新的 ArchUnit 规则（Batch 5 一次性处理）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
dh-connector Batch 3 新增 9 个 cases 全部通过
Batch 1 / Batch 2 测试保持全绿
Stage1 闭环测试保持通过
dh-app ArchitectureTest 5/5 保持通过
```

### 下一步

进入 Stage2-PoC-B4 IMPLEMENT：Reflection / Checkpoint / Dynamic Planner。

---

## 2026-05-25 Stage2-PoC-B4 IMPLEMENT

### 已完成

```text
dh-usecase (新增 12 个文件)
  agent/planner/
    PlannerStrategy                       (enum: DEFAULT / BULL_FOCUSED / BEAR_FOCUSED / VOLATILE_DIVERSIFIED；
                                           Stage2-PoC-B4 边界下放在 dh-usecase，避免改动 dh-domain)
    PlannerStrategyResolver               (接口：PlannerStrategy resolve(ResearchRun))
    PlannerStrategyRegistry               (EnumMap-backed；缺 DEFAULT 抛 IAE；重复注册抛 IAE；
                                           handlerFor(null/未知 strategy) 回退 DEFAULT)
    DynamicAgentTaskPlanner               (实现 AgentTaskPlanner：resolver.resolve -> registry.handlerFor ->
                                           handler.buildTask(run, TimeProvider.now()))
  agent/planner/impl/
    DefaultPlannerStrategyResolver        (1. payloadJson.plannerStrategy 显式覆盖（valueOf 失败回退到 regime）；
                                           2. payloadJson.marketRegime 关键字匹配：
                                              volatile / high_volatility 优先于 bull / bear，
                                              使 "bullish but volatile" -> VOLATILE_DIVERSIFIED；
                                           3. 未命中默认 DEFAULT；零 LLM 依赖)
  agent/planner/strategy/
    PlannerStrategyHandler                (接口：strategy() + buildTask(ResearchRun, Instant))
    DefaultPlannerStrategyHandler         (Stage1 6 节点 DAG：SCOUT/ANALYST/STRATEGY/
                                           RISK_REVIEWER/STRATEGY_REVIEWER/JUDGE；
                                           payload tag plannerStrategy=DEFAULT + planSchemaVersion=stage2-b4-v1)
    BullFocusedPlannerStrategyHandler     (SCOUT -> ANALYST -> STRATEGY(primary+secondary) ->
                                           STRATEGY_REVIEWER -> JUDGE；6 节点)
    BearFocusedPlannerStrategyHandler     (SCOUT -> ANALYST -> STRATEGY ->
                                           RISK_REVIEWER(primary+secondary) -> JUDGE；6 节点)
    VolatileDiversifiedPlannerStrategyHandler
                                          (LEADER -> SCOUT -> ANALYST -> STRATEGY ->
                                           (RISK_REVIEWER + STRATEGY_REVIEWER) -> JUDGE；7 节点)
  agent/
    ReflectionEntryRepository             (端口：save / listByRun)
    CheckpointEntryRepository             (端口：save / listByRun)
    ReflectionCheckpointService           (接口：recordReflection / recordCheckpoint /
                                           listReflections / listCheckpoints)
  agent/inmemory/
    InMemoryReflectionEntryRepository     (ConcurrentHashMap<runId, List>；按 stepIndex 排序返回)
    InMemoryCheckpointEntryRepository     (ConcurrentHashMap<runId, List>；按 checkpointIndex 排序返回)
  agent/impl/
    DefaultReflectionCheckpointService    (调用 ReflectionEntry.of / CheckpointEntry.of，
                                           id=IdGenerator.newId()，createdAt=TimeProvider.now()；
                                           ABORT checkpoint + reflection 不替代 JudgeDecision)

dh-usecase 测试 (新增 4 个文件，28 个 cases 全绿)
  agent/planner/PlannerStrategyResolverTest         9 cases
    ①  null run -> DEFAULT
    ②  空 payload -> DEFAULT
    ③  unknown regime 字符串 -> DEFAULT
    ④  bullish/BULL/mid-cap bull cycle -> BULL_FOCUSED
    ⑤  bearish/Bear market -> BEAR_FOCUSED
    ⑥  volatile / high_volatility -> VOLATILE_DIVERSIFIED
    ⑦  "bullish but volatile" -> VOLATILE_DIVERSIFIED（volatile 关键字优先级高于 bull/bear）
    ⑧  显式 plannerStrategy 覆盖 regime
    ⑨  非法显式 plannerStrategy -> 回退到 regime
  agent/planner/PlannerStrategyRegistryTest         5 cases
    ①  注册表缺少 DEFAULT handler -> IllegalArgumentException
    ②  重复 strategy 注册 -> IllegalArgumentException
    ③  handlerFor 返回已注册 handler
    ④  缺失 strategy -> 回退 DEFAULT
    ⑤  4 种 strategy 各自 buildTask 非空 + JUDGE 出现
  agent/planner/DynamicAgentTaskPlannerTest         7 cases
    ①  DEFAULT regime -> DefaultPlannerStrategyHandler 输出 6 节点
    ②  bull regime -> BullFocused 路径 (STRATEGY x2)
    ③  bear regime -> BearFocused 路径 (RISK_REVIEWER x2)
    ④  volatile regime -> VolatileDiversified 7 节点 (LEADER 在前)
    ⑤  显式 plannerStrategy 覆盖 regime
    ⑥  registry 缺失 strategy -> 回退 DEFAULT
    ⑦  每种 strategy 都保留 JUDGE 终点
  agent/reflection/ReflectionCheckpointServiceTest  7 cases
    ①  recordReflection 持久化命中
    ②  listReflections 按 stepIndex 排序
    ③  stepIndex<0 -> IllegalArgumentException
    ④  recordCheckpoint 持久化命中
    ⑤  snapshotJson null -> NullPointerException
    ⑥  ABORT checkpoint + reflection 仍不替代 JudgeDecision (JudgeDecision 仍是唯一终点)
    ⑦  未知 runId -> 空集合

docs/current/STATUS.md / WORKLOG.md / TESTING.md / README.md / AGENTS.md / docs/current/README.md
  状态切到 Stage2-PoC-B4 IMPLEMENT completed / Next: Stage2-PoC-B5 IMPLEMENT
```

### Planner 行为表（与 WO §Batch 4.3 一致）

```text
marketRegime 包含 "bullish" / "bull"                  -> BULL_FOCUSED
marketRegime 包含 "bearish" / "bear"                  -> BEAR_FOCUSED
marketRegime 包含 "volatile" / "high_volatility"      -> VOLATILE_DIVERSIFIED（优先级高于 bull/bear）
其它或字段缺失                                          -> DEFAULT
payload.plannerStrategy 显式合法值                     -> 覆盖 regime
payload.plannerStrategy 非法值                         -> 回退到 regime 关键字匹配
registry.handlerFor(未注册 strategy 或 null)            -> DEFAULT
```

### JudgeDecision 仍为唯一终点（与 WO §Batch 4.4 一致）

```text
所有 4 个 StrategyHandler 在任务图末尾必须挂 JUDGE 节点（buildTask 测试覆盖）
ReflectionEntry / CheckpointEntry 只是过程证据，不携带最终决策
ABORT 类型 CheckpointEntry / RUN 级 ReflectionEntry 都不替代 JudgeDecision
```

### 不在 dh-domain 落 PlannerStrategy（B4 边界 trade-off）

```text
原 WO 把 PlannerStrategy 枚举挂在 dh-domain/agent/；
B4 边界要求"不修改 dh-domain"，故 PlannerStrategy 暂放在 dh-usecase/agent/planner/。
Batch 5 评估是否补回 dh-domain 枚举并配套 ArchUnit 规则。
```

### 未做（B4 严格边界）

```text
未引入 LLM client / Python / graph scheduler / 复杂 agent graph runtime
未引入 TradingAgents Python 代码
未修改 dh-domain（复用 Batch 1 ReflectionEntry / CheckpointEntry / AgentTask / TaskNode）
未修改 NQ 仓库
未接真实 NQ API / Kronos / global-stock-data
未引入 JDBC（Batch 5 才落地 JDBC reflection / checkpoint 仓储）
未写 WiringConfig（Stage1 闭环测试仍直连 DefaultAgentTaskPlanner；
                   DynamicAgentTaskPlanner 装配延后到 Batch 5）
未做前端
未引入新的 ArchUnit 规则（Batch 5 一次性处理）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS
dh-usecase Batch 4 新增 28 个 cases 全部通过
Batch 1 / Batch 2 / Batch 3 测试保持全绿
Stage1 闭环测试保持通过（DefaultAgentTaskPlanner 直连未被破坏）
dh-app ArchitectureTest 5/5 保持通过
```

### 下一步

进入 Stage2-PoC-B5 IMPLEMENT：JDBC + Tests + Docs（V3 迁移脚本 + 9 个 JDBC 仓储 + ArchUnit 新规则 +
OpenAPI/装配收口）。

---

## 2026-05-25 Stage2-PoC-B5 IMPLEMENT

完成 Stage2-PoC 最后一批收口：V3 migration、JDBC 仓储、WiringConfig、ArchUnit 新规则、OpenAPI 对齐、文档与测试收口。

### 已完成

```text
dh-app/src/main/resources/db/migration/V3__stage2_poc_tools.sql
  4 张新表（CREATE IF NOT EXISTS）：
    dh_forecast_artifacts          (predictions_json jsonb, raw_payload_json jsonb)
    dh_external_market_snapshots   (symbols_json/data_json/raw_payload_json jsonb)
    dh_reflection_entries          (payload_json jsonb，unique(run_id, step_index))
    dh_checkpoint_entries          (snapshot_json jsonb，unique(run_id, checkpoint_index))
  2 张 ALTER（ADD COLUMN IF NOT EXISTS）：
    dh_research_runs               regime, planner_strategy default 'DEFAULT'
    dh_nq_feedback_events          event_id / schema_version / validation_status /
                                   source_job_id / request_id / correlation_id
    + DO 块创建 ux_dh_nq_feedback_events_event_id (event_id is not null)
  comment on table / comment on column 覆盖 trace_id/request_id/correlation_id 等追踪列

dh-infra（新增 5 个 Stage2 JDBC 仓储 + dh-connector 依赖 + jackson + jdbc starter）：
  JdbcNqFeedbackEventRepository      Stage1 append/listByRun + Stage2 saveEnvelope/findEnvelopeByEventId
                                     INSERT 使用 CAST(? AS jsonb)，eventId 幂等：先查询再 catch DuplicateKeyException
  JdbcForecastArtifactRepository     ForecastArtifactStore 实现；predictions_json + raw_payload_json
                                     双 CAST(? AS jsonb)；Jackson ArrayNode 序列化
  JdbcExternalMarketSnapshotRepository ResearchSnapshotStore 实现；symbols_json + data_json +
                                       raw_payload_json 三段 CAST(? AS jsonb)；
                                       findBySymbolAndDateRange 用 symbols_json @> CAST(? AS jsonb)
  JdbcReflectionEntryRepository      payload_json CAST(? AS jsonb)；RowMapper 重建 ReflectionEntry
  JdbcCheckpointEntryRepository      snapshot_json CAST(? AS jsonb)

dh-app config：
  Stage2JdbcWiringConfig             @ConditionalOnProperty(prefix="decisionhub.stage2.jdbc",
                                     name="enabled", havingValue="true", matchIfMissing=false)
                                     装配 5 个 JDBC bean；默认不生效
  AgentRuntimeWiringConfig           5 个 InMemory bean 加 @ConditionalOnMissingBean
                                     允许 JDBC bean 在 enabled=true 时覆盖；
                                     补 DynamicAgentTaskPlanner + Resolver + Registry +
                                     4 个 PlannerStrategyHandler + ReflectionCheckpointService +
                                     ForecastToolPort + ResearchDataAdapter +
                                     InMemoryForecastArtifactStore + InMemoryResearchSnapshotStore

contracts/openapi.yaml：
  POST /api/ai/feedback/nq           对齐 B2 实现（NqFeedbackEnvelope 请求 + {eventId, duplicate} 响应）
  B3/B4 路径以注释占位（forecast/snapshots/reflections/checkpoints controllers 留 VERIFY 上线）

dh-app tests：
  V3MigrationPresenceTest            5 cases（4 张新表 / 2 ALTER / event_id 唯一索引 /
                                     jsonb 列与 comment 保留 / 无 orders|trades|fills|positions|live_）
  ArchitectureTest                   扩到 10 条规则；新增：
                                     - connector.tools !depends ..infra..
                                     - connector.research !depends ..infra..
                                     - domain.{forecast,marketdata,reflection,checkpoint} !depends ..connector..
                                     - usecase.agent.planner !depends providers..
                                     - usecase.agent.feedback !depends providers..

dh-infra tests：
  JdbcNqFeedbackEventRepositoryTest  4 cases：首次写返回 true + CAST(? AS jsonb) /
                                     幂等命中返回 false 且不调 update / unique 竞态返回 false /
                                     null eventId 返回 Optional.empty
  JdbcSqlFragmentsTest               5 cases：reflection/checkpoint/forecast/external_snapshot
                                     insert SQL 命中正确表名 + CAST(? AS jsonb) 次数（>=1/2/3）+
                                     external_snapshot.findById RowMapper 路径

dh-usecase tests：
  Stage2ClosedLoopTest               2 cases：完整 Stage2 闭环——
                                     bullish 走 BULL_FOCUSED / bear 走 BEAR_FOCUSED；
                                     reflections 按 stepIndex 升序、checkpoints 按 checkpointIndex 升序；
                                     JudgeDecision 仍是唯一最终出口

docs/current/STATUS.md / WORKLOG.md / TESTING.md / API.md / DB_SCHEMA.md / README.md / AGENTS.md
  状态切到 Stage2-PoC-B5 IMPLEMENT completed / Next: Stage2-PoC VERIFY
```

### 边界守恒（B5 严格边界）

```text
未修改 NQ 仓库
未接真实 NQ API / Kronos / global-stock-data
未引入 TradingAgents Python 代码
未实现真实下单 / 未绕过 NQ 风控 / 未重写 NQ 回测核心
未建设前端
未改 Stage1 已冻结语义（Stage1ClosedLoopTest 仍直连 DefaultAgentTaskPlanner，绿）
未删除 legacy 旧链路（/legacy/runs 保留）
未引入外部 HTTP 客户端
未把 dh-memory 5 个 Store 替换为 JDBC（留 Stage3）
```

### 验收

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false  BUILD SUCCESS

Stage1 闭环 + Batch 1/2/3/4 测试保持全绿
Stage2-PoC-B5 新增 16 cases（V3 5 + JdbcNqFeedback 4 + JdbcSqlFragments 5 + Stage2ClosedLoop 2）全绿
ArchitectureTest 10/10 通过
默认 profile 下 JDBC bean 不装配，InMemory 通路不被破坏
```

### 下一步

进入 Stage2-PoC VERIFY：在装好 Docker 的 CI 环境跑 PostgresContainerSmokeTest，
与 NQ 团队对齐真实 ingest endpoint，灰度切换 decisionhub.stage2.jdbc.enabled=true。

