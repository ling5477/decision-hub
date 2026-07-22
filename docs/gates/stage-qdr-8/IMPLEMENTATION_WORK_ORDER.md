# DH Stage-QDR-8 Structured Feedback Attribution Foundation Implementation Work Order

## Terminal current authority — 2026-07-21 Stage-QDR-8 implementation local accepted

```text
Stage-QDR-7 B1: FROZEN
Stage-QDR-7 B2: CLOSED WITH CAPACITY GATE DEFERRED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
Stage-QDR-7 B3: CLOSED / ACCEPTED
Stage-QDR-8 plan commit: PUBLISHED / 0fae8b3ee3da197c32ac8bc2d13ce9e3ba0e86a3
Stage-QDR-8 planning exact-SHA CI: PASSED / ACCEPTED / RUN 29830659396
Planning remote regression: PASS / 19 OF 19 REACTOR / 1161 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Stage-QDR-8 implementation: IMPLEMENTED / LOCAL_ACCEPTED
Structured feedback attribution: DETERMINISTIC / DECISION_BOUND / TENANT_BOUND / ENVIRONMENT_BOUND
Attribution safety: AUDITABLE / REPLAY_REFERENCE_SAFE / IDEMPOTENT / NO_SIDE_EFFECT
Persistence / API / runtime wiring: NOT_ADDED
Implementation baseline: 0fae8b3ee3da197c32ac8bc2d13ce9e3ba0e86a3
Implementation commit: THIS_DOCUMENT_COMMIT / LOCAL_ONLY
Local regression: PASS / 19 OF 19 REACTOR / 1189 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
Local PostgreSQL/Testcontainers: REAL EXECUTION / ZERO MANDATORY SKIPS
Local quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
Remote CI: PENDING NEW IMPLEMENTATION COMMIT
Stage-QDR-8 final close: PENDING EXACT_SHA CI
current task: DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
current task status: DONE / LOCAL_ACCEPTED
next action: OBTAIN IMPLEMENTATION PUSH AUTHORIZATION; FAST-FORWARD PUSH; RUN EXACT-SHA TEST + QUALITY CI
Scope invariants: PASS / 3 OF 3
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 16 OF 16 / 0 CONFLICTS
ALLOW_STAGE_QDR_8_IMPLEMENTATION: NO / CONSUMED_LOCAL_ACCEPTED
ALLOW_STAGE_QDR_8_IMPLEMENTATION_NOW: NO / CONSUMED_LOCAL_ACCEPTED
ALLOW_EXACT_SHA_CI: YES / AFTER PUSH AUTHORIZATION
ALLOW_STAGE_QDR_8_FINAL_CLOSE: NO / EXACT_SHA_CI_REQUIRED
ALLOW_API_CHANGE / ALLOW_MIGRATION / ALLOW_REPOSITORY_EXPANSION: NO / NO / NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

本工单已消费并完成本地验收；冻结正文保留为历史 scope 合同。没有授权 persistence/API/runtime wiring，也没有授权 push 或 tag。

## 0. Historical planning freeze — 工单冻结状态

```text
Stage-QDR-7 B3: CLOSED / ACCEPTED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
CI-red remediation: CLOSED / ACCEPTED
Exact-SHA CI: PASSED / ACCEPTED / RUN 29823413542
Work order: FROZEN / SCOPE CONTRACT COMPLETE
Implementation task: DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
Planning baseline: 8906389352d9d92099acdb857fce97aece3e6a20
Selected direction: STRUCTURED_FEEDBACK_ATTRIBUTION_FOUNDATION
Implementation: NOT_STARTED / NEXT
Scope invariants: PASS / 3 OF 3
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 16 OF 16 / 0 CONFLICTS
ALLOW_STAGE_QDR_8_IMPLEMENTATION_NOW: NO / CURRENT_TASK_IS_PLANNING_ONLY
ALLOW_STAGE_QDR_8_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_API_CHANGE: NO
ALLOW_MIGRATION: NO
ALLOW_REPOSITORY_ADAPTER: NO
ALLOW_RUNTIME_WIRING: NO
ALLOW_REAL_HTTP / ALLOW_REAL_PROVIDER / ALLOW_NQ_RUNTIME: NO / NO / NO
ALLOW_AGENT / ALLOW_LANGGRAPH / ALLOW_PAPER / ALLOW_LIVE: NO / NO / NO / NO
```

本文件是下一精确 implementation task 的冻结工单，不代表已实施。任何 implementation 必须重新执行 Git 预检并确认当前 HEAD 仍以本 planning commit 为直接基线；若出现分叉、scope 不闭合或新的 P0/P1 安全问题，停止并输出明确 blocker。

## 1. 目标与边界

### 1.1 目标

在不接真实外部系统、不修改 API/DB/runtime wiring 的条件下，完成：

1. tenant/environment/decision/trace-bound feedback subject。
2. explicit source/time/id 的 outcome observation。
3. deterministic、explainable、bounded-confidence attribution domain contracts。
4. use-case validation、canonicalization、idempotency、policy、audit fail-closed orchestration。
5. 与现有 evidence/replay 的 safe ref 边界。
6. 完整测试、quality、边界扫描与 current factsources 收口。

### 1.2 明确不做

```text
不修改现有 NqFeedback API、Controller、schema 或 ingestion envelope
不把现有 boolean positive 自动强化链改写为新归因
不调用 ExperienceFeedbackService.apply
不修改 ExperienceEntry / PheromoneEdge / Prompt / 模型 / 策略 / 候选 / JudgeDecision
不新增 Flyway、JDBC、Repository adapter 或表
不新增 Spring production bean 或 dry-run runtime wiring
不接真实 NQ outcome、HTTP、Provider、Agent、LangGraph、Paper 或 LIVE
不重开 Stage-QDR-7 B2/B3
不宣称 production capacity
```

## 2. 实施前置条件

实施任务开工必须全部满足：

```text
branch: dev
planning commit: local HEAD or direct accepted successor
worktree: clean
staged: empty
Stage-QDR-7 B3: CLOSED / ACCEPTED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Stage-QDR-8 plan: CLOSED / ACCEPTED
Stage-QDR-8 work order: FROZEN / SCOPE CONTRACT COMPLETE
scope invariants: PASS / 3 OF 3
```

若 current authority、HEAD、scope 或安全边界不满足，输出：

```text
STAGE_QDR_8_IMPLEMENTATION_PREFLIGHT_BLOCKED
```

不得 pull、merge、rebase、reset、force push 或自行扩大 scope。

## 3. 冻结 Scope 五元组

### 3.1 `READ_SCOPE`

```text
全部 Git tracked repository files
本工单 implementation 所需的 git branch/status/diff/log 元数据
与 implementation commit 精确匹配的本地/远端 test + quality 只读证据
```

排除：

```text
.git/** 内容扫描（仅允许 git 命令读取必要元数据）
target/**
CapacityRuns/**
Recovery/**
node_modules/**
build/**
dist/**
logs/**
test-results/**
外部仓库
凭据目录与 production .env
IDE 缓存
```

### 3.2 `WRITE_ALLOWLIST`

#### 3.2.1 Domain

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
```

#### 3.2.2 Use case

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
```

#### 3.2.3 Boundary test

```text
dh-app/src/test/java/com/guidinglight/decisionhub/ArchitectureTest.java
```

`ArchitectureTest.java` 只允许增加 Stage-QDR-8 禁止依赖/副作用扫描，不允许顺手重构现有规则。

#### 3.2.4 Current factsources（16 个）

```text
AGENTS.md
CLAUDE.md
README.md
docs/current/README.md
docs/current/CODEX_PROJECT_INSTRUCTIONS.md
docs/current/FACTSOURCE_POLICY.md
docs/current/STATUS.md
docs/current/WORK_ORDER.md
docs/current/ROADMAP.md
docs/current/TESTING.md
docs/current/WORKLOG.md
docs/current/DH_STAGE_QDR_7_IMPLEMENTATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_7_B3_PLAN.md
docs/current/DH_STAGE_QDR_7_B3_IMPLEMENTATION_WORK_ORDER.md
docs/current/DH_STAGE_QDR_8_PLAN.md
docs/current/DH_STAGE_QDR_8_IMPLEMENTATION_WORK_ORDER.md
```

### 3.3 `VALIDATION_SCOPE`

```text
全部 Git tracked repository files
WRITE_ALLOWLIST 中的 domain/usecase/architecture tests
16 个 current factsources
Maven 19-module reactor 的 test 与 quality 输入
禁止范围 diff：production app/api/security/infra/POM/workflow/config/migration/Repository/contracts/golden_cases
```

`VALIDATION_SCOPE` 不授权修改其中不在 `WRITE_ALLOWLIST` 的文件。

### 3.4 `FIXABLE_BLOCKER_SCOPE`

```text
WRITE_ALLOWLIST 中的 domain/usecase production files
WRITE_ALLOWLIST 中的 domain/usecase/architecture tests
16 个 current factsources
```

以下 blocker 只记录并停止，不得在本任务修复：

```text
API / Controller / OpenAPI / contracts / golden_cases
Flyway / JDBC / persistence adapter / Repository expansion
Spring runtime wiring / config / POM / workflow
真实 Provider / HTTP / NQ / Agent / LangGraph / Paper / LIVE
Stage-QDR-7 B2 capacity gate
无关模块既有缺陷
```

### 3.5 `CURRENT_FACTSOURCE_SCAN_SCOPE`

精确等于 3.2.4 的 16 个路径；不得使用只扫描 primary 8 文件的缩减口径。

### 3.6 Scope invariant 结果

```text
VALIDATION_SCOPE ⊆ READ_SCOPE: PASS
FIXABLE_BLOCKER_SCOPE ⊆ WRITE_ALLOWLIST: PASS
CURRENT_FACTSOURCE_SCAN_SCOPE ⊆ WRITE_ALLOWLIST: PASS
SCOPE_INVARIANTS: PASS / 3 OF 3
TASK_SCOPE_DESIGN: PASS
```

若未来编辑前任一关系不成立：

```text
TASK_SCOPE_DESIGN_INVALID
```

## 4. Domain 实施合同

### Step

新增 immutable domain contracts 与 invariant validation。

### Files

```text
dh-domain/src/main/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
dh-domain/src/test/java/com/guidinglight/decisionhub/domain/qdr/feedback/**
```

### 必须实现

```text
FeedbackSubject
FeedbackEnvironment
ObservedDecisionOutcome
OutcomeObservation
OutcomeSource
AttributionDimension
AttributionContribution
AttributionImpact
AttributionResult
FeedbackConfidence
FeedbackStatus
FeedbackPolicy
FeedbackAuditReference
```

合同要求：

- 全部 public/protected 类型和方法使用中文 Javadoc，解释 tenant/environment、fail-closed、幂等和副作用边界。
- 所有集合 defensive copy + stable order；所有值对象 immutable。
- `FeedbackSubject` 必须绑定 tenantId/environment/decisionId/traceId。
- environment 只接受 `DEV` / `TEST`。
- `OutcomeSource` 只接受 `DRY_RUN_RESULT`、`DETERMINISTIC_REPLAY`、`STRUCTURED_TEST_FIXTURE`。
- confidence 拒绝 NaN、Infinity 与 `[0,1]` 外值。
- outcome/impact/dimension/status 未知值不得回退为默认成功。
- 不出现 BUY/SELL/PLACE_ORDER/CANCEL_ORDER 或 NQ/Paper/LIVE mutation 语义。

### Success

```text
domain unit tests: PASS
normal/failure/boundary/isolation paths: COVERED
cross-tenant/cross-environment: REJECTED
unknown values: FAIL_CLOSED
production dependency/API/persistence diff: 0
```

## 5. Use-case 实施合同

### Step

实现 pure deterministic orchestration、ports、canonical hash、idempotency 和 audit fail-closed。

### Files

```text
dh-usecase/src/main/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/qdr/feedback/**
```

### 必须实现

```text
FeedbackAttributionCommand
FeedbackAttributionService
DefaultFeedbackAttributionService
FeedbackAttributionPolicyEvaluator
FeedbackAttributionIdempotencyPort
FeedbackAttributionAuditPort
FeedbackAttributionResult
FeedbackAttributionErrorCode
FeedbackCanonicalizer
FeedbackCanonicalHash
```

### 固定执行顺序

```text
1. validate subject/environment
2. validate source/time/observation identity
3. validate tenant/decision/trace correlation
4. canonicalize and hash
5. idempotency lookup
6. policy evaluation
7. deterministic attribution
8. audit through port
9. immutable result
```

### 失败降级

| 失败 | 固定结果 |
|---|---|
| subject/source/time invalid | `INVALID_OBSERVATION` / REJECTED |
| tenant/environment/correlation mismatch | `CORRELATION_MISMATCH` / REJECTED |
| unknown enum/value | `UNKNOWN_VALUE` / REJECTED |
| idempotency port unavailable | `IDEMPOTENCY_UNAVAILABLE` / REJECTED |
| same key / different hash | `IDEMPOTENCY_CONFLICT` / REJECTED |
| mandatory evidence missing | `EVIDENCE_INCOMPLETE` / INCONCLUSIVE |
| audit port failure | `AUDIT_FAILED` / REJECTED |
| unexpected internal error | `ATTRIBUTION_FAILED` / REJECTED，脱敏，不返回堆栈/原始错误。 |

### 幂等 key

```text
SHA-256("DH-QDR8-FEEDBACK-ATTRIBUTION-V1"
  + tenantId
  + environment
  + decisionId
  + observationId)
```

同 key + 同 canonical hash 返回首个结果；同 key + 不同 hash fail-closed。并发重复必须只形成一个 accepted result/audit write。实现任务只定义 port 与 orchestration；测试使用有界 deterministic fake，不新增 production adapter。

### 禁止副作用

```text
ExperienceFeedbackService.apply calls = 0
Experience/Pheromone mutation = 0
Prompt/model/strategy/candidate/Judge mutation = 0
HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE calls = 0
database writes = 0（除尚未授权、且本任务不存在的 adapter）
```

### Success

```text
deterministic same input/policy result/hash: PASS
idempotency and concurrent duplicate contract: PASS
audit fail-closed: PASS
no-side-effect architecture scan: PASS
```

## 6. Batch 冻结

### Batch 1 — Domain Contracts and Invariants

- 完成第 4 节。
- 执行 domain 定向 tests 与 forbidden-term/dependency scan。
- 不创建 standalone review。
- 建议 commit：`feat(qdr): add structured feedback attribution contracts`

### Batch 2 — Use-case Orchestration and Deterministic Internal Wiring

- 完成第 5 节。
- “internal wiring” 仅指 service + ports + deterministic test fakes 的组合，不注册应用 runtime bean。
- 执行 usecase 定向 tests、并发幂等与 no-side-effect scan。
- 不创建 standalone review。
- 建议 commit：`feat(qdr): add deterministic feedback attribution service`

### Batch 3 — Persistence / API

```text
NOT_AUTHORIZED
```

若确有必要，停止当前 implementation，另起 migration 或 API/Controller review；不得在 Batch 1/2 顺手加入。

### Batch 4 — Full Regression, Quality, Evidence and Final Close

- 更新 16 个 current factsources。
- 执行完整测试与 quality。
- 执行禁止范围 diff 和 current conflict scan。
- 执行 Stage-QDR-8 final close review；archive/tag 仍需后续独立任务。
- 建议 commit：`docs(qdr): close stage-qdr-8 feedback attribution foundation`

## 7. 验证命令

### 每批最低验证

```powershell
mvn -ntp -pl dh-domain,dh-usecase -am test
git diff --check
git diff --name-only
git diff --cached --name-only
```

### Final close 验证

```powershell
mvn -B -ntp test
mvn -B -ntp -Pquality validate
git status --short
git diff --check
git diff --stat
git diff --name-only
git diff --cached --name-only
```

必须取得：

```text
19/19 reactor SUCCESS
tests >= planning baseline 1161
failures = 0
errors = 0
skipped = 0
PostgreSQL/Testcontainers mandatory suites = executed / zero skip
Checkstyle violations = 0
Spotless = PASS
scope invariants = 3/3 PASS
current factsources = 16/16
current conflict count = 0
unexpected files = 0
API/migration/Repository/contracts/golden_cases diff = 0
runtime wiring/POM/workflow/config diff = 0
```

## 8. Review 与停止条件

Standalone review 只允许：

```text
migration
API / Controller
security boundary
stage final close
new P0 / P1 blocker
```

任一以下情况立即停止：

- 需要修改 `WRITE_ALLOWLIST` 外文件。
- 需要真实 NQ outcome、Provider、HTTP、Agent、LangGraph、Paper 或 LIVE。
- 发现 cross-tenant、cross-environment、audit fail-open、unknown permissive default。
- 需要自动修改历史状态、Prompt、模型、策略、候选或 JudgeDecision。
- Maven/CI 揭示新的真实 P0/P1 代码、安全、tenant、事务、migration 或 API 问题。

外部 blocker 固定输出：

```text
STAGE_QDR_8_EXTERNAL_VALIDATION_BLOCKED
```

## 9. 回滚

- Batch 1/2 分开提交；优先 `git revert <implementation-commit>` 回滚，不重写历史。
- 本阶段无 migration/API/Repository adapter，因此回滚不涉及数据库回填或兼容窗口。
- 若 final close 文档需回滚，只 revert 对应 docs commit；Stage-QDR-7 B2/B3 状态不得改变。
- 未经用户明确授权不 push implementation/final-close commit，不创建 tag。

## 10. 完成判定

只有以下全部满足，implementation 才能进入 final close：

```text
Batch 1: DONE
Batch 2: DONE
Batch 3: NOT_AUTHORIZED / NOT_RUN
domain/usecase tests: PASS
full regression: PASS
quality: PASS
scope invariants: 3 OF 3 PASS
current conflict count: 0
no-side-effect boundaries: PASS
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
real HTTP/provider/NQ/Agent/LangGraph/Paper/LIVE: 0
```

当前下一动作仍为开始下一独立任务，不得在本 planning task 中实施：

```text
DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
```
