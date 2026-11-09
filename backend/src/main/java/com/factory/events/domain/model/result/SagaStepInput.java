package com.factory.events.domain.model.result;

import org.jspecify.annotations.NonNull;

/**
 * Entrada de um passo de saga. Value object imutável (record).
 */
public record SagaStepInput(
  @NonNull String stepName,
  @NonNull String payload
) {}
