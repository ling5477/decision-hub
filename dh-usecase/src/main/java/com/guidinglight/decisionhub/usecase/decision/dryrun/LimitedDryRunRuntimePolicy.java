package com.guidinglight.decisionhub.usecase.decision.dryrun;

import java.util.Objects;
import java.util.Optional;

/**
 * Stage-QDR-7 B3 limited dry-run runtime 的统一启动期 policy 快照。
 *
 * <p>判定顺序固定为 feature -> environment -> kill -> limits -> mock/retry -> bounded admission。所有未知或非法状态
 * 均 fail-closed，不存在真实 provider、HTTP、NQ 或 retry fallback。
 */
public final class LimitedDryRunRuntimePolicy {

  /** 唯一允许的 provider kind。 */
  public static final String MOCK_PROVIDER_KIND = "MOCK";

  private final RuntimeFeatureFlag featureFlag;
  private final RuntimeEnvironmentPolicy environmentPolicy;
  private final RuntimeKillSwitch killSwitch;
  private final RuntimeDeadlinePolicy deadlinePolicy;
  private final RuntimeConcurrencyPolicy concurrencyPolicy;
  private final String providerKind;
  private final int retryCount;
  private final NoSideEffectDecisionContract noSideEffectContract;

  /** 创建不可变 runtime policy 快照。 */
  public LimitedDryRunRuntimePolicy(
      final RuntimeFeatureFlag featureFlag,
      final RuntimeEnvironmentPolicy environmentPolicy,
      final RuntimeKillSwitch killSwitch,
      final RuntimeDeadlinePolicy deadlinePolicy,
      final RuntimeConcurrencyPolicy concurrencyPolicy,
      final String providerKind,
      final int retryCount,
      final NoSideEffectDecisionContract noSideEffectContract) {
    this.featureFlag = Objects.requireNonNull(featureFlag, "featureFlag");
    this.environmentPolicy = Objects.requireNonNull(environmentPolicy, "environmentPolicy");
    this.killSwitch = Objects.requireNonNull(killSwitch, "killSwitch");
    this.deadlinePolicy = Objects.requireNonNull(deadlinePolicy, "deadlinePolicy");
    this.concurrencyPolicy = Objects.requireNonNull(concurrencyPolicy, "concurrencyPolicy");
    this.providerKind = providerKind == null ? "" : providerKind.trim();
    this.retryCount = retryCount;
    this.noSideEffectContract = Objects.requireNonNull(noSideEffectContract, "noSideEffectContract");
  }

  /** @return feature flag 快照。 */
  public RuntimeFeatureFlag featureFlag() {
    return featureFlag;
  }

  /** @return environment policy 快照。 */
  public RuntimeEnvironmentPolicy environmentPolicy() {
    return environmentPolicy;
  }

  /** @return kill decision 快照。 */
  public RuntimeKillSwitch killSwitch() {
    return killSwitch;
  }

  /** @return deadline policy。 */
  public RuntimeDeadlinePolicy deadlinePolicy() {
    return deadlinePolicy;
  }

  /** @return concurrency/queue policy。 */
  public RuntimeConcurrencyPolicy concurrencyPolicy() {
    return concurrencyPolicy;
  }

  /** @return provider kind；B3 只能为 MOCK。 */
  public String providerKind() {
    return providerKind;
  }

  /** @return retry count；B3 固定为 0。 */
  public int retryCount() {
    return retryCount;
  }

  /** @return no-side-effect 合同。 */
  public NoSideEffectDecisionContract noSideEffectContract() {
    return noSideEffectContract;
  }

  /**
   * 按冻结顺序评估 runtime 是否可进入 bounded admission。
   *
   * @return allow 或稳定失败分类。
   */
  public Evaluation evaluate() {
    if (!featureFlag.enabled()) {
      return Evaluation.denied(RuntimeFailureClassification.RUNTIME_DISABLED);
    }
    if (!environmentPolicy.allowsLimitedRuntime()) {
      return Evaluation.denied(RuntimeFailureClassification.ENVIRONMENT_DENIED);
    }
    if (!killSwitch.allowsLimitedRuntime()) {
      return Evaluation.denied(RuntimeFailureClassification.KILL_SWITCH_DENIED);
    }
    if (!concurrencyPolicy.isValid()
        || !deadlinePolicy.isValid(concurrencyPolicy.queueCapacity())) {
      return Evaluation.denied(RuntimeFailureClassification.RUNTIME_CONFIGURATION_INVALID);
    }
    if (!MOCK_PROVIDER_KIND.equals(providerKind) || retryCount != 0) {
      return Evaluation.denied(RuntimeFailureClassification.MOCK_PROVIDER_REQUIRED);
    }
    return Evaluation.permitted();
  }

  /**
   * 验证 bounded delegate 返回值仍符合 no-side-effect 合同。
   *
   * @param result delegate result。
   * @return 空表示通过，否则为稳定失败分类。
   */
  public Optional<RuntimeFailureClassification> validateResult(
      final DecisionDryRunResult result) {
    return noSideEffectContract.validate(result);
  }

  /**
   * Runtime policy 判定结果。
   *
   * @param allowed 是否允许进入 bounded admission。
   * @param failureClassification 拒绝分类；allow 时为空。
   */
  public record Evaluation(
      boolean allowed, RuntimeFailureClassification failureClassification) {

    /** @return allow 判定。 */
    public static Evaluation permitted() {
      return new Evaluation(true, null);
    }

    /**
     * 创建拒绝判定。
     *
     * @param classification 稳定失败分类。
     * @return deny 判定。
     */
    public static Evaluation denied(final RuntimeFailureClassification classification) {
      return new Evaluation(false, Objects.requireNonNull(classification, "classification"));
    }
  }
}
