# Stage-QDR-11 Source Manifest
## Process sources
| Source path | Archive path | Source commit SHA | Classification | Copy status | SHA-256 | Post-tag action |
| --- | --- | --- | --- | --- | --- | --- |
| `docs/current/DH_POST_STAGE_QDR_10_NEXT_STAGE_PLAN.md` | `source/DH_POST_STAGE_QDR_10_NEXT_STAGE_PLAN.md` | `9b50fc7f51ebad3257aa77d1c2874d71385c81e9` | PLAN | COPIED_BYTE_IDENTICAL | `2c250aba32d38c0dcea32b3207b510194b3556c24f848fce5fc1d608fda751e2` | REMOVE_FROM_CURRENT_AFTER_REMOTE_TAG_VERIFICATION |
| `docs/current/DH_STAGE_QDR_11_CONSOLIDATED_EVIDENCE_INTERNAL_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md` | `source/DH_STAGE_QDR_11_CONSOLIDATED_EVIDENCE_INTERNAL_ACCEPTANCE_IMPLEMENTATION_WORK_ORDER.md` | `9b50fc7f51ebad3257aa77d1c2874d71385c81e9` | IMPLEMENTATION_WORK_ORDER | COPIED_BYTE_IDENTICAL | `2cde4a18ccdadea6ce2c9391893d4dafce68ea13e36dd5091ea7342523286165` | REMOVE_FROM_CURRENT_AFTER_REMOTE_TAG_VERIFICATION |
```text
planned = 2
copied = 2
missing = 0
unexpected = 0
hash failures = 0
```
## Security artifact inventory
| Archive path | SHA-256 |
| --- | --- |
| `security/scan-manifest.json` | `c1a05836f5c819d82038448927a5be2cdb753446c8af3c54b5e6eb58a70d193a` |
| `security/coverage.json` | `0174da05f4ec7a591375dc695034af11d3059f1fc340cd2ad3f9ea7d166012e2` |
| `security/findings.json` | `bcfff54d6217909c76f24fbb31f2ddc579252e72f6286ebe5738c4898e809444` |
| `security/report.md` | `a04637b970c3c7415505ba44d37025e91bfc91274feae8f9c7da7e91f6500eeb` |
| `security/threat_model.md` | `b3eaaa79951b4563f02a76ed10ac85517cfc691bce371bb653baa5a24f329108` |
| `security/deep_review_input.jsonl` | `c903ed22d6bbccbb9a417e4404f5facbe1c62100b4398e40773ad4bda41095b2` |
| `security/work_ledger.jsonl` | `a0e1d3c777c02d96cbdabbce7ea9dc148d9de1e26f468767040224b67e0544ca` |
| `security/finding_discovery_report.md` | `51d242b06b8b5b9aadd649df2bf94bbf962014996088f86a3c399a8401a5ba18` |
| `security/reviewed_surfaces.md` | `16e2b214106f4124fe6c168e9d16129c10ce278c4eea0128d417acc59ae9f615` |
| `security/results.sarif` | `1c96332be0e612ec4bccc7e71619524c406dfe9d2e78b0f1c4e7c09544138036` |
Security copied：10/10；missing：0；hash failures：0。

`security/.gitattributes` 为归档元数据，固定 `-text -whitespace`，确保 Git blob 保留 sealed artifact 原始字节；不计入上述 10 份 artifact inventory。
