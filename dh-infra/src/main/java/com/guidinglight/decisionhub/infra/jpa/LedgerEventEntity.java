package com.guidinglight.decisionhub.infra.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "dh_ledger_event")
public class LedgerEventEntity {

  @Id
  @Column(name = "event_id", length = 64, nullable = false)
  private String eventId;

  @Column(name = "run_id", length = 64, nullable = false)
  private String runId;

  @Column(name = "tenant_id", length = 64, nullable = false)
  private String tenantId;

  @Column(name = "type", length = 64, nullable = false)
  private String type;

  @Column(name = "at", nullable = false)
  private Instant at;

  @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
  private String payloadJson;

  public String getEventId() { return eventId; }
  public void setEventId(String eventId) { this.eventId = eventId; }
  public String getRunId() { return runId; }
  public void setRunId(String runId) { this.runId = runId; }
  public String getTenantId() { return tenantId; }
  public void setTenantId(String tenantId) { this.tenantId = tenantId; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Instant getAt() { return at; }
  public void setAt(Instant at) { this.at = at; }
  public String getPayloadJson() { return payloadJson; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
}
