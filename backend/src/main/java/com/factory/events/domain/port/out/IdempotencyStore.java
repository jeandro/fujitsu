package com.factory.events.domain.port.out;

import com.factory.events.domain.model.result.IdempotencyOutcome;
import java.time.Instant;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

/**
 * Porta de saída de idempotência.
 * <p>
 * Define a intenção de negócio ("esta operação é repetível") sem expor ao
 * domínio qualquer detalhe de infraestrutura: não há
 * {@code Idempotency-Key}, {@link jakarta.servlet.http.HttpServletRequest} ou
 * hash de payload nesta camada. O domínio recebe um resultado selado
 * ({@link IdempotencyOutcome}), nunca {@code null} nem {@link Optional}.
 */
public interface IdempotencyStore {

  /**
   * Consulta o estado de uma chave de idempotência dentro da janela de
   * retenção (24 h).
   *
   * @return nunca {@code null}: {@link IdempotencyOutcome.Processed} se a chave
   *         já foi processada, {@link IdempotencyOutcome.InProgress} se uma
   *         execução está em andamento, {@link IdempotencyOutcome.None} caso
   *         contrário.
   */
  IdempotencyOutcome lookup(@NonNull String scope, @NonNull String key);

  /**
   * Tenta marcar a chave como "em andamento" para a instância concorrente que
   * deve processar.
   *
   * @return {@code true} se esta instância ganhou o direito de processar
   *         (deve executar o efeito); {@code false} se outra execução já
   *         started (o adaptador web responde 409 in-progress).
   */
  boolean claim(@NonNull String scope, @NonNull String key, @NonNull Instant expiresAt);

  /**
   * Persiste o resultado final do processamento concluído (status + corpo da
   * resposta), permitindo replay idêntico (RN-03).
   */
  void storeResult(
    @NonNull String scope,
    @NonNull String key,
    @NonNull String requestHash,
    int responseStatus,
    @NonNull String responseBody,
    @NonNull Instant expiresAt
  );

  /**
   * Remove chaves expiradas da janela de retenção (expurgo periódico).
   */
  void purgeExpired(@NonNull Instant before);
}
