package com.guidinglight.decisionhub.domain.run;

import java.time.Instant;

public class RunStep {
  private final String stepId;
  private final StepType type;
  private String assignedProvider;
  private String outputRef;
  private RunStatus status;
  private Instant startedAt;
  private Instant finishedAt;

  public RunStep(String stepId, StepType type) {
    this.stepId = stepId;
    this.type = type;
    this.status = RunStatus.DRAFT;
  }

  public void start(Instant now) {
    this.status = RunStatus.RUNNING;
    this.startedAt = now;
  }

  public void complete(String outputRef, Instant now) {
    this.status = RunStatus.SUCCEEDED;
    this.outputRef = outputRef;
    this.finishedAt = now;
  }

  public String getStepId() { return stepId; }
  public StepType getType() { return type; }
  public String getAssignedProvider() { return assignedProvider; }
  public void setAssignedProvider(String assignedProvider) { this.assignedProvider = assignedProvider; }
  public String getOutputRef() { return outputRef; }
  public RunStatus getStatus() { return status; }
  public Instant getStartedAt() { return startedAt; }
  public Instant getFinishedAt() { return finishedAt; }
}
