package com.factory.events.adapter.in.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record SagaStepDTO(
  @Schema(example = "registrar-evento") String step,
  @Schema(example = "SUCCEEDED") String status,
  @Schema(example = "1") int attempts,
  @Schema(example = "null") String error
) {}
