package com.guidinglight.decisionhub.usecase.qdr.model;

import com.guidinglight.decisionhub.domain.qdr.model.PromptModelSafetyRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 本地 deterministic PromptInjectionGuard。
 *
 * <p>实现使用 denylist/rule set，覆盖 system prompt 泄露、credential extraction、风控旁路、交易执行、
 * NQ mutation、audit 绕过和外部 provider 直接调用。该 guard 不调用 LLM、不访问 HTTP。
 */
public final class DeterministicPromptInjectionGuard implements PromptInjectionGuard {

    private static final List<Rule> RULES =
            List.of(
                    rule(PromptInjectionViolationCode.IGNORE_SYSTEM_POLICY, "ignore previous instructions"),
                    rule(PromptInjectionViolationCode.IGNORE_SYSTEM_POLICY, "disregard previous instructions"),
                    rule(PromptInjectionViolationCode.IGNORE_SYSTEM_POLICY, "override system policy"),
                    rule(PromptInjectionViolationCode.RAW_PROMPT_REQUEST, "reveal system prompt"),
                    rule(PromptInjectionViolationCode.RAW_PROMPT_REQUEST, "show system prompt"),
                    rule(PromptInjectionViolationCode.RAW_PROVIDER_RESPONSE_REQUEST, "raw provider response"),
                    rule(PromptInjectionViolationCode.EXFILTRATE_SECRET, "output secret"),
                    rule(PromptInjectionViolationCode.REQUEST_CREDENTIAL, "api key"),
                    rule(PromptInjectionViolationCode.REQUEST_CREDENTIAL, "raw credential"),
                    rule(PromptInjectionViolationCode.REQUEST_CREDENTIAL, "passphrase"),
                    rule(PromptInjectionViolationCode.REQUEST_CREDENTIAL, "cookie"),
                    rule(PromptInjectionViolationCode.REQUEST_CREDENTIAL, "token"),
                    rule(PromptInjectionViolationCode.OVERRIDE_RISK_GATE, "bypass risk"),
                    rule(PromptInjectionViolationCode.OVERRIDE_RISK_GATE, "bypass policy"),
                    rule(PromptInjectionViolationCode.EXECUTE_TRADE, "place order"),
                    rule(PromptInjectionViolationCode.EXECUTE_TRADE, "execute order"),
                    rule(PromptInjectionViolationCode.EXECUTE_TRADE, "cancel order"),
                    rule(PromptInjectionViolationCode.EXECUTE_TRADE, "submit order"),
                    rule(PromptInjectionViolationCode.EXECUTE_TRADE, "\\bBUY\\b"),
                    rule(PromptInjectionViolationCode.EXECUTE_TRADE, "\\bSELL\\b"),
                    rule(PromptInjectionViolationCode.MAP_BIAS_TO_ORDER, "map long_bias to"),
                    rule(PromptInjectionViolationCode.MAP_BIAS_TO_ORDER, "map short_bias to"),
                    rule(PromptInjectionViolationCode.MUTATE_NQ, "mutate nq state"),
                    rule(PromptInjectionViolationCode.MUTATE_NQ, "write nq db"),
                    rule(PromptInjectionViolationCode.MUTATE_NQ, "update nq state"),
                    rule(PromptInjectionViolationCode.DISABLE_AUDIT, "disable audit"),
                    rule(PromptInjectionViolationCode.DISABLE_AUDIT, "skip audit"),
                    rule(PromptInjectionViolationCode.TOOL_OR_AGENT_ESCALATION, "call external provider"),
                    rule(PromptInjectionViolationCode.TOOL_OR_AGENT_ESCALATION, "call openai"),
                    rule(PromptInjectionViolationCode.TOOL_OR_AGENT_ESCALATION, "call anthropic"),
                    rule(PromptInjectionViolationCode.TOOL_OR_AGENT_ESCALATION, "langgraph"),
                    rule(PromptInjectionViolationCode.TOOL_OR_AGENT_ESCALATION, "autogen"),
                    rule(PromptInjectionViolationCode.TOOL_OR_AGENT_ESCALATION, "crewai"));

    @Override
    public PromptInjectionDecision evaluate(final String tenantId, final String input) {
        PromptModelSafetyRules.requireText(tenantId, "tenantId");
        if (input == null || input.isBlank()) {
            return PromptInjectionDecision.allow();
        }
        final List<PromptInjectionViolation> violations = new ArrayList<>();
        for (Rule rule : RULES) {
            if (rule.pattern().matcher(input).find()) {
                violations.add(PromptInjectionViolation.of(rule.code()));
            }
        }
        if (PromptModelSafetyRules.containsSecretLikeMaterial(input)) {
            violations.add(PromptInjectionViolation.of(PromptInjectionViolationCode.REQUEST_CREDENTIAL));
        }
        if (PromptModelSafetyRules.containsExecutableTradingInstruction(input)) {
            violations.add(PromptInjectionViolation.of(PromptInjectionViolationCode.EXECUTE_TRADE));
        }
        if (violations.isEmpty()) {
            return PromptInjectionDecision.allow();
        }
        return PromptInjectionDecision.deny(violations);
    }

    private static Rule rule(final PromptInjectionViolationCode code, final String tokenOrRegex) {
        Objects.requireNonNull(code, "code");
        return new Rule(code, Pattern.compile(tokenOrRegex, Pattern.CASE_INSENSITIVE));
    }

    private record Rule(PromptInjectionViolationCode code, Pattern pattern) {
    }
}
