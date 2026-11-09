package com.factory.events.application;

import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.result.EventPage;
import com.factory.events.domain.port.in.ListEventsUseCase;
import com.factory.events.domain.port.out.EventRepositoryPort;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListEventsUseCaseImpl implements ListEventsUseCase {

  private static final int MAX_SIZE = 100;
  private static final int DEFAULT_SIZE = 20;

  private final EventRepositoryPort events;

  public ListEventsUseCaseImpl(EventRepositoryPort events) {
    this.events = events;
  }

  @Override
  @Transactional(readOnly = true)
  public EventPage list(int page, int size) {
    int safeSize = Math.min(Math.max(size, 1), MAX_SIZE);
    int safePage = Math.max(page, 0);
    return events.findAll(safePage, safeSize);
  }

  @Override
  @Transactional(readOnly = true)
  @NonNull
  public Optional<FactoryEvent> findByEventId(String eventId) {
    return events.findByEventId(eventId);
  }
}
