# Finding Discovery Report

Scan: `c911d3a1-e60a-4284-a18f-ebbe6db52018`

Snapshot: `codex-security-snapshot/v1:sha256:3c499b40232176c7a74b40ea0b0c7a0f6ebbe89283eac6fe548aca093c30eb3f`

All 9 technical worklist files were read in full at the recorded SHA-256 values. The changed production chain has one consolidated evidence authority, exact tenant/environment/trace/request/decision/RUN correlation, preserved bounded policy, fail-closed completeness mapping, sanitized errors, one Spring evaluator/facade path, and no authorization or side-effect sink.

The corrected PostgreSQL fixture binds replay `decisionRunId` to the query, `v6-run:` evidence ref, and persisted fixture. That test mocks the nested decision aggregate and therefore does not independently prove its database fetch; the production exact RUN comparison and explicit mismatch-to-`INVALID` unit test close the security boundary.

The 10 governance-only factsource changes were inspected as a separate diff surface: eight terminal blocks share one hash, remote implementation CI remains pending, production capacity remains not proven, and push/tag plus capacity/reference-liveness/retention/learning/NQ/HTTP/Provider/Agent/LangGraph/Paper/LIVE remain unauthorized.

Plausible candidates: **0**.
Reportable findings: **0**.
Active P0/P1: **0/0**.
Unauthorized bypass: **0**.

