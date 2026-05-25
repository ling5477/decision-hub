package com.guidinglight.decisionhub.usecase.agent;

import com.guidinglight.decisionhub.domain.agent.AgentRole;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointEntry;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointStatus;
import com.guidinglight.decisionhub.domain.checkpoint.CheckpointType;
import com.guidinglight.decisionhub.domain.reflection.ReflectionEntry;
import com.guidinglight.decisionhub.domain.reflection.ReflectionType;
import com.guidinglight.decisionhub.domain.research.ResearchRun;
import java.util.List;

/**
 * Stage2-PoC-B4：reflection / checkpoint 写入 + 查询用例。
 *
 * <p>设计原则：
 *
 * <ul>
 *   <li>每个 agent step 完成后写入 1 条 {@link ReflectionEntry}；stepIndex 单调递增，由调用方提供。
 *   <li>{@link CheckpointEntry} 在 PIVOT_TRIGGERED / ABORT_TRIGGERED / CANDIDATE_FROZEN /
 *       JUDGE_DECISION / BACKTEST_REQUESTED 时写入。
 *   <li>{@link com.guidinglight.decisionhub.domain.judge.JudgeDecision} 仍是唯一最终出口；
 *       reflection 只是过程证据，不允许写入 final recommendation。
 *   <li>不调用 LLM，不发起 HTTP，不实现真实下单，不绕过 NQ 风控。
 * </ul>
 */
public interface ReflectionCheckpointService {

  /**
   * 记录单条 reflection。
   *
   * @param run 对应 ResearchRun，提供 runId / traceId 关联。
   * @param stepIndex 单调递增，下界 0。
   * @param agentRole 执行该 step 的 AgentRole。
   * @param type reflection 类型（STEP / AGENT / RUN_RETROSPECTIVE）。
   * @param content 自由文本，建议 &lt;= 4KB。
   * @param payloadJson 可空扩展字段。
   * @return 落库后的 ReflectionEntry。
   */
  ReflectionEntry recordReflection(
      ResearchRun run,
      int stepIndex,
      AgentRole agentRole,
      ReflectionType type,
      String content,
      String payloadJson);

  /**
   * 记录单条 checkpoint。
   *
   * @param run 对应 ResearchRun。
   * @param checkpointIndex 单调递增，下界 0。
   * @param type checkpoint 触发场景。
   * @param status checkpoint 状态。
   * @param snapshotJson 冗余整段 run snapshot，禁止丢失。
   * @return 落库后的 CheckpointEntry。
   */
  CheckpointEntry recordCheckpoint(
      ResearchRun run,
      int checkpointIndex,
      CheckpointType type,
      CheckpointStatus status,
      String snapshotJson);

  /** 按 runId 查询 reflections，按 stepIndex 升序。 */
  List<ReflectionEntry> listReflections(String runId);

  /** 按 runId 查询 checkpoints，按 checkpointIndex 升序。 */
  List<CheckpointEntry> listCheckpoints(String runId);
}
