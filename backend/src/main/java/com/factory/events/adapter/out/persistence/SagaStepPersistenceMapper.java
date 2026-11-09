package com.factory.events.adapter.out.persistence;

public final class SagaStepPersistenceMapper {

  private SagaStepPersistenceMapper() {}

  public static SagaStepJpaEntity toJpa(Long id, Long sagaId, int seq, String name,
                                        String state, String compensationState,
                                        String eventId, java.time.Instant updatedAt) {
    return new SagaStepJpaEntity(id, sagaId, seq, name, state, compensationState, eventId, updatedAt);
  }
}
