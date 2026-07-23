# DH Stage-QDR-7 B1 Source Normalization Blocker Fix

> task: `DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-BLOCKER-FIX`
> mode: `CODE_CHANGE + SECURITY_FIX + SOURCE_CONTRACT_ALIGNMENT + CONFIG_FAIL_CLOSED + UNIT_TESTS`
> endpoint: `POST /api/ai/decision-dry-runs`
> implementation scope: source normalization only
> verdict: `DONE / SOURCE_FIX_REVIEW_PENDING`

## 1. Fixed security boundary

`DecisionDryRunRuntimeProperties` 已移除 request source 的 lowercase normalization。唯一 canonical wire value 继续为 `NQ_DRYRUN`：request header/body 逐字符一致、case-sensitive、不得 trim、不得 alias；HMAC signature material 继续使用原始 `body.source` 与 raw body hash，未改变格式。

配置只允许移除单个条目的外层空白，随后必须精确等于 `NQ_DRYRUN`。`nq_dryrun`、mixed-case、`NQ-DRYRUN`、未知值、空白条目、CSV trailing empty item、非canonical tenant/source pair与pair/allowlist矛盾均在 Spring bean 创建时抛出 `IllegalArgumentException`；没有 lowercase/uppercase/case-insensitive fallback。

Production profile 保留既有空 source allowlist，表达受独立 feature/production/kill gate保护的no-source deny state；它不是一个可接受的source条目。该历史安全合同由`DecisionContractGapGuardTest`验证，未被本任务改写。

```text
CANONICAL_SOURCE_SEMANTICS: PASS / REVIEW_PENDING
CONFIG_CASE_PRESERVATION: PASS / REVIEW_PENDING
INVALID_CONFIG_FAIL_CLOSED: PASS / REVIEW_PENDING
WIRE_EXACT_MATCH: PASS / REVIEW_PENDING
HMAC_COMPATIBILITY: PASS / REVIEW_PENDING
TENANT_SOURCE_ISOLATION: PASS / REVIEW_PENDING
NO_ALIAS_FALLBACK: PASS / REVIEW_PENDING
SECURITY_BOUNDARY: PASS / REVIEW_PENDING
```

## 2. Code and test evidence

- Properties allowlist/pair lookup now performs exact set membership for request source.
- Configuration validation rejects noncanonical entries before runtime requests are admitted.
- HMAC tests retain canonical success and nonce replay, reject lowercase/alias/whitespace/header-body case mismatch, and prove source/body mutation invalidates an old signature.
- WebMvc tests verify exact source rejection through the Controller path without changing Controller or DTO production code.
- No request/response envelope, OpenAPI, nonce/replay semantics, rate limiter, idempotency, migration, Repository/JDBC, external HTTP, Provider, NQ, Agent, LangGraph or LIVE behavior changed.

## 3. Validation evidence

```text
targeted source regression: PASS
  DecisionDryRunRuntimePropertiesTest
  DecisionDryRunRuntimeWiringConfigTest
  HmacNqDryRunAuthenticatorTest
  DecisionDryRunControllerWebMvcTest

full Maven test: PASS / Docker Testcontainers executed
quality validation: PENDING_FINAL_DOC_VALIDATION
capacity benchmark: NOT_RUN / OUT_OF_SCOPE
actual-wiring mock-only 2xx harness: NOT_READY / FOLLOW-UP_BLOCKER
```

## 4. Gate decision

```text
STAGE_QDR_7_B1_SOURCE_NORMALIZATION_BLOCKER_FIX: DONE
SOURCE_DRIFT_DISPOSITION: FIXED / REVIEW_PENDING
B1_RUNTIME_CONTRACT: FROZEN / SOURCE_FIX_REVIEW_PENDING
ALLOW_STAGE_QDR_7_B1_SOURCE_FIX_REVIEW: YES / NEXT_TASK_ONLY
ALLOW_STAGE_QDR_7_B2_SCHEMA_SECURITY_REVIEW_NOW: NO
ALLOW_CAPACITY_BENCHMARK_RETRY_NOW: NO
ALLOW_MIGRATION_NOW: NO
ALLOW_REPOSITORY_EXPANSION_NOW: NO
ALLOW_API_CHANGE_NOW: NO
ALLOW_OPENAPI_CHANGE_NOW: NO
ALLOW_REAL_HTTP: NO
ALLOW_REAL_PROVIDER: NO
ALLOW_PROVIDER_SDK: NO
ALLOW_NQ_RUNTIME_INTEGRATION: NO
ALLOW_AGENT_PHASE: NO
ALLOW_LANGGRAPH_RUNTIME: NO
ALLOW_LIVE: NO
next action: DH-STAGE-QDR-7-B1-SOURCE-NORMALIZATION-FIX-REVIEW
```

OpenAPI对既有endpoint的覆盖缺口仍是B4独立API/security acceptance gap，本轮未修改`contracts/openapi.yaml`。成功actual-wiring 2xx harness、persistent guards、测量容量默认值与B2/B4准入仍未完成。
