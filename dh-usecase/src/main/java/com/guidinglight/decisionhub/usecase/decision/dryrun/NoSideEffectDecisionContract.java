package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.util.Optional;
import java.util.Set;

/**
 * Limited dry-run 成功结果的只读、无业务副作用合同。
 *
 * <p>该合同不执行任何 mutation；它只允许 read-only recommendation action，并显式冻结禁止的交易、资金、外部传输
 * 和 runtime 能力。
 */
public final class NoSideEffectDecisionContract {

  private static final Set<String> ALLOWED_ACTIONS =
      Set.of("OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS");

  private static final Set<String> FORBIDDEN_ACTIONS =
      Set.of(
          "BUY",
          "SELL",
          "MARKET_ORDER",
          "LIMIT_ORDER",
          "PLACE_ORDER",
          "CANCEL_ORDER",
          "TRANSFER",
          "WITHDRAW");

  private static final Set<String> FORBIDDEN_MUTATIONS =
      Set.of("ORDER", "RISK", "LEDGER", "ACCOUNT", "EXCHANGE", "PAPER", "LIVE", "FUNDS");

  private static final Set<String> FORBIDDEN_TRANSPORTS =
      Set.of("EXTERNAL_HTTP", "REAL_PROVIDER", "NQ_RUNTIME", "AGENT", "LANGGRAPH");

  /** @return immutable read-only action allowlist。 */
  public Set<String> allowedActions() {
    return ALLOWED_ACTIONS;
  }

  /** @return immutable forbidden executable action 集合。 */
  public Set<String> forbiddenActions() {
    return FORBIDDEN_ACTIONS;
  }

  /** @return immutable forbidden mutation 集合。 */
  public Set<String> forbiddenMutations() {
    return FORBIDDEN_MUTATIONS;
  }

  /** @return immutable forbidden transport/runtime 集合。 */
  public Set<String> forbiddenTransports() {
    return FORBIDDEN_TRANSPORTS;
  }

  /**
   * 验证成功结果仍为 read-only snapshot。
   *
   * @param result dry-run 结果。
   * @return 空表示通过；非空表示必须 fail-closed 的稳定分类。
   */
  public Optional<RuntimeFailureClassification> validate(final DecisionDryRunResult result) {
    if (result == null) {
      return Optional.of(RuntimeFailureClassification.INTERNAL_RUNTIME_FAILURE);
    }
    if (!result.success()) {
      return Optional.empty();
    }
    if (result.snapshot() == null || !result.snapshot().dryRun()) {
      return Optional.of(RuntimeFailureClassification.NO_SIDE_EFFECT_VIOLATION);
    }
    final String action = result.snapshot().action();
    if (action == null || !ALLOWED_ACTIONS.contains(action) || FORBIDDEN_ACTIONS.contains(action)) {
      return Optional.of(RuntimeFailureClassification.NO_SIDE_EFFECT_VIOLATION);
    }
    return Optional.empty();
  }
}
