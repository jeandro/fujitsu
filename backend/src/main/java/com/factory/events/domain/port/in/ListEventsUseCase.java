package com.factory.events.domain.port.in;

import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.result.EventPage;
import java.util.Optional;
import org.jspecify.annotations.NonNull;

public interface ListEventsUseCase {

  @NonNull EventPage list(int page, int size);

  /**
   * Busca evento por eventId visível na consulta (status ACTIVE).
   * Retorna {@link Optional#empty()} se não existir ou estiver cancelado.
   * Nunca {@code null}.
   */
  @NonNull Optional<FactoryEvent> findByEventId(@NonNull String eventId);
}
