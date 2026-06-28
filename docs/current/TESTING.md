# Decision Hub Testing

## 2026-06-28 DH-CODE-REALITY-AUDIT-FIX-PACK

```text
Scope:
  - /legacy/runs 纳入 DhApiAuthenticationFilter；POST/GET 匿名访问拒绝，认证请求使用认证 tenant。
  - production NQ feedback validator 对齐 INT0 forbidden-field / forbidden-capability 递归校验。
  - IdempotencyFilter 调整到认证之后，只使用认证 tenant，不再写固定 t-default 幂等 key。
  - docs/current/API.md 修正旧阶段口径。

Focused regression:
  Command:
    mvn "-Dtest=LegacyRunControllerSecurityWebMvcTest,IdempotencyFilterSecurityTest,NqFeedbackControllerWebMvcTest,NqFeedbackContractValidationTest,NqFeedbackIdempotencyTest,DhNqIntegration0*Test" "-Dsurefire.failIfNoSpecifiedTests=false" test
  Result:
    BUILD SUCCESS；34 tests passed；INT0 16/16 passed。

Full validation:
  git status --short:
    仅本 fix pack 允许范围内文件变更；新增 legacy/idempotency 回归测试文件。
  git diff --check:
    exit code 0；仅 LF/CRLF 提示，无 whitespace error。
  git diff --stat:
    tracked diff 11 files changed, 558 insertions(+), 28 deletions(-)；另有 2 个新测试文件未计入 tracked stat。
  mvn test:
    BUILD SUCCESS；Surefire reports 汇总 307 tests / 0 failures / 0 errors / 4 skipped。
    Skipped 为 Docker/Testcontainers 环境项（JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1）。
  mvn -Pquality validate:
    BUILD SUCCESS；checkstyle 0 violations；spotless check passed。
```

## 1. 当前状态

```text
Stage1                  代码 + 闭环测试已落地
Stage1-CLOSE            旧链路 @Deprecated + 文档单源 + ArchUnit 4 条新规则
Stage2-PoC-B1 IMPLEMENT 领域模型 + JSON Schema + OpenAPI components 落地
Stage2-PoC-B2 IMPLEMENT NQ feedback ingestion envelope/Validator/Router/8 Handler/幂等/WebMvc
Stage2-PoC-B3 IMPLEMENT dh-connector Forecast / Research Adapter 接口预留 + Fake 实现
Stage2-PoC-B4 IMPLEMENT Reflection / Checkpoint / Dynamic Planner + 4 个 StrategyHandler + 内存仓储 + 4 个测试
Stage2-PoC-B5 IMPLEMENT V3 migration + 5 个 Stage2 JDBC 仓储 + Stage2JdbcWiringConfig +
                        ArchUnit 扩到 10 条 + OpenAPI 对齐 + Stage2ClosedLoopTest 全闭环
Stage2-PoC VERIFY       2026-05-26 BUILD SUCCESS / 122 tests / ArchUnit 10/10；
                        硬边界扫描全 PASS；契约/文档不一致项已修正；
                        Verdict: GO，允许进入 Stage2-PoC FREEZE
Stage2-PoC FREEZE       2026-05-26 完成：docs/current 快照冻结到
                        docs/gates/dh-stage2-poc/；FREEZE 前最终验收 mvn test
                        BUILD SUCCESS / 122 tests / ArchUnit 10/10；
                        无 Java 业务代码改动；下一阶段进入 Stage3-PLAN
Stage3-PLAN              2026-05-26 完成：仅文档规划，新增 6 份 STAGE3_*.md；
                        无 Java 业务代码改动；mvn test 作为回归基线 BUILD SUCCESS / 122 tests
                        / ArchUnit 10/10；下一阶段进入 Stage3-WO
Stage3-WO                2026-05-26 完成：仅文档工单细化；重写 STAGE3_WORK_ORDER.md
                        + 新增 STAGE3_BATCH_PLAN.md；无 Java 业务代码改动；
                        mvn test 作为回归基线 BUILD SUCCESS / 122 tests / ArchUnit 10/10；
                        下一阶段进入 Stage3-B1 Contract Alignment IMPLEMENT
Stage3-B1 IMPLEMENT      2026-05-26 完成：DH 仓库内对齐 contracts / schema / OpenAPI；
                        新增 4 份 contract 测试类（NqFeedbackEnvelopeSchemaContractTest 7 +
                        DhBacktestRequestSchemaContractTest 7 +
                        BacktestResultSnapshotSchemaContractTest 6 +
                        OpenApiContractAlignmentTest 9 = 29 cases）；
                        mvn test BUILD SUCCESS / 151 tests / ArchUnit 10/10；
                        Stage1ClosedLoop / Stage2ClosedLoop / 全部历史用例保持全绿；
                        零 NQ 仓库改动；零真实 HTTP；零 Java 业务代码修改；
                        下一阶段进入 Stage3-B2 NQ Feedback Outbox PLAN
Stage3-B2 PLAN           2026-05-26 完成：仅落 docs/current/STAGE3_NQ_OUTBOX_SPEC.md
                        （NQ outbox 11 段完整规格：模块 / 表结构 / 8 触发点 / 5 状态机 +
                        8 attempt 退避矩阵 / audit / 5 字段语义 / HTTP 矩阵 /
                        NQ 后续 5 个 Batch / GateJ-FREEZE 防护 / schema 演进）；
                        无 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration / OpenAPI 修改；
                        mvn test 作为回归基线 BUILD SUCCESS / 151 tests / ArchUnit 10/10；
                        下一阶段进入 Stage3-B3 DH Backtest Request Adapter PLAN
Stage3-B3 PLAN           2026-05-26 完成：仅落 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md
                        （DH backtest request adapter 14 段完整规格：可插拔原则 10 条 + 三层 gate +
                        三 client 策略（Fake / Disabled / Real）+ 9 状态机 + 错误码映射 +
                        24h 幂等 + 8 attempt 退避 + DH/NQ 双方默认关闭 + 8 个测试类规划 +
                        B3-1..B3-5 五批 IMPL 拆解）；
                        无 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration / OpenAPI 修改；
                        mvn test 作为回归基线 BUILD SUCCESS / 151 tests / ArchUnit 10/10；
                        下一阶段进入 Stage3-B4 End-to-End Contract Test PLAN
Stage3-B4 PLAN           2026-05-26 完成：仅落 docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md
                        （DH/NQ 端到端契约测试 11 段完整规格：DH staging + NQ test cluster
                        环境规划 / 7 个联调用例 T1-T7 / 10 类 Contract Test / 5 字段对账 /
                        deterministic 测试数据 / 三段验收命令 / 失败处理矩阵 /
                        B4-1..B4-5 五批 IMPL 拆解 / Stage3-PLAN-FREEZE 衔接）；
                        无 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration / OpenAPI 修改；
                        零真实联调；零实盘；
                        mvn test 作为回归基线 BUILD SUCCESS / 151 tests / ArchUnit 10/10；
                        下一阶段进入 Stage3-PLAN-FREEZE
Stage3-PLAN-FREEZE       2026-05-26 完成：Stage3 规划成果落盘冻结：
                        - 10 份 STAGE3_*.md 一致性核查 9 条核心原则通过（无措辞修订）
                        - docs/current/* 33 个文件复制到 docs/gates/dh-stage3-plan/
                        - 冻结快照 README.md 顶部加冻结声明 + 一致性核查表 +
                          Stage3-PLAN 交付物清单
                        - 6 份状态文档同步到 "Stage3-PLAN-FREEZE completed /
                          Next: Stage3-B1 IMPLEMENT"
                        - mvn test 作为回归基线 BUILD SUCCESS / 151 tests / 0 failures /
                          0 errors / 0 skipped / ArchUnit 10/10
                        - 本轮为文档冻结，零 Java 业务代码改动；零 NQ 仓库改动；
                          零 contracts / migration / OpenAPI 修改；零真实外部接入
                        - 下一阶段进入 Stage3-B1 IMPLEMENT（B1 已完成；B2/B3/B4 单独开工）
DH-CODEX-WORKFLOW       2026-06-06 完成：Codex workflow routing 文档固化；
                        本轮仅文档与规则文件变更，不运行 mvn test；
                        使用 git status --short / git diff --check / 定向文本检查验证
```

最近一次 `mvn test` 见 §3。

## 2. 标准验证命令

最低验证：

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

说明：`PostgresContainerSmokeTest` 依赖 Docker，本机/CI 缺少 Docker 时排除。

质量检查：

```bash
mvn -Pquality validate
```

应用启动验证：

```bash
mvn -pl dh-app -am spring-boot:run
```

## 3. 最近一次验收结果（2026-05-25 Stage2-PoC-B5 IMPLEMENT）

```text
日期：2026-05-25
阶段：Stage2-PoC-B5 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  Batch 5 新增（dh-app）
    - V3MigrationPresenceTest                   5/5   通过（4 张新表 / 2 ALTER / event_id 唯一索引 /
                                                              jsonb 列 + comment 保留 / 无 orders|trades|fills|positions|live_）
    - ArchitectureTest                          10/10 通过（Stage1-CLOSE 5 + Stage2-PoC-B5 新增 5：
                                                              connector.tools/research !-> infra；
                                                              domain.{forecast,marketdata,reflection,checkpoint} !-> connector；
                                                              usecase.agent.planner/feedback !-> providers）

  Batch 5 新增（dh-infra）
    - JdbcNqFeedbackEventRepositoryTest         4/4   通过（首次写返回 true + CAST(? AS jsonb) /
                                                              幂等命中返回 false 不调 update /
                                                              unique 竞态 catch DuplicateKeyException /
                                                              null eventId 返回 Optional.empty）
    - JdbcSqlFragmentsTest                      5/5   通过（reflection/checkpoint insert 命中表名 + 1 段 CAST(? AS jsonb)，
                                                              forecast insert 2 段 CAST(? AS jsonb)，
                                                              external_snapshot insert 3 段 CAST(? AS jsonb)，
                                                              external_snapshot.findById 走 RowMapper 返回 empty）

  Batch 5 新增（dh-usecase）
    - Stage2ClosedLoopTest                      2/2   通过（bullish 走 BULL_FOCUSED + reflections 按 stepIndex 升序 +
                                                              checkpoints 按 checkpointIndex 升序 + JudgeDecision 唯一出口；
                                                              bear 走 BEAR_FOCUSED 仍以 JudgeDecision 终结）

  Batch 4 / Batch 3 / Batch 2 / Batch 1 回归保持全绿
    - dh-domain    Batch 1                      35/35
    - dh-connector Batch 3                      9/9
    - dh-usecase   Batch 2 + B4 + B5            47/47
    - dh-api       Batch 2 WebMvc               7/7
    - dh-app                                    15/15（含 ArchUnit 10 + V3MigrationPresence 5）
    - dh-infra     Batch 5                      9/9

  Stage1 回归
    - ResearchRunStage1ClosedLoopTest           1/1   通过（DefaultAgentTaskPlanner 仍直连）
    - DecisionHubFacadeImplTest                 1/1   通过（旧链路冒烟）

跳过：
  - PostgresContainerSmokeTest                  因当前环境无 Docker，按命令显式排除
                                                Stage2-PoC VERIFY 在装好 Docker 的 CI 上跑

Batch 5 范围（零 NQ 仓库改动 / 零真实外部服务调用 / 零 LLM / 零 TradingAgents Python 代码 / 零前端 /
              零 dh-memory JDBC 替换（留 Stage3）/ 零绕过 NQ 风控）：
  - dh-app 新增   V3__stage2_poc_tools.sql, Stage2JdbcWiringConfig, V3MigrationPresenceTest
                  AgentRuntimeWiringConfig 补 DynamicAgentTaskPlanner + ReflectionCheckpointService 等
                  ArchitectureTest 扩到 10 条规则
  - dh-infra 新增  5 个 JDBC 仓储 + 2 个测试类（9 cases 全绿） + pom.xml 加 dh-connector / jdbc starter
  - dh-usecase 新增 Stage2ClosedLoopTest（2 cases 全绿）
  - dh-connector 新增 ForecastArtifactStore + InMemoryForecastArtifactStore
  - contracts/openapi.yaml  /api/ai/feedback/nq 对齐 + B3/B4 路径占位注释
```

## 5. 历史验收：2026-05-25 Stage2-PoC-B4 IMPLEMENT

```text
日期：2026-05-25
阶段：Stage2-PoC-B4 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  Batch 4 新增（dh-usecase）
    - PlannerStrategyResolverTest               9/9   通过（DEFAULT 兜底 + bull/bear/volatile 关键字 + volatile 优先 + 显式 plannerStrategy 覆盖 regime + 非法 strategy 回退）
    - PlannerStrategyRegistryTest               5/5   通过（缺失 DEFAULT 拒绝 + 重复注册拒绝 + 注册查 handler + 缺失 strategy 回退 DEFAULT + 各 handler 产出非空任务图）
    - DynamicAgentTaskPlannerTest               7/7   通过（DEFAULT/BULL/BEAR/VOLATILE handler 选择 + 显式 plannerStrategy 覆盖 + registry 缺失回退 DEFAULT + 每种策略保留 JUDGE 终点）
    - ReflectionCheckpointServiceTest           7/7   通过（写入/排序/校验 stepIndex/必填 snapshotJson/ABORT 不替代 JudgeDecision/未知 runId 空集）

  Batch 3 / Batch 2 / Batch 1 回归保持全绿
    - dh-domain    Batch 1 35/35
    - dh-connector Batch 3 9/9
    - dh-usecase   Batch 2 + B4 28/28（feedback 15 + planner/reflection 28 等共 28 个 B4 用例 + 历史用例）
    - dh-api       Batch 2 WebMvc 7/7

  Stage1 回归
    - ResearchRunStage1ClosedLoopTest           1/1   通过（DefaultAgentTaskPlanner 仍直连，Stage1 行为不变）
    - DecisionHubFacadeImplTest                 1/1   通过（旧链路冒烟）
    - ArchitectureTest                          5/5   通过

跳过：
  - PostgresContainerSmokeTest                  因当前环境无 Docker，按命令显式排除

Batch 4 范围（零 NQ 仓库改动 / 零真实外部服务调用 / 零 dh-domain 改动 / 零 JDBC / 零前端 / 零 LLM / 零 TradingAgents Python 代码）：
  - dh-usecase 新增类       12 个
      agent/planner/PlannerStrategy
      agent/planner/PlannerStrategyResolver
      agent/planner/PlannerStrategyRegistry
      agent/planner/DynamicAgentTaskPlanner
      agent/planner/impl/DefaultPlannerStrategyResolver
      agent/planner/strategy/PlannerStrategyHandler
      agent/planner/strategy/DefaultPlannerStrategyHandler
      agent/planner/strategy/BullFocusedPlannerStrategyHandler
      agent/planner/strategy/BearFocusedPlannerStrategyHandler
      agent/planner/strategy/VolatileDiversifiedPlannerStrategyHandler
      agent/ReflectionCheckpointService + impl/DefaultReflectionCheckpointService
      agent/ReflectionEntryRepository + CheckpointEntryRepository
      agent/inmemory/InMemoryReflectionEntryRepository + InMemoryCheckpointEntryRepository
  - dh-usecase 新增测试     4 个（28 cases 全绿）
```

## 4. 历史验收：2026-05-25 Stage2-PoC-B3 IMPLEMENT

```text
日期：2026-05-25
阶段：Stage2-PoC-B3 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  Batch 3 新增（dh-connector）
    - FakeForecastToolAdapterTest               3/3   通过（happy path + symbol 空 + horizon 空）
    - FakeResearchDataAdapterTest               4/4   通过（happy path + symbols 空 + start>end + 空 dataTypes）
    - InMemoryResearchSnapshotStoreTest         2/2   通过（save+findById/findByTraceId、findBySymbolAndDateRange 命中/未命中）

  Batch 1 / Batch 2 回归保持全绿（与上一轮一致）
    - dh-domain    Batch 1 35/35
    - dh-usecase   Batch 2 15/15
    - dh-api       Batch 2 WebMvc 7/7

  Stage1 回归
    - ResearchRunStage1ClosedLoopTest           1/1   通过
    - DecisionHubFacadeImplTest                 1/1   通过（旧链路冒烟）
    - ArchitectureTest                          5/5   通过

跳过：
  - PostgresContainerSmokeTest                  因当前环境无 Docker，按命令显式排除

Batch 3 范围（零真实外部服务调用 / 零 NQ 仓库改动 / 零 JDBC / 零 dh-domain 改动 / 零 WiringConfig 改动）：
  - dh-connector 新增类     8 个
      tools/ForecastRequest, tools/ForecastToolPort, tools/fake/FakeForecastToolAdapter
      research/MarketSnapshotRequest, research/ResearchDataAdapter, research/ResearchSnapshotStore
      research/fake/FakeResearchDataAdapter, research/fake/InMemoryResearchSnapshotStore
  - dh-connector 新增测试   3 个 (9 cases 全绿)
  - dh-connector pom.xml    加 junit-jupiter (test scope)
```

## 4. 历史验收：2026-05-25 Stage2-PoC-B2 IMPLEMENT

```text
日期：2026-05-25
阶段：Stage2-PoC-B2 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  Batch 1 回归（dh-domain）
    - NqFeedbackEnvelopeTest                    4/4   通过
    - NqFeedbackPayloadContractTest             8/8   通过
    - DhBacktestRequestContractTest             5/5   通过
    - ForecastArtifactTest                      4/4   通过
    - ExternalMarketSnapshotTest                3/3   通过
    - ReflectionEntryTest                       3/3   通过
    - CheckpointEntryTest                       3/3   通过
    - JsonSchemaPresenceTest                    5/5   通过

  Batch 2 新增（dh-usecase）
    - NqFeedbackContractValidationTest          7/7   通过（8 eventType 合法 + 6 错误场景）
    - NqFeedbackIdempotencyTest                 3/3   通过（重放 / 不同 eventId / REJECTED 不入库）
    - NqFeedbackHandlerDispatchTest             5/5   通过（router 全覆盖 + 重复抛错 + Stage1 append + raw 保留）

  Batch 2 新增（dh-api）
    - NqFeedbackControllerWebMvcTest            7/7   通过（202/400 + outcome + trace/req/corr/job 分离 + bean 校验）

  Stage1 回归
    - ResearchRunStage1ClosedLoopTest           1/1   通过
    - DecisionHubFacadeImplTest                 1/1   通过（旧链路冒烟）
    - ArchitectureTest                          5/5   通过（Stage1-CLOSE 5 条规则保持）

跳过：
  - PostgresContainerSmokeTest                  因当前环境无 Docker，按命令显式排除
```

## 4. 历史验收：2026-05-25 Stage2-PoC-B1 IMPLEMENT

```text
日期：2026-05-25
阶段：Stage2-PoC-B1 IMPLEMENT
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  - dh-domain  NqFeedbackEnvelopeTest             4/4   通过
  - dh-domain  NqFeedbackPayloadContractTest      8/8   通过 (8 个 payload value object)
  - dh-domain  DhBacktestRequestContractTest      5/5   通过
  - dh-domain  ForecastArtifactTest               4/4   通过
  - dh-domain  ExternalMarketSnapshotTest         3/3   通过
  - dh-domain  ReflectionEntryTest                3/3   通过
  - dh-domain  CheckpointEntryTest                3/3   通过
  - dh-domain  JsonSchemaPresenceTest             5/5   通过 (16 个 schema 存在 + 结构校验)
  - dh-usecase ResearchRunStage1ClosedLoopTest    1/1   通过 (Stage1 回归)
  - dh-usecase DecisionHubFacadeImplTest          1/1   通过 (旧链路冒烟)
  - dh-app     ArchitectureTest                   5/5   通过 (Stage1-CLOSE 5 条规则保持)

跳过：
  - dh-app     PostgresContainerSmokeTest         因当前环境无 Docker，按命令显式排除

Batch 1 范围 (零 Controller/Service/Repository/JDBC/WiringConfig 改动)：
  - 新增 dh-domain 类     30 个 (含 8 payload + 5 enum 在 feedback / payload)
  - 新增 JSON Schema       16 个 (contracts/json-schema/)
  - 新增 OpenAPI schemas   23 项 (contracts/openapi.yaml components only, 无新 path)
  - 新增测试用例           35 个 (dh-domain/src/test, 全绿)
```

## 4. 历史验收：2026-05-25 Stage2-PoC WO

```text
日期：2026-05-25
阶段：Stage2-PoC WO
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  - dh-usecase  ResearchRunStage1ClosedLoopTest   1/1 通过
  - dh-usecase  DecisionHubFacadeImplTest         1/1 通过（旧链路冒烟）
  - dh-app      ArchitectureTest                  5/5 通过（旧 1 条 + Stage1-CLOSE 新增 4 条）

跳过：
  - dh-app      PostgresContainerSmokeTest        因当前环境无 Docker，按命令显式排除

说明：本轮 Stage2-PoC WO 只修改文档，未触碰 Java/SQL/Schema，Stage1 测试矩阵保持不变。
```

## 5. 历史验收：2026-05-25 Stage1-CLOSE

```text
日期：2026-05-25
阶段：Stage1-CLOSE
命令：mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果：BUILD SUCCESS

通过的关键测试：
  - dh-usecase  ResearchRunStage1ClosedLoopTest   1/1 通过
  - dh-usecase  DecisionHubFacadeImplTest         1/1 通过（旧链路冒烟）
  - dh-app      ArchitectureTest                  5/5 通过（旧 1 条 + Stage1-CLOSE 新增 4 条）

跳过：
  - dh-app      PostgresContainerSmokeTest        因当前环境无 Docker，按命令显式排除
```

## 4. Stage1 必测闭环

`ResearchRunStage1ClosedLoopTest` 覆盖：

```text
创建 ResearchRun
启动 ResearchRun
生成多个 StrategyCandidate
生成 JudgeDecision
接收 NQ Feedback Event（BACKTEST positive）
更新 ExperienceEntry / PheromoneEdge（success_count +1）
接收 NQ Feedback Event（RISK negative）
更新 ExperienceEntry（failure_count +1）+ FailureCaseStore
```

## 5. 边界测试（ArchUnit，全部由 dh-app/ArchitectureTest 覆盖）

```text
✅ ..domain.. 不依赖 ..infra..
✅ ..domain.. 不依赖 ..usecase.. / ..api.. / ..infra..（Stage1-CLOSE）
✅ ..connector.nq.. 类名/方法名禁字（placeOrder/submitOrder/executeOrder/
   bypassRisk/forceExecute），DefaultNqContractVerifier 自身黑名单豁免（Stage1-CLOSE）
✅ ..usecase.agent.. 不依赖 ..providers..（Stage1-CLOSE）
✅ ..api.. 控制器 @RequestMapping 不命中 /orders|/trades|/live（Stage1-CLOSE）
✅ ..connector.tools..  不依赖 ..infra..（Stage2-PoC-B5）
✅ ..connector.research.. 不依赖 ..infra..（Stage2-PoC-B5）
✅ ..domain.{forecast,marketdata,reflection,checkpoint}.. 不依赖 ..connector..（Stage2-PoC-B5）
✅ ..usecase.agent.planner.. 不依赖 ..providers..（Stage2-PoC-B5）
✅ ..usecase.agent.feedback.. 不依赖 ..providers..（Stage2-PoC-B5）
```

## 6. 验收记录格式

每次 VERIFY 后追加：

```text
日期
阶段
命令
结果
失败原因
修复结论
剩余风险
```

## 7. 2026-05-26 Stage2-PoC VERIFY 验收记录

```text
日期       2026-05-26
阶段       Stage2-PoC VERIFY (冻结前验证)
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS
           - dh-domain   35 tests   (JsonSchemaPresenceTest 5)
           - dh-connector 9 tests   (Fake adapter/store 全绿)
           - dh-usecase  47 tests   (Stage2ClosedLoopTest 2 / ResearchRunStage1ClosedLoopTest 1)
           - dh-infra     9 tests   (JdbcSqlFragments 5 / JdbcNqFeedback 4)
           - dh-api       7 tests   (NqFeedbackControllerWebMvcTest 7)
           - dh-app      15 tests   (ArchitectureTest 10 / V3MigrationPresenceTest 5)
           - 总计 122 tests / 0 failures / 0 errors / 0 skipped
失败原因   无
修复结论   契约/文档不一致项已修正：
           1) contracts/openapi.yaml /api/ai/feedback/nq 改为 202 + NqFeedbackAcceptedResponse
              / 400 + NqFeedbackErrorResponse 并补两个 schema
           2) docs/current/DB_SCHEMA.md 修正 V2 文件名为 V2__dh_agent_runtime.sql
           3) docs/current/API.md 把已实现的 7 条 research-runs 端点移入 "已实现端点"
剩余风险   - PostgresContainerSmokeTest 需 Docker，留给装好 Docker 的 CI 环境
           - 真实 NQ ingest endpoint 对齐留给 FREEZE 后阶段
           - OpenAPI 中 /api/ai/research-runs 端点未落 OpenAPI，列入 Stage3 文档补丁
准入决定   GO，允许进入 Stage2-PoC FREEZE
报告       docs/current/STAGE2_POC_VERIFY_REPORT.md
```

## 8. 2026-05-26 Stage2-PoC FREEZE 验收记录

```text
日期       2026-05-26
阶段       Stage2-PoC FREEZE (冻结前最终验收)
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS
           - 总计 122 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
冻结目录   docs/gates/dh-stage2-poc/ （已创建，含冻结声明 README.md）
文档同步   README.md / AGENTS.md / docs/current/README.md / STATUS.md
           / WORKLOG.md / TESTING.md 全部对齐：
           "Current stage: Stage2-PoC FREEZE completed / Next stage: Stage3-PLAN"
本次改动   仅文档；零 Java 业务代码变更；零 NQ 仓库变更；无 Stage3 功能
准入决定   进入 Stage3-PLAN（仅规划 NQ 真实联调，不实现）
```

## 9. 2026-05-26 Stage3-PLAN 回归记录

```text
日期       2026-05-26
阶段       Stage3-PLAN (文档规划阶段，无 Java 业务代码改动)
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 122 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
本次改动   仅新增 6 份 STAGE3_*.md 规划文档 + 6 份状态文档 bump；零 Java 业务代码改动
本次范围   仅 PLAN：不接真实 NQ / Kronos / global-stock-data；不引入 TradingAgents Python；
           不实现下单 / 风控旁路 / 实盘 / 前端；不修改 NQ 仓库
准入决定   进入 Stage3-WO（按 STAGE3_WORK_ORDER.md 拆批实施）
```

## 10. 2026-05-26 Stage3-B1 Contract Alignment IMPLEMENT 验收记录

```text
日期       2026-05-26
阶段       Stage3-B1 Contract Alignment IMPLEMENT
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS
           - dh-domain   64 tests   (Stage2 35 + 新增 4 份 contract 测试类 29 cases)
           - dh-connector 9 tests   (Fake adapter / store 全绿)
           - dh-usecase  47 tests   (Stage1ClosedLoopTest 1 / Stage2ClosedLoopTest 2 / feedback 15 等)
           - dh-infra     9 tests   (JdbcSqlFragments 5 / JdbcNqFeedback 4)
           - dh-api       7 tests   (NqFeedbackControllerWebMvcTest 7)
           - dh-app      15 tests   (ArchitectureTest 10 / V3MigrationPresenceTest 5)
           - 总计 151 tests / 0 failures / 0 errors / 0 skipped

新增测试   dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/
           - NqFeedbackEnvelopeSchemaContractTest      7 cases
             (schema 存在 / required 10 字段 / additionalProperties=false /
              eventType 枚举对齐 NqFeedbackEventType 8 值 / sourceSystem const /
              schemaVersion semver / 黑名单关键词为空)
           - DhBacktestRequestSchemaContractTest       7 cases
             (schema 存在 / required 14 字段 / additionalProperties=false /
              status 枚举对齐 DhBacktestRequestStatus 6 值 /
              frequency 枚举对齐 BacktestFrequency 3 值 /
              initialCapital exclusiveMinimum=0 + symbols minItems=1 / 黑名单为空)
           - BacktestResultSnapshotSchemaContractTest  6 cases
             (schema 存在 / required 9 字段 / additionalProperties=false /
              verdict 枚举对齐 BacktestVerdict 3 值 / winRate [0,1] / 黑名单为空)
           - OpenApiContractAlignmentTest              9 cases
             (openapi 存在 / 端点与 NqFeedbackController 一致 /
              outcome 枚举含 ACCEPTED + DUPLICATE /
              errorCode 枚举含 UNKNOWN_EVENT_TYPE + INVALID_SCHEMA + UNKNOWN_TRACE /
              DhBacktestRequest / DhBacktestRequestAccepted / DhBacktestResultSnapshot 组件存在 /
              NqFeedbackEventType 保持 8 种 /
              全文不含 placeOrder|submitOrder|executeOrder|bypassRisk|forceExecute /
              paths 段不含 /orders / /trades / /live /
              Stage3-B1 不允许在 paths 落 /api/ai/research/backtest-requests，仅注释占位)

ArchUnit   10/10 PASS（Stage1-CLOSE 5 + Stage2-PoC-B5 5；本批未新增也未放松）
失败原因   无
修复结论   - contracts/json-schema/nq-feedback-envelope.schema.json 补 description / examples，
             eventId / eventType / sourceSystem / traceId / requestId / correlationId /
             schemaVersion / payloadJson 9 字段对齐 STAGE3_CONTRACT_PLAN §1；
             不修改 required / enum / additionalProperties 等结构语义
           - contracts/json-schema/dh-backtest-request.schema.json 补 description；
             不修改 required / enum / additionalProperties
           - contracts/json-schema/dh-backtest-result-snapshot.schema.json 补 description；
             不修改 required / enum / additionalProperties
           - contracts/openapi.yaml info.description 加 Stage3-B1 硬边界声明；
             components 段保留 Stage3-B1 planned contract 注释占位；
             /api/ai/feedback/nq 端点语义不变
剩余风险   - PostgresContainerSmokeTest 需 Docker，留给装好 Docker 的 CI 环境
           - NQ 端 /api/ai/research/backtest-requests 实施由 NQ 团队后续完成（Stage3-B2/B3 规划与对接）
           - Stage3-B1 不修改任何 Handler 行为，经验沉淀路径（ExperienceEntry/PheromoneEdge/
             FailureCaseStore 写入）保留至后续 Batch 在 dh-usecase / dh-memory 实施
准入决定   进入 Stage3-B2 NQ Feedback Outbox PLAN（仅文档；NQ 仓库由 NQ 团队后续实施）
```

## 11. 2026-05-26 Stage3-B2 NQ Feedback Outbox PLAN 回归记录

```text
日期       2026-05-26
阶段       Stage3-B2 NQ Feedback Outbox PLAN（仅文档规格阶段，无 Java 业务代码改动）
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
本次改动   仅新增 docs/current/STAGE3_NQ_OUTBOX_SPEC.md（NQ outbox 11 段完整规格）
           + 6 份状态文档 bump（README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING）
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
           零 contracts/json-schema/*.schema.json 修改；零 Flyway migration 新增；
           零 OpenAPI path 新增
本次范围   仅 PLAN：声明 NQ 端 outbox SPEC（建议模块 / 表结构 / 8 触发点 / retry 矩阵 /
           audit / 5 字段语义 / HTTP 矩阵 / NQ 后续 5 个 Batch / 风险防护 / 验收）；
           不接真实 NQ / Kronos / global-stock-data；不引入 TradingAgents Python；
           不实现下单 / 风控旁路 / 实盘 / 前端；不修改 NQ 仓库；不写真实 outbox 客户端
准入决定   进入 Stage3-B3 DH Backtest Request Adapter PLAN（仅 PLAN；不写 Java；不联调真实 NQ）
```

## 12. 2026-05-26 Stage3-B3 DH Backtest Request Adapter PLAN 回归记录

```text
日期       2026-05-26
阶段       Stage3-B3 DH Backtest Request Adapter PLAN（仅文档规格阶段，无 Java 业务代码改动）
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
本次改动   仅新增 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md（DH backtest adapter 14 段完整规格）
           + 6 份状态文档 bump（README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING）
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
           零 contracts/json-schema/*.schema.json 修改；零 Flyway migration 新增；
           零 OpenAPI path 新增；零真实 HTTP / Kronos / global-stock-data / TradingAgents Python
本次范围   仅 PLAN：声明 DH 端 adapter SPEC（可插拔原则 10 条 + 三层 gate +
           三 client 策略 / 9 状态机 / 24h 幂等 + 8 attempt 退避 / 错误码映射 /
           result snapshot 消费 / 三段配置建议 / 8 个测试类规划 / B3-1..B3-5 五批 IMPL 拆解 /
           风险与防护）；
           不写 Java 业务代码；不联调真实 NQ；不接真实 HTTP；不实现下单 / 风控旁路 /
           实盘 / 前端；不修改 NQ 仓库；不写真实 backtest client
准入决定   进入 Stage3-B4 End-to-End Contract Test PLAN（仅 PLAN；不接实盘；不真实联调；
           联调用例 T1-T7 在 Stage3-B4 IMPLEMENT / VERIFY 阶段落地）
```

## 13. 2026-05-26 Stage3-B4 End-to-End Contract Test PLAN 回归记录

```text
日期       2026-05-26
阶段       Stage3-B4 End-to-End Contract Test PLAN（仅文档规格阶段，无 Java 业务代码改动）
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
本次改动   仅新增 docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md（DH/NQ 端到端契约测试 11 段完整规格）
           + 6 份状态文档 bump（README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING）
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
           零 contracts/json-schema/*.schema.json 修改；零 Flyway migration 新增；
           零 OpenAPI path 新增；零真实 HTTP / 真实联调 / 实盘 / Kronos /
           global-stock-data / TradingAgents Python
本次范围   仅 PLAN：DH staging + NQ test cluster 环境规划 / 7 个联调用例 T1-T7 /
           10 类 Contract Test（JSON Schema / OpenAPI / HTTP status matrix / Error code /
           Idempotency / Retry+dead-letter / Disabled startup / No dangerous endpoint /
           Trace correlation / Regression）/ 5 字段端到端对账 + deterministic 数据 /
           三段验收命令（DH 默认 / CI Docker / Stage3 联调）/ 失败处理矩阵 +
           联调回滚预案 / B4-1..B4-5 五批 IMPL 拆解 / Stage3-PLAN-FREEZE 衔接；
           不写 Java；不联调真实 NQ；不接真实 HTTP；不接实盘；不自动下单 / 发布
准入决定   进入 Stage3-PLAN-FREEZE（评审 10 份 STAGE3_*.md 文档口径一致性；视需要冻结到
           docs/gates/dh-stage3-plan/；6 份状态文档切到 "Stage3-PLAN-FREEZE completed /
           Next: Stage3-B1 IMPLEMENT" 体例）
```

## 14. 2026-05-26 Stage3-PLAN-FREEZE 验收记录

```text
日期       2026-05-26
阶段       Stage3-PLAN-FREEZE（仅文档冻结，无 Java 业务代码改动）
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 151 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 10/10 PASS
           - dh-domain 64 / dh-connector 9 / dh-usecase 47 / dh-infra 9 / dh-api 7 / dh-app 15
           - Stage1ClosedLoop / Stage2ClosedLoop / Stage3-B1 29 contract tests 全部保持回归基线
本次改动   仅文档冻结：
           - docs/current/* 33 个文件完整复制到 docs/gates/dh-stage3-plan/（含 10 份 STAGE3_*.md）
           - docs/gates/dh-stage3-plan/README.md 顶部加冻结声明（Status: FREEZE completed /
             Next: Stage3-B1 IMPLEMENT）+ 一致性核查表 + Stage3-PLAN 交付物清单
           - 6 份状态文档 bump（README / AGENTS / docs/current/README / STATUS / WORKLOG / TESTING）
           零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
           零 contracts/json-schema/*.schema.json 修改；零 Flyway migration 新增；
           零 OpenAPI path 新增；零真实 HTTP / 真实联调 / 实盘 / Kronos /
           global-stock-data / TradingAgents Python 接入
本次范围   文档冻结：10 份 STAGE3_*.md 一致性核查（9 条核心原则口径一致；无措辞修订）；
           落盘 docs/gates/dh-stage3-plan/；状态文档同步；mvn test 回归基线保持
           本轮为文档冻结，无代码实现；冻结后不得修改本目录内容
准入决定   进入 Stage3-B1 IMPLEMENT（Stage3-B1 Contract Alignment IMPLEMENT 已于 2026-05-26 完成；
           Stage3-B2/B3/B4 IMPLEMENT 单独开工，按 STAGE3_WORK_ORDER / NQ_OUTBOX_SPEC §8 /
           DH_BACKTEST_ADAPTER_SPEC §12 / E2E_CONTRACT_TEST_SPEC §8 推进）
```

## 15. 2026-05-26 Stage3-B3 DH Backtest Request Adapter IMPL 验收记录

```text
日期       2026-05-26
阶段       Stage3-B3 DH Backtest Request Adapter IMPL
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS / 190 tests / 0 failures / 0 errors / 0 skipped
           - ArchUnit 12/12 PASS（新增 R11 HTTP 客户端隔离 / R12 backtest 端口隔离）
           - 151 → 190（+39 new tests）

本次新增（Java 业务代码）：
  dh-usecase (新建 com.guidinglight.decisionhub.usecase.agent.backtest 包)
    - DhBacktestRequestService 端口
    - impl/DefaultDhBacktestRequestService（校验 + paramsHash sha256 + 24h 短路 +
      不抛 RuntimeException + 状态机映射）
    - DhBacktestRequestCommand（Builder 风格 12 字段）
    - DhBacktestRequestResult（4 工厂方法：accepted/duplicate/idempotentShortCircuit/disabled/failed）
    - DhBacktestRequestOutcome 枚举（6 值）
    - DhBacktestRequestErrorCode 枚举（17 值 + isRetryable）
    - DhBacktestRequestRepository 端口 + inmemory/InMemoryDhBacktestRequestRepository
  dh-connector
    - nq/NqBacktestSubmitStatus 枚举（4 值：ACCEPTED/DUPLICATE/DISABLED/FAILED）
    - nq/NqBacktestSubmitResult（5 工厂方法 + 6 字段）
    - nq/NqBacktestClient.submit(DhBacktestRequest) 默认方法（typed）
    - nq/fake/FakeNqBacktestClient typed deterministic submit
      （jobId = "fake-job-" + sha256(requestId).take(16) + Clock 可注入）
    - nq/fake/DisabledNqBacktestClient（DH gate 关闭；返回 DISABLED 不抛异常）
  dh-app
    - config/NqBacktestClientProperties (@ConfigurationProperties)
    - config/Stage3NqBacktestWiringConfig（互斥 SpEL 三层 gate 装配）
    - config/AgentRuntimeWiringConfig 移除 nqBacktestClient bean（由 Stage3 接管）

本次新增（测试，共 8 个测试类 39 cases）：
  dh-connector
    - FakeNqBacktestClientTest                                              5
    - DisabledNqBacktestClientTest                                          5
  dh-usecase
    - DhBacktestRequestServiceTest                                          5
    - DhBacktestRequestIdempotencyTest                                      3
    - DhBacktestResultSnapshotConsumptionTest                               4
  dh-app
    - RealNqBacktestClientDisabledByDefaultTest                             4
      （4 profile case 全覆盖装配真值表）
    - NoNqDependencyStartupTest                                             4
      （默认 profile / Spring context / 无 HTTP 客户端 bean / 零 Spring 手动构造）
  dh-domain
    - NoDangerousEndpointContractTest                                       6
      （openapi.yaml 无危险关键词 + paths 无危险段 + 16 schemas 无危险关键词 +
       DhBacktestRequestStatus / NqFeedbackEventType 无危险前缀 + 事件类型保持 8 值）
  dh-app ArchitectureTest                                                   2 (新增 R11 / R12)

本次范围：
  - 仅 DH-side IMPL；Fake / Disabled 装配；零真实 HTTP；零 RealNqBacktestClient
  - Stage3-B3 was executed before B2 because B2 touches NQ
  - NQ repository remains unchanged
  - 零 contracts/openapi.yaml 修改；零 contracts/json-schema 修改；
    零 Flyway migration 新增；零 OpenAPI path 新增

准入决定   Stage3-B3 IMPL completed；
           Next: Stage3-B2 NQ Feedback Outbox IMPL, blocked until NQ GateJ-FREEZE
           or isolated branch approval。
```

## 16. 2026-06-06 DH Codex Workflow Rules 验收记录

```text
日期       2026-06-06
阶段       DH-CODEX-WORKFLOW（仅文档规则固化）
命令       git status --short
结果       已执行；用于确认本轮只出现允许范围内的文档与规则文件变更
命令       git diff --check
结果       已执行；用于确认 diff 无 whitespace error
命令       定向 rg 检查
结果       已执行；检查 nq-dh-workflow-router active skill、router skill 文件存在、
           CODEX_PROJECT_INSTRUCTIONS 前置分类规则、Findings 标准字段、Summary 非必填、
           integration 未写成 started / completed、无新增业务代码路径
失败原因   无
修复结论   不适用
剩余风险   本轮未运行 mvn test；原因是任务类型为 DOCUMENTATION，且禁止修改业务代码
准入决定   Codex workflow routing 规则固化完成；NQ integration not started；
           Integration-0 not started / plan only
```

## 17. 2026-06-06 DH Codex Workflow Conflict Cleanup 验收记录

```text
日期       2026-06-06
阶段       DH-CODEX-WORKFLOW-CLEANUP（仅 Markdown / Skill 文档冲突修复）
命令       git status --short
结果       已执行；用于确认本轮只出现允许范围内的文档与规则文件变更
命令       git diff --check
结果       已执行；用于确认 diff 无 whitespace error
命令       定向文本检查
结果       已执行；检查 ROADMAP / WORK_ORDER next 已统一为 Integration-0-PLAN；
           Stage2-PoC / 真实 NQ client / HTTP / event / backtest request 已标记为
           historical / superseded / deferred / forbidden；
           workflow 文档包含完整排除目录、细凭证禁令、开工前范围字段和
           archived / historical / superseded 文档不作为当前事实源规则
失败原因   无
修复结论   不适用
剩余风险   本轮未运行 mvn test；原因是任务类型为 DOCUMENTATION，且只修改 Markdown / Skill 文档，
           未修改业务代码、API、migration、provider、NQ client、RealClient 或交易路径
准入决定   下一步只允许 Integration-0-PLAN；NQ integration not started；
           Integration-0 not started / plan only
```

## 18. 2026-06-06 DH Codex Workflow Final Cleanup 验收记录

```text
日期       2026-06-06
阶段       DH-CODEX-WORKFLOW-FINAL-CLEANUP（仅 Markdown 文档口径修复）
命令       git status --short
结果       已执行；用于确认本轮只出现允许范围内的 Markdown / Skill 文档变更，
           未出现业务代码、API、migration、provider、NQ client、RealClient 或交易路径变更
命令       git diff --check
结果       已执行；用于确认 diff 无 whitespace error
命令       定向文本检查
结果       已执行；检查 AGENTS.md / README.md / docs/current/README.md 不再把 Stage3-B2
           写成当前 next；docs/current/DH_NQ_INTEGRATION.md 已把 REST API /
           POST /api/ai/backtest-requests / 真实 HTTP / event / NQ client /
           RealClient / real provider 标记为 historical / superseded / deferred / gated
失败原因   无
修复结论   不适用
剩余风险   本轮未运行 mvn test；原因是任务类型为 DOCUMENTATION，且只修改 Markdown，
           未修改业务代码、API、migration、provider、NQ client、RealClient 或交易路径
准入决定   下一步只允许 Integration-0-PLAN；NQ integration not started；
           Integration-0 not started / plan only
```

## 19. 2026-06-11 DOC-SYNC-GATEK-PRE-AND-INT0-REGISTRATION 验收记录

```text
日期       2026-06-11
阶段       DOC-SYNC-GATEK-PRE-AND-INT0-REGISTRATION（仅事实源文档同步）
命令       git status --short
结果       已执行；仅命中本轮同步的 docs/current/{STATUS,README,ROADMAP,WORKLOG,TESTING}.md
           与 AGENTS.md，无业务代码、API、migration、provider、NQ client、RealClient 或交易路径变更
命令       git diff --check
结果       已执行；diff 无 whitespace error
命令       git diff --stat
结果       已执行；用于核对改动集中在事实源 Markdown 文件
全量测试   未执行；任务类型为 DOCUMENTATION，仅改 Markdown，未修改 Java、契约、migration 或部署代码
失败原因   无
修复结论   不适用
剩余风险   阶段口径误写风险已通过禁止项控制：未把 Integration-0 写成真实集成；
           未把 NQ integration 写成 started；未把 DH 写成 integrated；未把 LIVE 写成 enabled
准入决定   DH Next 仍为 Integration-0-PLAN；NQ-DH not integrated；
           Integration-0 = contract / mock / docs work line, not runtime integration；
           P1-4 残留阻塞 Integration-1，不阻塞 Integration-0
```

## 20. 2026-06-11 NQ-DH-INTEGRATION-0-CONTRACT-FREEZE 验收记录

```text
日期       2026-06-11
阶段       NQ-DH-INTEGRATION-0-CONTRACT-FREEZE（DOCUMENTATION + CONTRACT DESIGN）
新增       docs/current/DH_NQ_INTEGRATION0_CONTRACT_FREEZE.md
           docs/current/DH_NQ_INTEGRATION0_SECURITY_POLICY.md
           docs/current/DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md
修改       docs/current/README.md / ROADMAP.md / WORKLOG.md / TESTING.md
命令       git status --short
结果       已执行；仅命中本轮新增/修改的 docs/current Markdown，无业务代码、契约代码、
           migration、provider、NQ client、RealClient 或交易路径变更
命令       git diff --check
结果       已执行；diff 无 whitespace error
命令       git diff --stat
结果       已执行；改动集中在 docs/current/DH_NQ_INTEGRATION0_*.md 与 README/ROADMAP/WORKLOG/TESTING
全量测试   未执行；本轮 docs + contract design only，未修改 Java、contracts/ schema、
           migration、测试代码或部署脚本
失败原因   无
修复结论   不适用
剩余风险   文档契约与未来代码实现可能脱节，后续必须用 contract test 固化；
           本轮未实现集成、未接真实 HTTP / RealClient / 真实 Provider、未开启 LIVE
准入决定   下一步只允许 Integration-0 mock / contract test 设计或安全文档固化，禁止真实联调；
           真实通道必须等 Integration-1 并先修复 DH P1-4 残留
           （rate limit / memory cap / replay nonce 持久化）
```

## 21. 2026-06-11 NQ-DH-INTEGRATION0-MOCK-CONTRACT-TEST-DESIGN 验收记录

```text
日期       2026-06-11
阶段       NQ-DH-INTEGRATION0-MOCK-CONTRACT-TEST-DESIGN（DOCUMENTATION + CONTRACT TEST DESIGN）
修改       docs/current/DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md（新增详细矩阵 §6-§12）
           docs/current/README.md / ROADMAP.md / WORKLOG.md / TESTING.md
命令       git status --short
结果       已执行；仅命中本轮修改的 docs/current Markdown，无业务代码、contracts schema、
           migration、provider、NQ client、RealClient 或交易路径变更
命令       git diff --check
结果       已执行；diff 无 whitespace error
命令       git diff --stat
结果       已执行；改动集中在 DH_NQ_INTEGRATION0_CONTRACT_TEST_PLAN.md 与 README/ROADMAP/WORKLOG/TESTING
全量测试   未执行；本轮 docs + contract test design only，未写测试代码，未修改 Java、
           contracts schema、migration 或部署脚本
代码文件   未创建；futureCodeLocationSuggestion 仅为建议路径，未创建任何 .java / 测试 / fixture 文件
失败原因   无
修复结论   不适用
剩余风险   文档测试矩阵与未来测试代码可能脱节，后续 NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL
           必须按本矩阵固化；本轮未实现集成、未接真实 HTTP / RealClient / 真实 Provider、未开启 LIVE
准入决定   下一步可进入 contract test 代码实现（草案，只加测试与 fixture、走 Fake/Disabled，
           不接真实通道）；真实通道必须等 Integration-1 并先修复 DH P1-4 残留
```

## 24. 2026-06-12 DH-P1-4-RESIDUAL-FIX-PLAN 验收记录

```text
日期       2026-06-12
阶段       DH-P1-4-RESIDUAL-FIX-PLAN（DOCUMENTATION + FIX_PLAN）
新增       docs/current/DH_P1_4_RESIDUAL_FIX_PLAN.md
修改       docs/current/README.md / ROADMAP.md / WORKLOG.md / TESTING.md
命令       git status --short / git diff --check / git diff --stat
结果       已执行；仅命中 docs/current Markdown；无 whitespace error；无业务/测试代码改动
全量测试   未执行；本轮 docs-only fix planning，未改 Java、测试代码、API、migration、provider、NQ client
失败原因   无
修复结论   不适用（本轮仅出方案，未修复任何缺口）
剩余风险   方案与未来实现可能漂移；rate limit 阈值、fail-closed、TTL 优先于驱逐等须在实现阶段用测试固化
准入决定   Integration-1 仍 NOT STARTED；P1-4 未修复前禁止真实只读通道 / 真实 HTTP / RealClient；
           下一步 DH-P1-4-RESIDUAL-FIX-REVIEW 或 DH-P1-4-RESIDUAL-FIX-IMPL，不得直接 Integration-1
```

## 23. 2026-06-12 NQ-DH-INTEGRATION0-SAFETY-GATE-CLOSE 验收记录

```text
日期       2026-06-12
阶段       NQ-DH-INTEGRATION0-SAFETY-GATE-CLOSE（DOCUMENTATION + ACCEPTANCE_REPORT）
新增       docs/current/DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md
修改       docs/current/STATUS.md / README.md / ROADMAP.md / WORKLOG.md / TESTING.md
命令       git status --short
结果       已执行；仅命中本轮新增/修改的 docs/current Markdown
命令       git diff --check
结果       已执行；无 whitespace error
命令       git diff --stat
结果       已执行；改动集中在 acceptance report 与 STATUS/README/ROADMAP/WORKLOG/TESTING
全量测试   未执行；本轮 docs-only，未改业务/测试代码；验收依据引用上一轮 mvn test BUILD SUCCESS
           （dh-domain 86 tests / 0 failures，Integration-0 16 passed，ArchitectureTest 12 条全绿）
失败原因   无
修复结论   不适用
验收结论   Integration-0 PASS / CLOSED / ACCEPTED；Runtime integration / Integration-1 / AI NOT STARTED；
           DH NOT INTEGRATED；LIVE DISABLED
剩余风险   Integration-0 只证明 contract / test-only 安全边界，不证明真实通道安全；
           Integration-1 前必须修复 DH P1-4 residual 并重跑 contract tests
准入决定   下一步只允许 Integration-1 planning-only audit / DH P1-4 residual fix planning /
           NQ GateK-PLAN 文档规划；禁止直接真实联调
```

## 22. 2026-06-12 NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL 验收记录

```text
日期       2026-06-12
阶段       NQ-DH-INTEGRATION0-CONTRACT-TEST-IMPL（TEST_CODE_CHANGE）
新增       dh-domain/src/test/java/.../integration0/support/（9 个 test-only helper）
           dh-domain/src/test/java/.../integration0/（3 个测试类，16 用例覆盖 INT0-T01..T15）
           dh-domain/src/test/resources/integration0/（10 个脱敏 fixture JSON）
命令       mvn test
结果       BUILD SUCCESS；全仓回归全绿；DhNqIntegration0*Test 16 passed / 0 failed；
           ArchitectureTest（ArchUnit 12 条）全绿；PostgresContainerSmokeTest 因无 Docker skip（既有）
命令       mvn -pl dh-domain -am -Dtest='DhNqIntegration0*Test' -Dsurefire.failIfNoSpecifiedTests=false test
结果       16 tests / 0 failures / 0 errors
命令       git diff --check
结果       已执行；无 whitespace error
命令       git status --short
结果       已执行；仅命中 dh-domain/src/test/**（测试代码与 fixtures）
生产代码   未修改 src/main；未新增 API / migration / Controller / Service / Repository / DTO / RealClient / 真实 Provider
真实通道   未做真实 HTTP / 真实 NQ / 真实交易所；未读取真实密钥（固定假值）；未开启 LIVE
失败原因   无
剩余风险   nonce store 为 test-only 内存实现，不代表真实通道安全；Integration-1 前必须补
           持久化 nonce、rate limit、memory cap（DH P1-4 residual）
准入决定   下一步进入 Integration-0 contract test implementation review / safety gate review，
           不得直接真实联调
```

## 23. 2026-06-12 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1 验收记录（replay nonce persistence）

```text
日期       2026-06-12
阶段       DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1（CODE_CHANGE + SECURITY_FIX + DB_MIGRATION）
范围       只实现 replay nonce persistence；不实现 rate limit / memory cap / header alignment
新增测试   dh-security NonceReplayGuardTypeTest（5 用例）
             - explicit_jdbc_is_allowed_under_any_profile
             - in_memory_guard_only_dev_test（非 dev/test 显式 in-memory 启动失败）
             - missing_config_does_not_fallback_to_unsafe_in_memory（缺配置非 dev/test 默认 jdbc）
             - unknown_guard_type_fails_closed
             - isDevOrTest_detects_dev_or_test_profiles_only
           dh-infra JdbcNonceReplayGuardTest（5 用例，Mockito，无需 Docker）
             - markIfAbsent insert 1 -> true 且 SQL 含 on conflict (replay_key) do nothing
             - markIfAbsent insert 0 -> false（replay detected）
             - replay_store_unavailable_fails_closed（DataAccessException -> false，无 secret/payload 拼进 SQL）
             - cleanup_runs_before_mark_when_enabled / cleanup_skipped_when_disabled
           dh-infra JdbcNonceReplayGuardPersistenceTest（3 用例，Testcontainers，需 Docker）
             - persistent_nonce_rejects_replay_after_restart_simulation
             - persistent_nonce_is_source_tenant_request_scoped
             - cleanup_removes_expired_rows_only
命令       mvn test
结果       BUILD SUCCESS；全仓回归全绿；INT0-T01..T15（DhNqIntegration0*Test 16 用例）未被破坏；
           NonceReplayGuardTypeTest 5 / JdbcNonceReplayGuardTest 5 全绿；
           JdbcNonceReplayGuardPersistenceTest 因无 Docker 整类 skip（3）；PostgresContainerSmokeTest 无 Docker skip（既有）
说明       CI 需要 Docker 才能验证持久化 nonce 的 restart 语义（JdbcNonceReplayGuardPersistenceTest），
           本机无 Docker 时按 @Testcontainers(disabledWithoutDocker=true) 既有策略跳过。
命令       mvn -Pquality validate
结果       FAILURE，来源为既有/环境问题，非本轮改动：聚合模块 checkstyle 读 suppressions.xml 网络超时；
           spotless:check 在多个**未改动**既有文件即报 format 违规（基线本身不干净）。
           已对本轮自有文件单独 spotless:apply（-DspotlessFiles 限定），未触碰未改动文件。
命令       git diff --check / git status --short
结果       无 whitespace error；改动仅落在 dh-app/dh-infra/dh-security 允许范围与 docs/current
真实通道   未做真实 HTTP / 真实 NQ / 真实交易所；未读取真实密钥（固定假值）；未开启 LIVE；未接 AI
失败原因   无
剩余风险   replay nonce 已持久化，但 rate limit / memory cap 仍残留（P1-4 未全部关闭）；
           DB 不可用时 guard fail-closed（拒绝），属预期保护，需运维保障 DB 可用
准入决定   Integration-1 仍 NOT STARTED；下一步 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2（bounded memory cap），
           不得直接真实联调
```

## 25. 2026-06-12 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2 验收记录（bounded memory cap）

```text
日期       2026-06-12
阶段       DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2（CODE_CHANGE + SECURITY_FIX + TEST_CODE_CHANGE）
范围       只实现 bounded memory cap；不实现 rate limit / header alignment；不启动 Integration-1
本次改动（main）
  - dh-security InMemoryNonceReplayGuard 改为有界：maxEntries 全局上限 + TTL；
    markIfAbsent 先 TTL 清理 -> 命中拒绝 -> 满容量 fail-closed（绝不驱逐未过期 key）；
    有效期取 laterOf(认证层 expiresAt, now+ttl) 只延长不缩短防重放窗口；非法配置构造抛 IllegalArgumentException
  - dh-usecase InMemoryNqFeedbackEventRepository 改为有界：maxEvents 全局 + perTenantMaxEvents +
    retention TTL；append 前先 TTL 清理再按 tenant/global 驱逐最老项；envelope 幂等 + 有界；
    按域不变量与项目规范保留原始 payloadJson，不新增 secret/token 存储；非法配置构造抛 IllegalArgumentException
  - dh-app SecurityWiringConfig / AgentRuntimeWiringConfig 注入保守默认上限/TTL；application.yml 新增
    replay.in-memory.{max-entries,ttl-seconds} 与 feedback-store.{max-events,per-tenant-max-events,retention-seconds}
新增测试
  - dh-security BoundedInMemoryNonceReplayGuardTest（7）：memory_cap_rejects_overflow /
    ttl_cleanup_allows_new_nonce_after_expiry / does_not_evict_unexpired_nonce_window /
    replay_same_key_still_rejected / ttl_floor_never_shortens_replay_window /
    bad_config_fails_closed / default_constructor_is_bounded
  - dh-security NqFeedbackPayloadSizeGateTest（2）：payload_64kib_gate_remains_valid /
    payload_at_64kib_passes_size_gate
  - dh-usecase BoundedInMemoryNqFeedbackEventRepositoryTest（7）：rejects_or_bounds_overflow /
    ttl_cleanup_removes_expired_events / is_per_tenant_bounded /
    existing_query_semantics_preserved_sorted_by_received_at / save_envelope_is_idempotent_and_bounded /
    bad_config_fails_closed / default_constructor_is_bounded
命令       mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
结果       BUILD SUCCESS；7 模块全 SUCCESS；全仓回归全绿
           - dh-domain 86（INT0-T01..T15 = DhNqIntegration0*Test 16 用例未被破坏）
           - dh-security 26（Bounded 7 + PayloadSizeGate 2 + 既有）
           - dh-usecase 66（Bounded repo 7 + 既有）
           - dh-app 25（ArchitectureTest 12 / NoNqDependencyStartup 4 等，装配通过）
           - 本次 runner 恰好有 Docker：JdbcNonceReplayGuardPersistenceTest 3 用例真实跑通（未 skip）
命令       mvn -Pquality validate
结果       未在本轮重跑/未强制修复；既有 quality-baseline 问题（checkstyle DTD 网络 + spotless 基线）
           已登记为独立任务 DH-QUALITY-BASELINE-CLEANUP；本轮未顺手修未改动文件
命令       git diff --check / git status --short
结果       无 whitespace error（仅 LF→CRLF 提示）；改动仅落在 dh-app/dh-security/dh-usecase 允许范围与 docs/current
边界       未修改 NQ；未实现 rate limit；未做 header alignment；未新增 API / migration / RealClient /
           真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所调用；未接 AI；未开启 LIVE；未读取真实密钥
剩余风险   bounded in-memory 仅适合 dev/test 或单实例辅助路径，真实通道仍应用 JdbcNonceReplayGuard；
           容量满时的保护性拒绝属 fail-closed（拒绝而非放行），需运维监控容量与 TTL
准入决定   Integration-1 仍 NOT STARTED；P1-4 仍未全部关闭（rate limit 残留）；
           下一步 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2-REVIEW，再进入 Batch 3 rate limit，不得直接真实联调
```

### 25.1 2026-06-13 Batch 2 flaky 测试修复复验

```text
日期       2026-06-13
现象       2026-06-13 运行 mvn test 时 dh-usecase 失败 1 例：
           BoundedInMemoryNqFeedbackEventRepositoryTest.existing_query_semantics_preserved_sorted_by_received_at
           AssertionFailedError: expected:<3> but was:<1>（surefire-reports 已确认）
根因       该用例用默认构造（真实 Clock.systemUTC()），事件 receivedAt 固定为 T0=2026-06-12，
           默认 retention=24h。墙钟跨过 T0+24h 后，每次 append 的 TTL 清理把先前事件按 retention 过期驱逐，
           只剩最后一条 -> 时间依赖（flaky）用例，非 bounded memory cap 生产语义缺陷
修复       仅改该测试：注入固定 MutableClock(T0)（与同类其余用例一致），事件 age 远小于 retention，
           三条均保留，仅验证排序语义；生产代码与 retention/上限语义未改（test-only，+5/-1）
命令       mvn test（root，全 19 模块）
结果       BUILD SUCCESS；BoundedInMemoryNqFeedbackEventRepositoryTest 7/7、
           BoundedInMemoryNonceReplayGuardTest 7/7、NqFeedbackPayloadSizeGateTest 2/2、
           DhNqIntegration0*（INT0）6+2+8=16/16 全绿
命令       mvn -Pquality validate（root）
结果       BUILD SUCCESS（本轮未引入 quality 违规）
命令       git diff --check / git diff --stat
结果       无 whitespace error；1 file changed, 5 insertions(+), 1 deletion(-)
边界       未修改 NQ；未实现 rate limit；未做 header alignment；未新增 API / migration / RealClient /
           真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取真实密钥
准入决定   Integration-1 仍 NOT STARTED；P1-4 仍未全部关闭（rate limit 残留）
```

## 26. 2026-06-13 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3 验收记录（inbound rate limit / 429）

```text
日期       2026-06-13
阶段       DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3（CODE_CHANGE + SECURITY_FIX + TEST_CODE_CHANGE）
范围       只实现 NQ feedback 入站 rate limit / 429；不做 header alignment；不启动 Integration-1；不做真实联调
本次改动（main）
  - dh-security 新增 RateLimiter（端口）/ RateLimitResult（独立结果模型，不污染 HMAC authenticator）/
    InMemoryRateLimiter（固定窗口 + maxKeys 有界 + 窗口 TTL 清理优先 + 满容量 fail-closed + 非法配置启动失败）
  - dh-api NqFeedbackController：限流前置于 HMAC authenticator；key=source+tenant+route；超限 429 + errorCode
    RATE_LIMITED；审计 log.warn(auditCode/tenant/source/route/traceId，不含阈值/窗口/计数/密钥）；
    保留 HMAC/timestamp/nonce/replay 409/payload 413/成功 202 既有语义
  - dh-app SecurityWiringConfig 注入 RateLimiter bean（保守默认 window=1s / max-requests=20 / max-keys=10000；
    非法配置启动失败）；application.yml 新增 rate-limit.{window-seconds,max-requests,max-keys}
  - 配置加固（P2-1）：feedback-store.per-tenant-max-events 默认 10000 -> 1000（< 全局上限），
    仅改默认值（application.yml + AgentRuntimeWiringConfig @Value），不改 bounded memory cap 主逻辑
新增测试
  - dh-security InMemoryRateLimiterTest（6）：returns_limited_after_threshold / tenant_source_isolated /
    store_is_bounded_fail_closed（maxKeys=1）/ ttl_cleanup_allows_new_key_after_window /
    invalid_config_fails_closed / check_requires_no_secret_or_payload（no_credential_access 契约）
  - dh-api NqFeedbackRateLimitWebMvcTest（3）：returns_429_with_rate_limited /
    does_not_leak_internal_thresholds（429 body 无 window/maxRequests/maxKeys/count/threshold/retry-after/secret）/
    rate_limited_request_does_not_invoke_downstream_ingestion（no_trading_side_effect 代理）
  - 既有 NqFeedbackControllerWebMvcTest 更新构造器（注入宽松 limiter），15/15 仍全绿；
    payload 64KiB gate 由 NqFeedbackPayloadSizeGateTest（2）+ 既有 413 用例保持有效
命令       mvn test
结果       BUILD SUCCESS；全仓回归全绿
           - InMemoryRateLimiterTest 6/6、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackControllerWebMvcTest 15/15
           - NqFeedbackPayloadSizeGateTest 2/2、BoundedInMemoryNonceReplayGuardTest 7/7、
             BoundedInMemoryNqFeedbackEventRepositoryTest 7/7（Batch 2 未破坏）
           - INT0-T01..T15 = DhNqIntegration0*（6+2+8）16/16 未破坏；ArchUnit 全绿
           - 无 Docker 的 runner：JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1
             按 disabledWithoutDocker 跳过（非失败）
命令       mvn -Pquality validate
结果       BUILD SUCCESS（本轮未引入 quality 违规）
命令       git diff --check / git status --short
结果       无 whitespace error（仅 LF→CRLF 提示）；改动仅落在 dh-api/feedback + dh-api/test + dh-app/config +
           application.yml + dh-security/nq + dh-security/test 允许范围与 docs/current
边界       未修改 NQ；未做 header alignment；未新增 API 路径；未新增 migration / RealClient / 真实 Provider；
           未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取或输出真实密钥
剩余风险   in-memory limiter 仅适合 dev/test 或单实例辅助路径，真实多实例需集中式（Redis）limiter（另起任务）；
           容量满 / 超阈值的保护性拒绝属 fail-closed，需运维监控阈值与命中率；阈值需按真实流量调优
准入决定   Integration-1 仍 NOT STARTED；P1-4 三项残留实现均已落地（replay nonce / memory cap / rate limit），
           但 P1-4 未标记全部关闭——须先 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3-REVIEW，再
           DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE 单独验收后方可关闭；不得直接进入 Integration-1
```

## 27. 2026-06-13 DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE 验收记录（P1-4 整体回归收口）

```text
日期       2026-06-13
阶段       DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE（REGRESSION_VALIDATION + SECURITY_REVIEW + DOCUMENTATION）
范围       只做 P1-4 三项修复整体回归收口验收 + 文档关闭口径；不新增功能；不改生产代码 / 测试代码
本轮改动   仅文档：STATUS.md（§1.1 残留项指向关闭 + 新增 §1.3 CLOSED 记录）、ROADMAP.md、README.md、
           DH_P1_4_RESIDUAL_FIX_PLAN.md、TESTING.md（本节）、WORKLOG.md
回归制品核验（只读）
  - Batch1 replay nonce：JdbcNonceReplayGuard 存在；MARK_SQL = INSERT ... ON CONFLICT (replay_key) DO NOTHING；
    DataAccessException -> return false（fail-closed -> 409）；异常只记 ex.getClass().getName()（不记 replay_key/
    nonce/payload/secret）；V4 dh_nq_replay_nonce 表 if not exists、不动 V1–V3、无凭证列；NonceReplayGuardType.select
    缺省非 dev/test -> JDBC，非 dev/test 显式 in-memory -> IllegalStateException（启动失败）
  - Batch2 memory cap：InMemoryNonceReplayGuard maxEntries + TTL、满容量不驱逐未过期 key、fail-closed；
    InMemoryNqFeedbackEventRepository global + per-tenant + retention 上限、TTL 清理优先、非法配置启动失败；
    payload 64KiB gate 不受影响（per-tenant 默认 Batch3 已加固为 1000 < 全局上限）
  - Batch3 rate limit：RateLimiter / RateLimitResult / InMemoryRateLimiter 存在；限流前置于 HMAC；
    key=source+tenant+route（缺失项归一化为占位符，不合并成无界公共 key）；超限 429 RATE_LIMITED；
    429 不泄露阈值/窗口/计数/secret/签名材料；限流短路下游 ingestion；HMAC/timestamp/nonce/replay/payload 语义不变
命令       mvn test
结果       BUILD SUCCESS；关键测试全绿
           - JdbcNonceReplayGuardTest 5/5、NonceReplayGuardTypeTest 5/5、HmacNqFeedbackAuthenticatorTest 6/6
           - BoundedInMemoryNonceReplayGuardTest 7/7、BoundedInMemoryNqFeedbackEventRepositoryTest 7/7
           - NqFeedbackPayloadSizeGateTest 2/2、InMemoryRateLimiterTest 6/6、NqFeedbackRateLimitWebMvcTest 3/3
           - NqFeedbackControllerWebMvcTest 15/15、DhNqIntegration0*（INT0-T01..T15）6+2+8=16/16、ArchUnit 全绿
           - 本机无 Docker：JdbcNonceReplayGuardPersistenceTest 3 + PostgresContainerSmokeTest 1 按
             disabledWithoutDocker skip（非失败）；**持久化 nonce restart 语义仍需 Docker CI 独立验证，未在本机实跑**
命令       mvn -Pquality validate
结果       BUILD SUCCESS（本轮未引入 quality 违规；仅改文档）
命令       git diff --check / git status --short
结果       无 whitespace error；本轮改动仅文档（docs/current/*），无代码 / 测试改动
关闭口径   DH P1-4 residual: CLOSED（replay nonce persistence: closed / memory cap: closed / rate limit: closed）
           Integration-1: NOT STARTED；Runtime integration: NOT STARTED；DH: NOT INTEGRATED；
           AI: NOT STARTED；LIVE: DISABLED；header alignment: NOT DONE
边界       未修改 NQ；未做 header alignment；未新增 API / migration / RealClient / 真实 Provider；
           未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；未读取或输出真实密钥
剩余风险   in-memory limiter / in-memory guard 单实例局限，真实多实例需集中式（Redis）；fail-closed 保护性拒绝需监控；
           固定窗口边界突发；阈值需按真实流量调优；持久化 nonce restart 语义待 Docker CI 验证
准入决定   P1-4 CLOSED 仅表示 Integration-1 前置安全缺口关闭，不等于允许真实联调；Integration-1 仍 NOT STARTED；
           下一步只允许 DH-CI-PERSISTENT-NONCE-IT-ENABLE 或 DH-NQ-HEADER-ALIGNMENT-PLAN，不得直接 Integration-1 runtime
```

## 28. 2026-06-13 DH-CI-PERSISTENT-NONCE-IT-ENABLE（Docker CI 实跑持久化 nonce IT）

```text
日期       2026-06-13
阶段       DH-CI-PERSISTENT-NONCE-IT-ENABLE（CI_TEST_ENABLEMENT + SECURITY_VALIDATION）
范围       让带 Docker 的 CI runner 真实运行 Testcontainers IT，验证 persistent replay nonce 的 restart 语义；
           不改业务生产代码、不改测试逻辑、不接真实 NQ / 真实 HTTP / 不启动 Integration-1
本轮改动
  - 新增 .github/workflows/ci.yml（GitHub Actions）：ubuntu-latest（预装并运行 Docker）+ JDK 21（temurin）+
    maven cache；步骤：docker info -> ./mvnw -B -ntp test -> 断言 IT 未被 skip -> 上传 surefire 报告
  - 未修改 JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest：保留
    @Testcontainers(disabledWithoutDocker = true)，使本地/无 Docker 仍优雅 skip、CI 有 Docker 实跑

测试门控（关键区分）
  - JdbcNonceReplayGuardPersistenceTest（dh-infra，3 用例）：
      local no Docker  -> skipped（disabledWithoutDocker；本机已确认 Tests run: 3, Skipped: 3）
      CI with Docker   -> 实跑（workflow assert 步骤强制 Skipped: 0，否则 CI 失败）
  - PostgresContainerSmokeTest（dh-app，1 用例）：
      local no Docker  -> skipped
      CI with Docker   -> 实跑（同上 assert 守护）

本机验证（无 Docker）
  命令   mvn -pl dh-infra -am -Dtest=JdbcNonceReplayGuardPersistenceTest -Dsurefire.failIfNoSpecifiedTests=false test
  结果   BUILD SUCCESS（exit 0）；JdbcNonceReplayGuardPersistenceTest Tests run: 3, Skipped: 3（无 Docker 优雅 skip，符合预期）
  命令   git diff --check
  结果   无 whitespace error
  YAML   PyYAML 本机不可用，已做结构核验：无 Tab 缩进、顶层键（name/on/permissions/concurrency/jobs）与 6 个 step 结构正确

CI Docker 实跑结果（2026-06-14，已确认；GitHub Actions run 27485958120，结论 success）
  - JdbcNonceReplayGuardPersistenceTest：Tests run: 3, Failures: 0, Errors: 0, **Skipped: 0**, 8.317s
    —— persistent nonce restart 语义（重建 guard 复用同一持久化存储后窗口内重放仍被拒）经真实 PostgreSQL(postgres:17) 验证。
  - PostgresContainerSmokeTest：Tests run: 1, Failures: 0, Errors: 0, **Skipped: 0**, 6.645s（全 app 上下文加载成功）。
  - assert 步骤打印 `OK (executed, 0 skipped)` × 2；`[INFO] BUILD SUCCESS`；job success。
  - 迭代历程：首跑 mvnw 缺执行位（chmod 后仍）wrapper jar 损坏 -> 改用 runner 预装 mvn（fd522ce）；
    再暴露 PostgresContainerSmokeTest 因 Flyway 缺 PostgreSQL 模块报 "Unsupported Database: PostgreSQL 17.10"
    -> 补 flyway-database-postgresql（841354d，用户授权的生产级修复）-> CI 整体全绿。
  - 注意：本条为"配置完成 + 预期"，**首次 push/PR 触发 CI 前不得记为已 executed/passed**

边界       未修改 NQ；未新增业务 Java 生产代码；未新增 API / migration；未做 header alignment；
           未新增 RealClient / 真实 Provider；未做真实 HTTP / 真实 NQ / 真实交易所；未接 AI；未开启 LIVE；
           未启动 Integration-1；未把 DH 写成 integrated；未读取或输出真实密钥
风险       Testcontainers 依赖 Docker daemon；CI runner 须能拉取 postgres:17（网络 / 镜像源）；
           首次运行有镜像拉取耗时；私有 runner 若无 Docker 需另行启用
准入决定   CI 已确认整体全绿（run 27485958120 success）；persistent nonce restart 语义经真实 PG17 在 CI 实跑通过。
           Integration-1 仍 NOT STARTED；不改变 P1-4 CLOSED 口径（CI 实跑只是补强证据，非改变结论）；
           下一步 DH-NQ-HEADER-ALIGNMENT-PLAN 或 DH-CONFIG-CREDENTIAL-DEFAULTS-GOVERNANCE，不得直接 Integration-1 runtime
```

## 29. 2026-06-14 DH-NQ-HEADER-ALIGNMENT-PLAN（planning-only，未跑测试）

```text
日期       2026-06-14
阶段       DH-NQ-HEADER-ALIGNMENT-PLAN（INTEGRATION_CONTRACT_PLANNING + SECURITY_REVIEW + DOCUMENTATION）
范围       只读核查 DH + NQ header 用法 + 输出对齐方案文档；不改运行代码 / 测试
为何未跑   本轮纯文档（planning-only），未触及任何 Java / 测试 / 构建文件，故未运行 mvn test / quality；
           按 CLAUDE 文档任务纪律记录未跑原因。header alignment 的测试将于 IMPL-BATCH-* 实施时新增并验证。
只读核查   DH 生产 controller 用 legacy X-DH-NQ-*（4：Source/Timestamp/Nonce/Signature）；
           DH INT0 fixture + 两仓 docs + NQ INT0 fixture 用 canonical X-NQ-DH-*；NQ 无生产 header 处理代码；
           HmacNqFeedbackAuthenticator 签名 value-based（不含 header name）-> 改名不漂移。
规划测试   见 DH_NQ_HEADER_ALIGNMENT_PLAN.md §6（accepts_canonical / legacy_compat / conflict_fail_closed /
           signature_after_normalization / keeps_payload_64kib / keeps_nonce_replay / keeps_rate_limit_key /
           keeps_tenant_binding / keeps_INT0_T01_to_T15 / no_real_http / no_credential_access 等）。
边界       未修改 Java；未修改测试；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未接 AI；
           未开启 LIVE；未启动 Integration-1；未跨仓写 NQ；未读取或输出真实密钥。
准入决定   header alignment 仍 NOT STARTED（仅 PLAN）；Integration-1 仍 NOT STARTED；
           下一步 DH-NQ-HEADER-ALIGNMENT-PLAN-REVIEW，通过后 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1。
```

## 30. 2026-06-14 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1 验收记录（内部结构 skeleton）

```text
日期       2026-06-14
阶段       DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1（CODE_CHANGE + CONTRACT_REFACTOR + TEST_REVIEW）
范围       内部结构：集中 header 常量 + parser + 归一化模型 + validator skeleton；不切 canonical、不改对外行为
新增（main）NqDhHeaderNames / NormalizedNqDhHeaders / NqDhHeaderParser（legacy-only）/ NqDhHeaderValidator(skeleton) /
           NqDhHeaderValidationResult（均 dh-security/security/nq）
新增（test）NqDhHeaderNamesTest(2) / NqDhHeaderParserTest(3) / NqDhHeaderValidatorTest(2)
修改（main）NqFeedbackController：去 magic string，经 parser 读取 legacy header 产出归一化模型；构造器不变、行为不变
命令       mvn test
结果       BUILD SUCCESS（exit 0）
           - 新单测：NqDhHeaderNamesTest 2/2、NqDhHeaderParserTest 3/3、NqDhHeaderValidatorTest 2/2
           - 行为不变回归：NqFeedbackControllerWebMvcTest 15/15、NqFeedbackRateLimitWebMvcTest 3/3、
             NqFeedbackPayloadSizeGateTest 2/2
           - INT0 DhNqIntegration0*（INT0-T01..T15）6+2+8=16/16 未破坏；ArchUnit 全绿
           - 无 Docker：JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest 按 disabledWithoutDocker skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（未引入 quality 违规）
命令       git diff --check
结果       无 whitespace error
安全自查   不记录 raw signature / signature material / secret / token / full body；归一化模型 toString 对 signature
           脱敏（[REDACTED]，单测固化 model_toString_does_not_leak_signature）
边界       未修改 NQ；未切 canonical-only；未移除 legacy；未实现双接收；未新增 API / migration；未真实 HTTP /
           真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥
准入决定   header alignment 整体仍 NOT COMPLETED（canonical-only 未切换）；Integration-1 仍 NOT STARTED；
           下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-1-REVIEW，通过后 Batch 2（canonical 读取）
```

## 31. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-DOC-RECONCILE（纯文档，未跑测试）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-DOC-RECONCILE（DOCUMENTATION + INTEGRATION_CONTRACT_RECONCILE）
范围       仅将 header alignment 文档口径统一到 canonical-only；改 DH_NQ_HEADER_ALIGNMENT_PLAN.md / README.md / TESTING.md / WORKLOG.md
为何未跑   本轮纯文档，未触及任何 Java / 测试 / 构建文件，故未运行 mvn test / quality；按 CLAUDE 文档任务纪律记录未跑原因。
           回归基线沿用 BATCH-1 验收（2026-06-14）：mvn test BUILD SUCCESS / mvn -Pquality validate BUILD SUCCESS / INT0 16/16，未受文档改动影响。
验证       git status --short（仅 4 份 docs 改动 + 4 个会话起始即存在的未跟踪杂散文件）；git diff --check 无 whitespace error；git diff --stat 仅 docs/current/*.md。
口径       canonical-only：无兼容期、无双接收；Batch 2=canonical-only 读取；Batch 3=Tenant/Request/Trace binding 一致性校验（HEADER_BINDING_MISMATCH）；
           legacy 仅历史引用、生产在 Batch 2 前仍读 legacy；timestamp 格式分歧另列，不在本轮。
边界       未改 Java；未改测试；未切 canonical-only 行为；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；
           未接 AI；未开启 LIVE；未启动 Integration-1；未处理 wrapper / datasource 弱口令 / timestamp 格式；未删除未跟踪文件；未读取真实密钥。
后续项     P3-2 未跟踪杂散文件（4 个，会话起始即存在）登记为待用户授权后清理，本轮不处理。
准入决定   header alignment 整体仍 NOT COMPLETED（canonical-only 未切换）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2（canonical-only 读取）。
```

## 32. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2 验收记录（canonical-only 读取）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2（CODE_CHANGE + CONTRACT_ALIGNMENT + SECURITY_FIX + TEST_CODE_CHANGE）
范围       入站 header 切 canonical-only X-NQ-DH-*；不做双接收 / 不兼容 legacy / 不做 binding mismatch（Batch 3）/ 不启动 Integration-1
修改（main）NqDhHeaderParser（+parseCanonical；parseLegacy 保留历史引用）、NqFeedbackController（parseLegacy->parseCanonical）
修改（test）NqDhHeaderParserTest +2；NqFeedbackControllerWebMvcTest 切 canonical +5；NqFeedbackRateLimitWebMvcTest 切 canonical
命令       mvn test
结果       BUILD SUCCESS（exit 0）
           - NqFeedbackControllerWebMvcTest 20/20（canonical 成功 + legacy-only 403 + 缺 source 403 + 缺 timestamp 401 + 缺 nonce 401 + 签名不泄露）
           - NqFeedbackRateLimitWebMvcTest 3/3（429 RATE_LIMITED 保持）
           - NqDhHeaderParserTest 5/5、NqDhHeaderNamesTest 2/2、NqDhHeaderValidatorTest 2/2、NqFeedbackPayloadSizeGateTest 2/2
           - INT0 DhNqIntegration0*（INT0-T01..T15）6+2+8=16/16 未破坏；ArchUnit 全绿
           - 无 Docker：JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest 按 disabledWithoutDocker skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过）
命令       git diff --check
结果       无 whitespace error
缺失语义   canonical Source 缺失/不匹配->403 SOURCE_NOT_ALLOWED；Timestamp->401 TIMESTAMP_EXPIRED；Nonce->401 REPLAY_KEY_MISSING；Signature->401 BAD_SIGNATURE；replay->409；payload->413；rate limit->429；成功->202（全部保持）
安全自查   不记录 raw signature / signature material / secret / token / full body；canonical 成功路径断言响应不回显 signature / secret；HMAC value-based 不含 header name
边界       未改 NQ；未双接收；未兼容 legacy；未保留 legacy 为生产可接受 header；未移除 legacy 常量；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥
准入决定   生产入站现为 canonical-only；header alignment 整体仍 NOT COMPLETED（binding=Batch 3 / docs-fixtures=Batch 4 尚待）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-2-REVIEW，通过后 Batch 3
```

## 34. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-4 收口记录（docs/fixtures，纯文档 + 回归）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-4（DOCUMENTATION + CONTRACT_FIXTURE_REVIEW + REGRESSION_VALIDATION）
范围       docs/fixtures 收口：统一 MISSING_CANONICAL_HEADER 措辞 + 明确 canonical-only / binding 已落地 + 核对 fixtures；无运行代码改动
修改       仅 docs（PLAN / README / TESTING / WORKLOG）；无 Java / fixture 业务改动
fixtures   WebMvc 成功路径全 canonical X-NQ-DH-*；唯一 legacy 在 legacy-only 负路径（刻意保留）；INT0 Int0Contract 7 canonical X-NQ-DH-*
命令       mvn test
结果       BUILD SUCCESS（回归）。WebMvc 25/25、validator 6/6、parser 5/5、rate limit 3/3、payload gate 2/2、INT0 6+2+8=16/16；ArchUnit 全绿；无 Docker IT skip
命令       mvn -Pquality validate（本轮 3 次重跑）
结果       BUILD FAILURE（环境性）：checkstyle SuppressionFilter 联网解析 suppressions DTD 超时（Connection timed out），非本批所致；
           本批仅改 docs（无 Java/test），checkstyle/spotless 覆盖面同 Batch 3 最近 PASS；按规定本轮不修 checkstyle（见 DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE）
命令       git diff --check
结果       无 whitespace error
事实       MISSING_CANONICAL_HEADER=预留码（缺 canonical 由 authenticator 403/401 覆盖）；canonical-only 已落地（不接受 legacy、无兼容期、无双接收）；binding mismatch -> 403 HEADER_BINDING_MISMATCH
后续项     DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT / DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE / DH-NQ-HEADER-BINDING-PRE-AUTH-PLAN（独立，不阻塞 close）
准入决定   header alignment 整体 READY FOR CLOSE / PENDING FINAL REVIEW（仍未 CLOSED）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW
```

## 35. 2026-06-15 DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE 验收记录（quality gate 离线 DTD 治理）

```text
日期       2026-06-15
阶段       DH-CHECKSTYLE-OFFLINE-DTD-GOVERNANCE（QUALITY_GATE_FIX + BUILD_STABILITY + SECURITY_VALIDATION）
范围       仅治理 checkstyle suppressions 外部 DTD 联网解析；改 config/checkstyle/checkstyle-suppressions.xml 1 行 PUBLIC id + 文档
根因       suppressions DOCTYPE PUBLIC id=非标准 "-//Checkstyle//DTD Suppressions 1.2//EN" 不在 SuppressionsLoader 解析映射 -> 回退 SYSTEM URL 联网 -> 弱网超时 Connection timed out
修复       PUBLIC id 改为官方 "-//Checkstyle//DTD SuppressionFilter Configuration 1.2//EN" -> EntityResolver 命中内置 DTD、离线解析；未删 filter/文件、未降规则、未关 checkstyle、未跳 profile（仅 1 行）
命令       mvn -Pquality validate（修复后 2 次）
结果       BUILD SUCCESS ×2（0 Checkstyle violations；spotless 通过）；网络仍不可用 -> 证明离线解析成功（修复前同会话连续 6 次 DTD 超时 FAILURE）
命令       mvn test
结果       BUILD SUCCESS。NqDhHeaderNamesTest 2/2、NqDhHeaderParserTest 5/5、NqDhHeaderValidatorTest 6/6、NqFeedbackControllerWebMvcTest 25/25、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackPayloadSizeGateTest 2/2、INT0 6+2+8=16/16；ArchUnit 全绿；无 Docker IT skip
命令       git diff --check
结果       无 whitespace error
行为       未改任何 Java / header alignment / controller / 签名 / 限流 / 鉴权行为；仅 checkstyle 配置元数据
准入决定   quality gate 离线稳定通过；header alignment close review 环境性阻断 UNBLOCKED；本轮不直接 CLOSED。下一步 DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW-RERUN
```

## 36. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW-RERUN 验收记录（header alignment CLOSED）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-CLOSE-REVIEW-RERUN（REGRESSION_VALIDATION + CONTRACT/SECURITY/DOC REVIEW）
范围       离线 DTD 治理后重跑 close review；只评审 + 记录 CLOSED；未改代码/测试
命令       mvn test
结果       BUILD SUCCESS。names 2 / parser 5 / validator 6 / payload gate 2 / WebMvc 25 / rate limit 3；INT0 6+2+8=16/16；ArchUnit 全绿；无 Docker IT skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过）—— 离线 DTD 治理后稳定，gate 未降低（仅 1 行 PUBLIC id；全规则集实跑）
命令       git status --short / git diff --check
结果       仅状态文档改动；无 whitespace error
确认       17/17：canonical-only 读取、binding mismatch 403 HEADER_BINDING_MISMATCH、HMAC value-based、rate limit key、payload 413、replay 409、legacy-only 403、INT0 16/16
Close      header alignment overall = CLOSED（不放开 runtime；Integration-1 / Runtime / DH integration / AI NOT STARTED；LIVE DISABLED）
后续项     DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT / DH-NQ-HEADER-BINDING-PRE-AUTH-PLAN / Maven wrapper / datasource 弱口令（均与 close 解耦）
准入决定   CLOSED；下一步 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT 或 GateK-PLAN
```

## 37. 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT（planning-only，未跑测试）

```text
日期       2026-06-15
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT（INTEGRATION_CONTRACT_PLANNING + SECURITY_REVIEW + DOCUMENTATION）
范围       只读核查 X-NQ-DH-Timestamp 线缆格式 + 输出对齐方案；不改运行代码 / 测试
为何未跑   纯文档（planning-only），未触及任何 Java / 测试 / 构建文件，故未运行 mvn test / quality；按文档任务纪律记录未跑原因。
           回归基线沿用 header alignment CLOSE-REVIEW-RERUN（2026-06-15）：mvn test + mvn -Pquality validate BUILD SUCCESS / INT0 16/16。
只读核查   生产 Instant.parse(RFC3339)；SECURITY_POLICY §2=epoch 毫秒（冲突）；INT0 fixture=epoch 秒；CONTRACT_FREEZE/TEST_PLAN=仅 ±300s；NQ 仓库本会话不可达（未当面核对）。
决策       canonical = RFC3339 / ISO-8601 UTC（Instant.toString() 规范形）；窗口 ±300s 不变；HMAC value-based 不改；nonce/replay/binding 不受影响。
实施分批   T1 docs 收口 / T2 INT0 测试对齐 / T4 NQ companion /（可选 gated）T3 生产收紧（见 DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md §6）。
边界       未改 NQ / Java / 测试；未新增 API / migration；未真实 HTTP / NQ / 交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥。
准入决定   timestamp alignment NOT STARTED（仅 PLAN）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-PLAN-REVIEW
```

## 38. 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1 验收记录（docs 收口）

```text
日期       2026-06-15
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1（DOCUMENTATION + INTEGRATION_CONTRACT_ALIGNMENT）
范围       DH 侧 timestamp 契约文档统一为 RFC3339 / ISO-8601 UTC Z；不改 Java / 测试 / NQ
修改       SECURITY_POLICY §2+§4、CONTRACT_FREEZE §规则、CONTRACT_TEST_PLAN T5/INT0-T05、TIMESTAMP_FORMAT_ALIGNMENT_PLAN、README/TESTING/WORKLOG；无 Java/测试/NQ
canonical  RFC3339 / ISO-8601 UTC Z（例 2026-06-15T12:34:56Z）；拒绝 epoch 秒/毫秒/数字偏移；窗口 ±300s 不变；HMAC value-based 不改、header name 不入签
命令       mvn test
结果       BUILD SUCCESS（回归）。INT0 6+2+8=16/16、WebMvc 25、validator 6、parser 5、rate limit 3、payload gate 2；ArchUnit 全绿；无 Docker IT skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过）
命令       git diff --check
结果       无 whitespace error
未收口     INT0 epoch 秒(T2) / NQ companion(T4，Integration-1 前置阻断) / 生产 UTC-Z 强制(可选 T3)；timestamp alignment 整体 NOT COMPLETED
准入决定   T1 DONE；Integration-1 仍 NOT STARTED。下一步 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T1-REVIEW
```

## 39. 2026-06-15 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2 验收记录（INT0 测试对齐 RFC3339 UTC Z）

```text
日期       2026-06-15
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2（TEST_CODE_CHANGE + INTEGRATION_CONTRACT_ALIGNMENT + REGRESSION）
范围       DH INT0 contract test/fixture timestamp 由 epoch 秒改 RFC3339 / ISO-8601 UTC Z；不改生产 Java / NQ
修改       Int0RequestFactory（Instant.ofEpochSecond(...).toString()）、Int0ContractValidator（Instant.parse(...).getEpochSecond()，非 RFC3339→TIMESTAMP_INVALID）、DhNqIntegration0SecurityContractTest（int0T05 增量断言）；+docs
命令       mvn test
结果       BUILD SUCCESS。INT0 6+2+8=16/16（SecurityContractTest 8，int0T05 内含 RFC3339-Z accept + epoch 秒/毫秒 reject）；WebMvc 25 / validator 6 / parser 5 / rate limit 3 / payload gate 2；ArchUnit 全绿；无 Docker IT skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过）
命令       git diff --check
结果       无 whitespace error
不变量     HMAC（Int0Signing）value-based 不改、header name 不入签；±300s 窗口、TIMESTAMP_INVALID/TIMESTAMP_OUT_OF_WINDOW 语义、其它 INT0 测试均不变
未收口     T4 NQ companion（Integration-1 前置阻断）；T3 生产 UTC-Z-only 收紧（可选）；timestamp alignment 整体 NOT COMPLETED
准入决定   T2 DONE；Integration-1 仍 NOT STARTED。下一步 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T2-REVIEW
```

## 33. 2026-06-15 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3 验收记录（Tenant/Request/Trace binding 一致性）

```text
日期       2026-06-15
阶段       DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3（CODE_CHANGE + CONTRACT_ALIGNMENT + SECURITY_FIX + TEST_CODE_CHANGE）
范围       接入 NqDhHeaderValidator，canonical Tenant/Request/Trace 与权威来源 binding 一致性；不恢复 legacy / 不双接收 / 不启动 Integration-1
修改（main）NqDhHeaderValidator（4 参 validate + binding 校验）、NqFeedbackController（接入 validator，mismatch -> 403 HEADER_BINDING_MISMATCH）
修改（test）NqDhHeaderValidatorTest 重写为 6 binding 用例；NqFeedbackControllerWebMvcTest +5 binding 用例
命令       mvn test
结果       BUILD SUCCESS
           - NqDhHeaderValidatorTest 6/6、NqFeedbackControllerWebMvcTest 25/25、NqDhHeaderParserTest 5/5、NqFeedbackRateLimitWebMvcTest 3/3、NqFeedbackPayloadSizeGateTest 2/2
           - INT0 DhNqIntegration0*（INT0-T01..T15）6+2+8=16/16 未破坏；ArchUnit 全绿
           - 无 Docker：JdbcNonceReplayGuardPersistenceTest / PostgresContainerSmokeTest 按 disabledWithoutDocker skip
命令       mvn -Pquality validate
结果       BUILD SUCCESS（0 Checkstyle violations；spotless 通过；checkstyle DTD 未抖动）
命令       git diff --check
结果       无 whitespace error
binding    canonical Tenant-Id != auth tenant / Request-Id != body requestId / Trace-Id != body traceId（若提供）-> 403 HEADER_BINDING_MISMATCH；header 缺省跳过；header 不覆盖权威来源
保持       HMAC value-based；rate limit key=source+tenant+route；payload 413；nonce replay 409；缺 source/timestamp/nonce/signature 由 authenticator 403/401；成功 202
安全自查   mismatch 响应不回显 header 原值 / signature / secret / full payload；validator reason 不含具体值；不记录 raw signature / material / secret / token / full body
边界       未改 NQ；未恢复 legacy；未双接收；未新增 API / migration；未真实 HTTP / 真实 NQ / 真实交易所；未新增 RealClient / 真实 Provider；未接 AI；未开启 LIVE；未启动 Integration-1；未读取真实密钥
准入决定   header alignment 整体仍 NOT COMPLETED（仅余 docs/fixtures 收口 Batch 4）；Integration-1 仍 NOT STARTED。下一步 DH-NQ-HEADER-ALIGNMENT-IMPL-BATCH-3-REVIEW，通过后 Batch 4
```

## 40. 2026-06-28 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T3 验收记录（production / INT0 UTC-Z-only 收紧）

```text
日期       2026-06-28
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-IMPL-BATCH-T3（SECURITY_FIX + CONTRACT_ALIGNMENT + TEST_CODE_CHANGE + REGRESSION_VALIDATION）
范围       DH production parseTimestamp + DH INT0 validator 收紧为 RFC3339 / ISO-8601 UTC `Z`；不改 NQ / 不真实 HTTP / 不启动 Integration-1
修改（main）HmacNqFeedbackAuthenticator：parseTimestamp 在 Instant.parse 前要求 timestampHeader.endsWith("Z")；epoch 秒/毫秒与 +08:00 fail-closed，非法格式沿用 TIMESTAMP_EXPIRED（401）
修改（test）HmacNqFeedbackAuthenticatorTest：覆盖 RFC3339 UTC Z accept、epoch seconds reject、epoch milliseconds reject、+08:00 reject、过去/未来超出 ±300s reject
修改（INT0）Int0ContractValidator：同步要求 UTC Z 后再 Instant.parse；DhNqIntegration0SecurityContractTest INT0-T05 补 +08:00 reject（签名按 offset header 重算）
命令       mvn -pl dh-domain,dh-security -am "-Dtest=HmacNqFeedbackAuthenticatorTest,DhNqIntegration0SecurityContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
结果       BUILD SUCCESS；DhNqIntegration0SecurityContractTest 8/8；HmacNqFeedbackAuthenticatorTest 8/8
命令       git status --short
结果       仅本轮代码/测试/docs 变更；补 TESTING 前为 8 个文件
命令       git diff --check
结果       exit 0；仅 LF/CRLF warning，无 whitespace error
命令       git diff --stat
结果       补 TESTING 前 8 files changed, 146 insertions(+), 18 deletions(-)
命令       mvn test
结果       BUILD SUCCESS；INT0 6+2+8=16/16，Hmac 8/8，WebMvc 25/25，parser 5/5，validator 6/6，rate limit 3/3，payload gate 2/2，ArchUnit 12/12；本机无 Docker，既有 PostgresContainerSmokeTest skip 1
命令       mvn -Pquality validate
结果       BUILD SUCCESS；0 Checkstyle violations；spotless 通过
不变量     HMAC signatureMaterial value-based 不改、header name 不入签；验签仍使用 timestamp.toString() 归一化 UTC Z；±300s replay window、nonce/source/tenant/requestId/traceId/payload 语义不变
边界       未改 NQ；未新增 API / migration；未真实 HTTP / DH-NQ 调用 / 交易所调用；未新增 RealClient / 真实 Provider；未读取凭证；未启动 Integration-1；未开启 LIVE；未处理 Maven wrapper / datasource 默认弱口令 / nonce-burn race；未引入双格式兼容或 epoch fallback
准入决定   T3 已由后续 review ACCEPTED；timestamp alignment 后续由 FINALIZE 收口为 CLOSED / ACCEPTED。Integration-1 / Runtime integration NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED。
```

## 41. 2026-06-28 DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-FINALIZE（timestamp overall CLOSED）

```text
日期       2026-06-28
阶段       DH-NQ-TIMESTAMP-FORMAT-ALIGNMENT-FINALIZE（CONTRACT_FINALIZATION + DOCUMENTATION_FIX + CROSS_REPO_REGRESSION_VALIDATION）
范围       合并 DH T3 review 后旧状态修正、header naming stale docs 小修、DH/NQ 双仓最终回归；不改生产代码 / 测试代码 / API / migration
Final      timestamp alignment overall = CLOSED / ACCEPTED
canonical  RFC3339 / ISO-8601 UTC Z（例 2026-06-15T12:34:56Z）；DH/NQ 均拒绝 epoch seconds / epoch milliseconds / 数字时区偏移；窗口 ±300s 不变
边界       CLOSED 仅表示 timestamp 契约收口完成；Integration-1 / Runtime integration NOT STARTED；DH NOT INTEGRATED；LIVE DISABLED
验证       DH git status --short：仅允许的 7 个 docs/current 文件 modified
           DH git diff --check：通过；仅 LF/CRLF warning，无 whitespace error
           DH git diff --stat：7 files changed, 141 insertions(+), 134 deletions(-)
           DH mvn test：BUILD SUCCESS；既有 PostgresContainerSmokeTest 因本机无 Docker skipped 1
           DH mvn -Pquality validate：BUILD SUCCESS；0 Checkstyle violations；Spotless check 通过
           NQ git status --short：仅允许的 8 个 docs/current 文件 modified
           NQ git diff --check：通过；仅 LF/CRLF warning，无 whitespace error
           NQ git diff --stat：8 files changed, 55 insertions(+), 12 deletions(-)
           NQ mvn -f backend/pom.xml test：BUILD SUCCESS
           NQ INT0 scoped test：BUILD SUCCESS；Integration0 6+2+9=17/17
           NQ backend quality profile 探测：backend POM 未检出 quality profile / Spotless / Checkstyle
```
