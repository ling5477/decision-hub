# Security Review: decision-hub

## Scope

Final exact-working-tree Stage-QDR-11 consolidated evidence internal acceptance review.

- Scan mode: working_tree
- Target kind: git_diff
- Target ID: target_sha256_522e609f9f885c22d5c1c86229ccff71a58c5cac88233fe46babdb9897b2ae79
- Revision range: 9102c2f28bb0e470c51c09edaa20da9c7bbb32c2...9102c2f28bb0e470c51c09edaa20da9c7bbb32c2
- Snapshot digest: codex-security-snapshot/v1:sha256:3c499b40232176c7a74b40ea0b0c7a0f6ebbe89283eac6fe548aca093c30eb3f
- Inventory strategy: diff
- Included paths: .
- Excluded paths: none
- Runtime or test status: Affected-module regression PASS; full 19-module regression PASS with 1338 tests and 0 failures/errors/skipped; PostgreSQL 17.10 with Flyway V1-V15; quality PASS with Checkstyle 0 and Spotless PASS.
- Artifacts reviewed: artifacts/01_context/threat_model.md, artifacts/02_discovery/deep_review_input.jsonl, artifacts/02_discovery/work_ledger.jsonl, artifacts/02_discovery/finding_discovery_report.md, artifacts/03_coverage/reviewed_surfaces.md
- Scan context: Single evidence authority, completeness and bounded-policy propagation, tenant/environment/request/trace/decision/RUN isolation, internal facade, unique Spring wiring, authorization boundary, corrected PostgreSQL RUN fixture, zero side effects, and governance-only factsource synchronization.

Limitations and exclusions:
- The internal facade has no public or scheduled caller; production capacity was not evaluated.
- The QDR-11 PostgreSQL test mocks nested decision aggregation; exact RUN enforcement is independently covered by production comparison and a mismatch-to-INVALID unit test.

### Scan Summary

| Field | Value |
| --- | --- |
| Reportable findings | 0 |
| Severity mix | none |
| Confidence mix | none |
| Coverage | complete |
| Validation mode | exact working-tree digest plus 9 full-file SHA-256 receipts, governance diff review, targeted and full regression |

Canonical artifacts: `scan-manifest.json`, `findings.json`, and `coverage.json`. This report is a deterministic projection of those files.

## Threat Model

Decision Hub is a tenant- and environment-isolated Java decision system. Internal evidence and acceptance results remain read-only and cannot create Provider, NexusQuant, trading, Paper, or LIVE authorization.

### Assets

- tenant-scoped decision and feedback evidence
- decision/replay/audit correlation integrity
- bounded evidence policy
- persistence and audit integrity
- external Provider/NexusQuant authorization boundary

### Trust Boundaries

- external caller to application
- tenant and environment scope
- usecase/domain to PostgreSQL
- feedback ingest to learning stores
- internal evidence to external runtime

### Attacker Capabilities

- submit malformed or cross-scope identifiers at exposed entrypoints
- replay or mix correlation identities if controls permit
- attempt to reinterpret internal status as authorization

### Security Objectives

- fail closed on incomplete inconsistent overflow or cross-scope evidence
- preserve exact bounded policy
- keep one evidence authority and one Spring path
- prevent external authorization and side effects

### Assumptions

- no public or scheduled caller exists for the internal facade
- PostgreSQL and trusted caller identity are authentic
- production credentials are not stored in the repository

## Findings

### No findings

No reportable findings survived the canonical discovery, validation, and reportability gates.

## Reviewed Surfaces

| Surface | Risk Area | Outcome | Notes |
| --- | --- | --- | --- |
| Consolidated acceptance production chain | single authority, completeness, bounds, tenant/environment/request/trace/decision/RUN correlation | No issue found | Four production files reviewed at current SHA-256; mismatch and unusable evidence fail closed. Evidence: artifacts/02_discovery/work_ledger.jsonl |
| Spring wiring | duplicate bean or bypass path | No issue found | One consolidated evidence service, one evaluator and one facade; no legacy competing path. Evidence: artifacts/02_discovery/work_ledger.jsonl |
| Security-boundary tests | isolation, authorization misuse, zero side effects, PostgreSQL RUN fixture | No issue found | Five tests reviewed at current SHA-256; affected and full suites passed. Evidence: artifacts/02_discovery/work_ledger.jsonl |
| Governance factsources | unauthorized capability or publication expansion | No issue found | Ten allowlisted documentation diffs reviewed; current blocks synchronized and push/tag/capacity/runtime expansions remain unauthorized. Evidence: artifacts/03_coverage/reviewed_surfaces.md |
