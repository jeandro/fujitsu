package com.factory.events.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "saga_instance")
public class SagaInstanceJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "saga_type", nullable = false, length = 100)
  private String sagaType;

  @Column(name = "business_key", nullable = false, length = 255)
  private String businessKey;

  @Column(name = "state", nullable = false, length = 40)
  private String state;

  @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
  private String payload;

  @Column(name = "attempts", nullable = false)
  private int attempts;

  @Column(name = "error", columnDefinition = "text")
  private String error;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public SagaInstanceJpaEntity() {}

  public SagaInstanceJpaEntity(Long id, String sagaType, String businessKey, String state,
                               String payload, int attempts, String error,
                               Instant createdAt, Instant updatedAt) {
    this.id = id;
    this.sagaType = sagaType;
    this.businessKey = businessKey;
    this.state = state;
    this.payload = payload;
    this.attempts = attempts;
    this.error = error;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public String getSagaType() { return sagaType; }
  public void setSagaType(String sagaType) { this.sagaType = sagaType; }
  public String getBusinessKey() { return businessKey; }
  public void setBusinessKey(String businessKey) { this.businessKey = businessKey; }
  public String getState() { return state; }
  public void setState(String state) { this.state = state; }
  public String getPayload() { return payload; }
  public void setPayload(String payload) { this.payload = payload; }
  public int getAttempts() { return attempts; }
  public void setAttempts(int attempts) { this.attempts = attempts; }
  public String getError() { return error; }
  public void setError(String error) { this.error = error; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
