package com.factory.events.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SagaStepJpaRepository
  extends JpaRepository<SagaStepJpaEntity, Long> {

  List<SagaStepJpaEntity> findBySagaIdOrderBySeq(Long sagaId);

  @Modifying
  @Query(
    value = """
          INSERT INTO saga_step (saga_id, seq, name, state, compensation_state, updated_at)
          VALUES (:sagaId, :seq, :name, 'STARTED', 'NONE', :now)
          """,
    nativeQuery = true
  )
  void insertStep(@Param("sagaId") Long sagaId,
    @Param("seq") int seq,
    @Param("name") String name,
    @Param("now") Instant now);

  @Modifying
  @Query(
    value = "UPDATE saga_step SET state = :state, updated_at = :now WHERE saga_id = :sagaId AND seq = :seq",
    nativeQuery = true
  )
  int updateStepState(@Param("sagaId") Long sagaId,
    @Param("seq") int seq,
    @Param("state") String state,
    @Param("now") Instant now);

  @Modifying
  @Query(
    value = "UPDATE saga_step SET compensation_state = :compState, updated_at = :now WHERE saga_id = :sagaId AND seq = :seq",
    nativeQuery = true
  )
  int updateCompensationState(@Param("sagaId") Long sagaId,
    @Param("seq") int seq,
    @Param("compState") String compState,
    @Param("now") Instant now);

  @Modifying
  @Query(
    value = "UPDATE saga_step SET event_id = :eventId WHERE saga_id = :sagaId AND seq = :seq",
    nativeQuery = true
  )
  int updateStepEventId(@Param("sagaId") Long sagaId,
    @Param("seq") int seq,
    @Param("eventId") String eventId);
}
