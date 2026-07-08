# DH-NQ Integration-1 Limited Dry-run Runtime Plan

> 任务：`NQ-DH-I1-LIMITED-DRYRUN-RUNTIME-PLAN`
> 类型：`PLAN_ONLY + LIMITED_DRYRUN_RUNTIME_PLANNING + CROSS_REPO_RUNTIME_BOUNDARY_DESIGN + API_CONTRACT_REVIEW_PREP + NO_RUNTIME_IMPLEMENTATION + NO_LIVE`
> 日期：2026-07-04
> 仓库视角：Decision Hub（DH）
> 状态：`CLOSED / ACCEPTED / PLAN_ONLY / NOT_IMPLEMENTED / NO_RUNTIME`

## 1. 结论

本文件只评估 NQ-DH Integration-1 是否可以从 mock / test-support baseline 进入 limited dry-run runtime planning。结论是：**可以关闭本 planning 文档，并允许后续单独进入 runtime API / contract / security review 与 mock baseline PR prep；仍不允许 runtime implementation**。

本轮没有写生产代码、测试代码、API、Controller、Client、Repository、Service、migration、schema、contracts、golden_cases 或 fixture JSON，也没有启动 runtime、真实 HTTP、真实 provider、AI / LangGraph 或 LIVE。

Readiness decision：

```text
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
```

关键结论：

- `NQ_DRYRUN` 不能因为本计划进入 DH production source allowlist；如需生产化，必须另起 security contract change。
- canonical error enum / error schema 需要独立冻结，不得在本轮改 schema 或 contracts。
- DH dry-run endpoint / API / Controller 不能在本轮新增；是否新增必须由 `NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW` 决定。
- schema aliases 不能在本轮转为正式 envelope 字段；必须先做 schema / compatibility review。
- NQ 侧不需要、也不允许在本轮新增真实 HTTP client；未来若需要，必须 feature flag off by default，并单独 review。
- mock baseline 建议先走 PR prep，把 test-support / mock-only 证据合并策略固定下来，再进入 runtime API / contract review。

## 2. 事实源与证据

本轮已核对的本地边界：

```text
DH branch: dev
DH HEAD: 24d56abcf4f8bac3791b1cd8b6a0c5b147b547be
DH precheck: inherited docs/current diff present in allowed files only

NQ worktree branch: nq-dh-i1-dryrun
NQ worktree HEAD: f658f0ef3633e855d9b38ce5ef2ecda1020c4514
NQ worktree precheck: inherited docs/current diff present in allowed files only

NQ dev branch: dev
NQ dev HEAD: 2919f29e4d530e314d7484f2fd340bb5c8626abd
NQ dev scoped NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
```

mock / test-support baseline：

```text
NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION: CLOSED / TEST_SUPPORT_ONLY / MOCK_ONLY
NQ-DH-I1-IMP1-DH-DRYRUN-TEST-SUPPORT-ENTRY: CLOSED / TEST_SUPPORT_ONLY / MOCK_ONLY
NQ-DH-I1-IMP2-NQ-STUB-RECORDER-NO-SIDE-EFFECT: CLOSED / VERIFY_PASS / TEST_SUPPORT_ONLY / MOCK_ONLY
NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS: CLOSED / TEST_SUPPORT_ONLY / MOCK_ONLY
NQ-DH-I1-MOCK-CLOSE-REVIEW: CLOSED / ACCEPTED / REVIEW_ONLY / NO_RUNTIME
Mock / test-support baseline: CLOSED
Limited dry-run runtime planning: ALLOWED
Runtime implementation: NOT ALLOWED
```

代码边界核对：

- DH production scan 只命中既有 `NqFeedbackController` / `NqDhHeader*` feedback header 处理；未发现 dry-run runtime endpoint。
- DH Integration-1 dry-run 支撑只存在于 `dh-usecase/src/test/**` 与 `src/test/resources/**`。
- NQ production scan 只命中既有 credential permission probe DTO 的普通 `dryRun` 字段；未发现 `NQ_DRYRUN`、`NqDhIntegration1`、DH runtime client 或 `/dry-run` runtime token。
- NQ Integration-1 证据只存在于 `backend/nq-app/src/test/**` 与 `src/test/resources/**`。
- 本轮未修改 `contracts/**`、`golden_cases/**`、OpenAPI、migration 或 CI workflow。

## 3. 方案选项评估

### Option A：继续保持 test-support only，不进入 runtime

结论：可作为最低风险保守选项，但不作为推荐下一步。

适用条件：

- 团队决定继续暂停 Integration-1 runtime 方向。
- API / contract / security review 暂不排期。
- mock baseline PR 暂不准备。

风险：

- 不解决后续 limited runtime 的 API shape、source allowlist、error taxonomy、feature flag、kill-switch 与 rollback 设计。
- mock baseline 仍停留在 worktree，不能形成可审查合并里程碑。

### Option B：新增 DH dry-run runtime endpoint，但仅 mock provider、feature flag disabled by default

结论：需要独立 review；本轮不允许实现。

前置 review：

- API review：路径、method、auth、rate limit、payload size、status code、OpenAPI。
- Contract review：request / response schema、envelope、error taxonomy、schema version、alias compatibility。
- Security review：source allowlist、HMAC、timestamp、nonce replay、tenant / request / trace binding、redaction。
- Runtime boundary review：feature flag default disabled、mock provider only、no NQ DB、no executable trading output。

### Option C：NQ 侧新增 runtime client，但默认禁用，no-real-http 默认测试

结论：需要独立 review；本轮不允许实现。

约束：

- client 必须 feature flag off by default。
- 默认 profile 必须 no-outbound。
- 任何真实 HTTP、WebClient、RestTemplate 或 endpoint host 配置都必须经 security / no-egress / no-real-http review。
- NQ 只能记录 DH summary，不得触发 order / risk / ledger / Paper / LIVE mutation。

### Option D：先做 PR 合并 mock baseline，再另起 runtime API / contract review

结论：**推荐选项**。

推荐理由：

- IMP0-IMP3 与 mock close review 已把 test-support / mock-only baseline 收口，适合先作为可审查、可回滚的 PR 里程碑固定。
- PR prep 可以明确 diff scope、Maven 验证、no-runtime/no-real-http/no-provider/no-LIVE/no-mutation 边界，降低后续 runtime review 混入 test-support baseline 的风险。
- runtime API / contract review 应在 mock baseline PR prep 之后单独开启，避免把 test-support 结果误读为 runtime authorization。

```text
RECOMMENDED_OPTION: Option D
WHY: 先固定 mock/test-support baseline，再单独评审 runtime API/contract/security；这样能把可合并证据和未来 runtime 授权严格分离。
BLOCKERS: runtime endpoint shape; NQ_DRYRUN production allowlist; canonical error enum/schema; schema alias/envelope; NQ real HTTP client policy; rollback/kill-switch design.
REQUIRED_REVIEWS_BEFORE_IMPLEMENTATION: API/contract/security review; source allowlist review; error taxonomy/schema review; endpoint review; no-side-effect review; PR pre-merge review; rollback/kill-switch review.
PROHIBITED_CAPABILITIES: real HTTP; real provider; API/Controller implementation; schema/contracts/golden_cases modification; NQ mutation; order/risk/ledger/Paper/LIVE mutation; Agent/LangGraph runtime.
```

## 4. API / Endpoint / Source Allowlist 规划

当前 DH API 状态：

```text
Dry-run Controller: NOT IMPLEMENTED
Dry-run API path: NOT IMPLEMENTED
OpenAPI extension: NOT IMPLEMENTED
JSON Schema extension: NOT IMPLEMENTED
Runtime HTTP: NOT STARTED
```

如果未来新增 DH dry-run endpoint，必须单独执行 `NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW`，并至少冻结：

- endpoint path / method / auth / rate limit / payload size / status code。
- request / response envelope、canonical error enum、error schema、schema version。
- `NQ_DRYRUN` 是否进入 production source allowlist。
- feature flag default disabled 与 kill-switch 行为。
- fail-closed 语义和审计字段。
- 禁止返回可执行交易指令的合同断言。

`NQ_DRYRUN` 规划结论：

```text
NQ_DRYRUN production allowlist: NO in this task
Current state: test-support / review-gated source
Required change before production allowlist: separate security contract change
Failure mode before review: SOURCE_DENIED / fail-closed
```

未通过 review 前：

- 不得把 `NQ_DRYRUN` 写成 production allowlist。
- 不得用 `POST /api/ai/feedback/nq` 复用为 dry-run decision endpoint。
- endpoint 不得接 real provider。
- endpoint 不得接 Agent / LangGraph。
- endpoint 不得读写 NQ DB。
- endpoint 不得返回 `BUY`、`SELL`、`PLACE_ORDER`、`CANCEL_ORDER` 或任何可执行交易指令。
- 所有失败必须 fail-closed。

## 5. NQ Runtime Boundary 规划

即使未来进入 limited dry-run runtime，NQ 侧也只能记录 DH summary，不得执行 DH output。

NQ 必须保持：

```text
no-order
no-cancel-order
no-risk-mutation
no-ledger-mutation
no-paper-run-start
no-live
no-credential-read
no-real-provider
no-NQ-DB-access-from-DH
```

`LONG_BIAS / SHORT_BIAS` 只能作为 read-only analytical bias，不得映射为：

```text
BUY
SELL
PLACE_ORDER
CANCEL_ORDER
order side
Paper Run input
LIVE execution input
```

如未来需要真实 HTTP client：

- feature flag 必须默认 off。
- 默认 profile 必须 no-outbound。
- 必须有 no-real-http / no-provider / no-credential / no-order side-effect tests。
- 必须单独通过 API / contract / security review。

## 6. PR / Branch Strategy

推荐策略：

```text
NQ-DH related NQ changes continue in E:/Project/nexus-quant-i1-dryrun / branch nq-dh-i1-dryrun.
NQ dev continues to carry NQ Gate mainline.
Future NQ-side NQ-DH changes should merge to dev through PR, not direct dev commit.
This turn does not create PR.
```

PR prep 前置条件：

- NQ worktree clean。
- rebase / merge latest dev。
- full backend test pass。
- Integration0 targeted tests pass。
- no real HTTP。
- no provider。
- no runtime。
- no LIVE。
- no order / risk / ledger / Paper / LIVE mutation。
- diff scope only expected docs/test-support unless separately reviewed。
- NQ dev scoped dirty diff for `docs/current/*NQ_DH*` and `docs/current/*INTEGRATION1*` remains empty before merge。

当前 PR prep 结论：

```text
ALLOW_MOCK_BASELINE_PR_PREP: YES
NEXT_PR_TASK: NQ-DH-I1-MOCK-BASELINE-PR-PREP
```

## 7. Rollback / Kill-switch / Feature Flag / Environment Isolation

limited runtime 前必须先设计：

- runtime feature flag：默认 disabled；无配置时 fail-closed。
- source allowlist kill-switch：可立即拒绝 `NQ_DRYRUN`。
- endpoint kill-switch：route disabled 时返回明确 fail-closed 状态，不进入 orchestrator。
- outbound kill-switch：NQ runtime client 默认 no-outbound。
- environment isolation：test-support、local、CI、manual dry-run、runtime profile 分离。
- rollback：删除 runtime endpoint / client / wiring 后，test-support baseline 仍可独立保留。
- audit：所有拒绝、kill-switch 命中和 no-side-effect assertion 必须可追踪。

## 8. Next Concrete Action

推荐下一步：

```text
NEXT_ACTION: NQ-DH-I1-MOCK-BASELINE-PR-PREP
```

后续再进入：

```text
NEXT_ACTION_AFTER_PR_PREP: NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW
```

两者都不得写生产代码、测试代码、API、Controller、Client、schema、contracts、golden_cases、migration、runtime wiring、real HTTP、real provider、AI / LangGraph 或 LIVE。`NQ-DH-I1-LIMITED-RUNTIME-WO` 只有在 API / contract / security review 关闭后才允许考虑。

## 9. Boundary Confirmation

本轮计划确认：

```text
Production code changed: NO
Test code changed: NO
contracts changed: NO
golden_cases changed: NO
API / Controller added: NO
migration added: NO
runtime started: NO
real HTTP started: NO
real provider connected: NO
NQ DB accessed: NO
credential read/output: NO
Agent / LangGraph runtime started: NO
LIVE enabled: NO
NQ dev modified: NO
```
