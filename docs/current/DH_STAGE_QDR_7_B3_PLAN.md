# DH Stage-QDR-7 B3 Limited Dry-Run Runtime Readiness Plan

> task: `DH-STAGE-QDR-7-B3-PLAN-AND-WORK-ORDER-FREEZE`
> status: `CLOSED / ACCEPTED / PLAN_ONLY`
> planning baseline: `6193df72d1ac489f40f63cf665ca984f273b5344`
> stage name: `Stage-QDR-7 B3 / LIMITED DRY-RUN RUNTIME READINESS`
> implementation task: `DH-STAGE-QDR-7-B3-LIMITED-DRYRUN-RUNTIME-READINESS-IMPLEMENTATION`
> implementation state: `NOT_STARTED / NEXT`
> implementation work order: `FROZEN / SCOPE CONTRACT COMPLETE`
> scope invariants: `PASS / 3 OF 3`
> scope errata: `DH-STAGE-QDR-7-B3-IMPLEMENTATION-WORK-ORDER-SCOPE-ERRATA-FREEZE / CLOSED / ACCEPTED / DOCUMENTATION_ONLY`

## 1. 当前事实与规划边界

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
Post-B2 capacity acceptance: DEFERRED / KNOWN_LIMITATION
Stage-QDR-7 B3 plan: CLOSED / ACCEPTED
Stage-QDR-7 B3 implementation work order: FROZEN / SCOPE CONTRACT COMPLETE
Stage-QDR-7 B3 implementation: NOT_STARTED / NEXT
Scope contracts: READ_SCOPE / WRITE_ALLOWLIST / VALIDATION_SCOPE / FIXABLE_BLOCKER_SCOPE / CURRENT_FACTSOURCE_SCAN_SCOPE = FROZEN
Scope invariants: PASS / 3 OF 3
ALLOW_STAGE_QDR_7_B3_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_API_CHANGE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本计划只冻结 B3 名称、目标、实施范围、批次、验收与停止条件，不实施任何业务代码、测试、配置、API、migration、runtime wiring 或外部连接。B2 不重新打开，不创建 Retry-5、capacity blocker 或 harness 修复任务。

## 2. 代码现实

当前受保护入口为 `POST /api/ai/decision-dry-runs`。现有链路已经提供：

- `DhApiAuthenticationFilter` 认证上下文与 tenant 可信来源。
- Controller 内 raw payload cap、canonical header/body binding、HMAC、UTC timestamp、nonce、tenant/source allowlist 与 persistent rate admission。
- `PersistentGuardedDecisionDryRunService` 的 persistent idempotency admission、状态转换、safe result projection 与 fail-closed。
- `DecisionDryRunRuntimeProperties` 的默认关闭、非 dev/test 独立 production gate、启动期 kill switch 和 tenant/source allowlist。
- `DefaultDecisionDryRunService` 的 read-only decision、mock-only gateway、decision snapshot、audit、trace、replay ref 与失败关闭。
- `MockModelProvider` 的 deterministic、no HTTP、no SDK、no NQ、no trading 行为。
- 既有 V5/V6/V8/V12-V14 表与 JDBC adapter 足以承载当前 snapshot/audit/trace/persistent guard；B3 不需要新增 schema 或 Repository。

当前真实缺口：

- feature flag、environment 与 kill 状态仍聚合在启动期 properties snapshot，缺少独立 runtime policy 合同。
- kill switch 没有动态共享实现；在 production/capacity 未获准前不得把当前配置快照描述为多实例动态 kill。
- 从受信 admission 到 guard、执行、audit 和 idempotency finalization 尚无统一 deadline。
- 缺少明确的 bounded concurrency、bounded queue/backpressure 与拒绝分类。
- 运行边界测试尚未把 flag、production deny、kill、unknown environment、deadline、queue full、no retry 和 no-side-effect 汇总为 B3 readiness 证据。

## 3. 唯一方向选择

选择方向 A：`Limited Dry-Run Runtime Readiness`。

唯一业务目标：把现有受保护、mock-only、read-only decision dry-run 链路收敛为一个默认关闭、仅 dev/test 可显式启用、有界、可立即拒绝、全程 fail-closed 且可审计的 limited runtime readiness 边界。

拒绝其他方向：

- 方向 B `Decision Evidence / Audit Consolidation`：现有 decision snapshot、audit、trace、replay ref 与 JDBC persistence 已足以支撑本轮运行边界；本轮不再建设第二套 evidence/audit 主线。
- 方向 C `Persistent Guard Operational Hardening`：B2 persistent guard 已关闭并接受；capacity deferred 不是重开 B2 的理由。
- 方向 D `Agent / LangGraph Runtime Preparation`：当前明确禁止，继续后置。

## 4. 明确非目标

- 不新增或修改 endpoint、Controller、request/response envelope、OpenAPI、JSON Schema、contracts 或 golden cases。
- 不新增 migration、表、列、索引、Repository port 或 JDBC adapter。
- 不接真实 HTTP、Provider SDK、real provider、NQ runtime client、NQ DB、Agent、LangGraph、Paper 或 LIVE。
- 不改变 HMAC、timestamp、nonce、tenant、source、canonicalization、rate 或 idempotency 语义。
- 不执行 formal capacity acceptance，不宣称 capacity PASS，不把 B3 完成写成 production readiness 或 Integration-1 runtime accepted。
- 不实现 outbound retry、Provider retry 或 circuit breaker；当前没有真实 outbound target。

## 5. B2 capacity deferred 硬限制

1. Capacity gate deferred 不是 capacity PASS。
2. B3 不得依赖正式容量认证或把 qualification 当作 formal 证据。
3. B3 不得开启生产流量。
4. B3 不得连接真实 NQ 或真实 Provider。
5. B3 不得把 runtime flag 默认设为 enabled。
6. B3 所有可执行入口必须默认关闭且仅 dev/test 可显式启用；unknown environment 必须拒绝。
7. B3 不提升 Paper 或 LIVE 权限。
8. B3 完成不等于 Integration-1 runtime accepted。
9. B3 完成不允许 Agent 或 LangGraph。
10. 未完成的 formal capacity acceptance 保留为 `KNOWN_LIMITATION` 和后续 backlog；不得创建 Retry-5。

## 6. 冻结安全合同

### 6.1 Runtime policy

实施必须建立并使用以下明确合同；名称可按现有包风格微调，但职责不得合并消失：

```text
LimitedDryRunRuntimePolicy
RuntimeFeatureFlag
RuntimeKillSwitch
RuntimeEnvironmentPolicy
RuntimeFailureClassification
NoSideEffectDecisionContract
```

判定顺序冻结为：feature disabled -> production/unknown environment deny -> kill deny/unknown/stale -> invalid runtime limits -> bounded admission -> existing security/persistent guards -> mock-only execution -> audit/trace/snapshot -> idempotency finalization。任一状态未知、过期、互相矛盾或异常均拒绝。

### 6.2 配置与资源 hard ceiling

这些值是 dev/test limited readiness 的安全上限，不是容量测量或生产默认值：

```text
runtime enabled default: false
production enabled: false
prod kill switch: true
deadline: required when enabled / 1..30000 ms
max concurrency: required when enabled / 1..32
queue capacity: required when enabled / 0..64
max queue wait: required when queue > 0 / 0..1000 ms / strictly less than deadline
retry count: 0
provider kind: MOCK only
external HTTP calls: 0
NQ runtime calls: 0
```

Runtime 关闭时允许 limits 为 0 以保持惰性；一旦 enabled，缺失、非正、越界或互相矛盾必须在启动时失败或入口 fail-closed，禁止静默 fallback。`queue capacity = 0` 表示不排队、无执行槽立即拒绝。

### 6.3 Kill switch 限定

B3 必须抽象 kill decision，并证明 `DENY / UNKNOWN / STALE / READ_FAILED` 均拒绝。当前仓库没有共享动态 kill store，且本计划禁止 migration/Repository 扩展，因此 B3 只能完成 dev/test limited readiness 的配置驱动 kill 合同，不得声称多实例动态 kill 或 production kill readiness。若实施要求共享动态状态、传播版本或持久化，必须停止并进入独立 security/persistence review；不能在本任务内扩大范围。

### 6.4 No-side-effect 合同

- 成功输出只能是 `OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`；内部 `ABSTAIN` 只能安全映射为 `NO_TRADE`。
- `BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER` 必须拒绝或仅作为 forbidden label 出现。
- order、risk、ledger、account、Paper、LIVE mutation 必须为 0。
- outbound HTTP、NQ client、Agent、LangGraph 调用必须为 0。
- raw body、HMAC/signature、nonce、credential、raw prompt、raw provider response 不得进入日志、metric label、audit payload 或 snapshot。

## 7. 批次冻结

### B3.1 Runtime Boundary Contracts

实现 runtime policy、feature flag、kill decision、environment policy、failure classification、deadline/concurrency/queue 配置校验与 no-side-effect contract。普通 domain/usecase 变更连续实施，不单独 review。

### B3.2 Protected Mock-Only Runtime Wiring

仅在现有 dry-run endpoint 内部装配 B3.1 合同，复用当前认证、persistent guards、mock provider、snapshot/audit/trace/replay。允许修改既有 `DecisionDryRunRuntimeWiringConfig` 和 application profile 配置；不允许修改 Controller、API contract、migration 或 Repository。

### B3.3 Runtime Readiness and No-Side-Effect Tests

补齐 unit、wiring、WebMvc、architecture 与 PostgreSQL/Testcontainers 相关回归，证明所有运行限制与 no-side-effect 边界。不得修改 capacity harness、criteria 或 formal artifact contract。

### B3.4 Final Close

作为独立 milestone close 执行 full regression、quality、PostgreSQL/Testcontainers、security/no-side-effect scan、current facts 同步和自包含 B3 milestone evidence packet。B3 close 不创建 Stage-QDR-7 tag；Stage tag 仍由后续 B5 final close 按 archive-before-tag 规则处理。

## 8. 影响判定与停止条件

```text
API / Controller impact: NONE / NOT_ALLOWED
migration impact: NONE / NOT_ALLOWED
Repository impact: NONE / NOT_ALLOWED
runtime wiring impact: YES / EXISTING DRY-RUN INTERNAL WIRING ONLY
security impact: HARDENING ONLY / EXISTING AUTH SEMANTICS UNCHANGED
```

出现以下任一情况立即停止当前 implementation：

- 需要新增或修改 API、Controller、OpenAPI、request/response envelope。
- 需要 migration、表结构、Repository port/JDBC adapter 或 production persistence 扩张。
- 需要改变 HMAC、tenant、nonce、timestamp、source 或认证顺序。
- 提案包含真实 HTTP、real Provider、真实 NQ 调用。
- 发现 P0/P1 安全缺陷。

上述情况只能进入独立 review，不能在普通 B3 批次中顺带实现。

## 9. 测试矩阵

- flag 默认关闭；enabled 缺失仍关闭。
- prod profile 拒绝；unknown/no-profile environment 拒绝。
- kill `DENY / UNKNOWN / STALE / READ_FAILED` 均拒绝且优先于业务执行。
- deadline 超时、queue full、concurrency exhausted、queue wait 超限均返回稳定 fail-closed 分类。
- runtime limits 缺失、非正、越界、互相矛盾时启动失败或入口拒绝。
- HMAC、tenant、source、timestamp、nonce、same nonce reject 持续强制。
- persistent rate 与 idempotency duplicate 语义不回归，不回退 in-memory。
- mock provider only；external HTTP 与 NQ runtime call 均为 0；retry count 为 0。
- `BUY / SELL / MARKET_ORDER / PLACE_ORDER / CANCEL_ORDER` 拒绝。
- order/risk/ledger/account/Paper/LIVE mutation 为 0。
- success/reject/timeout/backpressure 路径均有 redacted audit、trace 与 snapshot/result ref；audit failure 仍 fail-closed。
- PostgreSQL/Testcontainers 必须真实执行且 0 skipped；full regression 0 failure/error/skip；quality Checkstyle 0、Spotless PASS。

## 10. 回滚与完成条件

实施回滚首先设置 kill 为 deny，并保持 runtime feature disabled；随后对各 B3 commit 使用 `git revert`，不得 destructive reset。回滚不得删除 B2 persistent guard evidence、idempotency/nonce evidence 或历史 migration。

B3 implementation 完成只表示 limited dev/test mock-only readiness 已实现并验证；仍不代表 capacity PASS、production readiness、Integration-1 runtime accepted、真实 Provider/NQ integration 或 LIVE。

下一任务唯一冻结为：

```text
DH-STAGE-QDR-7-B3-LIMITED-DRYRUN-RUNTIME-READINESS-IMPLEMENTATION
```
