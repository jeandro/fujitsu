package com.factory.events.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.factory.events.domain.model.EventType;
import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.result.EventPage;
import com.factory.events.domain.port.out.EventRepositoryPort;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ListEventsUseCaseImplTest {

  EventRepositoryPort events;
  ListEventsUseCaseImpl service;
  final Instant now = Instant.parse("2026-05-20T13:00:00Z");

  @BeforeEach
  void setup() {
    events = mock(EventRepositoryPort.class);
    service = new ListEventsUseCaseImpl(events);
  }

  @Test
  void listDelegatesToRepositoryWithPageable() {
    when(events.findAll(0, 20)).thenReturn(
      new EventPage(List.of(), 0, 20, 0, 0, true, true, 0, true)
    );
    var result = service.list(0, 20);
    assertEquals(0, result.totalElements());
    verify(events).findAll(0, 20);
  }

  @Test
  void listLimitaSizeAMaximo100() {
    when(events.findAll(0, 100)).thenReturn(
      new EventPage(List.of(), 0, 100, 0, 0, true, true, 0, true)
    );
    service.list(0, 500);
    verify(events).findAll(0, 100);
  }

  @Test
  void listPaginaMinimaEhZero() {
    when(events.findAll(0, 20)).thenReturn(
      new EventPage(List.of(), 0, 20, 0, 0, true, true, 0, true)
    );
    service.list(-5, 20);
    verify(events).findAll(0, 20);
  }

  @Test
  void findByEventIdReturnsEvent() {
    var evento = new FactoryEvent(1L, "EVT-1", EventType.PRODUCTION, "Teste", now);
    when(events.findByEventId("EVT-1")).thenReturn(Optional.of(evento));
    var result = service.findByEventId("EVT-1");
    assertTrue(result.isPresent());
    assertEquals("EVT-1", result.orElseThrow().eventId());
  }

  @Test
  void findByEventIdReturnsEmptyWhenNotFound() {
    when(events.findByEventId("EVT-NOTFOUND")).thenReturn(Optional.empty());
    var result = service.findByEventId("EVT-NOTFOUND");
    assertTrue(result.isEmpty());
  }
}
