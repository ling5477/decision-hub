# Decision Hub 当前工单

> 当前阶段: DH-STAGE-QDR-3-MODEL-GATEWAY-PROMPT-VERSION-PLAN / DONE / PLANNING_ONLY / IMPLEMENTATION_NOT_STARTED
> 已关闭: DH-CODEX-WORKFLOW conflict cleanup; Integration-0 safety gate; P1-4 residual; header alignment; timestamp alignment; Stage4 Decision Pipeline MVP; Integration-1 dry-run plan baseline; I1-P0 factsource rebase; I1-P1 contract dry-run plan; I1-P2 contract fixtures plan; I1-P3 dry-run implementation readiness plan; I1-P4 implementation gate review fix; I1 dry-run mock implementation work order; I1-M0 contract gap close work order; I1-M1 DH dry-run contract entry mock work order; I1-M2 NQ dry-run stub recorder work order; I1-M3 joint mock fixtures and contract tests work order; I1-IMP0 contract gap test-support implementation; I1-IMP1 DH dry-run test-support entry; I1 runtime API contract review; I1 DH runtime API work order; I1 DH limited runtime endpoint implementation; I1 DH limited runtime endpoint close review; I1 NQ runtime client work order
> 下一阶段: DH-STAGE-QDR-3-IMPLEMENTATION-WORK-ORDER / READY / WORK_ORDER_ONLY / NO_IMPLEMENTATION_YET / NO_REAL_PROVIDER

## 1. 当前目标

`stage-qdr-2 = Audit Trace Read Model + Human Approval Packet` 当前已完成 Work Order、B1 read model DTO / query contract、B2 tenant-bound read repository / read-only API、B3 human approval migration / domain / repository、B4 human approval API / audit，以及 B5 close review / acceptance；B1-B4 均已 `CLOSED / ACCEPTED / COMMITTED`，B5 close review 已 `YES / ACCEPTED`。完整工单入口为 `docs/current/DH_STAGE_QDR_2_WORK_ORDER.md`，discipline closeout 入口为 `docs/current/DH_STAGE_QDR_2_DISCIPLINE_CLOSEOUT.md`。stage-qdr-2 implementation 为 `DONE`，final close 为 `CLOSED`。stage-qdr-3 planning 已完成，计划入口为 `docs/current/DH_STAGE_QDR_3_MODEL_GATEWAY_PROMPT_VERSION_PLAN.md`；下一步只允许编写 stage-qdr-3 implementation work order，不得直接做 implementation。

stage-qdr-1 已按当前前置状态关闭：

```text
stage-qdr-1 implementation: DONE
stage-qdr-1 freeze: CLOSED / ACCEPTED
V6 decision_request / decision_run / quant_signal / quant_decision: EXISTS
POST /api/ai/decision-dry-runs success path: writes QDR four tables
V5 dh_decision_* audit chain: retained
```

stage-qdr-2 Work Order 后续拆分为：

```text
Batch 1: DH-STAGE-QDR-2-B1-READMODEL-QUERY-DESIGN-AND-DTO / CLOSED / ACCEPTED / COMMITTED
Batch 2: DH-STAGE-QDR-2-B2-READMODEL-REPOSITORY-AND-API / CLOSED / ACCEPTED / COMMITTED
Batch 3: DH-STAGE-QDR-2-B3-HUMAN-APPROVAL-MIGRATION-AND-DOMAIN / CLOSED / ACCEPTED / COMMITTED / NO_API
Batch 4: DH-STAGE-QDR-2-B4-HUMAN-APPROVAL-API-AND-AUDIT / CLOSED / ACCEPTED / COMMITTED
Batch 5: DH-STAGE-QDR-2-B5-CLOSE-REVIEW / YES / ACCEPTED / REVIEW_ONLY
```

stage-qdr-2 Work Order 不授权一次性全量实现，不授权真实 provider、真实 HTTP、LangGraph / AutoGen / CrewAI、Agent runtime、NQ mutation 或 LIVE。

### 当前禁止项

```text
禁止修改 V1-V6 历史 migration
禁止修改 V7 human_approval_packet migration
禁止扩大 B4 approval API / approval write endpoint 到 replay、provider、NQ 或交易语义
禁止把 B5 close review 写成新增功能实现
禁止新增 replay execution API 实现
禁止新增 model_call / prompt_template / prompt_version / tool registry
禁止接真实 HTTP / real provider / OpenAI / Anthropic / Gemini / Ollama SDK
禁止接 LangGraph / AutoGen / CrewAI
禁止直接启动 stage-qdr-3 implementation
禁止跳过 DH-STAGE-QDR-3-IMPLEMENTATION-WORK-ORDER
禁止修改 NQ 仓库
禁止触碰交易、订单、撤单、账户、ledger、risk、paper、live mutation
禁止把 LONG_BIAS / SHORT_BIAS 映射为 BUY / SELL
禁止把 APPROVED 映射为 BUY
禁止把 REJECTED 映射为 SELL
禁止把 real HTTP / real provider / Agent / LangGraph 写成 started
```

### 验收命令

```powershell
git status --short
git branch --show-current
git diff --check
git diff --stat
git diff --name-only
rg -n "GateK|GateL|GateM|stage-qdr|stage" docs/current
rg -n "BUY|SELL|PLACE_ORDER|CANCEL_ORDER|MARKET_ORDER|LIMIT_ORDER|placeOrder|cancelOrder|submitOrder|executeOrder|bypassRisk|forceExecute|mutateLedger|mutateRisk|paperRunStart|liveRunStart|apiKey|apiSecret|passphrase|credential|token|cookie|LangGraph|AutoGen|CrewAI|OpenAI|Anthropic|Gemini|Ollama|HttpClient|WebClient|RestTemplate|OkHttp" dh-* docs/current contracts golden_cases
mvn -ntp -pl dh-app -am test
mvn -ntp -Pquality validate
.\mvnw.cmd -v
```

若本机 `mvnw` wrapper 不可用，允许使用已安装 Maven `mvn` 执行同等命令，并在验证记录中说明降级原因。

## 0.0W DH-STAGE-QDR-3-MODEL-GATEWAY-PROMPT-VERSION-PLAN（DONE / PLAN_ONLY / WO NEXT）

本轮只规划 `stage-qdr-3 = Model Gateway + Prompt/Model Version Baseline`；不修改 Java 生产代码、测试代码、migration、API、repository、service、controller，不实现 Model Gateway，不接真实 provider，不接真实 HTTP，不启动 Agent / LangGraph / LIVE。

```text
Plan artifact: docs/current/DH_STAGE_QDR_3_MODEL_GATEWAY_PROMPT_VERSION_PLAN.md
stage-qdr-3 planning: DONE / PLAN_ONLY
stage-qdr-3 implementation: NOT STARTED
model gateway implementation: NOT STARTED
prompt version implementation: NOT STARTED
provider API: NOT STARTED
real HTTP: NO
real provider: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

下一步唯一允许动作：

```text
DH-STAGE-QDR-3-IMPLEMENTATION-WORK-ORDER / READY / WORK_ORDER_ONLY / NO_IMPLEMENTATION_YET
```

该 work order 必须继续保持 mock-only / contract / versioning baseline，不得直接接真实 provider、真实 HTTP、Provider SDK、LangGraph / AutoGen / CrewAI 或 LIVE。

## 0.0V DH-STAGE-QDR-2-FINAL-CLOSE-DOCS-SYNC（DONE / STAGE-QDR-3 PLAN READY）

本轮只把 `DH-STAGE-QDR-2-B5-CLOSE-REVIEW` 的 `ACCEPTED` 结论写回 `docs/current` 与 root README；不修改 Java 生产代码、测试代码、migration、API、repository、service、controller，不启动 stage-qdr-3 implementation。

```text
Current DH workspace: F:/Project/decision-hub
stage-qdr-2 implementation: DONE
stage-qdr-2 close review: YES
stage-qdr-2 acceptance: ACCEPTED
stage-qdr-2 final close: CLOSED
stage-qdr-3 planning: READY
stage-qdr-3 implementation: NOT STARTED
mvnw.cmd: UNUSABLE / P2 TOOLING RISK
Docker/Testcontainers: ENVIRONMENT_SKIP_RISK / named pipe access denied / skip is not PASS
real HTTP: NO
real provider: NO
Agent / LangGraph: NO
LIVE: DISABLED
```

下一步唯一允许动作：

```text
DH-STAGE-QDR-3-MODEL-GATEWAY-PROMPT-VERSION-PLAN / READY / PLANNING_ONLY / NO_IMPLEMENTATION
```

禁止直接做 `DH-STAGE-QDR-3` implementation；禁止接真实 provider、真实 HTTP、Agent runtime、LangGraph runtime、多 Agent runtime 或 LIVE。

## 0.0U DH-STAGE-QDR-2-DISCIPLINE-CLOSEOUT（DONE / B5 READY）

本轮只关闭 B4 freeze 后、B5 close review 前的工程纪律缺口；不实现 B5，不新增业务代码、测试代码、API、Controller、migration、replay execution、provider、真实 HTTP、Agent / LangGraph runtime 或 LIVE。

```text
Current DH workspace: E:/Project/decision-hub
Historical path only: F:/project/decision-hub
stage-qdr-2 B1: CLOSED / ACCEPTED / COMMITTED
stage-qdr-2 B2: CLOSED / ACCEPTED / COMMITTED
stage-qdr-2 B3: CLOSED / ACCEPTED / COMMITTED
stage-qdr-2 B4: CLOSED / ACCEPTED / COMMITTED
stage-qdr-2 B5: READY / NOT STARTED / REVIEW_ONLY
stage-qdr-2 overall: IMPLEMENTED_PENDING_CLOSE_REVIEW
stage-qdr-3: NOT STARTED
mvnw.cmd: UNUSABLE / P2 TOOLING RISK
Docker CLI: PASS / daemon reachable
Testcontainers: ENVIRONMENT_SKIP / named pipe access denied
```

后续 review 触发条件只保留 migration / 表结构变化、API / Controller 变化、auth / tenant / HMAC / nonce / source allowlist 变化、audit fail-closed / approval / replay 变化、stage close / acceptance、P0/P1 blocker fix。普通批次只要求 implementation、tests、boundary scan、minimal docs/current sync 和 commit。

下一步唯一允许动作：

```text
DH-STAGE-QDR-2-B5-CLOSE-REVIEW / READY / NOT STARTED / REVIEW_ONLY
```

## 0.0T DH-STAGE-QDR-2-B4-HUMAN-APPROVAL-API-AND-AUDIT（DONE / IMPLEMENTED_BY_VALIDATION）

本轮实现 stage-qdr-2 第四个批次 B4：Human Approval Packet 的 API 层、tenant-bound create approval packet、tenant-bound get approval packet、tenant-bound submit approval decision、审计事件写入、WebMvc tests、service tests、wiring 与 architecture guard。

实现边界：

```text
HumanApprovalPacketCommandService: IMPLEMENTED / USECASE_ONLY / TENANT_BOUND / AUDIT_FAIL_CLOSED
ApprovalWriteBoundary: IMPLEMENTED / USECASE_PORT / APP_TRANSACTION_BACKED_WHEN_AVAILABLE
HumanApprovalPacketController: IMPLEMENTED / AUTHENTICATED / TENANT_CONTEXT_ONLY
POST /api/ai/decision-runs/{decisionRunId}/approval-packets: IMPLEMENTED / CREATE_PENDING_ONLY
GET /api/ai/approval-packets/{approvalPacketId}: IMPLEMENTED / READ_ONLY / TENANT_BOUND
POST /api/ai/approval-packets/{approvalPacketId}/decision: IMPLEMENTED / STATE_MACHINE_ENFORCED
Audit events: IMPLEMENTED / dh_decision_audit_event
OpenAPI formalization: NOT UPDATED
New migration: NO
V7 migration modified: NO
Replay execution API: NOT STARTED
B5 close review: NOT STARTED
```

结论：

```text
STAGE_QDR_2_B4: DONE
STAGE_QDR_2_IMPLEMENTATION_OVERALL: PARTIAL
ALLOW_STAGE_QDR_2_B4_REVIEW_FREEZE: YES
ALLOW_STAGE_QDR_2_B5_CLOSE_REVIEW: NO
ALLOW_STAGE_QDR_2_FULL_IMPLEMENTATION_NOW: NO
ALLOW_REPLAY_EXECUTION_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

B4 未新增 migration、未修改 `V7__human_approval_packet.sql` 或 V1-V6 历史 migration；未新增 replay execution API；未新增 model_call、prompt_template / prompt_version、tool_definition / tool_invocation；未接真实 HTTP、real provider、OpenAI / Anthropic / Gemini / Ollama SDK、LangGraph / AutoGen / CrewAI；未修改 NQ；未开启 LIVE；未触碰交易、订单、撤单、账户、ledger、risk、paper 或 live mutation。`APPROVED` 只表示人工审查状态，不是 `BUY`；`REJECTED` 不是 `SELL`；approval API 只改变 DH 内部 approval 状态并写审计事件。

## 0.0S DH-STAGE-QDR-2-B2-READMODEL-REPOSITORY-AND-API（DONE / IMPLEMENTED）

本轮实现 stage-qdr-2 第二个批次 B2：tenant-bound read repository、read-only detail API、read-only trace API、unit/WebMvc tests、architecture guard 与 docs/current 最小同步。

实现边界：

```text
DecisionReadModelService: IMPLEMENTED / USECASE_ONLY / UUID_FAIL_CLOSED
JdbcDecisionReadModelQueryAdapter: IMPLEMENTED / JDBC_READONLY / EXISTING_V5_V6_TABLES_ONLY
DecisionRunReadController: IMPLEMENTED / GET_DETAIL_AND_TRACE / AUTHENTICATED / TENANT_BOUND
GET /api/ai/decision-runs/{decisionRunId}: IMPLEMENTED / READ_ONLY
GET /api/ai/decision-runs/{decisionRunId}/trace: IMPLEMENTED / READ_ONLY / NO_REPLAY_EXECUTION
OpenAPI formalization: NOT UPDATED
human_approval_packet: NOT STARTED
approval API: NOT STARTED
replay read API: NOT STARTED
```

结论：

```text
STAGE_QDR_2_B2: DONE
STAGE_QDR_2_IMPLEMENTATION_OVERALL: PARTIAL
ALLOW_STAGE_QDR_2_B3_WO_OR_IMPLEMENTATION: YES_AFTER_B2_COMMIT
ALLOW_STAGE_QDR_2_FULL_IMPLEMENTATION_NOW: NO
ALLOW_DB_MIGRATION_NOW: NO
ALLOW_APPROVAL_WRITE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

B2 未新增 migration、`human_approval_packet`、approval API、approval write、replay execution API、model_call、prompt_template / prompt_version、tool_definition / tool_invocation、真实 HTTP、real provider、LangGraph / AutoGen / CrewAI、NQ 修改或 LIVE。B3/B4 仍必须单独授权。

## 0.0R DH-STAGE-QDR-2-B1-READMODEL-QUERY-DESIGN-AND-DTO（DONE / IMPLEMENTED_BY_VALIDATION）

本轮只实现 stage-qdr-2 第一个批次 B1：usecase-level Audit Trace Read Model DTO / projection、tenant-bound query contract、fail-closed validation、unit tests、architecture guard 与 docs/current 最小同步。

实现边界：

```text
DecisionRunDetailView: IMPLEMENTED / READMODEL_ONLY
DecisionTraceTimelineView: IMPLEMENTED / READMODEL_ONLY
DecisionTraceStepView: IMPLEMENTED / READMODEL_ONLY
DecisionEvidenceView: IMPLEMENTED / REDACTED_REFS_ONLY
RedactionStatus: IMPLEMENTED / NO_RAW_UNREDACTED_VALUES
DecisionRunReadQuery: IMPLEMENTED / TENANT_BOUND
DecisionTraceReadQuery: IMPLEMENTED / TENANT_BOUND / NO_REPLAY_EXECUTION
DecisionEvidenceReadQuery: IMPLEMENTED / TENANT_BOUND / DEFAULT_REDACTED
DecisionReadModelQueryPort: IMPLEMENTED / CONTRACT_ONLY / NO_JDBC_IMPLEMENTATION
```

结论：

```text
STAGE_QDR_2_B1: DONE
STAGE_QDR_2_IMPLEMENTATION_OVERALL: PARTIAL
ALLOW_STAGE_QDR_2_B2_WO_OR_IMPLEMENTATION: YES
ALLOW_STAGE_QDR_2_FULL_IMPLEMENTATION_NOW: NO
ALLOW_API_IMPLEMENTATION: NO
ALLOW_DB_MIGRATION: NO
ALLOW_APPROVAL_WRITE: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

B1 未新增 Controller、OpenAPI path、API implementation、JDBC read repository、migration、`human_approval_packet`、approval API、replay API、真实 HTTP、real provider、LangGraph / AutoGen / CrewAI、NQ 修改或 LIVE。B2 才允许在单独授权下实现 read repository / API。

## 0.0Q NQ-DH-I1-MOCK-RUNTIME-PR-PREP（READY / PR_PREP_ONLY）

本轮只准备 NQ PR 材料与合并边界，不实现功能、不改 DH / NQ Java、不改测试、不改 contracts / OpenAPI / JSON Schema / golden_cases / migration、不真实调用 DH、不真实 HTTP、不接 provider、不接 Agent / LangGraph、不启用 LIVE。

结论：

```text
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

下一步唯一推荐：

```text
NQ-DH-I1-MOCK-RUNTIME-PR-CREATE / NOT STARTED / PR_CREATE_ONLY / NO_MERGE / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

## 0.0P NQ-DH-I1-INTEGRATION1-MOCK-RUNTIME-CLOSE-REVIEW（PASS / CLOSED / ACCEPTED）

本轮只做 Integration-1 mock runtime / test-only 里程碑关闭审查与工作纪律复位，不新增 WO，不实现功能，不修改 Java 生产代码或测试代码，不改 contracts / OpenAPI / JSON Schema / golden_cases / migration，不真实调用 DH，不真实 HTTP，不接 provider，不开启 LIVE。

结论：

```text
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

下一步唯一推荐：

```text
NQ-DH-I1-MOCK-RUNTIME-PR-PREP / NOT STARTED / PR_PREP_ONLY / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

## 0.0O NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-CLOSE-REVIEW（PASS / CLOSED / ACCEPTED）

本轮只做 review-only close review，不修改 DH / NQ Java production code，不修改测试代码，不改 contracts / OpenAPI / JSON Schema / golden_cases / migration，不真实调用 DH，不真实 HTTP，不接 provider，不开启 LIVE。

结论：

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

下一步唯一推荐：

```text
NQ-DH-I1-INTEGRATION1-MOCK-RUNTIME-CLOSE-REVIEW / REVIEW_ONLY / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

## 0.0N NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-BLOCKER-FIX（IMPLEMENTED / FULL_VALIDATION_PASS / CLOSED_BY_CLOSE_REVIEW）

本轮只修复 source normalization 与 schemaVersion 两个 blocker。DH 侧 HMAC 验签材料已改为使用 wire-level canonical source value；source allowlist 改为验签后 exact match；tenant/source pair 校验使用 wire source；lowercase / alias source 不得通过；signature material mismatch 仍 `SIGNATURE_INVALID`。DH endpoint 当前 response `schemaVersion=1.0.0` 仍为 runtime test source of truth；NQ response validator / tests 已对齐该值；invalid schemaVersion 仍 fail-closed。

结论：

```text
BLOCKER_SIGNATURE_MATERIAL_SOURCE_NORMALIZATION_MISMATCH: FIXED
BLOCKER_SCHEMA_VERSION_MISMATCH: FIXED
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

## 0.0M NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-IMPLEMENTATION（IMPLEMENTED / BLOCKER_FIX_APPLIED_BY_0.0N）

本轮产物：

```text
dh-api/src/test/java/com/guidinglight/decisionhub/api/decision/DecisionDryRunControllerWebMvcTest.java
dh-security/src/test/java/com/guidinglight/decisionhub/security/nq/HmacNqDryRunAuthenticatorTest.java
E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\integration\dh\DhDryRunJointRuntimeDryRunTest.java
E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\integration\dh\DhDryRunResponseHandlingTest.java
E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\integration\dh\DhDryRunTestSupport.java
```

结论：

```text
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_BLOCKER_FIX_CLOSE: YES
ALLOW_JOINT_RUNTIME_DRYRUN_TEST_CLOSE_REVIEW: YES / PASS / CLOSED / ACCEPTED
BLOCKER_SIGNATURE_MATERIAL_SOURCE_NORMALIZATION_MISMATCH: FIXED_BY_0.0N
BLOCKER_SCHEMA_VERSION_MISMATCH: FIXED_BY_0.0N
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

关键边界：

- 本轮只实现测试与 docs/current 同步；未改 DH / NQ 生产代码。
- DH 侧仅扩展 MockMvc 与 HMAC authenticator 回归；NQ 侧仅扩展 fake transport / response validation / test support 类型 seam。
- 未真实调用 DH，未真实 HTTP，未访问 localhost 真实服务，未接 provider，未接 AI / LangGraph，未开启 LIVE。
- 未修改 contracts / OpenAPI / JSON Schema / golden_cases / migration。
- 未触碰 order / execution / risk / ledger / account / paper / live。

下一步唯一推荐：

```text
NQ-DH-I1-INTEGRATION1-MOCK-RUNTIME-CLOSE-REVIEW / REVIEW_ONLY / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

## 0.0L NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-WO（CLOSED / ACCEPTED / WORK_ORDER_ONLY）

本轮产物：

```text
docs/current/DH_NQ_INTEGRATION1_JOINT_RUNTIME_DRYRUN_TEST_WO.md
E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_JOINT_RUNTIME_DRYRUN_TEST_WO.md
```

结论：

```text
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
```

关键边界：

- 本轮只写 joint runtime dry-run test implementation work order；未实现测试，未修改 Java 生产代码、测试代码、contracts、OpenAPI、JSON Schema、golden_cases、fixture JSON 或 migration。
- 下一轮只允许验证 NQ limited dry-run client 经 fake / in-memory / MockMvc / test-only transport 调用 DH `POST /api/ai/decision-dry-runs` 的 readonly envelope，并由 NQ 做 response validation 与 record-only result。
- 下一轮必须覆盖成功矩阵、DH side fail-closed、NQ side fail-closed、no-side-effect、audit / trace / record 与 error taxonomy；禁止 real outbound HTTP、真实 DH 地址、localhost 真实服务、外网、provider、Agent / LangGraph、LIVE 和交易副作用。

该 WO 已被 implementation 消费；当时下一步为：

```text
NQ-DH-I1-JOINT-RUNTIME-DRYRUN-TEST-IMPLEMENTATION / NOT STARTED / TEST_ONLY / FAKE_TRANSPORT_ONLY / NO_REAL_DH_CALL / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

## 0.0K NQ-DH-I1-NQ-RUNTIME-CLIENT-WO（CLOSED / ACCEPTED / WORK_ORDER_ONLY）

本轮 NQ 产物：

```text
E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_NQ_RUNTIME_CLIENT_WO.md
```

结论：

```text
DH_ENDPOINT: POST /api/ai/decision-dry-runs
DH_ENDPOINT_SCOPE: DH_ONLY_INBOUND_LIMITED_DRY_RUN
NQ_RUNTIME_CLIENT: NOT STARTED
NQ_RUNTIME_CLIENT_WO: CLOSED / ACCEPTED
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
```

关键边界：

- 本轮只写 NQ worktree work order 与 current docs 最小状态同步；未实现 NQ runtime client、未新增 HTTP client、未真实调用 DH、未改 NQ dev。
- 下一轮 NQ limited runtime client implementation 必须默认关闭、dev/test only、production disabled、kill switch fail-closed、no LIVE、no trading side effect、no provider call、no credential forwarding。
- `NQ_DRYRUN` 仍为 review-gated source，不进入 production allowlist；contracts/OpenAPI/schema/golden_cases/fixture JSON formalization 必须另起 review。
- `LONG_BIAS / SHORT_BIAS` 只能作为 readonly bias 记录，不得映射为 `BUY / SELL` 或进入 order / execution / risk / ledger / paper / live。

下一步唯一推荐：

```text
NQ-DH-I1-NQ-LIMITED-RUNTIME-CLIENT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION / DEFAULT_DISABLED / DEV_TEST_ONLY / NO_LIVE
```

## 0.0J NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-CLOSE-REVIEW（CLOSED / ACCEPTED / REVIEW_ONLY）

本轮产物：

```text
docs/current/DH_NQ_INTEGRATION1_DH_ENDPOINT_CLOSE_REVIEW.md
```

结论：

```text
ENDPOINT: POST /api/ai/decision-dry-runs
ENDPOINT_SCOPE: DH_ONLY_INBOUND_LIMITED_DRY_RUN
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
```

关键边界：

- close review 只审查 DH endpoint；未改 Java 生产代码、测试代码、contracts/OpenAPI/json-schema/golden_cases/fixture JSON/migration，未改 NQ dev 或 NQ dry-run worktree。
- endpoint 仍是 DH-only inbound limited dry-run，不包含 NQ runtime client implementation、真实 outbound HTTP、real provider、Agent / LangGraph runtime、LIVE 或 NQ mutation。
- 下一步 `NQ-DH-I1-NQ-RUNTIME-CLIENT-WO` 必须保持 work-order-only；不得夹带 NQ client implementation、真实 HTTP、real provider、schema/contracts/golden_cases 修改或 LIVE。

该下一步已由 §0.0K 消费并关闭为 work-order-only：

```text
NQ-DH-I1-NQ-RUNTIME-CLIENT-WO / CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_CLIENT_IMPLEMENTATION / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

## 0.0I NQ-DH-I1-DH-RUNTIME-API-WO（CLOSED / ACCEPTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION）

本轮产物：

```text
docs/current/DH_NQ_INTEGRATION1_DH_RUNTIME_API_WO.md
```

结论：

```text
FUTURE_ENDPOINT_CANDIDATE: POST /api/ai/decision-dry-runs
FUTURE_ENDPOINT_STATE_NOW: NOT IMPLEMENTED
NQ_DRYRUN_SOURCE: REVIEW_GATED / NOT_IN_PRODUCTION_ALLOWLIST
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
```

关键边界：

- 本 work order 只冻结下一轮 DH limited runtime endpoint implementation 的边界、文件范围、安全门槛、验收标准、禁止项、测试要求和回滚要求。
- endpoint 当前仍为 `NOT IMPLEMENTED`；本轮不新增 Controller、OpenAPI path、schema/contracts/golden_cases、fixture JSON、runtime wiring、Client、Service、Repository 或 migration。
- 下一轮 implementation 必须默认 feature flag disabled、dev/test only、production disabled，并强制 HMAC、UTC `Z` timestamp、±300s replay window、persistent nonce replay guard、tenant/source allowlist、requestId / traceId / tenantId binding、payload cap、rate limit、memory cap、fail-closed、redacted audit logging 和 kill switch。
- response 只能是 read-only decision snapshot；`LONG_BIAS / SHORT_BIAS` 只是 bias，不是 `BUY / SELL`；NQ 只能记录，不执行。
- 后续必须拆为 DH endpoint implementation、DH endpoint tests、NQ limited dry-run client WO、NQ client implementation、joint runtime dry-run tests、runtime close review。

下一步唯一推荐：

```text
NQ-DH-I1-DH-LIMITED-RUNTIME-ENDPOINT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION / FEATURE_FLAG_DISABLED_BY_DEFAULT / NO_REAL_HTTP / NO_PROVIDER / NO_LIVE
```

## 0.0H NQ-DH-I1-RUNTIME-API-CONTRACT-REVIEW（CLOSED / ACCEPTED / REVIEW_ONLY / NO_RUNTIME）

本轮产物：

```text
docs/current/DH_NQ_INTEGRATION1_RUNTIME_API_CONTRACT_REVIEW.md
E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_RUNTIME_API_CONTRACT_REVIEW.md
```

结论：

```text
RECOMMENDED_OPTION: Option D / freeze API contract, error taxonomy, envelope before split implementation
FUTURE_ENDPOINT_CANDIDATE: POST /api/ai/decision-dry-runs
FUTURE_ENDPOINT_STATE_NOW: NOT IMPLEMENTED
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
```

关键边界：

- 本轮 proposed endpoint 不是已实现 API，不得写入 OpenAPI 或 Controller。
- `NQ_DRYRUN` 仍为 review-gated source，不进入 production allowlist。
- canonical error taxonomy、runtime HMAC material、schema/envelope 字段、feature flag、kill switch、rate limit、payload cap、memory cap、persistent nonce namespace 均必须在后续 work order / implementation review 中冻结。
- 后续必须拆为 DH runtime API WO、DH endpoint implementation、NQ limited client WO、NQ client implementation、joint runtime tests 和 runtime close review。

下一步唯一推荐：

```text
NQ-DH-I1-DH-RUNTIME-API-WO / NOT STARTED / WORK_ORDER_ONLY / NO_RUNTIME_IMPLEMENTATION
```

## 0.0G NQ-DH-I1-LIMITED-DRYRUN-RUNTIME-PLAN（CLOSED / ACCEPTED / PLAN_ONLY / NOT_IMPLEMENTED / NO_RUNTIME）

本轮产物：

```text
docs/current/DH_NQ_INTEGRATION1_LIMITED_DRYRUN_RUNTIME_PLAN.md
E:\Project\nexus-quant-i1-dryrun\docs\current\NQ_DH_INTEGRATION1_LIMITED_DRYRUN_RUNTIME_PLAN.md
```

结论：

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

保留 review-gated 项：`NQ_DRYRUN` 未进入 production allowlist；canonical error enum / schema、dry-run endpoint shape、schema alias/envelope 字段和 NQ runtime client 均未通过独立 review。下一步是 mock baseline PR prep，不是 runtime work order。

## 0.0F NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS（IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_MOCK_CLOSE_REVIEW）

本轮产物位于 DH 与 NQ dry-run worktree 的测试范围：

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/DhIntegration1JointMockContractFixtureTest.java
dh-usecase/src/test/resources/nq-dh/integration1/joint_mock_contract_fixtures.json
E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\app\integration1\NqDhIntegration1JointMockContractFixtureTest.java
E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\resources\nq-dh\integration1\joint_mock_contract_fixtures.json
```

IMP3 只实现 joint mock fixtures / contract tests，不实现 runtime endpoint、Controller、OpenAPI path、schema alias、production source allowlist、真实 HTTP、provider 或 LIVE。结论如下：

```text
ALLOW_IMP3_CLOSE: YES
ALLOW_I1_MOCK_CLOSE_REVIEW: YES
ALLOW_PRODUCTION_CODE_CHANGE: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步唯一允许动作是 `NQ-DH-I1-MOCK-CLOSE-REVIEW / NOT STARTED / REVIEW_ONLY / NO_RUNTIME`。IMP3 不授权 `NQ_DRYRUN` 进入生产 allowlist，不授权 dry-run endpoint、真实 HTTP client、RealClient、real provider、AI / LangGraph runtime、NQ mutation 或 LIVE。

## 0.0E NQ-DH-I1-IMP2-NQ-STUB-RECORDER-NO-SIDE-EFFECT（VERIFY PASS / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_IMP3_JOINT_MOCK_CONTRACT_TESTS）

本轮产物位于 NQ dry-run worktree：

```text
E:\Project\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\app\integration1\NqDhIntegration1StubRecorderNoSideEffectTest.java
```

IMP2 只实现 NQ test-support / mock-only stub recorder guard，不实现 DH 代码、NQ production code、runtime endpoint、Controller、OpenAPI path、schema alias、fixture JSON、真实 HTTP、provider 或 LIVE。结论如下：

```text
ALLOW_IMP2_CLOSE: YES
ALLOW_I1_IMP3_JOINT_MOCK_CONTRACT_TESTS: YES
ALLOW_PRODUCTION_CODE_CHANGE: NO
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

该条已由 `NQ-DH-I1-IMP3-JOINT-MOCK-CONTRACT-TESTS / IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_MOCK_CLOSE_REVIEW` 消费。IMP3 不得被解读为 runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE 授权。

## 0.0D NQ-DH-I1-IMP1-DH-DRYRUN-TEST-SUPPORT-ENTRY（IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_VALIDATION）

本轮产物：

```text
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/support/DhDryRunTestSupportEntry.java
dh-usecase/src/test/java/com/guidinglight/decisionhub/usecase/decision/integration1/DhDryRunTestSupportEntryTest.java
```

IMP1 只实现 DH test-support / mock-only validation harness，不实现 runtime entry、Controller、OpenAPI path、schema alias、fixture JSON 或真实 HTTP。结论如下：

```text
ALLOW_IMP1_CLOSE: YES
ALLOW_I1_IMP2_NQ_STUB_RECORDER_NO_SIDE_EFFECT: YES
ALLOW_PRODUCTION_CODE_CHANGE: NO
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

IMP1 已由 IMP2 NQ test-support stub recorder 消费；不得据此启动 runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。

## 0.0C NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION（IMPLEMENTED / TEST_SUPPORT_ONLY / MOCK_ONLY / READY_FOR_REVIEW）

本轮产物：

```text
dh-domain/src/test/java/com/guidinglight/decisionhub/contracts/DecisionContractGapGuardTest.java
F:\worktrees\nexus-quant-i1-dryrun\backend\nq-app\src\test\java\com\guidinglight\nexusquant\app\integration1\NqDhIntegration1ContractGapGuardTest.java
```

IMP0 只实现 contract gap 的 test-support guard，不实现 runtime entry。结论如下：

```text
ALLOW_IMP0_CLOSE: YES
ALLOW_I1_IMP1_DH_DRYRUN_TEST_SUPPORT_ENTRY: YES
ALLOW_I1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步唯一允许动作是 `NQ-DH-I1-IMP1-DH-DRYRUN-TEST-SUPPORT-ENTRY / NOT STARTED / TEST_SUPPORT_ONLY / MOCK_ONLY`。IMP1 仍只能做 DH 侧 test-support entry，不得启动 runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。

## 0.0B NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO（COMPLETED / WORK_ORDER_ONLY / FINAL_WO_BEFORE_IMPLEMENTATION / NOT IMPLEMENTED）

工单产物：

```text
docs/current/DH_NQ_INTEGRATION1_M3_JOINT_MOCK_FIXTURES_AND_CONTRACT_TESTS_WO.md
```

本工单只规划 future joint mock fixture families 与 contract test batches。结论如下：

```text
Fixture families planned: 23
Contract test batches planned: 14
ALLOW_M3_WO_CLOSE: YES
ALLOW_I1_IMP0_CONTRACT_GAP_TEST_SUPPORT_IMPLEMENTATION: YES
ALLOW_MORE_PLANNING_WO: NO
ALLOW_I1_RUNTIME: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_SCHEMA_CHANGE: NO
ALLOW_CONTRACTS_MODIFICATION: NO
ALLOW_FIXTURE_IMPLEMENTATION: NO
ALLOW_GOLDEN_CASES_MODIFICATION: NO
ALLOW_API_CONTROLLER_CHANGE: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

下一步唯一允许动作是 `NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED`。IMP0 只允许 test-support / mock-only source handling、canonical error mapping test-support 与 fixture schema support guard；不得创建 M4/M5 大规划文档，不得启动 runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。

## 0.0A NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO（COMPLETED / WORK_ORDER_ONLY / NQ_DRYRUN_STUB_RECORDER_PLANNED / NOT IMPLEMENTED）

工单产物：

```text
docs/current/DH_NQ_INTEGRATION1_M2_NQ_DRYRUN_STUB_RECORDER_WO.md
```

本工单只规划 NQ dry-run stub / request builder / recorder 的后续实现工作订单。结论如下：

```text
RECOMMENDED_STUB_SHAPE: test-support mock-only stub + in-memory recorder plan, no runtime HTTP client
WHY_NO_REAL_HTTP_NOW: no DH runtime endpoint + NQ_DRYRUN / error taxonomy / endpoint shape / schema alias still review-gated
RECORDER_SCOPE: record summary only, never execute
REQUEST_BUILDER_SCOPE: safe read-only context / fixture / test-support input only
ALLOW_M2_WO_CLOSE: YES
ALLOW_I1_M3_JOINT_MOCK_FIXTURES_AND_CONTRACT_TESTS_WO: YES
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

M3 已完成 work-order-only 收口，当前下一步唯一允许动作是 `NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED`。IMP0 仍不允许直接创建 runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。

## 0.0 NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO（COMPLETED / WORK_ORDER_ONLY / DH_DRYRUN_ENTRY_PLANNED / NOT IMPLEMENTED）

工单产物：

```text
docs/current/DH_NQ_INTEGRATION1_M1_DH_DRYRUN_CONTRACT_ENTRY_MOCK_WO.md
```

本工单只规划 DH 侧 dry-run contract entry mock 的后续实现工作订单。结论如下：

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

M2 已完成 NQ dry-run stub recorder work order planning，M3 已完成 final work-order-only 收口。当前唯一允许动作是 `NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED`；不得直接进入 runtime implementation。

## 0. NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO（COMPLETED / WORK_ORDER_ONLY / CONTRACT_GAP_CLOSED / NOT IMPLEMENTED）

工单产物：

```text
docs/current/DH_NQ_INTEGRATION1_M0_CONTRACT_GAP_CLOSE_WO.md
```

本工单关闭 M0 contract gap，结论如下：

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

下一步唯一允许动作是 `NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO / NOT STARTED`。M1 仍是 work order / mock contract planning；如触及 schema、contracts、fixtures、OpenAPI、Controller、source allowlist 或 canonical error enum，必须另起 review，不能直接 implementation。

## 0.1 NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO（COMPLETED / WORK_ORDER_ONLY / NOT IMPLEMENTED）

工单产物：

```text
docs/current/DH_NQ_INTEGRATION1_DRYRUN_MOCK_IMPLEMENTATION_WO.md
```

本工单修正预检规则并拆分后续 M0-M4：

```text
M0 NQ-DH-I1-M0-CONTRACT-GAP-CLOSE-WO
M1 NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK
M2 NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER
M3 NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-CONTRACT-TESTS
M4 NQ-DH-I1-M4-DRYRUN-MOCK-CLOSE-REVIEW
```

本工单本身不授权代码、测试、fixture JSON、schema、contracts、golden_cases、API / Controller、migration、runtime、真实 HTTP、real provider、AI / Agent runtime、LangGraph runtime 或 LIVE。M0 已由 `docs/current/DH_NQ_INTEGRATION1_M0_CONTRACT_GAP_CLOSE_WO.md` 关闭；当前唯一下一步是 `NQ-DH-I1-M1-DH-DRYRUN-CONTRACT-ENTRY-MOCK-WO / NOT STARTED`。

`DH-STAGE4-DECISION-PIPELINE-MVP-PLAN` 已产出 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md`，状态为 `ACCEPTED / CLOSED`。`DH-STAGE4-DECISION-PIPELINE-MVP-WO` 已产出 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md`，状态为 `ACCEPTED / CLOSED`。K1-K7 已关闭；K8 acceptance / freeze 已 `CLOSED / ACCEPTED`，报告见 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_ACCEPTANCE_REPORT.md`，冻结快照见 `docs/gates/dh-stage4-decision-pipeline-mvp/`。Integration-1 dry-run plan 已基于 NQ GateN 完成 rebase；P0 只允许同步两仓事实源和旧 GateK 口径，不允许启动 Integration-1 runtime 或全量 runtime implementation：

```text
READ_ONLY_RECOMMENDATION
候选建议 / 风险解释 / evidence summary
DecisionRequest / DecisionOutput / DecisionTrace contract
Audit event shape contract
Policy denied / audit write failure / provider unavailable fail-closed 规则
ABSTAIN / NO_ACTION 语义
forbiddenActions 固化
验收清单
风险清单
```

语言治理要求：当前工单、后续 review 记录、WORKLOG、TESTING、STATUS、ROADMAP 正文必须中文为主；`DecisionOutput`、`READ_ONLY_RECOMMENDATION`、`ABSTAIN`、`forbiddenActions`、文件路径、命令和固定输出字段等稳定工程标识保留英文原样。

## 2. Stage1-CLOSE 已完成范围

```text
deprecation：旧 domain.run / api.run / usecase.facade / usecase.run / usecase.gate /
             usecase.contract / dh-providers 全部 @Deprecated(since="Stage1-CLOSE", forRemoval=true)
迁移：     api.run.RunController -> api.legacy.run.RunController，REST 路径 /runs -> /legacy/runs
契约：     contracts/openapi.yaml 中 /runs -> /legacy/runs，并标 deprecated
文档单源： 根 README + docs/current/{README,STATUS,ROADMAP,WORKLOG,WORK_ORDER,TESTING}.md
codex：    docs/codex/plans/_active/STATUS.json 切到 Stage1，老 M1 plan 归档到 _archive/2026-02-04_M1/
ArchUnit： 新增 4 条规则保护新边界
pom：      dh-eval parent 修回 dh-bom
```

## 3. DH-STAGE4-DECISION-PIPELINE-MVP-PLAN（计划产物）

本节记录已关闭的 planning artifact。`docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_PLAN.md` 已 `ACCEPTED / CLOSED`；后续 implementation 必须遵守 `docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md` 的 K1-K8 批次顺序，不得跳过 review 直接全量 implementation。

允许范围：

```text
输出 Decision Pipeline MVP PLAN 文档
定义 read-only recommendation 边界
定义 evidence / risk / policy / audit trace 字段
定义 DecisionResponse action vocabulary
定义 ABSTAIN / NO_ACTION / BLOCKED / POLICY_DENIED 语义
定义 forbiddenActions: PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
定义 audit write failure fail-closed 规则
定义 provider unavailable / no evidence fail-closed 规则
定义验收清单
定义风险清单
```

计划批次覆盖：

```text
K0 Factsource Sync / Docs Rebase
K1 Decision Contract Freeze
K2 DecisionOrchestrator Skeleton Plan
K3 Audit / Snapshot / Trace / Replay Plan
K4 Mock Provider / Provider Health Plan
K5 Mock NQ Dry-run Contract Test Plan
K6 Golden Cases / Eval Plan
K7 Acceptance / Freeze Plan
```

Readiness 推荐：

```text
ALLOW_STAGE4_PLAN_CLOSE: YES
ALLOW_STAGE4_WO: YES
ALLOW_DECISION_PIPELINE_IMPLEMENTATION: NO
ALLOW_INTEGRATION_1_DRYRUN_PLAN_REBASE_N: YES
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 4. DH-STAGE4-DECISION-PIPELINE-MVP-WO（工单产物）

本节记录已关闭工单。WO 已授权按 review gate 逐批 implementation；K1 已 `PASS / CLOSED / ACCEPTED`，K2 已 `IMPLEMENTED`，K3 已 `CLOSED / ACCEPTED after M1`，K4 已 `IMPLEMENTED / READY FOR NEXT`。WO 不授权 K5-K8 连续实施，不授权 Integration-1 runtime、Agent phase、LangGraph runtime 或 LIVE。

工单产物：

```text
docs/current/DH_STAGE4_DECISION_PIPELINE_MVP_WORK_ORDER.md
Status: ACCEPTED / CLOSED
K1 status: PASS / CLOSED / ACCEPTED
K2 status: IMPLEMENTED
K3 status: CLOSED / ACCEPTED after M1
M1 status: CLOSED / ACCEPTED
K4 status: CLOSED
K5 status: CLOSED
K6 status: CLOSED
K7 status: CLOSED
K8 status: CLOSED / ACCEPTED
Integration-1 dry-run plan: PLAN BASELINE ACCEPTED
I1-P0 factsource rebase: CLOSED / ACCEPTED / DOCS-ONLY
I1-P1 contract dry-run plan: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
I1-P2 contract fixtures plan: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
I1-P3 dry-run implementation readiness plan: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
I1-P4 implementation gate review fix: COMPLETED / DOCS-ONLY / GATE-FIX
Next: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
```

批次顺序：

```text
K1 Decision Contract Freeze
K2 DecisionOrchestrator Skeleton
K3 Audit / Snapshot / Trace Persistence
K4 Replay Read Model
K5 Mock Provider / Provider Health / Budget / Latency
K6 Mock NQ Dry-run Contract Tests
K7 Golden Cases / Eval 基线
K8 Acceptance / Freeze
```

顺序约束：

```text
K1 after review before K2
K2 after review before K3
K3 after M1 before K4
K4 after implementation before K5
K1-K5 complete before K6
K1-K7 complete before K8
Before Stage4 MVP closed: no Integration-1 runtime, no LangGraph runtime
LangGraph GateL or later
```

DecisionOutput 加固：

```text
decisionType = READ_ONLY_RECOMMENDATION
action only ABSTAIN / OBSERVE / NO_TRADE / LONG_BIAS / SHORT_BIAS
default ABSTAIN
no evidence -> ABSTAIN
provider failure -> ABSTAIN
policy denied -> BLOCKED or ABSTAIN, fail-closed
high risk forbids LONG_BIAS / SHORT_BIAS
forbiddenActions includes PLACE_ORDER / CANCEL_ORDER / MUTATE_NQ_STATE / READ_NQ_DB / WRITE_NQ_DB
structured JSON only
free-text final output forbidden
real trading instruction forbidden
```

Readiness 决策：

```text
ALLOW_WO_CLOSE: YES
ALLOW_K1_IMPLEMENTATION: YES
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_BATCH_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

当前状态必须保持：

```text
DH-AUDIT-FIX completed
NQ integration not started
Integration-0 safety gate CLOSED / ACCEPTED
Integration-1 implementation not started
Runtime integration not started
DH integrated NO
AI / Agent runtime not started
RealClient forbidden
real provider forbidden
real HTTP forbidden
LIVE trading forbidden
NQ mutation forbidden
Old NQ-DH-GATEK-INTEGRATION1-PLAN-PACK: SUPERSEDED / REBASE_REQUIRED
NQ current planning baseline: GateN
K2 DecisionOrchestrator Skeleton: IMPLEMENTED
K3 Audit / Snapshot / Trace Persistence: CLOSED / ACCEPTED after M1
M1 Readiness Review: CLOSED / ACCEPTED
K4 Replay Read Model: CLOSED
K5 Provider Health / Budget / Latency: CLOSED
K6 Mock NQ Dry-run Contract Tests: CLOSED
K7 Golden Cases / Eval: CLOSED
K8 Acceptance / Freeze: CLOSED / ACCEPTED
NQ-DH-INTEGRATION1-DRYRUN-PLAN-REBASEN: PLAN BASELINE ACCEPTED
NQ-DH-I1-P0-FACTSOURCE-REBASE-CONTINUE: CLOSED / ACCEPTED / DOCS-ONLY
NQ-DH-I1-P1-CONTRACT-DRYRUN-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P2-CONTRACT-FIXTURES-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P3-DRYRUN-IMPLEMENTATION-READINESS-PLAN: COMPLETED / PLAN ONLY / NOT IMPLEMENTED
NQ-DH-I1-P4-IMPLEMENTATION-GATE-REVIEW-FIX: COMPLETED / DOCS-ONLY / GATE-FIX
Next concrete action: NQ-DH-I1-DRYRUN-MOCK-IMPLEMENTATION-WO / NOT STARTED
```

## 5. DH-STAGE4-DECISION-PIPELINE-MVP-K2-ORCHESTRATOR-SKELETON（已实现）

K2 已在 `dh-usecase` 内完成 mock-only orchestrator skeleton；后续状态由 K3 / M1 readiness 入口接管。

K2 实施范围：

```text
DecisionOrchestrator / DefaultDecisionOrchestrator
DecisionContext / DecisionContextBuilder / DefaultDecisionContextBuilder
DecisionPolicyChecker / DefaultDecisionPolicyChecker
DecisionSignalProvider / DecisionSignalResult / MockDecisionSignalProvider
DecisionRiskReviewer / DefaultDecisionRiskReviewer
DecisionOutputAssembler
DecisionOutput observation / abstainForRisk factory
K2 unit tests
```

K2 明确未做：

```text
K3 audit / snapshot / trace / replay persistence
API path / Controller
Repository / JDBC / migration
real provider / OpenAI / Claude / Gemini / local model
LangGraph runtime
NQ runtime / real HTTP / RealClient
Integration-1 runtime
LIVE / trading / NQ mutation
```

## 6. DH-STAGE4-DECISION-PIPELINE-MVP-K3-AUDIT-SNAPSHOT-TRACE-PERSISTENCE（CLOSED / ACCEPTED after M1）

K3 已在 DH 仓库内完成 audit / snapshot / trace persistence，并已通过 M1 readiness review 关闭。

K3 实施范围：

```text
Flyway V5: dh_decision_request / dh_decision_context_snapshot /
           dh_decision_trace_step / dh_decision_provider_call_log /
           dh_decision_output / dh_decision_audit_event
DecisionAuditRepository usecase port
DecisionPersistenceRecords
InMemoryDecisionAuditRepository test/default fallback
JdbcDecisionAuditRepository
DecisionPipelineWiringConfig
DefaultDecisionOrchestrator persistence writes + fail-closed handling
K3 usecase / infra / migration tests
docs/current sync
```

K3 明确未做：

```text
K4 replay read model
replay API / Controller / query endpoint
real provider / OpenAI / Claude / Gemini / local model
LangGraph runtime
NQ runtime / real HTTP / RealClient
Integration-1 runtime
LIVE / trading / NQ mutation
NQ DB read/write
```

K3 readiness：

```text
ALLOW_K3_CLOSE: YES
ALLOW_K4_IMPLEMENTATION: YES
ALLOW_STAGE4_M1_CLOSE_REVIEW: YES
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 7. DH-STAGE4-DECISION-PIPELINE-MVP-K4-REPLAY-READ-MODEL（IMPLEMENTED / READY FOR NEXT）

K4 已在 DH 仓库内完成内部 Replay Read Model。该 read model 只读取 K3 已持久化的六类 DH-owned decision 记录，不重跑 provider、不重跑 orchestrator、不调用 NQ、不修改 audit 数据。

K4 实施范围：

```text
DecisionReplayView / Request / Context / TraceStep / ProviderCall / Output / AuditEvent / Timeline
DecisionReplayStatus
DecisionReplayQuery
DecisionReplayQueryRepository
DecisionReplayQueryService
DefaultDecisionReplayQueryService
JdbcDecisionReplayQueryRepository
DecisionPipelineWiringConfig replay repository / service wiring
K4 usecase / JDBC read / wiring tests
docs/current sync
```

K4 明确未做：

```text
replay API / Controller / query endpoint
new migration
K5 provider health / budget / latency
K6 mock NQ dry-run contract tests
K7 golden cases / eval
K8 acceptance / freeze
real provider / OpenAI / Claude / Gemini / local model
LangGraph runtime
NQ runtime / real HTTP / RealClient
Integration-1 runtime
LIVE / trading / NQ mutation
NQ DB read/write
```

K4 readiness：

```text
ALLOW_K4_CLOSE: YES
ALLOW_K5_IMPLEMENTATION: YES
ALLOW_STAGE4_M2_CLOSE_REVIEW: NO
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 8. DH-STAGE4-DECISION-PIPELINE-MVP-K5-PROVIDER-HEALTH-BUDGET-LATENCY（IMPLEMENTED / READY FOR NEXT）

K5 已在 DH 仓库内完成 mock-only provider health / budget / latency controls。该批次只强化 K2/K3/K4 已有 Decision Pipeline 的 provider guard 与 provider call summary，不新增 API、Controller、migration、真实 provider、HTTP、NQ runtime、LLM、LangGraph 或 LIVE。

K5 实施范围：

```text
DecisionProviderHealth / Budget / Latency / GuardResult / FailureClass value objects
DecisionProviderHealthEvaluator / DefaultDecisionProviderHealthEvaluator
DecisionProviderBudgetGuard / DefaultDecisionProviderBudgetGuard
DecisionProviderLatencyRecorder / DefaultDecisionProviderLatencyRecorder
DecisionProviderGuard / DefaultDecisionProviderGuard
DefaultDecisionOrchestrator provider pre/post guard integration
MockDecisionSignalProvider K5 failure-state test hooks
DecisionPipelineWiringConfig K5 guard wiring
provider call log signal_json / latency_ms / error_code summary
K4 replay provider call view regression
docs/current sync
```

K5 明确未做：

```text
K6 mock NQ dry-run contract tests
K7 golden cases / eval
K8 acceptance / freeze
new API path / Controller / replay API / query endpoint
new migration / provider health table / budget table / latency table
real provider / OpenAI / Claude / Gemini / local model
real HTTP / WebClient / RestTemplate / HttpClient
NQ runtime / RealClient / RealNqBacktestClient
Integration-1 runtime
LangGraph runtime
LIVE / trading / NQ mutation
NQ DB read/write
credential / token / API secret / passphrase access
```

K5 readiness：

```text
ALLOW_K5_CLOSE: YES
ALLOW_K6_IMPLEMENTATION: YES
ALLOW_STAGE4_M2_CLOSE_REVIEW: NO
ALLOW_FULL_STAGE4_IMPLEMENTATION_WITHOUT_MILESTONE_REVIEW: NO
ALLOW_INTEGRATION_1_RUNTIME: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
```

## 9. Historical / superseded / deferred 内容

```text
NqFeedbackClient 接通真实 HTTP / event         forbidden / deferred
NqBacktestClient 接通真实 NQ                    forbidden / deferred
NQ /api/ai/research/backtest-requests           forbidden / deferred
RealNqBacktestClient / RealClient               forbidden
real provider                                   forbidden
真实 HTTP / event 到 NQ                         forbidden
启动 Paper Run                                  forbidden
修改 NQ 交易状态                                forbidden
访问交易所密钥                                  forbidden
LIVE trading                                    forbidden
```

以上内容只保留为历史背景，不是当前 next，不允许作为当前实现任务。后续如需恢复，必须先基于 NQ GateN 重新进入 planning-only audit，并通过安全审查、契约冻结和人工确认。

## 10. 不做事项（持续硬约束）

```text
不修改 NQ 仓库交易核心
不实现真实下单
不绕过 NQ 风控
不复制 NQ 订单状态机
不重写 NQ 回测核心
不建设第二套完整前端
不引入 BCO/ACO/GWO 等重型数学优化器
不把 Kronos / TradingAgents / global-stock-data 整体复制进 DH/NQ
不新增 API / migration / provider / NQ client / RealClient / 交易路径
不读取 token / cookie / exchange secret / production .env / API key / private key / mnemonic / 2FA backup code
```

## 11. 下一轮 Codex 开工提示词草稿

```text
你在 decision-hub 仓库 dev 分支上工作。任务名：DH-STAGE4-DECISION-PIPELINE-MVP-K8-ACCEPTANCE-FREEZE。

目标：只基于 K1-K7 已完成 evidence 做 DH Stage4 Decision Pipeline MVP acceptance / freeze 审查与文档冻结记录。
判断是否允许 DH Stage4 Decision Pipeline MVP close；不要启动 Integration-1 runtime，不要实现任何 runtime integration。

禁止：
- 不修改生产代码
- 不新增测试代码，除非 K8 acceptance 发现必须用最小回归证明某个验收结论
- 不新增 API
- 不新增 migration
- 不新增 replay API / Controller / query endpoint
- 不实现真实 NQ client
- 不实现 RealClient
- 不接真实 HTTP / event 到 NQ
- 不调用 NQ /api/ai/research/backtest-requests
- 不接真实 LLM provider
- 不接 LangGraph runtime
- 不输出 BUY / SELL / PLACE_ORDER / CANCEL_ORDER 作为 action
- 不启动 Paper Run
- 不修改 NQ 交易状态
- 不访问交易所密钥
- 不触碰 LIVE trading
- 不读取或写入 NQ DB
- 不新增 provider / 交易路径

验收命令：
- git status --short
- git diff --check
- git diff --stat
- mvn -ntp test
- mvn -ntp -Pquality validate
- rg 边界扫描：RealClient / LangGraph / OpenAI / Claude / Gemini / WebClient / RestTemplate /
  HttpClient / placeOrder / cancelOrder / BUY / SELL / apiSecret / passphrase / accountId /
  Controller / NqClient / Exchange / Broker / live / LIVE / endpoint / Endpoint /
  PostMapping / GetMapping / RequestMapping
```
