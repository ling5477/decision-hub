# Overview

Decision Hub is a Java 21, Maven multi-module decision-support service. Its primary runtime surfaces are Spring HTTP APIs, authenticated NQ-to-DH dry-run and feedback ingress, decision orchestration, PostgreSQL-backed audit/replay/evidence stores, scheduled/internal evaluation components, and provider/model-gateway abstractions. The intended security posture is fail-closed and read-only with respect to trading: outputs are recommendations or audit evidence, not authorization to place or cancel orders, mutate NQ state, access NQ databases, or enable LIVE execution.

The highest-value assets are tenant-isolated decision and feedback records; request, trace, run, replay, and idempotency identities; persisted audit and guard state; cryptographic authentication inputs and secret material; bounded-resource guarantees; and the integrity of the controls that keep provider, Agent/LangGraph, NQ, Paper, and LIVE capabilities disabled unless a later explicit gate authorizes them.

# Threat Model, Trust Boundaries, and Assumptions

## Trust boundaries

- External callers cross the HTTP boundary in `dh-api`. Authentication filters, HMAC/token verification, tenant binding, request validation, rate limits, replay/idempotency checks, and stable error mapping must run before use-case side effects.
- NQ integration messages cross a signed-header/body boundary implemented across `dh-api`, `dh-security`, and integration contracts. Header, body, tenant, source, environment, timestamp, nonce, and signature identities must agree exactly; missing, stale, ambiguous, or unverifiable values fail closed.
- Use cases cross a persistence boundary into JDBC/PostgreSQL adapters in `dh-infra`. Queries and writes must be tenant- and environment-scoped, parameterized, bounded, transactionally consistent where aggregates span rows, and explicit about duplicate/commit-unknown behavior.
- Model/provider abstractions cross an external-service boundary. Real HTTP and real providers are assumed disabled in the current governed state; future enablement would require timeout, bounded retry, redaction, allowlisting, auditability, and no secret or raw provider-response disclosure.
- Internal schedulers, replay, evaluation, learning, retention, and Agent/LangGraph components are privileged control planes. Current governance assumes feedback learning, retention/reference-liveness, NQ mutation, Agent/LangGraph runtime, Paper, and LIVE are not authorized.
- Build, CI, migrations, and configuration are developer/operator-controlled supply-chain surfaces. Flyway migrations are immutable history, configuration defaults must be conservative, and credentials must remain outside the repository and logs.

## Inputs and actors

- Attacker-controlled inputs include HTTP bodies, headers, bearer material presented for verification, tenant and correlation identifiers, feedback payloads, pagination/batch values, replay/idempotency keys, and any provider-originated response that reaches a future adapter.
- Operator-controlled inputs include deployment configuration, database connectivity, allowed environments, rate/timeout/batch thresholds, feature flags, and secret injection. Misconfiguration is security-relevant when it weakens fail-closed defaults or crosses DEV/TEST/production boundaries.
- Developer-controlled inputs include Java code, tests, Maven dependencies, Flyway migrations, CI workflows, documentation authority, and local tooling. Repository documents cannot override runtime authorization or create production capability by assertion.

## Core invariants and assumptions

- Tenant and environment provenance must be explicit and consistently propagated; no default tenant/environment, Spring-profile inference, first-row-wins ambiguity, or globally unique business identifier may substitute for persisted provenance.
- Decision, feedback, audit, replay, snapshot, guard, and rate/idempotency state must remain correlated by the full identity required by each contract.
- Missing, ambiguous, inconsistent, overflowed, unverifiable, or storage-error states fail closed and do not disclose partially accepted data.
- Any endpoint or internal workflow with side effects must validate authorization, tenant scope, state transition, idempotency, transaction boundaries, and bounded resource use before mutation.
- Secrets, tokens, cookies, signatures, raw prompts/provider responses, personal data, and database credentials must not be logged or persisted in unsafe form.
- Decision output remains `READ_ONLY_RECOMMENDATION`; forbidden actions include order placement/cancellation and NQ state/database mutation.

# Attack Surface, Mitigations, and Attacker Stories

- API authentication and authorization: forged or missing tokens, header/body tenant mismatch, role confusion, or filter-order bypass could expose cross-tenant data or privileged actions. Existing security filters, tenant-bound ports, WebMvc security tests, and fail-closed defaults reduce risk.
- Signed NQ messages and replay controls: canonicalization differences, timestamp/nonce replay, environment confusion, or signature coverage gaps could let an attacker reuse or transplant a valid message. Exact canonical fields, HMAC verification, bounded clock windows, replay persistence, and environment binding are the expected controls.
- Database and evidence integrity: SQL injection, unbounded reads, N+1 access, torn multi-statement reads, ambiguous provenance, duplicate-key races, or partial aggregate writes could corrupt or misattribute evidence. Parameterized JDBC, bounded queries, PostgreSQL/Testcontainers tests, unique constraints, explicit isolation, atomic aggregate operations, and fail-closed ambiguity handling are relevant mitigations.
- Deserialization and validation: malformed JSON, duplicate keys, multiple roots, oversized collections, pathologically deep values, or unsafe polymorphism could bypass validation or exhaust resources. Strict single-root parsing, DTO validation, page/batch limits, and rejection before lookup/write are expected.
- Feedback and learning: crafted feedback could poison mutable learning stores or promote cases implicitly. Current policy requires ingest/evidence reads to have no implicit learning or mutable learning-store writes.
- Provider/model boundary: prompt injection, malicious provider output, SSRF, secret leakage, excessive retry, and raw-response logging become realistic only if real providers/HTTP are enabled. They remain high-priority future risks even though current governance keeps those paths disabled.
- Audit, logs, and observability: correlation data helps incident response but can leak tenant data or secrets if raw requests, authorization material, signatures, or provider payloads are logged. Redaction and stable safe references are required.
- Build and migration chain: compromised dependencies, mutable historical migrations, unsafe CI scripts, or leaked secrets could affect every runtime. Pinned/reviewed dependencies, immutable Flyway history, quality checks, and secret-free repository practices are primary controls.

Out of scope under the current governed deployment are attackers who can already replace binaries, control the database superuser, or alter CI secrets; those are platform-compromise scenarios. Real trading abuse through Decision Hub is also outside the currently authorized runtime because order placement, NQ mutation, Paper, and LIVE are forbidden, but any code change that silently enables them is still a severe security regression.

# Severity Calibration (Critical, High, Medium, Low)

- Critical: a reachable path that enables real order placement/cancellation, NQ state or database mutation, LIVE execution, credential exfiltration, or broad unauthenticated cross-tenant compromise.
- High: authentication/signature bypass; reliable tenant isolation failure; arbitrary SQL/code execution; persistent audit/replay/decision corruption; or fail-open provider/decision behavior with material downstream trust impact.
- Medium: bounded but meaningful tenant data disclosure, replay/idempotency bypass with constrained impact, inconsistent aggregate state requiring operator recovery, or resource exhaustion across a service instance.
- Low: narrowly scoped metadata disclosure, defense-in-depth ambiguity with strong upstream controls, misleading bounded-completeness semantics without direct mutation, or a safe-default/configuration footgun that requires trusted operator action.

Severity depends on reachability and current authorization. A dormant provider, Agent, NQ, Paper, or LIVE path is less exploitable today, but a change that activates it or removes a controlling guard must be evaluated against the higher-impact future capability rather than dismissed as test-only intent.

Repository: target_sha256_522e609f9f885c22d5c1c86229ccff71a58c5cac88233fe46babdb9897b2ae79
Version: d275b9e30bb381bea8467286786add2c5b43e119
