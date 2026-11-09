package com.factory.events.adapter.out.persistence;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "saga_step")
public class SagaStepJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "saga_id", nullable = false)
  private Long sagaId;

  @Column(name = "seq", nullable = false)
  private int seq;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "event_id", length = 100)
  private String eventId;

  @Column(name = "state", nullable = false, length = 40)
  private String state;

  @Column(name = "compensation_state", length = 40)
  private String compensationState;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public SagaStepJpaEntity() {}

  public SagaStepJpaEntity(Long id, Long sagaId, int seq, String name, String state,
                           String compensationState, String eventId, Instant updatedAt) {
    this.id = id;
    this.sagaId = sagaId;
    this.seq = seq;
    this.name = name;
    this.state = state;
    this.compensationState = compensationState;
    this.eventId = eventId;
    this.updatedAt = updatedAt;
  }

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }
  public Long getSagaId() { return sagaId; }
  public void setSagaId(Long sagaId) { this.sagaId = sagaId; }
  public int getSeq() { return seq; }
  public void setSeq(int seq) { this.seq = seq; }
  public String getName() { return name; }
  public void setName(String name) { this.name = name; }

  public String getEventId() { return eventId; }
  public void setEventId(String eventId) { this.eventId = eventId; }
  public String getState() { return state; }
  public void setState(String state) { this.state = state; }
  public String getCompensationState() { return compensationState; }
  public void setCompensationState(String compensationState) { this.compensationState = compensationState; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
