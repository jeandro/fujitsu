package com.factory.events.adapter.in.web;

import com.factory.events.adapter.in.web.dto.Problem;
import com.factory.events.domain.exception.DuplicateEventException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handler de exceções global. Toda resposta de erro segue RFC 9457 (ProblemDetail)
 * com {@code type} estável mapeado a RN-xx. Nunca vaza stack trace nem dado pessoal.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

  private static final String ERROR_BASE = "https://api.factory/events/errors/";

  @ExceptionHandler(DuplicateEventException.class)
  public ResponseEntity<Problem> duplicate(DuplicateEventException ex, WebRequest request) {
    Problem problem = new Problem(
      ERROR_BASE + "duplicate-event",
      "Evento duplicado",
      409,
      ex.getMessage(),
      instance(request),
      Map.of(),
      null,
      null
    );
    return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<Problem> invalidArgument(IllegalArgumentException ex, WebRequest request) {
    Problem problem = new Problem(
      ERROR_BASE + "validation-error",
      "Erro de validação de entrada",
      400,
      ex.getMessage(),
      instance(request),
      Map.of(),
      null,
      null
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Problem> unexpected(Exception ex, WebRequest request) {
    Problem problem = new Problem(
      ERROR_BASE + "internal-error",
      "Erro interno",
      500,
      "Erro interno. Tente novamente ou informe o identificador ao suporte.",
      instance(request),
      Map.of(),
      null,
      null
    );
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
    MethodArgumentNotValidException ex,
    org.springframework.http.HttpHeaders headers,
    org.springframework.http.HttpStatusCode status,
    WebRequest request
  ) {
    Map<String, String> errors = new LinkedHashMap<>();
    ex.getBindingResult().getFieldErrors()
      .forEach(e -> errors.put(e.getField(), e.getDefaultMessage()));
    Problem problem = new Problem(
      ERROR_BASE + "validation-error",
      "Erro de validação de entrada",
      400,
      "Verifique os campos informados.",
      instance(request),
      errors,
      null,
      null
    );
    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
  }

  private String instance(WebRequest request) {
    String path = request.getDescription(false);
    if (path.startsWith("uri=")) {
      path = path.substring(4);
    }
    return "/v1/events#req-" + java.util.UUID.randomUUID();
  }
}
