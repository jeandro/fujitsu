package com.factory.events.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SagaStateDTO(
  @Schema(format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000") String sagaId,
  @Schema(example = "SUCCEEDED") String status,
  @Schema(example = "gravar-auditoria-qualidade") String currentStep,
  @Schema(description = "Passos da saga orquestrada") List<SagaStepDTO> steps,
  @Schema(example = "null", description = "Erro sanitizado, sem stack trace nem dado pessoal") String error,
  @Schema(type = "string", format = "date-time", example = "2026-11-05T13:00:00Z") Instant createdAt,
  @Schema(type = "string", format = "date-time", example = "2026-11-05T13:00:02Z") Instant updatedAt
) {
  public SagaStateDTO {
    if (sagaId == null) {
      sagaId = UUID.randomUUID().toString();
    }
  }
}
