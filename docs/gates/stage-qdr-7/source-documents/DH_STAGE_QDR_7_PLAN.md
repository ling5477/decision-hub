# DH Stage-QDR-7 Limited Dry Run Runtime Readiness Plan

## 1. 文档状态

```text
task: DH-STAGE-QDR-7-PLAN
task type: PLANNING_ONLY
stage: Stage-QDR-7 / Limited Dry Run Runtime Readiness
plan status: DONE / PLAN_ONLY
stage status: PLANNING / IMPLEMENTATION_NOT_STARTED
recommended mainline: A / RUNTIME_SAFETY_AND_GUARD_BASELINE
ALLOW_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER: YES
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO
ALLOW_RUNTIME_CONTRACT_IMPLEMENTATION_NOW: NO
ALLOW_RATE_LIMIT_IMPLEMENTATION_NOW: NO
ALLOW_IDEMPOTENCY_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next task: DH-STAGE-QDR-7-IMPLEMENTATION-WORK-ORDER
```

本计划只冻结 Stage-QDR-7 的目标、批次顺序、安全门禁、review 触发条件、测试矩阵、回滚与阶段关闭纪律。它不授权修改代码、测试、migration、API、Controller、Repository/JDBC、runtime wiring、NQ、Provider、Agent、LangGraph 或 LIVE。

## 2. 前置事实

```text
Stage-QDR-6: CLOSED / ACCEPTED / ARCHIVED / TAGGED
Stage-QDR-6 tag: dh-stage-qdr-6-close
Stage-QDR-6 tag target: b9b68b3c4ea35813959ac5bf5a4566e5393e20be
Stage-QDR-6 post-tag cleanup: DONE
Stage-QDR-6 current process sources: PRUNED
Stage-QDR-7 before this task: NOT_STARTED / PLANNING_FIRST_ONLY
```

Stage-QDR-6 已完成 archive、annotated tag、remote verification 与 current pruning，因此允许独立进入 Stage-QDR-7 planning。本轮完成后 Stage-QDR-7 只推进到 `PLANNING / IMPLEMENTATION_NOT_STARTED`，不推进到 implementation 或 runtime enabled。

## 3. Stage 核心目标

Stage-QDR-7 的主线是：为仓库内既有、默认关闭的 DH-only limited dry-run inbound entry 建立可进入后续实现工单的正式 runtime readiness 基线，并关闭多实例、幂等、操作安全与准入证据缺口。

目标不是创建新的 endpoint，也不是连接 NQ 或 Provider。目标是证明并加固以下性质：

1. 入口默认关闭，错误配置和依赖故障均 fail-closed。
2. emergency kill switch 能在明确时限内阻断新请求，且不能被普通 feature flag 绕过。
3. tenant/source/correlation/authentication 边界精确且可审计。
4. rate limit、idempotency 与 replay protection 在多实例部署下保持一致语义。
5. payload、memory/context、并发、deadline 与重试均有硬上限。
6. audit、trace、error taxonomy 和日志只保存脱敏材料。
7. 成功结果仍是结构化、只读、不可执行的 decision/risk evidence，不产生 NQ、交易、Paper 或 LIVE side effect。

## 4. 当前代码现实与缺口

### 4.1 已有基线

| 能力 | 当前代码现实 | Stage-QDR-7 处理方式 |
| --- | --- | --- |
| dry-run entry | 已存在 `POST /api/ai/decision-dry-runs` 与 `DecisionDryRunController` | 不新增 endpoint；B1 冻结既有合同，B4 做准入复核 |
| feature flag | `decisionhub.integration1.runtime.enabled` 默认 `false` | 冻结 default-disabled 与 profile policy |
| production gate | `production-enabled` 独立 gate，默认 `false` | 继续禁止 production enable；任何变化单独 security review |
| kill switch | `kill-switch-enabled` 为 true 时 `runtimeEnabled()` 拒绝 | 冻结优先级；补动态生效、传播延迟和故障语义设计 |
| tenant/source allowlist | runtime properties 与 HMAC authenticator 均校验 source 和 tenant/source pair | 冻结双层校验及 cross-tenant rejection |
| HMAC/timestamp/nonce | canonical material、UTC `Z`、clock window、nonce/requestId binding 已存在 | 保持合同；补 multi-instance、clock drift 与 failure taxonomy 验收 |
| replay guard | `JdbcNonceReplayGuard` 使用原子 insert、冲突拒绝、存储故障 fail-closed | 保留 domain-separated replay key；复核共享表语义和并发证据 |
| payload cap | raw body 在反序列化前执行上限检查，默认 64 KiB | 冻结硬上限；非法配置必须启动失败，不得静默放大 |
| memory cap | `decisionContext.approxBytes` 默认 32 KiB | 扩展为 context/evidence/collection/concurrency 综合 cap |
| rate limit | 仅有 bounded `InMemoryRateLimiter` | 不满足多实例一致性；B2 规划 persistent/distributed limiter |
| idempotency | 通用 `IdempotencyFilter` + `InMemoryIdempotencyStore`，只记录 key | 不满足多实例和结果复用；B2 冻结状态机与 duplicate response 语义 |
| audit/trace | dry-run success/rejection 均写 DH audit；audit 写失败 fail-closed | 冻结事件族、必填 identity、redaction 与 failure audit |
| external dependency | dry-run path 使用既有 mock-only orchestrator/gateway；无 outbound HTTP、NQ client、real Provider | 保持 architecture guard，禁止把 future resilience 设计变成外部连接 |
| no-side-effect | 输出只允许 bias/observe/no-trade；禁止执行字段和交易动作 | 继续以 architecture + integration tests 验证 |

### 4.2 已识别 blocker

进入 protected runtime acceptance 前必须关闭：

1. **Persistent multi-instance rate limit 缺失**：当前 limiter 是 JVM-local fixed window，实例间不共享计数。
2. **正式 idempotency 语义缺失**：当前 store 是进程内 key-only `tryPut`，没有 `IN_PROGRESS / SUCCEEDED / FAILED_RETRYABLE / FAILED_FINAL` 状态、request hash、response replay 或并发 owner 语义。
3. **Duplicate handling 不完整**：通用 filter 只返回 generic conflict，不能证明相同 request 返回同一脱敏结果，也不能区分同 key 不同 payload。
4. **Emergency kill 操作合同不完整**：已有静态配置 gate，但动态刷新、传播上限、依赖失效、重启和多实例一致性未形成验收合同。
5. **Operational resilience 合同缺失**：未冻结 end-to-end deadline、并发上限、backpressure、retry eligibility、circuit state 与超时后的写入终止规则。
6. **配置非法值的 fail-closed 规则需收紧**：payload/memory cap 等不得在非法配置时静默回落到默认值。
7. **日志脱敏证据需统一**：request/audit/exception 路径必须证明不记录 HMAC、nonce、raw body、raw prompt、raw provider response、credential 或可执行交易材料。
8. **既有 Controller 的正式 Stage-QDR-7 准入证据尚未形成**：B4 必须在不新增 API 的前提下完成 API/security/architecture acceptance；如发现需改 Controller，先独立 review。

## 5. 候选方向与推荐顺序

### 5.1 A. Runtime Safety and Guard Baseline — 推荐主线

优先级：1。

先关闭 persistent rate limit、idempotency、replay、payload/memory cap、feature flag、kill switch 与 fail-closed runtime policy。原因是这些 guard 决定入口能否在多实例和异常环境中安全存在，优先级高于扩展 entry contract 或任何跨仓联调。

### 5.2 B. Protected Dry-Run Entry Contract — 第二顺位

优先级：2。

仓库已有 DH-only endpoint，因此本方向不是新增 API，而是冻结和复核现有 envelope、authentication/source policy、canonical error taxonomy、audit/trace 与 no-side-effect contract。任何 path、Controller、OpenAPI 或 wire schema 变化必须另起 API/security review；Stage-QDR-7 普通 implementation batch 不得直接修改。

### 5.3 C. NQ-DH Runtime Contract Alignment — 独立跨仓阶段

优先级：Stage-QDR-7 内不执行。

只记录 blocker 和 future alignment checklist。不得修改 NQ，不得把 NQ client、真实 HTTP 或 cross-repo runtime 混入 DH 普通 batch。未来必须独立任务、独立 worktree、独立 security/contract review。

### 5.4 D. Provider Dry-Run Readiness — 继续后置

```text
NO Provider SDK
NO credential
NO outbound HTTP
NO real model call
```

Stage-QDR-7 只允许 mock-only provider status/failure taxonomy 回归，不得建立真实 Provider capability。

### 5.5 E. Agent Runtime Contract — Stage-QDR-8 候选

继续后置。Stage-QDR-7 不引入 Python、LangGraph、Agent 或 MCP runtime，不把 limited dry-run readiness 写成 Agent runtime readiness。

## 6. 批次计划

### B1：Runtime Contract and Safety Policy

目标：冻结既有入口的正式 runtime contract 与安全策略，不修改 API、migration 或代码。

必须冻结：

- request/response envelope、schema version 与 request hash domain；
- `tenantId / source / requestId / traceId / timestamp / nonce` authority 与 body/header binding；
- `dryRun=true`、read-only/no-side-effect、mandatory forbidden capabilities；
- canonical error taxonomy、HTTP status mapping 与 retryability classification；
- feature flag、production gate、kill switch 的优先级和 fail-closed truth table；
- payload/context/collection/concurrency/deadline budget；
- rate-limit key、idempotency key、replay key 的独立 domain separation；
- audit/trace/redaction minimum fields 与禁止保存材料；
- existing endpoint 是 current code reality，但本批不新增、不修改 Controller/OpenAPI。

交付：B1 contract/policy freeze record、error matrix、gate truth table、review decision。B1 若发现 API/security contract 必须改变，应阻断并转独立 review，不得顺手实现。

### B2：Persistent Runtime Guards

目标：在独立 implementation work order 后实现并验证多实例 guard；本 planning task 不授权实现。

规划内容：

- persistent/distributed rate-limit port，key 至少绑定 tenant/source/route/policy version；
- 原子计数、窗口/令牌算法、clock source、capacity exhaustion fail-closed；
- idempotency 状态机、request hash、owner/lease、TTL、completed response replay；
- 相同 key + 相同 hash 返回同一脱敏结果；相同 key + 不同 hash 固定拒绝；
- nonce replay 与 idempotency 的顺序、失败映射和独立 key space；
- payload、context、evidence refs、collection count、并发与 memory cap；
- 存储不可用、超时、冲突、partial write 与 cleanup failure 全部 fail-closed。

Review gate：如需 migration、production Repository/JDBC 或新基础设施，必须先分别完成 migration/schema review 与 Repository/JDBC review。不得复用 V1–V11 修改历史 migration；只能在获批后新增 forward-only migration。

### B3：Operational Resilience

目标：建立受限入口的 deadline、backpressure、failure isolation 与日志安全基线，不接真实 Provider 或 HTTP。

规划内容：

- end-to-end deadline 与各内部 step budget；deadline 到期后不得继续写成功状态；
- retry 只允许显式标记为 safe、bounded、idempotent 的内部瞬时失败；认证、policy、rate limit、replay、validation 与 audit failure 不重试；
- retry 必须有 max attempts、总时间预算、jitter 和 amplification guard；
- circuit breaker 只用于已批准的内部依赖边界，默认打开保护/失败关闭，不得借此创建 outbound dependency；
- concurrency cap、bounded queue、backpressure 与 overload 固定拒绝；
- environment/profile 隔离；production 继续 disabled；
- emergency kill 的动态生效来源、最大传播时间、失效策略和多实例一致性；
- 日志、metrics、audit、trace 的 redaction policy 与低基数标签；
- failure taxonomy 覆盖 timeout、overload、dependency unavailable、unknown error 和 audit failure。

### B4：Protected Runtime Entry Readiness

目标：对既有 protected entry 做准入验收，不直接实现或扩展 Controller、Client 或真实 HTTP。

准入条件：

1. B1 contract/policy freeze 通过。
2. B2 persistent guards 通过 PostgreSQL/Testcontainers、多实例并发与故障注入。
3. B3 deadline/backpressure/kill/redaction/failure audit 通过。
4. endpoint 在所有 profile 默认关闭；非 dev/test 无明确批准不得启用。
5. read-only/no-side-effect architecture guards 通过。
6. 没有 NQ client、outbound HTTP、Provider SDK、Agent/LangGraph/Python runtime 或交易依赖。
7. API/security review 明确接受既有 path/envelope；如需任何 API/Controller 变化，B4 必须 BLOCKED 并转独立 review。
8. NQ contract alignment 只输出 blocker 清单，不启动跨仓 runtime。

### B5：Stage-QDR-7 Final Close

目标：统一汇总 B1–B4 acceptance、tests、security boundary、known risks 和 no-side-effect evidence。

B5 不是把 final review、archive、tag、cleanup 合并在一次提交。关闭顺序必须保持：

```text
final close review PASS
-> close docs commit
-> self-contained archive packet + archive commit
-> clean worktree
-> annotated tag close + remote verification
-> independent post-tag current cleanup
```

每一步必须由独立任务显式授权。B5 不得预先创建 archive、tag 或 Stage-QDR-8 plan。

## 7. Security gates

以下 gate 缺一不可：

```text
DEFAULT_DISABLED: PASS
PRODUCTION_DISABLED: PASS
KILL_SWITCH_PRECEDENCE: PASS
TENANT_SOURCE_ALLOWLIST: PASS
CROSS_TENANT_REJECTION: PASS
HMAC_TIMESTAMP_NONCE: PASS
RATE_LIMIT_MULTI_INSTANCE: PASS
IDEMPOTENCY_MULTI_INSTANCE: PASS
REPLAY_DOMAIN_SEPARATION: PASS
PAYLOAD_MEMORY_CONCURRENCY_CAPS: PASS
TIMEOUT_RETRY_BACKPRESSURE: PASS
AUDIT_TRACE_REDACTION: PASS
NO_SIDE_EFFECT: PASS
NO_EXTERNAL_IO: PASS
NO_NQ_RUNTIME: PASS
NO_PROVIDER_AGENT_LIVE: PASS
```

任何 gate 不能证明时必须 fail-closed，B4 不得给出 entry readiness acceptance。

## 8. Review 触发条件

仅以下变化需要 standalone review：

- migration/schema；
- API/Controller/OpenAPI/wire envelope；
- production Repository/JDBC 或 distributed guard backend；
- authentication、tenant、source、HMAC、timestamp、nonce 边界；
- rate limit、idempotency、duplicate handling、replay semantics；
- real HTTP、cross-repo runtime 或 NQ client；
- security boundary；
- P0/P1 blocker；
- stage final close。

普通实现批次只执行 implementation、tests、boundary scan、最小 docs sync 和独立 commit，不为每个普通子项创建额外 freeze/review 链。

## 9. 测试矩阵

### 9.1 Identity 与 authentication

- source allowlist allow/deny；
- tenant/source pair allow/deny；
- authenticated tenant、header tenant、body tenant exact binding；
- cross-tenant rejection；
- signature missing/invalid/body tamper；
- timestamp invalid、非 UTC `Z`、future/past out-of-window；
- nonce missing/replay；
- requestId/traceId/header/body mismatch。

### 9.2 Persistent guards

- duplicate request：首次成功、并发重复、完成后 replay；
- same key/same hash 返回同一脱敏结果；
- same key/different hash 固定拒绝；
- owner crash、lease expiry、partial result、cleanup race；
- multi-instance rate limit 原子性、tenant/source 隔离、窗口边界；
- rate-limit/idempotency/replay store unavailable、timeout、capacity exhausted；
- replay key 与 idempotency key/domain 不冲突。

### 9.3 Resource 与 operational safety

- raw payload size cap 在 JSON parse 前拒绝；
- context bytes、evidence ref count、collection depth/width cap；
- memory/context cap；
- concurrency cap、queue full、backpressure；
- feature flag default disabled；
- production gate disabled；
- kill switch 生效与传播上限；
- end-to-end timeout；
- retry allowed/forbidden matrix、attempt/time budget；
- circuit open/half-open/close 与 dependency failure；
- unknown error 固定 fail-closed。

### 9.4 Audit、redaction 与 no-side-effect

- success/rejection/timeout/duplicate/overload/kill-switch audit 完整性；
- requestId、traceId、tenantId、source、policy version 可关联；
- signature、secret、nonce、raw body、raw prompt、raw provider response、credential 不进入日志/audit；
- error response 不回显敏感材料；
- 不产生 order/risk/ledger/account/Paper/LIVE mutation；
- 不存在 Provider SDK、outbound HTTP、NQ client、Agent/LangGraph/Python dependency；
- `LONG_BIAS / SHORT_BIAS` 不映射为 `BUY / SELL`。

### 9.5 Regression 与环境

- Stage-QDR-6 deterministic replay/canonical snapshot/hash regression；
- existing dry-run controller/service/HMAC tests；
- architecture guards；
- PostgreSQL/Testcontainers multi-instance/persistence tests；
- clean V1–current migration 与 historical upgrade path；
- full Maven tests、quality、Checkstyle、Spotless；
- skipped tests 必须显式分类，不能把未运行项写成 PASS。

## 10. Rollback 与 no-side-effect 策略

Runtime rollback 优先级：

1. emergency kill switch 拒绝所有新请求；
2. feature flag 关闭；
3. 回退到 default-disabled profile/config；
4. 停止 consumer/entry traffic，不触碰 NQ 或交易状态；
5. 保留 audit/trace 与 idempotency/replay records 到 TTL/retention 到期；
6. 使用普通代码 revert 或 forward-fix；不得修改历史 migration、不得清空安全表、不得移动既有 stage tag。

Rollback 本身不得产生订单、风控、ledger、account、Paper 或 LIVE mutation。若 guard storage 不可用，入口必须保持拒绝，不能为了可用性 fail-open。

## 11. Stage close acceptance

Stage-QDR-7 只有在以下条件全部通过后才能 final close：

- B1–B4 全部完成且需要的 standalone review 已关闭；
- persistent multi-instance rate limit/idempotency/replay 有真实 PostgreSQL/Testcontainers evidence；
- default-disabled、kill switch、deadline/backpressure、audit/redaction 与 no-side-effect evidence 完整；
- full tests、architecture guards、quality、Checkstyle、Spotless 通过且 skipped 分类清楚；
- migration/API/Repository/JDBC 变更均有独立 acceptance；
- forbidden dependency/scope scan 为空；
- current factsources 一致且 worktree/staged 状态符合 close task 要求；
- final close review 明确 PASS。

## 12. 固定边界

```text
no real Provider
no Provider SDK
no credential
no real outbound HTTP
no NQ runtime integration
no order/risk/ledger/account mutation
no Paper/Live Run trigger
no Agent/LangGraph/Python runtime
no BUY/SELL/MARKET_ORDER mapping
all failures fail-closed
```

DH 输出仍只能是结构化、只读、不可执行的 decision 或 risk evidence。Existing limited inbound endpoint 不等于 NQ runtime integration，不等于 real HTTP outbound，不等于 Provider readiness，也不构成 Paper/LIVE 授权。

## 13. 下一步

```text
DH-STAGE-QDR-7-IMPLEMENTATION-WORK-ORDER
```

下一任务只能把本计划拆成可审查的 implementation work order，并继续保持：

```text
ALLOW_STAGE_QDR_7_IMPLEMENTATION_NOW: NO
ALLOW_RUNTIME_CONTRACT_IMPLEMENTATION_NOW: NO
ALLOW_RATE_LIMIT_IMPLEMENTATION_NOW: NO
ALLOW_IDEMPOTENCY_IMPLEMENTATION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
```
