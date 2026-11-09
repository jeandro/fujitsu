package com.factory.events.domain.port.out;

import org.jspecify.annotations.NonNull;

/**
 * Porta de saída do transactional outbox.
 */
public interface OutboxPort {

  /**
   * Grava um evento de saída para publicação posterior.
   * Deve ser chamado na mesma transação da alteração do agregado.
   */
  void save(@NonNull String aggregateType,
            @NonNull String aggregateId,
            @NonNull String eventType,
            int eventVersion,
            @NonNull String payload);
}
