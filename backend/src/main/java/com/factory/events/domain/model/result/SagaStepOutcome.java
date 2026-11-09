package com.factory.events.domain.model.result;

import org.jspecify.annotations.NonNull;

/**
 * Resultado selado da execução ou compensação de um passo de saga
 * ({@link SagaStepPort#execute} / {@link SagaStepPort#compensate}).
 * Nunca {@code null}; transições inválidas são tratadas como exceção de
 * domínio, não como {@code if} no orquestrador.
 */
public sealed interface SagaStepOutcome
  permits SagaStepOutcome.Completed, SagaStepOutcome.Failed,
    SagaStepOutcome.Skipped, SagaStepOutcome.Compensated,
    SagaStepOutcome.CompensationFailed {

  record Completed() implements SagaStepOutcome {}

  record Failed(@NonNull String reason) implements SagaStepOutcome {}

  record Skipped(@NonNull String reason) implements SagaStepOutcome {}

  record Compensated() implements SagaStepOutcome {}

  record CompensationFailed(@NonNull String reason) implements SagaStepOutcome {}
}
