package com.factory.events.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaInstanceJpaRepository
  extends JpaRepository<SagaInstanceJpaEntity, Long> {

  Optional<SagaInstanceJpaEntity> findByBusinessKey(String businessKey);

  @Query(
    value = """
      INSERT INTO saga_instance (saga_type, business_key, state, payload, attempts, created_at, updated_at)
      VALUES (:sagaType, :businessKey, 'STARTED', :payload, 0, :now, :now)
      """,
    nativeQuery = true
  )
  Long insertSagaInstance(@Param("sagaType") String sagaType,
                          @Param("businessKey") String businessKey,
                          @Param("payload") String payload,
                          @Param("now") Instant now);

  @Query(
    value = "UPDATE saga_instance SET state = :state, updated_at = :now WHERE id = :id",
    nativeQuery = true
  )
  int updateState(@Param("id") Long id,
                  @Param("state") String state,
                  @Param("now") Instant now);

  @Query(
    value = "UPDATE saga_instance SET state = :state, error = :error, updated_at = :now WHERE id = :id",
    nativeQuery = true
  )
  int updateStateWithError(@Param("id") Long id,
                           @Param("state") String state,
                           @Param("error") String error,
                           @Param("now") Instant now);
}
