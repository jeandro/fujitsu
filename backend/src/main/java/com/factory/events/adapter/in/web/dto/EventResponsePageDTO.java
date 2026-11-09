package com.factory.events.adapter.in.web.dto;

import com.factory.events.domain.model.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

public record EventResponsePageDTO(
  @Schema(description = "Itens da página") List<EventResponseDTO> content,
  @Schema(minimum = "0", defaultValue = "0", example = "0") int page,
  @Schema(minimum = "1", maximum = "100", defaultValue = "20", example = "20") int size,
  @Schema(example = "[\"timestamp,desc\"]") List<String> sort,
  @Schema(example = "2") long totalElements,
  @Schema(example = "1") int totalPages,
  @Schema(example = "true") boolean first,
  @Schema(example = "true") boolean last,
  @Schema(example = "2") int numberOfElements,
  @Schema(example = "false") boolean empty
) {}
