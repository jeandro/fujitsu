package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.port.out.OutboxPort;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * Adaptador do transactional outbox. Salva evento na mesma transação do agregado.
 */
@Component
public class OutboxAdapter implements OutboxPort {

  private final OutboxEventJpaRepository repository;

  public OutboxAdapter(OutboxEventJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public void save(@NonNull String aggregateType, @NonNull String aggregateId,
                   @NonNull String eventType, int eventVersion,
                   @NonNull String payload) {
    repository.saveOutbox(
      aggregateType, aggregateId, eventType,
      String.valueOf(eventVersion), payload, Instant.now()
    );
  }
}
