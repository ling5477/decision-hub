package com.guidinglight.decisionhub.domain.decision;

import java.util.EnumSet;
import java.util.Set;

/**
 * Actions permanently forbidden from GateK Decision Pipeline output.
 *
 * <p>The default set is part of the K1 frozen contract. Any output missing
 * these values is unsafe.
 */
public enum ForbiddenAction {
  /** DH must not place orders. */
  PLACE_ORDER,

  /** DH must not cancel orders. */
  CANCEL_ORDER,

  /** DH must not mutate NQ state. */
  MUTATE_NQ_STATE,

  /** DH must not read NQ databases. */
  READ_NQ_DB,

  /** DH must not write NQ databases. */
  WRITE_NQ_DB;

  /** Returns the complete mandatory forbidden-action set. */
  public static Set<ForbiddenAction> mandatorySet() {
    return EnumSet.allOf(ForbiddenAction.class);
  }
}
