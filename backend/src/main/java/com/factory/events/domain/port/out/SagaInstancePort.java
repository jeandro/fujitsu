package com.factory.events.domain.port.out;

import org.jspecify.annotations.NonNull;

/**
 * Persistência de instância de saga orquestrada. Estado em banco, nunca memória.
 */
public interface SagaInstancePort {

  /**
   * Cria uma nova instância de saga em estado STARTED.
   *
   * @return identificador (UUID) da instância criada
   */
  @NonNull String createSagaInstance(@NonNull String sagaType,
                                     @NonNull String businessKey,
                                     @NonNull String payload);

  void markSagaSucceeded(@NonNull String sagaId);

  void markSagaFailedCompensated(@NonNull String sagaId, @NonNull String reason);
}
