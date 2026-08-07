# Transaction Boundary

The ingestion service enters `NqFeedbackIngestionUnitOfWork.required()` only after validation and canonicalization.

For JDBC, the repository and unit of work are the same bean, use the same `DataSource`, and execute with `PROPAGATION_REQUIRED`. Envelope insert, correlation marker, synchronous routed event append, and exact completion-state readback occur inside this boundary. Any orphan, conflict, ambiguity, routing failure, append failure, or incomplete readback aborts the unit and rolls back.

The in-memory adapter provides parity by synchronizing the unit and restoring deep snapshots of envelopes, events, correlation index, and insertion order on failure.
