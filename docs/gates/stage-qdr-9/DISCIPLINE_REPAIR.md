# Stage-QDR-9 Discipline Repair

B4 曾出现先发布后 milestone review 的治理 P1。仓库使用普通 revert 恢复 B3 safe baseline，保留历史链，
随后通过 owner-attested minimal remediation、authority rebaseline 与 final-close review 收口；没有重写历史。

关键链路：

- blocked implementation `549ed5a3224ce3ce375452dcf57629c73e3101d0`；
- ordinary revert `df921f275c61d67cebbb95c0924391866a6d09dc`；
- minimal implementation `6baabe113a3efa447ddd8036bb1b6c086ce1bcff`；
- remediation authority `064774df6eabb75678b7a46cd01b524e870d3ca0`；
- rebaseline `9d472b4642f4d17fc4ee3c0cc7a0d66a0b7d7d83`；
- accepted technical close `c7f940c0c48900a0cfb7eac86aac745c8006629c` / CI `30633947829`。

Superseded identity/audit/legacy designs 与 reference-liveness candidates 被完整归档，不因 close 自动复活。
