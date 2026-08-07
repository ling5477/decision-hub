# PostgreSQL Test Evidence

- Real Testcontainers PostgreSQL: `17.10`
- Flyway clean migration: `V1-V15`
- Mandatory Testcontainers assertion: `PASS`
- Concurrent same key: 8 workers, 1 accepted, 7 duplicate, 1 envelope, 1 event
- False `EVENT_ONLY`: `0`
- True envelope-only/event-only, ambiguity, unrelated event, and conflicts: fail closed
- Deterministic committed-winner/readback test: latch-coordinated, no sleep dependency
- Full regression: 1,293 tests, 0 failures, 0 errors, 0 skipped

Production-scale query planning and capacity were not executed.
