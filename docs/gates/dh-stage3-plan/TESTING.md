# Decision Hub Testing

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
