# Security Review

## Sealed exact-range evidence

- Scan ID: `61a49a7f-ae4b-4d15-8a09-3791a100e331`
- Range: `16ecded2f3708d69e05afe5f2a4f621c823d8be3..1c41a85e94a48ded74ce7a71e65b45f8d239a12b`
- Target ID: `target_sha256_522e609f9f885c22d5c1c86229ccff71a58c5cac88233fe46babdb9897b2ae79`
- Snapshot digest: `codex-security-snapshot/v1:sha256:7f245c34fbccb210c617be71559cfe048261840e91958c4440102ec19d3f4bc8`
- Coverage: complete; worklist receipts `9/9`; candidate validation/attack path `2/2`
- Reportable findings: `0`; active P0/P1: `0/0`
- Manifest SHA-256: `eed904825cac08f2db46db3f5eb7e8aae229545a7b0670f466f48eea989d9ca4`
- Findings SHA-256: `5031b714779a79810ea870741d6e3a3457b7202342f72d9d6d6c961022d3bb60`
- Coverage SHA-256: `ba7f4d086f366d9cf7acd2e69c58dd556cdfc87c3e4d495be277bfbccf26ca44`
- Report SHA-256: `40feb4d15989faef47649422a7f334084853805eb537485baf926b28105f2a92`

The sealed bundle is preserved under `security/`. The missing dedicated `correlation_id` index remains a capacity limitation, not a reportable security finding. The unchanged embedded textual-JSON duplicate-key candidate is outside this diff's introduced/expanded behavior and was suppressed.

CodeRabbit: `NOT_EXECUTED / CLI_NOT_INSTALLED / INSTALL_SCRIPT_BLOCKED_BY_POLICY`.
