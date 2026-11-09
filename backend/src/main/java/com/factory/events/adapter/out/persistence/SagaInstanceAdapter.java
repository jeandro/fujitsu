package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.port.out.SagaInstancePort;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * Adaptador de persistência de instância de saga.
 */
@Component
public class SagaInstanceAdapter implements SagaInstancePort {

  private final SagaInstanceJpaRepository repository;

  public SagaInstanceAdapter(SagaInstanceJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  @NonNull
  public String createSagaInstance(@NonNull String sagaType,
                                   @NonNull String businessKey,
                                   @NonNull String payload) {
    Instant now = Instant.now();
    Long id = repository.insertSagaInstance(sagaType, businessKey, payload, now);
    return "saga-" + id;
  }

  @Override
  public void markSagaSucceeded(@NonNull String sagaId) {
    Long id = Long.parseLong(sagaId.replace("saga-", ""));
    repository.updateState(id, "SUCCEEDED", Instant.now());
  }

  @Override
  public void markSagaFailedCompensated(@NonNull String sagaId, @NonNull String reason) {
    Long id = Long.parseLong(sagaId.replace("saga-", ""));
    repository.updateStateWithError(id, "FAILED_COMPENSATED", reason, Instant.now());
  }
}
