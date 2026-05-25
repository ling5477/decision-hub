# Decision Hub Status

> Current stage: Stage2-PoC VERIFY completed
> Next stage:    Stage2-PoC FREEZE
> AI trading execution: not allowed
> NQ core changes:      not allowed in this stage

## 1. 当前结论

DH 已经把"多模型调用平台"升级为"可进化的多 Agent 决策能力层"的最小骨架；
旧链路（domain.run.* / api.run.RunController / usecase.facade / usecase.run / usecase.gate /
usecase.contract / dh-providers）已全部 `@Deprecated`，REST 路径迁移到 `/legacy/runs` 子路径，
不再与新链路 `/api/ai/research-runs` 冲突。

文档单源已收敛到 `docs/current/`，与 `docs/codex/plans/_active/STATUS.json` 一致。

## 2. 当前已完成

```text
DH-REFIT-1-PLAN         文档结构、边界、计划、工作流统一
Stage1                  Boundary Freeze + Agent Runtime Skeleton 代码与闭环测试落地
Stage1-CLOSE            旧链路 @Deprecated；REST 旧路径 /legacy/runs；文档单源；ArchUnit 4 条新规则；
                        dh-eval parent 修回 dh-bom；docs/codex 计划切换到 Stage1，
                        老 M1 mock-provider 计划归档到 _archive/2026-02-04_M1
Stage1-FREEZE           docs/current 快照冻结到 docs/gates/dh-stage1/；状态三处对齐
Stage2-PoC PLAN         规划 NQ 事件契约 + Kronos/global-stock-data 接口预留 + TradingAgents 轻量设计
Stage2-PoC WO           5 个 Batch 拆解：契约+领域 / NQ Ingestion / Tool Ports / Reflection-Planner / JDBC+Tests+Docs
Stage2-PoC-B1 IMPLEMENT 领域模型 + JSON Schema + OpenAPI components 落地，零 Controller/Service/Repository/JDBC/WiringConfig 改动
Stage2-PoC-B2 IMPLEMENT NQ feedback ingestion 正式契约：envelope DTO + Validator + Router + 8 个 Handler + 幂等 + WebMvc 入口
Stage2-PoC-B3 IMPLEMENT dh-connector Forecast / Research Adapter 端口预留 + Fake / InMemory 实现 + 3 个测试类全绿
Stage2-PoC-B4 IMPLEMENT Reflection / Checkpoint / Dynamic Planner：
                        PlannerStrategy + Resolver/Registry + 4 个 StrategyHandler +
                        DynamicAgentTaskPlanner + ReflectionCheckpointService +
                        Reflection/Checkpoint InMemory 仓储 + 4 个测试类（28 cases 全绿）；
                        JudgeDecision 仍是唯一最终出口；零 LLM/Python/graph scheduler/dh-domain 改动
Stage2-PoC-B5 IMPLEMENT JDBC + Tests + Docs 收口：
                        V3 Flyway 迁移（4 新表 + 2 ALTER）+ 5 个 Stage2 JDBC 仓储 +
                        Stage2JdbcWiringConfig + @ConditionalOnMissingBean 兜底 +
                        ArchUnit 10 条规则（新增 5 条）+ OpenAPI 对齐 + Stage2ClosedLoopTest 全闭环；
                        本地无 Docker，PostgresContainerSmokeTest 跳过，跑 mvn test
                        -Dtest='!PostgresContainerSmokeTest' 全绿
Stage2-PoC VERIFY       2026-05-26 冻结前验证：mvn test BUILD SUCCESS / 122 tests / ArchUnit 10/10；
                        硬边界扫描全 PASS；契约/文档不一致项已修正：
                        - contracts/openapi.yaml /api/ai/feedback/nq 改为 202 + NqFeedbackAcceptedResponse
                          / 400 + NqFeedbackErrorResponse 并补两个 schema
                        - docs/current/DB_SCHEMA.md 修正 V2 文件名为 V2__dh_agent_runtime.sql
                        - docs/current/API.md 把已实现的 7 条 research-runs 端点移入 "已实现端点"
                        - 生成 docs/current/STAGE2_POC_VERIFY_REPORT.md (Verdict: GO)
```

## 3. 当前阶段边界

```text
不写真实下单代码
不绕过 NQ 风控
不复制 NQ 订单状态机
不重写 NQ 回测核心
不接入真实 LLM provider
不建设第二套完整前端
不引入 BCO/ACO/GWO 等重型数学优化器
不把 Kronos / TradingAgents / global-stock-data 直接复制进 DH/NQ
不引入 TradingAgents Python 代码 / graph scheduler / 复杂 agent graph runtime
```

## 4. 下一阶段（Stage2-PoC FREEZE）

```text
按 docs/current/STAGE2_POC_VERIFY_REPORT.md §10 操作清单实施：
- 将 docs/current 完整拷贝到 docs/gates/dh-stage2-poc/ 并锁定
- 状态推进到 "Stage2-PoC FREEZE completed / Next: Stage3 PLAN"
- FREEZE 完成后再执行：
  - 在装好 Docker 的 CI 环境中跑 PostgresContainerSmokeTest（V3 schema + JDBC 仓储）
  - 对接 NQ 团队真实 ingest endpoint（保持只读、回放安全）
  - 灰度切换 decisionhub.stage2.jdbc.enabled=true，验证 InMemory → JDBC 平滑切换
- dh-memory 5 个 Store 的 JDBC 替换仍归 Stage3 完整持久化，本阶段不触动
```

## 5. 当前风险

```text
ArchUnit 已扩到 10 条规则，覆盖 connector.tools/research、domain.{forecast/marketdata/reflection/checkpoint}、
usecase.agent.{planner,feedback} 边界。Stage2 持久化通路已就绪但默认仍走 InMemory；
decisionhub.stage2.jdbc.enabled=true 后必须先在 CI 跑 PostgresContainerSmokeTest 再上线。
NQ 端 /api/ai/* endpoint 不存在；Stage2 启动前需先与 NQ 团队达成事件契约。
TradingAgents 思想只能"借鉴"，禁止整体复制；本批仅落 Reflection/Checkpoint + 4 strategy handler，
无 LLM / Python / graph scheduler。
dh-memory 5 个 Store 仍是 InMemory，留 Stage3 替换。
```
