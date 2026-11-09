package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.model.EventType;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
  name = "tb_factory_events",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_events_event_id",
    columnNames = "event_id"
  )
)
public class FactoryEventJpaEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "event_id", nullable = false, length = 100)
  private String eventId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventType eventType;

  @Column(nullable = false, length = 1000)
  private String description;

  @Column(nullable = false)
  private Instant timestamp;

  @Column(name = "status", nullable = false, length = 20)
  private String status;

  public FactoryEventJpaEntity() {}

  public FactoryEventJpaEntity(
    Long id,
    String eventId,
    EventType eventType,
    String description,
    Instant timestamp,
    String status
  ) {
    this.id = id;
    this.eventId = eventId;
    this.eventType = eventType;
    this.description = description;
    this.timestamp = timestamp;
    this.status = status;
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

  public EventType getEventType() {
    return eventType;
  }

  public void setEventType(EventType eventType) {
    this.eventType = eventType;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Instant getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Instant timestamp) {
    this.timestamp = timestamp;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}