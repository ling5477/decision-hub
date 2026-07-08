# NQ-DH Integration-1 M2 NQ Dry-run Stub Recorder Work Order（DH）

> Task: NQ-DH-I1-M2-NQ-DRYRUN-STUB-RECORDER-WO
> Status: COMPLETED / WORK_ORDER_ONLY / NQ_DRYRUN_STUB_RECORDER_PLANNED / NOT IMPLEMENTED
> Date: 2026-07-03
> Repository: Decision Hub
> Source of truth: docs/current

## 1. 目标与边界

本文件记录 NQ M2 dry-run stub recorder work order 对 DH 的只读依赖，以及后续 M3 joint mock fixtures and contract tests work order 的前置条件。DH 本轮不实现 entry，不新增 API / Controller / OpenAPI path，不修改 schema、contracts、golden_cases、fixture JSON、生产代码或测试代码。

本工单不是 DH implementation，不授权 runtime endpoint、真实 HTTP、real provider、RealClient、AI / Agent runtime、LangGraph runtime、NQ runtime connection、NQ DB access、NQ mutation 或 LIVE。

## 2. 前置边界确认

```text
DH repository: F:\project\decision-hub
DH branch: dev
DH HEAD: 82fa6d9036bc7b2a883e1495967d7c5d7dcc11cf
DH precheck: clean

NQ dry-run worktree: F:\worktrees\nexus-quant-i1-dryrun
NQ dry-run branch: nq-dh-i1-dryrun
NQ dry-run HEAD: 8c44683162c63d2d349d6679e210214d7634ae06
NQ dry-run precheck: clean

NQ dev repository: F:\project\nexus-quant
NQ dev branch: dev
NQ dev HEAD: e62f1e437b5fee9a9e7193f1f777e3e49b343f28
NQ dev precheck: clean
NQ dev NQ-DH / Integration-1 dirty diff: none
WORKSTREAM_MIXED_BLOCKED: NO
```

## 3. DH 对 NQ M2 的只读依赖

NQ M2 只能消费 DH M1 的以下 planning 结论：

```text
DH entry shape: Option C / test-support mock-only / no runtime endpoint
NQ_DRYRUN source: NEEDS_SECURITY_CONTRACT_CHANGE / future source plan only
canonical error taxonomy: planning-only normalization, no enum/schema/code change
decisionId / confidence / traceSummary / replayRef / auditRef / X-NQ-DH-Schema-Version: DOC_ONLY_ALIAS
validation chain: 14 steps fixed in M1
fail-closed: mandatory
LONG_BIAS / SHORT_BIAS: read-only bias, not BUY / SELL
```

DH 本轮不提供 runtime endpoint，也不承诺 wire-level response 字段。NQ M2 recorder 只能按 mock-only / test-support summary 规划，不得假定 DH 已有可调用 HTTP API。

## 4. NQ M2 对 DH 的反馈结论

```text
RECOMMENDED_STUB_SHAPE: test-support mock-only stub + in-memory recorder plan, no runtime HTTP client
WHY_NO_REAL_HTTP_NOW: DH M1 no endpoint + NQ_DRYRUN/error taxonomy/schema alias still review-gated
RECORDER_SCOPE: record summary only, never execute
REQUEST_BUILDER_SCOPE: safe read-only context / fixture / test-support input only
NO_SIDE_EFFECT_TEST_PLAN: planned only, no tests created in M2
M2 close: YES
M3 work order allowed: YES, only as work-order-only
Implementation code allowed by M2: NO
```

## 5. M3 前置条件

后续 M3 只能进入：

```text
NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO / NOT STARTED / WORK_ORDER_ONLY
```

M3 已完成 work-order-only 收口，且未创建 fixture JSON、测试代码、schema/contracts/golden_cases、API、Controller、runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。M3 之后若需要真正创建 fixture 或测试，只能在受控 IMP0 或后续独立授权任务中执行。

## 6. Readiness decision

```text
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

## 7. DH 边界确认

DH 未新增 API path、Controller、OpenAPI、schema、contracts、golden_cases、fixture JSON、production code、test code、runtime endpoint、HTTP client、provider、AI runtime 或 LangGraph runtime。DH 不连接 NQ runtime、不访问 NQ DB、不修改 NQ 状态、不读取 credential、不输出 BUY / SELL / PLACE_ORDER / CANCEL_ORDER。

## 8. M3 后续收口

`NQ-DH-I1-M3-JOINT-MOCK-FIXTURES-AND-CONTRACT-TESTS-WO` 已于 2026-07-03 完成 work-order-only 收口。M3 只规划 future fixture family 与 future contract test batch；未创建 fixture JSON，未写测试代码，未修改 schema、contracts、golden_cases、API、Controller、runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。

下一步不再继续创建 M4/M5 大规划工单，只允许进入：

```text
NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED
```

## 9. 下一步

```text
NQ-DH-I1-IMP0-CONTRACT-GAP-TEST-SUPPORT-IMPLEMENTATION / NOT STARTED / CONTROLLED_IMPLEMENTATION_BATCH_ALLOWED
```

下一步是受控 test-support implementation batch，不是 runtime、真实 HTTP、real provider、AI / LangGraph 或 LIVE。
