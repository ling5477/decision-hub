package com.guidinglight.decisionhub.domain.decision;

import java.util.Objects;

/**
 * Subject of a GateK read-only recommendation.
 *
 * <p>The subject identifies the analytical instrument and optional research
 * references. It intentionally excludes account, quantity, price, side, and
 * execution venue fields.
 */
public record DecisionSubject(
    String symbol, String market, String timeframe, String strategyRef, String researchRef) {

  /** Validates required analytical identifiers. */
  public DecisionSubject {
    symbol = requireText(symbol, "symbol");
    market = requireText(market, "market");
    timeframe = requireText(timeframe, "timeframe");
  }

  private static String requireText(final String value, final String field) {
    final String checked = Objects.requireNonNull(value, field).trim();
    if (checked.isEmpty()) {
      throw new IllegalArgumentException(field + " must not be blank");
    }
    return checked;
  }
}
