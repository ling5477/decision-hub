# Stage3 DH Backtest Request Adapter Spec

> Created: 2026-05-26 (Stage3-B3 DH Backtest Request Adapter PLAN)
> Stage: Stage3-B3 PLAN (本文件落盘即视为 B3 PLAN completed 草案，等待 STATUS 同步)
> Parent: `docs/current/STAGE3_PLAN.md`
> Sibling: `docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md`
>          `docs/current/STAGE3_NQ_OUTBOX_SPEC.md`
>          `docs/current/STAGE3_CONTRACT_PLAN.md`
>          `docs/current/STAGE3_WORK_ORDER.md`
> Scope: 本文件是 DH 侧 backtest request adapter 的可实施规格。
>        本轮只产出规划；零 Java 业务代码改动；零 NQ 仓库改动；零真实联调；
>        零 OpenAPI path 新增；零 Flyway migration 新增；零 contracts/openapi.yaml
>        与 contracts/json-schema/*.schema.json 语义修改。
>        所有 Java 类与配置项均为"建议路径"，实施阶段（B3-1~B3-5）以代码为准。

---

## 一、目标与边界

### 1.1 定位

```text
DH -> NQ backtest request 是 DH 的可选增强能力，不是 DH 主流程必要环节。
DH 只生成结构化 DhBacktestRequest；DH 不执行回测；
NQ 仍然是唯一正式回测执行方；DH 不重写 NQ 回测核心。
DH 不直接读取 NQ 数据库；DH 不直接调用 NQ 交易 / 风控 / 订单 / 账本 / 实盘接口。
本规格只用于后续 Stage3 实施工单参考；本轮（B3 PLAN）不写 Java；不联调真实 NQ；
不接真实 HTTP；不引入 Kronos / global-stock-data / TradingAgents Python。
```

### 1.2 关键不变量

```text
DH ResearchRun 主流程不依赖 NQ：
  - 无 NQ 时 DH 仍能完成 ResearchRun / Candidate 生成 / JudgeDecision / Fake 闭环
  - DH 默认 profile FakeNqBacktestClient 兜底，不连任何真实 HTTP
NQ 主流程不依赖 DH：
  - 无 DH 时 NQ 仍能完整启动 / 回测 / 模拟盘 / 实盘 / 风控 / 账本 / 审计
  - NQ 端 AI/backtest ingress 默认关闭（nq.ai.backtest-request.enabled=false）
契约不变：
  - contracts/json-schema/dh-backtest-request.schema.json 维持（Stage2-PoC-B1 落地 +
    Stage3-B1 描述补充；本 PLAN 不修改 required / enum / additionalProperties）
  - contracts/openapi.yaml 不新增 path（NQ 端 endpoint 仅在 components 段
    Stage3-B1 已加注释占位）
  - dh-domain.backtest 不修改
  - V1 / V2 / V3 Flyway migration 语义不变
```

### 1.3 硬禁止

```text
不让 DH 执行正式回测                  不让 DH 直接下单
不让 DH 绕过 NQ 风控                  不让 DH 修改 NQ 订单状态
不让 DH 读取或写入 NQ 交易核心表       不让 NQ 强依赖 DH 才能启动或运行
不让 DH 把 NQ verdict 当作 JudgeDecision 终态
不让 DH 在 ingest 路径自动发布策略 / 自动 paper / 自动下单
不让 DH 把 paper / live 触发包装成 backtest request 提交（绕风控）
不让 DH 在 ingest 路径触发新的 NQ 请求（避免回环）
不让 DH 在 ResearchRun 主流程依赖 RealNqBacktestClient
不让 DH 在 dh-connector.nq 之外引用 RestTemplate / WebClient / OkHttp / HttpURLConnection
不在 DhBacktestRequest 或 Command 中携带 API key / secret / 账号凭证 / 实盘 token
```

### 1.4 价值边界声明

```text
能力定位：DH "请求 NQ 执行正式回测"，不是"代替 NQ 执行回测"。
失败语义：NQ 不可用 -> DH 内部状态切 PENDING / FAILED / DISABLED；
          ResearchRun 主流程不感知（continue with Fake snapshot 或人工决策）。
默认安全：默认 profile 不连任何真实 HTTP；不消耗任何外部配额；不产生 NQ 入站流量。
切换路径：仅当运维显式配置 decisionhub.stage3.nq.enabled=true 且
          decisionhub.stage3.nq.backtest-request.enabled=true 时，才装配 RealNqBacktestClient。
```

---

## 二、可插拔原则

### 2.1 Pluggable Backtest Request Principle（10 条）

```text
1.  DH -> NQ backtest request 是可选增强能力，不是 DH 主流程必要环节。
2.  默认使用 FakeNqBacktestClient（@ConditionalOnMissingBean(NqBacktestClient.class) 兜底）。
3.  RealNqBacktestClient 只能在配置开启时装配（@ConditionalOnProperty 双重 gate）。
4.  建议配置：
        decisionhub.stage3.nq.enabled=false
        decisionhub.stage3.nq.backtest-request.enabled=false
5.  配置关闭时，DH 不创建真实 NQ HTTP client；不持有真实 HTTP 连接池；
    不消耗任何 NQ test cluster / prod cluster 配额。
6.  配置关闭时，所有 backtest request 走 Fake 兜底，或在专属 Disabled 模式下返回 outcome=DISABLED。
7.  NQ 不可用时（5xx / timeout / 网络错误 / 401 / 403 / 423），DH 降级为 PENDING /
    FAILED / DISABLED；不抛系统级 RuntimeException 阻断 ResearchRun 主流程。
8.  DH 没有 NQ 时仍能完成研究、候选生成、JudgeDecision 和 Fake 闭环
    （Stage1ClosedLoopTest / Stage2ClosedLoopTest 始终全绿）。
9.  NQ 没有 DH 时仍能完整启动、回测、模拟盘、实盘、风控、账本和审计
    （NQ GateJ-FREEZE 不允许因 DH 不可用而退化）。
10. NQ 侧 AI/backtest ingress 也必须默认关闭
    （nq.ai.enabled=false / nq.ai.backtest-request.enabled=false）。
```

### 2.2 三层 gate 模型

```text
gate-1：运维总开关
        decisionhub.stage3.nq.enabled              默认 false
        含义：Stage3 NQ 集成总开关；false 时 DH 不装配任何 stage3 nq bean
gate-2：能力子开关
        decisionhub.stage3.nq.backtest-request.enabled  默认 false
        含义：backtest-request 能力子开关；false 时即使 stage3.nq.enabled=true，
              仍走 Disabled / Fake 模式（不装配 RealNqBacktestClient）
gate-3：fake-mode override（仅测试用）
        decisionhub.stage3.nq.backtest-request.fake-mode  默认 true
        含义：fake-mode=true 时即便 enabled=true，仍走 FakeNqBacktestClient；
              用于 staging / 集成测试 mock；prod 不允许同时 enabled=true && fake-mode=true

装配真值表：

  stage3.nq.enabled | backtest-request.enabled | fake-mode | 装配结果
  ----------------- + ------------------------ + --------- + --------------------
  false             | (any)                    | (any)     | FakeNqBacktestClient（默认兜底）
  true              | false                    | (any)     | DisabledNqBacktestClient
  true              | true                     | true      | FakeNqBacktestClient
  true              | true                     | false     | RealNqBacktestClient
```

### 2.3 失败降级矩阵

```text
NQ 完全不可用（DNS / 连接拒绝 / 持续 5xx）：
  - RealNqBacktestClient 仍是装配的 bean（不动态卸载）
  - 单次请求结果：outcome = FAILED + errorCode = NETWORK / HTTP_5xx
  - ResearchRun 主流程不阻塞；caller 决定使用 Fake 兜底 / 跳过回测 / 人工介入
  - 不影响 Stage1ClosedLoopTest / Stage2ClosedLoopTest

NQ 端业务关闭（nq.ai.backtest-request.enabled=false，HTTP 423 AI_DISABLED）：
  - 单次请求结果：outcome = DISABLED + errorCode = NQ_AI_DISABLED
  - ResearchRun 主流程不阻塞
  - 不计入重试（永久状态）
  - 触发 DH oncall（仅信息级，不告警风暴）

DH 端运维关闭：
  - backtest-request.enabled=false -> DisabledNqBacktestClient -> outcome = DISABLED
  - 所有调用立即返回；不发 HTTP
```

---

## 三、建议 DH 侧模块与类

> 仅规划，本 PLAN 不创建文件。最终路径以 B3-1~B3-5 IMPL 阶段代码为准。

### 3.1 dh-usecase 建议类清单

```text
dh-usecase/src/main/java/.../usecase/agent/backtest/
  DhBacktestRequestService                       端口接口
                                                 单一方法：submit(DhBacktestRequestCommand) : DhBacktestRequestResult
  impl/DefaultDhBacktestRequestService           默认实现：
                                                   - 构造 DhBacktestRequest（参见 §5）
                                                   - 校验入参
                                                   - 调用 NqBacktestClient 端口（不直接持有 HTTP）
                                                   - 写 DhBacktestRequestRepository（如启用持久化）
                                                   - 返回 DhBacktestRequestResult

dh-usecase/src/main/java/.../usecase/agent/backtest/dto/
  DhBacktestRequestCommand                       入参：candidateId / strategyName / strategyVersion /
                                                       strategyParametersJson / startDate / endDate /
                                                       initialCapital / symbols / frequency / requestedBy /
                                                       correlationId / traceId（沿用 ResearchRun.traceId）/
                                                       optional: feeModel / slippageModel / market / payloadContextJson
                                                 - 不携带 requestId（由 service 端 IdGenerator 生成）
                                                 - 不携带 sourceJobId（由 NQ 端在 202 响应中给出）
                                                 - 不携带凭证 / token / API key
  DhBacktestRequestResult                        出参：requestId / status / outcome / errorCode? /
                                                       sourceJobId? / acceptedAt? / paramsHash /
                                                       retryable: boolean / receivedSnapshot? (后续异步填充)
  DhBacktestRequestOutcome                       枚举：ACCEPTED / DUPLICATE / DISABLED / FAILED /
                                                       FAKE_ACCEPTED / IDEMPOTENT_SHORT_CIRCUIT
  DhBacktestRequestErrorCode                     枚举：见 §6.3 错误码映射表

dh-usecase/src/main/java/.../usecase/agent/backtest/
  DhBacktestRequestRepository                    端口（可选；B3-1 决定是否引入）
                                                 - save(requestId, candidateId, paramsHash, traceId, sentAt, status)
                                                 - findByRequestId(requestId)
                                                 - findByCandidateAndParamsHashWithin24h(candidateId, paramsHash, now)
                                                 - updateStatus(requestId, newStatus, sourceJobId?, errorCode?, lastErrorMessage?)
  inmemory/InMemoryDhBacktestRequestRepository   默认 InMemory 兜底（Stage3 不引入 JDBC，可选 B3-4）
```

### 3.2 dh-connector 建议类清单

```text
dh-connector/src/main/java/.../connector/nq/
  NqBacktestClient                               端口接口（Stage1 已有 Fake；本 PLAN 沿用 + 扩展）
                                                 - submit(DhBacktestRequest) : DhBacktestRequestAccepted
                                                 - 不暴露 HTTP 客户端实现细节
  FakeNqBacktestClient                           Fake 实现（Stage1 已有）
                                                 - @ConditionalOnMissingBean(NqBacktestClient.class) 兜底装配
                                                 - 默认返回 deterministic accepted（jobId = "fake-" + UUIDv7）
                                                 - 不发 HTTP；不消耗外部资源
  DisabledNqBacktestClient                       新增 Disabled 实现
                                                 - 装配时机：stage3.nq.enabled=true && backtest-request.enabled=false
                                                 - submit() 返回 outcome=DISABLED + errorCode=DH_DISABLED
                                                 - 不抛 RuntimeException
                                                 - 不发 HTTP
  RealNqBacktestClient                           真实 HTTP 实现（B3-3 IMPLEMENT 阶段允许，本 PLAN 不写）
                                                 - 仅在 stage3.nq.enabled=true && backtest-request.enabled=true
                                                   && backtest-request.fake-mode=false 时装配
                                                 - 仅在 dh-connector.nq 模块内引用 RestTemplate / WebClient / OkHttp
                                                   （ArchUnit 守门）
                                                 - timeout：connect=10s / read=30s 建议；可配置
                                                 - 错误映射：见 §6.3
  NqBacktestClientProperties                     @ConfigurationProperties("decisionhub.stage3.nq.backtest-request")
                                                 字段：enabled / fake-mode / timeout-ms / base-url / endpoint-path /
                                                       max-retries（仅当 B3-3 加重试时启用）
  exception/NqBacktestClientDisabledException    可选：service 层用于显式抛出 "disabled" 语义
                                                 注：推荐改用 DisabledBacktestClientResult 返回值，
                                                     不用受检异常打断 caller 流程
```

### 3.3 dh-infra 建议类清单（仅当 B3-4 决定持久化时）

```text
dh-infra/src/main/java/.../infra/jdbc/
  JdbcDhBacktestRequestRepository                JDBC 实现（B3-4 才允许，本 PLAN 不写）
                                                 - @ConditionalOnProperty(decisionhub.stage3.jdbc.enabled=true)
                                                 - 写入 dh_backtest_request_outbox 表（B3-4 视情况新增
                                                   V4__stage3_dh_outbox.sql）
                                                 - 默认 profile 仍走 InMemoryDhBacktestRequestRepository

注意：
  - 本 PLAN 不新增 Flyway migration；V4 是否落地由 B3-4 决定，B3 PLAN 仅规划字段
  - 若不落 V4，则 Stage3 全程走 InMemory；幂等短路依赖 ConcurrentHashMap（仅单实例有效）
```

### 3.4 dh-app 建议类清单

```text
dh-app/src/main/java/.../app/config/
  Stage3NqBacktestWiringConfig                   新增 @Configuration
                                                 - bean 1: DhBacktestRequestService（DefaultDhBacktestRequestService）
                                                 - bean 2: DhBacktestRequestRepository
                                                            -> InMemoryDhBacktestRequestRepository
                                                            @ConditionalOnMissingBean
                                                 - bean 3: NqBacktestClient（三选一）
                                                            -> RealNqBacktestClient
                                                                @ConditionalOnProperty(
                                                                  "decisionhub.stage3.nq.enabled=true" AND
                                                                  "decisionhub.stage3.nq.backtest-request.enabled=true" AND
                                                                  "decisionhub.stage3.nq.backtest-request.fake-mode=false")
                                                            -> DisabledNqBacktestClient
                                                                @ConditionalOnProperty(
                                                                  "decisionhub.stage3.nq.enabled=true" AND
                                                                  "decisionhub.stage3.nq.backtest-request.enabled=false")
                                                            -> FakeNqBacktestClient
                                                                @ConditionalOnMissingBean(NqBacktestClient.class)
                                                 - bean 4: NqBacktestClientProperties（@ConfigurationProperties）

注意：
  - 多个 @ConditionalOnProperty 用 Spring `name+havingValue` 组合表达；
    Spring 不直接支持 AND，需要单 bean 多注解 / 自定义 Condition 或拆分配置类
  - 本 PLAN 仅声明意图，B3-1 / B3-2 实施阶段按 Spring 习惯落地（推荐自定义 Condition class）
  - 默认 profile（所有 enabled=false）必须装配 FakeNqBacktestClient，不允许空 bean
```

### 3.5 ArchUnit 建议规则（可选；不破坏既有 10 条）

```text
新增规则 R11（B3-1 IMPL 阶段允许，本 PLAN 仅声明意图）：
  除 dh-connector.nq 外的任何模块不得引用：
    - org.springframework.web.client.RestTemplate
    - org.springframework.web.reactive.function.client.WebClient
    - okhttp3.*
    - java.net.HttpURLConnection
  违反 -> ArchitectureTest 红

新增规则 R12（可选）：
  dh-usecase.agent.backtest 不得引用 dh-connector.nq.RealNqBacktestClient（依赖端口 NqBacktestClient）
  违反 -> ArchitectureTest 红

规则总数上限：12（不允许放松 R1-R10）
```

---

## 四、状态模型

### 4.1 9 个状态

```text
CREATED         DH 端 service 构造完 Command，尚未做幂等短路 / 校验
VALIDATED       已通过校验（symbols / dates / capital / frequency / paramsHash 等）
SUBMITTED       已调用 NqBacktestClient.submit()；尚未收到 NQ 响应
ACCEPTED        NQ 同步响应 202 + DhBacktestRequestAccepted，已记录 sourceJobId
RUNNING         NQ 仍在跑回测（DH 通过外部事件回写；可选；多数情况下 DH 直接等待 RESULT_READY）
RESULT_READY    收到 NQ feedback BACKTEST_RESULT_READY，已落 DhBacktestResultSnapshot
FAILED          调用失败（5xx / 网络 / 序列化 / 400 永久错误）；可重试与否见 §8
DISABLED        backtest-request 能力当前关闭（DH 端或 NQ 端 423 AI_DISABLED）
CANCELLED       人工或上游主动取消（admin 操作；本 PLAN 不实现 admin endpoint）
```

### 4.2 状态机迁移规则

```text
合法迁移：
  CREATED      -> VALIDATED            校验通过
  CREATED      -> FAILED               校验失败（参数非法）
  VALIDATED    -> SUBMITTED            client.submit() 已发起
  VALIDATED    -> DISABLED             DisabledNqBacktestClient 兜底 / DH gate 关闭
  VALIDATED    -> ACCEPTED             FakeNqBacktestClient 直接返回 ACCEPTED
  SUBMITTED    -> ACCEPTED             NQ 202 + DhBacktestRequestAccepted
  SUBMITTED    -> FAILED               NQ 4xx 永久错误 / 5xx 重试耗尽 / 网络
  SUBMITTED    -> DISABLED             NQ 423 AI_DISABLED
  ACCEPTED     -> RUNNING              （可选）NQ 阶段性进度事件
  ACCEPTED     -> RESULT_READY         feedback BACKTEST_RESULT_READY 命中
  ACCEPTED     -> FAILED               feedback verdict=FAIL 或超时无回传
  RUNNING      -> RESULT_READY         feedback BACKTEST_RESULT_READY
  RUNNING      -> FAILED               feedback verdict=FAIL 或超时
  CREATED|VALIDATED|SUBMITTED|ACCEPTED|RUNNING -> CANCELLED   admin 操作

终态：
  RESULT_READY / FAILED / DISABLED / CANCELLED 都是终态
  不允许从终态回到非终态（admin 复发只能新建新 requestId）

非法迁移（必须拒绝）：
  任何状态 -> CREATED                   不允许回退
  RESULT_READY -> 任何非终态状态         不允许"重新跑"原 requestId
  FAILED      -> RESULT_READY           DH 不允许自行标记成功
  DISABLED    -> RESULT_READY           DH 不允许在 disabled 状态下接收结果
```

### 4.3 DH 不允许"自行成功"的硬规则

```text
RESULT_READY 状态来源唯一：
  必须来自 NqFeedbackEnvelope(eventType=BACKTEST_RESULT_READY) 的合法 ingest；
  必须通过 §9 完整校验链（traceId / requestId / candidateId / sourceJobId 全部命中）

DH 端禁止：
  - 在 Fake 路径直接标记 RESULT_READY（Fake 只能返回 FAKE_ACCEPTED，不模拟正式回测结果）
  - 在 ingest 路径之外的任何位置写 dh_backtest_result_snapshot 表
  - 把 NQ 的 verdict 当作 JudgeDecision 终态（JudgeDecision 仍是唯一最终出口）
  - 在 admin 接口里"硬改"requestId 的 status 到 RESULT_READY（admin 仅允许 CANCELLED）

判定来源唯一：
  DhBacktestResultSnapshot.verdict 必须等于 NqFeedbackEnvelope.payloadJson 中的 verdict；
  DH 不允许覆盖 NQ verdict；仅可附加 DH 侧 evaluator 评分（dh-eval BacktestResultScorer 计算）
```

---

## 五、DH -> NQ 请求契约

### 5.1 wire-level 契约（不变）

```text
wire schema：    contracts/json-schema/dh-backtest-request.schema.json
                 （Stage2-PoC-B1 落地 + Stage3-B1 描述补充；本 PLAN 不修改）
domain class：   dh-domain.backtest.DhBacktestRequest（Stage2-PoC-B1 落地）
required 14：    requestId / traceId / candidateId / strategyName / strategyVersion /
                 strategyParametersJson / startDate / endDate / initialCapital /
                 symbols / frequency / requestedBy / requestedAt / status
optional：       entryRulesRef / exitRulesRef
enums：          status (DhBacktestRequestStatus 6 值) / frequency (BacktestFrequency 3 值)
additionalProperties: false
```

### 5.2 Stage3-B3 Command 模型（DH 内部）

DH 内部使用 `DhBacktestRequestCommand` 作为 service 入参，与 wire schema 解耦。
Command 字段比 wire schema 多出"业务上下文"字段，由 service 转换为 wire `DhBacktestRequest`。

| Command 字段 | 来源 | 落到 wire schema 何处 | 备注 |
| --- | --- | --- | --- |
| requestId | 由 service 生成（UUIDv7） | DhBacktestRequest.requestId | 幂等键 |
| traceId | 沿用 ResearchRun.traceId | DhBacktestRequest.traceId | 跨系统贯穿 |
| correlationId | DH 业务上下文 | header `X-Correlation-Id`（不落 wire body；非 wire 字段） | 跨 DH/NQ 关联 |
| sourceJobId | NQ 端 202 响应中给出 | 不存在于请求 body；存 DhBacktestRequestRepository | 来自 NQ |
| strategyCandidateId | StrategyCandidate.id | DhBacktestRequest.candidateId | 业务 ID |
| symbol（单值，可选） | 简化场景 | 转 symbols=[symbol] | 兼容 wire schema |
| symbols（列表） | StrategyCandidate.symbols | DhBacktestRequest.symbols | wire 主字段 |
| market（可选） | StrategyCandidate.market | 存 payloadContextJson | wire 当前无此字段 |
| frequency | StrategyCandidate.frequency 或 Command | DhBacktestRequest.frequency | DAILY/HOURLY/MINUTE |
| startTime / endTime | DH 业务区间 | DhBacktestRequest.startDate / endDate（date 部分） | wire 是 ISO date |
| initialCapital | DH 业务配置 | DhBacktestRequest.initialCapital | exclusiveMinimum 0 |
| feeModel（可选） | StrategyCandidate.feeModel | 存 payloadContextJson | wire 当前无此字段 |
| slippageModel（可选） | StrategyCandidate.slippageModel | 存 payloadContextJson | wire 当前无此字段 |
| payloadJson / payloadContextJson | DH 原始业务上下文 | 部分进 DhBacktestRequest.strategyParametersJson | 不允许包含密钥 |
| requestedBy | DH 调用者标识 | DhBacktestRequest.requestedBy | NQ 仅审计 |
| requestedAt | TimeProvider.now() | DhBacktestRequest.requestedAt | DH 端时间 |
| status | service 初始为 DRAFT；进入 outbox 切 QUEUED | DhBacktestRequest.status | wire 主字段 |

### 5.3 字段语义与限制

```text
requestId
  长度：    UUIDv7 推荐
  生成：    DH 出站 DhBacktestRequestService 内 IdGenerator
  唯一性：  全局唯一（DH 端 24h 内同 paramsHash 也短路；NQ 端 409 视为成功）
  幂等键：  本字段是 DH -> NQ 出站幂等键；不与 envelope.eventId 混用

traceId
  长度：    32 位 hex（OpenTelemetry trace id 对齐）
  生成：    DH 端 ResearchRun 创建时
  贯穿：    DH ResearchRun -> NQ Job -> NQ feedback 全程不变
  传递：    HTTP header X-Trace-Id + body.traceId 双通道
  约束：    NQ feedback envelope.traceId 必须命中 dh_research_runs.trace_id

correlationId
  长度：    UUIDv7 推荐
  生成：    DH 业务上下文（candidate 进入研究周期时分配）
  作用：    跨多个 requestId / traceId 串联同一业务周期
  约束：    不落 wire body（仅在 HTTP header 与 envelope.correlationId 中携带）；
            DH service 内通过 Command.correlationId 持有

sourceJobId
  长度：    NQ 端任意字符串（推荐 UUIDv7）
  生成：    NQ 端在同步响应 DhBacktestRequestAccepted.jobId 中给出
  存储：    DhBacktestRequestRepository(requestId, sourceJobId) 映射表
  约束：    本字段不是 DH 入参；DH 不允许伪造 sourceJobId

strategyCandidateId（= wire.candidateId）
  约束：    必须是 StrategyCandidate.frozen=true 的候选；不允许 in-flight candidate
  对账：    feedback BACKTEST_RESULT_READY.candidateId 必须 = 本字段

symbols / symbol / market
  约束：    minItems=1；不允许全市场扫描；测试环境仅 t-test-* 白名单
  规则：    若 Command 只给 symbol 单值，service 自动包装为 symbols=[symbol]；
            market 字段在当前 wire schema 中不直接存在，B3-1 决定是否引入

startTime / endTime
  类型：    Command 接受 LocalDate / OffsetDateTime；service 截 date 部分落 wire.startDate / endDate
  约束：    startDate < endDate，否则 service 抛 IllegalArgumentException

initialCapital
  约束：    exclusiveMinimum=0；超 NQ 限制由 NQ 返回 400 RISK_GATED / QUOTA_EXCEEDED

frequency
  枚举：    DAILY / HOURLY / MINUTE（BacktestFrequency 三值；Stage3-B3 不扩展）
  约束：    NQ 不支持的频率由 NQ 返回 400 UNSUPPORTED_FREQUENCY

feeModel / slippageModel
  当前 wire 无字段；B3 阶段先放在 strategyParametersJson 或独立 payloadContextJson；
  若 NQ 团队要求强类型字段，必须走 STAGE3_CONTRACT_PLAN.md §6 MAJOR 升级流程

payloadJson / payloadContextJson
  内容：    DH 原始业务上下文 JSON 字符串（StrategyCandidate snapshot / market notes / 调用者 ID 等）
  限制：    总长建议 < 64KB；不允许携带 API key / secret / token / 实盘账号凭证 / cookies
  落地：    部分进 strategyParametersJson（必须 JSON 解析合法）；其它进 DhBacktestRequestRepository 留底
```

### 5.4 禁止字段

```text
不允许 Command 或 wire body 携带：
  - 任何下单指令（orderId / fillId / positionId / liveAccountId 等）
  - 任何实盘账户凭证（apiKey / apiSecret / token / cookies / sessionId）
  - 任何"绕风控"标记（forceExecute / bypassRisk / placeOrder 等）
  - 任何 NQ 内部敏感配置（NQ 内部 endpoint / 数据库 DSN / NQ 内部 jobId 范围）
  - 任何 Kronos / global-stock-data / TradingAgents Python 真实数据集 URL
```

---

## 六、NQ 接收契约草案

> 本 PLAN 仅声明 NQ 未来 endpoint 的期望；本仓库 Stage3-B3 不新增 OpenAPI path，
> 不实现真实 HTTP；NQ 仓库的 endpoint 实施由 NQ 团队后续完成。

### 6.1 endpoint

```text
endpoint       POST /api/ai/backtest-requests           （NQ 端未来 endpoint，待 NQ 团队确认）
                注：与 STAGE3_DH_TO_NQ_BACKTEST_PLAN.md §2 中 /api/ai/research/backtest-requests
                    的命名差异由 Stage3-B3 IMPL 阶段与 NQ 团队最终确认；
                    本 PLAN 视两条命名为同一逻辑端点；不在 contracts/openapi.yaml 落 path
auth           NQ 端规定（建议 mTLS / 服务账号 token）；token 通过环境变量注入
content-type   application/json; charset=utf-8
body           DhBacktestRequest（contracts/json-schema/dh-backtest-request.schema.json）
headers        X-Trace-Id        = body.traceId（建议双通道）
               X-Correlation-Id  = Command.correlationId（wire body 不携带 correlationId 字段）
               X-Request-Id      = body.requestId（可选）
               Idempotency-Key   = body.requestId（可选；NQ 用于 409 判定）
               User-Agent        = "dh-backtest-client/<version>"
```

### 6.2 期望响应矩阵

```text
202 ACCEPTED + DhBacktestRequestAccepted
  body：{ requestId, jobId, status: QUEUED|ACCEPTED, acceptedAt }
  含义：NQ 已接收请求并创建异步回测任务（不直接实盘交易；不直接执行；进入 NQ backtest 队列）
  DH 行为：写 DhBacktestRequestRepository(requestId, jobId, ACCEPTED, acceptedAt)；
           状态机切 SUBMITTED -> ACCEPTED

400 INVALID_SCHEMA + 错误 body
  含义：请求结构错误 / 字段非法（symbols 空 / 日期反向 / 频率非法 / 资金 <= 0 等）
  errorCode 之一：INVALID_SYMBOLS / INVALID_DATE_RANGE / UNSUPPORTED_FREQUENCY /
                  INVALID_PARAMETERS_JSON / QUOTA_EXCEEDED / RISK_GATED
  DH 行为：状态机切 FAILED + errorCode；不重试；触发 DH oncall（INFO）

401 Unauthorized / 403 Forbidden
  含义：认证 / 授权错误（token 过期 / 权限缺失）
  DH 行为：状态机切 FAILED + errorCode = HTTP_401 / HTTP_403；不重试；触发 DH oncall（WARN）

409 DUPLICATE_REQUEST
  含义：NQ 已接收过同 requestId；NQ 视为 idempotent 成功
  DH 行为：状态机切 ACCEPTED（视为成功）+ 记录 outcome=DUPLICATE；
           尝试回填 sourceJobId（若响应 body 给出）；不再重发

423 AI_DISABLED
  含义：NQ 端 AI/backtest ingress 未开启（nq.ai.backtest-request.enabled=false）
  DH 行为：状态机切 DISABLED + errorCode = NQ_AI_DISABLED；不重试；
           ResearchRun 主流程不阻塞（caller 可选 Fake 兜底）

429 RATE_LIMITED
  含义：NQ 限流
  DH 行为：状态机保持 SUBMITTED；按退避矩阵重试；遵守 Retry-After header；
           retry_count 不计入死信上限

5xx Server Error / 网络超时 / 连接失败
  含义：NQ 临时错误
  DH 行为：状态机保持 SUBMITTED；按退避矩阵重试（参见 §8.2）

任何其他状态码：
  含义：协议违规
  DH 行为：视为 5xx 处理（重试）+ 告警
```

### 6.3 DH 端错误码（DhBacktestRequestErrorCode）映射

```text
DH 端错误码                NQ HTTP 响应              是否重试    备注
DH_VALIDATION_FAILED       (DH 本地校验失败)         否         CREATED -> FAILED
DH_DISABLED                (DH gate 关闭)            否         VALIDATED -> DISABLED
INVALID_SYMBOLS            400 INVALID_SCHEMA        否         参数错误
INVALID_DATE_RANGE         400 INVALID_SCHEMA        否
UNSUPPORTED_FREQUENCY      400 INVALID_SCHEMA        否
INVALID_PARAMETERS_JSON    400 INVALID_SCHEMA        否
QUOTA_EXCEEDED             400 INVALID_SCHEMA        否         NQ 业务规则
RISK_GATED                 400 INVALID_SCHEMA        否         NQ 风控
HTTP_401                   401                       否         认证错误
HTTP_403                   403                       否         授权错误
DUPLICATE_REQUEST          409                       否         视为成功；切 ACCEPTED
NQ_AI_DISABLED             423 AI_DISABLED           否         切 DISABLED
RATE_LIMITED               429                       是         不计死信上限
HTTP_5xx                   5xx                       是         按退避矩阵
TIMEOUT                    HTTP 客户端超时           是
NETWORK                    连接 / DNS / SSL 失败    是
PROTOCOL_VIOLATION         其他状态码               是         视为 5xx 处理
```

### 6.4 不允许的 NQ 行为

```text
NQ 接收 endpoint 不允许：
  - 在同步响应里直接给回测结果（必须异步通过 BACKTEST_RESULT_READY feedback 回传）
  - 在同步响应里返回任何与下单 / 实盘有关的字段
  - 触发实盘交易 / 修改仓位 / 写账本 / 调拨资金
  - 绕过 NQ 既有风控（DhBacktestRequest 必须走 NQ 风控配额）
  - 在 enabled=false 时仍接受请求（必须返回 423 AI_DISABLED）

NQ 端默认关闭（强制）：
  nq.ai.enabled=false
  nq.ai.backtest-request.enabled=false
  nq.ai.feedback.enabled=false  (沿用 STAGE3_NQ_OUTBOX_SPEC.md §8.1 决议)
```

---

## 七、Fake / Disabled / Real 三种 client 策略

### 7.1 FakeNqBacktestClient（默认）

```text
装配条件        @ConditionalOnMissingBean(NqBacktestClient.class)
                  OR
                stage3.nq.enabled=true && backtest-request.enabled=true && fake-mode=true
适用场景        - 本地开发（无 NQ 环境）
                - CI 集成测试
                - staging 配置错误时兜底
                - prod 默认配置（Stage3-B3 IMPL 完成后仍是默认）
行为
  submit(request)
    1. 校验 request 非空 + 必填字段齐全（同 Real 一致）
    2. 生成 deterministic jobId = "fake-job-" + sha256(requestId).take(16)
    3. 返回 DhBacktestRequestAccepted { requestId, jobId, status=ACCEPTED, acceptedAt=now }
    4. 不发任何 HTTP
    5. 不消耗外部资源
    6. 记录 INFO 日志：fake submit accepted（traceId / requestId / jobId）
约束
  - 不模拟 BACKTEST_RESULT_READY；Fake 不能伪造正式回测结果
  - 测试需要 result 时必须显式 mock NqFeedbackController 入站
  - outcome 永远不返回 FAKE_RESULT_READY（不允许越权标记结果就绪）
```

### 7.2 DisabledNqBacktestClient（关闭模式）

```text
装配条件        stage3.nq.enabled=true && backtest-request.enabled=false
适用场景        - 运维显式关闭 backtest-request 能力（如 NQ 联调暂停）
                - 与"未配置 stage3.nq.enabled"区分（后者走 FakeNqBacktestClient）
行为
  submit(request)
    1. 立即返回 DhBacktestRequestResult { outcome=DISABLED, errorCode=DH_DISABLED,
                                          retryable=false, jobId=null }
    2. 不校验字段（避免在 disabled 状态下报错；尽量减少副作用）
    3. 不发 HTTP；不写日志（或仅 DEBUG 级别）；不消耗外部资源
约束
  - 不抛 RuntimeException
  - 不影响 ResearchRun 主流程（caller 决定是否切 Fake 兜底）
  - admin 切 enabled=true 后立即生效（无需重启；通过 Spring Cloud Config 或 @RefreshScope，可选）
返回值与 Fake 的区别：
  - Fake.outcome    = ACCEPTED (deterministic)
  - Disabled.outcome= DISABLED (immediate)
```

### 7.3 RealNqBacktestClient（B3-3 IMPL 阶段）

```text
装配条件        stage3.nq.enabled=true && backtest-request.enabled=true && fake-mode=false
适用场景        - staging 联调（NQ test cluster）
                - prod 启用 NQ 集成后（仅在长期稳定后启用）
行为
  submit(request)
    1. 校验字段（同 Fake）
    2. 序列化为 JSON（Jackson；不允许携带凭证）
    3. POST baseUrl + endpointPath（默认 /api/ai/backtest-requests）
    4. timeout: connect=10s / read=30s 建议；可配置
    5. 按 §6.2 解析响应
    6. 失败时按 §8.2 退避重试（如启用 max-retries > 0）
    7. 返回 DhBacktestRequestResult
约束
  - 仅在 dh-connector.nq 模块内引用 RestTemplate / WebClient / OkHttp / HttpURLConnection
    （ArchUnit R11 守门）
  - 必须明确错误映射（§6.3）
  - 必须有连接池上限（建议 maxConnections=20 / maxConnectionsPerRoute=10）
  - 必须有指标采集（请求计数 / 延迟分位数 / 错误率），通过 Micrometer
  - 默认 fake-mode=true 时不装配；prod 启用时 fake-mode 必须显式 false
  - 不在 ingest 路径触发新 NqBacktestClient.submit()（避免回环）
  - 不读取 token / API key / 账号私密凭证从代码常量；必须从环境变量 / Vault 注入
本 PLAN 不实现此类。B3-3 IMPLEMENT 阶段才允许落地代码。
```

### 7.4 三 client 切换路径

```text
prod 默认部署
  decisionhub.stage3.nq.enabled=false                  -> FakeNqBacktestClient
  decisionhub.stage3.nq.backtest-request.enabled=false

staging 联调（NQ test cluster）
  decisionhub.stage3.nq.enabled=true
  decisionhub.stage3.nq.backtest-request.enabled=true
  decisionhub.stage3.nq.backtest-request.fake-mode=false
  decisionhub.stage3.nq.backtest-request.base-url=https://nq-test-cluster/...
  decisionhub.stage3.nq.backtest-request.timeout-ms=30000
                                                        -> RealNqBacktestClient

prod 启用集成（长期稳定后）
  同 staging，但 base-url 指向 prod NQ；并 tenantId 不允许 t-test-* 前缀

应急关闭（NQ 不可用时降级）
  decisionhub.stage3.nq.enabled=true
  decisionhub.stage3.nq.backtest-request.enabled=false   -> DisabledNqBacktestClient
                                                            （切换后立即生效；不影响 ResearchRun 主流程）

应急回退（彻底关闭 stage3）
  decisionhub.stage3.nq.enabled=false                    -> FakeNqBacktestClient（兜底）
```

---

## 八、幂等与重试规则

### 8.1 幂等键

```text
DH 端出站幂等键    requestId（UUIDv7）
24h 短路条件       同 candidateId + 同 paramsHash 在 24h 窗口内重复触发
paramsHash         sha256(candidateId || strategyVersion || strategyParametersJson ||
                          startDate || endDate || initialCapital || symbols.sorted ||
                          frequency || feeModel? || slippageModel?)
存储               - InMemory 默认：ConcurrentHashMap<paramsHash, RequestRecord>，TTL 24h
                   - JDBC 模式（B3-4 启用 V4）：唯一约束 (candidate_id, params_hash) + sent_at

NQ 端入站幂等键    body.requestId
NQ 行为            409 DUPLICATE_REQUEST -> DH 视为成功（参见 §6.2）；DH 切 ACCEPTED + outcome=DUPLICATE
```

### 8.2 重试矩阵

```text
默认 max-retries = 0（保守起点；B3-3 IMPL 决定是否提高到 8）
若启用 max-retries > 0：

  attempt | next_retry_at（基于上次失败时间）
  0       | 立即重试
  1       | + 1s
  2       | + 5s
  3       | + 30s
  4       | + 5min
  5       | + 30min
  6       | + 1h
  7       | + 6h
  8       | -> FAILED（不再重试）

抖动：±10% 随机偏移

429 限速：
  - 遵守 Retry-After header
  - retry_count 不计入 max-retries 上限（视为限速，无限退避）

400 / 401 / 403 / 423 / DH_VALIDATION_FAILED：
  - 立即终态（FAILED 或 DISABLED），不重试

5xx / TIMEOUT / NETWORK / PROTOCOL_VIOLATION：
  - 按退避矩阵重试
  - 重试期间状态保持 SUBMITTED；retry_count 累计

重试不阻塞 ResearchRun 主流程：
  - 重试运行在独立线程池 / 独立调度器（建议 @Async + 专用 ExecutorService）
  - submit() 同步返回的是"首次发送结果"；后续重试通过定时拉取 DhBacktestRequestRepository 完成
  - ResearchRun 主流程 caller 见 outcome 后决定是否等待（默认不等待，走 Fake 或人工）
```

### 8.3 result snapshot 对齐规则

```text
异步 BACKTEST_RESULT_READY 命中后，DH 端通过以下三字段联合对齐到 DhBacktestRequest：
  1. envelope.requestId        == DhBacktestRequestRepository(requestId).requestId
  2. envelope.correlationId    == 原 Command.correlationId（DH 持有）
  3. envelope.sourceJobId      == DhBacktestRequestRepository(requestId).sourceJobId

任一字段失配 -> DH ingest 返回：
  - traceId 失配          400 UNKNOWN_TRACE（已落地 Stage2-PoC-B2）
  - requestId 失配        DH 仅记录，不写经验沉淀（详见 §9）
  - sourceJobId 失配      DH 仅记录，不切 RESULT_READY 状态

DH 端处理顺序：
  1. envelope schema 校验（Stage2-PoC-B2 已落地）
  2. eventId 幂等查重
  3. traceId 反查 dh_research_runs
  4. requestId 反查 dh_backtest_request_outbox / InMemory 仓储
  5. sourceJobId 比对
  6. 命中后状态机切 ACCEPTED -> RESULT_READY；落 DhBacktestResultSnapshot
  7. 触发 §9 经验沉淀
```

---

## 九、DH 消费 NQ result snapshot 规则

### 9.1 来源唯一

```text
DhBacktestResultSnapshot 唯一来源：
  NqFeedbackEnvelope(eventType=BACKTEST_RESULT_READY) ingest 链路
  - Stage2-PoC-B2 已落地的 NqFeedbackController -> NqFeedbackIngestionService -> Router
  - Stage3-B3 不允许其它路径写入 result snapshot

禁止：
  - 在 RealNqBacktestClient.submit() 同步响应里读取 result（NQ 不允许同步返回结果）
  - 在 FakeNqBacktestClient 路径生成 fake result
  - 在 admin API 手动写入 result snapshot
  - 在 DH 端用 dh-eval BacktestResultScorer 计算后覆盖 NQ verdict
```

### 9.2 经验沉淀

```text
ingest 命中 BACKTEST_RESULT_READY 后，DH 端按以下顺序写入经验：
  1. 反查 dh_research_runs by traceId（失配 -> 400 UNKNOWN_TRACE，不沉淀）
  2. 反查 DhBacktestRequest by requestId（失配 -> 仅记录，不沉淀）
  3. 反查 StrategyCandidate by candidateId（失配 -> 仅记录，不沉淀）
  4. 落 DhBacktestResultSnapshot（contracts/json-schema/dh-backtest-result-snapshot.schema.json）：
       - resultId      DH 端 UUIDv7
       - requestId     = envelope.requestId
       - traceId       = envelope.traceId
       - candidateId   = envelope.payloadJson.candidateId
       - sharpeRatio / maxDrawdown / annualReturn / winRate / profitFactor  来自 payload
       - periodStart / periodEnd
       - verdict       来自 payload（DH 不覆盖）
       - recordedAt    DH 落地时间
       - rawPayloadJson 原始 payload 留底
  5. 更新 ExperienceEntry：
       - successKey = (strategyPattern, regime)
       - score 由 dh-eval BacktestResultScorer 计算（Stage1 已落地）
  6. 更新 PheromoneEdge：
       - verdict=PASS 加权
       - verdict=FAIL 衰减
       - verdict=MARGINAL 弱加权
  7. 写 dh_checkpoint_entries（type=BACKTEST_RESULT_RECEIVED）：
       - snapshotJson 引用 resultId
       - stepIndex 跟随 ResearchRun 已有 stepIndex

DH 不允许：
  - 自动发布策略 / 自动 paper / 自动下单（JudgeDecision 仍是唯一最终出口）
  - 把 NQ verdict 当作 JudgeDecision 终态
  - 写任何 NQ 数据
  - 把 result snapshot 反向同步回 NQ（DH 是只读消费方）
```

### 9.3 缺字段 / 非法字段处理

```text
若 envelope.requestId 缺失：
  - DH ingest 返回 400 INVALID_SCHEMA（Stage2-PoC-B2 已落地 envelope 必填校验）
  - NQ outbox 转 DEAD_LETTER（参见 STAGE3_NQ_OUTBOX_SPEC.md §5）

若 envelope.traceId 缺失或 dh_research_runs 失配：
  - DH ingest 返回 400 UNKNOWN_TRACE（Stage2-PoC-B2 已落地）

若 envelope.correlationId 缺失：
  - DH ingest 返回 400 INVALID_SCHEMA（envelope required 10 字段已包含 correlationId）

若 payload.candidateId 与 DhBacktestRequest.candidateId 不一致：
  - 仅记录 WARN 日志；不写 ExperienceEntry / PheromoneEdge；不切状态
  - 触发 DH oncall（视为 NQ 端错误回写）

若 payload.verdict 不在 {PASS / FAIL / MARGINAL}：
  - 视为 INVALID_SCHEMA；返回 400；不切状态

DH 不覆盖 NQ 正式回测记录：
  - DH 仅写 DhBacktestResultSnapshot（AI 侧快照表）
  - 不允许通过任何 endpoint 反向修改 NQ backtest_results 表
  - 不允许通过任何 endpoint 重发请求让 NQ 重新计算同 requestId
```

---

## 十、配置建议

### 10.1 DH 端配置（application.yml 或 application-stage3.yml）

```yaml
decisionhub:
  stage3:
    nq:
      # 总开关：Stage3 NQ 集成
      enabled: false
      # 仅在 enabled=true 时其他子配置生效
      base-url: ""
      backtest-request:
        # 能力子开关
        enabled: false
        # fake-mode=true 时即便 enabled=true 仍走 FakeNqBacktestClient
        fake-mode: true
        # 端点路径（NQ 团队最终命名以 STAGE3_DH_TO_NQ_BACKTEST_PLAN §2 为准）
        endpoint-path: "/api/ai/backtest-requests"
        # HTTP 客户端超时（仅 RealNqBacktestClient 使用）
        timeout-ms: 30000
        connect-timeout-ms: 10000
        # 重试上限（默认 0；B3-3 IMPL 决定是否提高）
        max-retries: 0
        # 连接池
        max-connections: 20
        max-connections-per-route: 10
        # 24h 幂等窗口
        idempotency-window-hours: 24
      # 认证（仅 RealNqBacktestClient 使用；token 从环境变量注入）
      auth:
        token-env: "DH_NQ_BACKTEST_TOKEN"
        # 或 mTLS：
        mtls:
          enabled: false
          cert-path: ""
          key-path: ""
          ca-path: ""
```

### 10.2 NQ 端配置建议（NQ 仓库后续落地）

```yaml
nq:
  ai:
    # 总开关
    enabled: false
    # backtest-request ingress 子开关
    backtest-request:
      enabled: false
      # 限速（建议）
      rate-limit-per-tenant-per-hour: 100
    # feedback outbox 子开关（沿用 STAGE3_NQ_OUTBOX_SPEC.md §8.1）
    feedback:
      enabled: false
      endpoint: "https://<dh-host>/api/ai/feedback/nq"
```

### 10.3 prod 部署默认值（强制）

```text
DH 端：
  decisionhub.stage3.nq.enabled                          = false
  decisionhub.stage3.nq.backtest-request.enabled         = false
  decisionhub.stage3.nq.backtest-request.fake-mode       = true

NQ 端：
  nq.ai.enabled                                          = false
  nq.ai.backtest-request.enabled                         = false
  nq.ai.feedback.enabled                                 = false

任何 prod 配置变更必须经过：
  1. 双方 oncall 评审
  2. 在 staging 完整跑 Stage3-B4 联调用例 T1-T7 全绿
  3. 通过 ChangeRequest / runbook 发布
```

### 10.4 配置敏感性约束

```text
不允许写入配置（必须环境变量 / Vault 注入）：
  - 任何真实 token / API key / secret
  - 任何实盘账户凭证
  - 任何 NQ 内部数据库 DSN
  - 任何 prod URL（如 nq prod cluster endpoint）必须区分 stage（staging / prod 配置文件分开）

允许写入配置：
  - 总开关与子开关（明确 false / true）
  - 超时与重试参数
  - 连接池上限
  - endpoint-path 字符串
  - token-env 变量名（不是 token 本身）
```

---

## 十一、测试规划

### 11.1 单测清单（B3-5 IMPLEMENT 阶段落地）

```text
1. FakeNqBacktestClientTest
   - happy path：合法 request -> deterministic ACCEPTED
   - empty request -> 抛 IllegalArgumentException
   - jobId 固定：sha256(requestId).take(16)
   - 重复请求 deterministic（相同 requestId -> 相同 jobId）

2. DisabledNqBacktestClientTest
   - submit() -> outcome=DISABLED + errorCode=DH_DISABLED + retryable=false
   - 不抛任何异常
   - 不发任何 HTTP（mock HttpClient 验证）

3. RealNqBacktestClientDisabledByDefaultTest
   - 默认 Spring Boot context（所有 properties 默认）-> bean 类型为 FakeNqBacktestClient
   - stage3.nq.enabled=true + backtest-request.enabled=false -> bean 类型为 DisabledNqBacktestClient
   - stage3.nq.enabled=true + backtest-request.enabled=true + fake-mode=true -> FakeNqBacktestClient
   - stage3.nq.enabled=true + backtest-request.enabled=true + fake-mode=false -> RealNqBacktestClient
     （仅断言 bean 类型；不真发 HTTP）

4. DhBacktestRequestServiceTest
   - build：从 Command 构造合法 wire DhBacktestRequest
   - validate：symbols 空 / dates 反向 / capital 非正 / frequency 非法 -> IllegalArgumentException
   - submit：调用 NqBacktestClient 端口 -> 写 DhBacktestRequestRepository -> 返回 DhBacktestRequestResult
   - 状态机：CREATED -> VALIDATED -> SUBMITTED -> ACCEPTED 路径
   - 状态机：CREATED -> VALIDATED -> DISABLED 路径（DisabledClient）
   - 状态机：SUBMITTED -> FAILED 路径（permanent 错误）

5. DhBacktestRequestIdempotencyTest
   - 24h 内同 paramsHash 重复 -> 短路返回原 requestId（outcome=IDEMPOTENT_SHORT_CIRCUIT）
   - 24h 外重复 -> 生成新 requestId
   - NQ 409 DUPLICATE -> outcome=DUPLICATE + 切 ACCEPTED
   - 并发同 paramsHash race -> 仅一个 SUBMITTED，其它短路

6. DhBacktestResultSnapshotConsumptionTest
   - feedback BACKTEST_RESULT_READY 命中 -> 落 DhBacktestResultSnapshot
   - 状态机 ACCEPTED -> RESULT_READY
   - requestId 失配 -> 仅记录，不沉淀
   - candidateId 失配 -> WARN 日志
   - verdict 非法 -> 400 INVALID_SCHEMA（参见 §9.3）
   - ExperienceEntry / PheromoneEdge 更新断言

7. NoNqDependencyStartupTest
   - Spring Boot context 在 NQ 完全不可达情况下成功启动（mock DNS 失败 / 端口不通）
   - 默认 profile bean wiring 验证 FakeNqBacktestClient
   - ResearchRun 主流程跑通（候选生成 / JudgeDecision / Fake 闭环）
   - 不存在任何 RestTemplate / WebClient / OkHttp 已实例化对象

8. NoDangerousEndpointContractTest
   - contracts/openapi.yaml paths 段不含 /orders / /trades / /live
   - contracts/openapi.yaml 全文不含 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute
   - contracts/json-schema/dh-backtest-request.schema.json 同样不含上述关键词
   - dh-domain.backtest.DhBacktestRequestStatus 枚举值不含 PLACE_ / SUBMIT_ / EXECUTE_ 前缀
   - 与 Stage3-B1 OpenApiContractAlignmentTest 重叠的部分确认仍全绿
```

### 11.2 测试目标矩阵

| 测试 | 验证目标 | 验证手段 |
| --- | --- | --- |
| 1 | 默认不发 HTTP；deterministic | mock HttpClient + 字节流断言 |
| 2 | DISABLED 立即返回；不抛异常 | 返回值 + mock HttpClient 调用计数 |
| 3 | 默认不创建 RealNqBacktestClient | Spring context bean 类型断言 |
| 4 | Service 构造与状态机正确 | 单测覆盖 8 路径 |
| 5 | requestId 幂等 | ConcurrentHashMap 短路 + NQ 409 处理 |
| 6 | result snapshot 只能由 NQ 事件驱动 | mock NqFeedbackController + DB 断言 |
| 7 | 无 NQ 时 DH 仍能启动 | Spring context + 端口断言 |
| 8 | 不存在危险关键词 / 路径 | contracts grep + ArchUnit |

### 11.3 ArchUnit 配套规则（B3-1 IMPL 决定是否落地）

```text
R11 非 dh-connector.nq 模块不得引用 RestTemplate / WebClient / OkHttp / HttpURLConnection
R12 dh-usecase.agent.backtest 不得引用 dh-connector.nq.RealNqBacktestClient（依赖端口 NqBacktestClient）

规则总数上限 12；不允许放松 R1-R10（Stage1-CLOSE 5 + Stage2-PoC-B5 5）
```

---

## 十二、后续 Stage3-B3 IMPLEMENT Batch 建议

> 本 PLAN 不实施。NQ 团队 / DH IMPL 团队按以下 Batch 拆批落地。

### 12.1 Batch B3-1：Contract + Service Interface

```text
目标
  - 落 dh-usecase 端 service / repository 接口与 DTO（不写 HTTP）
  - 落 wire 校验逻辑（与 contracts/json-schema/dh-backtest-request.schema.json 对齐）
  - 落 ArchUnit R11 / R12（如启用）

允许改动
  - dh-usecase 新增 DhBacktestRequestService / DefaultDhBacktestRequestService
  - dh-usecase 新增 DhBacktestRequestCommand / Result / Outcome / ErrorCode
  - dh-usecase 新增 DhBacktestRequestRepository 端口（不强制持久化）
  - dh-usecase 新增 inmemory/InMemoryDhBacktestRequestRepository
  - dh-app 新增 Stage3NqBacktestWiringConfig 装配 DhBacktestRequestService + Repository
  - dh-app 修改 ArchitectureTest（如启用 R11 / R12）

禁止改动
  - 不修改 contracts/openapi.yaml 已落地端点
  - 不修改 contracts/json-schema/dh-backtest-request.schema.json
  - 不新增 Flyway migration
  - 不引入真实 HTTP client
  - 不动 dh-connector.nq.RealNqBacktestClient（B3-3 才允许）
  - 不修改 NqFeedbackController（Stage2-PoC-B2 已落地）

文件清单（建议路径）
  dh-usecase/src/main/java/.../agent/backtest/DhBacktestRequestService.java
  dh-usecase/src/main/java/.../agent/backtest/impl/DefaultDhBacktestRequestService.java
  dh-usecase/src/main/java/.../agent/backtest/dto/DhBacktestRequestCommand.java
  dh-usecase/src/main/java/.../agent/backtest/dto/DhBacktestRequestResult.java
  dh-usecase/src/main/java/.../agent/backtest/dto/DhBacktestRequestOutcome.java
  dh-usecase/src/main/java/.../agent/backtest/dto/DhBacktestRequestErrorCode.java
  dh-usecase/src/main/java/.../agent/backtest/DhBacktestRequestRepository.java
  dh-usecase/src/main/java/.../agent/backtest/inmemory/InMemoryDhBacktestRequestRepository.java
  dh-app/src/main/java/.../config/Stage3NqBacktestWiringConfig.java
  dh-app/src/test/java/.../ArchitectureTest.java                (扩 R11 / R12)

验收标准
  - mvn test BUILD SUCCESS
  - 新增单测：DhBacktestRequestServiceTest 主路径覆盖
  - ArchUnit 全绿（10 -> 12 条；视情况启用 R11 / R12）
  - 默认 profile bean wiring 正常（FakeNqBacktestClient 兜底）
```

### 12.2 Batch B3-2：Fake / Disabled Client

```text
目标
  - 扩展 FakeNqBacktestClient（沿用 Stage1 已有 + 接 service 端口）
  - 新增 DisabledNqBacktestClient
  - 落配置项 NqBacktestClientProperties
  - 落三 client 切换断言（FakeNqBacktestClientTest / DisabledNqBacktestClientTest /
    RealNqBacktestClientDisabledByDefaultTest）

允许改动
  - dh-connector 修改 FakeNqBacktestClient（加 @ConditionalOnMissingBean）
  - dh-connector 新增 DisabledNqBacktestClient
  - dh-connector 新增 NqBacktestClientProperties
  - dh-app 修改 Stage3NqBacktestWiringConfig（落三 client 切换逻辑）
  - dh-app 测试 / dh-connector 测试

禁止改动
  - 不新增 RealNqBacktestClient（B3-3 才允许）
  - 不引入真实 HTTP 依赖
  - 不修改 contracts / migration

文件清单
  dh-connector/src/main/java/.../connector/nq/FakeNqBacktestClient.java            (修改)
  dh-connector/src/main/java/.../connector/nq/DisabledNqBacktestClient.java        (新增)
  dh-connector/src/main/java/.../connector/nq/NqBacktestClientProperties.java      (新增)
  dh-app/src/main/java/.../config/Stage3NqBacktestWiringConfig.java                 (修改)
  dh-connector/src/test/java/.../nq/FakeNqBacktestClientTest.java                   (新增)
  dh-connector/src/test/java/.../nq/DisabledNqBacktestClientTest.java               (新增)
  dh-app/src/test/java/.../config/RealNqBacktestClientDisabledByDefaultTest.java    (新增)

验收标准
  - mvn test BUILD SUCCESS
  - 三 client 切换断言全绿
  - 默认 profile FakeNqBacktestClient 装配
  - DisabledClient 切换断言通过
  - ArchUnit 全绿
```

### 12.3 Batch B3-3：Optional Real Client Skeleton（默认关闭，不真实联调）

```text
目标
  - 新增 RealNqBacktestClient 骨架（仅 HTTP POST + 解析响应）
  - 不联调真实 NQ；默认 fake-mode=true
  - mock HTTP 服务器（MockWebServer / WireMock）覆盖 5xx / 400 / 409 / 423 / 429

允许改动
  - dh-connector 新增 RealNqBacktestClient
  - dh-connector 新增 HTTP 客户端 bean（仅在 dh-connector.nq 内）
  - dh-app 修改 Stage3NqBacktestWiringConfig 装配条件
  - 新增 RealNqBacktestClientTest（mock HTTP）

禁止改动
  - 默认 profile 仍走 Fake；不允许 fake-mode 默认 false
  - 不连真实 NQ test cluster（B3-5 / Stage3-B4 才允许）
  - 不修改 contracts/openapi.yaml 落新 path
  - 不修改 NqFeedbackController
  - 不修改 dh-domain
  - 不允许在 dh-connector.nq 之外引用 RestTemplate / WebClient / OkHttp

文件清单
  dh-connector/src/main/java/.../connector/nq/RealNqBacktestClient.java            (新增)
  dh-connector/src/test/java/.../nq/RealNqBacktestClientTest.java                   (新增；mock HTTP)
  dh-app/src/main/java/.../config/Stage3NqBacktestWiringConfig.java                 (修改)

验收标准
  - mvn test BUILD SUCCESS
  - mock HTTP 测试覆盖 202 / 400 / 401 / 403 / 409 / 423 / 429 / 5xx / timeout / network
  - 默认 profile 仍走 Fake（断言 bean 类型）
  - ArchUnit R11 全绿（HTTP client 仅在 dh-connector.nq 内）
```

### 12.4 Batch B3-4：Result Snapshot Consumption

```text
目标
  - 落 BacktestResultReadyHandler 经验沉淀路径（dh-usecase.agent.feedback.handler）
  - 落 DhBacktestResultSnapshot 持久化（默认 InMemory；可选 JDBC）
  - 落 ExperienceEntry / PheromoneEdge 更新（仍走 InMemory 存储）
  - （可选）新增 V4__stage3_dh_outbox.sql 若决定持久化 DhBacktestRequest

允许改动
  - dh-usecase 修改 BacktestResultReadyHandler（接经验沉淀）
  - dh-usecase 新增 DhBacktestResultSnapshotRepository 端口 + InMemory 实现
  - dh-memory 修改 InMemoryExperienceStore / InMemoryPheromoneStore 补查询方法（如需）
  - dh-app 修改装配
  - 视情况新增 V4__stage3_dh_outbox.sql（不修改 V1/V2/V3）

禁止改动
  - 不修改 NqFeedbackController 入口（语义不变）
  - 不修改 NqFeedbackEnvelope schema
  - 不允许 DH 覆盖 NQ verdict
  - 不允许在非 ingest 路径写 DhBacktestResultSnapshot

文件清单
  dh-usecase/src/main/java/.../agent/feedback/handler/BacktestResultReadyHandler.java  (修改)
  dh-usecase/src/main/java/.../agent/backtest/DhBacktestResultSnapshotRepository.java   (新增)
  dh-usecase/src/main/java/.../agent/backtest/inmemory/InMemoryDhBacktestResultSnapshotRepository.java
                                                                                        (新增)
  dh-memory/src/main/java/.../memory/inmemory/InMemoryExperienceStore.java              (视情况修改)
  dh-memory/src/main/java/.../memory/inmemory/InMemoryPheromoneStore.java               (视情况修改)
  dh-infra/src/main/resources/db/migration/V4__stage3_dh_outbox.sql                     (可选)
  dh-app/src/main/java/.../config/Stage3NqBacktestWiringConfig.java                     (修改)

验收标准
  - mvn test BUILD SUCCESS
  - DhBacktestResultSnapshotConsumptionTest 全绿
  - 与 Stage2-PoC-B5 已落地的 ingest 路径兼容（NqFeedbackContractValidationTest 等仍全绿）
  - ArchUnit 全绿
```

### 12.5 Batch B3-5：Tests + Docs

```text
目标
  - 补齐 8 个测试类（参见 §11.1）
  - 联调 profile（@EnabledIfEnvironmentVariable）骨架（具体联调用例在 Stage3-B4）
  - 文档同步

允许改动
  - 新增 8 个测试类（如尚未在 B3-1~B3-4 落地）
  - 修改 ArchitectureTest 加 R11 / R12
  - 修改 docs/current/API.md / DB_SCHEMA.md（若 V4 落地）
  - 修改 6 份状态文档同步 "Stage3-B3 IMPLEMENT completed / Next: Stage3-B4 PLAN"

禁止改动
  - 不联调真实 NQ test cluster（Stage3-B4 才允许）
  - 不修改已落地 endpoint 语义
  - 不放松 ArchUnit 已落地规则

文件清单
  8 个测试类（按 §11.1）
  docs/current/API.md            (补 RealNqBacktestClient + stage3.nq.enabled flag 说明)
  docs/current/DB_SCHEMA.md      (若 V4 启用，补表结构)
  docs/current/STATUS.md / WORKLOG.md / TESTING.md / README.md / AGENTS.md / docs/current/README.md

验收标准
  - mvn test BUILD SUCCESS
  - 8 个测试类全绿
  - ArchUnit 10 -> 12 条（如启用 R11 / R12）
  - 6 份状态文档对齐
  - 文档与代码一致
```

### 12.6 Batch 依赖与执行顺序

```text
B3-1 Contract + Service Interface          先落用例层接口
  |
B3-2 Fake / Disabled Client                 三 client 切换骨架
  |
B3-3 Optional Real Client Skeleton          mock HTTP 骨架
  |
B3-4 Result Snapshot Consumption            经验沉淀路径
  |
B3-5 Tests + Docs                           收口

任一 Batch 验收失败 -> 回该 Batch 起点，禁止跳跃。
B3-1 ~ B3-5 完成后 -> Stage3-B4 End-to-End Contract Test PLAN。
```

---

## 十三、验收标准

### 13.1 本轮（Stage3-B3 DH Backtest Request Adapter PLAN）完成标准

```text
本仓库 Stage3-B3 仅落 SPEC 文档；零 Java 业务代码改动；零 NQ 仓库改动；零 contracts / migration 修改。

文档完成度：
  ✅ docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md 存在（本文件）
  ✅ 可插拔原则完整（Pluggable Backtest Request Principle 10 条 + 三层 gate + 失败降级矩阵）
  ✅ Fake / Disabled / Real 三种 client 策略完整（装配条件 / 行为 / 切换路径）
  ✅ DH/NQ 双方默认关闭原则明确（DH stage3.nq.enabled=false / NQ nq.ai.*.enabled=false）
  ✅ requestId / traceId / correlationId / sourceJobId 规则完整
  ✅ 状态模型（9 状态 + 迁移规则 + 终态唯一来源）
  ✅ 错误码映射完整（HTTP 状态码 -> DH 端 errorCode + retryable）
  ✅ Result Snapshot 消费规则完整（来源唯一 / 经验沉淀路径 / 缺字段处理）
  ✅ 配置建议完整（DH application.yml + NQ application.yml + prod 默认值）
  ✅ 测试规划完整（8 个测试类 + ArchUnit R11/R12 + 测试目标矩阵）
  ✅ 后续 Stage3-B3 IMPL 5 个 Batch 完整（B3-1 ~ B3-5）
  ✅ 硬边界明确（不下单 / 不绕风控 / 不重写回测 / 不直接读写 NQ）

文档同步：
  ✅ README.md / AGENTS.md / docs/current/README.md / STATUS.md / WORKLOG.md / TESTING.md
     同步到 "Stage3-B3 DH Backtest Request Adapter PLAN completed /
     Next: Stage3-B4 End-to-End Contract Test PLAN"

代码与构建：
  ✅ 无 Java 业务代码修改
  ✅ 无 contracts/openapi.yaml 修改
  ✅ 无 contracts/json-schema/*.schema.json 修改
  ✅ 无 Flyway migration 新增 / 修改
  ✅ 无 NQ 仓库改动
  ✅ mvn test -Dtest='!PostgresContainerSmokeTest' -Dsurefire.failIfNoSpecifiedTests=false BUILD SUCCESS
  ✅ ArchUnit 10/10 保持
  ✅ Stage1ClosedLoopTest / Stage2ClosedLoopTest 保持全绿
  ✅ 151 tests（Stage3-B1 / B2 基线）保持，未下降
```

### 13.2 后续 Stage3-B3 IMPL 验收口径（B3-1 ~ B3-5 实施后）

```text
代码完成度：
  - DhBacktestRequestService / Result / Outcome / ErrorCode 落地
  - FakeNqBacktestClient / DisabledNqBacktestClient / RealNqBacktestClient 三 client
  - Stage3NqBacktestWiringConfig 三层 gate 装配
  - 8 个测试类全绿
  - ArchUnit 10 -> 12 条（如启用 R11 / R12）

行为完成度：
  - 默认 profile 装配 FakeNqBacktestClient（mock 验证）
  - decisionhub.stage3.nq.enabled=true && backtest-request.enabled=false -> DisabledClient
  - decisionhub.stage3.nq.enabled=true && backtest-request.enabled=true && fake-mode=true -> Fake
  - decisionhub.stage3.nq.enabled=true && backtest-request.enabled=true && fake-mode=false -> Real
  - DH 启动不依赖 NQ 可达
  - NQ 不可用时 DH 主流程不阻塞
  - feedback BACKTEST_RESULT_READY 命中 -> 落 DhBacktestResultSnapshot + 经验沉淀

边界完成度：
  - 全文不含 placeOrder / submitOrder / executeOrder / bypassRisk / forceExecute
  - paths 段不含 /orders / /trades / /live
  - dh-connector.nq 之外不引用 RestTemplate / WebClient / OkHttp
  - 任何 Batch 验收失败 -> 回该 Batch 起点，禁止跳跃
```

### 13.3 硬边界（本 SPEC 与 NQ 后续实施都不允许违反）

```text
不修改 NQ 仓库                          不修改 Java 业务代码（本 PLAN 阶段）
不修改 contracts/openapi.yaml 语义       不修改 contracts/json-schema 已落地字段
不新增 Flyway migration（B3-4 视情况新增 V4，不修改 V1/V2/V3）
不新增 OpenAPI path（NQ 端 endpoint 仅在 components 段注释占位，Stage3-B1 已声明）
不接真实 NQ API（本 PLAN）              不接真实 Kronos
不接真实 global-stock-data              不引入 TradingAgents Python
不实现真实下单                          不绕过 NQ 风控
不重写 NQ 回测核心                      不建设前端
不让 DH 执行正式回测                    不让 DH 直接下单
不让 DH 读取或写入 NQ 交易核心表        不让 NQ 强依赖 DH 才能启动或运行
不让 DH 把 NQ verdict 当作 JudgeDecision 终态
不在 DhBacktestRequest 携带凭证 / token / API key
默认 profile 不创建真实 NQ client 实例（FakeNqBacktestClient 兜底）
```

---

## 十四、与 Stage3 其他文档的衔接

```text
Stage3 主索引           docs/current/STAGE3_PLAN.md
NQ -> DH 出站事件链路   docs/current/STAGE3_NQ_TO_DH_FEEDBACK_PLAN.md       (8 事件 + ingest + 幂等)
NQ outbox 规格         docs/current/STAGE3_NQ_OUTBOX_SPEC.md                (NQ 端 outbox 表 / 触发点 / retry)
DH -> NQ 入站请求链路   docs/current/STAGE3_DH_TO_NQ_BACKTEST_PLAN.md       (高层链路)
DH backtest adapter SPEC（本文件）
                       docs/current/STAGE3_DH_BACKTEST_ADAPTER_SPEC.md      (DH 端 adapter 规格)
端到端契约规则          docs/current/STAGE3_CONTRACT_PLAN.md                 (envelope / 5 字段 / errorCode / version)
测试策略                docs/current/STAGE3_TEST_PLAN.md                     (单测 / 联调 / 幂等 / 重试 / 边界)
4 批 IMPLEMENT 工单     docs/current/STAGE3_WORK_ORDER.md                    (B1 / B2 / B3 / B4 工单)
Batch 边界对照表        docs/current/STAGE3_BATCH_PLAN.md                    (执行顺序 + 依赖)
```

本 SPEC 是 Stage3-B3 IMPLEMENT 阶段的事实源；DH 仓库后续 B3-1 ~ B3-5 实施时
若发现 SPEC 与已落地契约 / endpoint 不一致，应回到
docs/current/STAGE3_CONTRACT_PLAN.md / contracts/* 确认；必要时回 Stage3-B3 PLAN 修订本 SPEC。
