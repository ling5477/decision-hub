package com.guidinglight.decisionhub.domain.run;

import com.guidinglight.decisionhub.common.util.IdGenerator;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @deprecated Stage1-CLOSE：旧多模型平台 Run 模型，已被 {@link com.guidinglight.decisionhub.domain.research.ResearchRun} 取代。
 *     保留仅为兼容 V1 schema，不要在新代码中使用。
 */
@Deprecated(since = "Stage1-CLOSE", forRemoval = true)
public class Run {

  private final String runId;
  private final String tenantId;
  private RunStatus status;

  private final String question;
  private final Map<String, Object> configSnapshot;

  /**
   * v1：DecisionRecord（全量执行档案）。
   * 用于记录：model_runs / eval_results / final_decision 等，便于复盘与回放。
   * 持久化层建议增加 JSON/LONGTEXT 列 decision_record_json。
   */
  private Map<String, Object> decisionRecord;

  private final List<RunStep> steps = new ArrayList<>();

  private final Instant createdAt;
  private Instant updatedAt;

  public Run(String tenantId, String question, Map<String, Object> configSnapshot, Instant now) {
    this(IdGenerator.newId(), tenantId, RunStatus.DRAFT, question, configSnapshot, now, now);
  }

  private Run(
      String runId,
      String tenantId,
      RunStatus status,
      String question,
      Map<String, Object> configSnapshot,
      Instant createdAt,
      Instant updatedAt) {
    this.runId = runId;
    this.tenantId = tenantId;
    this.status = status;
    this.question = question;
    this.configSnapshot = configSnapshot == null ? Map.of() : configSnapshot;
    this.decisionRecord = null;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public static Run rehydrate(
      String runId,
      String tenantId,
      RunStatus status,
      String question,
      Map<String, Object> configSnapshot,
      Instant createdAt,
      Instant updatedAt) {
    return new Run(runId, tenantId, status, question, configSnapshot, createdAt, updatedAt);
  }

  public void enqueue(Instant now) {
    this.status = RunStatus.QUEUED;
    this.updatedAt = now;
  }

  public void start(Instant now) {
    this.status = RunStatus.RUNNING;
    this.updatedAt = now;
  }

  public void defer(Instant now) {
    this.status = RunStatus.DEFERRED;
    this.updatedAt = now;
  }

  public void succeed(Instant now) {
    this.status = RunStatus.SUCCEEDED;
    this.updatedAt = now;
  }

  public void fail(Instant now) {
    this.status = RunStatus.FAILED;
    this.updatedAt = now;
  }

  public RunStep addStep(StepType type) {
    RunStep s = new RunStep(IdGenerator.newId(), type);
    this.steps.add(s);
    return s;
  }

  public String getRunId() { return runId; }
  public String getTenantId() { return tenantId; }
  public RunStatus getStatus() { return status; }
  public String getQuestion() { return question; }
  public Map<String, Object> getConfigSnapshot() { return configSnapshot; }

  public Map<String, Object> getDecisionRecord() { return decisionRecord; }
  public void setDecisionRecord(Map<String, Object> decisionRecord, Instant now) {
    this.decisionRecord = decisionRecord;
    this.updatedAt = now;
  }

  public List<RunStep> getSteps() { return Collections.unmodifiableList(steps); }
  public Instant getCreatedAt() { return createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
}
