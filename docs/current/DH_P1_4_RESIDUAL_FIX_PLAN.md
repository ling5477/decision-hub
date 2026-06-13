# DH P1-4 Residual Fix Plan

> 任务：DH-P1-4-RESIDUAL-FIX-PLAN
> 类型：DOCUMENTATION + FIX_PLAN
> 日期：2026-06-12
> 仓库：Decision Hub（DH）
> 配套：`DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md` / `DH_NQ_INTEGRATION0_SECURITY_POLICY.md` / `NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md`

本文件**只做修复方案设计，不改代码**：不实现限流、不实现 memory cap、不实现 replay nonce persistence、不启动 Integration-1、不做真实 NQ 联调。

---

## 1. 当前状态（只读核查结论）

### 1.1 涉及 feedback / replay / nonce / in-memory / payload 的位置

| 关注点 | 位置 | 现状 |
| --- | --- | --- |
| feedback 入口 | `dh-api/.../api/feedback/NqFeedbackController.java`（`POST /api/ai/feedback/nq`） | 调用 `feedbackAuthenticator.authenticate()` 后再 `ingestionService.ingest()`；**无限流** |
| 来源认证 / 防重放 / payload gate | `dh-security/.../security/nq/HmacNqFeedbackAuthenticator.java` | payload gate 413、source allowlist 403、timestamp 401、nonce/requestId 401、signature 401、replay 409；**无 rate limit** |
| 认证结果 | `dh-security/.../security/nq/NqFeedbackAuthResult.java` | 状态码 202/4xx；**无 429 / RATE_LIMITED** |
| 防重放端口 | `dh-security/.../security/nq/NonceReplayGuard.java` | `markIfAbsent(replayKey, expiresAt)`；接口已含 `expiresAt`（TTL 意图）但实现未用 |
| 防重放实现 | `dh-security/.../security/nq/InMemoryNonceReplayGuard.java` | `ConcurrentHashMap<String,Instant>` **无界、无清理、仅单实例**（注释已声明生产需替换为共享存储） |
| 安全装配 | `dh-app/.../config/SecurityWiringConfig.java` | `@Value("${decisionhub.security.nq-feedback.*}")` 注入 secret/allowed-sources/max-payload-bytes/max-clock-skew；**硬编码 `new InMemoryNonceReplayGuard()`** |
| feedback 内存仓储 | `dh-usecase/.../agent/inmemory/InMemoryNqFeedbackEventRepository.java` | `indexByRun` / `envelopesByEventId` 均为**无界** ConcurrentHashMap/ArrayList |
| 其它内存仓储 | `dh-usecase/.../agent/inmemory/*`（research run/agent task/candidate/judge/reflection/checkpoint/backtest request）、`dh-memory/.../inmemory/*`（5 Store）、`dh-infra/.../impl/InMemoryIdempotencyStore.java` / `InMemoryUsageMeter.java` / `InMemoryTaskQueue.java` | 均为**无界**内存结构 |
| 配置模板 | `dh-app/src/main/resources/application.yml` / `application-dev.yml` / `application-prod.yml` | 现有 nq-feedback 安全配置；**无 rate limit / memory cap 配置项** |

### 1.2 P1-4 三项缺口判定

```text
rate limit            已实现（DH-P1-4-...-BATCH-3，2026-06-13）：dh-security RateLimiter 端口 +
                      RateLimitResult + bounded fail-closed InMemoryRateLimiter；NqFeedbackController
                      前置限流（key=source+tenant+route，超限 429 RATE_LIMITED）；SecurityWiringConfig
                      注入保守默认；in-memory 仅 dev/test/单实例，真实多实例集中式 limiter 另起任务
memory cap            已实现（DH-P1-4-...-BATCH-2，2026-06-12）：InMemoryNonceReplayGuard /
                      InMemoryNqFeedbackEventRepository 改为有界（上限 + TTL + fail-closed / 驱逐最老）；
                      Batch 3 加固：feedback-store per-tenant 默认 10000 -> 1000（< 全局上限，仅默认值）
replay nonce          已实现（DH-P1-4-...-BATCH-1，2026-06-12）：JdbcNonceReplayGuard + V4 持久化
```

> 状态（2026-06-13）：P1-4 三项残留实现（replay nonce / memory cap / rate limit）均已落地。但 **P1-4 未标记全部关闭**：
> 须先 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-3-REVIEW，再 DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE 单独验收后方可关闭。
> Integration-1 仍 NOT STARTED；header `X-DH-NQ-*` -> canonical `X-NQ-DH-*` 对齐仍未做（另起独立任务）。

> 当前 header 为 `X-DH-NQ-*`；Integration-0 冻结 canonical 为 `X-NQ-DH-*`。header 对齐是 Integration-1 前置（见 acceptance report），不在本 P1-4 方案内修复，但 nonce/replay 实现切换时应同步评估。

---

## 2. Proposed fix — Rate limit

### 2.1 作用点（layer）

- 优先在 **dh-api 层**新增一个 `NqFeedbackRateLimitFilter` 或在 `NqFeedbackController` 前置一个 `RateLimiter` 端口调用，**位于认证之前或紧随租户解析之后、authenticator 之前**，使超限请求在进入 HMAC/ingestion 前被拒，降低被刷成本。
- `RateLimiter` 抽象端口放 `dh-security`（与认证同层），实现可插拔（in-memory token bucket 仅 dev/test；Integration-1 real channel 用集中式）。

### 2.2 限流 key

```text
主键：source + tenant + route        （route = "/api/ai/feedback/nq"）
次级：source + tenant + requestId     （仅用于幂等重复提交识别，不作为限流主键）
```

- 以 `source + tenant + route` 为限流桶，保证**租户/来源隔离**，避免单租户拖垮其它租户。

### 2.3 窗口与默认阈值（配置化，保守默认）

```text
decisionhub.security.nq-feedback.rate-limit.enabled            默认 true
decisionhub.security.nq-feedback.rate-limit.window-seconds     默认 1
decisionhub.security.nq-feedback.rate-limit.max-requests       默认 N（保守值，配置注入，文档不写死生产值）
decisionhub.security.nq-feedback.rate-limit.burst              默认 0（可选 token bucket 突发）
```

- 算法建议：固定窗口或 token bucket（实现阶段二选一，dev/test 用内存实现）。

### 2.4 超限返回语义

```text
HTTP 429 TOO_MANY_REQUESTS
errorCode: RATE_LIMITED
不在响应体泄露内部阈值、窗口大小或当前计数（只返回类型 + traceId）
可选 Retry-After 头（粗粒度，不暴露精确剩余）
```

- 需在 `NqFeedbackAuthResult`（或新增 `RateLimitResult`）补 `429 / RATE_LIMITED`，controller 映射到 429。

### 2.5 审计事件

```text
RATE_LIMITED（含 source / tenant / route / traceId / requestId；不含阈值与计数明细）
```

### 2.6 测试方案（设计，不实现）

```text
rate_limit_returns_429：超过窗口阈值返回 429 + RATE_LIMITED
rate_limit_tenant_source_isolated：A 租户超限不影响 B 租户/其它 source
rate_limit_does_not_break_mock_contract_test：默认 test profile 阈值足够高或关闭，不影响 INT0-T01..T15
rate_limit_audit_event_emitted：429 必产出 RATE_LIMITED 审计，且不含阈值明细
```

---

## 3. Proposed fix — Memory cap

### 3.1 需要上限的 in-memory store

```text
高优先（安全相关，Integration-1 必修）：
  InMemoryNonceReplayGuard（replay cache）—— 见 §4，优先用持久化/集中式替换；保留的内存兜底也必须有界
中优先（入站数据放大面）：
  InMemoryNqFeedbackEventRepository（indexByRun / envelopesByEventId）
  dh-infra InMemoryIdempotencyStore / InMemoryUsageMeter / InMemoryTaskQueue
低优先（agent runtime 内存，按容量评估）：
  dh-usecase agent inmemory 仓储、dh-memory 5 Store
```

### 3.2 上限维度

```text
单 tenant 上限：每 tenant 最大条目数（防单租户撑爆）
全局上限：进程级最大条目数（兜底）
TTL：replay cache 必须有 TTL（= 2 × maxClockSkew，与现有 replayExpiresAt 设计一致）；
      feedback/idempotency store 按业务保留窗口设 TTL 或落 JDBC
```

### 3.3 eviction 策略

```text
replay cache：TTL 过期清理 + 容量上限 LRU/FIFO 兜底（过期优先于容量驱逐）
其它 store：size-bounded（LRU）或迁移到 JDBC（dh-infra 已有 Jdbc* 仓储样板）
驱逐不得影响仍在有效窗口内的合法请求（TTL 未到不得提前驱逐 replay key）
```

### 3.4 超限返回语义

```text
replay cache 容量达上限且无法安全驱逐：保守 fail-closed -> 503 或 429（视实现），不得静默丢弃 replay 保护
入站 store 超限：返回受控错误（如 429 / 503），不得 OOM
不得在响应体泄露内部容量
```

### 3.5 审计事件

```text
MEMORY_CAP_EVICTION（驱逐计数，聚合，不逐条刷屏）
MEMORY_CAP_REJECTED（达到硬上限拒绝）
```

### 3.6 测试方案（设计，不实现）

```text
memory_cap_rejects_overflow：超过上限的新条目被受控拒绝，不 OOM
memory_cap_eviction_does_not_break_valid_requests：TTL 未到的 replay key / 有效条目不被误删
bounded_store_respects_tenant_and_global_limit：单 tenant 与全局上限分别生效
replay_cache_ttl_eviction：过期 key 被清理，过期后同 key 可视为新（与防重放窗口一致）
```

---

## 4. Proposed fix — Replay nonce persistence

> **实现状态（2026-06-12，DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1）：已实现候选 A。**
> 落地 = `JdbcNonceReplayGuard`（dh-infra）+ Flyway `V4__nq_feedback_replay_nonce.sql`（表 `dh_nq_replay_nonce`）+ `SecurityWiringConfig` 条件装配 + `NonceReplayGuardType` 选择策略（dev/test 允许 in-memory，非 dev/test 默认 jdbc，fail-closed）。
> 表口径采用本节 §4.2 候选 A 的窄表 `dh_nq_replay_nonce(replay_key PK, expires_at, created_at)`：因端口 `markIfAbsent(replayKey, expiresAt)` 只提供 replayKey 与 expiresAt（无独立 tenant），故不拆分 source/tenant/request/nonce 列，避免不可靠解析与存储非必要字段。
> 详见 `WORKLOG.md` / `TESTING.md §23`。memory cap 已于 DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2（2026-06-12）实现（§3，`TESTING.md §25`）；rate limit 仍未实现。

### 4.1 nonce key 组成（沿用现有口径）

```text
replayKey = source + "::" + nonce + "::" + requestId   （HmacNqFeedbackAuthenticator 现有格式）
TTL = 2 × maxClockSkew（现有 replayExpiresAt 已如此计算，但 InMemory 实现未消费）
```

### 4.2 持久化 / 集中式方案候选

```text
候选 A（推荐，复用现有基建）：PostgreSQL-backed NonceReplayGuard
  - dh-infra 新增 JdbcNonceReplayGuard 实现 NonceReplayGuard
  - 表：dh_nq_replay_nonce(replay_key PK, expires_at, created_at)，对 expires_at 建索引
  - markIfAbsent = INSERT ... ON CONFLICT DO NOTHING + 判断影响行数；过期清理用定时任务或惰性 WHERE expires_at < now()
  - 迁移用新增 Flyway V{n}（实现阶段，不在本轮）
候选 B：Redis-backed NonceReplayGuard
  - SET replayKey value NX EX <ttl>；NX 失败即重放
  - 需引入 Redis 依赖与运维；TTL 由 Redis 原生过期
候选 C：bounded in-memory（仅 dev/test）
  - 现有 InMemoryNonceReplayGuard 加 TTL + 容量上限，仅允许 dev/test/mock，禁止 Integration-1 real channel
```

### 4.3 单实例 vs 多实例

```text
单实例（dev/test）：bounded in-memory 可用（候选 C）
多实例（Integration-1 real channel）：必须候选 A 或 B（集中式），否则各实例各自内存，重放可绕过
装配通过 profile / @ConditionalOnProperty 切换；real channel 默认集中式，缺配置 fail-closed
```

### 4.4 crash recovery 语义

```text
候选 A：DB 持久化，重启后 replay 记录仍在，窗口内重放仍被拒（强）
候选 B：Redis 持久化（取决于持久化配置）；纯内存模式重启丢失（需说明）
候选 C：重启丢失（仅 dev/test 可接受）
```

### 4.5 replay rejection 语义

```text
重放命中 -> 409 REPLAY_DETECTED（与现有一致）
存储不可用（DB/Redis down）-> fail-closed：拒绝（如 503），不得降级为“放行”
```

### 4.6 审计事件

```text
REPLAY_REJECTED（含 source / tenant / requestId / traceId；不含 nonce 明文是否必要由实现评估，倾向只记 hash 或省略）
REPLAY_STORE_UNAVAILABLE（存储不可用 fail-closed 时）
```

### 4.7 测试方案（设计，不实现）

```text
persistent_nonce_rejects_replay_after_restart_simulation：模拟重启（重建 guard 但复用持久化）后同 key 仍被拒
persistent_nonce_source_tenant_request_scoped：不同 source/tenant/requestId 的同 nonce 不互相误判
replay_store_unavailable_fails_closed：存储不可用时拒绝而非放行
in_memory_guard_only_dev_test：bounded in-memory guard 不被 real-channel profile 装配
```

---

## 5. 哪些可在 Integration-1 前完成 / 哪些仍只允许 fake / mock / paper-only

```text
可在 Integration-1 前完成（实现 + 测试，仍不接真实 NQ / 不真实 HTTP）：
  - rate limit 端口 + 内存实现 + 429/RATE_LIMITED + 审计 + 单元/contract 测试
  - memory cap：bounded store + TTL + eviction + 审计 + 测试
  - replay nonce 持久化：JdbcNonceReplayGuard（候选 A）实现 + Flyway 迁移 + 测试（用内存/容器 DB）
  - 这些都可在 DH 仓库内、用 fake/mock/容器 DB 验证，不需要真实 NQ

仍只允许 fake / mock / paper-only（不在 P1-4 范围，属 Integration-1）：
  - 真实 NQ 入站联调
  - 真实 HTTP / RealClient / 真实 Provider
  - header X-DH-NQ-* 与 X-NQ-DH-* 真实对齐联调
  - staging / paper-only 端到端 T1-T7
```

---

## 6. Files allowed for future implementation（修复阶段允许修改，本轮不改）

```text
dh-security/src/main/java/.../security/nq/**         （NonceReplayGuard 端口、RateLimiter 端口、AuthResult 扩展 429）
dh-security/src/test/java/**                          （安全单元/契约测试）
dh-infra/src/main/java/.../infra/**                   （JdbcNonceReplayGuard、bounded store 实现）
dh-infra/src/main/resources/db/migration/**           （新增 Flyway 迁移，不改历史）
dh-infra/src/test/java/**
dh-api/src/main/java/.../api/feedback/**              （rate limit filter / controller 接线 + 429 映射）
dh-api/src/test/java/**
dh-app/src/main/java/.../config/SecurityWiringConfig.java  （装配可插拔 guard / rate limiter）
dh-app/src/main/resources/application*.yml             （新增 rate-limit / memory-cap / replay 配置项，保守默认）
dh-app/src/test/java/**
dh-usecase/src/main/java/.../agent/inmemory/**         （bounded 变体，仅在确有放大风险处）
dh-usecase/src/test/java/**
dh-domain/src/test/java/.../integration0/**            （新增 P1-4 相关 contract test；INT0-T01..T15 回归）
dh-domain/src/test/resources/integration0/**
docs/current/**                                        （记录实现与验收）
```

## 7. Files forbidden for future implementation（仍禁止）

```text
NQ 仓库任何文件
dh-domain/src/main/**（契约/领域模型不为限流/容量改动，除非新增 ErrorCode 且经评审）
任何 RealClient / RealNqBacktestClient
任何真实 Provider 接入
真实 HTTP / 真实 NQ endpoint / 真实交易所 client
下单 / 撤单 / Paper Run 启停 / 策略状态 / 风控状态相关代码
凭证读取代码 / .env / 密钥文件
LIVE 开关
contracts/openapi.yaml 的危险路径 / 危险关键词（保持 ArchUnit 禁字）
```

## 8. Tests to add（修复阶段，本轮只列清单）

```text
rate_limit_returns_429
rate_limit_is_tenant_source_isolated
memory_cap_rejects_overflow
memory_cap_eviction_does_not_break_valid_requests
persistent_nonce_rejects_replay_after_restart_simulation
persistent_nonce_is_source_tenant_request_scoped
replay_store_unavailable_fails_closed
payload_64kib_gate_remains_valid（回归现有 413）
existing INT0-T01..T15 still pass（NQ 16 + DH 16 回归）
no_credential_access（回归）
no_trading_side_effect（回归）
```

## 9. Validation plan（修复后必须运行，本轮不运行）

```text
DH：
  mvn test                                   全仓回归（含新增 P1-4 测试 + INT0-T01..T15）
  mvn -Pquality validate                     spotless / 质量门禁
  （如引入容器 DB 测试）确保 Testcontainers 可用或在 CI 跑，本机无 Docker 时按既有策略 skip 并说明
NQ：
  mvn -f backend/pom.xml test                Integration-0 contract test 回归（header 对齐后 fixtures 同步重跑）
通用：
  git status --short / git diff --check / git diff --stat
```

## 10. Integration-1 decision

```text
Integration-1:        NOT STARTED
Runtime integration:  NOT STARTED
DH integration:       NOT INTEGRATED
AI:                   NOT STARTED
LIVE:                 DISABLED
```

- P1-4 未修复前，**仍禁止进入 Integration-1**（真实只读通道 / 真实 HTTP / RealClient / 真实 Provider）。
- 方案产出时未修复任何缺口；**DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-1（2026-06-12）已实现 replay nonce persistence（§4）**；**DH-P1-4-RESIDUAL-FIX-IMPL-BATCH-2（2026-06-12）已实现 bounded memory cap（§3）**（InMemoryNonceReplayGuard / InMemoryNqFeedbackEventRepository 有界 + TTL + fail-closed）；rate limit（§2）仍未实现，P1-4 仍未全部关闭，仍不得进入 Integration-1。

## 11. Risks

```text
- 方案与未来实现可能漂移：必须用 §8 测试在实现阶段固化，尤其 fail-closed 行为。
- rate limit 阈值设置不当：过严影响合法 feedback，过松失去保护；阈值必须配置化 + 保守默认 + 可观测。
- 持久化 nonce 引入 DB/Redis 依赖与运维成本，存储不可用必须 fail-closed，否则反成单点放行风险。
- memory cap 驱逐若早于 TTL 会破坏防重放窗口；TTL 必须优先于容量驱逐。
- header X-DH-NQ-* 与 X-NQ-DH-* 未对齐前，real channel 仍不可联调；nonce/replay 实现切换需与 header 对齐协同。
- 本轮为只读方案设计，不代表任何缺口已修复或真实通道已安全。
```

## 12. Next concrete action

```text
DH-P1-4-RESIDUAL-FIX-REVIEW（评审本方案） 或 DH-P1-4-RESIDUAL-FIX-IMPL（按本方案分批实现 rate limit /
memory cap / replay nonce persistence + 测试），均不得直接 Integration-1、不得真实 NQ 联调、不得真实 HTTP。
建议实现顺序：replay nonce persistence（含 bounded 兜底）-> memory cap -> rate limit，最后回归 INT0-T01..T15。
```
