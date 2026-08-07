# Duplicate and Correlation Semantics

- Validation and strict canonicalization happen before any duplicate decision.
- `DUPLICATE` requires a canonical envelope match plus exactly one correlated event.
- Correlation binds tenant, source, type, trace/run identity, and the envelope event ID.
- Payload/content tuple matching is not a substitute for event-ID correlation.
- Multiple envelopes/events are ambiguous and fail closed.
- Envelope-only, event-only, canonical conflict, and event conflict fail closed.
- Concurrent same-key requests produce one accepted winner; followers become duplicate only after an exact complete pair is visible.
