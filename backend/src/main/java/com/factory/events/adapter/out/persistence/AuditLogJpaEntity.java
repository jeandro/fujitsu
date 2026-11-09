package com.factory.events.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "tb_audit_logs")
public class AuditLogJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String eventId;

  @Column(nullable = false, length = 2000)
  private String logMessage;

  @Column(nullable = false)
  private Instant createdAt;

  public AuditLogJpaEntity() {}

  public AuditLogJpaEntity(
    Long id,
    String eventId,
    String logMessage,
    Instant createdAt
  ) {
    this.id = id;
    this.eventId = eventId;
    this.logMessage = logMessage;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId;
  }

  public String getLogMessage() {
    return logMessage;
  }

  public void setLogMessage(String logMessage) {
    this.logMessage = logMessage;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}