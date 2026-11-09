package com.factory.events.adapter.out.persistence;

public final class OutboxEventPersistenceMapper {

  private OutboxEventPersistenceMapper() {}

  public static OutboxEventJpaEntity toJpa(Long id, String aggregateType, String aggregateId,
                                           String eventType, String eventVersion,
                                           String payload, java.time.Instant occurredAt,
                                           java.time.Instant publishedAt) {
    return new OutboxEventJpaEntity(id, aggregateType, aggregateId, eventType, eventVersion,
      payload, occurredAt, publishedAt);
  }
}
