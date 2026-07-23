# DH Stage-QDR-7 B1 Capacity Blocker Resolution

> task: `DH-STAGE-QDR-7-B1-CAPACITY-BLOCKER-RESOLUTION`
> mode: `REVIEW_ONLY + CAPACITY_GATE_RESEQUENCING + SOURCE_CONTRACT_DISPOSITION + WORK_ORDER_CORRECTION`
> endpoint: `POST /api/ai/decision-dry-runs`
> implementation: `NOT_STARTED`
> verdict: `DONE / SEQUENCE_FROZEN / SOURCE_CODE_FIX_REQUIRED`

## 1. Resolution decision

本轮只裁定 source 合同、容量安全边界和阶段顺序，不实施代码、测试、migration、API/OpenAPI、persistent guards 或 capacity harness。既有无 2xx benchmark 仍只证明容量证据不足，不再作为阻止 B2 schema/security review 的循环前置条件。

```text
STAGE_QDR_7_B1_CAPACITY_BLOCKER_RESOLUTION: DONE
SOURCE_CONTRACT: FROZEN
SOURCE_DRIFT_DISPOSITION: CODE_FIX_REQUIRED
B1_SOURCE_NORMALIZATION_CONTRACT_FIX_REQUIRED
CAPACITY_GATE_SEQUENCE: FROZEN
PRE_B2_SAFETY_LIMITS: FROZEN
POST_B2_CAPACITY_ACCEPTANCE: REQUIRED
B1_RUNTIME_CONTRACT: BLOCKED / SOURCE_CODE_FIX_REQUIRED
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW: NO / SOURCE_FIX_FIRST
ALLOW_STAGE_QDR_7_B2_IMPLEMENTATION_NOW: NO
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
```

## 2. Canonical source contract

| 项目 | 冻结合同 |
|---|---|
| canonical wire value | `NQ_DRYRUN` |
| case sensitivity | 大小写敏感；只接受精确大写值 |
| request trim | 禁止；header 与 body 的 wire value 不做 trim、lowercase、uppercase、alias 或 fallback |
| header/body binding | `X-NQ-DH-Source` 与 `body.source` 必须逐字符精确相等 |
| normalization owner | 调用方负责发送 canonical wire value；DH request path 不做 normalization |
| config normalization | 只允许在配置解析时去除配置项首尾空白；配置值仍保留大小写并与验签后的 wire value 精确比较 |
| signature material | 使用 `body.source` 的原始 wire value；header 必须与该值精确绑定；raw body hash 继续覆盖 body 内 source |
| source allowlist | 对验签后的 wire value 做 case-sensitive exact match |
| tenant/source pair | tenant 继续按既有 authenticated/body binding；pair 中 source 使用同一精确 wire value，不做 lowercase |
| aliases | `nq_dryrun`、`NQ-DRYRUN`、`NQ_MOCK` 均不是该 protected endpoint 的 canonical source |

Benchmark harness 不拥有 source normalization 权限，不得临时改写 source 来制造成功样本。任何 source/header/body/config 不一致必须先按正式合同修复，再生成合法 HMAC。

### 2.1 Error taxonomy

```text
missing/blank/unknown/alias/lowercase source: SOURCE_DENIED / 403
header source != body source: SOURCE_DENIED / 403
tenant/source pair not allowlisted: SOURCE_DENIED / 403
signature missing/invalid or signature material drift: SIGNATURE_INVALID / 401
authenticated tenant != body tenant: TENANT_MISMATCH / 403
```

错误响应、日志与 audit 不得回显 raw source、signature、nonce 或 raw payload。

### 2.2 Disposition

- `HmacNqDryRunAuthenticator` 已按精确 wire value 签名和比较，测试也明确拒绝 lowercase 与 alias。
- `DecisionDryRunRequest` 与 application 配置将 `NQ_DRYRUN` 作为 canonical source。
- `DecisionDryRunRuntimeProperties` 当前对 request/config source 执行 `trim().toLowerCase(Locale.ROOT)`，与 authenticator 的 exact-wire 语义冲突；actual wiring 已因此拒绝 uppercase canonical request。
- `contracts/openapi.yaml` 未声明既有 decision dry-run endpoint/source enum，存在 contract coverage ambiguity；本轮不修改 OpenAPI。
- 历史 integration fixtures 主要使用 `NQ_MOCK`，只能作为旧 mock contract evidence，不能覆盖当前 protected endpoint 合同。
- 上轮临时 lowercase benchmark smoke 属于诊断，不是可保留的 harness normalization。

主分类为 `PRODUCTION_CODE_DRIFT`，并伴随 `CONTRACT_AMBIGUITY` 与诊断性 `TEST_HARNESS_DRIFT`。修复需要 Java 测试/实现及可能的 OpenAPI contract sync，超出本轮授权，因此必须独立阻断：

```text
B1_SOURCE_NORMALIZATION_CONTRACT_FIX_REQUIRED
next action: DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-BLOCKER-FIX
```

## 3. Pre-B2 safety contract

以下是配置 schema/security 的绝对 hard ceiling，不是生产默认值，不声称来自吞吐 benchmark。实现必须配置驱动、启动时严格校验；缺失、零值、负值、溢出、unsupported unit、超过 ceiling 或跨字段冲突均启动失败，禁止静默 fallback。

| 配置项 | 类型 / 单位 | 合法范围与绝对上限 |
|---|---|---|
| raw payload | positive integer / bytes | `1..65536`；wire hard limit 为 `65536 bytes` |
| decision context | positive integer / bytes | `1..32768`；context hard limit 为 `32768 bytes` |
| end-to-end deadline | positive integer / milliseconds | `1..300000`；不得 infinite |
| max concurrency | positive integer / requests per instance | `1..1024` |
| queue capacity | positive integer / requests per instance | `1..4096`；禁止无界队列 |
| rate-limit window | positive integer / milliseconds | `1..3600000` |
| rate-limit quota | positive integer / requests per window/key | `1..100000` |
| idempotency lease | positive integer / milliseconds | `1..900000` |
| idempotency TTL | positive integer / milliseconds | `1..2592000000`（30 days） |
| cleanup batch | positive integer / rows | `1..10000` |
| cleanup interval | positive integer / milliseconds | `1..86400000`（24 hours） |
| retention period | positive integer / milliseconds | `1..7776000000`（90 days） |

这些 ceiling 只限制配置空间。B2 schema/security review 可以在不掌握最终吞吐默认值的前提下审查字段类型、约束、索引、事务、隔离、清理安全与 failure handling；不得把 ceiling 直接复制为 default。

### 3.1 Cross-field invariants

- `queueMaxWait < endToEndDeadline < idempotencyLease < idempotencyTtl <= retentionPeriod`。
- deadline 必须覆盖 persistent guard、排队、执行、audit 和 idempotency finalization；不得只覆盖业务方法。
- queue 必须显式有界；queue full 或预计等待耗尽 deadline 时立即拒绝。
- max concurrency 与 queue capacity 必须分别配置，禁止用 Tomcat/Hikari 或 executor 的隐式/无界默认值代替。
- rate-limit window/quota 必须绑定 `environment + endpoint + source + tenant`，storage failure fail-closed。
- lease 使用数据库时间；active lease 不得被 cleanup/retention 删除或抢占。
- cleanup 必须有界、可重入、tenant/environment-safe，不在 request hot path 做无界扫描。
- persistent rate/idempotency/kill store 不可用、未知或 commit outcome unknown 时拒绝；禁止回退到 JVM-local/in-memory。

## 4. Post-B2 measured defaults

以下运行默认值全部保持 `NOT_SELECTED / POST_B2_MEASUREMENT_REQUIRED`：

```text
end-to-end deadline
max concurrency
queue capacity
rate-limit window/quota
idempotency lease/TTL
cleanup batch/interval
retention period
```

它们只能在 persistent guards 实现完成、actual-wiring 2xx harness 成功、Docker/Testcontainers 0 skipped 且 metrics 完整后，通过 B2 capacity acceptance 冻结。本轮不选择、不推荐、不暗示任何默认值。

## 5. Revised stage sequence

```text
B1:
  安全语义、配置模型、合法范围、绝对 hard ceiling

B2:
  schema/security review
  -> persistent guards implementation
  -> actual-wiring 2xx harness

B2 capacity acceptance:
  冻结测量后的运行默认值

B3:
  deadline、backpressure、dynamic kill 等 operational safety

B4:
  protected-entry acceptance
```

- B2 schema/security review 不依赖最终吞吐调优值，但当前须先关闭 source production-code blocker。
- B2 implementation 必须配置驱动并严格执行本文件的类型、单位、范围、ceiling 与跨字段校验。
- B2 capacity acceptance 通过前禁止进入 B4；B3 也不得把未测量默认值伪装为已冻结容量合同。
- Persistent guard store failure 始终 fail-closed，禁止 in-memory fallback。

## 6. Actual-wiring 2xx harness contract

后续 harness 必须同时满足：

1. 使用 actual Spring wiring 与实际 embedded server，不用 standalone MockMvc 替代。
2. 使用 `NQ_DRYRUN` 精确 source、合法 tenant/source pair 与基于原始 wire value 的 HMAC。
3. 提供 deterministic mock gateway seed/profile，成功路径可重复且不依赖临时手工数据。
4. 仅绑定 loopback，不调用 external HTTP、Provider、NQ、Agent、LangGraph 或 LIVE。
5. 每轮使用 isolated PostgreSQL；Docker/Testcontainers 相关 suite 必须 `0 skipped`。
6. 采集 Tomcat worker/connection/accept queue、Hikari active/pending/acquire、JVM heap/GC/CPU、HTTP status/latency/throughput 与必要的低基数 guard metrics。
7. raw payload、signature、nonce、credential 与敏感上下文不得进入日志、metrics label 或报告。
8. 固定 seed、profile、payload corpus、warmup、样本数、并发矩阵、环境指纹和结果格式，重复运行结果可比较。

在 source blocker、persistent guards 和上述 harness 前置均未完成时：

```text
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
POST_B2_CAPACITY_ACCEPTANCE: REQUIRED / NOT_STARTED
ALLOW_STAGE_QDR_7_B4_ACCEPTANCE_NOW: NO
```

## 7. Boundary

本 resolution 不修改 NQ、Java、测试、migration、API/Controller/OpenAPI、Repository/JDBC 或 runtime wiring；不实现 persistent guards/harness，不运行 benchmark，不创建 tag，不 commit，不 push。Stage-QDR-7 继续为 `PLANNING / IMPLEMENTATION_NOT_STARTED`。
