# Stage2-PoC VERIFY Report

> Date: 2026-05-26
> Branch: dev
> Scope: 对 Stage2-PoC 全量实现做一次冻结前验证，仅含验证、文档修正与必要小修。
> Source of truth: `docs/current`

## 1. 当前阶段

```text
Current stage: Stage2-PoC VERIFY (this report)
Next stage:    Stage2-PoC FREEZE
```

## 2. 验收命令

```bash
mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false
```

`PostgresContainerSmokeTest` 依赖 Docker，留给装好 Docker 的 CI 环境单独跑，不阻塞本次 VERIFY。

## 3. 测试结果

```text
BUILD SUCCESS
Total: 122 tests / 0 failures / 0 errors / 0 skipped
```

模块明细：

```text
dh-domain      35 tests   (含 JsonSchemaPresenceTest 5)
dh-connector    9 tests   (Fake adapter / store 全绿)
dh-usecase     47 tests   (含 Stage2ClosedLoopTest 2, ResearchRunStage1ClosedLoopTest 1)
dh-infra        9 tests   (JdbcSqlFragmentsTest 5, JdbcNqFeedbackEventRepositoryTest 4)
dh-api          7 tests   (NqFeedbackControllerWebMvcTest 7)
dh-app         15 tests   (ArchitectureTest 10, V3MigrationPresenceTest 5)
```

关键测试：

```text
Stage1 闭环               ResearchRunStage1ClosedLoopTest      1/1
Stage2 闭环 (B3+B4 整合)  Stage2ClosedLoopTest                 2/2
Batch1 契约 + Batch5 SQL  JsonSchemaPresenceTest 5 / V3MigrationPresenceTest 5
ArchUnit 架构守门         ArchitectureTest                     10/10
Batch2 NQ feedback ingest NqFeedbackControllerWebMvcTest       7/7
Batch5 JDBC 仓储 SQL      JdbcSqlFragmentsTest                 5/5
Batch5 幂等 race          JdbcNqFeedbackEventRepositoryTest    4/4
```

## 4. 边界扫描结果

| 检查项 | 结论 | 备注 |
| --- | --- | --- |
| 不修改 NQ 仓库 | PASS | DH 仓库内零 NQ 路径改动 |
| 不接真实 NQ API | PASS | 无 NQ 客户端，仅契约 / Fake |
| 不接真实 Kronos | PASS | Kronos 仅在 javadoc / enum 字面量出现 |
| 不接真实 global-stock-data | PASS | 仅在 enum / Fake / 注释出现 |
| 不引入 TradingAgents Python | PASS | `dh-*` 模块无 `.py` 文件 |
| 不实现真实下单 | PASS | 无 REST / HTTP client (RestTemplate/WebClient/HttpClient/OkHttpClient/RestClient/FeignClient/HttpURLConnection) |
| 无下单关键词 | PASS | `placeOrder` / `submitOrder` / `executeOrder` / `bypassRisk` / `forceExecute` 仅出现在 ArchUnit 黑名单守门测试 / NqContractVerifier 黑名单白名单本身 |
| 无 /orders /trades /live 路径 | PASS | controller 路径不含上述前缀 |
| JudgeDecision 仍是唯一最终出口 | PASS | `DefaultResearchRunCommandService` 行 94 调用 `JudgeDecisionService.judge(...)`；ReflectionEntry / CheckpointEntry 仅作过程证据 |

## 5. 文档一致性结果

`README.md` / `AGENTS.md` / `docs/current/README.md` / `docs/current/STATUS.md` / `docs/current/WORKLOG.md` / `docs/current/TESTING.md` 在本次 VERIFY 前后均保持：

```text
Current stage: Stage2-PoC VERIFY 完成 (本报告生成后由 doc-state-bump 步骤切换)
Next stage:    Stage2-PoC FREEZE
Source of truth: docs/current
```

提交本报告同 commit，6 份文档同步 bump 到 "VERIFY completed / Next FREEZE"。

## 6. 契约一致性结果

| 契约对照 | 结果 | 处置 |
| --- | --- | --- |
| `contracts/openapi.yaml` ↔ `POST /api/ai/feedback/nq` 实现 | 修正 | 原 OpenAPI 声明 `200 + {eventId,duplicate}` + `400 ErrorResponse`；实际 controller 返回 `202 + NqFeedbackAcceptedResponse{eventId,status,outcome,traceId,correlationId}` (ACCEPTED 或 DUPLICATE) 与 `400 + NqFeedbackErrorResponse{error,errorCode,message,eventId,traceId,correlationId}`。本次 VERIFY 已在 `contracts/openapi.yaml` 增补 `NqFeedbackAcceptedResponse` / `NqFeedbackErrorResponse` 两个 schema 并切换 `/api/ai/feedback/nq` 响应到 `202` + 这两个 schema |
| `contracts/json-schema/*` ↔ Batch1 domain | PASS | `JsonSchemaPresenceTest` 锁定 16 份 schema：envelope + 8 个 NqFeedback payload + 3 个 DhBacktest* + Forecast / ExternalMarketSnapshot / Reflection / Checkpoint。对应 domain 类型一一齐备 |
| `V3__stage2_poc_tools.sql` ↔ `docs/current/DB_SCHEMA.md` | 修正 | SQL 文件名口径：实际 V2 为 `V2__dh_agent_runtime.sql`（内含 dh_nq_feedback_events），`DB_SCHEMA.md` 之前误写 `V2__nq_feedback_events.sql`，本次 VERIFY 已修正。4 张新表 / 2 张 ALTER 列与文档保持一致 |
| `docs/current/API.md` ↔ 实际 controller 路径 | 修正 | 修正前 `API.md` 在 "已实现端点" 仅列了 `/api/ai/feedback/nq` + 健康 + legacy，而 `/api/ai/research-runs/...` 7 条 (ResearchRunController) 被错放到 "Stage1 最小 API 草案"。VERIFY 已把这 7 条迁入 "已实现端点"，原 "草案" 章节改为 "Stage1 最小 API 集合（已实现，留作历史记录）" |

## 7. 必要小修汇总（本次 VERIFY 改动）

```text
contracts/openapi.yaml             /api/ai/feedback/nq 响应改为 202 + Accepted / 400 + Error，补两个 schema
docs/current/DB_SCHEMA.md          修正 V2 文件名为 V2__dh_agent_runtime.sql
docs/current/API.md                把已实现的 7 条 research-runs 端点移入 "已实现端点"
docs/current/STAGE2_POC_VERIFY_REPORT.md   新增（本报告）
docs/current/STATUS.md             bump 状态到 VERIFY completed / Next FREEZE
docs/current/WORKLOG.md            追加 VERIFY 段
docs/current/TESTING.md            追加 2026-05-26 VERIFY 结果
README.md / AGENTS.md / docs/current/README.md   状态字串同步
```

所有改动属于 "验证、文档修正、必要小修"，无新增业务功能。

## 8. 已知问题与延期事项

```text
PostgresContainerSmokeTest         需要 Docker，留给 Stage2-PoC FREEZE 后的 CI 环境单独执行
真实 NQ ingest endpoint 对齐      与 NQ 团队协调，进入下一阶段后处理
decisionhub.stage2.jdbc.enabled     默认 false（in-memory）；JDBC 仓储已就位，灰度切换列入下一阶段
OpenAPI 中 /api/ai/research-runs    Stage1 上线时未落盘到 openapi.yaml，列入 Stage3 文档补丁（不阻塞 FREEZE）
```

## 9. FREEZE 准入决定

```text
Verdict: GO
进入 Stage2-PoC FREEZE 的前置条件全部满足：
  - BUILD SUCCESS / 122 tests green
  - ArchUnit 10/10 守门通过
  - 硬边界扫描全部 PASS（无下单 / 无真实外部接入 / 无 NQ 仓库改动 / JudgeDecision 仍是唯一出口）
  - 契约与文档不一致项已全部修正
  - 无未关闭的阻断性缺陷
```

## 10. FREEZE 操作清单（移交给下一阶段）

```text
1. 将 docs/current 完整拷贝到 docs/gates/dh-stage2-poc/
2. 锁定 docs/gates/dh-stage2-poc/，禁止编辑
3. docs/current/STATUS.md 进入 "Stage2-PoC FREEZE completed / Next: Stage3 PLAN"
4. 与 NQ 团队对齐真实 ingest endpoint + 灰度切换 decisionhub.stage2.jdbc.enabled
5. 在装好 Docker 的 CI 环境补跑 PostgresContainerSmokeTest
```
