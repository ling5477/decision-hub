package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.util.Objects;

/**
 * Limited dry-run kill decision 的启动期快照。
 *
 * <p>B3 不声称具备多实例共享动态 kill store。除明确 {@link State#ALLOW} 外，未知、过期、读取失败和显式拒绝
 * 均 fail-closed。
 *
 * @param state kill decision 状态。
 */
public record RuntimeKillSwitch(State state) {

  /** Kill decision 稳定状态。 */
  public enum State {
    /** 明确允许 limited runtime。 */
    ALLOW,
    /** 显式拒绝。 */
    DENY,
    /** 状态缺失或未知。 */
    UNKNOWN,
    /** 状态已过期。 */
    STALE,
    /** 读取状态失败。 */
    READ_FAILED
  }

  /** 拒绝 null 状态，避免隐式 allow。 */
  public RuntimeKillSwitch {
    state = Objects.requireNonNull(state, "state");
  }

  /**
   * 将既有 emergency boolean 映射为明确 kill decision。
   *
   * @param killSwitchEnabled true 表示拒绝，false 表示启动期明确允许。
   * @return kill decision 快照。
   */
  public static RuntimeKillSwitch fromEmergencyFlag(final boolean killSwitchEnabled) {
    return new RuntimeKillSwitch(killSwitchEnabled ? State.DENY : State.ALLOW);
  }

  /** @return 是否为唯一允许状态。 */
  public boolean allowsLimitedRuntime() {
    return state == State.ALLOW;
  }
}
