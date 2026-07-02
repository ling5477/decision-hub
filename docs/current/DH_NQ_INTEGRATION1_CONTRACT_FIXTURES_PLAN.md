# DH-NQ Integration-1 Contract Fixtures Plan

> 任务：`NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN`
> 类型：`PLAN_ONLY + CONTRACT_FIXTURES_PLAN + SCHEMA_GAP_REVIEW + GOLDEN_CASE_ALIGNMENT_PLAN + SECURITY_BOUNDARY + NO_RUNTIME + NO_LIVE`
> 日期：2026-07-02
> 仓库视角：Decision Hub（DH）
> 状态：`COMPLETED / PLAN ONLY / NOT IMPLEMENTED`

## 1. 结论

本文件只规划后续 NQ-DH Integration-1 dry-run contract fixtures、schema gap、golden case 对齐、error taxonomy 和验证批次。`COMPLETED` 只表示规划完成；`PLAN ONLY` 表示不落地 fixture、schema、测试或 runtime；`NOT IMPLEMENTED` 表示本轮没有新增 API、Controller、client、provider、Repository、Service、migration、测试代码、fixture JSON、真实 HTTP 或 runtime wiring。

```text
NQ current main line: GateO.
NQ rebase input: GateN no-real public marketdata / exchange sandbox baseline.
DH baseline: DH-STAGE4-DECISION-PIPELINE-MVP / ACCEPTED / CLOSED.
NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED.
NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED.
Integration-1 implementation: NOT STARTED.
Integration-1 runtime: NOT STARTED.
Runtime integration: NOT STARTED.
Real HTTP: NOT STARTED.
Real provider: NOT STARTED.
Agent / LangGraph runtime: NOT STARTED.
LIVE: DISABLED.
```

下一步只允许进入：

```text
NQ-DH-I1-P3-NQ-DRYRUN-STUB-TEST-PLAN / NOT STARTED
```

## 2. 前置状态确认

本轮写入前已确认：

| 仓库 | HEAD | `git status --short` | 结论 |
| --- | --- | --- | --- |
| NQ | `39d55381fc9ca5dc66c9e6d805283bdf0707d4ff` | clean | latest commit 为 `docs(nq-dh): plan Integration-1 dry-run contract`，可继续 P2。 |
| DH | `5eb17c23880c23569ee680702375488efad337cc` | clean | latest commit 为 `docs(dh): plan Integration-1 dry-run contract`，可继续 P2。 |

前置状态满足：

```text
NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN: CLOSED / COMPLETED / PLAN ONLY.
NQ current main line: GateO.
DH current baseline: DH-STAGE4-DECISION-PIPELINE-MVP / ACCEPTED / CLOSED.
Integration-1 implementation: NOT STARTED.
Integration-1 runtime: NOT STARTED.
real HTTP: NOT STARTED.
real provider: NOT STARTED.
Agent / LangGraph: NOT STARTED.
LIVE: DISABLED.
```

## 3. 当前合同事实

### 3.1 `DecisionRequest`

当前 `contracts/json-schema/dh-decision-request.schema.json` 已支持：

```text
requestId
traceId
tenantId
source
decisionType = READ_ONLY_RECOMMENDATION
subject.symbol / subject.market / subject.timeframe
subject.strategyRef / subject.researchRef
contextRef
contextSnapshot.snapshotId / capturedAt / evidenceRefs
requestedAt
schemaVersion = 1.0.0
```

当前 schema 为 `additionalProperties=false`。因此 future request fixture 不得把 `dryRun`、顶层 `evidenceRefs`、`runRef` 或任意未审查字段写成当前已支持字段。

### 3.2 `DecisionOutput`

当前 `contracts/json-schema/dh-decision-output.schema.json` 已支持：

```text
requestId
traceId
tenantId
decisionType = READ_ONLY_RECOMMENDATION
action = ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS
status = ABSTAINED / OBSERVATION_ONLY / DIRECTIONAL_BIAS / BLOCKED / INVALID
riskLevel = LOW / MEDIUM / HIGH / BLOCKED / UNKNOWN
policyStatus = ALLOWED / DENIED / REVIEW_REQUIRED / INVALID / BLOCKED
providerStatus = NOT_CALLED / MOCKED / SUCCESS / TIMEOUT / FAILED / UNTRUSTED / BUDGET_EXCEEDED / DISABLED
forbiddenActions = PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
reasonCodes
evidenceRefs
createdAt
schemaVersion = 1.0.0
```

当前 schema 同样为 `additionalProperties=false`。因此 `decisionId`、`confidence`、`traceSummary`、`replayRef`、`auditRef`、`dryRun` 都是 future envelope / extension 规划项，不是当前 response fixture 可直接使用的字段。

### 3.3 DH golden cases

当前 `golden_cases/decision/**` 是 DH 内部 deterministic decision baseline，覆盖 no-trade、no-evidence、provider timeout、provider budget exceeded、policy blocked、high risk、replay tenant isolation、mock NQ dry-run 等内部回归。它们不是 NQ-DH 跨仓 shared fixture，不得被 NQ 当作真实交易信号或 runtime evidence。

## 4. Schema gap review

| 项 | 分类 | 当前事实 | P2 结论 |
| --- | --- | --- | --- |
| `requestId / traceId / tenantId` | `EXISTS_NOW` | request/output schema 与 domain 均已存在。 | future fixtures 必须保持 header/body/auth/audit 绑定一致。 |
| `source` 字段 | `EXISTS_NOW` | request schema 已存在。 | `NQ_DRYRUN` 作为 future allowlist value 仍需 review。 |
| `NQ_DRYRUN` source value | `GAP_NEEDS_CONTRACT_REVIEW` | 当前 Integration-0 test allowlist 不是该值。 | P3/P4 前需单独确定 source allowlist。 |
| `decisionType=READ_ONLY_RECOMMENDATION` | `EXISTS_NOW` | request/output schema 与 domain 固定单值。 | future fixtures 必须固定。 |
| `subject / contextSnapshot.evidenceRefs` | `EXISTS_NOW` | request schema 已支持。 | 可作为 no-evidence / high-risk / evidence refs 的当前承载方式。 |
| `dryRun` wire field | `GAP_NEEDS_CONTRACT_REVIEW` | request/output schema 均不存在。 | 不得创建含 `dryRun` 的 current-schema fixture；可用 `source=NQ_DRYRUN` 做 `DOC_ONLY_ALIAS` 候选。 |
| `decisionId` response field | `GAP_NEEDS_CONTRACT_REVIEW` | DH internal persistence/replay 有 `decisionId`，但 output schema 不含该字段。 | 若进入 wire-level response，必须单独 schema/envelope review。 |
| `confidence` response field | `GAP_NEEDS_CONTRACT_REVIEW` | DH internal replay/persistence 和旧 contract 语境有 confidence，但 `DecisionOutput` schema 不含该字段。 | 不得写入 current output fixture。 |
| `traceSummary` | `GAP_NEEDS_CONTRACT_REVIEW` | 当前 output schema 不含该字段。 | 后续只能作为 envelope 或 NQ 本地 metadata 候选。 |
| `replayRef` | `GAP_NEEDS_CONTRACT_REVIEW` | DH replay read model 已存在，但 output schema 不含 wire reference。 | 只能规划，不能在 current fixture 中直接写入。 |
| `auditRef` | `GAP_NEEDS_CONTRACT_REVIEW` | DH audit records 已存在，但 output schema 不含 wire reference。 | 只能规划，不能在 current fixture 中直接写入。 |
| `X-NQ-DH-Schema-Version` | `GAP_NEEDS_CONTRACT_REVIEW` | Integration-0 canonical header 不含该 header；schema version 当前在 body。 | 若加入 header，必须 review header/body 一致性和签名材料。 |
| `DecisionAction` vocabulary | `EXISTS_NOW` | `ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS`。 | `LONG_BIAS / SHORT_BIAS` 仅为只读倾向，不是 `BUY / SELL`。 |
| `ForbiddenAction` fixed set | `EXISTS_NOW` | `PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB`。 | output fixture 必须完整包含五项。 |
| `BUY / SELL / quantity / price / leverage` | `PROHIBITED` | 不在 request/output 合同内。 | 命中即 fail-closed；不得作为扩展候选。 |
| `placeOrder / cancelOrder / paperRunStart / liveRunStart / mutateRisk / mutateLedger` | `PROHIBITED` | 与 DH/NQ 边界冲突。 | 不允许加入 dry-run 合同。 |

## 5. NQ -> DH request fixture plan

未来 request fixture 默认路径建议：

```text
NQ owning repo:
backend/nq-app/src/test/resources/nqdh/i1/dryrun/request/<future_filename>.json
```

P2 不创建这些文件。进入 P3/P4 前，必须先确认 source allowlist、header harness、signature fixture 生成方式和是否允许 schema extension。

| Future filename | Owning repo | Purpose | Required fields | Prohibited fields | Expected status / action | Schema support now | Contract review before implementation |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `valid_read_only_recommendation_request.json` | NQ | happy path request shape。 | `requestId/traceId/tenantId/source/decisionType/subject/contextSnapshot/requestedAt/schemaVersion` | credential/account/order/quantity/price/side/mutation fields | accepted for mock-only DH decision | body yes; `source=NQ_DRYRUN` value needs allowlist review | yes for source allowlist |
| `missing_signature.json` | NQ | 缺少 `X-NQ-DH-Signature` fail-closed。 | valid body + all canonical headers except signature | raw secret/signature material in body | reject `SIGNATURE_INVALID` or auth-equivalent | body yes; header harness outside schema | yes |
| `invalid_signature.json` | NQ | HMAC mismatch fail-closed。 | valid body + canonical headers + invalid signature placeholder | raw secret/signature material | reject `SIGNATURE_INVALID` | body yes; header harness outside schema | yes |
| `timestamp_skew.json` | NQ | timestamp 超窗或非 UTC `Z` fail-closed。 | valid body + timestamp variant | epoch seconds/millis as accepted value | reject; current INT0 uses `TIMESTAMP_INVALID` / `TIMESTAMP_OUT_OF_WINDOW` | body yes | yes for `TIMESTAMP_SKEW` name |
| `nonce_replay.json` | NQ | nonce/requestId replay fail-closed。 | valid body + repeated nonce/requestId identity | mutation or credential fields | reject; current INT0 uses `REPLAY` audit `REPLAY_REJECTED` | body yes | yes for `NONCE_REPLAY` name |
| `source_denied.json` | NQ | source not allowlisted。 | valid body with denied source | credentials/orders | reject; current INT0 uses `SOURCE_NOT_ALLOWED` | body yes | yes for source code name |
| `payload_too_large.json` | NQ | payload size gate fail-closed。 | representative oversize payload metadata | raw full oversize body in committed fixture if unsafe | reject `PAYLOAD_TOO_LARGE` | body fixture may need generated harness | yes |
| `rate_limited.json` | NQ | tenant/source/route rate limit fail-closed。 | valid body + repeated rate-limit context | threshold/counter internals in output | reject `RATE_LIMITED` | body yes; behavior harness needed | yes |
| `tenant_mismatch.json` | NQ | header/body/auth tenant mismatch。 | valid body + mismatched tenant header | credential/order fields | reject `TENANT_MISMATCH` | body yes; header harness needed | no schema change, yes fixture review |
| `forbidden_credential_field.json` | NQ | credential field rejection。 | valid minimal body plus one forbidden credential field for negative test | real credential values | reject `FORBIDDEN_FIELD` | intentionally invalid by schema/policy | yes |
| `forbidden_order_field.json` | NQ | order identifiers rejection。 | valid minimal body plus `orderId/clientOrderId` sentinel | real order ids | reject `FORBIDDEN_FIELD` | intentionally invalid | yes |
| `forbidden_account_field.json` | NQ | account identifiers rejection。 | valid minimal body plus `accountId/subAccountId` sentinel | real account ids | reject `FORBIDDEN_FIELD` | intentionally invalid | yes |
| `forbidden_quantity_price_side_field.json` | NQ | quantity/price/side rejection。 | valid minimal body plus `quantity/price/leverage/side=BUY` sentinel | tradable quantities/prices | reject `FORBIDDEN_FIELD` | intentionally invalid | yes |
| `forbidden_mutation_intent.json` | NQ | mutation intent rejection。 | valid minimal body plus `placeOrder/cancelOrder/mutateRisk/mutateLedger` sentinel | any executable command payload | reject `FORBIDDEN_FIELD` or capability-denied equivalent | intentionally invalid | yes |
| `duplicate_request_id.json` | NQ | same requestId idempotency / conflict plan。 | two logical requests with same requestId | mutation/credential fields | same payload idempotent; changed payload fail-closed | body yes; idempotency harness needed | yes |
| `no_evidence_request.json` | NQ | no-evidence fail-closed。 | valid body with `contextSnapshot.evidenceRefs=[]` | top-level unsupported `evidenceRefs` | DH response should `ABSTAIN` | body yes | no schema change |
| `high_risk_context_request.json` | NQ | high-risk evidence blocks directional bias。 | valid body with high-risk evidence refs | execution hints | DH response should `ABSTAIN` / no directional bias | body yes | no schema change |
| `forbidden_order_field_with_body_only.json` | NQ | schema/policy should reject order field even without header issue。 | valid headers + invalid body | real order ids | reject before provider/decision path | intentionally invalid | yes |

## 6. DH -> NQ response fixture plan

未来 response fixture 默认路径建议：

```text
DH owning repo:
dh-usecase/src/test/resources/nqdh/i1/dryrun/response/<future_filename>.json
```

P2 不创建这些文件。当前可直接支持的 response fixture 必须只使用 `DecisionOutput` schema 已存在字段；带 `decisionId/confidence/replayRef/auditRef/dryRun` 的 fixture 只能作为 schema gap negative fixture 或后续 envelope review 输入。

| Future filename | Owning repo | Purpose | Required fields | Prohibited fields | Expected status / action | Schema support now | Contract review before implementation |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `valid_abstain_response.json` | DH | no-evidence / fail-closed happy negative。 | current output required fields + fixed forbiddenActions | decisionId/confidence/replayRef/auditRef/dryRun/order fields | `status=ABSTAINED`, `action=ABSTAIN` | yes | no schema change |
| `valid_observe_response.json` | DH | observation-only recommendation。 | current output fields | trading side/order fields | `status=OBSERVATION_ONLY`, `action=OBSERVE` | yes | no schema change |
| `valid_no_trade_response.json` | DH | explicit no-trade recommendation。 | current output fields | trading side/order fields | `status=OBSERVATION_ONLY`, `action=NO_TRADE` | yes | no schema change |
| `valid_long_bias_readonly_response.json` | DH | read-only long analytical bias。 | current output fields, `riskLevel=LOW/MEDIUM`, `policyStatus=ALLOWED` | `BUY`, order side, quantity, price | `status=DIRECTIONAL_BIAS`, `action=LONG_BIAS` | yes | no schema change, NQ no-execution review required |
| `valid_short_bias_readonly_response.json` | DH | read-only short analytical bias。 | current output fields, `riskLevel=LOW/MEDIUM`, `policyStatus=ALLOWED` | `SELL`, order side, quantity, price | `status=DIRECTIONAL_BIAS`, `action=SHORT_BIAS` | yes | no schema change, NQ no-execution review required |
| `policy_denied_blocked_response.json` | DH | policy denied fail-closed。 | current output fields | directional bias, trading fields | `status=BLOCKED`, `policyStatus=DENIED`, `action=ABSTAIN` | yes | no schema change |
| `provider_disabled_fail_closed_response.json` | DH | provider disabled fail-closed。 | current output fields, `providerStatus=DISABLED` | provider raw response/secret | `action=ABSTAIN` | yes | no schema change |
| `provider_timeout_fail_closed_response.json` | DH | provider timeout fail-closed。 | current output fields, `providerStatus=TIMEOUT` | provider raw response/secret | `action=ABSTAIN` | yes | no schema change |
| `provider_budget_exceeded_response.json` | DH | provider budget exceeded fail-closed。 | current output fields, `providerStatus=BUDGET_EXCEEDED` | provider budget internals/secrets | `action=ABSTAIN` | yes | no schema change |
| `risk_blocked_response.json` | DH | risk blocks directional bias。 | current output fields, `riskLevel=HIGH/BLOCKED` | directional bias when high/blocked | `action=ABSTAIN`, no long/short bias | yes | no schema change |
| `internal_fail_closed_response.json` | DH | unknown/internal failure fail-closed。 | current output fields, safe reason code | stack trace/raw error/secret | `action=ABSTAIN`, `status=INVALID` or `ABSTAINED` | yes for shape; exact reason code gap | yes for code name |
| `replay_audit_reference_response.json` | DH | replay/audit refs planning。 | current output fields plus planned `replayRef/auditRef` only in gap variant | treating refs as execution credential | current schema rejects extra fields | no; gap fixture only | yes, mandatory |
| `schema_gap_response_fields_disallowed.json` | DH | prove gap fields are rejected before review。 | current output fields plus `decisionId/confidence/replayRef/auditRef/dryRun` | any trading field | reject `CONTRACT_INVALID` / schema invalid | intentionally invalid | yes |

## 7. Golden case alignment

- DH `golden_cases/decision/**` 是 DH 内部 deterministic regression / eval baseline。
- NQ-DH dry-run fixtures 是跨仓 contract baseline，目标是 wire shape、header/auth binding、schema gap、error taxonomy 和 no-side-effect。
- 两者不得混用为同一种证据：golden case 不能证明 NQ-DH wire contract 已支持，contract fixture 也不能替代 DH 内部 eval baseline。
- 未来若需要扩展 golden cases，必须另起任务；本轮不修改 `golden_cases/**`。
- NQ 不得把 DH golden case 当作真实交易信号、策略建议、Paper Run 输入或 LIVE 输入。
- Golden case 只允许用于 deterministic regression、contract shape reference 和 no-live boundary proof。

## 8. Error taxonomy alignment

| P2 planned code | 当前事实 | 对齐状态 | P2 结论 |
| --- | --- | --- | --- |
| `AUTH_FAILED` | DH feedback controller 当前对未授权使用 `UNAUTHORIZED_NQ_FEEDBACK`，NQ 其他 credential 文档有 `AUTH_FAILED`。 | `GAP_NEEDS_CONTRACT_REVIEW` | 需要统一为 dry-run auth code 或记录 alias。 |
| `SIGNATURE_INVALID` | DH/NQ Integration-0 validators 已使用。 | `EXISTS_NOW` | 可作为 future fixture expected code。 |
| `TIMESTAMP_SKEW` | 当前 INT0 使用 `TIMESTAMP_INVALID` / `TIMESTAMP_OUT_OF_WINDOW`。 | `DOC_ONLY_ALIAS` | P2 记录 gap；后续 review 决定是否改名。 |
| `NONCE_REPLAY` | 当前 INT0 使用 errorCategory `REPLAY`，audit event `REPLAY_REJECTED`。 | `DOC_ONLY_ALIAS` | P2 记录 gap；后续 review 决定 canonical name。 |
| `SOURCE_DENIED` | 当前 INT0 使用 `SOURCE_NOT_ALLOWED`。 | `DOC_ONLY_ALIAS` | P2 记录 gap；后续 review 决定是否改为 `SOURCE_DENIED`。 |
| `PAYLOAD_TOO_LARGE` | DH/NQ Integration-0 validators 已使用。 | `EXISTS_NOW` | 可直接规划。 |
| `RATE_LIMITED` | DH feedback rate limit 与 INT0 audit event 已使用。 | `EXISTS_NOW` | 可直接规划，但 dry-run route 维度需 review。 |
| `CONTRACT_INVALID` | 当前有 schema validation / `INVALID_SCHEMA` 等语义，但 code name 不统一。 | `GAP_NEEDS_CONTRACT_REVIEW` | 不改 enum/schema/code，本轮只记录 gap。 |
| `FORBIDDEN_FIELD` | DH/NQ Integration-0 validators 与 feedback validation 已使用。 | `EXISTS_NOW` | 可直接规划。 |
| `TENANT_MISMATCH` | DH/NQ Integration-0 validators 已使用。 | `EXISTS_NOW` | 可直接规划。 |
| `PROVIDER_DISABLED` | `providerStatus=DISABLED` 与 reason code 已存在。 | `EXISTS_NOW` | 用 response reason/providerStatus 表达，不新增 error enum。 |
| `PROVIDER_TIMEOUT` | `providerStatus=TIMEOUT` 和 provider health reason 已存在。 | `EXISTS_NOW` | 用 response reason/providerStatus 表达。 |
| `PROVIDER_BUDGET_EXCEEDED` | `providerStatus=BUDGET_EXCEEDED` 和 reason code 已存在。 | `EXISTS_NOW` | 用 response reason/providerStatus 表达。 |
| `RISK_BLOCKED` | DH decision audit event / reason code 已存在。 | `EXISTS_NOW` | 可用于 fail-closed response reason。 |
| `INTERNAL_FAIL_CLOSED` | 当前有 `DECISION_FAILED` / `PERSISTENCE_FAILED` 等语义。 | `GAP_NEEDS_CONTRACT_REVIEW` | 后续 review 决定 canonical name；本轮不改代码。 |

## 9. No-runtime / no-live fixture boundary

所有 future fixtures 均不得包含：

```text
real URL
real credential
accountId
orderId
clientOrderId
venue credential
broker credential
quantity / price / leverage / BUY / SELL
placeOrder / cancelOrder
paperRunStart / liveRunStart
mutateRisk / mutateLedger
provider secret
exchange key
raw signature material
raw prompt / full context
raw provider response
```

如果为了 negative fixture 需要表达 forbidden field，只能使用 synthetic sentinel key/value，并确保不会被误认为真实账号、真实订单、真实凭证或真实交易参数。

## 10. 后续批次

| Batch | 目标 | 仍然禁止 |
| --- | --- | --- |
| `I1-P3-NQ-DRYRUN-STUB-TEST-PLAN` | 规划 NQ 侧 stub / no-outbound / no-order 测试，不写真实 client。 | 不接 DH runtime、不触发 order/risk/ledger/Paper/LIVE。 |
| `I1-P4-DH-DRYRUN-ENTRY-PLAN` | 规划 DH 侧 dry-run 入口需求，不实现 Controller/API。 | 不新增 API path、Controller、migration。 |
| `I1-P5-JOINT-MOCK-VALIDATION-PLAN` | 规划联合 mock 验证，不真实 HTTP。 | 不启动 NQ/DH runtime，不真实 HTTP。 |
| `I1-P6-IMPLEMENTATION-GATE-REVIEW` | 判断 P0-P5 是否足以允许 implementation。 | 即使进入 implementation，也仍禁止 LIVE、real provider、自动下单。 |

## 11. Readiness decision

```text
ALLOW_I1_P2_CONTRACT_FIXTURES_PLAN_CLOSE: YES
ALLOW_I1_P3_NQ_DRYRUN_STUB_TEST_PLAN: YES
ALLOW_SCHEMA_CHANGE: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_INTEGRATION1_DRYRUN_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 12. 边界确认

本 P2 未修改生产代码；未修改测试代码；未修改 `contracts/**`；未修改 `golden_cases/**`；未创建 fixture JSON；未新增 API；未新增 migration；未新增 Controller / Client / Repository / Service；未真实 HTTP；未启动 runtime；未读取 credential；未接 provider；未接 AI / LangGraph；未进入 Paper Run；未进入 LIVE；未让 DH 输出进入 order、risk mutation、paper/live trading 或 private trading 路径。
