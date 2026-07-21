# DH Stage-QDR-8 Structured Feedback Attribution Foundation Plan

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

本计划已消费；已实现的范围仅为冻结的 domain/usecase foundation 与测试。远端 implementation exact-SHA CI 尚未执行，因此 Stage-QDR-8 仍未 final close。

## 0. Historical planning freeze — 冻结结论

```text
Stage-QDR-7 B3: CLOSED / ACCEPTED
B2 capacity gate: DEFERRED / KNOWN_LIMITATION
Production capacity: NOT_PROVEN
CI-red remediation: CLOSED / ACCEPTED
Stage-QDR-8 plan: CLOSED / ACCEPTED
Selected direction: STRUCTURED_FEEDBACK_ATTRIBUTION_FOUNDATION
Planning baseline: 8906389352d9d92099acdb857fce97aece3e6a20
Exact-SHA CI: PASSED / ACCEPTED / RUN 29823413542
Implementation task: DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
Implementation status: NOT_STARTED / NEXT
Implementation work order: FROZEN / SCOPE CONTRACT COMPLETE
CURRENT_FACTSOURCE_CONSISTENCY: PASS / 16 OF 16 / 0 CONFLICTS
ALLOW_STAGE_QDR_8_IMPLEMENTATION_NOW: NO / PLANNING_TASK_ONLY
ALLOW_STAGE_QDR_8_IMPLEMENTATION: YES / NEXT_TASK_ONLY
ALLOW_API_CHANGE_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_NQ_RUNTIME: NO
ALLOW_AGENT: NO
ALLOW_LANGGRAPH: NO
ALLOW_PAPER: NO
ALLOW_LIVE: NO
```

本计划只冻结 Stage-QDR-8 的方向、合同、安全边界和实施路线，不实施任何 Java、测试、API、migration、Repository、runtime wiring 或外部连接。Stage-QDR-7 B3 保持 `CLOSED / ACCEPTED`；B2 capacity gate 保持 `DEFERRED / KNOWN_LIMITATION`；Stage-QDR-8 不依赖 production capacity。

## 1. 规划输入与代码现实

### 1.1 Exact-SHA 前置门禁

修复提交 `8906389352d9d92099acdb857fce97aece3e6a20` 已 fast-forward 发布到 `origin/dev`。GitHub Actions run `29823413542` 的 head SHA 精确匹配该提交，test 与 Quality job 均成功：

```text
remote regression: PASS / 19 OF 19 REACTOR / 1161 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
PostgreSQL/Testcontainers: REAL EXECUTION / ZERO MANDATORY SKIPS
same-pool recovery: PASS / 4 TESTS / 0 FAILURES / 0 ERRORS / 0 SKIPPED
collector stress: PASS / 10 ROUNDS / 16 WRITERS / 1000 SAMPLES EACH
quality: PASS / 19 OF 19 REACTOR / CHECKSTYLE 0 / SPOTLESS PASS
```

因此：

```text
REMOTE_CI: PASS
ALLOW_STAGE_QDR_8_PLANNING: YES / CONSUMED_BY_THIS_PLAN
```

### 1.2 Feedback 现实

现有代码已经具备两条 feedback 基础：

1. `NqFeedbackEnvelope` / `NqFeedbackEvent`、8 种正式事件、HMAC/tenant/trace 校验、eventId 幂等、raw payload 保存与 JDBC/in-memory repository。
2. Stage1 `ExperienceFeedbackService` 会把粗粒度 `positive` 与 `FeedbackSource` 直接转换为 Experience/Pheromone 增强或衰减。

但现有基础不等于结构化归因：

- feedback 只有 `traceId` / run / candidate 关联，没有稳定的 `decisionId + traceId` 主关联合同。
- 没有 `OutcomeObservation`、`OutcomeSource`、归因维度、归因结果、置信度、归因状态、归因策略或审计引用模型。
- 现有 handler 把 payload verdict 映射为 boolean `positive`，并直接触发经验/信息素更新；该行为不能作为 Stage-QDR-8 的归因合同。
- raw payload 可以保存，但不能直接成为可信 outcome、归因解释或学习输入。
- 现有 ingestion 幂等只覆盖 eventId 接收，不覆盖 decision outcome observation 与 attribution result 的幂等冲突语义。

### 1.3 Evidence / replay 现实

现有 QDR 已具备 tenant-bound evidence aggregate、audit/trace、canonical replay snapshot、deterministic replay、regression verdict/finding 与内部 report 基础。Evidence/replay 不是空白区；Stage-QDR-8 只需让新的结构化 feedback attribution 能形成 safe ref 与可回放输入，不重建第二套 replay 系统。

### 1.4 Candidate / review 现实

现有 Stage1 已有：

```text
StrategyCandidate
CandidateGenerationService
CandidateReviewService
JudgeDecisionService
sourceAgent / searchPath / evidenceRefs / scoreSnapshot
deterministic in-memory/fake wiring
```

这些实现仍是轻量骨架，但候选、评审和 Judge 仲裁的基本链路已经存在。因此 Multi-Agent Candidate and Review Foundation 不是当前最小关键缺口。

### 1.5 Provider / Agent / LangGraph 现实

`ModelProviderPort` 只允许 deterministic `MockModelProvider`；代码未引入真实 provider SDK、outbound HTTP 或凭证读取。Agent 相关代码是 Stage1 in-memory/fake 骨架；LangGraph 仅存在于禁止性架构扫描和边界说明中，没有 runtime 实现。

## 2. 方向选择

### 2.1 选择方向

```text
Stage-QDR-8: STRUCTURED FEEDBACK ATTRIBUTION FOUNDATION
Exact implementation task ID: DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
```

选择理由：现有系统能够接收反馈、保存原始 payload、关联 trace 并进行 deterministic replay，但缺少把一次 dry-run decision 与结构化 outcome observation、可解释 attribution、confidence、audit/replay ref 安全连接起来的领域和用例合同。该缺口可以在纯内部、deterministic、无外部连接条件下关闭，不需要真实 Provider、NQ runtime 或 Agent runtime。

### 2.2 拒绝方向

| 方向 | 结论 | 原因 |
|---|---|---|
| Multi-Agent Candidate and Review Foundation | REJECTED_FOR_QDR_8 | 已有候选生成、评审、Judge 与 in-memory/fake 骨架；应在未来独立阶段增量演进，而不是覆盖当前 feedback attribution 缺口。 |
| Evidence / Replay Expansion | REJECTED_FOR_QDR_8 | evidence aggregate、canonical snapshot、deterministic replay 与 report 已存在；当前缺口是 feedback attribution 输入，而不是再建 replay 基础。 |
| Real Provider / NQ / Agent / LangGraph | FORBIDDEN | 超出当前安全边界，也不是结构化归因基础的必要条件。 |
| B2 capacity retry | FORBIDDEN | B2 capacity gate 已终局 `DEFERRED / KNOWN_LIMITATION`，不得重开。 |

## 3. 目标与非目标

### 3.1 目标

1. 为 dry-run decision 建立 tenant/environment-bound 的反馈主体合同。
2. 建立带明确 source、observation time、observation id 的结构化 outcome observation。
3. 建立可解释、可审计、可回放的 attribution result，所有维度和 reason code 均为稳定枚举/值对象。
4. 建立有界 confidence、fail-closed unknown handling 与明确状态机。
5. 建立与现有 audit/replay 的 safe reference 边界，不保存 raw prompt、raw provider response 或凭证材料。
6. 冻结幂等 key、canonical hash、重复响应与冲突拒绝语义。

### 3.2 非目标

```text
不自动更新模型
不自动修改 Prompt
不自动修改策略或候选状态
不自动重新执行 decision
不复用自然语言反馈直接改变历史状态
不调用现有 ExperienceFeedbackService.apply 形成自动强化
不接真实 NQ outcome
不形成在线学习闭环
不新增 API / Controller / OpenAPI / contracts / golden_cases
不新增 migration / JDBC / Repository adapter
不修改现有 dry-run runtime wiring
不接真实 HTTP / Provider / NQ / Agent / LangGraph / Paper / LIVE
```

## 4. 冻结领域合同

实现阶段默认在 `com.guidinglight.decisionhub.domain.qdr.feedback` 建立以下合同；不得复用已废弃的 `com.guidinglight.decisionhub.usecase.contract.DecisionOutcome`：

| 合同 | 冻结职责 |
|---|---|
| `FeedbackSubject` | 绑定 `tenantId`、`environment`、`decisionId`、`traceId`；四者均必填且不可变。 |
| `FeedbackEnvironment` | 仅允许 `DEV` / `TEST`；production/unknown fail-closed。 |
| `ObservedDecisionOutcome` | 仅表达 `CONFIRMED`、`CONTRADICTED`、`INCONCLUSIVE`、`NOT_OBSERVED`，不表达 BUY/SELL/交易动作。 |
| `OutcomeObservation` | 绑定 `observationId`、subject、outcome、source、`observedAt`、safe evidence refs 与 canonical input hash。 |
| `OutcomeSource` | 只允许 `DRY_RUN_RESULT`、`DETERMINISTIC_REPLAY`、`STRUCTURED_TEST_FIXTURE`；不包含真实 NQ runtime source。 |
| `AttributionDimension` | 固定维度：`EVIDENCE_QUALITY`、`POLICY`、`RISK_REVIEW`、`DATA_FRESHNESS`、`PROVIDER_SIGNAL`、`REPLAY_REPRODUCIBILITY`。 |
| `AttributionContribution` | 每个维度的结构化 impact、稳定 reason code、safe evidence refs 与有界 confidence。 |
| `AttributionResult` | subject + observation + contributions + aggregate confidence + status + audit reference + canonical hash。 |
| `FeedbackConfidence` | `0.0 <= value <= 1.0`；拒绝 NaN、Infinity 和越界值。 |
| `FeedbackStatus` | `VALIDATED`、`ATTRIBUTED`、`INCONCLUSIVE`、`REJECTED`；禁止任意字符串状态。 |
| `FeedbackPolicy` | 冻结允许 source、必需维度、最大 observation lag、unknown handling 与 `autoLearning=false`。 |
| `FeedbackAuditReference` | 只保存 tenant-bound safe ref/hash/version，不保存 raw payload、prompt、provider response 或敏感材料。 |

所有 enum 遇到未知值、所有主体关联不一致、所有 cross-tenant/cross-environment 输入、所有缺失 source/time/decisionId/traceId 的输入必须 fail-closed。

## 5. 冻结用例合同

实现阶段默认在 `com.guidinglight.decisionhub.usecase.qdr.feedback` 建立：

```text
FeedbackAttributionCommand
FeedbackAttributionService
FeedbackAttributionPolicyEvaluator
FeedbackAttributionIdempotencyPort
FeedbackAttributionAuditPort
FeedbackAttributionResult
FeedbackAttributionErrorCode
FeedbackCanonicalizer
```

用例执行顺序固定为：

```text
subject/environment validation
-> observation source/time validation
-> tenant/decision/trace correlation validation
-> canonicalize + hash
-> idempotency lookup
-> policy evaluation
-> deterministic attribution
-> audit reference write through port
-> immutable result
```

任一步失败均返回结构化 fail-closed 结果；不得吞异常、不得把 raw 外部错误返回调用方、不得继续调用后续步骤。

Stage-QDR-8 只冻结 ports 与 deterministic orchestration；不提供 production persistence adapter、API endpoint 或 Spring runtime wiring。实现测试可使用有界 deterministic fake port，但不得将 unbounded in-memory store 装入应用 runtime。

## 6. 幂等、审计与回放

### 6.1 幂等

```text
idempotency key = SHA-256(
  domain separator
  + tenantId
  + environment
  + decisionId
  + observationId
)
```

- 同 key + 同 canonical input hash：返回首个稳定结果，不重复写 audit。
- 同 key + 不同 canonical input hash：`IDEMPOTENCY_CONFLICT`，fail-closed。
- cross-tenant/cross-environment key 不得互相命中。
- 没有 idempotency port 或 port 失败：`IDEMPOTENCY_UNAVAILABLE`，不得继续归因。

### 6.2 审计

归因结果必须包含可追踪 `decisionId`、`traceId`、`observationId`、policy version、canonical hash、status、reason codes 与 safe refs。Audit port 失败时整体 fail-closed；不得只记日志后返回成功。

### 6.3 回放

Stage-QDR-8 只产生可被现有 canonical snapshot/evidence 体系消费的 immutable safe input；不实现第二套 replay executor。相同 canonical input + policy version 必须产生相同 attribution result 与 hash。不得在 replay 中调用 Provider、NQ、Agent、LangGraph 或外部时钟/随机数。

## 7. 安全合同

```text
tenant isolation: REQUIRED / FAIL_CLOSED
environment isolation: REQUIRED / DEV_TEST_ONLY
decisionId + traceId: REQUIRED / MUST_MATCH
outcome source: REQUIRED / CLOSED ENUM
observation time: REQUIRED / EXPLICIT UTC INSTANT
attribution explainability: REQUIRED / DIMENSION + REASON_CODE + SAFE_REFS
confidence: BOUNDED / 0.0..1.0
unknown value: FAIL_CLOSED
idempotency: REQUIRED
audit: REQUIRED / FAIL_CLOSED
replay: DETERMINISTIC / NO_SIDE_EFFECT
```

禁止：

```text
自然语言反馈直接改变历史状态
跨 tenant 或跨 environment 归因
缺失 source 时默认可信
unknown enum 退化为 permissive default
自动修改 Prompt、模型、策略、候选或 JudgeDecision
自动调用 ExperienceFeedbackService
自动触发 Provider、NQ、订单、风控、账务、Paper 或 LIVE
保存 raw prompt、raw provider response、credential 或完整敏感 payload
```

## 8. 批次冻结

### Batch 1 — Domain Contracts and Invariants

实现第 4 节领域合同、canonical validation 与 domain unit tests。不得新增 repository、API 或 wiring。普通 batch 不创建 standalone review。

### Batch 2 — Use-case Orchestration and Deterministic Internal Wiring

实现第 5/6 节用例、ports、deterministic policy/canonicalizer、幂等/审计 fail-closed orchestration 与 unit tests。只允许测试 fake；不得注册 production Spring bean，不得调用现有自动强化链。普通 batch 不创建 standalone review。

### Batch 3 — Persistence / API

```text
NOT_AUTHORIZED_BY_DEFAULT
```

只有代码现实证明 Batch 1/2 无法满足冻结验收，并另起 migration 或 API/Controller standalone review 后，才允许重新规划。当前 work order 不包含 Batch 3 写权限。

### Batch 4 — Full Regression, Quality, Evidence and Final Close

执行 domain/usecase 定向测试、完整 `mvn test`、quality、boundary scan、current factsource sync 与 stage final close。只有真实新的 P0/P1 安全、tenant、事务、migration 或 API 问题可阻断技术 acceptance。

## 9. 测试矩阵

| 类别 | 必测项 |
|---|---|
| 正常路径 | DEV/TEST subject、允许 source、完整 observation、deterministic attribution、audit success。 |
| 边界值 | confidence 0/1、observation lag 上限、空 optional refs、稳定排序与 hash。 |
| 非法输入 | null/blank、unknown enum、NaN/Infinity/越界 confidence、缺失 source/time/decisionId/traceId。 |
| 隔离 | cross-tenant、cross-environment、decisionId/traceId mismatch 全部拒绝。 |
| 幂等 | same key/same hash 稳定返回；same key/different hash 冲突；并发重复只有一个结果。 |
| 审计 | audit success、audit unavailable、audit conflict；失败不得返回 ATTRIBUTED。 |
| 回放 | 相同 input/policy 得到相同 result/hash；无 clock/random/provider/NQ/Agent/LangGraph。 |
| 无副作用 | Experience/Pheromone/Prompt/策略/候选/Judge 状态不变；HTTP/Provider/NQ/Paper/LIVE 调用为 0。 |
| 回归 | full reactor、PostgreSQL mandatory suites、B3 runtime contracts、quality。 |

## 10. Review 触发条件

只有以下条件创建 standalone review：

```text
migration / DDL / JDBC persistence
API / Controller / OpenAPI / contracts
security boundary change
stage final close
new P0 / P1 blocker
```

Batch 1/2 不分别创建 review；使用 implementation + tests + boundary scan + minimal docs 的普通批次收口。

## 11. 回滚与停止条件

- Batch 1/2 以独立、可审查 commit 推进；回滚优先 revert 对应 implementation commit，不改历史 migration。
- 任一越界需求触及 API、migration、Repository、真实外部连接或 production runtime 时立即停止，输出 scope blocker，不扩大 allowlist。
- 发现跨 tenant、unknown permissive default、audit fail-open、自动学习副作用或交易语义时立即阻断。
- Stage-QDR-8 final close 前不得标记实现 `CLOSED / ACCEPTED`，不得 archive/tag。

## 12. 完成与下一步

本计划完成判定：方向、合同、scope、安全边界、批次、测试、review 和回滚均已冻结；implementation 尚未开始。

下一精确任务：

```text
DH-STAGE-QDR-8-STRUCTURED-FEEDBACK-ATTRIBUTION-FOUNDATION-IMPLEMENTATION
```
