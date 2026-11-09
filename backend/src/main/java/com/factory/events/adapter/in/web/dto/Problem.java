package com.factory.events.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Schema(description = "Problema da API conforme RFC 9457. Membros de extensão são permitidos.")
public record Problem(
  @Schema(format = "uri", example = "https://api.factory/events/errors/validation-error") String type,
  @Schema(example = "Erro de validação de entrada") String title,
  @Schema(example = "400") int status,
  @Schema(example = "O ID do evento (eventId) é obrigatório.") String detail,
  @Schema(format = "uri", example = "/v1/events#req-550e8400-e29b-41d4-a716-446655440000") String instance,
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  Map<String, String> errors,
  @Schema(type = "string", format = "date-time", example = "2026-11-05T13:00:00Z") Instant timestamp,
  @Schema(example = "550e8400-e29b-41d4-a716-446655440000") String requestId
) {
  public Problem {
    if (requestId == null) {
      requestId = UUID.randomUUID().toString();
    }
    if (timestamp == null) {
      timestamp = Instant.now();
    }
  }
}
