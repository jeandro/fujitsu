package com.factory.events.adapter.out.persistence;

public final class SagaInstancePersistenceMapper {

  private SagaInstancePersistenceMapper() {}

  public static SagaInstanceJpaEntity toJpa(Long id, String sagaType, String businessKey,
                                            String state, String payload, int attempts,
                                            String error, java.time.Instant createdAt,
                                            java.time.Instant updatedAt) {
    return new SagaInstanceJpaEntity(id, sagaType, businessKey, state, payload, attempts,
      error, createdAt, updatedAt);
  }
}
