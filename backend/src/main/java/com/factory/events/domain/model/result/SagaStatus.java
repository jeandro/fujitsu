package com.factory.events.domain.model.result;

import org.jspecify.annotations.NonNull;

/**
 * Estados possíveis de uma instância de saga orquestrada.
 * Transição inválida é exceção de domínio, nunca {@code if} no orquestrador.
 */
public sealed interface SagaStatus
  permits SagaStatus.Started, SagaStatus.InProgress, SagaStatus.Succeeded,
    SagaStatus.Failed, SagaStatus.FailedCompensated, SagaStatus.Compensated {

  record Started() implements SagaStatus {}

  record InProgress() implements SagaStatus {}

  record Succeeded() implements SagaStatus {}

  record Failed(@NonNull String reason) implements SagaStatus {}

  record FailedCompensated(@NonNull String reason) implements SagaStatus {}

  record Compensated() implements SagaStatus {}
}
