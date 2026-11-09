package com.factory.events.adapter.in.web;

import com.factory.events.adapter.in.web.dto.Problem;
import com.factory.events.domain.model.result.IdempotencyOutcome;
import com.factory.events.domain.port.out.IdempotencyStore;
import tools.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Filtro de idempotência para POST /v1/events.
 * <p>
 * Implementa o claim via {@code INSERT ... ON CONFLICT DO NOTHING} (atomicidade
 * no banco, nunca em memória). Responde 409 idempotency-key-reused (payload
 * diferente) e 409 idempotency-in-progress (execução concorrente). Replay
 * idêntico para mesma chave + mesmo corpo (RN-03, RN-04, RN-05).
 */
public class IdempotencyFilter extends OncePerRequestFilter {

  private static final String SCOPE = "events-api:register";
  private static final Duration RETENTION = Duration.ofHours(24);
  private static final int REQUEST_CACHE_LIMIT = 8192;
  private static final String ERROR_BASE = "https://api.factory/events/errors/";

  private final IdempotencyStore idempotencyStore;
  private final ObjectMapper objectMapper;

  public IdempotencyFilter(IdempotencyStore idempotencyStore, ObjectMapper objectMapper) {
    this.idempotencyStore = idempotencyStore;
    this.objectMapper = objectMapper;
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Filtra apenas POST /v1/events (exato)
    String path = request.getServletPath();
    return !("POST".equalsIgnoreCase(request.getMethod()) && "/v1/events".equals(path));
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
                                  HttpServletResponse response,
                                  FilterChain chain)
    throws ServletException, IOException {

    String key = request.getHeader("Idempotency-Key");
    if (key == null || key.isBlank()) {
      writeError(response, 400, ERROR_BASE + "validation-error",
        "Erro de validação de entrada",
        "Cabeçalho Idempotency-Key é obrigatório para POST /v1/events (RN-03).");
      return;
    }

    ContentCachingRequestWrapper wrappedRequest =
      new ContentCachingRequestWrapper(request, REQUEST_CACHE_LIMIT);
    byte[] bodyBytes = wrappedRequest.getContentAsByteArray();
    String requestHash = sha256Hex(bodyBytes);

    IdempotencyOutcome outcome = idempotencyStore.lookup(SCOPE, key);

    if (outcome instanceof IdempotencyOutcome.Processed processed) {
      if (processed.requestHash().equals(requestHash)) {
        // Replay idêntico (RN-03)
        response.setStatus(processed.responseStatus());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(processed.responseBody());
        return;
      }
      // Chave reutilizada com payload diferente (RN-04)
      writeError(response, 409, ERROR_BASE + "idempotency-key-reused",
        "Chave de idempotência reutilizada com payload diferente",
        "A Idempotency-Key já foi processada com um corpo diferente. " +
          "Reutilize a chave apenas com o mesmo payload.");
      return;
    }

    if (outcome instanceof IdempotencyOutcome.InProgress inProgress) {
      // Execução concorrente em andamento (RN-05)
      writeError(response, 409, ERROR_BASE + "idempotency-in-progress",
        "Execução de idempotência em andamento",
        "Uma execução com esta Idempotency-Key já está em andamento. Tente novamente em instantes.");
      return;
    }

    // None: tenta claim atômico
    Instant expiresAt = Instant.now().plus(RETENTION);
    boolean claimed = idempotencyStore.claim(SCOPE, key, expiresAt);
    if (!claimed) {
      writeError(response, 409, ERROR_BASE + "idempotency-in-progress",
        "Execução de idempotência em andamento",
        "Uma execução com esta Idempotency-Key já está em andamento. Tente novamente em instantes.");
      return;
    }

    // Processa a requisição e captura a resposta para armazenar
    ContentCachingResponseWrapper wrappedResponse =
      new ContentCachingResponseWrapper(response);
    try {
      chain.doFilter(wrappedRequest, wrappedResponse);
    } finally {
      int status = wrappedResponse.getStatus();
      byte[] respBytes = wrappedResponse.getContentAsByteArray();
      String respBody = new String(respBytes, StandardCharsets.UTF_8);

      if (status < 500) {
        idempotencyStore.storeResult(
          SCOPE, key, requestHash, status, respBody, expiresAt
        );
      } else {
        // 5xx: libera a chave para retry do cliente (não armazena resultado)
        idempotencyStore.purgeExpired(Instant.now()); // best-effort cleanup
      }
      wrappedResponse.copyBodyToResponse();
    }
  }

  private void writeError(HttpServletResponse response, int status, String type,
                          String title, String detail) throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("type", type);
    body.put("title", title);
    body.put("status", status);
    body.put("detail", detail);
    body.put("instance", "/v1/events#req-" + UUID.randomUUID());
    body.put("timestamp", Instant.now().toString());
    body.put("requestId", UUID.randomUUID().toString());
    objectMapper.writeValue(response.getWriter(), body);
  }

  private String sha256Hex(byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(data);
      StringBuilder hex = new StringBuilder(64);
      for (byte b : hash) {
        hex.append(Character.forDigit((b >> 4) & 0xF, 16));
        hex.append(Character.forDigit(b & 0xF, 16));
      }
      return hex.toString();
    } catch (Exception ex) {
      throw new RuntimeException("Falha ao calcular hash SHA-256", ex);
    }
  }
}
