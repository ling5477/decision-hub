# Failure and Retry Semantics

- Pre-commit validation, routing, append, and readback failures roll back all ingestion state.
- Known rollback outcomes are returned as stable internal persistence failures.
- An exception after transaction action completion during commit is classified as `COMMIT_OUTCOME_UNKNOWN`.
- Commit-unknown fails closed and is never automatically retried.
- A caller may safely retry only through the normal idempotent ingress path; exact complete state becomes duplicate, while orphan/conflict/ambiguity remains rejected.
- No loop, unbounded retry, or external side effect is introduced.
