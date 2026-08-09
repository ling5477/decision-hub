# Validation: CAND-S5-STALE-CURRENT-AUTHORITY-001

Disposition: **suppressed**

Confidence: high

Validation method: bounded static diff and base-revision comparison.

## Rubric

- [x] Confirm whether the cited active-looking strings were introduced or changed by the exact diff.
- [x] Confirm the current first authority and fact-source precedence controls.
- [x] Identify any machine consumer or automatic execution sink for the Markdown strings.
- [x] Determine whether the text grants forbidden push, tag, implementation, or runtime authority.
- [x] Preserve both cited documentation instances independently.

## Evidence

- `git diff --unified=0 87304e3..d275b9e -- README.md docs/current/STATUS.md` shows only new top authority blocks at the candidate documents; it does not add the cited lower stale lines.
- `git show 87304e3:README.md` already contains the lower “唯一下一动作” string.
- `git show 87304e3:docs/current/STATUS.md` already contains the cited QDR-6 current/next-action strings.
- The new first authority explicitly preserves the QDR-10 BLOCKED history, authorizes only an independent retry, and denies current push/tag/runtime expansion.
- No repository consumer was found that machine-executes those Markdown routing strings.

## Static tuple

- Source: a human or coding agent reading lower current documentation.
- Control: first/current authority precedence, `FACTSOURCE_POLICY.md`, and separate explicit authorization for writes.
- Sink: selection of an obsolete governance workflow, with no automatic code or production sink.
- Counterevidence: every cited line predates the diff; newer top blocks supersede it and deny side effects.
- Remaining uncertainty: a reader could manually ignore precedence rules, but that is not a vulnerability introduced or newly exposed by this diff.

The candidate does not survive validation.
