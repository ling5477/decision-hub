# Strict JSON Contract

The top-level payload must be exactly one JSON object root. Parsing enables duplicate-key detection, rejects array/scalar/null roots, and requires EOF after the object while allowing ordinary trailing JSON whitespace.

The same strict parsing semantics support validation and canonical duplicate comparison, preventing invalid repeated requests from using a duplicate shortcut.

The unchanged helper that inspects JSON text embedded inside a string is recorded as a suppressed out-of-diff candidate; no new technical change is authorized by this close.
