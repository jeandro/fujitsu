package com.factory.events.service;

import com.factory.events.dto.*;
import com.factory.events.exception.DuplicateEventException;
import com.factory.events.model.*;
import com.factory.events.repository.*;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventService {

  private final EventRepository events;
  private final AuditRepository audits;
  private final Clock clock;

  public EventService(
    EventRepository events,
    AuditRepository audits,
    Clock clock
  ) {
    this.events = events;
    this.audits = audits;
    this.clock = clock;
  }

  @Transactional
  public EventResponseDTO processEvent(EventRequestDTO dto) {
    if (
      events.existsByEventId(dto.getEventId())
    ) throw new DuplicateEventException(dto.getEventId());
    FactoryEvent event = new FactoryEvent(
      dto.getEventId(),
      dto.getEventType(),
      dto.getDescription(),
      dto.getTimestamp()
    );
    try {
      // O flush força a disputa pela chave única dentro deste ponto de tratamento.
      events.saveAndFlush(event);
    } catch (DataIntegrityViolationException ex) {
      if (isEventIdConflict(ex)) throw new DuplicateEventException(
        dto.getEventId()
      );
      throw ex;
    }
    if (dto.getEventType() == EventType.QUALITY) {
      Instant now = clock.instant();
      audits.save(
        new AuditLog(
          dto.getEventId(),
          "AUDITORIA QUALIDADE: Evento registrado em " +
            now +
            ". Descrição: " +
            dto.getDescription(),
          now
        )
      );
    }
    return toResponse(event);
  }

  private boolean isEventIdConflict(Throwable ex) {
    for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
      if (cause instanceof ConstraintViolationException violation) {
        String name = violation.getConstraintName();
        if (
          name != null &&
          name.toLowerCase(Locale.ROOT).contains("uk_events_event_id")
        ) return true;
      }
    }
    return false;
  }

  @Transactional(readOnly = true)
  public List<EventResponseDTO> findAllEvents() {
    return events
      .findAll(Sort.by(Sort.Order.desc("timestamp"), Sort.Order.desc("id")))
      .stream()
      .map(this::toResponse)
      .toList();
  }

  private EventResponseDTO toResponse(FactoryEvent event) {
    return new EventResponseDTO(
      event.getEventId(),
      event.getEventType(),
      event.getDescription(),
      event.getTimestamp()
    );
  }
}
