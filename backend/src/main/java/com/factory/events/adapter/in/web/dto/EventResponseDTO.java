package com.factory.events.adapter.in.web.dto;

import com.factory.events.domain.model.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

public record EventResponseDTO(
  @Schema(example = "EVT-1001") String eventId,
  @Schema(example = "PRODUCTION") EventType eventType,
  @Schema(example = "Linha Ativa", description = "Confidencial (RN-17); devolvido apenas a papéis autorizados; mascarado em logs (RN-20).") String description,
  @Schema(type = "string", format = "date-time", example = "2026-05-20T13:00:00Z") Instant timestamp
) {}
