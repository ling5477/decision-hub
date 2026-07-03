# NQ-DH Integration-1 Dry-run Mock Implementation Work Order（DH）

> Task: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO
> Status: COMPLETED / WORK_ORDER_ONLY / NOT IMPLEMENTED
> Date: 2026-07-03
> Repository: Decision Hub
> Source of truth: docs/current

## 1. 目标与边界

本工单只把 Integration-1 dry-run mock implementation 拆成可执行、可审查、可回滚的小批次，并修正上一轮预检规则：NQ dev 不再要求全局干净，只要求不存在 NQ-DH / Integration-1 相关 dirty diff。

本工单不是 implementation，不授权代码落地，不创建 fixtures，不修改 schema，不新增 API，不启动 runtime。

```text
DH current branch: dev
DH precheck: clean
NQ dry-run worktree branch: nq-dh-i1-dryrun
NQ dry-run worktree precheck: clean
NQ dev branch: dev
NQ dev precheck: NQ_MAINLINE_DIRTY_ALLOWED
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
```

允许继续的 NQ dev dirty scope：

```text
marketdata
API
Gate mainline docs
```

必须阻断的 NQ dev dirty scope：

```text
docs/current/*NQ_DH*
docs/current/*INTEGRATION1*
任何 NQ-DH / Integration-1 related dirty diff
```

若触发上述阻断，必须停止并输出：

```text
WORKSTREAM_MIXED_BLOCKED
```

## 2. 当前事实

```text
NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE: CLOSED / ACCEPTED / DOCS-ONLY
NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX: COMPLETED / DOCS-ONLY / GATE-FIX
NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO: COMPLETED / WORK_ORDER_ONLY / NOT IMPLEMENTED
NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO: COMPLETED / WORK_ORDER_ONLY / CONTRACT_GAP_CLOSED / NOT IMPLEMENTED
NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO: COMPLETED / WORK_ORDER_ONLY / DH_DRYRUN_ENTRY_PLANNED / NOT IMPLEMENTED
Next concrete action: NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO / NOT STARTED / WORK_ORDER_ONLY_ALLOWED
```

Schema / contract reality：

```text
DecisionAction whitelist: EXISTS_NOW
Fixed ForbiddenAction list: EXISTS_NOW
dryRun / decisionId / confidence / traceSummary / replayRef / auditRef: DOC_ONLY_ALIAS
X-NQ-DH-Schema-Version: DOC_ONLY_ALIAS
source=NQ_DRYRUN: NEEDS_CONTRACT_REVIEW_BEFORE_CODE
canonical error code names: NEEDS_CONTRACT_REVIEW_BEFORE_CODE
dry-run endpoint shape: NEEDS_CONTRACT_REVIEW_BEFORE_CODE
BUY / SELL / quantity / price / leverage / order / account / credential / mutation: PROHIBITED
```

## 3. 全局禁止项

本工单及其后续批次默认禁止：

```text
真实 NQ runtime connection
真实 HTTP
RealClient
real provider
NQ DB access
NQ mutation
order / trade / position / account / credential access
Paper Run 启动
LIVE
AI / Agent runtime
LangGraph runtime
production profile
真实 exchange API
真实密钥读取或输出
```

本工单本身还禁止：

```text
production code
test code
fixture JSON
contracts/** 修改
golden_cases/** 修改
OpenAPI 修改
Controller 修改
migration
workflow
backend runtime
frontend
research
scripts
deploy
dependency change
```

## 4. 批次总览

执行顺序固定为：

```text
M0 -> M1 -> M2 -> M3 -> M4
```

不得跳批。M0 未关闭前不得进入 M1 / M2 / M3 implementation。M4 只做 close review，不启动 runtime。

## 5. M0 - Contract Gap Close WO

```text
Batch: NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO
Type: CONTRACT_REVIEW_WO + DOCS_ONLY + NO_CODE
Status: COMPLETED / WORK_ORDER_ONLY / CONTRACT_GAP_CLOSED / NOT IMPLEMENTED
Artifact: docs/current/DH_NQ_INTEGRATION1_M0_CONTRACT_GAP_CLOSE_WO.md
```

目标：

- 对 P4 标记为 `NEEDS_CONTRACT_REVIEW_BEFORE_CODE` 的项目做最小 contract close。
- 明确 `source=NQ_DRYRUN` 是否进入 source allowlist。
- 明确 canonical error taxonomy 是否只作为文档映射，还是需要 schema / enum / API 变更。
- 明确 dry-run endpoint shape 是否存在，若存在由哪个仓库负责，若不存在则只允许 test-support mock entry。
- 明确 `dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version` 是否继续保持 `DOC_ONLY_ALIAS`。
- 明确是否需要独立 schema / contract review；如需要，不得混入 M1 / M2 / M3。

允许修改：

```text
docs/current/**
```

禁止修改：

```text
dh-domain/**
dh-usecase/**
dh-memory/**
dh-eval/**
dh-connector/**
dh-api/**
dh-app/**
dh-infra/**
contracts/**
golden_cases/**
src/test/**
pom.xml
```

安全边界：

```text
只读规划
不接 NQ
不新增 endpoint
不新增 Controller
不新增 schema
不新增 fixture
不启动 runtime
```

测试要求：

```text
git diff --check
git diff --stat
targeted rg for forbidden scope
docs/current term consistency check
```

验收标准：

```text
NQ_DRYRUN source decision recorded
error taxonomy decision recorded
dry-run endpoint shape decision recorded
DOC_ONLY_ALIAS fields decision recorded
schema / contract review need recorded
M1 / M2 / M3 entry conditions recorded
```

回滚方式：

```text
revert M0 docs changes
```

M0 close decision：

```text
NQ_DRYRUN source allowlist: NEEDS_SECURITY_CONTRACT_CHANGE
dry-run endpoint shape: Option C / test-support mock-only, no runtime endpoint
DOC_ONLY_ALIAS fields: dryRun / decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version
ALLOW_M0_WO_CLOSE: YES
ALLOW_I1_M1_DH_DRYRUN_CONTRACT_ENTRY_MOCK_WO: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

Review：

```text
M0 requires separate review: YES
implementation code allowed: NO
schema / contracts / golden_cases / fixture JSON allowed: NO
API / Controller allowed: NO
real HTTP / runtime / provider / LIVE allowed: NO
```

## 6. M1 - DH Dry-run Contract Entry Mock

```text
Batch: NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK
Type: DH_TEST_SUPPORT_OR_MOCK_ONLY
Status: COMPLETED / WORK_ORDER_ONLY / DH_DRYRUN_ENTRY_PLANNED / NOT IMPLEMENTED
```

目标：

- 在 M0 关闭后，为 DH 侧 dry-run contract entry 提供 mock-only / test-support entry。
- 验证 canonical header、timestamp、nonce、source allowlist、payload size、rate limit、HMAC、schema 与 forbidden action 边界。
- 不接真实 provider，不接 Agent runtime，不接 LangGraph，不接 NQ runtime。

允许修改范围必须由 M0 明确。默认候选只限：

```text
dh-domain/src/test/**
dh-usecase/src/test/**
dh-api/src/test/**
docs/current/**
```

默认禁止：

```text
dh-api/src/main/**
dh-app/src/main/**
dh-connector/src/main/**
dh-infra/src/main/**
contracts/**
golden_cases/**
fixture JSON
migration
production Controller
```

安全校验顺序必须在 M1 review 中写清：

```text
1. payload size / content boundary
2. required canonical headers
3. source allowlist
4. timestamp UTC Z format and clock window
5. tenant / requestId / traceId binding
6. rate limit
7. HMAC signature
8. nonce replay guard with mark-on-accepted-auth semantics
9. JSON schema and forbidden action validation
10. mock-only orchestration
```

测试要求：

```text
mvn -ntp test
mvn -ntp -Pquality validate
targeted tests for success, missing header, bad timestamp, replay nonce, bad HMAC, disallowed source, oversized payload, forbidden action
```

验收标准：

```text
no real HTTP
no RealClient
no provider
no runtime integration
no secret access
all security failure paths fail closed
M1 review completed before any M2 dependency
```

回滚方式：

```text
revert M1 files
run mvn -ntp test
```

M1 close decision：

```text
RECOMMENDED_ENTRY_SHAPE: Option C / test-support mock-only / no runtime endpoint
NQ_DRYRUN source allowlist: NEEDS_SECURITY_CONTRACT_CHANGE
canonical error code names: planning-only normalization, no enum/schema/code change
DOC_ONLY_ALIAS fields: decisionId / replayRef / auditRef / traceSummary / X-NQ-DH-Schema-Version
ALLOW_M1_WO_CLOSE: YES
ALLOW_I1_M2_NQ_DRYRUN_STUB_RECORDER_WO: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

Review：

```text
M1 requires separate review: YES
implementation code allowed by current next: NO, M1 is work-order/mock-contract planning first
future implementation code allowed: only after M1 review, only mock/test-support scope
schema / contracts / golden_cases / fixture JSON allowed: NO
API / Controller allowed: NO unless M0 creates and closes a separate API review
real HTTP / runtime / provider / LIVE allowed: NO
```

## 7. M2 - NQ Dry-run Stub Recorder

```text
Batch: NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER
Type: NQ_TEST_SUPPORT_OR_MOCK_ONLY
Status: BLOCKED_BY_M1
```

目标：

- 在 NQ worktree 中构造 dry-run request。
- 接收 mock `DecisionOutput`。
- 记录 summary / trace / blocked reason。
- 不执行 order，不读取 account，不访问 credential，不启动 Paper Run。

默认候选范围由 NQ worktree 单独执行，DH 仅记录边界。默认候选只限：

```text
backend/nq-app/src/test/**
backend/nq-app/src/test/resources/**
docs/current/**
```

默认禁止：

```text
backend/*/src/main/**
frontend/**
contracts/**
golden_cases/**
migration
API / Controller
real HTTP client
provider
exchange adapter
```

测试要求：

```text
mvn -ntp -f backend/pom.xml test
mvn -ntp -f backend/pom.xml -pl nq-app -am -Dtest=*Integration0* -Dsurefire.failIfNoSpecifiedTests=false test
targeted tests for dry-run summary, prohibited action rejection, no order side effect, no credential access
```

验收标准：

```text
NQ only records mock dry-run summary
no mutation
no order / position / account / credential read
no Paper Run
no real HTTP
no provider
```

回滚方式：

```text
revert M2 NQ worktree files
run targeted NQ test commands
```

Review：

```text
M2 requires separate review: YES
implementation code allowed: YES, only after M1 review, only NQ mock/test-support scope
schema / contracts / golden_cases allowed: NO
fixture JSON allowed: NO, unless M3 separately authorizes fixture files
API / Controller allowed: NO
real HTTP / runtime / provider / LIVE allowed: NO
```

## 8. M3 - Joint Mock Fixtures And Contract Tests

```text
Batch: NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-CONTRACT-TESTS
Type: CROSS_REPO_MOCK_CONTRACT_TESTS
Status: BLOCKED_BY_M1_M2
```

目标：

- 在 M1 与 M2 review 通过后，最小化补齐跨仓 mock fixtures / contract tests。
- 覆盖 request / response / error taxonomy / forbidden actions / trace fields / audit refs 的 mock-only contract path。
- fixtures 必须脱敏、无真实账户、无真实凭证、无真实交易数据。

允许范围由 M3 工单单独确认。默认候选：

```text
DH test resources only
NQ test resources only
docs/current/**
```

默认禁止：

```text
production code
OpenAPI path
Controller
migration
runtime client
provider
real HTTP
LIVE
```

测试要求：

```text
DH: mvn -ntp test
DH: mvn -ntp -Pquality validate
NQ: mvn -ntp -f backend/pom.xml test
NQ: targeted Integration0 / NQ-DH contract tests
fixture secret scan / forbidden term scan
```

验收标准：

```text
all fixtures are mock-only
all prohibited actions rejected
error taxonomy stable
no real endpoint
no runtime connection
no credential material
```

回滚方式：

```text
revert M3 fixture and test files
run DH and NQ targeted validation
```

Review：

```text
M3 requires separate review: YES
implementation code allowed: YES, only test code after M1/M2 review
schema / contracts / golden_cases allowed: NO unless separately authorized by M0 follow-up
fixture JSON allowed: YES, only if M3 explicitly authorizes mock-only fixture files
API / Controller allowed: NO
real HTTP / runtime / provider / LIVE allowed: NO
```

## 9. M4 - Close Review

```text
Batch: NQ-DH-I1-M4-DRYRUN-MOCK-CLOSE-REVIEW
Type: CLOSE_REVIEW + DOCS_ONLY
Status: BLOCKED_BY_M1_M2_M3
```

目标：

- 复核 M0-M3 是否全部按边界完成。
- 复核双仓没有越界代码、schema、API、runtime、provider、LIVE。
- 决定是否允许进入下一轮 limited dry-run runtime planning。
- 不启动 runtime planning 本身。

允许修改：

```text
docs/current/**
docs/gates/** only if closeout archive is separately authorized
```

禁止修改：

```text
production code
test code
contracts/**
golden_cases/**
API / Controller
migration
runtime
provider
```

测试要求：

```text
DH: git diff --check
DH: mvn -ntp test
DH: mvn -ntp -Pquality validate
NQ: git diff --check
NQ: mvn -ntp -f backend/pom.xml test
NQ: targeted forbidden-scope diff scan
```

验收标准：

```text
M0-M3 evidence complete
no forbidden runtime
no real HTTP
no provider
no NQ mutation
no credentials
next planning decision explicit
```

回滚方式：

```text
revert M4 docs
retain previous accepted batch state
```

Review：

```text
M4 requires separate review: YES
implementation code allowed: NO
schema / contracts / golden_cases / fixture JSON allowed: NO
API / Controller allowed: NO
real HTTP / runtime / provider / LIVE allowed: NO
```

## 10. 验收清单

```text
ALLOW_WORK_ORDER_CLOSE: YES
ALLOW_I1_M0_CONTRACT_GAP_CLOSE_WO: YES / COMPLETED
ALLOW_I1_M1_DH_DRYRUN_CONTRACT_ENTRY_MOCK_WO: YES
ALLOW_I1_DRYRUN_MOCK_IMPLEMENTATION_CODE_THIS_TURN: NO
ALLOW_SCHEMA_CHANGE_THIS_TURN: NO
ALLOW_FIXTURE_JSON_THIS_TURN: NO
ALLOW_CONTRACTS_MODIFICATION_THIS_TURN: NO
ALLOW_GOLDEN_CASES_MODIFICATION_THIS_TURN: NO
ALLOW_API_CONTROLLER_THIS_TURN: NO
ALLOW_RUNTIME_THIS_TURN: NO
ALLOW_REAL_HTTP_THIS_TURN: NO
ALLOW_REAL_PROVIDER_THIS_TURN: NO
ALLOW_AI_AGENT_RUNTIME_THIS_TURN: NO
ALLOW_LANGGRAPH_RUNTIME_THIS_TURN: NO
ALLOW_LIVE_THIS_TURN: NO
```

## 11. 下一步

唯一下一步：

```text
NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO / NOT STARTED / WORK_ORDER_ONLY_ALLOWED
```

M2 仍是 work order / mock contract planning 文档任务，不是 code implementation；不得在 NQ dev 主线执行。
