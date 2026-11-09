package com.factory.events.domain.port.in;

import com.factory.events.domain.model.RegisterEventCommand;
import com.factory.events.domain.model.result.RegisterEventResult;
import org.jspecify.annotations.NonNull;

/**
 * Caso de uso: registro de evento de fábrica.
 * Retorna resultado selado ({@link RegisterEventResult}) nunca {@code null}.
 */
public interface RegisterEventUseCase {

  /**
   * Registra um evento. Para QUALITY, orquestra saga com compensação (RN-06, RN-07).
   *
   * @return nunca {@code null}: {@link RegisterEventResult.Success},
   *         {@link RegisterEventResult.Duplicate} ou {@link RegisterEventResult.Failure}
   */
  @NonNull RegisterEventResult register(@NonNull RegisterEventCommand command);
}
