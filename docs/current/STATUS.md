# Decision Hub Status

> Current stage: stage-qdr-2 / Audit Trace Read Model + Human Approval Packet / B1_READMODEL_CONTRACT_DONE / PARTIAL_IMPLEMENTATION / NO_AGENT / NO_LIVE / NO_REAL_HTTP / NO_PROVIDER
> Next stage:    DH-STAGE-QDR-2-B2-READMODEL-REPOSITORY-AND-API / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED / READMODEL_REPOSITORY_AND_API
> AI trading execution: not allowed
> NQ core changes:      not allowed in this stage

## 1. 当前结论

DH 已经把"多模型调用平台"升级为"可进化的多 Agent 决策能力层"的最小骨架；
旧链路（domain.run.* / api.run.RunController / usecase.facade / usecase.run / usecase.gate /
usecase.contract / dh-providers）已全部 `@Deprecated`，REST 路径迁移到 `/legacy/runs` 子路径，
不再与新链路 `/api/ai/research-runs` 冲突。

文档单源已收敛到 `docs/current/`，与 `docs/codex/plans/_active/STATUS.json` 一致。

Codex workflow routing 已固化到 `nq-dh-workflow-router` 与 `docs/current/CODEX_*.md` / `docs/current/DH_CODEX_*.md` 文档。所有 DH / NQ 任务必须先分类，再选择插件、scope 和标准输出格式。

当前状态锁定：

```text
DH-AUDIT-FIX completed.
NQ integration not started.
Integration-0 safety gate CLOSED / ACCEPTED.
Integration-1 runtime implementation NOT STARTED.
Runtime integration NOT STARTED.
DH integrated NO.
AI / Agent runtime NOT STARTED.
RealClient forbidden.
real provider forbidden.
LIVE DISABLED.
NQ mutation forbidden.
DH Stage4 Decision Pipeline MVP PLAN: ACCEPTED / CLOSED.
DH Stage4 Decision Pipeline MVP WO: ACCEPTED / CLOSED.
K1 Contract Freeze Review: PASS / CLOSED / ACCEPTED.
M1 Readiness Review: CLOSED / ACCEPTED.
Current main line: stage-qdr-2 / Audit Trace Read Model + Human Approval Packet / B1_READMODEL_CONTRACT_DONE / PARTIAL_IMPLEMENTATION / NO_AGENT / NO_LIVE / NO_REAL_HTTP / NO_PROVIDER.
stage-qdr-1 implementation: DONE.
stage-qdr-1 freeze: CLOSED / ACCEPTED.
stage-qdr-2 Work Order: DONE / WORK_ORDER_READY.
stage-qdr-2 B1: DONE / IMPLEMENTED_BY_VALIDATION / READMODEL_DTO_QUERY_CONTRACT_ONLY.
stage-qdr-2 implementation overall: PARTIAL.
human_approval_packet: NOT STARTED.
approval API: NOT STARTED.
replay read API: NOT STARTED.
model gateway: NOT STARTED.
Mock baseline line: NQ-DH-I1-IMP0..IMP3 + MOCK-CLOSE-REVIEW / CLOSED / ACCEPTED / TEST_SUPPORT_ONLY / MOCK_ONLY / NO_RUNTIME.
Post-PR baseline: NQ dev contains mock/test-support baseline PR #12 merge commit 578eb65e; final read-only check shows current dev / origin/dev at b856cf07155de26f87fad9c21234c1a8a07b964a, with 578eb65e as ancestor.
NQ runtime client work order: CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_CLIENT_IMPLEMENTATION / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE.
NQ limited runtime client implementation: IMPLEMENTED / TARGETED_TEST_PASS / DEFAULT_DISABLED / FAKE_TRANSPORT_ONLY.
NQ limited runtime client close review: PASS / CLOSED / ACCEPTED / REVIEW_ONLY / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE.
Joint runtime dry-run test work order: CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_TEST_IMPLEMENTATION / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE.
Joint runtime dry-run test implementation: IMPLEMENTED / FULL_VALIDATION_PASS / TEST_ONLY / FAKE_TRANSPORT_ONLY / BLOCKER_FIX_APPLIED / CLOSED_BY_CLOSE_REVIEW.
Joint runtime dry-run test blockers: SIGNATURE_MATERIAL_SOURCE_NORMALIZATION_MISMATCH FIXED; SCHEMA_VERSION_MISMATCH FIXED.
Joint runtime dry-run test close review: PASS / CLOSED / ACCEPTED / REVIEW_ONLY / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE.
Integration-1 mock runtime milestone close review: PASS / CLOSED / ACCEPTED / REVIEW_ONLY / MOCK_RUNTIME_MILESTONE_CLOSED / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE.
Integration-1 mock runtime PR prep: READY / PR_PREP_ONLY / NQ_PR_CREATE_ALLOWED / NO_MERGE / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE.
Next concrete action: DH-STAGE-QDR-2-B2-READMODEL-REPOSITORY-AND-API / NOT STARTED / NO_APPROVAL_WRITE / NO_DB_MIGRATION.
K2 DecisionOrchestrator Skeleton: IMPLEMENTED.
K3 Audit / Snapshot / Trace Persistence: CLOSED / ACCEPTED after M1.
K4 Replay Read Model: CLOSED.
K5 Provider Health / Budget / Latency: CLOSED.
K6 Mock NQ Dry-run Contract Tests: CLOSED.
K7 Golden Cases / Eval: CLOSED.
K8 Acceptance / Freeze: CLOSED / ACCEPTED.
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED.
NQ current planning baseline: GateN.
```

## 1.0.30 DH-STAGE-QDR-2-B1 Read Model DTO / Query Contract（2026-07-06，DONE / IMPLEMENTED_BY_VALIDATION）

```text
Task: DH-STAGE-QDR-2-B1-READMODEL-QUERY-DESIGN-AND-DTO
Task type: CODE_CHANGE + TEST + DOCUMENTATION + READMODEL_DTO + QUERY_CONTRACT + SECURITY_BOUNDARY_PRESERVATION + NO_API_IMPLEMENTATION + NO_DB_MIGRATION + NO_APPROVAL_WRITE
stage-qdr-2 WO: DONE / WORK_ORDER_READY
stage-qdr-2 B1: DONE / IMPLEMENTED_BY_VALIDATION
stage-qdr-2 implementation overall: PARTIAL
Audit Trace Read Model DTO / projection: IMPLEMENTED
Tenant-bound query contract: IMPLEMENTED
Read repository / API: NOT STARTED
human_approval_packet: NOT STARTED
approval API: NOT STARTED
replay read API: NOT STARTED
model gateway: NOT STARTED
model_call: NOT STARTED
prompt version: NOT STARTED
tool registry: NOT STARTED
Real HTTP: NO
Real provider: NO
Agent / LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

B1 只新增 `dh-usecase` 的 read model DTO / projection、query record 与 query port contract，并补充 unit tests 与 architecture guard。查询 contract 必须 tenant-bound，read model 只读，不触发 replay execution、approval write、外部 HTTP、real provider、Agent runtime、LangGraph / AutoGen / CrewAI、NQ mutation 或 LIVE。

B1 未新增 Controller、OpenAPI path、API implementation、JDBC read repository、migration、`human_approval_packet`、approval API 或 replay API。B2 才允许在单独授权下实现 read repository / API；B3/B4 仍 `NOT STARTED`。

## 1.0.29 stage-qdr-2 Audit Trace Read Model + Human Approval Packet WO（2026-07-06，WORK_ORDER_READY）

```text
Task: DH-STAGE-QDR-2-AUDIT-TRACE-READMODEL-AND-HUMAN-APPROVAL-WO
Task type: WORK_ORDER_ONLY + STAGE_QDR_2_PLANNING + AUDIT_TRACE_READMODEL_DESIGN + HUMAN_APPROVAL_DESIGN + SECURITY_BOUNDARY_DESIGN + NO_CODE_CHANGE
Artifact: docs/current/DH_STAGE_QDR_2_WORK_ORDER.md
stage-qdr-1 implementation: DONE
stage-qdr-1 freeze: CLOSED / ACCEPTED
stage-qdr-2 WO: DONE / WORK_ORDER_READY
stage-qdr-2 implementation: NOT STARTED
Audit Trace Read Model: PLANNED / NOT IMPLEMENTED
human_approval_packet: PLANNED / NOT MIGRATED
approval API: PLANNED / NOT IMPLEMENTED
replay read API: PLANNED / NOT IMPLEMENTED
model_call: NOT STARTED
prompt version: NOT STARTED
tool registry: NOT STARTED
Real HTTP: NO
Real provider: NO
Agent / LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

本轮只编制 stage-qdr-2 Work Order，不实现功能。允许后续进入第一个小批次 `DH-STAGE-QDR-2-B1-READMODEL-QUERY-DESIGN-AND-DTO`；不允许一次性全量实现 stage-qdr-2，不允许新增 `human_approval_packet` 表、approval API、replay read API、model_call、prompt version、tool registry、真实 HTTP、real provider、LangGraph / AutoGen / CrewAI 或 LIVE。

stage-qdr-2 的设计范围是只读 Audit Trace Read Model 与 Human Approval Packet：查询必须 tenant-bound，approval 只能改变 DH 内部审批状态，不能触发 NQ mutation、order、cancel、risk mutation、ledger mutation、paper 或 live。`LONG_BIAS / SHORT_BIAS` 仍是只读方向性意见，不映射 `BUY / SELL`；`APPROVED` 不是交易授权。

## 1.0.28 stage-qdr-1 Quant Decision Review Core Baseline（2026-07-06，IN_PROGRESS）

```text
Task: DH-STAGE-QDR-1-FACTSOURCE-AND-DECISION-CORE-BASELINE
Stage: stage-qdr-1 = Quant Decision Review Core Baseline
Stage naming rule: DH current docs must use stage* naming; GateK / GateL / GateM must not be used for new DH current stages.
Task type: STAGE_IMPLEMENTATION_PREP + FACTSOURCE_REBASE + DECISION_CORE_BASELINE + MIGRATION_PLANNING + SECURITY_BOUNDARY_PRESERVATION + NO_AGENT + NO_LIVE
Current main line: Quant Decision Review
Limited Integration-1 dry-run endpoint: EXISTS / POST /api/ai/decision-dry-runs / DEFAULT_DISABLED / DEV_TEST_ONLY
NQ feedback endpoint: EXISTS / POST /api/ai/feedback/nq
V5 decision audit tables: EXISTS / dh_decision_context_snapshot + dh_decision_trace_step + dh_decision_provider_call_log + dh_decision_output
Decision Core baseline: IMPLEMENTING / decision_request + decision_run + quant_signal + quant_decision
Human approval packet: NOT IMPLEMENTED / RESERVED_FOR_stage-qdr-2
Replay read API: NOT IMPLEMENTED
model_call / prompt version / tool registry: NOT IMPLEMENTED
Agent / LangGraph runtime: NOT STARTED
Real provider: FORBIDDEN
Real HTTP: FORBIDDEN
LIVE: DISABLED
```

本轮事实源冻结以当前代码为准：DH 已存在 NQ feedback endpoint，已存在 limited Integration-1 dry-run endpoint，已存在 HMAC / timestamp / nonce replay / tenant-source binding / payload cap / memory cap / rate limit / kill switch 等 fail-closed 安全能力，已存在 V4 `dh_nq_replay_nonce` 与 V5 `dh_decision_*` 审计链路。历史文档中把 dry-run endpoint 写成未实现的描述属于滞后事实，不再作为当前状态。

当前缺口收口为 Quant Decision Review 主线：`decision_request`、`decision_run`、`quant_signal`、`quant_decision` 为本阶段最小 Decision Core；`human_approval_packet`、approval API、replay read API、model_call、prompt version、tool registry、真实 provider、LangGraph / multi-agent runtime 均不在本轮实现范围。`LONG_BIAS / SHORT_BIAS` 仅表示方向性审查意见，不得映射为 `BUY / SELL` 或任何 order / execution / risk / ledger / paper / live mutation。

GateK naming deprecated for DH project docs；后续 DH current stage 必须使用 stage* 命名。本轮选用 `stage-qdr-1`，因为当前 QDR 主线采用 stage slug 命名，不新建 GateK / GateL / GateM 文档或状态。

## 1.0.27 NQ-DH I1 Mock Runtime PR Prep（2026-07-05，READY / PR_PREP_ONLY）

```text
Task: NQ-DH-I1-MOCK-RUNTIME-PR-PREP
Task type: PR_PREP_ONLY + CROSS_REPO_DIFF_REVIEW + MILESTONE_MERGE_BOUNDARY_REVIEW + SECURITY_BOUNDARY_RECHECK + NO_CODE_CHANGE + NO_REAL_DH_CALL + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE
Result: READY / PR_PREP_ONLY / NQ_PR_CREATE_ALLOWED / NO_MERGE
Runtime integration: NOT STARTED
DH integrated: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_NQ_MOCK_RUNTIME_PR_CREATE: YES
ALLOW_NQ_MOCK_RUNTIME_PR_MERGE_NOW: NO
ALLOW_REAL_DH_CALL_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_DH_CODE_CHANGE_NOW: NO
ALLOW_NQ_CODE_CHANGE_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本轮只准备 NQ worktree `nq-dh-i1-joint-runtime-dryrun-test-impl` 到 NQ `origin/dev` 的 PR 材料。NQ PR diff 初始范围 `origin/dev...HEAD` 已分类为 allowed isolated `integration/dh` package、allowed tests、disabled-by-default config 与 docs/current；forbidden migration、contracts、golden_cases、frontend、research、scripts、deploy、`.github` 与 uncategorized diff 均为空。NQ PR title 固定为 `test(nq-dh): add Integration-1 mock dry-run runtime boundary`。DH dev 本轮只做 PR companion status 同步，不改 DH Java、测试、contracts、golden_cases 或 migration。

安全边界复核通过：无真实 DH call、无真实 HTTP、无 provider、无 credential forwarding、无 order / execution / risk / ledger / account / paper / live side effect；`LONG_BIAS / SHORT_BIAS` 仍 bias-only，不映射为 `BUY / SELL`。DH HMAC wire-level source fix、DH endpoint close review、joint runtime dry-run test close review 与 mock runtime milestone close review 已提交；NQ `DEFAULT_SCHEMA_VERSION=1.0.0` 与 DH endpoint response `schemaVersion=1.0.0` 一致。contracts/OpenAPI/json-schema/golden_cases 未 formalize，本 PR 不声称 formalized。

本轮未重跑 Maven：`Maven：未重跑；沿用 mock runtime close review 前一轮已记录结果。` NQ `quality` profile missing / not effective quality gate，不得写成 quality PASS。NQ dev 最终只读复核为 `## dev...origin/dev`，NQ-DH / Integration-1 scoped diff 为空；本轮未修改 NQ dev。下一步只允许 `NQ-DH-I1-MOCK-RUNTIME-PR-CREATE / PR_CREATE_ONLY`；不允许 merge、real DH call、real HTTP、provider、schema/contracts/golden_cases formalization、DH code change、NQ code change、Agent / LangGraph 或 LIVE。

## 1.0.26 NQ-DH I1 Integration-1 Mock Runtime Close Review（2026-07-05，PASS / CLOSED / ACCEPTED）

```text
Task: NQ-DH-I1-INTEGRATION1-MOCK-RUNTIME-CLOSE-REVIEW
Task type: REVIEW_ONLY + MILESTONE_CLOSE_REVIEW + WORKSTREAM_DISCIPLINE_RESET + CROSS_REPO_SECURITY_BOUNDARY_REVIEW + NO_CODE_CHANGE + NO_REAL_DH_CALL + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE
Result: PASS / CLOSED / ACCEPTED / REVIEW_ONLY / MOCK_RUNTIME_MILESTONE_CLOSED
Runtime integration: NOT STARTED
DH integrated: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_INTEGRATION1_MOCK_RUNTIME_CLOSE: YES
ALLOW_MOCK_RUNTIME_PR_PREP: YES
ALLOW_REAL_DH_CALL_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_DH_PRODUCTION_CODE_CHANGE_NOW: NO
ALLOW_NQ_PRODUCTION_CODE_CHANGE_NOW: NO
ALLOW_DH_TEST_CODE_CHANGE_NOW: NO
ALLOW_NQ_TEST_CODE_CHANGE_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本轮只做 mock runtime / test-only 里程碑关闭审查和工作纪律复位。审查确认 phase chain 已完成并提交；上一轮 close review 为 docs-only commit；blocker fix 为独立 test / code alignment commit；本轮写入前 DH dev 与 NQ worktree 均无未提交 close-review docs；NQ dev 只读 status 显示 unrelated backend untracked 文件，但 NQ-DH / Integration-1 scoped unstaged 与 staged diff 为空。当前证据仍限定为 fake transport / in-memory / MockMvc / test-only validation；不得解释为 real runtime integration started、DH integrated、NQ integrated、LIVE ready、production ready 或 real HTTP ready。

安全边界复核通过：HMAC source wire value 已对齐，`source=NQ_DRYRUN` 不被 lowercase / alias / fallback 重写，source allowlist 与 tenant/source pair 在验签后 exact match，lowercase / alias source denied，signature material mismatch 返回 `SIGNATURE_INVALID`；NQ `DEFAULT_SCHEMA_VERSION=1.0.0` 对齐 DH endpoint 实际 response `schemaVersion=1.0.0`，invalid schemaVersion 仍 fail-closed；`BUY / SELL / PLACE_ORDER / CANCEL_ORDER` fail-closed，`LONG_BIAS / SHORT_BIAS` 仅 bias-only，不映射为 `BUY / SELL`。

本 review 未重跑 Maven，沿用并接受上一轮记录的测试证据。NQ backend full、Integration0 scoped、Integration1 scoped 与 dry-run targeted tests 均已记录 `BUILD SUCCESS`；NQ `-Pquality validate` 因 `quality` profile missing，不是有效 quality gate，不得写成 quality PASS。DH `dh-api`、`dh-usecase` 与 `-Pquality validate` 已记录 `BUILD SUCCESS`。

下一步只允许 `NQ-DH-I1-MOCK-RUNTIME-PR-PREP / PR_PREP_ONLY`；不得新增 implementation WO，不得真实调用 DH，不得真实 HTTP，不得接 provider，不得 schema / contracts / golden_cases formalization，不得进入 Agent / LangGraph 或 LIVE。

## 1.0.25 NQ-DH I1 Joint Runtime Dry-run Test Close Review（2026-07-05，PASS / CLOSED / ACCEPTED）

```text
Task: NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-CLOSE-REVIEW
Task type: REVIEW_ONLY + JOINT_RUNTIME_DRYRUN_TEST_SECURITY_REVIEW + CROSS_REPO_CONTRACT_ALIGNMENT_REVIEW + NO_CODE_CHANGE + NO_REAL_DH_CALL + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE
Result: PASS / CLOSED / ACCEPTED / REVIEW_ONLY
Blocker 1: SIGNATURE_MATERIAL_SOURCE_NORMALIZATION_MISMATCH / FIXED / CLOSED
Blocker 2: SCHEMA_VERSION_MISMATCH / FIXED / CLOSED
Runtime integration: NOT STARTED
DH integrated: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_CLOSE: YES
ALLOW_INTEGRATION1_MOCK_RUNTIME_CLOSE_REVIEW: YES
ALLOW_REAL_DH_CALL_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_DH_PRODUCTION_CODE_CHANGE_NOW: NO
ALLOW_NQ_PRODUCTION_CODE_CHANGE_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本轮只做 close review 文档收口，审查 DH / NQ joint runtime dry-run tests 与 blocker fix 是否可关闭。结论为通过：HMAC source wire-level material 对齐、schemaVersion `1.0.0` 对齐、fake transport / in-memory / MockMvc / test-only 边界、success path、fail-closed、no-side-effect、audit/trace/record 与 redaction 均满足关闭条件。NQ `-Pquality validate` 因 profile missing 不得写成 quality gate PASS。下一步只允许 `NQ-DH-I1-INTEGRATION1-MOCK-RUNTIME-CLOSE-REVIEW`。

## 1.0.24 NQ-DH I1 Joint Runtime Dry-run Test Blocker Fix（2026-07-05，IMPLEMENTED / FULL_VALIDATION_PASS / CLOSED_BY_CLOSE_REVIEW）

```text
Task: NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-BLOCKER-FIX
Task type: BLOCKER_FIX + CROSS_REPO_CONTRACT_ALIGNMENT + TEST_ONLY_OR_MINIMAL_RUNTIME_ALIGNMENT + NO_REAL_DH_CALL + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE
Blocker 1: SIGNATURE_MATERIAL_SOURCE_NORMALIZATION_MISMATCH / FIXED
Blocker 2: SCHEMA_VERSION_MISMATCH / FIXED
DH HMAC material source: wire-level canonical value
DH source allowlist: exact match after signature verification
DH tenant/source pair: wire source
lowercase / alias source: denied
signature material mismatch: SIGNATURE_INVALID
DH response schemaVersion: 1.0.0
Runtime integration: NOT STARTED
DH integrated: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_BLOCKER_FIX_CLOSE: YES
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_CLOSE_REVIEW: YES / PASS / CLOSED / ACCEPTED
ALLOW_REAL_DH_CALL_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_DH_PRODUCTION_CODE_CHANGE_NOW: YES / LIMITED_TO_HMAC_SOURCE_WIRE_VALUE_ALIGNMENT_ONLY
ALLOW_NQ_PRODUCTION_CODE_CHANGE_NOW: YES / LIMITED_TO_ISOLATED_CLIENT_SCHEMA_VERSION_ALIGNMENT_ONLY
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

本轮最小修改 DH `HmacNqDryRunAuthenticator` 与对应回归测试，使 HMAC signature material 使用 request wire-level source value；allowlist 与 tenant/source pair 校验在验签后使用 exact source。NQ worktree 对齐 `DEFAULT_SCHEMA_VERSION=1.0.0`，以 DH endpoint 实际返回值为 runtime test source of truth。DH `dh-api`、`dh-usecase` 与 `-Pquality validate` 均通过；NQ full backend、Integration0、Integration1、dry-run targeted tests 均通过，NQ `-Pquality validate` 为 profile missing / not effective quality gate。invalid signature、source denied、invalid schemaVersion、BUY / SELL / PLACE_ORDER / CANCEL_ORDER 均保持 fail-closed；contracts、golden_cases、migration 不修改。

## 1.0.23 NQ-DH I1 Joint Runtime Dry-run Test Implementation（2026-07-05，IMPLEMENTED / BLOCKER_FIX_APPLIED_BY_1.0.24）

```text
Task: NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-IMPLEMENTATION
Task type: JOINT_RUNTIME_DRYRUN_TEST_IMPLEMENTATION + TEST_ONLY + CROSS_REPO_FAKE_TRANSPORT_VALIDATION + NO_PRODUCTION_CODE_CHANGE + NO_REAL_DH_CALL + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE
NQ test scope: E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\integration\dh
DH test scope: dh-api/src/test/**, dh-security/src/test/**
Implementation state: IMPLEMENTED / TARGETED_TEST_PASS / TEST_ONLY / FAKE_TRANSPORT_ONLY
Close review state: CLOSED_BY_1.0.25
Blocker 1: SIGNATURE_MATERIAL_SOURCE_NORMALIZATION_MISMATCH / FIXED_BY_1.0.24
Blocker 2: SCHEMA_VERSION_MISMATCH / FIXED_BY_1.0.24
Real DH call: NO
Real HTTP: NO
Real provider: NO
Runtime integration: NOT STARTED
DH integrated: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_BLOCKER_FIX_CLOSE: YES
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_CLOSE_REVIEW: YES / PASS / CLOSED / ACCEPTED
ALLOW_REAL_DH_CALL_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_DH_PRODUCTION_CODE_CHANGE_NOW: YES / LIMITED_TO_HMAC_SOURCE_WIRE_VALUE_ALIGNMENT_ONLY
ALLOW_NQ_PRODUCTION_CODE_CHANGE_NOW: YES / LIMITED_TO_ISOLATED_CLIENT_SCHEMA_VERSION_ALIGNMENT_ONLY
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
Next concrete action: NQ-DH-I1-INTEGRATION1-MOCK-RUNTIME-CLOSE-REVIEW / REVIEW_ONLY
```

- DH side 新增/扩展 MockMvc 与 HMAC authenticator 回归：valid signed request、readonly envelope、`auditRef`、`replayRef`、`traceSummary`、canonical fail-closed errors、memory cap、provider disabled / timeout / budget、audit failure fail-closed，以及 uppercase-source NQ-style signature 被当前 DH verifier 拒绝的回归。
- NQ side 新增 fake transport / DH-style validator evidence 与 response policy 扩展：NQ signed dry-run request 生成、canonical header、UTC `Z` timestamp、nonce、`dryRun=true`、`source=NQ_DRYRUN`、forbiddenCapabilities、response `OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS` record-only / bias-only、forbidden trading/executable response fail-closed、error taxonomy fail-closed、record 脱敏。
- 该条记录 implementation 当轮发现的 test-close blocker；1.0.24 已将 DH HMAC source material 对齐为 wire-level value，并由 NQ worktree 将 response schemaVersion 对齐 DH endpoint 实际值 `1.0.0`；1.0.25 close review 已接受该实现与 blocker fix。完整 validation 已通过，仍不授权 real DH call、real HTTP、provider、Agent / LangGraph 或 LIVE。

## 1.0.22 NQ-DH I1 Joint Runtime Dry-run Test WO（2026-07-05，CLOSED / ACCEPTED / WORK_ORDER_ONLY）

```text
Task: NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-WO
Task type: WORK_ORDER_ONLY + JOINT_RUNTIME_DRYRUN_TEST_PLAN + CROSS_REPO_TEST_BOUNDARY_FREEZE + NO_TEST_IMPLEMENTATION + NO_REAL_DH_CALL + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE
NQ artifact: E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_JOINT_RUNTIME_DRYRUN_TEST_WO.md
DH artifact: docs/current/DH_NQ_INTEGRATION1_JOINT_RUNTIME_DRYRUN_TEST_WO.md
DH endpoint: POST /api/ai/decision-dry-runs
NQ limited client: CLOSED / ACCEPTED / DEFAULT_DISABLED / FAKE_TRANSPORT_ONLY
Joint test implementation: NOT STARTED
Real DH call: NO
Real HTTP: NO
Real provider: NO
Runtime integration: NOT STARTED
DH integrated: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_WO_CLOSE: YES
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_IMPLEMENTATION: NO
ALLOW_REAL_DH_CALL_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_DH_CODE_CHANGE_NOW: NO
ALLOW_NQ_PRODUCTION_CODE_CHANGE_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
Next concrete action: NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-IMPLEMENTATION / NOT STARTED / TEST_ONLY / FAKE_TRANSPORT_ONLY / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

- 本轮只写 joint runtime dry-run test implementation work order；未实现测试，未修改 Java 生产代码或测试代码，未改 contracts/OpenAPI/json-schema/golden_cases/fixture JSON/migration。
- Work order 冻结下一轮测试目标：NQ limited dry-run client -> fake / in-memory / MockMvc / test-only transport -> DH `POST /api/ai/decision-dry-runs` -> DH readonly decision envelope -> NQ response validation -> NQ record-only dry-run result。
- 下一轮只能使用 fake transport、in-memory adapter、MockMvc、test-only request/response vector 或 isolated test support module；禁止 real outbound HTTP、真实 DH 地址、localhost 真实服务、外网、provider、LIVE 或交易副作用。
- 成功矩阵覆盖 signed request、canonical `X-NQ-DH-*` header、UTC `Z` timestamp、nonce、`dryRun=true`、`source=NQ_DRYRUN`、`OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS` record-only / bias-only；失败矩阵覆盖 DH side security fail-closed 与 NQ side client/response fail-closed。
- `LONG_BIAS / SHORT_BIAS` 只能作为 bias，不得映射 `BUY / SELL`；DH error 不得转为 NQ trading signal；NQ client failure 不得触发交易行为。

## 1.0.21 NQ-DH I1 NQ Runtime Client Work Order（2026-07-04，CLOSED / ACCEPTED / WORK_ORDER_ONLY）

```text
Task: NQ-DH-I1-NQ-RUNTIME-CLIENT-WO
Task type: WORK_ORDER_ONLY + NQ_SCOPED_RUNTIME_CLIENT_IMPLEMENTATION_PLAN + CROSS_REPO_BOUNDARY_FREEZE + NO_CLIENT_IMPLEMENTATION + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE
NQ artifact: E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_NQ_RUNTIME_CLIENT_WO.md
DH endpoint: POST /api/ai/decision-dry-runs
Endpoint scope: DH-only inbound limited dry-run
NQ runtime client: NOT STARTED
Real outbound HTTP: NO
Real provider: NO
Runtime integration: NOT STARTED
DH integrated: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_NQ_RUNTIME_CLIENT_WO_CLOSE: YES
ALLOW_NQ_LIMITED_RUNTIME_CLIENT_IMPLEMENTATION_WO: YES
ALLOW_NQ_RUNTIME_CLIENT_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_DH_CODE_CHANGE_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
Next concrete action: NQ-DH-I1-NQ-LIMITED-RUNTIME-CLIENT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION / DEFAULT_DISABLED / DEV_TEST_ONLY / NO_LIVE
```

- 本轮只在 NQ integration worktree 写 work order，并对 DH `docs/current` 做最小状态同步；未改 DH Java、未改 DH contracts / OpenAPI / JSON Schema / golden_cases / fixture JSON / migration。
- NQ work order 冻结下一轮 limited runtime client 的 default disabled、dev/test only、production disabled、kill switch、fail-closed、request envelope、canonical `X-NQ-DH-*` header、HMAC value material、timeout/retry/idempotency、response validation、audit/logging redaction、error taxonomy、测试矩阵和回滚要求。
- `NQ_DRYRUN` 仍为 review-gated source，不进入 production allowlist；`LONG_BIAS / SHORT_BIAS` 只能作为 readonly bias，不得映射为 `BUY / SELL`。
- 下一步只允许用户单独授权 NQ limited runtime client implementation；不得把本 WO close 解读为 runtime integration started、DH integrated、real HTTP allowed、real provider allowed、Agent / LangGraph runtime started 或 LIVE enabled。

## 1.0.20 NQ-DH I1 DH Limited Runtime Endpoint Close Review（2026-07-04，CLOSED / ACCEPTED / REVIEW_ONLY）

```text
Task: NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW
Task type: REVIEW_ONLY + DH_RUNTIME_ENDPOINT_SECURITY_REVIEW + API_BOUNDARY_REVIEW + NO_CODE_CHANGE + NO_NQ_CHANGE + NO_LIVE
Artifact: docs/current/DH_NQ_INTEGRATION1_DH_ENDPOINT_CLOSE_REVIEW.md
Endpoint: POST /api/ai/decision-dry-runs
Endpoint state: IMPLEMENTED / DH_ONLY / DEFAULT_DISABLED / DEV_TEST_ENABLE_ONLY / PRODUCTION_DISABLED / CLOSE_REVIEW_ACCEPTED
Runtime integration: NOT STARTED
NQ runtime client: NOT STARTED
Real HTTP outbound: NO
Real provider: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_DH_LIMITED_RUNTIME_ENDPOINT_CLOSE: YES
ALLOW_NQ_RUNTIME_CLIENT_WO: YES
ALLOW_NQ_RUNTIME_CLIENT_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP_NOW: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_FORMALIZATION_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
Next concrete action at close-review time: NQ-DH-I1-NQ-RUNTIME-CLIENT-WO / CONSUMED_BY_1.0.21 / CLOSED / ACCEPTED / WORK_ORDER_ONLY
```

- Close review 逐项复核 API boundary、security gate、error taxonomy、audit / trace / replay、feature flag、no-side-effect boundary 与 test coverage；结论为 `PASS / CLOSED / ACCEPTED`。
- endpoint 只保持 DH inbound limited dry-run；未新增或修改 Java 生产代码、测试代码、contracts/OpenAPI/json-schema/golden_cases/fixture JSON/migration，未修改 NQ dev 或 NQ dry-run worktree。
- `UNKNOWN_ERROR`、security error、provider disabled / timeout / budget exceeded、policy denied、audit write failure 均 fail-closed；error envelope 不回显 secret、signature、raw body、raw prompt、provider raw response、token、cookie 或 credential。
- `mvn -ntp test` 中 `PostgresContainerSmokeTest` 因 Docker named pipe 权限不可用由测试自身 skip；该 skip 不阻断 endpoint close，但不得写成 Docker/Testcontainers PASS。
- 下一步只允许进入 `NQ-DH-I1-NQ-RUNTIME-CLIENT-WO`，且该下一步仍是 work-order-only；不得直接进入 NQ runtime client implementation、真实 HTTP、real provider、schema/contracts/golden_cases 修改、Agent / LangGraph runtime 或 LIVE。

## 1.0.19 NQ-DH I1 DH Limited Runtime Endpoint Implementation（2026-07-04，IMPLEMENTED / PENDING_CLOSE_REVIEW / DH_ONLY）

```text
Task: NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION
Task type: DH_SCOPED_RUNTIME_ENDPOINT_IMPLEMENTATION + SECURITY_GATE_ENFORCEMENT + DRY_RUN_ONLY + NO_NQ_RUNTIME_CLIENT + NO_REAL_PROVIDER + NO_LIVE
Endpoint: POST /api/ai/decision-dry-runs
Endpoint state: IMPLEMENTED / DH_ONLY / DEFAULT_DISABLED / DEV_TEST_ENABLE_ONLY / PRODUCTION_DISABLED
NQ_DRYRUN source: DEV_TEST_ONLY / NOT_IN_PRODUCTION_ALLOWLIST
Runtime integration: NOT STARTED
NQ runtime client: NOT STARTED
Real HTTP outbound: NO
Real provider: NO
Agent / LangGraph runtime: NO
LIVE: DISABLED
ALLOW_DH_LIMITED_RUNTIME_ENDPOINT_IMPLEMENTATION_CLOSE: YES
ALLOW_DH_ENDPOINT_CLOSE_REVIEW: YES
ALLOW_NQ_RUNTIME_CLIENT_WO: NO
ALLOW_NQ_RUNTIME_CLIENT_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_CHANGE_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
Next concrete action: NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW / NOT STARTED / REVIEW_ONLY / NO_NQ_RUNTIME_CLIENT / NO_REAL_PROVIDER / NO_LIVE
```

- 本轮只实现 DH 侧 limited dry-run inbound endpoint；未修改 NQ dev 或 NQ dry-run worktree，未新增 NQ runtime client，未调用 NQ，未新增真实 outbound HTTP client、RealClient、real provider、Agent / LangGraph runtime 或 LIVE。
- endpoint 默认关闭；dev/test profile 可显式启用 `NQ_DRYRUN`，production profile disabled、kill switch fail-closed，且 `NQ_DRYRUN` 不进入 production allowlist。
- request gate 覆盖 HMAC signature、canonical `X-NQ-DH-*` headers、UTC `Z` timestamp、±300s window、nonce replay、tenant/source pair allowlist、requestId / traceId / tenantId binding、payload cap、rate limit、memory cap、forbidden material、policy gate 与 audit fail-closed。
- response 只返回 read-only decision snapshot；action 仅允许 `OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`。内部 `ABSTAIN` 对外映射为 `NO_TRADE`，并记录 `INTERNAL_ABSTAIN_MAPPED` reason；不输出 `BUY / SELL / PLACE_ORDER / CANCEL_ORDER`、quantity、leverage 或 order instruction。
- 本轮未修改 `contracts/openapi.yaml`、`contracts/json-schema/**`、`golden_cases/**`、fixture JSON 或 migration。OpenAPI/schema 兼容性与是否提升为正式 wire contract 必须由后续 close review 或独立 schema review 决定。

## 1.0.18 NQ-DH I1 DH Runtime API WO（2026-07-04，CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION）

```text
Task: NQ-DH-I1-DH-RUNTIME-API-WO
Task type: WORK_ORDER_ONLY + DH_SCOPED_RUNTIME_API_IMPLEMENTATION_PLAN + SECURITY_BOUNDARY_FREEZE + NO_RUNTIME_IMPLEMENTATION + NO_LIVE
DH artifact: docs/current/DH_NQ_INTEGRATION1_DH_RUNTIME_API_WO.md
Future endpoint candidate: POST /api/ai/decision-dry-runs
Endpoint state at WO close: NOT IMPLEMENTED
NQ_DRYRUN source: REVIEW_GATED / NOT_IN_PRODUCTION_ALLOWLIST
ALLOW_DH_RUNTIME_API_WO_CLOSE: YES
ALLOW_DH_LIMITED_RUNTIME_ENDPOINT_IMPLEMENTATION_WO: YES
ALLOW_DH_RUNTIME_IMPLEMENTATION_NOW: NO
ALLOW_NQ_RUNTIME_CLIENT_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_CHANGE_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION_NOW: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
Next concrete action at WO close: NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION / FEATURE_FLAG_DISABLED_BY_DEFAULT / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

- 本轮只写 DH limited dry-run runtime API implementation work order；未新增 Controller、API implementation、Client、Service、Repository、migration、schema、contracts、golden_cases、fixture JSON、真实 HTTP、real provider、AI / LangGraph 或 LIVE。
- Work order 冻结 future endpoint `POST /api/ai/decision-dry-runs`；WO 关闭时 endpoint 仍为 `NOT IMPLEMENTED`，后续实现状态以 §1.0.19 为准。
- 下一轮 implementation 必须默认 feature flag disabled、dev/test only、production disabled，强制 HMAC、UTC `Z` timestamp、±300s replay window、persistent nonce replay guard、tenant/source allowlist、requestId / traceId / tenantId binding、payload cap、rate limit、memory cap、fail-closed、redacted audit logging 和 kill switch。
- `NQ_DRYRUN` 当前仍为 review-gated source，不得直接进入 production allowlist；如下一轮允许，只能先限 dev/test profile，并要求 tenant + source pair allowlist。
- request envelope 仅允许 read-only dry-run 字段；禁止 credential、apiKey、apiSecret、passphrase、accountSecret、executableOrder、BUY/SELL order instruction、quantity/leverage/order price executable instruction。
- response envelope 只能是 read-only decision snapshot；`LONG_BIAS / SHORT_BIAS` 只是 bias，不是 `BUY / SELL`；NQ 只能记录，不执行。
- error taxonomy 冻结为 `SIGNATURE_INVALID / TIMESTAMP_INVALID / TIMESTAMP_OUT_OF_WINDOW / NONCE_REPLAY / TENANT_MISMATCH / SOURCE_DENIED / PAYLOAD_TOO_LARGE / RATE_LIMITED / MEMORY_LIMIT_EXCEEDED / POLICY_DENIED / PROVIDER_DISABLED / PROVIDER_TIMEOUT / BUDGET_EXCEEDED / UNKNOWN_ERROR`；unknown、security、provider unavailable、policy denied 均 fail-closed。
- 后续拆分固定为 DH limited endpoint implementation、DH runtime endpoint tests、NQ limited dry-run client work order、NQ client implementation、joint runtime dry-run tests、runtime close review；不得把 DH endpoint 和 NQ client 合并为一个大实现任务。

## 1.0.17 NQ-DH I1 Runtime API Contract Review（2026-07-04，CLOSED / ACCEPTED / REVIEW_ONLY / NO_RUNTIME）

```text
Task: NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW
Task type: REVIEW_ONLY + RUNTIME_API_CONTRACT_SECURITY_REVIEW + CROSS_REPO_BOUNDARY_REVIEW + POST_PR_MERGE_BASELINE + NO_RUNTIME_IMPLEMENTATION + NO_LIVE
DH artifact: docs/current/DH_NQ_INTEGRATION1_RUNTIME_API_CONTRACT_REVIEW.md
NQ artifact: E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_RUNTIME_API_CONTRACT_REVIEW.md
NQ dev post-PR baseline: PR #12 merge commit 578eb65e is present as ancestor of current dev / origin/dev b856cf07155de26f87fad9c21234c1a8a07b964a
Recommended option: Option D / freeze API contract, error taxonomy, envelope before split implementation
ALLOW_RUNTIME_API_CONTRACT_REVIEW_CLOSE: YES
ALLOW_DH_RUNTIME_API_WO: YES
ALLOW_NQ_RUNTIME_CLIENT_WO: YES
ALLOW_RUNTIME_IMPLEMENTATION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_API_CONTROLLER_CHANGE_NOW: NO
ALLOW_SCHEMA_CHANGE_NOW: NO
ALLOW_CONTRACTS_MODIFICATION_NOW: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
Next concrete action: NQ-DH-I1-DH-RUNTIME-API-WO / NOT STARTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION
```

- 本轮只做 runtime API / contract / security review；final read-only check 显示当前 NQ dev / origin/dev 为 `b856cf07155de26f87fad9c21234c1a8a07b964a`，PR #12 merge commit `578eb65e` 是其 ancestor，且 NQ-DH / Integration-1 scoped diff 为空；未写生产代码、测试代码、API、Controller、Client、migration、schema、contracts、golden_cases、fixture JSON、runtime wiring、真实 HTTP、real provider、AI / LangGraph 或 LIVE。
- 推荐 future DH endpoint 形态为 `POST /api/ai/decision-dry-runs`，但当前仍是 `NOT IMPLEMENTED`，不得写入已实现 API。future request 必须 signed / timestamped / nonce / tenant-bound，future response 只能是 readonly `DecisionOutput` envelope。
- `NQ_DRYRUN` 当前仍为 review-gated source，不允许本轮进入 production allowlist；进入 allowlist 前必须冻结 tenant/source pair allowlist、profile isolation、persistent nonce、rate limit、payload cap、memory cap、HMAC、schema version、kill switch 和 logging redaction。
- canonical error taxonomy 必须先冻结为正式 enum / contract；unknown / provider timeout / budget exceeded / policy denied / source denied / signature invalid / nonce replay / tenant mismatch 均必须 fail-closed，NQ 只记录不执行。
- `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 当前仍是 `DOC_ONLY_ALIAS` 或 future envelope planning；本 review 允许后续合同将其提升为正式字段，但不允许本轮改 schema/contracts/golden_cases。
- HMAC signature material 必须在 implementation 前正式冻结；推荐 material 包含 method、path、source、tenantId、requestId、traceId、timestamp、nonce、schemaVersion 与 canonical body hash。
- 后续必须拆成 DH runtime API WO、DH endpoint implementation、NQ limited client WO、NQ client implementation、joint runtime tests 和 runtime close review；不得合并成一个大实现任务。

## 1.0.16 NQ-DH I1 Limited Dry-run Runtime Plan（2026-07-04，CLOSED / ACCEPTED / PLAN_ONLY / NOT_IMPLEMENTED / NO_RUNTIME）

```text
Task: NQ-DH-I1-LIMITED-DRYRUN-RUNTIME-PLAN
Task type: PLAN_ONLY + LIMITED_DRYRUN_RUNTIME_PLANNING + CROSS_REPO_RUNTIME_BOUNDARY_DESIGN + API_CONTRACT_REVIEW_PREP + NO_RUNTIME_IMPLEMENTATION + NO_LIVE
DH artifact: docs/current/DH_NQ_INTEGRATION1_LIMITED_DRYRUN_RUNTIME_PLAN.md
NQ artifact: E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_LIMITED_DRYRUN_RUNTIME_PLAN.md
Mock close review evidence: CLOSED / ACCEPTED / REVIEW_ONLY / NO_RUNTIME
NQ dev scoped NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
ALLOW_LIMITED_DRYRUN_RUNTIME_PLAN_CLOSE: YES
ALLOW_RUNTIME_IMPLEMENTATION: NO
ALLOW_RUNTIME_API_CONTRACT_REVIEW: YES
ALLOW_MOCK_BASELINE_PR_PREP: YES
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
Next concrete action at plan-close time: NQ-DH-I1-MOCK-BASELINE-PR-PREP / CONSUMED_BY_POST_PR_BASELINE / CLOSED / MERGED / NO_RUNTIME
```

- 本轮只生成 limited dry-run runtime planning 文档；未写生产代码、测试代码、API、Controller、Client、migration、schema、contracts、golden_cases、fixture JSON、runtime wiring、真实 HTTP、real provider、AI / LangGraph 或 LIVE。
- `NQ_DRYRUN` 在该 plan close 当时仍为 review-gated test-support source；canonical error enum / error schema 未正式冻结；dry-run endpoint / API / Controller 当时不存在；schema alias 仍为 `DOC_ONLY_ALIAS` / future envelope planning；NQ 侧没有 runtime DH client。
- 该 plan 的推荐方向已被后续 mock baseline PR、runtime API contract review、DH endpoint work order / implementation / close review 和 `NQ-DH-I1-NQ-RUNTIME-CLIENT-WO` 消费；当前不得把该历史推荐误读为 current next。

## 1.0.15 NQ-DH I1-IMP3 Joint Mock Contract Tests（2026-07-04，IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_MOCK_CLOSE_REVIEW）

```text
Task: NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS
Task type: CONTROLLED_IMPLEMENTATION + JOINT_MOCK_CONTRACT_TESTS + TEST_RESOURCE_FIXTURES + NO_SIDE_EFFECT_GUARDS + MOCK_ONLY + NO_RUNTIME + NO_LIVE
DH fixture: dh-usecase/src/test/resources/nq-dh/integration1/joint_mock_contract_fixtures.json
DH test: dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/DhIntegration1JointMockContractFixtureTest.java
NQ fixture: E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\resources\nq-dh\integration1\joint_mock_contract_fixtures.json
NQ test: E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\app\integration1\NqDhIntegration1JointMockContractFixtureTest.java
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
API / Controller: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
ALLOW_IMP3_CLOSE: YES
ALLOW_I1_MOCK_CLOSE_REVIEW: YES
Next concrete action: NQ-DH-I1-MOCK-CLOSE-REVIEW / NOT STARTED / REVIEW_ONLY / NO_RUNTIME
```

- 本轮只在 DH 与 NQ dry-run worktree 测试范围新增 joint mock fixture / contract tests。request family 覆盖 valid read-only recommendation、missing / invalid signature、timestamp skew、nonce replay、source denied、tenant mismatch、forbidden credential / order-account / execution intent；response family 覆盖 abstain、observe、no-trade、long / short bias、provider timeout、risk blocked、internal fail-closed。
- Fixture family 名称双仓一致，fixture 文件分别归属各自 `src/test/resources/nq-dh/integration1/**`；当前不要求 hash 相同，但字段语义保持一致。Fixture 不包含真实 URL、credential、BUY / SELL、quantity / price / leverage、placeOrder / cancelOrder、paper/live start 或 mutation 字段；forbidden request 用测试侧 synthetic mutation 触发 fail-closed。
- 本轮未修改 DH / NQ `src/main`、`contracts/**`、`golden_cases/**`、OpenAPI、Controller、migration、runtime wiring、provider、RealClient、真实 HTTP、AI / LangGraph 或 LIVE。`NQ_DRYRUN` 仍是 review-gated，不进入 production allowlist；schema alias 仍不进入 current schema required/property。

## 1.0.14 NQ-DH I1-IMP2 NQ Stub Recorder No-side-effect（2026-07-04，VERIFY PASS / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_IMP3_JOINT_MOCK_CONTRACT_TESTS）

```text
Task: NQ-DH-I1-IMP2-NQ-STUB-RECORDER-NO-SIDE-EFFECT
Task type: CONTROLLED_IMPLEMENTATION + NQ_TEST_SUPPORT_STUB_RECORDER + NO_SIDE_EFFECT_GUARDS + WORKTREE_ONLY + MOCK_ONLY + NO_RUNTIME + NO_LIVE
NQ test support: E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\app\integration1\NqDhIntegration1StubRecorderNoSideEffectTest.java
DH code change: NONE
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
API / Controller: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
ALLOW_IMP2_CLOSE: YES
ALLOW_I1_IMP3_JOINT_MOCK_CONTRACT_TESTS: YES
Next concrete action consumed: NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS / IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_MOCK_CLOSE_REVIEW
```

- 本轮只在 NQ dry-run worktree 测试范围新增 stub / recorder / no-side-effect guard：覆盖 dry-run request builder 允许字段、forbidden execution / credential / HTTP shape、readonly recorder summary、`LONG_BIAS / SHORT_BIAS` 不映射 `BUY / SELL`、provider failure / high risk / no evidence / fail-closed / duplicate requestId record-only、生产路径无 `NQ_DRYRUN` / real dry-run client token。
- 本轮未改 DH 代码、DH contracts、DH golden_cases、DH fixture JSON、OpenAPI、Controller、migration 或 runtime wiring；DH current docs 仅同步 IMP2 状态与下一步。
- IMP2 不授权 `NQ_DRYRUN` 进入生产 source allowlist，不授权 dry-run endpoint、真实 HTTP client、RealClient、real provider、AI / LangGraph runtime、NQ mutation 或 LIVE。

## 1.0.13 NQ-DH I1-IMP1 DH Dry-run Test-support Entry（2026-07-03，IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_VALIDATION）

```text
Task: NQ-DH-I1-IMP1-DH-DRYRUN-TEST-SUPPORT-ENTRY
Task type: CONTROLLED_IMPLEMENTATION + DH_TEST_SUPPORT_ENTRY + CONTRACT_VALIDATION_CHAIN_TESTS + MOCK_ONLY + NO_RUNTIME + NO_LIVE
DH test support: dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/support/DhDryRunTestSupportEntry.java
DH tests: dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/DhDryRunTestSupportEntryTest.java
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
API / Controller: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
ALLOW_IMP1_CLOSE: YES
ALLOW_I1_IMP2_NQ_STUB_RECORDER_NO_SIDE_EFFECT: YES
Next concrete action consumed: NQ-DH-I1-IMP2-NQ-STUB-RECORDER-NO-SIDE-EFFECT / VERIFY PASS / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_IMP3_JOINT_MOCK_CONTRACT_TESTS
```

- 本轮只新增 DH 侧 test-support dry-run entry harness 与 validation chain 测试：payload size gate、canonical header、requestId / traceId / tenantId binding、source allowlist guard、UTC `Z` timestamp、nonce replay、value-based HMAC、schema/contract shape、forbidden fields、DecisionOrchestrator mock-only、provider guard、audit / trace / replay safe summary、structured `DecisionOutput` assembly 与 fail-closed normalization。
- `NQ_DRYRUN` 仍保持 review-gated，不进入生产 allowlist；`LONG_BIAS` / `SHORT_BIAS` 只作为 read-only bias，不映射为 BUY / SELL；unknown error 统一 fail-closed 到 `ABSTAIN`。
- 未修改 DH Java production code、NQ Java production code、schema/contracts/golden_cases/fixture JSON、OpenAPI、Controller、migration、runtime 配置、provider、RealClient、真实 HTTP、AI/LangGraph 或 LIVE。

## 1.0.12 NQ-DH I1-IMP0 Contract Gap Test-support Implementation（2026-07-03，IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_REVIEW）

```text
Task: NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION
Task type: CONTROLLED_IMPLEMENTATION + TEST_SUPPORT_ONLY + CONTRACT_GAP_GUARD_IMPLEMENTATION + MOCK_ONLY + NO_RUNTIME + NO_LIVE
DH test support: dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionContractGapGuardTest.java
NQ test support: F:\worktrees\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\app\integration1\NqDhIntegration1ContractGapGuardTest.java
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
ALLOW_IMP0_CLOSE: YES
ALLOW_I1_IMP1_DH_DRYRUN_TEST_SUPPORT_ENTRY: YES
Next concrete action: NQ-DH-I1-IMP1-DH-DRYRUN-TEST-SUPPORT-ENTRY / NOT STARTED / TEST_SUPPORT_ONLY / MOCK_ONLY
```

- 本轮只新增 DH / NQ 双侧 test-support guard：DH 侧防止 `NQ_DRYRUN`、dry-run runtime endpoint、DOC_ONLY_ALIAS、BUY/SELL/订单/账户/凭证字段和 canonical error names 提前进入当前生产契约；NQ dry-run worktree 侧验证 future `NQ_DRYRUN` source、request builder、summary-only recorder、read-only LONG/SHORT bias、no real URL / credential / HTTP / order / risk / ledger / Paper / LIVE side effect。
- 未修改 DH Java production code、NQ Java production code、schema/contracts/golden_cases/fixture JSON、OpenAPI、Controller、migration、runtime 配置、provider、RealClient、真实 HTTP、AI/LangGraph 或 LIVE。
- 验证已通过：DH 窄口 6 tests、NQ 新增窄口 5 tests、DH 全量 `mvn -ntp test`、DH `mvn -ntp -Pquality validate`、NQ worktree `mvn -ntp -f backend/pom.xml test`、NQ worktree Integration0 17 tests 均为 BUILD SUCCESS。

## 1.0.11 NQ-DH I1-M3 Joint Mock Fixtures And Contract Tests WO（2026-07-03，COMPLETED / WORK_ORDER_ONLY）

```text
Task: NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO
Task type: WORK_ORDER_ONLY + JOINT_MOCK_FIXTURE_TEST_PLANNING + CONTRACT_TEST_BATCH_DESIGN + FINAL_WO_BEFORE_IMPLEMENTATION + SECURITY_BOUNDARY + NO_RUNTIME + NO_LIVE
Artifact: docs/current/DH_NQ_INTEGRATION1_M3_JOINT_MOCK_FIXTURES_AND_CONTRACT_TESTS_WO.md
DH dev precheck: clean
NQ dry-run worktree precheck: clean
NQ dev precheck: NQ_MAINLINE_DIRTY_ALLOWED
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action: NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED
```

- 本轮只生成 joint mock fixtures and contract tests 的 M3 工作订单；不写 production code / test code，不创建 fixture JSON，不改 `contracts/**` 或 `golden_cases/**`，不新增 OpenAPI path / Controller / migration，不启动 runtime，不真实 HTTP，不接 real provider，不接 AI / Agent runtime / LangGraph / LIVE。
- M3 固化 23 类 future fixture family：valid dry-run request、valid readonly response、invalid/missing signature、timestamp skew、nonce replay、source denied、payload too large、rate limited、tenant mismatch、forbidden credential/order/account/trade fields、provider disabled/timeout/budget exceeded、risk blocked、no evidence fail-closed、internal fail-closed、long/short bias readonly、no real URL、no credential、no outbound。
- M3 固化 14 个 future contract test batch：DH validator shape、NQ request builder shape、NQ recorder no-side-effect、joint fixture parse、forbidden field fail-closed、source denied、UTC Z timestamp、HMAC material、tenant/requestId/traceId binding、error taxonomy、no-order/no-risk/no-ledger/no-paper/no-live scan、no real HTTP/no outbound、no credential logging/persistence、DH golden_cases compatibility smoke。
- `ALLOW_M3_WO_CLOSE: YES`；`ALLOW_I1_IMP0_CONTRACT_GAP_TEST_SUPPORT_IMPLEMENTATION: YES`；`ALLOW_MORE_PLANNING_WO: NO`；`ALLOW_I1_RUNTIME: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_SCHEMA_CHANGE: NO`；`ALLOW_CONTRACTS_MODIFICATION: NO`；`ALLOW_FIXTURE_IMPLEMENTATION: NO`；`ALLOW_GOLDEN_CASES_MODIFICATION: NO`；`ALLOW_API_CONTROLLER_CHANGE: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。
- 下一步只允许 `NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION`，且只允许 test-support / mock-only source handling、canonical error mapping test-support 与 fixture schema support guard；不得创建 M4/M5 大规划文档。

## 1.0.10 NQ-DH I1-M2 NQ Dry-run Stub Recorder WO（2026-07-03，COMPLETED / WORK_ORDER_ONLY）

```text
Task: NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO
Task type: WORK_ORDER_ONLY + NQ_DRYRUN_STUB_RECORDER_PLANNING + NO_SIDE_EFFECT_TEST_DESIGN + WORKTREE_ONLY + SECURITY_BOUNDARY + NO_RUNTIME + NO_LIVE
Artifact: docs/current/DH_NQ_INTEGRATION1_M2_NQ_DRYRUN_STUB_RECORDER_WO.md
DH dev precheck: clean
NQ dry-run worktree precheck: clean
NQ dev precheck: NQ-DH / Integration-1 scoped diff empty; whole-worktree zero-diff not asserted
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action after M2: NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO / COMPLETED / WORK_ORDER_ONLY / FINAL_WO_BEFORE_IMPLEMENTATION / NOT IMPLEMENTED
```

- 本轮只记录 NQ 侧 dry-run stub / request builder / recorder 的 M2 工作订单和 DH 只读依赖；不写 production code / test code，不创建 fixture JSON，不改 `contracts/**` 或 `golden_cases/**`，不新增 OpenAPI path / Controller / migration，不启动 runtime，不真实 HTTP，不接 real provider，不接 AI / Agent runtime / LangGraph / LIVE。
- M2 推荐 NQ stub 形态为 `test-support mock-only stub + in-memory recorder plan, no runtime HTTP client`；若未来需要真实 client，必须另起 API / contract / security / source allowlist / error taxonomy / no-side-effect / runtime isolation review。
- request builder 只允许 `requestId / traceId / tenantId / source / timestamp / nonce / signature / decisionType / subject / contextSnapshot / evidence summary` 等脱敏只读字段；`accountId / orderId / credential / BUY / SELL / quantity / price / leverage / placeOrder / cancelOrder / paperRunStart / liveRunStart` 等字段一律 forbidden 并 fail-closed。
- recorder 只记录只读 summary，不执行交易、不触发风控或账务修改、不启动 Paper Run / LIVE；`LONG_BIAS / SHORT_BIAS` 仍是只读 bias，不是 `BUY / SELL`。
- M2 只规划 no-side-effect 测试矩阵，不新增测试代码；未来必须覆盖 no-real-http、no-order、no-cancel-order、no-risk-mutation、no-ledger-mutation、no-paper-run-start、no-live、no-credential、forbidden field、idempotency、tenant mismatch、source denied、provider failure 和 bias-not-buy-sell。
- `ALLOW_M2_WO_CLOSE: YES`；`ALLOW_I1_M3_JOINT_MOCK_FIXTURES_AND_CONTRACT_TESTS_WO: YES`；`ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO`；`ALLOW_SCHEMA_CHANGE: NO`；`ALLOW_CONTRACTS_MODIFICATION: NO`；`ALLOW_FIXTURE_IMPLEMENTATION: NO`；`ALLOW_GOLDEN_CASES_MODIFICATION: NO`；`ALLOW_API_CONTROLLER_CHANGE: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0.9 NQ-DH I1-M1 DH Dry-run Contract Entry Mock WO（2026-07-03，COMPLETED / WORK_ORDER_ONLY）

```text
Task: NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO
Task type: WORK_ORDER_ONLY + DH_DRYRUN_ENTRY_PLANNING + CONTRACT_VALIDATION_CHAIN + SECURITY_CHAIN_DESIGN + TEST_SUPPORT_ONLY + NO_RUNTIME + NO_LIVE
Artifact: docs/current/DH_NQ_INTEGRATION1_M1_DH_DRYRUN_CONTRACT_ENTRY_MOCK_WO.md
DH dev precheck: clean
NQ dry-run worktree precheck: clean
NQ dev precheck: NQ_MAINLINE_DIRTY_ALLOWED
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action after M1: NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO / COMPLETED / WORK_ORDER_ONLY
```

- 本轮只生成 DH 侧 dry-run contract entry mock 的 M1 工作订单；不写 production code / test code，不创建 fixture JSON，不改 `contracts/**` 或 `golden_cases/**`，不新增 OpenAPI path / Controller / migration，不启动 runtime，不真实 HTTP，不接 real provider，不接 AI / Agent runtime / LangGraph / LIVE。
- M1 推荐入口形态为 `Option C / test-support mock-only / no runtime endpoint`；如未来需要真实 API / Controller，必须单独进入 API / contract / security review。
- Contract validation chain 固化为 payload size、canonical header、requestId / traceId / tenantId binding、source allowlist、RFC3339 UTC `Z` timestamp、nonce replay、HMAC value-based signatureMaterial、schema / forbidden fields、mock-only orchestrator、provider guard、audit / trace / replay、structured output、fail-closed normalization。
- `NQ_DRYRUN` source 仍为 `NEEDS_SECURITY_CONTRACT_CHANGE`，未通过 review 前只能作为 future source plan；`SOURCE_DENIED` 必须 fail-closed，source allowlist 不能被测试绕过。
- canonical error code names 尚未全部实现；M1 只规划 error normalization，不新增 enum / schema / code；所有错误路径 fail-closed，NQ 不得根据 error response 执行交易。
- `decisionId`、`replayRef`、`auditRef`、`traceSummary` 与 `X-NQ-DH-Schema-Version` 仍为 `DOC_ONLY_ALIAS` 或 future envelope planning，不得写入 required fixture 或 OpenAPI。
- `ALLOW_M1_WO_CLOSE: YES`；`ALLOW_I1_M2_NQ_DRYRUN_STUB_RECORDER_WO: YES / COMPLETED`；`ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO`；`ALLOW_SCHEMA_CHANGE: NO`；`ALLOW_CONTRACTS_MODIFICATION: NO`；`ALLOW_FIXTURE_IMPLEMENTATION: NO`；`ALLOW_GOLDEN_CASES_MODIFICATION: NO`；`ALLOW_API_CONTROLLER_CHANGE: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0.8 NQ-DH I1-M0 Contract Gap Close WO（2026-07-03，COMPLETED / WORK_ORDER_ONLY）

```text
Task: NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO
Task type: WORK_ORDER_ONLY + CONTRACT_GAP_CLOSE_PLANNING + SOURCE_ALLOWLIST_REVIEW + ERROR_TAXONOMY_REVIEW + DRYRUN_ENDPOINT_SHAPE_REVIEW + NO_RUNTIME + NO_LIVE
Artifact: docs/current/DH_NQ_INTEGRATION1_M0_CONTRACT_GAP_CLOSE_WO.md
DH dev precheck: clean
NQ dry-run worktree precheck: clean
NQ dev precheck: NQ-DH / Integration-1 scoped diff empty; whole-worktree zero-diff not asserted
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action: NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO / NOT STARTED
```

- 本轮只关闭 M0 contract gap work order；不写 production code / test code，不创建 fixture JSON，不改 `contracts/**` 或 `golden_cases/**`，不新增 OpenAPI path / Controller / migration，不启动 runtime，不真实 HTTP，不接 real provider，不接 AI / Agent runtime / LangGraph / LIVE。
- `NQ_DRYRUN` source 结论为 `NEEDS_SECURITY_CONTRACT_CHANGE`：现有 `source` 字段与 fail-closed source 校验机制可复用，但 `NQ_DRYRUN` 不是当前已实现 source 值，后续必须经安全合同 review 才能进入 allowlist。
- Error taxonomy 结论：`SIGNATURE_INVALID` / `PAYLOAD_TOO_LARGE` / `RATE_LIMITED` / `FORBIDDEN_FIELD` / `TENANT_MISMATCH` / `PROVIDER_DISABLED` / `PROVIDER_TIMEOUT` / `PROVIDER_BUDGET_EXCEEDED` / `RISK_BLOCKED` 为 `EXISTS_NOW`；`TIMESTAMP_SKEW` / `NONCE_REPLAY` / `SOURCE_DENIED` 仅为 `DOC_MAPPING_ONLY`；`AUTH_FAILED` / `CONTRACT_INVALID` / `INTERNAL_FAIL_CLOSED` 仍需 `NEEDS_CONTRACT_REVIEW_BEFORE_CODE`。
- Dry-run endpoint shape 结论为 `RECOMMENDED_SHAPE = Option C / test-support mock-only, no runtime endpoint`；新增 DH endpoint、复用 feedback ingest endpoint 或新增 header/schema/envelope 均必须另起 API / contract / security review。
- `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 继续保持 `DOC_ONLY_ALIAS` 或 future envelope planning，不进入 required fixture、OpenAPI、JSON Schema 或 Controller。
- `ALLOW_M0_WO_CLOSE: YES`；`ALLOW_I1_M1_DH_DRYRUN_CONTRACT_ENTRY_MOCK_WO: YES`；`ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO`；`ALLOW_SCHEMA_CHANGE: NO`；`ALLOW_CONTRACTS_MODIFICATION: NO`；`ALLOW_FIXTURE_IMPLEMENTATION: NO`；`ALLOW_GOLDEN_CASES_MODIFICATION: NO`；`ALLOW_API_CONTROLLER: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0.7 NQ-DH I1 Dry-run Mock Implementation WO（2026-07-03，COMPLETED / WORK_ORDER_ONLY）

```text
Task: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO
Task type: WORK_ORDER_ONLY + PRECHECK_RULE_FIX + DRYRUN_MOCK_IMPLEMENTATION_PLANNING + CROSS_REPO_BATCH_DESIGN + SECURITY_BOUNDARY + NO_RUNTIME + NO_LIVE
Artifact: docs/current/DH_NQ_INTEGRATION1_DRYRUN_MOCK_IMPLEMENTATION_WO.md
DH dev precheck: clean
NQ dry-run worktree precheck: clean
NQ dev precheck: NQ_MAINLINE_DIRTY_ALLOWED
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action after M0 close: NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO / NOT STARTED
```

- 本轮只产出 dry-run mock implementation work order，修正前置预检口径：NQ dev 仅当存在 `docs/current/*NQ_DH*` 或 `docs/current/*INTEGRATION1*` dirty diff 时阻断；marketdata / API / Gate 主线 dirty diff 记录为 `NQ_MAINLINE_DIRTY_ALLOWED`，不得覆盖或回滚。
- 工单拆分为 M0-M4：M0 contract gap close WO、M1 DH dry-run contract entry mock、M2 NQ dry-run stub recorder、M3 joint mock fixtures and contract tests、M4 close review。M0 已由 `docs/current/DH_NQ_INTEGRATION1_M0_CONTRACT_GAP_CLOSE_WO.md` 关闭；当前下一步唯一允许动作是 M1 work order / mock contract planning。
- 本轮不写 production code / test code，不创建 fixture JSON，不改 `contracts/**` 或 `golden_cases/**`，不新增 OpenAPI path / Controller / migration，不启动 runtime，不真实 HTTP，不接 real provider，不接 AI / Agent runtime / LangGraph / LIVE。
- `ALLOW_WORK_ORDER_CLOSE: YES`；`ALLOW_I1_M0_CONTRACT_GAP_CLOSE_WO: YES`；`ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE_THIS_TURN: NO`；`ALLOW_SCHEMA_CHANGE_THIS_TURN: NO`；`ALLOW_FIXTURE_JSON_THIS_TURN: NO`；`ALLOW_CONTRACTS_MODIFICATION_THIS_TURN: NO`；`ALLOW_GOLDEN_CASES_MODIFICATION_THIS_TURN: NO`；`ALLOW_API_CONTROLLER_THIS_TURN: NO`；`ALLOW_RUNTIME_THIS_TURN: NO`；`ALLOW_REAL_HTTP_THIS_TURN: NO`；`ALLOW_REAL_PROVIDER_THIS_TURN: NO`；`ALLOW_AI_AGENT_RUNTIME_THIS_TURN: NO`；`ALLOW_LANGGRAPH_RUNTIME_THIS_TURN: NO`；`ALLOW_LIVE_THIS_TURN: NO`。

## 1.0.6 NQ-DH I1-P4 Implementation Gate Review Fix（2026-07-03，COMPLETED / DOCS-ONLY）

```text
Task: NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX
Task type: DOCS_ONLY + GATE_REVIEW_FIX + SCHEMA_GAP_CONTRACT_REVIEW + CROSS_REPO_BOUNDARY_RECHECK + NO_RUNTIME + NO_LIVE
DH P3 commit status: CLEAN / SUBMITTED at 1fb16c20613411b42a2e9df3880d04d220fe1f36
NQ P3 commit status: CLEAN / SUBMITTED at 5c6bc895cb63e0990fd662c11cd3cfc6b7df2d05
P0/P1/P2/P3 docs: SUBMITTED / CLOSED-FOR-P4-GATE
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
```

- P4 fix 只关闭上一轮 gate review 的文档阻塞和 schema gap review 阻塞；不实施代码、不新增测试、不改 `contracts/**`、不改 `golden_cases/**`、不创建 fixture JSON、不新增 API path / Controller / migration。
- Contract review 结论：`DecisionAction` vocabulary 与固定 `ForbiddenAction` list 为 `EXISTS_NOW`；`dryRun`、`decisionId`、`confidence`、`traceSummary`、`replayRef`、`auditRef`、`X-NQ-DH-Schema-Version` 当前不进入 required fixture / schema / code，只能作为 `DOC_ONLY_ALIAS` 或 future envelope planning；`NQ_DRYRUN` source、canonical error code names 与 dry-run endpoint shape 只允许作为后续 WO 的 mock/test-support 约束，不得写成已实现 runtime。
- P4 fix 不要求 schema change；后续 WO 只能基于现有 `DecisionRequest` / `DecisionOutput` schema 字段、现有 `source` 字段、固定 action vocabulary、固定 forbiddenActions 与文档 alias 编写 mock/stub/test-support 计划。任何 wire-level gap 字段、schema header、真实 endpoint 或 canonical error enum 落地都必须另起 contract/schema review。
- `ALLOW_I1_P4_IMPLEMENTATION_GATE_REVIEW_FIX_CLOSE: YES`；`ALLOW_I1_P4_RETRY: YES`；`ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_WORK_ORDER: YES`；`ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO`；`ALLOW_SCHEMA_CHANGE: NO`；`ALLOW_FIXTURE_IMPLEMENTATION: NO`；`ALLOW_CONTRACTS_MODIFICATION: NO`；`ALLOW_GOLDEN_CASES_MODIFICATION: NO`；`ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0.5 NQ-DH I1-P3 Dry-run Implementation Readiness Plan（2026-07-03，COMPLETED / PLAN ONLY）

```text
Plan artifact: docs/current/DH_NQ_INTEGRATION1_DRYRUN_IMPLEMENTATION_READINESS_PLAN.md
Plan status: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
Merged tasks: I1-P3-NQ-DRYRUN-STUB-TEST-PLAN + I1-P4-DH-DRYRUN-ENTRY-PLAN + I1-P5-JOINT-MOCK-VALIDATION-PLAN
NQ current main line: GateO
NQ rebase input: GateN no-real public marketdata / exchange sandbox baseline
DH baseline: DH-STAGE4-DECISION-PIPELINE-MVP / ACCEPTED / CLOSED
NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
```

- 本轮只规划 NQ dry-run stub readiness、DH dry-run entry readiness、joint mock validation readiness、security boundary 和 P4 implementation gate checklist。
- 原 `I1-P3-NQ-DRYRUN-STUB-TEST-PLAN`、`I1-P4-DH-DRYRUN-ENTRY-PLAN`、`I1-P5-JOINT-MOCK-VALIDATION-PLAN` 已 `MERGED_INTO_NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN`；原 `I1-P6-IMPLEMENTATION-GATE-REVIEW` 已重新编号并由 `NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX / COMPLETED / DOCS-ONLY / GATE-FIX` 关闭。
- `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version / NQ_DRYRUN source` 仍为 review-gated gap，不得写成已实现。
- `ALLOW_I1_P3_DRYRUN_IMPLEMENTATION_READINESS_PLAN_CLOSE: YES`；`ALLOW_I1_P4_IMPLEMENTATION_GATE_REVIEW: YES`；`ALLOW_SCHEMA_CHANGE: NO`；`ALLOW_FIXTURE_IMPLEMENTATION: NO`；`ALLOW_CONTRACTS_MODIFICATION: NO`；`ALLOW_GOLDEN_CASES_MODIFICATION: NO`；`ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0.4 NQ-DH I1-P2 Contract Fixtures Plan（2026-07-02，COMPLETED / PLAN ONLY）

```text
Plan artifact: docs/current/DH_NQ_INTEGRATION1_CONTRACT_FIXTURES_PLAN.md
Plan status: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ current main line: GateO
NQ rebase input: GateN no-real public marketdata / exchange sandbox baseline
DH baseline: DH-STAGE4-DECISION-PIPELINE-MVP / ACCEPTED / CLOSED
NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action: NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN / COMPLETED / PLAN ONLY / NOT IMPLEMENTED
```

- 本轮只规划 future request/response fixtures、schema gap、golden case alignment、error taxonomy alignment、no-runtime/no-live fixture boundary 和 P3-P6 后续批次。
- `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 当前不属于已实现 wire schema；后续如需进入 fixture 或 runtime contract，必须单独 contract review。
- DH `golden_cases/decision/**` 仍是 DH 内部 deterministic baseline；NQ-DH fixture 是跨仓合同基线，两者不得混作同一种证据。
- `ALLOW_I1_P2_CONTRACT_FIXTURES_PLAN_CLOSE: YES`；`ALLOW_I1_P3_DRYRUN_IMPLEMENTATION_READINESS_PLAN: YES / COMPLETED / PLAN ONLY`；`ALLOW_I1_P4_IMPLEMENTATION_GATE_REVIEW: YES`；`ALLOW_SCHEMA_CHANGE: NO`；`ALLOW_FIXTURE_IMPLEMENTATION: NO`；`ALLOW_CONTRACTS_MODIFICATION: NO`；`ALLOW_GOLDEN_CASES_MODIFICATION: NO`；`ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0.3 NQ-DH I1-P1 Contract Dry-run Plan（2026-07-02，COMPLETED / PLAN ONLY）

```text
Plan artifact: docs/current/DH_NQ_INTEGRATION1_DRYRUN_CONTRACT_PLAN.md
Plan status: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ current main line: GateO
NQ rebase input: GateN no-real public marketdata / exchange sandbox baseline
DH baseline: DH-STAGE4-DECISION-PIPELINE-MVP / ACCEPTED / CLOSED
NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE: CLOSED / ACCEPTED / DOCS-ONLY
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
Next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
```

- 本轮只规划 NQ -> DH dry-run request、DH -> NQ dry-run response、canonical `X-NQ-DH-*` header、timestamp / nonce / HMAC、tenant / trace / requestId binding、error taxonomy、trace / audit / replay、后续批次和测试矩阵。
- `DecisionRequest` 当前 schema 已覆盖 `requestId / traceId / tenantId / source / decisionType / subject / contextSnapshot / requestedAt / schemaVersion`；`dryRun / runRef / 顶层 evidenceRefs` 属计划字段或映射问题，不在本轮改 schema。
- `DecisionOutput` 当前 schema 已覆盖 `requestId / traceId / tenantId / decisionType / action / status / riskLevel / policyStatus / providerStatus / forbiddenActions / reasonCodes / evidenceRefs / createdAt / schemaVersion`；`decisionId / confidence / replayRef / auditRef / dryRun` 属 response envelope 规划项，不写成已实现字段。
- `LONG_BIAS / SHORT_BIAS` 只能作为只读倾向，不是 `BUY / SELL`，不得进入 order、risk mutation、ledger、position、Paper Run、LIVE 或 private trading path。
- `ALLOW_I1_P1_CONTRACT_PLAN_CLOSE: YES`；`ALLOW_I1_P2_CONTRACT_FIXTURES_PLAN: YES`；`ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0.0 DH Stage4 命名 rebase（2026-07-02，CLOSED）

```text
DH-STAGE4-NAMING-REBASE-FIX: CLOSED
DH-STAGE4-DECISION-PIPELINE-MVP: ACCEPTED / CLOSED
Legacy DH-GATEK-DECISION-PIPELINE-MVP: SUPERSEDED / NAMING_REPLACED
Legacy docs/gates/dh-gatek-decision-pipeline-mvp: SUPERSEDED / NAMING_REPLACED
Correct freeze directory: docs/gates/dh-stage4-decision-pipeline-mvp/
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
P1 follow-up consumed: YES, by NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY.
Current next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
```

- 本轮只修正 DH 自身阶段命名、current docs 事实源引用、验收报告文件名和冻结目录名。
- `DH_GATEK_DECISION_PIPELINE_MVP_PLAN.md`、`DH_GATEK_DECISION_PIPELINE_MVP_WORK_ORDER.md`、`DH_GATEK_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md` 已重命名为 `DH_STAGE4_DECISION_PIPELINE_MVP_*`。
- `docs/gates/dh-gatek-decision-pipeline-mvp/` 已重命名为 `docs/gates/dh-stage4-decision-pipeline-mvp/`；目录 README 已说明原错误目录名、正确目录名与 `ACCEPTED / CLOSED` 事实不变。
- 后续 Integration-1 前置条件固定为 `NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED`，不得写为 `NQ GateN + DH GateK CLOSED`。
- 本轮未修改生产代码、测试代码、contracts、golden_cases、API、Controller、migration、runtime client、provider 或 NQ 仓库。

## 1.0.1 NQ-DH Integration-1 Dry-run Plan RebaseN（2026-07-02，PASS / PLAN ONLY）

```text
Plan artifact: docs/current/DH_NQ_INTEGRATION1_DRYRUN_PLAN_REBASEN.md
Plan status: PLAN BASELINE ACCEPTED / I1-P0 FACTSOURCE REBASE CLOSED
NQ baseline: GateN no-real public marketdata / exchange sandbox baseline frozen and tagged
DH baseline: DH Stage4 Decision Pipeline MVP CLOSED / ACCEPTED
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
Integration-1 dry-run implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NO
Real provider: NO
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
P1 follow-up consumed: YES, by NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY.
Current next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
```

- 本轮只做 NQ GateN rebase 后的 Integration-1 dry-run 规划，定义 dry-run 目标、NQ/DH 职责边界、request/response 合同规划、安全协议、I1-P0..P5 批次和测试矩阵。
- dry-run request 以现有 `DecisionRequest` 为基线规划 `source=NQ_DRYRUN`、`decisionType=READ_ONLY_RECOMMENDATION`、`subject`、`contextSnapshot/evidenceRefs`、`requestedAt`、`schemaVersion` 和 `dryRun=true` 语义；`dryRun` 属计划字段，当前 schema 尚未实现，后续如需 wire-level 扩展必须单独 contract review。
- dry-run response 以现有 `DecisionOutput` 为基线规划 `action in ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`、`riskLevel`、`policyStatus`、`reasonCodes` 和固定五项 `forbiddenActions`；`decisionId / confidence / replayRef / auditRef / dryRun` 属后续 response envelope 规划项，当前不写成已实现字段。
- NQ 只记录 DH 输出，不执行 DH 输出；DH 不下单、不改 NQ 状态、不读写 NQ DB、不接真实 exchange / broker。
- `ALLOW_INTEGRATION1_DRYRUN_PLAN_CLOSE: YES`；`ALLOW_NQ_DH_I1_P0_FACTSOURCE_REBASE: YES`；`ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0.2 NQ-DH I1-P0 Factsource Rebase Close（2026-07-02，CLOSED / ACCEPTED）

```text
NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE: CLOSED / ACCEPTED / DOCS-ONLY
Integration-1 dry-run plan baseline: ACCEPTED
Prerequisite: NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED
P1 follow-up consumed: YES, by NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY.
Current next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
Integration-1 implementation: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
Real HTTP: NOT STARTED
Real provider: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮只同步 NQ / DH 当前事实源，不新增 API、Controller、migration、production code、test code、contracts 或 golden_cases。
- NQ GateN 仅作为 Integration-1 dry-run rebase input；NQ 当前 GateO 主线不被 P0 回滚或覆盖。
- P1 仍是 contract dry-run plan，不是 Integration-1 implementation、runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。
- `ALLOW_I1_P0_CLOSE: YES`；`ALLOW_I1_P1_CONTRACT_PLAN: YES`；`ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_REAL_HTTP: NO`；`ALLOW_REAL_PROVIDER: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.0 DH Stage4 Decision Pipeline MVP K8 Acceptance / Freeze（2026-07-02，CLOSED / ACCEPTED）

```text
DH Stage4 Decision Pipeline MVP: CLOSED / ACCEPTED
K1 Contract Freeze: CLOSED
K2 Orchestrator Skeleton: CLOSED
K3 Audit / Snapshot / Trace Persistence: CLOSED
M1 Readiness Review: CLOSED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN: PASS / PLAN ONLY / READY FOR P0 FACTSOURCE REBASE
P1 follow-up consumed: YES, by NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY.
Current next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 验收报告：`docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md`。
- 冻结快照：`docs/gates/dh-stage4-decision-pipeline-mvp/`。
- K1-K7 已形成只读 Decision Pipeline MVP 闭环：合同冻结、mock-only orchestrator、audit/snapshot/trace persistence、internal replay read model、mock provider guard、mock NQ dry-run contract tests、golden cases/eval baseline。
- 本次 K8 只做 acceptance / freeze / regression validation / security boundary review / docs sync；未修改生产代码、测试代码、contracts、golden_cases、API、Controller、migration、runtime client、provider 或 NQ 仓库。
- 验收命令 `mvn -ntp -pl dh-domain,dh-usecase,dh-infra,dh-app -am test`、`mvn -ntp test`、`mvn -ntp -Pquality validate` 均 `BUILD SUCCESS`；当前 sandbox 下 Docker daemon 管道不可用，`PostgresContainerSmokeTest` 按 Testcontainers 机制 skipped 1。
- 关键词边界扫描未发现生产越界实现；命中均为禁止说明、negative tests、denylist、migration comment、historical/deferred docs 或固定 `forbiddenActions`。
- `ALLOW_STAGE4_CLOSE: YES`；`ALLOW_INTEGRATION1_DRYRUN_PLAN_REBASE_N: YES`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.1 NQ / DH 三轮审计同步（2026-06-11，DOC-SYNC-GATEK-PRE-AND-INT0-REGISTRATION）

本轮只做事实源文档同步，不修改代码，不启动 Integration-0 实现。

三轮只读审计已完成：

- 第一轮：NQ 全仓只读审计 completed。
- 第二轮：DH 全仓只读审计 completed。
- 第三轮：NQ-DH 联合边界审计 completed（`docs/current/NQ_DH_INTEGRATION_SECURITY_AUDIT_REPORT.md`）。
- 三轮审计汇总 completed。

DH 当前阶段口径：

```text
Current:         DH audit fixed / ready for Integration-0 planning
Next:            Integration-0-PLAN
Provider:        no real provider
NQ client:       no RealClient
Trading ability: none
Security baseline: P1-1 / P1-2 / P1-3 closed
Remaining issue: P1-4 residual rate limit / memory cap / replay nonce persistence
                 -> CLOSED 2026-06-13（见 §1.3）；关闭前阻塞 Integration-1，从不阻塞 Integration-0
```

NQ-DH 口径：

```text
Integration status: not integrated
Runtime connection: none
Contract status:    Integration-0 contract freeze allowed
Allowed work:       docs, contract freeze, mock, stub, contract test, security policy
Forbidden work:     real NQ connection, RealClient, real Provider, trading,
                    credential access, NQ DB access, LIVE
```

Integration-0 是 contract / mock / documentation 工作线，不是 runtime integration；NQ 侧仍无 DH 入站端点、无 DH client、无 feedback outbox，DH not integrated 成立。

## 1.2 NQ-DH Integration-0 safety gate（2026-06-12，CLOSED / ACCEPTED）

```text
Decision:             PASS
Integration-0 safety gate: CLOSED / ACCEPTED
Runtime integration:  NOT STARTED
Integration-1:        NOT STARTED
LIVE:                 DISABLED
AI:                   NOT STARTED
DH integration:       NOT INTEGRATED
```

- 详见 `DH_NQ_INTEGRATION0_ACCEPTANCE_REPORT.md`。
- 已完成链路：三轮审计 + 汇总 → 事实源同步 → 契约冻结 → contract test 矩阵设计 → contract test 代码实现（NQ 16 + DH 16）→ implementation review（PASS）→ 本次验收关闭。
- 验收依据：DH `mvn test` BUILD SUCCESS（dh-domain 86 tests / 0 failures，Integration-0 16 passed，ArchitectureTest 12 条全绿，PostgresContainerSmokeTest 既有环境性 skip）；NQ `mvn -f backend/pom.xml test` BUILD SUCCESS（nq-app 51 tests / 0 failures，Integration-0 16 passed，ArchUnit 全绿）。两侧均覆盖 INT0-T01..T15，含 negative path、audit event shape、forbidden side-effect。
- 契约范围：10 个契约 contract-only / mock-only / test-protected（无真实 HTTP / 无 RealClient / 无真实 NQ）。
- Integration-1 前置 blocker：DH P1-4 residual（rate limit / memory cap / replay nonce persistence，修复后须重跑 contract tests，T06 须以持久化 nonce 重跑，并新增 429 限流与 bounded store 测试）；header `X-DH-NQ-*`/`X-NQ-DH-*` 对齐；真实通道安全前置（单独开工 + 设计审计 + staging/paper-only + LIVE disabled + 无凭证落日志 + no trading side-effect + 安全审查）。
- 下一步只允许：DH docs governance / Decision Pipeline MVP planning，或基于 NQ GateN rebase 的 Integration-1 planning-only audit。禁止直接 Integration-1 实现 / 真实只读通道 / 真实 HTTP / RealClient / Provider / LIVE / AI 自动交易 / LangGraph runtime。

## 1.3 DH P1-4 residual regression close（2026-06-13，CLOSED）

P1-4 residual 三项修复整体回归收口验收（DH-P1-4-RESIDUAL-FIX-REGRESSION-CLOSE）通过：

```text
DH P1-4 residual:        CLOSED
  replay nonce persistence: closed   （BATCH-1：JdbcNonceReplayGuard + V4 + 条件装配 + fail-closed）
  memory cap:               closed   （BATCH-2：bounded nonce guard / feedback repo + TTL + fail-closed）
  rate limit:               closed   （BATCH-3：RateLimiter + 429 RATE_LIMITED，前置于 HMAC，bounded fail-closed）
Integration-1:           NOT STARTED
Runtime integration:     NOT STARTED
DH:                      NOT INTEGRATED
AI:                      NOT STARTED
LIVE:                    DISABLED
header alignment:        CLOSED（2026-06-15，见 DH_NQ_HEADER_ALIGNMENT_PLAN.md）
timestamp alignment:     CLOSED（2026-06-15，见 DH_NQ_TIMESTAMP_FORMAT_ALIGNMENT_PLAN.md）
```

- 验收依据：`mvn test` BUILD SUCCESS、`mvn -Pquality validate` BUILD SUCCESS；关键测试全绿——
  JdbcNonceReplayGuardTest 5、NonceReplayGuardTypeTest 5、HmacNqFeedbackAuthenticatorTest 6、
  BoundedInMemoryNonceReplayGuardTest 7、BoundedInMemoryNqFeedbackEventRepositoryTest 7、
  NqFeedbackPayloadSizeGateTest 2、InMemoryRateLimiterTest 6、NqFeedbackRateLimitWebMvcTest 3、
  NqFeedbackControllerWebMvcTest 15、DhNqIntegration0*（INT0-T01..T15）16/16、ArchUnit 全绿。
- 既有安全语义保持：HMAC / timestamp / nonce / replay 409 / payload 64KiB gate / source allowlist /
  tenant binding 全部不变；状态码 401/403/409/413/429/202 均有测试覆盖；三项修复之间无冲突
  （Batch1 JDBC 与 Batch2 in-memory 经 NonceReplayGuardType 按 profile 二选一同端口；Batch3 限流前置短路，不消耗 nonce/replay store）。
- **持久化 nonce restart 语义仍需 Docker CI 独立验证**：本机无 Docker，JdbcNonceReplayGuardPersistenceTest（3）
  按 `disabledWithoutDocker` skip；不得据此声称 restart 语义已在本机实跑。
- **P1-4 CLOSED 仅表示 Integration-1 的前置安全缺口关闭，不等于允许真实联调。** Integration-1 仍 NOT STARTED。
- 非阻塞后续项（均未在本轮处理）：① Docker CI 跑通持久化 nonce IT；② header `X-DH-NQ-*`/`X-NQ-DH-*` 对齐；
  ③ datasource 默认弱口令治理；④ 多实例真实通道集中式（Redis）rate limiter，Integration-1 前设计审查；
  ⑤ rate limit 指标 / counter 可观测性增强；⑥ P1-4 后 Integration-1 planning-only audit。

## 1.4 DH docs governance / Decision Pipeline MVP pre-plan baseline（2026-07-01）

```text
Integration-0 safety gate: CLOSED / ACCEPTED
P1-4 residual: CLOSED
header alignment: CLOSED
timestamp alignment: CLOSED
code reality audit blockers: fixed
DH security state: FULL
DH fail-closed state: FULL
Integration-0 contract state: MATCH
Decision pipeline state: PARTIAL
Audit state: PARTIAL
Replay state: PARTIAL
Integration-1 implementation: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LIVE: DISABLED
DH Stage4 Decision Pipeline MVP PLAN: ACCEPTED / CLOSED
DH Stage4 Decision Pipeline MVP WO: ACCEPTED / CLOSED
K1 Contract Freeze Review: PASS / CLOSED / ACCEPTED
M1 Readiness Review: CLOSED / ACCEPTED
Current main line: NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY / NOT IMPLEMENTED
Next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
K2 DecisionOrchestrator Skeleton: IMPLEMENTED
K3 Audit / Snapshot / Trace Persistence: CLOSED / ACCEPTED after M1
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
NQ current planning baseline: GateN
```

- 本轮新增 DH 文档治理 skill：`.agents/skills/dh-docs-writer/SKILL.md`。
- DH 文档任务必须继续以 `docs/current` 为事实源；root `README.md` 只做入口和简要状态。
- 后续 K1 只能冻结 DecisionRequest / DecisionOutput / DecisionAction / forbiddenActions / DecisionAuditEvent / DecisionTraceStep 的 domain contract 与 JSON schema，并补齐 K1 contract tests；K2-K8 仍需逐批 review。
- 第一版 DecisionOutput 必须保持 `READ_ONLY_RECOMMENDATION`；默认无证据或 provider 失败时 `ABSTAIN`；policy denied 与 audit 写失败必须 fail-closed。
- 当前仍禁止真实 NQ runtime、真实 provider、真实 HTTP、LangGraph runtime、AI / Agent runtime、LIVE、NQ DB 读写、NQ mutation、下单和撤单。

## 1.5 DH Stage4 Decision Pipeline MVP PLAN（2026-07-01，ACCEPTED / CLOSED）

```text
Plan artifact: docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md
Plan status: ACCEPTED / CLOSED
Work order artifact: docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md
Work order status: ACCEPTED / CLOSED
K1 contract freeze status: PASS / CLOSED / ACCEPTED
K2 DecisionOrchestrator Skeleton status: IMPLEMENTED
K3 Audit / Snapshot / Trace Persistence status: CLOSED / ACCEPTED after M1
M1 Readiness Review status: CLOSED / ACCEPTED
K4 Replay Read Model status: CLOSED
K5 Provider Health / Budget / Latency status: CLOSED
K6 Mock NQ Dry-run Contract Tests status: CLOSED
K7 Golden Cases / Eval status: CLOSED
K8 Acceptance / Freeze status: CLOSED / ACCEPTED
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN status: PASS / PLAN ONLY / READY FOR P0 FACTSOURCE REBASE
P1 follow-up consumed: YES, by NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY.
Current next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
Full decision pipeline runtime: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮只做 docs-only / plan-only：规划 K0-K7、DecisionRequest / DecisionOutput、DecisionOrchestrator skeleton、snapshot / trace / audit / replay、mock provider、mock NQ dry-run contract tests、provider health / budget / latency、golden cases / eval、acceptance / freeze。
- Readiness recommendation：`ALLOW_STAGE4_PLAN_CLOSE: YES`；`ALLOW_STAGE4_WO: YES`；`ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO`；`ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。
- 旧 `NQ-DH-GATEK-INTEGRATION1-PLAN-PACK` 继续保持 `SUPERSEDED / REBASE_REQUIRED`，只保留安全边界作为参考；后续 Integration-1 必须基于 NQ GateN 重新规划。
- 本计划不新增 API path、不新增 migration、不新增 Controller / Service / Repository / Client、不接真实 HTTP、不接真实 provider、不接 AI / LangGraph、不改 NQ、不启动 Integration-1、不启用 LIVE。

## 1.6 DH Stage4 Decision Pipeline MVP WO（2026-07-01，ACCEPTED / CLOSED）

```text
Work order artifact: docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md
Work order status: ACCEPTED / CLOSED
Source plan: DH-STAGE4-DECISION-PIPELINE-MVP-PLAN / ACCEPTED / CLOSED
K1 contract freeze status: PASS / CLOSED / ACCEPTED
K2 DecisionOrchestrator Skeleton status: IMPLEMENTED
K3 Audit / Snapshot / Trace Persistence status: CLOSED / ACCEPTED after M1
M1 Readiness Review status: CLOSED / ACCEPTED
K4 Replay Read Model status: CLOSED
K5 Provider Health / Budget / Latency status: CLOSED
K6 Mock NQ Dry-run Contract Tests status: CLOSED
K7 Golden Cases / Eval status: CLOSED
K8 Acceptance / Freeze status: CLOSED / ACCEPTED
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN status: PASS / PLAN ONLY / READY FOR P0 FACTSOURCE REBASE
P1 follow-up consumed: YES, by NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY.
Current next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
Full decision pipeline runtime: NOT STARTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮只做 docs-only / work-order-only：新增 DH Stage4 Decision Pipeline MVP K1-K8 工单拆解，不写生产代码、不写测试代码、不新增 API、不新增 migration、不新增 Controller / Service / Repository / Client、不接 provider、不接 NQ、不接 AI / LangGraph、不启用 LIVE。
- K1-K8 顺序锁定：K1 review 后才允许 K2；K2 review 后才允许 K3；K3 review 后才允许 K4；K1-K5 完成后才允许 K6；K1-K7 完成后才允许 K8。
- DecisionOutput hardening 固化为 `READ_ONLY_RECOMMENDATION`，action 仅允许 `ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`，默认 `ABSTAIN`；no evidence / provider failure -> `ABSTAIN`；policy denied -> `BLOCKED` or `ABSTAIN` fail-closed；high risk 禁止 `LONG_BIAS / SHORT_BIAS`。
- `forbiddenActions` 必须包含 `PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB`；最终输出必须是 structured JSON，不允许 free-text final output，不允许真实交易指令。
- Readiness decision：`ALLOW_WO_CLOSE: YES`；`ALLOW_K1_IMPLEMENTATION: YES`；`ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.7 DH Stage4 Decision Pipeline MVP K1 Contract Freeze（2026-07-01，PASS / CLOSED / ACCEPTED）

```text
K1 status: PASS / CLOSED / ACCEPTED
K1 review: PASS / CLOSED / ACCEPTED
K2 DecisionOrchestrator Skeleton: IMPLEMENTED
K3 Audit / Snapshot / Trace Persistence: CLOSED / ACCEPTED after M1
M1 Readiness Review: CLOSED / ACCEPTED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN: PASS / PLAN ONLY / READY FOR P0 FACTSOURCE REBASE
P1 follow-up consumed: YES, by NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY.
Current next concrete action: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
API changes: NONE
Migration changes: NONE
Runtime integration: NOT STARTED
Integration-1 runtime: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮新增 `com.guidinglight.decisionhub.domain.decision` K1 合同包，冻结 `DecisionRequest`、`DecisionOutput`、`DecisionSubject`、`DecisionContextSnapshot`、`DecisionEvidence`、`DecisionPolicyResult`、`DecisionRiskReview`、`ProviderDecisionSignal`、`DecisionTraceStep`、`DecisionAuditEvent` 与配套 enum。
- 新增 JSON Schema：`contracts/json-schema/dh-decision-request.schema.json`、`contracts/json-schema/dh-decision-output.schema.json`。
- 新增 K1 contract tests：schema 存在性、required 字段、`additionalProperties=false`、Java enum 与 schema enum 一致、action 禁止 `BUY / SELL / PLACE_ORDER / CANCEL_ORDER / MARKET_ORDER / LIMIT_ORDER`、`decisionType` 仅允许 `READ_ONLY_RECOMMENDATION`、`forbiddenActions` 固定五项、request schema 不含 credential / execution intent 字段、output schema 不含 free-text final output 或 execution command 字段。
- K1 已通过 review 并关闭；K1 只冻结 domain contract / JSON schema / contract tests，未授权 API、migration、repository、client、NQ runtime、AI / LangGraph 或 LIVE。
- K1 review decision：`ALLOW_K1_CLOSE: YES`；`ALLOW_K2_IMPLEMENTATION: YES`；`ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.8 DH Docs Language Governance Fix（2026-07-01，DOCS-ONLY / READY FOR REVIEW）

```text
Task: DH-DOCS-LANGUAGE-GOVERNANCE-FIX
Scope: docs governance / language policy / comment style rules / factsource sync
Business state change: NONE
Current main line now: NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN / PASS / PLAN ONLY / READY FOR P0 FACTSOURCE REBASE
P1 follow-up consumed now: YES, by NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN / COMPLETED / PLAN ONLY.
Current next concrete action now: NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN / NOT STARTED
K2 DecisionOrchestrator Skeleton: IMPLEMENTED
K3 Audit / Snapshot / Trace Persistence: CLOSED / ACCEPTED after M1
M1 Readiness Review: CLOSED / ACCEPTED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
Integration-1 runtime: NOT STARTED
Runtime integration: NOT STARTED
DH integrated: NO
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮只治理 DH 文档语言规则与明显英文漂移，不修改 Java 生产代码、测试代码、contracts、golden_cases、API path、migration、runtime、provider、NQ runtime 或 LIVE。
- `dh-docs-writer` 已新增语言规则：DH 文档正文、架构说明、阶段计划、Worklog、Testing、Roadmap、Status 默认中文为主；DH 代码注释 / Javadoc 默认中文为主。
- 允许保留英文的范围：Java 包名 / 类名 / 方法名 / 字段名、enum 值、JSON Schema 字段、OpenAPI 字段、HTTP header、状态枚举、命令、外部技术名和固定输出字段。
- 固定输出字段可保留英文，但字段内容必须中文为主；不得新增英文长段落；不得把中文业务概念翻译成不稳定英文术语后反复使用；从 NQ skill 同步规则时必须改写为 DH 中文主语言风格。
- 已修正 `DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md` 与 `DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md` 的明显英文标题、说明段落和表格列名漂移；保留工程名词、enum、schema key、状态词和命令原样。

## 1.9 DH Stage4 Decision Pipeline MVP K2 Orchestrator Skeleton（2026-07-01，IMPLEMENTED / READY FOR REVIEW）

```text
K2 status: IMPLEMENTED
Follow-up status: K3 closed after M1 in §1.10; K4 implemented in §1.11
K3 Audit / Snapshot / Trace Persistence: CLOSED / ACCEPTED after M1
M1 Readiness Review: CLOSED / ACCEPTED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
API changes: NONE
Migration changes: NONE
Repository / JDBC changes: NONE
Runtime integration: NOT STARTED
Integration-1 runtime: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮只实现 K2 usecase 编排骨架：`DecisionOrchestrator`、`DefaultDecisionOrchestrator`、`DecisionContext`、`DefaultDecisionContextBuilder`、`DefaultDecisionPolicyChecker`、`MockDecisionSignalProvider`、`DefaultDecisionRiskReviewer` 与 `DecisionOutputAssembler`。
- K2 provider 为 deterministic mock-only，默认 `ProviderSignalStatus.MOCKED + DecisionAction.NO_TRADE`；显式拒绝 `ProviderSignalStatus.SUCCESS`，避免误表示真实 provider runtime。
- K2 fail-closed 路径：missing request / forbidden execution intent -> `BLOCKED`；no evidence -> `ABSTAIN`；provider failure / timeout / untrusted -> `ABSTAIN`；high risk -> `ABSTAIN`；内部异常 -> structured `ABSTAIN`。
- 本轮只在 K1 domain output 中补充 observation / risk factory，以便 K2 复用已冻结输出合同；未新增第二套 response model。
- 新增 K2 单元测试 22 cases，覆盖 orchestrator、policy、risk、assembler、mock provider、null / missing evidence / provider failure / high risk / unexpected failure。
- Readiness decision：`ALLOW_K2_CLOSE: YES`；`ALLOW_K3_IMPLEMENTATION: NO`；`ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.10 DH Stage4 Decision Pipeline MVP K3 Audit / Snapshot / Trace Persistence（2026-07-01，CLOSED / ACCEPTED after M1）

```text
K3 status: CLOSED / ACCEPTED after M1
M1 Readiness Review: CLOSED / ACCEPTED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
API changes: NONE
Controller changes: NONE
Replay API: NOT STARTED
Runtime integration: NOT STARTED
Integration-1 runtime: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮新增 DH-owned Flyway V5：`dh_decision_request`、`dh_decision_context_snapshot`、`dh_decision_trace_step`、`dh_decision_provider_call_log`、`dh_decision_output`、`dh_decision_audit_event` 六张表，字段包含 `decision_id` / `trace_id` / `tenant_id`、`jsonb` payload、`timestamptz` 时间戳、索引和中文 comment。
- 本轮新增 `DecisionAuditRepository` usecase port 与 `DecisionPersistenceRecords`，由 `JdbcDecisionAuditRepository` 在 `dh-infra` 做 insert-only JDBC 写入；`dh-app` 仅新增本地 wiring，不新增 API path 或 Controller。
- `DefaultDecisionOrchestrator` 已接入 request、context snapshot、trace step、mock provider call summary、decision output、audit event 持久化；audit / snapshot / trace / output 写失败统一返回 structured `ABSTAIN` + `PERSISTENCE_FAILURE`，即使 fail-closed output 二次写入失败也不返回普通成功或裸异常。
- K3 `output_json` 使用显式安全 Map 保存 K1 structured fields，不直接序列化 domain object；provider call 只保存 deterministic mock summary，不保存真实 provider raw response、credential、NQ DB 内容或敏感 header。
- 新增 K3 测试覆盖 valid write-through、policy denied、provider timeout、high risk no directional bias、request/context/output/audit persistence failure、missing request unknown IDs、JDBC SQL / JSONB cast / exception mapping、V5 migration presence 和 no trading table。
- M1 readiness review 已关闭，K4 Replay Read Model 已实现内部只读 read model；K5 provider health / budget / latency 已实现；K6 mock NQ dry-run contract tests 已完成；K7 golden cases / eval 已实现；K8 acceptance / freeze 仍未开始。
- Readiness decision：`ALLOW_K3_CLOSE: YES`；`ALLOW_K4_IMPLEMENTATION: YES`；`ALLOW_STAGE4_M1_CLOSE_REVIEW: YES`；`ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.11 DH Stage4 Decision Pipeline MVP K4 Replay Read Model（2026-07-01，IMPLEMENTED / READY FOR NEXT）

```text
K4 status: IMPLEMENTED / READY FOR NEXT
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
Replay API: NOT STARTED
API changes: NONE
Controller changes: NONE
Migration changes: NONE
Runtime integration: NOT STARTED
Integration-1 runtime: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮新增 K4 内部 replay read model：`DecisionReplayView` / request / context / trace step / provider call / output / audit event / timeline 只读视图，以及 `DecisionReplayStatus` fail-closed 状态。
- 本轮新增 usecase port/service：`DecisionReplayQuery`、`DecisionReplayQueryRepository`、`DecisionReplayQueryService`、`DefaultDecisionReplayQueryService`；输入非法、repository 异常、tenant mismatch、trace/request mismatch 均返回结构化 fail-closed 结果。
- 本轮新增 `JdbcDecisionReplayQueryRepository`，只读取 K3 六张 DH-owned 表：`dh_decision_request`、`dh_decision_context_snapshot`、`dh_decision_trace_step`、`dh_decision_provider_call_log`、`dh_decision_output`、`dh_decision_audit_event`；所有 SQL 均带 `tenant_id = ? and decision_id = ?`，trace / provider / audit 按时间稳定排序。
- K4 JSON 读取使用内部 `ObjectMapper.readValue` 解析安全 `Map` / `List`；JSON、枚举或时间字段不可解析时返回 `CORRUPTED`，DB 读取失败返回 `BLOCKED`；不写库、不重跑 provider、不重跑 orchestrator、不修改 audit 数据。
- `DecisionPipelineWiringConfig` 仅补充 replay repository/service bean，复用私有 persistence `ObjectMapper` 方法，未新增全局 `ObjectMapper` bean，也不影响 WebMVC Jackson。
- 新增 K4 单元/JDBC/wiring 回归测试，覆盖 found、not found、tenant mismatch、incomplete、corrupted、DB failure、trace/provider/audit ordering、tenant-scoped SQL、no JDBC write、ObjectMapper bean 唯一性。
- Readiness decision：`ALLOW_K4_CLOSE: YES`；`ALLOW_K5_IMPLEMENTATION: YES`；`ALLOW_STAGE4_M2_CLOSE_REVIEW: NO`；`ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.12 DH Stage4 Decision Pipeline MVP K5 Provider Health / Budget / Latency（2026-07-01，IMPLEMENTED / READY FOR NEXT）

```text
K5 status: IMPLEMENTED / READY FOR NEXT
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
API changes: NONE
Controller changes: NONE
Migration changes: NONE
Runtime integration: NOT STARTED
Integration-1 runtime: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮新增 K5 provider guard 内部模型：`DecisionProviderHealth`、`DecisionProviderBudget`、`DecisionProviderLatency`、`DecisionProviderGuardResult`、`DecisionProviderFailureClass` 以及 health / budget 状态枚举；模型只表达 provider 观测与 guard 结果，不表达交易动作、不包含 credential 或真实 provider response。
- 本轮新增 usecase guard 组件：`DecisionProviderHealthEvaluator`、`DecisionProviderBudgetGuard`、`DecisionProviderLatencyRecorder`、`DecisionProviderGuard` 及默认实现；首版只处理 deterministic mock provider，本地预算估算默认 1 unit / 1 unit，默认 latency timeout 1000ms。
- `DefaultDecisionOrchestrator` 已接入 provider 调用前 enabled / budget guard，调用后 latency recorder / health evaluator / guard result；disabled、unhealthy、timeout、untrusted、budget exceeded 或 provider exception 均在 risk review 前 fail-closed 到 `ABSTAIN`，并写入 provider call summary。
- K3 既有 `dh_decision_provider_call_log` 被复用；K5 未新增 table。provider call summary 通过既有 `latency_ms`、`provider_status`、`error_code` 与 `signal_json` 保存 `guardAllowed`、`failureClass`、`healthStatus`、`budgetStatus`、`latencyMs`、`timeoutMs`、`timedOut` 等安全字段。
- K4 replay read model 可继续读取 provider call summary；`JdbcDecisionReplayQueryRepositoryTest` 已覆盖 `failureClass`、`latencyMs`、`errorCode` 经 replay provider call view 可见。
- `DecisionPipelineWiringConfig` 已补充 K5 guard wiring，并保持全局 `ObjectMapper` 仍唯一，不污染 WebMVC。
- 新增 / 更新 K5 tests 覆盖 health evaluator、budget guard、latency recorder、provider guard、orchestrator fail-closed、mock provider K5 failure hooks、no outbound runtime scan、replay provider guard view regression 和 app wiring。
- Readiness decision：`ALLOW_K5_CLOSE: YES`；`ALLOW_K6_IMPLEMENTATION: YES`；`ALLOW_STAGE4_M2_CLOSE_REVIEW: NO`；`ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.13 DH Stage4 Decision Pipeline MVP K6 Mock NQ Dry-run Contract Tests（2026-07-01，IMPLEMENTED / READY FOR NEXT）

```text
K6 status: IMPLEMENTED / READY FOR NEXT
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
API changes: NONE
Controller changes: NONE
Migration changes: NONE
Production code changes: NONE
Runtime integration: NOT STARTED
Integration-1 runtime: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮只新增 K6 test-support、contract tests 与 `golden_cases/decision` mock fixture；未修改 `src/main` 生产代码、API、Controller、migration、contracts schema 或 wiring。
- 新增 `MockNqDecisionRequestFactory`、`MockNqDryRunFixtures`、`MockNqDryRunAssertionSupport` 与 test-only `RecordingDecisionAuditReplayRepository`，用于生成 mock NQ 只读 `DecisionRequest`、读取 fixture、统一合同断言，并证明 K3 写入能被 K4 replay read model 读回。
- 新增 K6 tests 覆盖 valid mock NQ request -> structured `DecisionOutput`、`READ_ONLY_RECOMMENDATION`、allowed action vocabulary、mandatory forbiddenActions、credential / execution intent 禁止字段、provider disabled / budget exceeded / timeout fail-closed、persistence / replay / tenant isolation、no-live-trade source scan。
- 新增最小 fixture：`mock_nq_valid_dryrun.json`、`mock_nq_provider_blocked.json`、`mock_nq_no_live_trade_guard.json`；fixture 仅作为 K6 contract test 输入，不进入 K7 eval framework，不含真实账户、凭证或交易指令。
- Readiness decision：`ALLOW_K6_CLOSE: YES`；`ALLOW_K7_IMPLEMENTATION: YES`；`ALLOW_STAGE4_ACCEPTANCE_REVIEW: NO`；`ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_ACCEPTANCE_REVIEW: NO`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

## 1.14 DH Stage4 Decision Pipeline MVP K7 Golden Cases / Eval（2026-07-01，IMPLEMENTED / READY FOR ACCEPTANCE）

```text
K7 status: IMPLEMENTED / READY FOR ACCEPTANCE
K8 Acceptance / Freeze: CLOSED / ACCEPTED
API changes: NONE
Controller changes: NONE
Migration changes: NONE
Production code changes: NONE
Runtime integration: NOT STARTED
Integration-1 runtime: NOT STARTED
AI / Agent runtime: NOT STARTED
LangGraph runtime: NOT STARTED
LIVE: DISABLED
```

- 本轮将 `golden_cases/decision` 扩展为 12 个 deterministic golden cases，并把 K6 的 3 个 mock NQ fixture 升级为完整 golden case wrapper：`caseId`、`description`、`input`、`expectedDecision`、可选 `expectedReplay`、`forbiddenAssertions`、`securityBoundary`。
- K7 覆盖 valid no-trade、policy blocked、provider timeout、provider budget exceeded、high risk、no evidence、forbidden action rejected、mock NQ dry-run、mock NQ provider blocked、mock NQ no-live guard、replay found trace、replay tenant mismatch。
- 新增 K7 eval baseline tests：`DecisionGoldenCaseTest`、`DecisionEvalBaselineTest`、`DecisionGoldenCaseSecurityBoundaryTest`，覆盖 JSON 可解析、K1 request/output schema 形状、action 白名单、mandatory forbiddenActions、安全字段扫描、deterministic orchestrator 输出、K6 mock NQ dry-run 兼容与 replay tenant isolation。
- 本轮只修改 golden cases、test code 与 `docs/current`；未修改生产代码、contracts schema、API、Controller、migration、wiring、provider runtime 或 NQ runtime。
- Readiness decision：`ALLOW_K7_CLOSE: YES`；`ALLOW_K8_ACCEPTANCE_FREEZE: YES`；`ALLOW_INTEGRATION_1_RUNTIME: NO`；`ALLOW_AGENT_PHASE: NO`；`ALLOW_LANGGRAPH_RUNTIME: NO`；`ALLOW_LIVE: NO`。

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
DH-CODEX-WORKFLOW       2026-06-06 完成 Codex workflow routing 规则固化（仅文档）：
                        - 新增 `.agents/skills/nq-dh-workflow-router/SKILL.md`
                        - 新增 docs/current/CODEX_PROJECT_INSTRUCTIONS.md
                        - 新增 docs/current/CODEX_WORKFLOW_INDEX.md
                        - 新增 docs/current/DH_CODEX_PLUGIN_WORKFLOW.md
                        - 新增 docs/current/DH_WORKFLOW_ROUTER_SKILL.md
                        - 新增 docs/current/DH_CODEX_TASK_TEMPLATES.md
                        - AGENTS.md 声明 `nq-dh-workflow-router` 为 active skill
                        - 统一输出字段使用 Findings；Summary 不作为必填字段
                        - 明确 NQ integration not started；Integration-0 not started / plan only
                        - RealClient / real provider / LIVE trading / NQ mutation 继续禁止
                        - 本轮不修改业务代码，不新增 API / migration / NQ client
DH-CODEX-WORKFLOW-CLEANUP
                        2026-06-06 完成 DOCUMENTATION 冲突修复（仅 Markdown / Skill 文档）：
                        - ROADMAP.md / WORK_ORDER.md 已收口，当前 next 统一为 Integration-0-PLAN
                        - Stage2-PoC / 真实 NQ client / 真实 HTTP / event / backtest request
                          表述已改为 historical / superseded / deferred / forbidden
                        - DH workflow 已补齐 NQ 通用排除目录：
                          node_modules / target / build / dist / .git / logs / test-results /
                          secrets / credentials
                        - DH workflow 已补齐细凭证禁令：
                          token / cookie / API key / API secret / exchange secret /
                          production .env / private key / mnemonic / keystore password /
                          2FA backup code
                        - DH workflow 已补齐开工前范围字段：
                          repository / module / target files / excluded files / expected output
                        - DH workflow 已补齐 archived / historical / superseded 文档不作为当前事实源规则
                        - 当前仍保持 DH-AUDIT-FIX completed；NQ integration not started；
                          Integration-0 not started / plan only；RealClient / real provider /
                          LIVE trading / NQ mutation forbidden
DH-CODEX-WORKFLOW-FINAL-CLEANUP
                        2026-06-06 完成 DOCUMENTATION 入口口径收口（仅 Markdown）：
                        - AGENTS.md / README.md / docs/current/README.md 的 current next 已统一为
                          Integration-0-PLAN
                        - docs/current/DH_NQ_INTEGRATION.md 已增加当前状态锁定，并把 REST API /
                          POST /api/ai/backtest-requests / 真实 HTTP / event / NQ client /
                          RealClient / real provider 方向标记为 historical / superseded /
                          deferred / gated
                        - 本轮不修改业务代码、API、migration、provider、NQ client、RealClient 或交易路径
                        - 当前仍保持 DH-AUDIT-FIX completed；NQ integration not started；
                          Integration-0 not started / plan only；RealClient / real provider /
                          LIVE trading / NQ mutation forbidden
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

## 4. 下一阶段（NQ-DH-I1-P3-NQ-DRYRUN-STUB-TEST-PLAN）

```text
唯一下一步是 NQ-DH-I1-P3-NQ-DRYRUN-STUB-TEST-PLAN（NOT STARTED）。

P0 已关闭：
- NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE = CLOSED / ACCEPTED / DOCS-ONLY。
- NQ / DH 当前事实源已统一到 NQ GateN + DH Stage4 Decision Pipeline MVP CLOSED。
- 旧 NQ-DH-GATEK-INTEGRATION1-PLAN-PACK 保持 SUPERSEDED / REBASE_REQUIRED。
- dry-run 不等于 runtime integration、真实 HTTP、真实交易或 LIVE。

P1 已关闭：
- NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN = COMPLETED / PLAN ONLY / NOT IMPLEMENTED。
- 已规划 NQ -> DH dry-run request、DH -> NQ dry-run response、canonical header、安全协议、error taxonomy、trace / audit / replay 和测试矩阵。

P2 已关闭：
- NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN = COMPLETED / PLAN ONLY / NOT IMPLEMENTED。
- 已规划 future request / response fixtures、schema gap、golden case alignment、error taxonomy alignment、no-runtime/no-live fixture boundary 和 P3-P6 后续批次。
- 未创建 fixture JSON，未修改 schema、contracts、golden_cases、生产代码或测试代码。

P3 只允许：
- 规划 NQ 侧 stub / no-outbound / no-order 测试，不实现 dispatcher、client、Controller、API 或 migration。
- 明确 future NQ stub 如何验证 no-outbound、no-order、no-risk-mutation、no-paper-run-start 和 no-live boundary。
- 继续保持 no real NQ runtime、no real provider、no HTTP、no LIVE。
- 不把 stub test plan 写成真实 NQ 联调、runtime integration 或 provider 接入。

P3 不允许：
- 新增 replay API / Controller / query API。
- 实现真实 NQ client。
- 实现 RealClient / RealNqBacktestClient。
- 接真实 HTTP / event 到 NQ。
- 调用 NQ /api/ai/research/backtest-requests。
- 接真实 LLM provider。
- 接 LangGraph runtime。
- 输出 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 作为 action。
- 启动 Paper Run。
- 修改 NQ 交易状态。
- 访问交易所密钥。
- 触碰 LIVE trading。
- 读取或写入 NQ DB。
- 新增真实 provider / 交易路径。

Stage2-PoC、Stage3-B2/B3/B4 的真实接入、联调、RealClient 或 NQ mutation 方向均为 historical / superseded / deferred，不是当前 next，不允许作为当前实现任务。旧 NQ-DH-GATEK-INTEGRATION1-PLAN-PACK 为 SUPERSEDED / REBASE_REQUIRED；如需恢复 NQ runtime 相关 planning，必须基于 NQ GateN 重新规划。

当前状态保持：
- DH-AUDIT-FIX completed
- NQ integration not started
- Integration-0 safety gate CLOSED / ACCEPTED
- DH-STAGE4-DECISION-PIPELINE-MVP-PLAN ACCEPTED / CLOSED
- DH-STAGE4-DECISION-PIPELINE-MVP-WO ACCEPTED / CLOSED
- DH-STAGE4-DECISION-PIPELINE-MVP-K1-CONTRACT-FREEZE PASS / CLOSED / ACCEPTED
- DH-STAGE4-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON IMPLEMENTED
- DH-STAGE4-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE CLOSED / ACCEPTED after M1
- DH-STAGE4-DECISION-PIPELINE-MVP-M1-READINESS-REVIEW CLOSED / ACCEPTED
- K4 Replay Read Model CLOSED
- K5 Provider Health / Budget / Latency CLOSED
- K6 Mock NQ Dry-run Contract Tests CLOSED
- K7 Golden Cases / Eval CLOSED
- K8 Acceptance / Freeze CLOSED / ACCEPTED
- Full decision pipeline runtime not started
- Integration-1 implementation not started
- Runtime integration not started
- DH integrated NO
- AI / Agent runtime not started
- RealClient forbidden
- real provider forbidden
- real HTTP forbidden
- LIVE trading forbidden
- NQ mutation forbidden
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
