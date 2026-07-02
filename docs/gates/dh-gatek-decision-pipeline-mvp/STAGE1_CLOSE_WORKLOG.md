# DH Stage1-CLOSE Worklog

> Author: ling5477（执行 `Stage1-CLOSE` 开工提示词）
> Date:   2026-05-25
> Branch: dev

## 1. 目标

让 Stage1（Boundary Freeze + Agent Runtime Skeleton）成为仓库唯一主链路；
旧"多模型平台"链路整体 @Deprecated；文档单源；ArchUnit 兜底新边界；dh-eval pom parent 修复。

## 2. 改动清单

### 2.1 旧链路 @Deprecated（不删除，等 Stage2 接通真实 NQ 后再清理）

加上 `@Deprecated(since = "Stage1-CLOSE", forRemoval = true)` 的类型：

```text
domain.run.Run
domain.run.RunStatus
domain.run.RunStep
domain.run.StepType

usecase.facade.DecisionHubFacade
usecase.facade.dto.RunCreateCommand
usecase.facade.dto.RunCreateResult
usecase.facade.impl.DecisionHubFacadeImpl

usecase.run.DecisionEngineV1
usecase.run.RunRepository
usecase.run.RunService
usecase.run.support.DecisionRecordV1Factory
usecase.run.support.OutputResolver

usecase.gate.CostBudgetGate
usecase.gate.EvidenceSufficiencyGate
usecase.gate.Gate
usecase.gate.GateDecision
usecase.gate.GateEngine
usecase.gate.GateResult
usecase.gate.RunContext
usecase.gate.evaluator.CompletenessEvaluatorV1
usecase.gate.evaluator.ConsistencyEvaluatorV1
usecase.gate.evaluator.ConstraintEvaluatorV1
usecase.gate.evaluator.OutputResolverV1

usecase.contract.DecisionOutcome
usecase.contract.DecisionRecordStore
usecase.contract.DecisionRequest
usecase.contract.DecisionStrategy
usecase.contract.EvalRequest
usecase.contract.EvalResult
usecase.contract.Evaluator
usecase.contract.TargetRef

providers.ModelProvider
providers.MockProvider
providers.ModelOutput
providers.ProviderRegistry

app.config.AppWiringConfig（@Deprecated + @SuppressWarnings("deprecation")，旧 bean 全部 @Deprecated）
```

### 2.2 RunController 迁移

| 操作 | 旧 | 新 |
|---|---|---|
| 包 | `api.run.RunController` 等 3 个文件 | `api.legacy.run.RunController` / `CreateRunRequest` / `RunView` |
| REST 路径 | `@RequestMapping("/runs")` | `@RequestMapping("/legacy/runs")` |
| 注解 | 无 | `@Deprecated(since="Stage1-CLOSE", forRemoval=true)` + `@SuppressWarnings("deprecation")` |
| 旧目录 | `dh-api/src/main/java/com/guidinglight/decisionhub/api/run/` | 删除 |

### 2.3 契约同步

- `contracts/openapi.yaml`：`/runs` 与 `/runs/{runId}` → `/legacy/runs` 与 `/legacy/runs/{runId}`，并加 `deprecated: true` 与 deprecated summary。

### 2.4 文档单源同步

下列文件全部更新为 `Current stage: Stage1 completed / Next: Stage2-PoC`：

```text
README.md
docs/current/README.md
docs/current/STATUS.md
docs/current/ROADMAP.md（重写：DH-REFIT-1 → Stage1 → Stage1-CLOSE → Stage2-PoC → Stage3 → DH-FREEZE）
docs/current/WORKLOG.md（追加 Stage1 顶层小结 + Stage1-CLOSE 小结）
docs/current/WORK_ORDER.md（标 Stage1-CLOSE 已完成；下一轮草稿提示词写入 §5）
docs/current/TESTING.md（写最新 mvn 命令 + 验收结果）
```

### 2.5 docs/codex 同步

- `docs/codex/plans/_active/STATUS.json`：
  - `milestone` 从 `M1` 改 `Stage1-CLOSE`
  - `activePlanId` 从 `2026-02-04_M1_run_gate_mockprovider` 改 `2026-05-25_Stage1_agent_runtime_skeleton`
  - 整份 steps 重写为 Stage1 + Stage1-CLOSE 的 6 项 DONE
  - `lastVerify.result` = PASS
- 老 M1 计划归档到 `docs/codex/plans/_archive/2026-02-04_M1/`：
  - `STATUS.json`（`state=ARCHIVED`，步骤 4-7 标 OBSOLETE 并写 `supersededBy`）
  - `README.md`（说明为何归档与对应替代物）

### 2.6 ArchUnit 4 条新规则

`dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java` 重写：

| 规则 | 说明 | 实现 |
|---:|---|---|
| 0 | （保留）`..domain..` 不依赖 `..infra..` | ArchUnit |
| ① | `..domain..` 不依赖 `..usecase..`/`..api..`/`..infra..` | ArchUnit `noClasses().resideInAPackage(..domain..).should().dependOnClassesThat().resideInAnyPackage(..usecase..,..api..,..infra..)` |
| ② | `..connector.nq..` 禁字 `placeOrder/submitOrder/executeOrder/bypassRisk/forceExecute`（豁免 `DefaultNqContractVerifier`） | 源文件扫描（覆盖类名/方法名/字段名/字符串字面量/注释，最稳） |
| ③ | `..usecase.agent..` 不依赖 `com.guidinglight.decisionhub.providers..` | ArchUnit |
| ④ | `..api..` 控制器 `@RequestMapping/@GetMapping/...` 不命中 `/orders\|/trades\|/live` | 源文件扫描 + 正则 |

### 2.7 dh-eval pom

`dh-eval/pom.xml` parent 从 `decision-hub`（根 aggregator pom）改为 `dh-bom`（共享 BOM），保留 `jackson-databind` 与 `slf4j-api` 依赖；`exec-maven-plugin: golden-cases-verify` 保持不动（后续 Stage2 再决定是否迁移到新链路）。

## 3. 验收命令与结果

### 3.1 编译 + 测试

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

结果：**BUILD SUCCESS**（2026-05-25 14:10:05 +08:00，总耗时 33.7s）。

Reactor 全部 SUCCESS（19 modules）。

测试统计（dh-app）：

```text
Running com.guidinglight.decisionhub.ArchitectureTest
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 10.39 s
```

5 条规则：1 条保留 + 4 条新增，全部通过。

dh-usecase 已有的 `ResearchRunStage1ClosedLoopTest` / `DecisionHubFacadeImplTest` 也在前序构建中通过（surefire XML 已留存）。

### 3.2 dep-tree.txt

```bash
mvn dependency:tree > dep-tree.txt
```

结果：**BUILD SUCCESS**，dep-tree.txt 846 行。

### 3.3 grep `/runs`

```bash
grep -rn "/runs" F:/project/decision-hub/dh-api/src/main/java/
```

输出（只命中 `api/legacy/run/`，符合预期）：

```text
.../api/legacy/run/RunController.java:19: * <p>Stage1-CLOSE：从 {@code /runs} 改路径到 {@code /legacy/runs}…
.../api/legacy/run/RunController.java:28: @RequestMapping("/legacy/runs")
```

```bash
grep -n "/runs" F:/project/decision-hub/contracts/openapi.yaml
```

```text
12:  /legacy/runs:
35:  /legacy/runs/{runId}:
```

### 3.4 三处 STATUS 一致性

```text
README.md L12              :> Current stage: Stage1 (Boundary Freeze + Agent Runtime Skeleton) completed
docs/current/STATUS.md L3  :> Current stage: Stage1 (Boundary Freeze + Agent Runtime Skeleton) completed
docs/codex/.../STATUS.json :  milestone: "Stage1-CLOSE" / activePlanId: "2026-05-25_Stage1_agent_runtime_skeleton"
```

三处统一指向"Stage1 已完成；Stage1-CLOSE 已收敛；下一阶段 Stage2-PoC"。

## 4. 未做事项（继续保留为后置项）

```text
不删除任何旧链路代码（仅 @Deprecated，等 Stage2 联调真实 NQ 后再决断）
不迁移旧 RunService 行为到新 ResearchRunCommandService
不接通真实 NQ HTTP / 真实 LLM provider
不引入 Kronos / global-stock-data / TradingAgents 代码
不修改 NQ 仓库
不建设前端
不补 Stage2 PoC 工单（只在 docs/current/WORK_ORDER.md §5 留草稿提示词）
```

## 5. 下一步

进入 **Stage2-PoC**，按 `docs/current/WORK_ORDER.md §5` 的提示词起 PLAN：

```text
1. 与 NQ 协调 NqFeedbackEvent / NqBacktestRequest OpenAPI 草稿
2. dh-connector.tools.ForecastToolPort + ForecastArtifact 接口签名
3. dh-connector.research.* 三个接口签名（global-stock-data 预留）
4. AgentTaskPlanner 动态选边决策表
5. dh-infra：InMemory → JDBC 仓储迁移清单（含 V3 候选）
```
