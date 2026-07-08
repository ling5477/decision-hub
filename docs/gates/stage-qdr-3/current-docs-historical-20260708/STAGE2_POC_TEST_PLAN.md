# Stage2-PoC Test Plan

> Status: PLAN
> Created: 2026-05-25

## 1. 现有测试（保持绿色）

```text
dh-usecase   ResearchRunStage1ClosedLoopTest   闭环测试（create→start→candidate→judge→feedback→experience）
dh-usecase   DecisionHubFacadeImplTest         旧链路冒烟
dh-app       ArchitectureTest                  5 条规则（1 旧 + 4 Stage1-CLOSE）
```

## 2. Stage2 新增测试

### 2.1 Stage2ClosedLoopTest（dh-usecase）

覆盖完整 Stage2 闭环：

```text
1. 创建 ResearchRun（带 topic + regime）
2. AgentTaskPlanner 动态选边（根据 regime 选择不同 agent 组合）
3. 调用 ForecastToolPort（Fake 返回 mock 预测）
4. 调用 ResearchDataAdapter（Fake 返回 mock 市场数据）
5. 生成 StrategyCandidate（引用 forecast + market data）
6. 生成 JudgeDecision
7. 接收 NQ Feedback Event（正式契约格式，BACKTEST_COMPLETED）
8. 验证 ExperienceEntry 更新（success_count +1）
9. 验证 PheromoneEdge 更新
10. 验证 ReflectionCheckpoint 写入
11. 接收 NQ Feedback Event（RISK_REJECTED）
12. 验证 ExperienceEntry 更新（failure_count +1）
13. 验证 FailureCaseStore 写入
```

### 2.2 NqFeedbackContractValidationTest（dh-usecase）

覆盖事件契约校验：

```text
1. 合法事件（8 种 eventType 各一条）-> 202 Accepted
2. 未知 eventType -> 400 UNKNOWN_EVENT_TYPE
3. 缺少 traceId -> 400 INVALID_SCHEMA
4. traceId 不对应已知 ResearchRun -> 400 UNKNOWN_TRACE
5. schemaVersion 不兼容 -> 400 INVALID_SCHEMA
6. payload 结构不匹配 eventType -> 400 INVALID_SCHEMA
```

### 2.3 ForecastToolPortTest（dh-connector）

```text
1. FakeForecastToolAdapter 返回 mock ForecastArtifact
2. 请求参数校验（symbol 非空、horizon 合法）
3. 超时场景返回 FAILED 状态
```

### 2.4 ResearchDataAdapterTest（dh-connector）

```text
1. FakeResearchDataAdapter 返回 mock ExternalMarketSnapshot
2. 请求参数校验（symbols 非空、dateRange 合法）
3. 空数据场景返回空 snapshot
```

### 2.5 AgentTaskPlannerDynamicTest（dh-usecase）

```text
1. regime=BULL -> 选择偏进攻型 agent 组合
2. regime=BEAR -> 选择偏防守型 agent 组合
3. regime=VOLATILE -> 选择多样化 agent 组合
4. topic 影响任务优先级排序
5. 默认 regime -> 使用 Stage1 原有逻辑
```

### 2.6 ReflectionCheckpointTest（dh-usecase）

```text
1. 每个 agent step 完成后写入 reflection
2. reflection 包含 decision (CONTINUE/PIVOT/ABORT)
3. ABORT decision 终止后续 step
4. checkpoint 可按 runId 查询
```

## 3. ArchUnit 规则（保持 + 新增）

### 保持（5 条）

```text
✅ ..domain.. 不依赖 ..infra..
✅ ..domain.. 不依赖 ..usecase.. / ..api.. / ..infra..
✅ ..connector.nq.. 禁字
✅ ..usecase.agent.. 不依赖 ..providers..
✅ ..api.. 控制器 @RequestMapping 不命中 /orders|/trades|/live
```

### 新增候选（Stage2）

```text
⑥ ..connector.tools.. 不依赖 ..infra..（工具端口保持纯接口）
⑦ ..connector.research.. 不依赖 ..infra..（研究适配器保持纯接口）
⑧ ..domain.tool.. 和 ..domain.research.. 不依赖 ..connector..（领域不反向依赖连接器）
```

## 4. 验证命令

```bash
# 最低验证
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false

# 质量检查
mvn -Pquality validate
```

## 5. 覆盖率目标

```text
Stage2 新增代码行覆盖率 >= 80%
关键路径（feedback ingestion + planner 动态选边）覆盖率 >= 90%
所有 Fake adapter 必须有对应单元测试
```

## 6. 回归保护

```text
Stage1 闭环测试必须继续通过
旧链路冒烟测试必须继续通过
ArchUnit 规则不允许回退
```
