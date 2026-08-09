# Attack Path Analysis: CAND-S5-STALE-CURRENT-AUTHORITY-001

## Factual path

1. A human or coding agent opens a current Markdown entry document.
2. The reader ignores the first, newer Stage-QDR-10 authority and the documented fact-source precedence.
3. The reader selects a lower stale QDR-6 or feedback-containment next-action string.
4. The selected string can at most suggest an obsolete governance task; it does not execute a command, mutate data, grant credentials, or bypass the independent authorization required for Git/production writes.

## Counterevidence

- All cited lower strings existed in the base revision.
- The exact diff adds stricter first authority blocks that explicitly supersede older sections.
- Current top authority denies implementation push, tag, learning, capacity, NQ, HTTP/provider, Agent/LangGraph, Paper and LIVE.
- No machine consumer or runtime path uses the Markdown string as executable input.

## Calibration and policy

- Product surface: governance documentation, not a runtime ingress.
- Exposure: local developer/operator reading.
- Cross-boundary behavior: none established.
- Impact: ignore; at most obsolete manual workflow selection.
- Likelihood: ignore under the required precedence and independent authorization controls.
- Severity: ignore.
- Final policy decision: **ignore**.

The candidate is retained only as a coverage receipt and is excluded from reportable findings.
