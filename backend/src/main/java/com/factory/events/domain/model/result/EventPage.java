package com.factory.events.domain.model.result;

import com.factory.events.domain.model.FactoryEvent;
import java.util.List;
import org.jspecify.annotations.NonNull;

/**
 * Resultado paginado de consulta de eventos. Value object imutável do domínio;
 * nenhuma dependência de Spring Data.
 */
public record EventPage(
  @NonNull List<FactoryEvent> content,
  int page,
  int size,
  long totalElements,
  int totalPages,
  boolean first,
  boolean last,
  int numberOfElements,
  boolean empty
) {}
