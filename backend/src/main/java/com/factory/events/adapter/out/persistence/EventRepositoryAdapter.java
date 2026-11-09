package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.result.EventPage;
import com.factory.events.domain.port.out.EventRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
public class EventRepositoryAdapter implements EventRepositoryPort {

  private final FactoryEventJpaRepository repository;

  public EventRepositoryAdapter(FactoryEventJpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public boolean existsByEventId(String eventId) {
    return repository.existsByEventId(eventId);
  }

  @Override
  @NonNull
  public FactoryEvent saveAndFlush(FactoryEvent event) {
    var saved = repository.saveAndFlush(
      FactoryEventPersistenceMapper.toJpa(event)
    );
    return FactoryEventPersistenceMapper.toDomain(saved);
  }

  @Override
  @NonNull
  public Optional<FactoryEvent> findByEventId(String eventId) {
    return repository.findByEventIdAndStatusActive(eventId)
      .map(FactoryEventPersistenceMapper::toDomain);
  }

  @Override
  public void cancelEvent(String eventId) {
    repository.cancelByEventId(eventId);
  }

  @Override
  @NonNull
  public EventPage findAll(int page, int size) {
    var pageable = PageRequest.of(page, size,
      Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("id")));
    var jpaPage = repository.findAllByStatusActive(pageable);
    List<FactoryEvent> content = jpaPage.getContent()
      .stream()
      .map(FactoryEventPersistenceMapper::toDomain)
      .toList();
    return new EventPage(
      content,
      jpaPage.getNumber(),
      jpaPage.getSize(),
      jpaPage.getTotalElements(),
      jpaPage.getTotalPages(),
      jpaPage.isFirst(),
      jpaPage.isLast(),
      jpaPage.getNumberOfElements(),
      jpaPage.isEmpty()
    );
  }
}
