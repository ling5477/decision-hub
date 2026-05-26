# Decision Hub Status

> Current stage: Stage3-B3 DH Backtest Request Adapter IMPL completed
> Next stage:    Stage3-B2 NQ Feedback Outbox IMPL, blocked until NQ GateJ-FREEZE or isolated branch approval
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
Stage2-PoC FREEZE       2026-05-26 完成冻结：
                        - docs/current 完整快照拷贝到 docs/gates/dh-stage2-poc/
                        - docs/gates/dh-stage2-poc/README.md 顶部含冻结声明
                          （Verdict: GO，Next: Stage3-PLAN）
                        - 6 份当前文档状态推进到 "Stage2-PoC FREEZE completed / Next: Stage3-PLAN"
                        - 无 Java 业务代码变更；无 NQ 仓库变更；无 Stage3 新功能
Stage3-PLAN              2026-05-26 完成规划文档（仅 PLAN，不写代码、不动 NQ）：
                        - 新增 docs/current/STAGE3_PLAN.md（主索引、目标、范围、风险、验收）
                        - 新增 docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md（出站事件链路）
                        - 新增 docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md（入站回测请求）
                        - 新增 docs/current/STAGE3_CONTRACT_PLAN.md（契约 / status / errorCode / version）
                        - 新增 docs/current/STAGE3_TEST_PLAN.md（单测 / 联调 / 幂等 / 重试 / 边界）
                        - 新增 docs/current/STAGE3_WORK_ORDER.md（4 个 Batch IMPLEMENT 草案）
                        - 6 份状态文档同步到 "Stage3-PLAN completed / Next: Stage3-WO"
                        - 零 Java 业务代码改动；零 NQ 仓库改动；零 Stage3 实现代码
Stage3-WO                2026-05-26 完成工单细化（仅文档，不写代码、不动 NQ）：
                        - 重写 docs/current/STAGE3_WORK_ORDER.md 为可直接执行的 4 Batch 工单
                        - 新增 docs/current/STAGE3_BATCH_PLAN.md（Batch 1-4 边界 / 依赖 / 执行顺序）
                        - 6 份状态文档同步到 "Stage3-WO completed / Next: Stage3-B1 Contract Alignment"
                        - 零 Java 业务代码改动；零 NQ 仓库改动；零 Stage3 实现代码
Stage3-B1 Contract Alignment IMPLEMENT
                        2026-05-26 完成 Stage3 Batch1（只在 DH 仓库内对齐契约、schema、OpenAPI、测试与文档）：
                        - contracts/json-schema/nq-feedback-envelope.schema.json 补充 Stage3-B1 字段描述与示例
                          （eventId / eventType / occurredAt / sourceSystem / sourceJobId / traceId /
                          requestId / correlationId / schemaVersion / payloadJson）；不修改 required / enum /
                          additionalProperties 等结构语义
                        - contracts/json-schema/dh-backtest-request.schema.json 补充 Stage3-B1 字段描述；
                          required 维持 14 个；status 枚举 6 / frequency 枚举 3 一一对应 dh-domain
                        - contracts/json-schema/dh-backtest-result-snapshot.schema.json 补充 Stage3-B1 字段描述；
                          required 维持 9 个；verdict 枚举 3 一一对应 BacktestVerdict；winRate range [0,1]
                        - contracts/openapi.yaml info.description 加 Stage3-B1 硬边界声明；
                          components 段保留 Stage3-B1 planned contract 注释占位（NQ 端 endpoint），不新增 path；
                          /api/ai/feedback/nq 端点语义不变（202 NqFeedbackAcceptedResponse / 400 NqFeedbackErrorResponse）
                        - dh-domain/src/test 新增 4 份 contract 测试类共 29 cases：
                          NqFeedbackEnvelopeSchemaContractTest (7) /
                          DhBacktestRequestSchemaContractTest (7) /
                          BacktestResultSnapshotSchemaContractTest (6) /
                          OpenApiContractAlignmentTest (9)
                          检查项覆盖：schema 文件存在 + required 完整 + additionalProperties=false +
                          enum 与 dh-domain 枚举一致 + sourceSystem const + schemaVersion semver +
                          OpenAPI 与 NqFeedbackController 一致 + 黑名单关键词 / 危险路径全无
                        - mvn test -Dtest='!PostgresContainerSmokeTest' BUILD SUCCESS / 151 tests
                          (Stage2 122 + 新增 29) / ArchUnit 10/10 全绿
                        - 零 NQ 仓库改动；零真实 HTTP client；零下单 / 绕风控 / 重写回测核心；
                          不引入 TradingAgents Python / Kronos / global-stock-data
Stage3-B2 NQ Feedback Outbox PLAN
                        2026-05-26 完成 Stage3 Batch2（只在 DH 仓库内落 NQ outbox SPEC，不写 Java，不动 NQ）：
                        - 新增 docs/current/STAGE3_NQ_OUTBOX_SPEC.md（11 段完整规格）：
                          §1 目标与边界 + 硬禁止清单（不下单 / 不绕风控 / 不重写回测 /
                              不修改订单状态 / 不影响 GateJ-FREEZE）
                          §2 NQ 侧建议模块（nq-ai-contracts / nq-infra / nq-scheduler /
                              nq-app / nq-api admin）+ 硬边界模块清单（nq-core / nq-risk /
                              nq-backtest-kernel / nq-paper-engine / nq-live-engine / nq-ledger /
                              nq-fund-manager / nq-marketdata-core / nq-adapter-* / nq-console）+
                              模块职责矩阵（5 模块 × 5 维度）
                          §3 主表 nq_ai_feedback_outbox + 死信表 nq_ai_feedback_dead_letter
                              完整建表 SQL：19 列 + 5 CHECK 约束 + 4 索引 + 表/列 COMMENT；
                              status CHECK 5 值；event_type CHECK 8 值；source_system const；
                              schemaVersion semver 正则；payload jsonb；timestamps timestamptz；
                              不存密钥 / token / 账号凭证
                          §4 8 种事件触发点（每种含 NQ 来源模块 / 触发时机 / payload schema /
                              payload 来源 / eventId 生成 / 5 字段填充 / 重试与丢弃策略 /
                              对交易主链路影响声明）+ 8 种事件统一约束
                          §5 retry / dead-letter / audit：
                              5 状态机（PENDING / SENDING / SENT / FAILED / DEAD_LETTER）+
                              退避矩阵（1s/5s/30s/5min/30min/1h/6h，attempt 上限 8）+
                              429 退避不计死信上限 +
                              错误码分类（HTTP_400/401/403/429/5xx/TIMEOUT/NETWORK/PAYLOAD_BUILD）+
                              dead-letter 30 天 + admin 手动复发 +
                              每日双向对账（NQ outbox sent ⊇ DH events ⊆ NQ outbox sent+dead）+
                              主链路解耦（独立线程池 / 连接池）
                          §6 幂等与追踪规则：eventId / traceId / requestId / correlationId /
                              sourceJobId 五字段语义 + 不可混用 + DH 端校验顺序
                          §7 HTTP 交互规则：POST /api/ai/feedback/nq 请求与响应矩阵
                              （202 ACCEPTED|DUPLICATE / 400 + errorCode / 401 / 403 / 429 / 5xx）+
                              dispatcher 安全约束白名单 + 黑名单
                          §8 NQ 后续实施 5 个 Batch（NQ-1 Contract+DB / NQ-2 Outbox repo+fake /
                              NQ-3 8 事件源写入 / NQ-4 真实 dispatcher+retry+audit /
                              NQ-5 联调 contract test）每批含目标 / 允许 / 禁止 / 文件清单 / 验收
                          §9 风险与防护：不影响 GateJ-FREEZE / 失败隔离矩阵 / DH 不可用降级 /
                              事件重复发送幂等 / schema 演进 semver 双方协同
                          §10 验收标准（本轮 + NQ 后续 + 硬边界）
                          §11 与 Stage3 其他文档的衔接
                        - 6 份状态文档同步到 "Stage3-B2 NQ Feedback Outbox PLAN completed /
                          Next: Stage3-B3 DH Backtest Request Adapter PLAN"
                        - mvn test -Dtest='!PostgresContainerSmokeTest' BUILD SUCCESS / 151 tests 全绿 /
                          ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest 全绿
                        - 零 Java 业务代码改动；零 NQ 仓库改动；零 contracts 修改；
                          零 Flyway migration 新增；零 OpenAPI path 新增；零真实 HTTP；
                          零下单 / 绕风控 / 重写回测核心；零 TradingAgents Python / Kronos /
                          global-stock-data 接入
Stage3-B3 DH Backtest Request Adapter PLAN
                        2026-05-26 完成 Stage3 Batch3（只在 DH 仓库内规划 DH -> NQ backtest request adapter
                        SPEC；不写 Java，不动 NQ，不接真实 HTTP）：
                        - 新增 docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md（14 段完整规格）：
                          §1 目标与边界 + 关键不变量（DH/NQ 互不强依赖）+ 硬禁止清单 + 价值边界声明
                          §2 可插拔原则 10 条 + 三层 gate（stage3.nq.enabled / backtest-request.enabled /
                              fake-mode）+ 装配真值表 + 失败降级矩阵
                          §3 建议 DH 侧模块与类清单：
                              dh-usecase（DhBacktestRequestService + Default + Command + Result +
                              Outcome + ErrorCode + Repository + InMemory）
                              dh-connector（NqBacktestClient 端口 + Fake + Disabled + Real +
                              Properties + DisabledException）
                              dh-infra（可选 JdbcDhBacktestRequestRepository）
                              dh-app（Stage3NqBacktestWiringConfig）
                              ArchUnit 建议 R11 / R12（不破坏既有 10 条）
                          §4 9 状态机（CREATED / VALIDATED / SUBMITTED / ACCEPTED / RUNNING /
                              RESULT_READY / FAILED / DISABLED / CANCELLED）+ 合法迁移表 +
                              非法迁移拒绝规则 + DH 不允许自行成功的硬规则
                          §5 DH -> NQ 请求契约：wire-level（不变）+ Stage3-B3 Command 模型字段映射表 +
                              字段语义与限制 + 禁止字段清单
                          §6 NQ 接收契约草案：endpoint + auth + headers + 8 种响应矩阵
                              （202 / 400 / 401 / 403 / 409 / 423 / 429 / 5xx）+ 错误码映射表 +
                              NQ 不允许的行为 + 默认关闭
                          §7 Fake / Disabled / Real 三种 client 策略：装配条件 / 行为 / 约束 / 切换路径
                          §8 幂等与重试规则：requestId / 24h 短路 / paramsHash / 8 attempt 退避矩阵 /
                              429 不计死信上限 / result snapshot 三字段对齐
                          §9 DH 消费 NQ result snapshot 规则：来源唯一 + 经验沉淀路径 + 缺字段处理 +
                              不覆盖 NQ 正式回测记录
                          §10 配置建议：DH application.yml + NQ application.yml + prod 默认值
                              （DH 与 NQ 双方默认全部 false）+ 配置敏感性约束
                          §11 测试规划：8 个测试类清单 + 测试目标矩阵 + ArchUnit R11/R12 配套
                          §12 后续 Stage3-B3 IMPLEMENT 5 个 Batch（B3-1 Contract+Service /
                              B3-2 Fake+Disabled Client / B3-3 Real Client Skeleton /
                              B3-4 Result Snapshot Consumption / B3-5 Tests+Docs）
                              每批含目标 / 允许 / 禁止 / 文件清单 / 验收
                          §13 验收标准（本轮 + 后续 IMPL + 硬边界）
                          §14 与 Stage3 其他文档的衔接
                        - 6 份状态文档同步到 "Stage3-B3 DH Backtest Request Adapter PLAN completed /
                          Next: Stage3-B4 End-to-End Contract Test PLAN"
                        - mvn test -Dtest='!PostgresContainerSmokeTest' BUILD SUCCESS / 151 tests 全绿 /
                          ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest 全绿
                        - 零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
                          零 contracts/json-schema 修改；零 Flyway migration 新增；零 OpenAPI path 新增；
                          零真实 HTTP；零下单 / 绕风控 / 重写回测核心；零 TradingAgents Python /
                          Kronos / global-stock-data 接入
Stage3-B4 End-to-End Contract Test PLAN
                        2026-05-26 完成 Stage3 Batch4（只在 DH 仓库内规划 DH/NQ 端到端契约测试 SPEC；
                        不写 Java，不动 NQ，不真实联调，不接实盘）：
                        - 新增 docs/current/STAGE3_E2E_CONTRACT_TEST_SPEC.md（11 段完整规格）：
                          §1 目标与边界（验证契约面，不验证实盘收益）+ 硬边界 +
                              联调前置条件（NQ-1..NQ-4 + B3-1..B3-5 + 双方 oncall 评审）
                          §2 测试环境规划：
                              DH staging（profile stage3-test 或 staging / 独立 namespace /
                              tenantId 前缀 t-test-* / 禁止实盘配置）
                              NQ test cluster（profile local/test/stage3-test /
                              AI 默认关闭 / live trading 必须关闭）
                              网络与配置（DH/NQ 双向 base url + timeout + retry + auth token +
                              disabled mode + fake mode + 出站白名单 + 回滚预案）
                          §3 7 个端到端联调用例 T1-T7：
                              T1 PAPER_RUN_CREATED feedback（5 字段端到端对账）
                              T2 PAPER_RUN_ALERT_RAISED 幂等（重放 → 202 DUPLICATE）
                              T3 BACKTEST_RESULT_READY 结果消费（DH 不覆盖 NQ 正式回测记录）
                              T4 backtest request accepted（DH → NQ 主路径 + jobId 持久化）
                              T5 disabled mode（DisabledClient 零 HTTP + ResearchRun 不阻塞）
                              T6 outbox retry / dead-letter（退避矩阵 + DEAD_LETTER + 主链路保护）
                              T7 安全边界扫描（关键词 / 配置 / 双向无依赖启动 / 凭证 / 实盘隔离）
                          §4 10 类 Contract Test 类型：JSON Schema / OpenAPI / HTTP status matrix /
                              Error code matrix / Idempotency / Retry+dead-letter /
                              Disabled startup / No dangerous endpoint / Trace correlation / Regression
                          §5 测试数据与追踪规则：5 字段生成规则 + payload 留档 + deterministic 数据 +
                              tenantId t-test-* 严格遵守
                          §6 验收命令规划：DH 默认 profile + CI Docker + Stage3 联调（ENABLED_STAGE3=true）+
                              NQ 仓库命令 + 端到端联调 19 步 checklist
                          §7 失败处理规则：DH 入站 10 种响应映射 + DH 出站 10 种响应映射 +
                              联调用例失败处理（6 优先级排查 + 1 小时回滚）+ NQ 主链路保护
                          §8 后续 Stage3-B4 IMPLEMENT 5 个 Batch：
                              B4-1 DH contract test suite（@EnabledIfEnvironmentVariable 隔离）
                              B4-2 NQ contract test fixture plan（NQ 团队执行）
                              B4-3 Stub server / fake server（WireMock / MockWebServer）
                              B4-4 Disabled mode startup test（DH 启动不依赖 NQ）
                              B4-5 End-to-end dry-run checklist（T1-T7 联调 + VERIFY_REPORT）
                              每批含目标 / 允许 / 禁止 / 文件清单 / 验收
                          §9 验收标准（本轮 + 后续 IMPL + 硬边界三段）
                          §10 与 Stage3 其他文档的衔接
                          §11 Stage3-PLAN-FREEZE 衔接（10 份 PLAN 文档清单 + FREEZE 路径 + IMPL → FREEZE → DH-FREEZE）
                        - 6 份状态文档同步到 "Stage3-B4 End-to-End Contract Test PLAN completed /
                          Next: Stage3-PLAN-FREEZE"
                        - mvn test -Dtest='!PostgresContainerSmokeTest' BUILD SUCCESS / 151 tests 全绿 /
                          ArchUnit 10/10 / Stage1ClosedLoopTest + Stage2ClosedLoopTest 全绿
                        - 零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
                          零 contracts/json-schema 修改；零 Flyway migration 新增；零 OpenAPI path 新增；
                          零真实 HTTP；零真实联调；零实盘；零下单 / 绕风控 / 重写回测核心；
                          零 TradingAgents Python / Kronos / global-stock-data 接入
Stage3-PLAN-FREEZE      2026-05-26 完成 Stage3 规划成果落盘冻结：
                        - 一致性核查 10 份 STAGE3_*.md：9 条核心原则口径一致
                          （可插拔 + 默认关闭 + 非强依赖 / NQ without DH 可运行 /
                           DH without NQ 走 fake-disabled 闭环 / outbox 旁路不阻塞主链路 /
                           backtest request 默认 disabled-fake / NQ 仍是唯一正式回测执行方 /
                           DH 不下单 + 不绕风控 + 不改订单状态 + 不重写回测核心 /
                           Stage3 只规划真实联调不接实盘 / IMPL 必须逐 Batch 执行）；
                           无措辞修订需要
                        - 落盘快照：docs/current/* 33 个文件完整复制到
                          docs/gates/dh-stage3-plan/（含 10 份 STAGE3_*.md）
                        - docs/gates/dh-stage3-plan/README.md 顶部加 8 行冻结声明 +
                          冻结范围说明 + Stage3-B1 IMPLEMENT 已先行完成特别说明 +
                          Stage3 后续不允许做的事清单 + 后续允许进入的工单清单 +
                          验收命令与结果 + 模块测试分布 + 冻结快照文件清单 +
                          10 份 STAGE3_*.md 一致性核查表 + Stage3-PLAN 交付物清单
                        - 6 份状态文档同步到 "Stage3-PLAN-FREEZE completed /
                          Next: Stage3-B1 IMPLEMENT"
                        - mvn test -Dtest='!PostgresContainerSmokeTest' BUILD SUCCESS /
                          151 tests / 0 failures / 0 errors / 0 skipped / ArchUnit 10/10 /
                          Stage1ClosedLoopTest + Stage2ClosedLoopTest + Stage3-B1 29 contract tests
                          全部回归基线保持
                        - 零 Java 业务代码改动；零 NQ 仓库改动；零 contracts/openapi.yaml 修改；
                          零 contracts/json-schema 修改；零 Flyway migration 新增；零 OpenAPI path 新增；
                          零真实 HTTP；零真实联调；零实盘；零下单 / 绕风控 / 重写回测核心；
                          零 TradingAgents Python / Kronos / global-stock-data 接入
                        - 冻结后任何 Stage3 推进必须在新工单中单独开工
Stage3-NEXT-STATUS-FIX  2026-05-26 修正 PLAN-FREEZE 后 Next 指向：
                        - Stage3-B1 Contract Alignment 已于 2026-05-26 完成；
                          Next 不应再写 Stage3-B1 IMPLEMENT
                        - 修正 5 份状态文档 Next 字段为 Stage3-B2 NQ Feedback Outbox IMPL
                        - STATUS.md §4 新增 3 条执行口径：
                          * B1 already completed (2026-05-26)
                          * B2 must wait for NQ GateJ-FREEZE or isolated-branch approval
                          * DH-side B3 can proceed independently with fake/disabled mode
Stage3-B3 IMPL          2026-05-26 完成 DH Backtest Request Adapter 可插拔骨架：
                        - dh-usecase 新增 backtest 包 (9 类)：
                          DhBacktestRequestService 端口 + Default 实现 +
                          Command (Builder 风格 12 字段) + Result (4 工厂方法) +
                          Outcome 枚举 (6 值) + ErrorCode 枚举 (17 值 + isRetryable) +
                          Repository 端口 + InMemory 实现 (24h paramsHash 短路) +
                          DefaultDhBacktestRequestService (校验 + paramsHash sha256 +
                          24h 短路 + 不抛 RuntimeException 中断 caller)
                        - dh-connector (4 类 + 2 扩展)：
                          NqBacktestSubmitStatus 枚举 (ACCEPTED/DUPLICATE/DISABLED/FAILED) +
                          NqBacktestSubmitResult (5 工厂方法 + 6 字段) +
                          NqBacktestClient.submit(DhBacktestRequest) 默认方法 (typed) +
                          FakeNqBacktestClient typed deterministic submit
                          (jobId = "fake-job-" + sha256(requestId).take(16) + Clock 可注入) +
                          DisabledNqBacktestClient (DH gate 关闭；返回 DISABLED 不抛异常)
                        - dh-app (2 类 + 1 修改)：
                          NqBacktestClientProperties @ConfigurationProperties("decisionhub.stage3.nq")
                          (含 BacktestRequest 子配置 6 字段) +
                          Stage3NqBacktestWiringConfig (互斥 SpEL 三层 gate；
                          stage3.nq.enabled + backtest-request.enabled + fake-mode 真值表；
                          fake-mode=false 仍走 Fake 兜底 - 本轮无 RealNqBacktestClient) +
                          AgentRuntimeWiringConfig 移除 nqBacktestClient bean (由 Stage3 接管)
                        - dh-app ArchitectureTest 扩到 12 条：
                          R11 HTTP 客户端只允许出现在 connector.nq / config 之外 (RestTemplate /
                          WebClient / OkHttp3 / HttpURLConnection 全部封堵) +
                          R12 usecase.agent.backtest 不依赖 RealNqBacktestClient (类不存在;
                          占位 + 防御) 与 providers
                        - 8 个 B3 测试类共 39 cases 全绿：
                          * dh-connector: FakeNqBacktestClientTest 5 + DisabledNqBacktestClientTest 5
                          * dh-usecase: DhBacktestRequestServiceTest 5 +
                                          DhBacktestRequestIdempotencyTest 3 +
                                          DhBacktestResultSnapshotConsumptionTest 4
                          * dh-app: RealNqBacktestClientDisabledByDefaultTest 4 (4 profile case) +
                                     NoNqDependencyStartupTest 4 (无 NQ 时 DH 仍能启动)
                          * dh-domain: NoDangerousEndpointContractTest 6
                            (openapi.yaml 无危险关键词/路径段 + 16 schemas 无危险关键词 +
                             DhBacktestRequestStatus / NqFeedbackEventType 无危险前缀 +
                             NqFeedbackEventType 保持 8 值)
                        - mvn test BUILD SUCCESS / 190 tests (151 → 190, +39) /
                          0 failures / 0 errors / 0 skipped / ArchUnit 12/12 /
                          Stage1ClosedLoopTest + Stage2ClosedLoopTest + Stage3-B1 29 contract tests
                          全部回归基线保持全绿
                        - 零真实 HTTP；零 RealNqBacktestClient；零 contracts/openapi.yaml 修改；
                          零 contracts/json-schema 修改；零 Flyway migration 新增；零 OpenAPI path 新增；
                          零 NQ 仓库改动；零下单 / 绕风控 / 重写回测核心；
                          零 TradingAgents Python / Kronos / global-stock-data 接入
                        - 注：Stage3-B3 was executed before B2 because B2 touches NQ.
                              Stage3-B3 remains DH-only and fake/disabled.
                              NQ repository remains unchanged.
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

## 4. 下一阶段（Stage3-B2 NQ Feedback Outbox IMPL，blocked until NQ GateJ-FREEZE）

```text
按 docs/current/STAGE3_WORK_ORDER.md + docs/current/STAGE3_BATCH_PLAN.md 拆批实施：
- Batch 1  Contract Alignment IMPLEMENT     （DH 仓库内对齐契约 + schema + OpenAPI + 测试 + 文档）  ✅ DONE (2026-05-26)
- Batch 2  NQ Feedback Outbox PLAN          （DH 仓库内 SPEC：STAGE3_NQ_OUTBOX_SPEC.md）           ✅ DONE (PLAN)
- Batch 3  DH Backtest Request Adapter PLAN （DH 仓库内 SPEC：STAGE3_DH_BACKTEST_ADAPTER_SPEC.md）  ✅ DONE (PLAN)
- Batch 3  DH Backtest Request Adapter IMPL （DH 仓库内 IMPL：可插拔骨架，190 tests 全绿）        ✅ DONE (IMPL, 2026-05-26)
- Batch 4  End-to-End Contract Test PLAN    （DH 仓库内 SPEC：STAGE3_E2E_CONTRACT_TEST_SPEC.md）   ✅ DONE (PLAN)
- PLAN-FREEZE                                 （冻结快照 docs/gates/dh-stage3-plan/）                ✅ DONE

Stage3-PLAN 全部完工 + Stage3-B1/B3 DH 侧 IMPL 完工。

下一步执行口径（重要约束）：

- Stage3-B1 Contract Alignment already completed (2026-05-26).
- Stage3-B3 was executed before B2 because B2 touches NQ.
  Stage3-B3 remains DH-only and fake/disabled.
  NQ repository remains unchanged.
- Stage3-B2 touches NQ and must not start until NQ GateJ-FREEZE is complete
  or explicitly approved on an isolated branch.
  B2 是 NQ 仓库工作（按 STAGE3_NQ_OUTBOX_SPEC §8 / NQ-1..NQ-5）；
  NQ GateJ-FREEZE 未完工前 B2 不允许启动；
  即便有隔离分支启动也必须遵守 STAGE3_NQ_OUTBOX_SPEC §1.3 / §9 全部硬边界
  （不影响 GateJ-FREEZE / 不进入交易同步链路 / 不阻塞订单/风控/账本/回测）。
- DH-side Stage3-B3 已完成 (2026-05-26).
  仍可在后续单独工单中扩展 RealNqBacktestClient skeleton（仅 mock HTTP，不联调真实 NQ）；
  本轮 fake-mode=false 仍走 Fake 兜底，无 RealClient 类。

后续路径：
- Stage3-B2 NQ Feedback Outbox IMPL          NQ 团队实施，DH 仓库不动（blocked）
- Stage3-B3 Real Client Skeleton（可选）      DH 团队按 SPEC §12 / B3-3 实施 mock HTTP（不真实联调）
- Stage3-B4 End-to-End Contract Test IMPL    DH+NQ 联调，按 SPEC §8 实施（B4-1..B4-5）
- Stage3-VERIFY                               B4-5 联调 GO 后；产出 STAGE3_VERIFY_REPORT.md
- Stage3-FREEZE                               VERIFY GO 后；拷贝 docs/current 到 docs/gates/dh-stage3/
- DH-FREEZE                                   Stage3-FREEZE 后；DH Agent Decision Layer v1 长期维护态

每个 Batch 严格遵守：
- 不修改 NQ 仓库（B2 启动前；B2 启动期间隔离分支也不允许在 DH 仓库 mirror NQ 改动）
- 不接真实下单 / 不绕风控 / 不重写回测核心
- 不建设前端
- 不引入 TradingAgents Python / Kronos / global-stock-data 真实接入
- mvn test 全绿
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
