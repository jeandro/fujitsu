package com.factory.events.adapter.in.web.dto;

import com.factory.events.adapter.in.web.EventTimestampDeserializer;
import com.factory.events.domain.model.EventType;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public record EventRequestDTO(
  @NotBlank(message = "O ID do evento (eventId) é obrigatório.")
  @Size(max = 100, message = "O ID deve ter até 100 caracteres.")
  String eventId,

  @NotNull(message = "O tipo do evento (eventType) é obrigatório.")
  EventType eventType,

  @NotBlank(message = "A descrição é obrigatória.")
  @Size(max = 1000, message = "A descrição deve ter até 1000 caracteres.")
  String description,

  @NotNull(message = "O timestamp é obrigatório.")
  @JsonDeserialize(using = EventTimestampDeserializer.class)
  @Schema(
    type = "string",
    format = "date-time",
    example = "2026-05-20T13:00:00Z",
    description = "Instante ISO 8601 válido (RN-01). Offset-preservante; legado sem offset interpretado em America/Sao_Paulo."
  )
  Instant timestamp
) {
  public EventRequestDTO {
    if (eventId != null) {
      eventId = eventId.strip();
    }
    if (description != null) {
      description = description.strip();
    }
  }
}
