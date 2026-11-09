package com.factory.events.domain.model.result;

import com.factory.events.domain.model.FactoryEvent;
import java.time.Instant;
import org.jspecify.annotations.NonNull;

/**
 * Resultado selado do caso de uso {@code RegisterEventUseCase}.
 * <p>
 * Desfechos esperados de regra de negócio são modelados aqui; exceções são
 * reservadas para falhas inesperadas ou de infraestrutura.
 */
public sealed interface RegisterEventResult
  permits RegisterEventResult.Success,
          RegisterEventResult.Duplicate,
          RegisterEventResult.IdempotencyReused,
          RegisterEventResult.InProgress,
          RegisterEventResult.Failure {

  /** Evento registrado com sucesso. */
  record Success(FactoryEvent event) implements RegisterEventResult {}

  /** {@code eventId} já existe (RN-02): duplicidade, sem novo efeito. */
  record Duplicate(@NonNull String eventId) implements RegisterEventResult {}

  /** {@code Idempotency-Key} reutilizada com payload diferente (RN-04). */
  record IdempotencyReused(@NonNull String key) implements RegisterEventResult {}

  /** Execução com mesma chave ainda em andamento (RN-05). */
  record InProgress(@NonNull String key, @NonNull Instant expiresAt)
    implements RegisterEventResult {}

  /** Falha de orquestração (ex.: compensação de saga executada, RN-07). */
  record Failure(@NonNull String reason) implements RegisterEventResult {}
}
