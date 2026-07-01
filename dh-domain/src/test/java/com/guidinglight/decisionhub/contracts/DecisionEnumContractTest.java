package com.guidinglight.decisionhub.contracts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionType;
import com.guidinglight.decisionhub.domain.decision.ForbiddenAction;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** GateK K1 Java enum freeze tests for decision contracts. */
class DecisionEnumContractTest {

  @Test
  void decisionTypeRemainsReadOnlyRecommendationOnly() {
    assertEquals(1, DecisionType.values().length);
    assertEquals(DecisionType.READ_ONLY_RECOMMENDATION, DecisionType.values()[0]);
  }

  @Test
  void actionVocabularyIsReadOnlyAndContainsNoExecutionCommands() {
    final Set<String> values =
        Stream.of(DecisionAction.values()).map(Enum::name).collect(Collectors.toSet());
    assertEquals(
        Set.of("ABSTAIN", "OBSERVE", "NO_TRADE", "LONG_BIAS", "SHORT_BIAS"), values);
    assertFalse(values.contains("BUY"));
    assertFalse(values.contains("SELL"));
    assertFalse(values.contains("PLACE_ORDER"));
    assertFalse(values.contains("CANCEL_ORDER"));
    assertFalse(values.contains("MARKET_ORDER"));
    assertFalse(values.contains("LIMIT_ORDER"));
  }

  @Test
  void forbiddenActionsRemainTheMandatoryFiveValueSet() {
    final Set<String> values =
        Stream.of(ForbiddenAction.values()).map(Enum::name).collect(Collectors.toSet());
    assertEquals(
        Set.of("PLACE_ORDER", "CANCEL_ORDER", "MUTATE_NQ_STATE", "READ_NQ_DB", "WRITE_NQ_DB"),
        values);
    assertTrue(ForbiddenAction.mandatorySet().containsAll(ForbiddenAction.mandatorySet()));
  }
}
