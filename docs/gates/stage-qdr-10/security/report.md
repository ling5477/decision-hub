# Security Review: decision-hub

## Scope

Cumulative Stage-QDR-10 implementation and security-remediation committed exact diff.

- Scan mode: branch_diff
- Target kind: git_diff
- Target ID: target_sha256_522e609f9f885c22d5c1c86229ccff71a58c5cac88233fe46babdb9897b2ae79
- Revision range: 87304e334787d778b10ebad1b1b7f17057f47322...d275b9e30bb381bea8467286786add2c5b43e119
- Snapshot digest: codex-security-snapshot/v1:sha256:1d74df0bc5da366ec7aad16a4841552de3d91d1cb5319d4e849096130ccb54eb
- Inventory strategy: diff
- Included paths: .
- Excluded paths: none
- Runtime or test status: Existing committed evidence reports 1326 tests, PostgreSQL 17.10/Flyway V1-V15 and quality PASS; this security pass was read-only and did not rerun Maven.
- Artifacts reviewed: 28 exact committed-range full-file receipts, Stage-QDR-10 production, unit, architecture, wiring and PostgreSQL acceptance sources, Original sealed scan and its manifest/coverage/findings/report relationship, Current authority and blocker-history documents, Forbidden technical-scope diff
- Scan context: Fresh committed-range scan at exact head d275b9e; head Git tree aee96dcbba36020c8c92ea48f455d2be90216a7c. The prior sealed working-tree scan matched only 18 of 28 d275b9e files; 10 authority documents differed, so it was retained only as historical evidence.

Limitations and exclusions:
- Production/formal capacity was not executed and remains not proven.
- CodeRabbit CLI is unavailable and produced no review result.
- The old sealed working-tree scan is historical evidence only; this new scan supplies exact d275b9e committed-tree acceptance.

### Scan Summary

| Field | Value |
| --- | --- |
| Reportable findings | 0 |
| Severity mix | none |
| Confidence mix | none |
| Coverage | complete |
| Validation mode | Diff-scoped 28/28 full-file review with static candidate validation, attack-path closure, PostgreSQL/Testcontainers acceptance evidence, and forbidden-scope diff checks. |

Canonical artifacts: `scan-manifest.json`, `findings.json`, and `coverage.json`. This report is a deterministic projection of those files.

## Threat Model

Decision Hub is a tenant-scoped, fail-closed decision-support and evidence system. Its core objectives are exact tenant/environment provenance, bounded and transactionally coherent evidence, audit integrity, secret protection, and prevention of trading or mutable-learning side effects.

### Assets

- Tenant-scoped decision and feedback evidence
- Decision/request/run/audit/replay and persisted guard identities
- Execution-scope environment provenance
- Credentials, signatures, redacted provider material, and audit integrity
- NQ and trading safety boundary

### Trust Boundaries

- External HTTP caller to dh-api authentication and validation
- Signed NQ message to security and tenant/environment binding
- Use-case reader/writer to JDBC/PostgreSQL persistence
- Provider/model abstraction to disabled external-service boundary
- Governance authority to privileged scheduler/learning/NQ/Agent/LIVE controls

### Attacker Capabilities

- Submit malformed, replayed, cross-tenant, or cross-environment identifiers and feedback
- Cause conflicting, ambiguous, oversized, or partially available evidence states
- Attempt to exploit unsafe parsing, unbounded reads, or correlation gaps
- Influence human task routing through stale lower-priority documentation

### Security Objectives

- Require caller environment, persisted decision origin, and feedback environment equality
- Fail closed on missing, ambiguous, inconsistent, overflowed, unsafe, or storage-error evidence
- Prevent rejected-evidence disclosure and implicit learning writes
- Keep API/schema/migration/provider/NQ/Agent/LangGraph/Paper/LIVE boundaries unchanged
- Keep secrets and raw privileged payloads out of logs and persistence

### Assumptions

- FeedbackExecutionScope originates at the existing trusted caller boundary.
- Current governance keeps real HTTP/provider, NQ mutation, Agent/LangGraph, Paper and LIVE unauthorized.
- Production capacity and production readiness are not proven.

## Findings

### No findings

No reportable findings survived the canonical discovery, validation, and reportability gates.

## Reviewed Surfaces

| Surface | Risk Area | Outcome | Notes |
| --- | --- | --- | --- |
| 28 cumulative exact-diff files | Diff coverage | No issue found | All 28 changed production, test, and authority files have unique full-file completion receipts; deferred 0. |
| Persisted decision environment provenance | Cross-environment collision | No issue found | One parameterized bounded query requires COMPLETED guard plus exact V5/V6 and core request/run chain; missing, ambiguity and errors fail closed. |
| Caller, decision, and feedback environment equality | Tenant/environment isolation | No issue found | Positive DEV path passes; caller/decision or decision/feedback mismatch, missing and ambiguous provenance are unusable. |
| Policy-qualified bounded completeness | Silent truncation and completeness confusion | No issue found | Aggregate carries from/to/max/policy/version/order/overflow; 100 without next page is allowed while hasNext or the 101st row fails closed. |
| Rejected-evidence and side-effect containment | Data disclosure and learning poisoning | No issue found | Fail-closed states carry zero feedback; path is read-only and acceptance/architecture evidence shows no learning or mutable-store writes. |
| API, schema, migration, and runtime boundaries | Unauthorized capability expansion | No issue found | No API, migration, schema, contract, POM, workflow, NQ, HTTP/provider, Agent/LangGraph, Paper, LIVE, or trading mutation change is in the range. |
| Pre-existing lower stale authority strings | Governance routing ambiguity | Rejected | Candidate CAND-S5-STALE-CURRENT-AUTHORITY-001 was present at baseline, explicitly superseded by new first authority, and has no automatic execution or runtime sink. |
