# Decision Hub Testing

## 1. 当前状态

```text
Stage1                  代码 + 闭环测试已落地
Stage1-CLOSE            旧链路 @Deprecated + 文档单源 + ArchUnit 4 条新规则
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

## 3. 最近一次验收结果（2026-05-25 Stage2-PoC-B2 IMPLEMENT）

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

Batch 2 范围（零 JDBC / 零 NQ 仓库 / 零真实外部服务调用）：
  - dh-usecase 新增类     17 个（接口 4 + 值对象/枚举 5 + impl 3 + handler 9）
  - dh-usecase 修改类      2 个（repo 接口 + InMemory 实现）
  - dh-api     新增类      3 个（envelope/accepted/error DTO）
  - dh-api     修改类      2 个（Controller 升级 + 旧 DTO @Deprecated）
  - dh-app     修改类      1 个（AgentRuntimeWiringConfig 装配 12 个新 bean）
  - dh-api/pom.xml         加 spring-boot-starter-test (test scope)
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
