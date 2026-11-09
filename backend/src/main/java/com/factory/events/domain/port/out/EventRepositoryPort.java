package com.factory.events.domain.port.out;

import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.result.EventPage;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface EventRepositoryPort {

  boolean existsByEventId(@NonNull String eventId);

  @NonNull FactoryEvent saveAndFlush(@NonNull FactoryEvent event);

  /**
   * Busca evento por eventId visível na consulta (status ACTIVE).
   * Retorna {@link Optional#empty()} se não existir ou estiver cancelado
   * (compensação de saga, PA-05). Nunca {@code null}.
   */
  @NonNull Optional<FactoryEvent> findByEventId(@NonNull String eventId);

  /**
   * Exclusão lógica do evento: marca como CANCELLED. Idempotente — evento
   * já cancelado não produz efeito adicional. Usado pela compensação de saga.
   */
  void cancelEvent(@NonNull String eventId);

  @NonNull EventPage findAll(int page, int size);
}
