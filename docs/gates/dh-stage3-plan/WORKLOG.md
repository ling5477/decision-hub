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

## 2026-05-26 Stage2-PoC VERIFY

对 Stage2-PoC 全量实现做冻结前验证，仅含验证、文档修正与必要小修。详见
`docs/current/STAGE2_POC_VERIFY_REPORT.md`。

### 已完成

```text
mvn test BUILD SUCCESS / 122 tests / 0 failures / 0 errors / 0 skipped
ArchUnit 10/10 通过
硬边界扫描全 PASS（不修改 NQ / 无真实 HTTP / 无下单关键词 / JudgeDecision 唯一出口）
契约一致性：
  - contracts/openapi.yaml /api/ai/feedback/nq 改为 202 + NqFeedbackAcceptedResponse
    / 400 + NqFeedbackErrorResponse 并补两个 schema
  - docs/current/DB_SCHEMA.md 修正 V2 文件名为 V2__dh_agent_runtime.sql
  - docs/current/API.md 把已实现的 7 条 research-runs 端点移入 "已实现端点"
生成 docs/current/STAGE2_POC_VERIFY_REPORT.md（Verdict: GO，允许进入 FREEZE）
6 份文档状态同步：README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING
```

### 边界与硬约束

```text
不修改 NQ 仓库            不接真实 NQ API
不接真实 Kronos          不接真实 global-stock-data
不引入 TradingAgents Python   不实现真实下单
不绕过 NQ 风控            不重写 NQ 回测核心
不建设前端                不新增 Stage3 功能
```

### 下一步

进入 Stage2-PoC FREEZE：复制 docs/current 至 docs/gates/dh-stage2-poc/ 并锁定，
状态推进至 "Stage2-PoC FREEZE completed / Next: Stage3 PLAN"。

## 2026-05-26 Stage2-PoC FREEZE

把 Stage2-PoC VERIFY 通过的 `docs/current/` 完整快照冻结到 `docs/gates/dh-stage2-poc/`，
状态三处对齐到 "Stage2-PoC FREEZE completed / Next: Stage3-PLAN"。只做文档冻结，不动 Java 业务代码、不动 NQ 仓库、不引入 Stage3 功能。

### 冻结目录

```text
docs/gates/dh-stage2-poc/
  ├── README.md                       冻结声明 + 验收结果 + 交付物 + 边界
  ├── STATUS.md  / WORKLOG.md  / TESTING.md
  ├── API.md     / DB_SCHEMA.md / DH_NQ_INTEGRATION.md
  ├── STAGE2_POC_PLAN.md / STAGE2_POC_WORK_ORDER.md / STAGE2_POC_TEST_PLAN.md
  ├── STAGE2_POC_API_PLAN.md / STAGE2_POC_CONTRACT_PLAN.md / STAGE2_POC_DB_PLAN.md
  ├── STAGE2_POC_VERIFY_REPORT.md
  ├── ARCHITECTURE.md / ROADMAP.md / WORKFLOW.md / WORK_ORDER.md / DOCS_STRUCTURE.md
  └── DH_REFACTOR_STAGE1_*  + STAGE1_CLOSE_WORKLOG.md（沿用上一阶段背景文档）
```

冻结后 `docs/gates/dh-stage2-poc/` 不得修改，后续变更只在 `docs/current/` 进行。

### 验收结果

```text
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
BUILD SUCCESS / 122 tests / 0 failures / 0 errors / 0 skipped
ArchUnit 10/10 PASS
STAGE2_POC_VERIFY_REPORT.md Verdict: GO
```

### 文档同步

```text
README.md                       Current stage: Stage2-PoC FREEZE completed / Next: Stage3-PLAN
AGENTS.md                       同步
docs/current/README.md          同步
docs/current/STATUS.md          同步 + 追加 FREEZE 段
docs/current/WORKLOG.md         追加 2026-05-26 Stage2-PoC FREEZE（本段）
docs/current/TESTING.md         追加 FREEZE 验收记录
```

### 边界与硬约束

```text
不修改 Java 业务代码          不修改 NQ 仓库
不接真实 NQ API              不接真实 Kronos
不接真实 global-stock-data    不引入 TradingAgents Python
不实现真实下单                不绕过 NQ 风控
不重写 NQ 回测核心            不建设前端
不做 Stage3 功能              不修改 migration 语义
不修改 OpenAPI 语义           （仅修文档错字时除外）
```

### 下一步

进入 Stage3-PLAN：仅做 NQ 真实 feedback / backtest request 联调规划；
不允许直接实现 Stage3 功能；不允许修改 NQ 交易核心；不允许接实盘自动交易。

## 2026-05-26 Stage3-PLAN

只做 PLAN 文档，不写 Java 业务代码、不修改 NQ 仓库、不接任何真实外部系统。

### 已完成

```text
新增 6 份 Stage3 规划文档：
  docs/current/STAGE3_PLAN.md                       Stage3 主索引（目标 / 范围 / 风险 / 验收）
  docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md     NQ -> DH feedback 出站事件链路规划
  docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md     DH -> NQ backtest request 入站链路规划
  docs/current/STAGE3_CONTRACT_PLAN.md              端到端契约（status / errorCode / version / 4 字段规则）
  docs/current/STAGE3_TEST_PLAN.md                  测试策略（DH 单测 / NQ 契约 / 联调 / 幂等 / 重试 / 边界）
  docs/current/STAGE3_WORK_ORDER.md                 4 个 IMPLEMENT Batch 工单草案 + Codex 提示词

6 份状态文档同步到 "Stage3-PLAN completed / Next: Stage3-WO"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md（本段）/ TESTING.md

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（122 tests 全绿，作为 PLAN 文档阶段的回归基线）
```

### 严格边界（本阶段未违反）

```text
不修改任何 Java 业务代码           不修改 NQ 仓库
不接真实 NQ API                   不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不实现 Stage3 功能                 不修改 contracts/openapi.yaml 语义
不修改 Flyway migration 语义       不新增 NqFeedbackEventType
```

### 下一步

进入 Stage3-WO：按 docs/current/STAGE3_WORK_ORDER.md 启动 Stage3-Batch1 IMPLEMENT
（Contract Alignment：DH 仓库内补 8 个 Handler 经验沉淀；不联调；不动 NQ）。

---

## 2026-05-26 Stage3-WO

把 Stage3-PLAN 细化为可直接开工的 4 Batch 工单。仅文档，不写 Java 业务代码、不修改 NQ 仓库、不接任何真实外部系统。

### 已完成

```text
重写 docs/current/STAGE3_WORK_ORDER.md：
  §0 通用守则                必读清单 / 验收命令 / 硬边界 / 状态推进
  §1 Batch 1  Contract Alignment
                              文件清单 / contract 清单 / JSON Schema / OpenAPI 影响范围 /
                              DH 侧影响范围 / NQ 侧未来影响范围 / 验收标准 / 禁止事项 /
                              Codex 开工提示词
  §2 Batch 2  NQ Feedback Outbox
                              NQ 侧建议模块 / 建议表（nq_dh_feedback_outbox + nq_dh_feedback_dead_letter）/
                              event outbox 字段映射 / retry 矩阵（1s/5s/30s/5min/1h，attempt 上限 8） /
                              dead-letter / 30 天保留 / audit 对账 / 8 个触发点清单 /
                              不允许触碰的核心模块清单（订单状态机/风控/回测/实盘/账本/资金/主行情/Console） /
                              验收标准 / 禁止事项 / Codex 开工提示词
  §3 Batch 3  DH Backtest Request Adapter
                              DH 侧 adapter 文件清单（DhBacktestRequestService / Repository /
                              RealNqBacktestClient @ConditionalOnProperty / FakeNqBacktestClient
                              @ConditionalOnMissingBean 兜底 / 视情况新增 V4__stage3_dh_outbox.sql） /
                              request DTO -> domain 映射规则 + paramsHash / NQ 接收契约草案 /
                              result snapshot 回传规则 / 24h 幂等规则 + NQ 409 视为成功 /
                              traceId / requestId / correlationId / sourceJobId / eventId 5 字段规则 /
                              ArchUnit 新增规则（非 dh-connector.nq 禁 RestTemplate/WebClient/OkHttp） /
                              验收标准 / 禁止事项 / Codex 开工提示词
  §4 Batch 4  End-to-End Contract Test
                              7 个联调用例（T1-T7：入站正向 / 入站幂等 / 入站契约失败 /
                              出站正向 / 端到端反馈 / 出站幂等 / 4 字段对账） /
                              contract test 清单（DH 仓库内 + 联调 profile + NQ 仓库内）/
                              fake / stub 策略（默认 FakeNqBacktestClient / WireMock / MockWebServer） /
                              失败重试测试（5xx/400/409/429 矩阵） / 幂等测试 / 边界安全测试 /
                              验收命令（mvn 默认 + Postgres + Stage3 联调）/ 出口标准 /
                              禁止事项 / Codex 开工提示词
  §5 Stage3 整体下一步与冻结路径

新增 docs/current/STAGE3_BATCH_PLAN.md：
  §1 总览                    Batch 1-4 范围与依赖关系
  §2 Batch 边界对照表        9 维度 × 4 Batch 矩阵（仓库范围 / 是否写 Java / OpenAPI / Flyway /
                              新增事件类型 / 动 NQ 仓库 / 真实 HTTP / 实盘 / ArchUnit）
  §3 执行顺序与里程碑        M1-M4 + 不变量
  §4 全 Stage3 共同硬边界
  §5 与现有冻结物的关系       docs/gates/dh-stage1 / docs/gates/dh-stage2-poc 保护清单
  §6 Stage3 之后冻结路径      docs/gates/dh-stage3 + DH-FREEZE
  §7 与 Stage3 其他文档的衔接

6 份状态文档同步到 "Stage3-WO completed / Next: Stage3-B1 Contract Alignment"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md（本段）/ TESTING.md

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（122 tests 全绿，作为 WO 文档阶段的回归基线）
```

### 严格边界（本阶段未违反）

```text
不修改任何 Java 业务代码           不修改 NQ 仓库
不接真实 NQ API                   不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不实现 Stage3 功能                 不修改 contracts/openapi.yaml 语义
不修改 Flyway migration 语义       不新增 NqFeedbackEventType
不修改 contracts/json-schema       不联调真实 NQ
```

### 下一步

进入 Stage3-B1 Contract Alignment IMPLEMENT：
按 docs/current/STAGE3_WORK_ORDER.md §1 在 dh-usecase / dh-memory 内补 8 个 Handler 的经验沉淀链路；
仍走 InMemory；不联调；不动 NQ；mvn test 全绿。

---

## 2026-05-26 Stage3-B1 Contract Alignment IMPLEMENT

按用户工单：只在 DH 仓库内对齐契约、schema、OpenAPI、测试与文档，不修改 NQ 仓库、不实现真实联调、
不接真实 HTTP / Kronos / global-stock-data / TradingAgents Python。

### 已完成

```text
contracts/json-schema：
  - nq-feedback-envelope.schema.json
      title 顶部 description 补 "Stage2-PoC-B1 / Stage3-B1" 来源说明；
      9 个 envelope 字段 + 1 个 receivedAt 字段全部补 Stage3-B1 字段描述与示例：
        * eventId       UUIDv7 + DH 幂等键说明
        * eventType     8 种枚举映射规则 + 不允许扩展声明
        * occurredAt    业务时间（不是发送时间）
        * sourceSystem  const "nexus-quant"；任何其它值 -> 400 INVALID_SCHEMA
        * sourceJobId   paperRunId / backtestId / alertId 等；DH 只用于对账
        * traceId       与 dh_research_runs.trace_id 关联；失配 -> 400 UNKNOWN_TRACE
        * requestId     由 DH 发起请求 -> 等于原 requestId；否则 NQ 生成
        * correlationId 业务上下文（candidate 的 paper 周期等）
        * schemaVersion semver；MAJOR 升级需双方同步；本地不支持 MAJOR -> 400 INVALID_SCHEMA
        * payloadJson   原始 JSON 字符串；DH 永久留底
        * receivedAt    DH 入口时间戳；NQ 端不写
      required / enum / additionalProperties 等结构性语义不变；
      Stage2 已通过测试 NqFeedbackContractValidationTest / NqFeedbackHandlerDispatchTest /
      NqFeedbackIdempotencyTest / NqFeedbackControllerWebMvcTest 全部保持全绿。

  - dh-backtest-request.schema.json
      title 顶部 description 补 "Stage2-PoC-B1 / Stage3-B1" 说明；
      14 个 required 字段 + 2 个 nullable 字段（entryRulesRef / exitRulesRef）全部补 Stage3-B1 描述；
      status 枚举 6 值（DRAFT/QUEUED/ACCEPTED/REJECTED/RESULT_READY/FAILED）注明
      "不含 PLACE/SUBMIT/EXECUTE 等下单语义"；
      frequency 枚举 3 值（DAILY/HOURLY/MINUTE）注明 NQ 不支持 -> 400 UNSUPPORTED_FREQUENCY；
      initialCapital exclusiveMinimum=0 + symbols minItems=1 维持；
      required / enum / additionalProperties 不变。

  - dh-backtest-result-snapshot.schema.json
      title 顶部 description 补 "Stage2-PoC-B1 / Stage3-B1" 说明；
      9 个 required 字段 + 5 个 nullable 指标字段（sharpeRatio / maxDrawdown / annualReturn /
      winRate / profitFactor）全部补 Stage3-B1 描述；
      verdict 枚举 3 值（PASS/FAIL/MARGINAL）注明 "DH 不推翻 NQ verdict，
      但 verdict 进入 ExperienceEntry / PheromoneEdge 经验链路"；
      winRate range [0,1] 维持；
      required / enum / additionalProperties 不变。

contracts/openapi.yaml：
  - info.description 新增 Stage3-B1 硬边界声明（不出现 /orders / /trades / /live；
    不出现下单 / 绕风控关键词；不新增事件类型；不修改已落地端点语义；
    JudgeDecision 仍是 DH 唯一最终出口）；
  - components 段保留 NQ 端 endpoint Stage3-B1 planned contract 注释占位
    （注释里描述 POST /api/ai/research/backtest-requests 期望的 request body /
     202 DhBacktestRequestAccepted / 400 6 类 errorCode / 409 / 5xx 矩阵 /
     异步 BACKTEST_RESULT_READY feedback）；
  - paths 段不新增任何路径（Stage3-B1 严格禁止 path 落地，OpenApiContractAlignmentTest
    显式验证 paths 段无 /api/ai/research/backtest-requests / /orders / /trades / /live）；
  - 已落地 /api/ai/feedback/nq 端点语义不变：202 NqFeedbackAcceptedResponse +
    400 NqFeedbackErrorResponse；outcome 枚举（ACCEPTED / DUPLICATE）+
    errorCode 枚举（UNKNOWN_EVENT_TYPE / INVALID_SCHEMA / UNKNOWN_TRACE）保持。

dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/ 新增 4 份契约测试类：
  - NqFeedbackEnvelopeSchemaContractTest               7 cases
      schema 存在 + 可解析 / required 完整 10 字段 / additionalProperties=false /
      eventType.enum 与 NqFeedbackEventType 8 个枚举值一一对应 /
      sourceSystem const "nexus-quant" / schemaVersion semver 正则 /
      黑名单（placeOrder/submitOrder/executeOrder/bypassRisk/forceExecute/
      /orders //trades //live）全无

  - DhBacktestRequestSchemaContractTest                7 cases
      schema 存在 + 可解析 / required 完整 14 字段 / additionalProperties=false /
      status.enum 与 DhBacktestRequestStatus 6 值一一对应 /
      frequency.enum 与 BacktestFrequency 3 值一一对应 /
      initialCapital exclusiveMinimum=0 + symbols minItems=1 / 黑名单全无

  - BacktestResultSnapshotSchemaContractTest           6 cases
      schema 存在 + 可解析 / required 完整 9 字段 / additionalProperties=false /
      verdict.enum 与 BacktestVerdict 3 值一一对应 / winRate range [0,1] / 黑名单全无

  - OpenApiContractAlignmentTest                       9 cases
      openapi.yaml 存在 / /api/ai/feedback/nq 与 NqFeedbackController 一致
      （body=NqFeedbackEnvelope / 202=NqFeedbackAcceptedResponse / 400=NqFeedbackErrorResponse） /
      outcome 枚举含 ACCEPTED + DUPLICATE / errorCode 枚举含
      UNKNOWN_EVENT_TYPE + INVALID_SCHEMA + UNKNOWN_TRACE /
      DhBacktestRequest / DhBacktestRequestAccepted / DhBacktestResultSnapshot 组件存在 /
      NqFeedbackEventType 保持 8 种 /
      全文不含 placeOrder | submitOrder | executeOrder | bypassRisk | forceExecute /
      paths 段不含 /orders / /trades / /live /
      Stage3-B1 不允许在 paths 段引入 /api/ai/research/backtest-requests

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
  - 总计 151 tests / 0 failures / 0 errors / 0 skipped
    (Stage2 122 + 新增 29；增量分布在 dh-domain：35 -> 64)
  - ArchUnit 10/10 PASS（Stage1-CLOSE 5 + Stage2-PoC-B5 5；本批未新增也未放松）
  - Stage1ClosedLoopTest 1/1 / Stage2ClosedLoopTest 2/2 / NqFeedback* 22/22 全绿

docs 同步：
  - README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md（本段）/ TESTING.md
    全部对齐 "Stage3-B1 Contract Alignment completed / Next: Stage3-B2 NQ Feedback Outbox PLAN"
  - docs/current/STATUS.md §2 新增 Stage3-B1 IMPLEMENT 段
  - docs/current/TESTING.md §1 新增 Stage3-B1 IMPLEMENT 状态行 + §10 验收记录块
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不接真实 NQ API
不接真实 Kronos                    不接真实 global-stock-data
不引入 TradingAgents Python        不实现真实下单
不绕过 NQ 风控                     不重写 NQ 回测核心
不建设前端                         不实现 Stage3-B2/B3/B4 功能
不修改 contracts/openapi.yaml 已落地端点语义
不修改 contracts/json-schema 已落地字段 / required / additionalProperties / 枚举
不新增 NqFeedbackEventType（保持 8 种）
不修改 V1 / V2 / V3 Flyway migration 语义
不新增真实 HTTP client            不修改 NQ 订单 / 风控 / 回测 / 实盘语义
不破坏 ArchUnit 已落地 10 条规则
```

### 下一步

进入 Stage3-B2 NQ Feedback Outbox PLAN：
按 docs/current/STAGE3_WORK_ORDER.md §2 在 DH 仓库内产出 docs/current/STAGE3_NQ_OUTBOX_SPEC.md
（NQ 侧建议模块 + outbox/dead-letter 表 + envelope 映射 + retry 矩阵 +
audit + 8 触发点 + NQ 端硬边界）；零 Java 业务代码改动；NQ 仓库由 NQ 团队后续实施；
DH 不写真实 outbox 客户端；mvn test 全绿。


---

## 2026-05-26 Stage3-B2 NQ Feedback Outbox PLAN

按用户工单：只在 DH 仓库内规划 NQ 侧最小 feedback outbox 规格，落 docs/current/STAGE3_NQ_OUTBOX_SPEC.md。
不修改 Java 业务代码；不修改 NQ 仓库；不接真实联调；不接真实 HTTP / Kronos / global-stock-data /
TradingAgents Python；不新增 Flyway migration；不新增 OpenAPI path。

### 已完成

```text
新增 docs/current/STAGE3_NQ_OUTBOX_SPEC.md（11 段完整规格）：
  §1 目标与边界            定位（NQ outbox 仅"事实回流通道"，不是"交易控制通道"）/ 关键不变量 /
                            硬禁止（不下单 / 不绕风控 / 不修改订单状态 / 不重写回测核心 /
                            不影响 GateJ-FREEZE / outbox 不进入交易同步路径）/ 价值边界声明
  §2 NQ 侧建议模块         建议：nq-ai-contracts / nq-infra / nq-scheduler / nq-app /
                            nq-api admin（admin 命名空间 + 内部鉴权）；
                            不建议放入：nq-core / nq-risk / nq-backtest-kernel /
                            nq-paper-engine / nq-live-engine / nq-ledger / nq-fund-manager /
                            nq-marketdata-core / nq-adapter-* / nq-console-frontend；
                            模块职责矩阵（5 模块 × 5 维度：写 / 读 outbox / 发 HTTP /
                            调用核心 / 备注）
  §3 表结构                主表 nq_ai_feedback_outbox：19 列 + 5 CHECK 约束 + 4 索引 +
                            表/列 COMMENT；status CHECK 5 值；event_type CHECK 8 值；
                            source_system const；schemaVersion semver；payload jsonb；
                            timestamps timestamptz；不存密钥 / token / 账号凭证
                            死信表 nq_ai_feedback_dead_letter：13 列 + UNIQUE event_id + 索引
                            表约束统一规则（event_id 唯一 / 5 状态 CHECK / 8 类型 CHECK /
                            source_system const / schema_version 正则 / JSONB / TIMESTAMPTZ /
                            COMMENT 强制 / 不存密钥）
  §4 8 种事件触发点        每种事件含：NQ 来源模块 / 触发时机（事务提交后）/
                            payload schema 文件 / payload 来源表 / eventId 生成（UUIDv7）/
                            5 字段填充规则 / 是否允许重试与丢弃 / 对交易主链路影响声明
                            统一约束：8 项共同满足条件 + 禁止扩展事件类型清单
  §5 retry / dead-letter / audit
                            5 状态机（PENDING / SENDING / SENT / FAILED / DEAD_LETTER）+
                            状态迁移规则 +
                            退避矩阵（1s / 5s / 30s / 5min / 30min / 1h / 6h，attempt 上限 8，±10% 抖动）+
                            429 退避不计死信上限（遵守 Retry-After）+
                            失败原因分类（HTTP_400 / 401 / 403 / 429 / 5xx / TIMEOUT / NETWORK /
                            PAYLOAD_BUILD；last_error_message 限长 1024 字符且脱敏）+
                            dead-letter 进入条件 + 归档 + 30 天保留 + admin 手动复发 +
                            每日双向对账（NQ outbox sent ⊇ DH events ⊆ NQ outbox sent+dead）+
                            主链路解耦（独立线程池 / 独立连接池 / 不复用交易主链路）
  §6 幂等与追踪规则        eventId / traceId / requestId / correlationId / sourceJobId 五字段语义；
                            不可混用规则；DH 端校验顺序（schema -> sourceSystem -> eventType ->
                            schemaVersion -> eventId 幂等 -> traceId 反查）；时序约束
  §7 HTTP 交互规则         POST /api/ai/feedback/nq 请求结构 + headers + body；
                            期望响应矩阵（202 ACCEPTED|DUPLICATE / 400 + errorCode /
                            401 / 403 / 429 / 5xx / 任何其他状态码）+
                            dispatcher 安全约束白名单 + 黑名单清单 + envelope 冻结要求
  §8 NQ 后续实施 5 个 Batch
                            NQ-1 Contract + DB migration（DTO + Flyway migration，不发 HTTP）
                            NQ-2 Outbox repository + dispatcher fake（JDBC + Fake dispatcher，不发 HTTP）
                            NQ-3 8 事件源写入 outbox（事件源模块 OutboxWriter，仍 Fake dispatcher）
                            NQ-4 Real dispatcher + retry + dead-letter + audit + ArchUnit
                            NQ-5 DH/NQ contract test 联调（NQ test cluster + DH staging，
                            7 个用例 T1-T7，不接实盘）
                            每批含：目标 / 允许改动 / 禁止改动 / 文件清单 / 验收标准
  §9 风险与防护            不影响 GateJ-FREEZE（ArchUnit + 独立 Bean + 独立 ExecutorService）/
                            不进入交易同步链路 / 失败隔离矩阵（outbox 写表 / dispatcher 拉行 /
                            HTTP 发送 / DH 不可用 / outbox 表 IO 慢 5 种失败的影响范围）/
                            DH 不可用降级（PENDING 堆积 + admin pause/resume）/
                            事件重复发送幂等 + 防护手段 /
                            payload schema 演进 semver（PATCH/MINOR 自动；MAJOR 双方协同流程）
  §10 验收标准             本轮（Stage3-B2 PLAN）+ NQ 后续实施 + 硬边界 三段验收
  §11 与 Stage3 其他文档的衔接
                            STAGE3_PLAN / STAGE3_NQ_TO_DH_FEEDBACK_PLAN / STAGE3_DH_TO_NQ_BACKTEST_PLAN /
                            STAGE3_CONTRACT_PLAN / STAGE3_TEST_PLAN / STAGE3_WORK_ORDER /
                            STAGE3_BATCH_PLAN

6 份状态文档同步到 "Stage3-B2 NQ Feedback Outbox PLAN completed /
Next: Stage3-B3 DH Backtest Request Adapter PLAN"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md /
  WORKLOG.md（本段）/ TESTING.md（§1 状态行 + §11 验收记录块）

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（151 tests 全绿 / ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest +
Stage3-B1 新增 29 contract tests 全部保持回归）
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不修改 Java 业务代码
不修改 contracts/openapi.yaml      不修改 contracts/json-schema/*
不新增 Flyway migration            不新增 OpenAPI path
不接真实 NQ API                    不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不新增 NqFeedbackEventType（保持 8 种）
本轮只输出 outbox 规格文档
```

### 下一步

进入 Stage3-B3 DH Backtest Request Adapter PLAN：
按 docs/current/STAGE3_WORK_ORDER.md §3 在 DH 仓库内规划 DhBacktestRequestService + RealNqBacktestClient
（默认 disabled；仅 decisionhub.stage3.nq.enabled=true 装配；其余 profile 走 FakeNqBacktestClient）；
本批仍为 PLAN，不写 Java 业务代码；不联调真实 NQ；不接真实 HTTP；mvn test 全绿。

---

## 2026-05-26 Stage3-B3 DH Backtest Request Adapter PLAN

按用户工单：只在 DH 仓库内规划 DH -> NQ backtest request adapter，落
docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md。重点是把"DH 请求 NQ 正式回测"设计成
可插拔、默认关闭、可降级、非强依赖的增强能力。
不修改 Java 业务代码；不修改 NQ 仓库；不接真实联调；不接真实 HTTP / Kronos /
global-stock-data / TradingAgents Python；不新增 Flyway migration；不新增 OpenAPI path；
不修改 contracts/openapi.yaml 与 contracts/json-schema/* 语义。

### 已完成

```text
新增 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md（14 段完整规格）：
  §1 目标与边界           定位（DH 仅请求，NQ 仍是唯一回测执行方）/
                          关键不变量（DH/NQ 互不强依赖；ResearchRun 主流程无 NQ 时仍能闭环；
                          NQ 主流程无 DH 时仍能完整运行）/
                          硬禁止（不让 DH 执行正式回测 / 不让 DH 直接下单 /
                          不让 DH 绕过 NQ 风控 / 不让 DH 修改 NQ 订单状态 /
                          不让 DH 读写 NQ 交易核心表 / 不让 NQ 强依赖 DH）/
                          价值边界声明（能力 / 失败语义 / 默认安全 / 切换路径）
  §2 可插拔原则           Pluggable Backtest Request Principle 10 条 +
                          三层 gate 模型（stage3.nq.enabled / backtest-request.enabled /
                          fake-mode override，4 状态真值表）+
                          失败降级矩阵（NQ 完全不可用 / NQ 423 / DH 运维关闭）
  §3 建议 DH 侧模块与类   dh-usecase：DhBacktestRequestService / Default / Command /
                                       Result / Outcome / ErrorCode / Repository / InMemory
                          dh-connector：NqBacktestClient 端口 / FakeNqBacktestClient /
                                        DisabledNqBacktestClient / RealNqBacktestClient /
                                        NqBacktestClientProperties / DisabledException
                          dh-infra：（可选）JdbcDhBacktestRequestRepository
                          dh-app：Stage3NqBacktestWiringConfig（三 client 切换）
                          ArchUnit：建议 R11（非 dh-connector.nq 禁 HTTP client）+
                                    R12（usecase.backtest 禁引用 Real client）
  §4 状态模型             9 状态（CREATED / VALIDATED / SUBMITTED / ACCEPTED / RUNNING /
                                  RESULT_READY / FAILED / DISABLED / CANCELLED）+
                          合法迁移表 + 非法迁移拒绝规则 +
                          DH 不允许自行成功的硬规则（RESULT_READY 仅由 NQ 事件驱动）
  §5 DH -> NQ 请求契约    wire-level（contracts/json-schema 14 字段不变）+
                          Stage3-B3 Command 模型字段映射表（Command -> wire 字段映射）+
                          字段语义与限制（requestId / traceId / correlationId / sourceJobId /
                          strategyCandidateId / symbols / market / startTime / endTime /
                          initialCapital / frequency / feeModel / slippageModel /
                          payloadJson）+ 禁止字段清单（凭证 / token / 下单指令）
  §6 NQ 接收契约草案      POST /api/ai/backtest-requests endpoint（NQ 待确认，
                          DH 不落 path）+ auth + headers + 8 种响应矩阵
                          （202 ACCEPTED / 400 INVALID_SCHEMA / 401 / 403 /
                          409 DUPLICATE / 423 AI_DISABLED / 429 / 5xx）+
                          DH 端错误码映射表 + NQ 不允许的行为 + 默认关闭
  §7 三 client 策略       FakeNqBacktestClient（默认；deterministic；不发 HTTP）
                          DisabledNqBacktestClient（DH gate 关闭；返回 DISABLED；不抛异常）
                          RealNqBacktestClient（B3-3 IMPL 阶段；mTLS / token；timeout；
                          仅 dh-connector.nq 内；ArchUnit 守门）
                          + 四种 profile 切换路径（prod 默认 / staging / prod 启用 / 应急关闭）
  §8 幂等与重试规则       requestId 幂等键 + 24h 短路（paramsHash = sha256 of 9 字段）+
                          NQ 409 DUPLICATE 视为成功 +
                          退避矩阵（1s / 5s / 30s / 5min / 30min / 1h / 6h，attempt 上限 8，
                          ±10% 抖动）+
                          429 不计死信上限 + 4xx 永久终态 +
                          重试不阻塞 ResearchRun 主流程 +
                          result snapshot 三字段对齐（requestId / correlationId / sourceJobId）
  §9 消费 result snapshot DhBacktestResultSnapshot 来源唯一（仅 ingest 链路）+
                          经验沉淀路径（traceId 反查 -> requestId 反查 -> candidateId 反查 ->
                          落 snapshot -> ExperienceEntry / PheromoneEdge 更新 ->
                          dh_checkpoint_entries）+
                          缺字段处理 + DH 不覆盖 NQ verdict / 不反向同步
  §10 配置建议            DH application.yml（decisionhub.stage3.nq.*）+
                          NQ application.yml（nq.ai.*）+ prod 默认值（双方全 false）+
                          配置敏感性约束（凭证必须从环境变量 / Vault 注入）
  §11 测试规划            8 个测试类（FakeNqBacktestClientTest /
                          DisabledNqBacktestClientTest /
                          RealNqBacktestClientDisabledByDefaultTest /
                          DhBacktestRequestServiceTest /
                          DhBacktestRequestIdempotencyTest /
                          DhBacktestResultSnapshotConsumptionTest /
                          NoNqDependencyStartupTest /
                          NoDangerousEndpointContractTest）+
                          测试目标矩阵 + ArchUnit R11/R12 配套规则
  §12 后续 IMPL 5 个 Batch B3-1 Contract + Service Interface（先落用例层接口与 DTO）
                          B3-2 Fake / Disabled Client（三 client 切换骨架）
                          B3-3 Optional Real Client Skeleton（默认关闭，mock HTTP 测试）
                          B3-4 Result Snapshot Consumption（ingest 命中后经验沉淀）
                          B3-5 Tests + Docs（8 测试类 + 6 状态文档同步）
                          每批含：目标 / 允许改动 / 禁止改动 / 文件清单 / 验收标准
  §13 验收标准            本轮（PLAN）+ 后续 IMPL + 硬边界三段
  §14 与 Stage3 其他文档的衔接
                          STAGE3_PLAN / STAGE3_NQ_TO_DH_FEEDBACK_PLAN / STAGE3_NQ_OUTBOX_SPEC /
                          STAGE3_DH_TO_NQ_BACKTEST_PLAN / STAGE3_CONTRACT_PLAN /
                          STAGE3_TEST_PLAN / STAGE3_WORK_ORDER / STAGE3_BATCH_PLAN

6 份状态文档同步到 "Stage3-B3 DH Backtest Request Adapter PLAN completed /
Next: Stage3-B4 End-to-End Contract Test PLAN"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md /
  WORKLOG.md（本段）/ TESTING.md（§1 状态行 + §12 验收记录块）

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（151 tests 全绿 / ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest +
Stage3-B1 新增 29 contract tests 全部保持回归基线）
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不修改 Java 业务代码
不修改 contracts/openapi.yaml      不修改 contracts/json-schema/*
不新增 Flyway migration            不新增 OpenAPI path
不接真实 NQ API                    不接真实 Kronos
不接真实 global-stock-data         不引入 TradingAgents Python
不实现真实下单                     不绕过 NQ 风控
不重写 NQ 回测核心                 不建设前端
不新增 NqFeedbackEventType（保持 8 种）
本轮只输出 adapter 规划文档
```

### 下一步

进入 Stage3-B4 End-to-End Contract Test PLAN：
按 docs/current/STAGE3_WORK_ORDER.md §4 规划 NQ test cluster + DH staging 端到端联调用例（T1-T7）；
本批仍为 PLAN，不写 Java 业务代码；不联调真实 NQ；不接真实 HTTP；不接实盘；mvn test 全绿。

---

## 2026-05-26 Stage3-B4 End-to-End Contract Test PLAN

按用户工单：只在 DH 仓库内规划 DH/NQ 未来端到端契约测试方案，落
docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md。本轮只写文档，不修改 Java 业务代码，
不修改 NQ 仓库，不真实联调，不接实盘。

### 已完成

```text
新增 docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md（11 段完整规格）：
  §1 目标与边界           验证目标限定契约面（不验证实盘收益 / NQ 内部回测核心 / LLM 真实推理）+
                          硬边界（NQ test cluster 与生产物理隔离 / DH staging 与生产物理隔离 /
                          所有测试 fake-paper-backtest / 不接实盘 / 不自动下单 / 不自动发布）+
                          触发联调的前置条件（NQ-1..NQ-4 + B3-1..B3-5 + 双方 oncall 评审 +
                          默认 profile mvn test 通过 + prod 严格保持 enabled=false）
  §2 测试环境规划         DH staging（profile stage3-test 或 staging / 独立 namespace + VPC /
                          独立 PostgreSQL / 独立监控 / tenantId 前缀 t-test-* /
                          traceId / correlationId 前缀 stage3- / symbols 测试白名单 /
                          禁止实盘配置）
                          NQ test cluster（profile local/test/stage3-test / AI 默认全关 /
                          paper 与 backtest 可用 / live trading 必须关闭 / tenantId
                          前缀 t-test-* / 联调期间持续监控）
                          网络与配置（DH/NQ 双向 base url + timeout + retry + auth token
                          via Vault 24h 自动轮换 + disabled / fake mode + 出站白名单 +
                          回滚预案：失败 1h 内 enabled=false）
  §3 7 个端到端契约测试用例 每条用例含目标 / 前置 / 步骤 / 期望结果 / 失败回退：
                          T1 PAPER_RUN_CREATED feedback：单事件正向链路 + 5 字段对账
                          T2 PAPER_RUN_ALERT_RAISED 幂等：重放 → 202 DUPLICATE +
                              ack 丢失模拟 + dh_nq_feedback_events 唯一索引保障
                          T3 BACKTEST_RESULT_READY 结果消费：完整 ingest 校验链 +
                              DhBacktestResultSnapshot 落 + ExperienceEntry / PheromoneEdge
                              更新 + DH 不反向修改 NQ + JudgeDecision 仍是唯一最终出口
                          T4 backtest request accepted：DH → NQ 主路径 + 202 + jobId 持久化 +
                              状态机 SUBMITTED → ACCEPTED + NQ 风控正常评估（DH 不绕过）
                          T5 disabled mode：DisabledNqBacktestClient 装配 / 零 HTTP 出站 /
                              ResearchRun 不阻塞 / JudgeDecision 仍可生成
                          T6 outbox retry / dead-letter：DH 临时 5xx → NQ 退避矩阵重试 →
                              最终 SENT 或 DEAD_LETTER（attempt=8）+ 主链路保护断言
                          T7 安全边界扫描：关键词 / 配置 / 双向无依赖启动 / 凭证不泄露 /
                              实盘隔离（A 关键词扫描 / B 配置扫描 / C NQ 无 DH 仍可启动 /
                              D DH 无 NQ 仍可启动 / E 凭证不泄露 / F 实盘隔离）
  §4 10 类 Contract Test  JSON Schema / OpenAPI / HTTP status matrix / Error code matrix /
                          Idempotency / Retry + dead-letter / Disabled startup /
                          No dangerous endpoint / Trace correlation / Regression
                          每类含范围 / 检查项 / 落点 / 基线
  §5 测试数据与追踪规则   5 字段（eventId / requestId / traceId / correlationId / sourceJobId）
                          生成规则 + 联调前缀（stage3-{用例}-{seq}-{type}）+
                          payload / rawPayloadJson 留档（无凭证）+ deterministic 数据
                          （固定 universe / 时间窗口 / capital / paramsHash / sourceJobId 前缀）+
                          tenantId t-test-* 严格遵守 + 30 天保留供排错
  §6 验收命令规划         DH 默认 profile / CI Docker / Stage3 联调（ENABLED_STAGE3=true）三段 +
                          NQ 仓库默认 / 联调两段 + 端到端联调 19 步 checklist
                          （联调启动前 5 步 + 执行 10 步 + 结束后 4 步 + Verdict GO / NO-GO）
  §7 失败处理规则         DH 入站 10 种响应 → NQ outbox 行为映射表
                          DH 出站 10 种响应 → DH 状态机切换映射表
                          联调用例失败处理（6 优先级排查 + 1 小时回滚 + 三轮失败回 Batch PLAN）
                          NQ 主链路保护（订单 / 风控 / 账本 / 实盘 / GateJ-FREEZE 全程不退化）+
                          DH 主链路保护（NQ 不可达时 Fake 闭环 + Stage1/2ClosedLoop 全绿）
  §8 后续 Stage3-B4 IMPL 5 个 Batch
                          B4-1 DH contract test suite（@EnabledIfEnvironmentVariable 隔离）
                          B4-2 NQ contract test fixture plan（NQ 团队执行）
                          B4-3 Stub server / fake server（WireMock / MockWebServer）
                          B4-4 Disabled mode startup test（DH 启动不依赖 NQ）
                          B4-5 End-to-end dry-run checklist（T1-T7 联调 + STAGE3_VERIFY_REPORT.md）
                          每批含：目标 / 允许 / 禁止 / 文件清单 / 验收标准
                          + Batch 依赖与执行顺序
  §9 验收标准             本轮（PLAN）+ 后续 IMPL + 硬边界三段
  §10 与 Stage3 其他文档的衔接
                          STAGE3_PLAN / STAGE3_WORK_ORDER / STAGE3_BATCH_PLAN /
                          STAGE3_CONTRACT_PLAN / STAGE3_NQ_TO_DH_FEEDBACK_PLAN /
                          STAGE3_DH_TO_NQ_BACKTEST_PLAN / STAGE3_NQ_OUTBOX_SPEC /
                          STAGE3_DH_BACKTEST_ADAPTER_SPEC / STAGE3_E2E_CONTRACT_TEST_SPEC（本文件）/
                          STAGE3_TEST_PLAN
  §11 Stage3-PLAN-FREEZE 衔接
                          10 份 STAGE3_*.md PLAN 文档清单 + FREEZE 路径 +
                          IMPL → FREEZE → DH-FREEZE 路径声明

6 份状态文档同步到 "Stage3-B4 End-to-End Contract Test PLAN completed /
Next: Stage3-PLAN-FREEZE"：
  README.md / AGENTS.md / docs/current/README.md / STATUS.md /
  WORKLOG.md（本段）/ TESTING.md（§1 状态行 + §13 验收记录块）

mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
（151 tests 全绿 / ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest +
Stage3-B1 新增 29 contract tests 全部保持回归基线）
```

### 严格边界（本阶段未违反）

```text
不修改 NQ 仓库                     不修改 Java 业务代码
不修改 contracts/openapi.yaml      不修改 contracts/json-schema/*
不新增 Flyway migration            不新增 OpenAPI path
不接真实 NQ API                    不启动真实联调
不接真实 Kronos                    不接真实 global-stock-data
不引入 TradingAgents Python        不实现真实下单
不绕过 NQ 风控                     不重写 NQ 回测核心
不建设前端                         不自动下单 / 不自动发布策略
NqFeedbackEventType 保持 8 种
本轮只输出端到端契约测试规划文档
```

### 下一步

进入 Stage3-PLAN-FREEZE：
评审 10 份 STAGE3_*.md 文档（STAGE3_PLAN / STAGE3_WORK_ORDER / STAGE3_BATCH_PLAN /
STAGE3_CONTRACT_PLAN / STAGE3_TEST_PLAN / STAGE3_NQ_TO_DH_FEEDBACK_PLAN /
STAGE3_DH_TO_NQ_BACKTEST_PLAN / STAGE3_NQ_OUTBOX_SPEC / STAGE3_DH_BACKTEST_ADAPTER_SPEC /
STAGE3_E2E_CONTRACT_TEST_SPEC）口径一致性；视需要在 docs/gates/dh-stage3-plan/ 落盘冻结快照；
切到 "Stage3-PLAN-FREEZE completed / Next: Stage3-B1 IMPLEMENT" 体例。
