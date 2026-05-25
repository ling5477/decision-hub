# Decision Hub Status

> Current stage: Stage2-PoC-B2 IMPLEMENT completed
> Next stage:    Stage2-PoC-B3 IMPLEMENT
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
```

## 4. 下一阶段（Stage2-PoC-B3 IMPLEMENT）

按 docs/current/STAGE2_POC_WORK_ORDER.md §Batch 3 实施：

```text
Batch 3  Forecast / Research Adapter Interfaces：
         dh-connector/tools/ForecastToolPort + Fake +
         dh-connector/research/ResearchDataAdapter + Fake + ResearchSnapshotStore +
         rawPayloadJson 留档 + status 枚举留口（不实现真实 HTTP / Python 调用）
```

## 5. 当前风险

```text
ArchUnit 当前只新增 4 条规则；未来若引入 Stage2 新包仍需补规则。
所有 InMemory 仓储进程重启即丢；Stage2 必须在持久化升级完成后再灰度对外。
NQ 端 /api/ai/* endpoint 不存在；Stage2 启动前需先与 NQ 团队达成事件契约。
TradingAgents 思想只能"借鉴"，禁止整体复制；Stage2 PR 必须显式声明吸收清单。
```
