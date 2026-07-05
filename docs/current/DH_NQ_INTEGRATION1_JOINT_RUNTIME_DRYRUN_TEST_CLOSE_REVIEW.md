# DH-NQ-INTEGRATION1-JOINT-RUNTIME-DRYRUN-TEST-CLOSE-REVIEW

日期：2026-07-05

## 1. 任务分类

```text
Task classification:
REVIEW_ONLY + JOINT_RUNTIME_DRYRUN_TEST_SECURITY_REVIEW + CROSS_REPO_CONTRACT_ALIGNMENT_REVIEW + NO_CODE_CHANGE + NO_REAL_DH_CALL + NO_REAL_HTTP + NO_REAL_PROVIDER + NO_LIVE

Repository:
DH dev: E:/Project/decision-hub
NQ worktree: E:/Project/nexus-quant-i1-dryrun
NQ dev: E:/Project/nexus-quant read-only

Result:
PASS / CLOSED / ACCEPTED / REVIEW_ONLY / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

本 review 只关闭已经实现的 DH / NQ joint runtime dry-run test 与 blocker fix 证据；不继续实现功能，不修改 Java 生产代码，不修改测试代码，不修改 contracts / OpenAPI / JSON Schema / golden_cases / migration，不真实调用 DH 服务，不真实 HTTP，不访问 localhost 真实服务，不访问外网，不接 provider，不开启 LIVE。

## 2. Review Result

```text
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

本结论只代表 fake / in-memory / MockMvc / test-only transport 级别的联合 dry-run 测试证据可关闭；不代表 Integration-1 runtime started，不代表 DH integrated，不允许 real DH call、real HTTP、real provider、schema/contracts/golden_cases formalization、Agent / LangGraph 或 LIVE。

## 3. Blocker Fix Review

结论：**PASS / FIXED / CLOSED**。

- `SIGNATURE_MATERIAL_SOURCE_NORMALIZATION_MISMATCH` 已修复：DH `HmacNqDryRunAuthenticator` 使用 wire-level canonical source value 参与 HMAC material，NQ signing material 同样使用 `NQ_DRYRUN`。
- `source=NQ_DRYRUN` 不被 lowercase / alias / fallback 重写。
- source allowlist 与 tenant/source pair 在验签后 exact match。
- lowercase source 与 alias source 均 denied。
- signature material mismatch 返回 `SIGNATURE_INVALID`。
- `SCHEMA_VERSION_MISMATCH` 已修复：NQ `DEFAULT_SCHEMA_VERSION=1.0.0`，接受 DH endpoint 实际 response `schemaVersion=1.0.0`。
- invalid schemaVersion 仍 fail-closed。
- 未修改 contracts / OpenAPI / JSON Schema / golden_cases / migration。

## 4. Boundary / Success / Fail-closed Review

结论：**PASS / TEST_ONLY / FAIL_CLOSED**。

已确认：

- 测试只使用 fake transport、in-memory helper、MockMvc、DH-style verifier 或 test-only vector。
- 未真实调用 DH。
- 未真实 HTTP。
- 未访问 localhost 真实服务或外网。
- 未接 provider。
- 未触碰 order / execution / risk / ledger / account / paper / live。
- 未开启 LIVE。
- 未引入 Agent / LangGraph。

成功路径覆盖：

- NQ signed dry-run request。
- canonical `X-NQ-DH-*` headers。
- no legacy `X-DH-NQ-*` headers。
- RFC3339 UTC `Z` timestamp。
- nonce unique。
- `dryRun=true`。
- `source=NQ_DRYRUN`。
- `forbiddenCapabilities` present。
- HMAC value-based material 与 DH 一致。
- DH MockMvc 接受有效请求并返回 readonly decision envelope。
- NQ 接受 `OBSERVE / NO_TRADE` record-only。
- NQ 接受 `LONG_BIAS / SHORT_BIAS` bias-only。
- 不输出 `BUY / SELL / PLACE_ORDER / CANCEL_ORDER`。
- 不触发交易状态变更。

fail-closed 覆盖：

- missing / invalid signature。
- epoch seconds / epoch milliseconds / non-UTC-Z timestamp。
- timestamp out of `+/-300s`。
- replay nonce。
- source denied。
- tenant mismatch。
- `dryRun=false`。
- forbidden `BUY / SELL / executableOrder`。
- payload too large。
- rate limit。
- memory cap。
- audit failure。
- client disabled / kill switch enabled / endpoint url missing。
- timeout / parse failure / DH error envelope。
- response `dryRun=false`、missing decisionId、invalid schemaVersion。
- `BUY / SELL / PLACE_ORDER` response。
- executable quantity / leverage / order price response。

## 5. Audit / Trace / Record Review

结论：**PASS / TRACEABLE / REDACTED**。

- DH 写 `auditRef`、`replayRef`、`traceSummary`。
- NQ 记录 `requestId`、`traceId`、`tenantId`、`decisionId`、`auditRef`。
- NQ 只记录 dry-run result。
- fail-closed reason 可追踪。
- 不记录 HMAC secret、token、cookie、apiKey、apiSecret、passphrase、raw credential 或 executable order payload。

## 6. Validation

| Scope | Result | Notes |
| --- | --- | --- |
| NQ `git diff --check` | PASS | close review 写入前通过；写入后仅允许 docs/current diff。 |
| NQ forbidden-scope diff | PASS / EMPTY | `backend/**/src/main`、migration、frontend、research、scripts、deploy、`.github`、contracts、golden_cases 无 diff。 |
| NQ Maven full backend | BUILD SUCCESS | `mvn -ntp -f backend/pom.xml test`，23/23 reactor SUCCESS。 |
| NQ Integration0 targeted | BUILD SUCCESS | 17 tests / 0 failures / 0 errors / 0 skipped。 |
| NQ Integration1 targeted | BUILD SUCCESS | 18 tests / 0 failures / 0 errors / 0 skipped。 |
| NQ dry-run targeted | BUILD SUCCESS | 30 tests / 0 failures / 0 errors / 0 skipped。 |
| NQ quality profile | PROFILE MISSING / NOT EFFECTIVE QUALITY GATE | `-Pquality validate` returned BUILD SUCCESS，但 profile 不存在，不声明 NQ quality PASS。 |
| DH `git diff --check` | PASS | close review 写入前通过；写入后仅允许 docs/current diff。 |
| DH forbidden-scope diff | PASS / EMPTY | `dh-domain/src/main`、`dh-usecase/src/main`、`dh-api/src/main`、`dh-app/src/main`、`dh-infra/src/main`、contracts、golden_cases、migration 无 diff。 |
| DH `mvn -ntp -pl dh-api -am test` | BUILD SUCCESS | 11/11 reactor SUCCESS；`dh-api` 55 tests。 |
| DH `mvn -ntp -pl dh-usecase -am test` | BUILD SUCCESS | 9/9 reactor SUCCESS；`dh-usecase` 179 tests。 |
| DH `mvn -ntp -Pquality validate` | BUILD SUCCESS | 19/19 reactor SUCCESS；Checkstyle / Spotless gate 通过。 |
| NQ dev read-only | SCOPED EMPTY / NOT CLEAN GATE | 任务输入声明存在非本轮 unrelated dirty；本 review 只确认 NQ-DH / Integration-1 scoped diff 为空，未修改 NQ dev。 |

## 7. Boundary Confirmation

```text
DH Java production code changed: NO
NQ Java production code changed: NO
Test code changed in this review: NO
NQ dev changed: NO
contracts/OpenAPI/json-schema changed: NO
golden_cases changed: NO
migration changed: NO
real DH call: NO
real HTTP: NO
localhost real service access: NO
external network access: NO
real provider: NO
credential read: NO
secret output: NO
Agent / LangGraph: NO
LIVE enabled: NO
order/execution/risk/ledger/account/paper/live touched: NO
LONG_BIAS / SHORT_BIAS mapped to BUY / SELL: NO
Runtime integration started: NO
DH integrated: NO
```

## 8. Risks

- NQ `quality` profile missing，不能写成 quality gate PASS。
- contracts / OpenAPI / JSON Schema / golden_cases 仍未 formalize；本 review 不授权现在修改。
- broad `rg` 命中包含历史/否定语境和测试断言，必须结合 scoped diff 判断。
- real DH call、real HTTP、provider、LIVE、Agent / LangGraph 仍需后续独立 gate；当前不得启动。
- NQ dev unrelated dirty 不属于本任务范围；后续仍必须在 `E:/Project/nexus-quant-i1-dryrun` 专用 worktree 推进。

## 9. Next Concrete Action

```text
NQ-DH-I1-INTEGRATION1-MOCK-RUNTIME-CLOSE-REVIEW
```
