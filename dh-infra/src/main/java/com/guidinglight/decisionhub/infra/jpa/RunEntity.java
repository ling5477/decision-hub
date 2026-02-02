package com.guidinglight.decisionhub.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "dh_run")
public class RunEntity {

  @Id
  @Column(name = "run_id", length = 64, nullable = false)
  private String runId;

  @Column(name = "tenant_id", length = 64, nullable = false)
  private String tenantId;

  @Column(name = "status", length = 32, nullable = false)
  private String status;

  @Column(name = "question", columnDefinition = "text", nullable = false)
  private String question;

  @Column(name = "config_snapshot", columnDefinition = "jsonb", nullable = false)
  private String configSnapshotJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public String getRunId() { return runId; }
  public void setRunId(String runId) { this.runId = runId; }
  public String getTenantId() { return tenantId; }
  public void setTenantId(String tenantId) { this.tenantId = tenantId; }
  public String getStatus() { return status; }
  public void setStatus(String status) { this.status = status; }
  public String getQuestion() { return question; }
  public void setQuestion(String question) { this.question = question; }
  public String getConfigSnapshotJson() { return configSnapshotJson; }
  public void setConfigSnapshotJson(String configSnapshotJson) { this.configSnapshotJson = configSnapshotJson; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
