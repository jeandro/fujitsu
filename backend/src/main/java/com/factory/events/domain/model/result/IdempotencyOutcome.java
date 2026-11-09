package com.factory.events.domain.model.result;

import java.time.Instant;
import org.jspecify.annotations.NonNull;

/**
 * Resultado selado da consulta de idempotência ({@link IdempotencyStore#lookup}).
 * Nunca {@code null}; nenhum dos três estados permite ausência de informação.
 */
public sealed interface IdempotencyOutcome
  permits IdempotencyOutcome.Processed, IdempotencyOutcome.InProgress, IdempotencyOutcome.None {

  /**
   * A chave já foi processada com sucesso dentro da janela de retenção.
   * Devolve o status e o corpo da resposta armazenados (replay idêntico, RN-03).
   */
  record Processed(int responseStatus, @NonNull String responseBody,
                   @NonNull String requestHash)
    implements IdempotencyOutcome {}

  /**
   * Uma execução com esta chave está em andamento (RN-05; o adaptador web
   * responde 409 idempotency-in-progress).
   */
  record InProgress(@NonNull Instant expiresAt) implements IdempotencyOutcome {}

  /**
   * Não há registro da chave na janela de retenção (chave nunca vista ou
   * expirada): cabe processar a requisição.
   */
  record None() implements IdempotencyOutcome {}
}
