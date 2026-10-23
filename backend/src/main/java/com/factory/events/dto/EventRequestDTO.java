package com.factory.events.dto;

import com.factory.events.config.EventTimestampDeserializer;
import com.factory.events.model.EventType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * DTO para recebimento do payload do evento enviado por equipamentos externos.
 */
public class EventRequestDTO {

  @NotBlank(message = "O ID do evento (eventId) é obrigatório.")
  @Size(max = 100, message = "O ID deve ter até 100 caracteres.")
  private String eventId;

  @NotNull(message = "O tipo do evento (eventType) é obrigatório.")
  private EventType eventType;

  @NotBlank(message = "A descrição é obrigatória.")
  @Size(max = 1000, message = "A descrição deve ter até 1000 caracteres.")
  private String description;

  @NotNull(message = "O timestamp é obrigatório.")
  @JsonDeserialize(using = EventTimestampDeserializer.class)
  @Schema(
    type = "string",
    format = "date-time",
    example = "2026-05-20T13:00:00Z",
    description = "Instante com offset. Legado sem offset interpretado em America/Sao_Paulo."
  )
  private Instant timestamp;

  public EventRequestDTO() {}

  public EventRequestDTO(
    String eventId,
    EventType eventType,
    String description,
    Instant timestamp
  ) {
    setEventId(eventId);
    this.eventType = eventType;
    this.description = description;
    this.timestamp = timestamp;
  }

  public String getEventId() {
    return eventId;
  }

  public void setEventId(String eventId) {
    this.eventId = eventId == null ? null : eventId.strip();
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
