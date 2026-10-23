package com.factory.events.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.factory.events.dto.*;
import com.factory.events.exception.DuplicateEventException;
import com.factory.events.model.*;
import com.factory.events.repository.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Sort;

class EventServiceTest {

  EventRepository events;
  AuditRepository audits;
  EventService service;
  final Instant now = Instant.parse("2026-05-20T13:00:00Z");

  @BeforeEach
  void setup() {
    events = mock(EventRepository.class);
    audits = mock(AuditRepository.class);
    service = new EventService(
      events,
      audits,
      Clock.fixed(now, ZoneOffset.UTC)
    );
  }

  EventRequestDTO event(EventType type) {
    var dto = new EventRequestDTO();
    dto.setEventId(" TEST-1 ");
    dto.setEventType(type);
    dto.setDescription("Teste");
    dto.setTimestamp(now);
    return dto;
  }

  @Test
  void productionDoesNotAudit() {
    var result = service.processEvent(event(EventType.PRODUCTION));
    assertEquals("TEST-1", result.getEventId());
    assertEquals(now, result.getTimestamp());
    verify(events).saveAndFlush(any());
    verifyNoInteractions(audits);
  }

  @Test
  void qualityRecordsContentAndSameClockInstant() {
    service.processEvent(event(EventType.QUALITY));
    var capture = ArgumentCaptor.forClass(AuditLog.class);
    verify(audits).save(capture.capture());
    assertEquals("TEST-1", capture.getValue().getEventId());
    assertEquals(now, capture.getValue().getCreatedAt());
    assertTrue(capture.getValue().getLogMessage().contains("Teste"));
  }

  @Test
  void duplicateDoesNotWriteAnything() {
    when(events.existsByEventId("TEST-1")).thenReturn(true);
    assertThrows(DuplicateEventException.class, () ->
      service.processEvent(event(EventType.QUALITY))
    );
    verify(events, never()).saveAndFlush(any());
    verifyNoInteractions(audits);
  }

  @Test
  void listMapsAndOrdersEvents() {
    when(events.findAll(any(Sort.class))).thenReturn(
      List.of(new FactoryEvent("X", EventType.PRODUCTION, "d", now))
    );
    assertEquals("X", service.findAllEvents().get(0).getEventId());
  }

  @Test
  void unrelatedIntegrityErrorIsNotDuplicate() {
    when(events.saveAndFlush(any())).thenThrow(
      new org.springframework.dao.DataIntegrityViolationException("other")
    );
    assertThrows(
      org.springframework.dao.DataIntegrityViolationException.class,
      () -> service.processEvent(event(EventType.PRODUCTION))
    );
  }
}
