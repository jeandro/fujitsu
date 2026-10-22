package com.factory.events.repository;

import com.factory.events.model.FactoryEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<FactoryEvent, Long> {
    boolean existsByEventId(String eventId);
    Optional<FactoryEvent> findByEventId(String eventId);
}
