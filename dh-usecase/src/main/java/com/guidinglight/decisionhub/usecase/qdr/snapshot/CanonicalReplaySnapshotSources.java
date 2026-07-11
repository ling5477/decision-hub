package com.guidinglight.decisionhub.usecase.qdr.snapshot;

import com.guidinglight.decisionhub.domain.decision.DecisionReplayView;
import com.guidinglight.decisionhub.usecase.qdr.evidence.DecisionEvidenceAggregate;
import com.guidinglight.decisionhub.usecase.qdr.gateway.ModelGatewayCallRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.ModelVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.model.PromptVersionRecord;
import com.guidinglight.decisionhub.usecase.qdr.readmodel.DecisionRunDetailView;
import com.guidinglight.decisionhub.usecase.qdr.replay.EvaluationCaseRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.RegressionVerdictRecord;
import com.guidinglight.decisionhub.usecase.qdr.replay.ReplayCaseRecord;
import java.util.Objects;

/**
 * 一次 tenant-bound source read 得到的 immutable structured bundle。
 *
 * <p>该 bundle 只组合现有 ports 已返回的 safe read models；optional lineage 缺失时保持 {@code null}，
 * 不通过 latest、时间或相似字符串补造。所有 source 必须在同一 transaction 中读取。
 */
public record CanonicalReplaySnapshotSources(
    DecisionReplayView v5Replay,
    DecisionRunDetailView v6Run,
    PromptVersionRecord v8Prompt,
    ModelVersionRecord v8Model,
    ModelGatewayCallRecord v8GatewayCall,
    ReplayCaseRecord v9ReplayCase,
    EvaluationCaseRecord v9EvaluationCase,
    RegressionVerdictRecord v9RegressionVerdict,
    DecisionEvidenceAggregate evidenceAggregate) {

  /** 校验全部 required source 已存在；optional lineage 原样保留。 */
  public CanonicalReplaySnapshotSources {
    v5Replay = Objects.requireNonNull(v5Replay, "v5Replay");
    v6Run = Objects.requireNonNull(v6Run, "v6Run");
    v8Prompt = Objects.requireNonNull(v8Prompt, "v8Prompt");
    v8Model = Objects.requireNonNull(v8Model, "v8Model");
    v8GatewayCall = Objects.requireNonNull(v8GatewayCall, "v8GatewayCall");
    v9ReplayCase = Objects.requireNonNull(v9ReplayCase, "v9ReplayCase");
    evidenceAggregate = Objects.requireNonNull(evidenceAggregate, "evidenceAggregate");
    if (v9RegressionVerdict != null && v9EvaluationCase == null) {
      throw new IllegalArgumentException("regression verdict source requires evaluation source");
    }
  }
}
