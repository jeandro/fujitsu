package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.model.FactoryEvent;

public final class FactoryEventPersistenceMapper {

  private FactoryEventPersistenceMapper() {}

  public static FactoryEventJpaEntity toJpa(FactoryEvent domain) {
    return new FactoryEventJpaEntity(
      domain.id(),
      domain.eventId(),
      domain.eventType(),
      domain.description(),
      domain.timestamp(),
      "ACTIVE"
    );
  }

  public static FactoryEvent toDomain(FactoryEventJpaEntity entity) {
    return new FactoryEvent(
      entity.getId(),
      entity.getEventId(),
      entity.getEventType(),
      entity.getDescription(),
      entity.getTimestamp()
    );
  }
}
