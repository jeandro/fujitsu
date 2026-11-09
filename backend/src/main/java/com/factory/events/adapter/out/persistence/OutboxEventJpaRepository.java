package com.factory.events.adapter.out.persistence;

import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OutboxEventJpaRepository
  extends JpaRepository<OutboxEventJpaEntity, Long> {

  @Query(
    value = """
      INSERT INTO outbox_event (aggregate_type, aggregate_id, event_type, event_version, payload, occurred_at)
      VALUES (:aggregateType, :aggregateId, :eventType, :eventVersion, :payload, :occurredAt)
      """,
    nativeQuery = true
  )
  void saveOutbox(@Param("aggregateType") String aggregateType,
                  @Param("aggregateId") String aggregateId,
                  @Param("eventType") String eventType,
                  @Param("eventVersion") String eventVersion,
                  @Param("payload") String payload,
                  @Param("occurredAt") Instant occurredAt);
}
