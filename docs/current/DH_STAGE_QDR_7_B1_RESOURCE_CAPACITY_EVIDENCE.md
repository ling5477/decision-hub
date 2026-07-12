# DH Stage-QDR-7 B1 Resource Capacity Evidence

## Capacity blocker resolution disposition（2026-07-12）

`DH-STAGE-QDR-7-B1-CAPACITY-BLOCKER-RESOLUTION` 已裁定：本文件的 `0` 个有效 2xx 样本、Tomcat/Hikari 可观测缺口、Docker/Testcontainers skip 与 persistent guard 未实现事实继续有效，但它们属于 `POST_B2_CAPACITY_ACCEPTANCE` 前置，不再循环阻止 B2 schema/security review。Pre-B2 已冻结配置类型、单位、合法范围、绝对 hard ceiling 与跨字段关系；最终运行默认值仍全部 `NOT_SELECTED`。

当前 actual wiring 同时存在 `NQ_DRYRUN` production-code normalization drift，须先完成 `DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-BLOCKER-FIX`。修复前不重跑 benchmark、不开放 B2 implementation；详情以 `DH_STAGE_QDR_7_B1_CAPACITY_BLOCKER_RESOLUTION.md` 为当前 disposition authority。本文件以下原始 benchmark evidence 保持不变。

> task: `DH-STAGE-QDR-7-B1-RESOURCE-CAPACITY-BLOCKER`
> mode: `EVIDENCE_ONLY`
> endpoint: `POST /api/ai/decision-dry-runs`
> verdict: `BLOCKED / RESOURCE_CAPACITY_EVIDENCE_INSUFFICIENT`
> implementation: `NOT_STARTED`

## 1. Evidence decision

本轮完成了本机环境核验、真实 test-profile 应用启动、隔离 PostgreSQL/Flyway、Actuator runtime 指标检查、现有 MockMvc/Testcontainers harness 审查，以及 payload/concurrency benchmark 的安全尝试。由于实际 app-wired endpoint 无法取得任何 2xx successful sample，且 Docker/Testcontainers、Tomcat worker/accept queue、future persistent rate/idempotency/cleanup 路径均无有效测量，本轮不能冻结数值预算。

```text
STAGE_QDR_7_B1_RESOURCE_CAPACITY_BLOCKER: BLOCKED
RESOURCE_CAPACITY_EVIDENCE: INSUFFICIENT
VALID_SUCCESS_SAMPLES: 0
B1_RESOURCE_CAPACITY_EVIDENCE_BLOCKED
STAGE_QDR_7_B1_RUNTIME_CONTRACT_SAFETY_POLICY: BLOCKED
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: NO
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
```

## 2. Environment

```text
OS: Microsoft Windows 11 Pro / 10.0.26200 / 64-bit
CPU: AMD Ryzen 9 9950X / 16 physical cores / 32 logical processors
physical memory: 50,524,966,912 bytes
visible memory at preflight: 49,340,788 KiB total / 20,072,628 KiB free
JDK: Oracle Java 21.0.9 LTS / HotSpot 64-bit
Maven: Apache Maven 3.9.12
standalone java estimated max heap: 11.77 GiB
test-profile app Actuator max JVM memory: 13,958,643,709 bytes
test-profile app observed used JVM memory before rejected-path run: 209,858,408 bytes
embedded server dependency: Spring Boot 3.5.10 / embedded Tomcat 10.1.50
HikariCP: 6.3.3
PostgreSQL JDBC: 42.7.9
local isolated PostgreSQL runtime: 17.7 / loopback
Docker CLI: 29.6.1
Docker daemon: UNAVAILABLE
Testcontainers: 1.20.4 / runtime suites skipped
```

临时应用只绑定 loopback `18080`，使用 `test` profile、本轮临时认证材料和唯一隔离数据库。未读取现有业务数据库。测试结束后应用已停止，临时数据库已删除。

## 3. Runtime configuration evidence

### 3.1 Embedded server / threads

- 依赖树确认 embedded Tomcat 10.1.50。
- 仓库没有显式 `server.tomcat.threads.*`、`max-connections` 或 `accept-count` 配置。
- Actuator 未暴露 Tomcat worker thread、connection backlog 或 accept queue metrics，因此不得假设框架默认值。
- Actuator 暴露的 `applicationTaskExecutor` 为 core `8`、max `2147483647`、queue remaining `2147483647`；该 executor 不是已证明的 endpoint worker queue，也不能作为 bounded queue 合同。

结论：server thread/queue capacity 不可证。

### 3.2 JVM / heap / GC

- 本机默认 Java 进程 estimated max heap 为 11.77 GiB。
- test-profile app Actuator 报告 max JVM memory 13,958,643,709 bytes。
- rejected-path 测量前 used JVM memory 209,858,408 bytes；GC pause 为 count `1`、total/max `0.008s`。
- 没有 successful steady-state samples，因此没有可用于安全余量计算的 heap growth/GC curve。

结论：只能记录环境基线，不能冻结 heap-derived concurrency。

### 3.3 Datasource / PostgreSQL

- Hikari runtime metrics：max `10`、min `10`、idle `10`、active `0`、pending `0`（空闲基线）。
- 本机隔离 PostgreSQL 为 17.7；Flyway V1–V11 可完成启动迁移。
- 当前代码未显式配置 Hikari pool；上述 `10` 是本轮实际 runtime metric，不外推到其他环境。
- Docker daemon 不可用；PostgreSQL/Testcontainers capacity path 未运行。
- 没有 successful concurrent request，因此没有 active/pending/acquire/usage saturation curve。

结论：连接池空闲事实可证，连接池容量预算不可证。

### 3.4 Endpoint execution and database operations

代码路径包含 authentication、payload/header binding、JVM-local rate admission、HMAC/timestamp/nonce、runtime/source/context policy、Decision Core request/run/signal/decision persistence、orchestrator audit、mock gateway integration、run completion与 final audit。

数据库操作次数随 provider/gateway/failure 分支变化。当前没有 per-request SQL counter/trace；本轮失败样本在 gateway 前后产生多条 core/audit记录，但不能据此推断成功请求的固定事务/SQL次数。精确 DB round-trip count 仍缺失。

### 3.5 Current rate limit / idempotency

```text
current JVM-local rate window: 1 second
current JVM-local quota: 20 requests per source+tenant+route
current JVM-local max keys: 10000
generic in-memory idempotency TTL: 10 minutes
```

Benchmark 进程曾临时提高 JVM-local limiter，仅用于避免测到 429；该 override 不是候选生产值。Persistent multi-instance rate limit/idempotency 尚未实现，因此无法测量目标 storage contention、lease、TTL、cleanup 或 retention。

## 4. Harness assessment

### 4.1 Existing MockMvc

`DecisionDryRunControllerWebMvcTest` 使用 `MockMvcBuilders.standaloneSetup` 与 in-memory repositories。它可验证 HMAC、nonce、policy、payload/context cap、rate rejection、audit fail-closed等功能，但不经过实际 Tomcat、Hikari、PostgreSQL、server queue或真实 app wiring。

结论：不能作为 endpoint capacity benchmark。

### 4.2 Actual test-profile app

应用在 loopback/test profile、隔离 PostgreSQL上成功启动，health `UP`。正式测量前 smoke 暴露两个阻断：

1. Configured `NQ_DRYRUN` tenant/source pair 经 runtime properties lowercase 后再次进入 exact-wire authenticator，uppercase wire request被 `SOURCE_DENIED`。临时 lowercase source只用于诊断，不改变正式合同。
2. Lowercase diagnostic request通过认证、nonce、policy与多次DB写入，但 app-wired mock gateway 缺少可复现 runtime seed/profile，最终返回 `UNKNOWN_ERROR / 500`。

继续手工注入未冻结 model/profile 数据会使 benchmark依赖临时业务 seed，不满足可重复 evidence 要求，因此停止扩压。

## 5. Benchmark attempt

```text
profile: test
network: localhost/loopback only
external HTTP/provider/NQ: none
payload matrix: small / typical / near-32KiB-context / near-64KiB-payload
concurrency matrix attempted: 1 / 2 / 4 / 8 / 16
warmup attempted: 20 per payload/concurrency level
measured attempts: 100 per payload/concurrency level
formal matrix attempts: 2400
valid successful samples: 0
```

Formal matrix全部命中 `SOURCE_DENIED / 403`，仅测得快速拒绝路径，全部排除。Lowercase诊断 smoke随后命中 mock gateway `UNKNOWN_ERROR / 500`，也全部排除。不得引用这些 latency/throughput 数字冻结 deadline、concurrency、queue或rate quota。

```text
p50: NOT_AVAILABLE / NO_SUCCESS_SAMPLES
p95: NOT_AVAILABLE / NO_SUCCESS_SAMPLES
p99: NOT_AVAILABLE / NO_SUCCESS_SAMPLES
sustainable throughput: NOT_AVAILABLE
first saturation point: NOT_FOUND
success error/rejection rate: NOT_AVAILABLE
CPU utilization under success load: NOT_AVAILABLE
heap/GC under success load: NOT_AVAILABLE
Hikari active/pending under success load: NOT_AVAILABLE
queue/thread saturation: NOT_OBSERVABLE
audit success-path latency: NOT_AVAILABLE
```

## 6. Numeric budget review

| Budget | Selected value | Unit | Legal range | Evidence | Safety margin | Decision |
|---|---:|---|---|---|---|---|
| raw payload | 65536 | bytes | positive integer；zero/negative/overflow/unsupported unit rejected | existing config/tests；actual near-limit success capacity未完成 | 不适用 | compatibility value retained / capacity revalidation BLOCKED |
| decision context | 32768 | bytes | positive integer；zero/negative/overflow/unsupported unit rejected | existing config/tests；actual near-limit success capacity未完成 | 不适用 | compatibility value retained / capacity revalidation BLOCKED |
| `endToEndDeadline` | NOT_SELECTED | ms | NOT_FROZEN | 无successful p99/audit transaction evidence | 无法计算 | BLOCKED |
| `maxConcurrentRequests` | NOT_SELECTED | count | NOT_FROZEN | 无成功并发饱和点/Tomcat worker/Hikari curve | 无法计算30% margin | BLOCKED |
| `queueCapacity` | NOT_SELECTED | count | 必须有界；数值未冻结 | 无queue metric/等待曲线 | 无法计算 | BLOCKED |
| `rateLimitWindow` | NOT_SELECTED | ms或s | NOT_FROZEN | 仅有JVM-local current value，无persistent evidence | 无法计算 | BLOCKED |
| `rateLimitQuota` | NOT_SELECTED | requests/window/key | NOT_FROZEN | 无可持续成功吞吐、fairness或多实例 evidence | 无法计算 | BLOCKED |
| `idempotencyLease` | NOT_SELECTED | ms | NOT_FROZEN | persistent state machine未实现，无deadline/transaction/recovery evidence | 无法计算 | BLOCKED |
| `idempotencyTtl` | NOT_SELECTED | s | NOT_FROZEN | 无客户端重试窗口与storage growth evidence | 无法计算 | BLOCKED |
| `cleanupBatchSize` | NOT_SELECTED | rows | NOT_FROZEN | 无目标表、索引、锁/删除吞吐 evidence | 无法计算 | BLOCKED |
| `cleanupInterval` | NOT_SELECTED | s | NOT_FROZEN | 无growth/cleanup throughput evidence | 无法计算 | BLOCKED |
| `retentionPeriod` | NOT_SELECTED | s | NOT_FROZEN | 无重试窗口、审计要求、storage growth evidence | 无法计算 | BLOCKED |

## 7. Invalid configuration policy

Policy语义继续冻结：所有 budget 必须显式单位、正值、无溢出且满足跨字段关系；零值、负值、NaN/非整数、unsupported unit、超实现范围、deadline不覆盖lease/事务、retention短于active lease等配置必须启动失败，不得 fallback默认值。

由于多数预算尚无数值，上下限只能冻结上述结构性规则，numeric min/max仍 `BLOCKED`。

## 8. Missing evidence for retry

1. 可复现的 actual app-wired 2xx mock-only success seed/harness，不绕过 source/tenant/HMAC/nonce/audit。
2. Uppercase `NQ_DRYRUN` current contract 与 app wiring normalization一致性关闭或明确的独立 blocker disposition。
3. Tomcat worker、accept/backlog/queue runtime metrics或等价可观测证据。
4. Successful payload × concurrency matrix，至少 1/2/4/8/16 并继续至首个明确退化点；每档预热与足量样本。
5. Successful CPU、heap/GC、Hikari active/pending/acquire、audit latency 与 DB round-trip evidence。
6. Docker/Testcontainers或等价隔离 PostgreSQL并发证据，skipped必须为0。
7. Persistent rate/idempotency schema/port存在后的 contention、lease、crash recovery、cleanup/retention evidence；在实现前无法通过现有 JVM-local替代。

## 9. Final readiness

```text
STAGE_QDR_7_B1_RESOURCE_CAPACITY_BLOCKER: BLOCKED
RESOURCE_CAPACITY_EVIDENCE: INSUFFICIENT
PAYLOAD_CONTEXT_BUDGETS: BLOCKED / CAPACITY_REVALIDATION_INCOMPLETE
DEADLINE_BUDGET: BLOCKED
CONCURRENCY_QUEUE_BUDGETS: BLOCKED
RATE_LIMIT_BUDGET: BLOCKED
IDEMPOTENCY_LEASE_TTL: BLOCKED
CLEANUP_RETENTION_BUDGETS: BLOCKED
INVALID_CONFIGURATION_POLICY: FROZEN / NUMERIC_RANGES_BLOCKED
STAGE_QDR_7_B1_RUNTIME_CONTRACT_SAFETY_POLICY: BLOCKED
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: NO
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
next action: DH-STAGE-QDR-7-B1-RESOURCE-CAPACITY-BLOCKER-RETRY
```
