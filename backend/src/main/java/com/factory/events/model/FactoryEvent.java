package com.factory.events.model;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Entidade JPA que representa o evento persistido no banco de dados.
 */
@Entity
@Table(
  name = "tb_factory_events",
  uniqueConstraints = @UniqueConstraint(
    name = "uk_events_event_id",
    columnNames = "event_id"
  )
)
public class FactoryEvent {

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

  public FactoryEvent() {}

  public FactoryEvent(
    String eventId,
    EventType eventType,
    String description,
    Instant timestamp
  ) {
    this.eventId = eventId;
    this.eventType = eventType;
    this.description = description;
    this.timestamp = timestamp;
  }

  public Long getId() {
    return id;
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
}
