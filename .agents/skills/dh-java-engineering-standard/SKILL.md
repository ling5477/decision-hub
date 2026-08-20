---
name: dh-java-engineering-standard
description: Apply Decision Hub Java engineering standards when adding, changing, reviewing, or refactoring Java, Spring Boot, JDBC, transactions, concurrency, logging, exceptions, Maven Java dependencies/tests, Checkstyle, PMD, SpotBugs, or ArchUnit. Do not auto-trigger for pure frontend, Python, docs-only, Authority-only, read-only acceptance, or Git-only work with no Java scope.
---

# DH Java Engineering Standard

Use this skill for Java engineering work in Decision Hub. The detected platform and repository architecture win over external style guidance.

## Required context

1. Read current authority at the top of `docs/current/STATUS.md` and `docs/current/WORK_ORDER.md`; never hard-code the current stage or next action here.
2. Detect the repository and read `AGENTS.md`.
3. Read `docs/standards/java/platform-profile.json` before making Java or Spring decisions.
4. Read `docs/standards/java/common-java-engineering-standard.md`.
5. Read `docs/standards/java/java-platform-profile.md`.
6. Read `docs/standards/java/spring-platform-profile.md`.
7. Read `docs/standards/java/architecture-overlay.md`.
8. Read `docs/standards/java/dh-java-domain-overlay.md`.
9. Read the relevant entries in `docs/standards/java/alibaba-huangshan-rule-mapping.yaml`.
10. Read `docs/standards/java/java-rule-exceptions.yaml`.

## Workflow

1. Identify target Java/Maven files, excluded files, expected output and existing dirty paths.
2. Check whether frozen contracts, Schema, Golden Case, state machines, atomicity, append-only evidence, tenant/source security or Authority could be affected; stop if the task requires an unauthorized change.
3. Inspect `ArchitectureTest`, module/package direction, TimeProvider/Clock, Repository/JDBC, transaction and Spring bean boundaries before implementing the smallest valid change.
4. Apply rule IDs compatible with the detected platform; report rules intentionally not applied because of platform, architecture or domain priority.
5. Do not remediate unrelated historical Shadow findings or batch-format untouched files.
6. Run relevant Maven tests plus existing Checkstyle/Spotless/ArchUnit checks, then run:

```powershell
pwsh -NoProfile -File scripts/java-standard/verify-java-engineering-standard.ps1
pwsh -NoProfile -File scripts/java-standard/invoke-java-shadow-scan.ps1 -OutputPath artifacts/java-shadow/shadow-report.json
```

7. Treat `VIOLATION_FOUND` as Shadow-only. Treat invalid mapping/platform/baseline, rule collisions, checker failure, missing report or nondeterministic output as blocking.

## Prohibited actions

- Do not use style rules to override DH atomicity, append-only, idempotency, Schema compatibility, fail-closed, evidence or current Authority contracts.
- Do not enable real HTTP/provider, NQ runtime integration, Agent/LangGraph, LIVE or production external side effects.
- Do not create broad exceptions, allow new usages under migration exceptions, or upgrade Shadow to a required incremental gate.
- Do not force obsolete Java or Spring idioms merely because they appear in Huangshan guidance.
- Do not infer Spring APIs from model knowledge; use `platform-profile.json` and effective repository dependencies.
- Do not use preview features, enable virtual threads, change Spring profiles or introduce unmanaged executors unless separately authorized and already supported by project configuration.
- Do not create `ServiceImpl`/repository interface ceremony or downgrade architecture invariants into style rules.

## Final report

Report: `Repository`, `Task classification`, `Authority inspected`, `Platform profile`, `Rules applied`, `Rules intentionally not applied`, `Platform compatibility`, `Exceptions used`, `Java files changed`, `Contracts affected`, `Validation performed`, `Remaining risks`, and `Final decision`.
