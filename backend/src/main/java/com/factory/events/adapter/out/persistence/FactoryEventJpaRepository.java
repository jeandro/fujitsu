package com.factory.events.adapter.out.persistence;

import com.factory.events.domain.model.EventType;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

public interface FactoryEventJpaRepository
  extends JpaRepository<FactoryEventJpaEntity, Long> {

  boolean existsByEventId(String eventId);

  @Query("SELECT e FROM FactoryEventJpaEntity e WHERE e.eventId = ?1 AND e.status = 'ACTIVE'")
  Optional<FactoryEventJpaEntity> findByEventIdAndStatusActive(String eventId);

  @Query("SELECT e FROM FactoryEventJpaEntity e WHERE e.status = 'ACTIVE' ORDER BY e.timestamp DESC, e.id DESC")
  Page<FactoryEventJpaEntity> findAllByStatusActive(Pageable pageable);

  @Modifying
  @Query(
    value = "UPDATE tb_factory_events SET status = 'CANCELLED' WHERE event_id = ?1 AND status = 'ACTIVE'",
    nativeQuery = true
  )
  int cancelByEventId(String eventId);
}
