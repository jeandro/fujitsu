package com.factory.events.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.Map;

@Schema(description = "Resposta de erro padronizada da API")
public record ApiError(
  @Schema(description = "Instante do erro em UTC") Instant timestamp,
  @Schema(example = "400") int status,
  @Schema(example = "Verifique os campos informados.") String message,
  @Schema(description = "Identificador para correlação com o log")
  String requestId,
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  @Schema(description = "Mensagens por campo; presente em erros de validação")
  Map<String, String> errors
) {}
