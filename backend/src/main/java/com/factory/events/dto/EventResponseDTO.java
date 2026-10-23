package com.factory.events.dto;

import com.factory.events.model.EventType;
import java.time.Instant;

public class EventResponseDTO {

  private String eventId;
  private EventType eventType;
  private String description;
  private Instant timestamp;

  public EventResponseDTO() {}

  public EventResponseDTO(
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
