package com.factory.events.domain.port.out;

import com.factory.events.domain.model.result.SagaStepInput;
import com.factory.events.domain.model.result.SagaStepOutcome;
import org.jspecify.annotations.NonNull;

/**
 * Porta de saída para execução e compensação de um passo de saga orquestrada.
 * <p>
 * Cada passo declara ação e compensação com semântica de negócio (RN-07). A
 * compensação é um comando novo, auditável e idempotente — não é rollback de
 * banco.
 */
public interface SagaStepPort {

  /**
   * Executa o passo {@code stepIndex} da saga {@code sagaId}.
   *
   * @return resultado selado da execução (nunca {@code null})
   */
  SagaStepOutcome execute(
    @NonNull String sagaId,
    int stepIndex,
    @NonNull SagaStepInput input
  );

  /**
   * Compensa o passo {@code stepIndex} da saga {@code sagaId}.
   *
   * @return resultado selado da compensação (nunca {@code null})
   */
  SagaStepOutcome compensate(@NonNull String sagaId, int stepIndex);
}
