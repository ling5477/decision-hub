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

## 3. 最近一次验收结果（2026-05-25 Stage1-CLOSE）

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
