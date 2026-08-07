# CI History

## Failed exact-SHA CI

- Run: `30823448218 / FAIL`
- Classification: real Testcontainers regression, not infrastructure-only
- Failing scenario: concurrent same-key ingestion
- Root cause: two-statement READ COMMITTED torn-state read
- Observable failure: false `EVENT_ONLY` during concurrent committed-winner readback

## Accepted implementation CI

- Run: `31184220520 / PASS`
- Fix: single-statement ingestion-state snapshot
- Quality: job `92884622294 / PASS`
- Testcontainers: job `92884622330 / PASS`, mandatory assertion passed
- Tests: `1293 / 0 / 0 / 0`
- PostgreSQL/Flyway: `17.10 / V1-V15`

Close-commit and cleanup-commit exact-SHA CI results are recorded after publication.
