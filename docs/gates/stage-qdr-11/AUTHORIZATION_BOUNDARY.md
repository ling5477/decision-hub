# Authorization Boundary
Internal acceptance 仅是 evidence 质量与一致性报告，不是执行授权。
`InternalAcceptanceStatus.ACCEPTED` 明确保持：
- `authorizesProvider() = false`
- `authorizesNqIntegration() = false`
- `allowsTradingOrExecution() = false`
- `enablesPaperOrLive() = false`
本阶段未新增 HTTP/Provider/NQ/Agent/LangGraph/execution dependency，未接 Controller 或 scheduler，未产生 learning store 写入。安全声明固定为 `INTERNAL_ACCEPTANCE_ONLY`，不可被 report input 覆盖。
