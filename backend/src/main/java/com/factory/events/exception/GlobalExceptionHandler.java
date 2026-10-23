package com.factory.events.exception;

import com.factory.events.dto.ApiError;
import java.time.Instant;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(
    GlobalExceptionHandler.class
  );

  private ApiError body(int status, String message, String requestId) {
    return new ApiError(Instant.now(), status, message, requestId, Map.of());
  }

  @ExceptionHandler(DuplicateEventException.class)
  public ResponseEntity<Object> duplicate(DuplicateEventException ex) {
    return ResponseEntity.status(409).body(
      body(409, ex.getMessage(), UUID.randomUUID().toString())
    );
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException ex,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request
  ) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult()
      .getFieldErrors()
      .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
    ApiError result = new ApiError(
      Instant.now(),
      400,
      "Verifique os campos informados.",
      UUID.randomUUID().toString(),
      errors
    );
    return new ResponseEntity<>(result, headers, status);
  }

  @Override
  protected ResponseEntity<Object> handleExceptionInternal(
    Exception ex,
    @Nullable Object ignored,
    HttpHeaders headers,
    HttpStatusCode status,
    WebRequest request
  ) {
    String message = switch (status.value()) {
      case 400 -> "JSON inválido. Confira os campos, o tipo do evento e a data/hora ISO 8601.";
      case 404 -> "Recurso não encontrado.";
      case 405 -> "Método HTTP não permitido para este recurso.";
      case 415 -> "Formato não suportado. Utilize Content-Type: application/json.";
      default -> "Não foi possível atender à solicitação.";
    };
    return new ResponseEntity<>(
      body(status.value(), message, UUID.randomUUID().toString()),
      headers,
      status
    );
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Object> unexpected(Exception ex) {
    String id = UUID.randomUUID().toString();
    log.error("Falha inesperada requestId={}", id, ex);
    return ResponseEntity.internalServerError().body(
      body(
        500,
        "Erro interno. Tente novamente ou informe o identificador ao suporte.",
        id
      )
    );
  }
}
