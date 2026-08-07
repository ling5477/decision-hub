# Security Review: decision-hub

## Scope

Immutable diff security review for the published feedback-ingest atomicity implementation.

- Scan mode: branch_diff
- Target kind: git_diff
- Target ID: target_sha256_522e609f9f885c22d5c1c86229ccff71a58c5cac88233fe46babdb9897b2ae79
- Revision range: 16ecded2f3708d69e05afe5f2a4f621c823d8be3...1c41a85e94a48ded74ce7a71e65b45f8d239a12b
- Snapshot digest: codex-security-snapshot/v1:sha256:7f245c34fbccb210c617be71559cfe048261840e91958c4440102ec19d3f4bc8
- Inventory strategy: diff
- Included paths: .
- Excluded paths: none
- Runtime or test status: Static source review plus existing exact-SHA CI evidence; no new runtime or production-capacity test was executed in this close task.
- Artifacts reviewed: 9 changed production source files in the immutable diff, 14 exact-range threat-model, discovery, work-ledger, validation, and attack-path artifacts copied with SHA-256 equality, directly supporting repository, wiring, validation, transaction, integration, and architecture tests, GitHub Actions exact-SHA run 31184220520
- Scan context: Reviewed the exact range 16ecded2f3708d69e05afe5f2a4f621c823d8be3..1c41a85e94a48ded74ce7a71e65b45f8d239a12b. The review focused on transaction atomicity, validation ordering, strict JSON parsing, exact envelope-event correlation, single-statement READ COMMITTED snapshot semantics, rollback/retry behavior, and learning side-effect containment.

Limitations and exclusions:
- Production-sized correlation_id query-plan and capacity evidence was not executed.
- No new dynamic proof was run for the unchanged embedded textual-JSON duplicate-key behavior.
- Formal production capacity remains not proven.

### Scan Summary

| Field | Value |
| --- | --- |
| Reportable findings | 0 |
| Severity mix | none |
| Confidence mix | none |
| Coverage | complete |
| Validation mode | Full-file static review of every changed production source file, candidate validation, and attack-path analysis. |

Canonical artifacts: `scan-manifest.json`, `findings.json`, and `coverage.json`. This report is a deterministic projection of those files.

## Threat Model

The reviewed ingress accepts authenticated, HMAC-bound, rate-limited NQ feedback. Untrusted fields include tenant-bound envelope metadata, event identifiers, and JSON payloads. Security objectives are tenant isolation, strict validation before duplicate decisions, exact envelope-event correlation, atomic all-or-nothing persistence, fail-closed ambiguity/orphan handling, and zero implicit learning-store mutation.

### Assets

- tenant-scoped feedback envelopes and events
- idempotency and correlation integrity
- database consistency and service availability
- mutable learning stores that must remain unreachable from ingress

### Trust Boundaries

- NQ feedback ingress authentication/HMAC/rate-limit boundary
- use-case validation and unit-of-work boundary
- JDBC datasource/transaction boundary
- learning-store containment boundary

### Attacker Capabilities

- submit syntactically valid or invalid authenticated feedback within an authorized tenant/source
- repeat event identifiers, race requests, and craft JSON payload structure

### Security Objectives

- reject invalid input before duplicate resolution
- bind envelope and event by exact correlation
- read ingestion state from one PostgreSQL statement snapshot
- persist envelope and event atomically or roll back
- fail closed on orphan, conflict, ambiguity, and commit-unknown outcomes
- prevent inbound feedback from mutating learning stores

### Assumptions

- configured authentication, HMAC, nonce, and rate-limit controls operate as reviewed
- JDBC mode remains explicitly configured and uses the validated same datasource transaction manager

## Findings

### No findings

No reportable findings survived the canonical discovery, validation, and reportability gates.

## Reviewed Surfaces

| Surface | Risk Area | Outcome | Notes |
| --- | --- | --- | --- |
| Wiring, unit of work, and learning containment | Atomic transaction wiring and implicit learning side effects | No issue found | Same repository/unit-of-work instance, same datasource, PROPAGATION_REQUIRED, and no inbound mutable learning-store dependency or write path. |
| JDBC snapshot and exact correlation | READ COMMITTED torn reads, orphan ambiguity, and cross-tenant correlation | No issue found | One JdbcTemplate query and one SQL statement snapshot; exact eventId correlation and fail-closed orphan/conflict/ambiguity semantics. |
| Validation and strict JSON | Duplicate shortcut bypass and parser ambiguity | No issue found | Validation and canonicalization precede duplicate resolution; top-level JSON requires one object root, strict duplicate detection, and EOF. |
| In-memory rollback parity | Partial state, concurrent same-key handling, and correlation-index rollback | No issue found | Synchronized unit-of-work snapshots and restores envelope, events, correlation index, and insertion order. |
| Correlation lookup index candidate | Potential resource consumption as feedback-event volume grows | Rejected | No dedicated correlation_id index exists, but no production-sized EXPLAIN, row-count, or service-impact evidence proves a security vulnerability; ingress is authenticated/rate-limited and JDBC is default-disabled. Retained as a known capacity limitation. |
| Embedded textual JSON duplicate-key candidate | Potential forbidden-field scan ambiguity inside a JSON string | Rejected | The ordinary readTree helper behavior is unchanged from the base revision, and this diff neither introduced nor expanded the path; no disclosure, privilege, learning, or cross-tenant impact was proven. |
