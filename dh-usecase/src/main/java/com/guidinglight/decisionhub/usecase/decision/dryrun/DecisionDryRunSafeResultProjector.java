package com.guidinglight.decisionhub.usecase.decision.dryrun;

import com.guidinglight.decisionhub.domain.decision.DecisionAction;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayOutputView;
import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQuery;
import com.guidinglight.decisionhub.usecase.decision.DecisionReplayQueryRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;

/** 从existing tenant-bound dh_decision_output/replay read model重建immutable endpoint-safe结果。 */
public final class DecisionDryRunSafeResultProjector {

  private final DecisionReplayQueryRepository replayRepository;

  /** @param replayRepository tenant-first exact read model port。 */
  public DecisionDryRunSafeResultProjector(
      final DecisionReplayQueryRepository replayRepository) {
    this.replayRepository = Objects.requireNonNull(replayRepository, "replayRepository");
  }

  /**
   * 读取并规范化safe response，使首次完成与COMPLETED duplicate返回相同projection。
   *
   * @param tenantId 已认证tenant。
   * @param decisionId persisted result id。
   * @return immutable safe snapshot。
   */
  public DecisionDryRunSnapshot project(final String tenantId, final String decisionId) {
    final DecisionReplayView replay =
        replayRepository.findReplay(new DecisionReplayQuery(tenantId, decisionId, null, null));
    if (!replay.isComplete() || replay.output() == null || replay.request() == null) {
      throw new IllegalStateException("persistent idempotency result unavailable");
    }
    final DecisionReplayOutputView output = replay.output();
    final String externalAction =
        output.action() == DecisionAction.ABSTAIN ? DecisionAction.NO_TRADE.name() : output.action().name();
    return new DecisionDryRunSnapshot(
        output.decisionId(),
        true,
        externalAction,
        output.confidence(),
        output.riskLevel().name(),
        stringList(output.outputJson().get("reasonCodes")),
        List.of("result:" + output.decisionId(), "idempotency:completed"),
        "replay:" + output.requestId(),
        "audit:" + output.decisionId(),
        replay.request().schemaVersion());
  }

  /** 对immutable safe projection计算lowercase SHA-256。 */
  public String checksum(final DecisionDryRunSnapshot snapshot) {
    final DecisionDryRunSnapshot checked = Objects.requireNonNull(snapshot, "snapshot");
    final String projection =
        checked.decisionId()
            + "\n"
            + checked.dryRun()
            + "\n"
            + checked.action()
            + "\n"
            + checked.confidence().toPlainString()
            + "\n"
            + checked.riskLevel()
            + "\n"
            + String.join("\u001f", checked.reasons())
            + "\n"
            + String.join("\u001f", checked.traceSummary())
            + "\n"
            + checked.replayRef()
            + "\n"
            + checked.auditRef()
            + "\n"
            + checked.schemaVersion();
    try {
      return java.util.HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(projection.getBytes(StandardCharsets.UTF_8)));
    } catch (final NoSuchAlgorithmException error) {
      throw new IllegalStateException("SHA-256 unavailable", error);
    }
  }

  private static List<String> stringList(final Object value) {
    if (!(value instanceof List<?> list)) {
      return List.of();
    }
    return list.stream()
        .filter(Objects::nonNull)
        .map(Object::toString)
        .filter(text -> !text.isBlank())
        .toList();
  }
}
