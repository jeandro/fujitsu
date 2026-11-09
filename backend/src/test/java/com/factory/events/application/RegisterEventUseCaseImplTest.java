package com.factory.events.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.factory.events.domain.model.EventType;
import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.RegisterEventCommand;
import com.factory.events.domain.model.result.RegisterEventResult;
import com.factory.events.domain.port.out.AuditRepositoryPort;
import com.factory.events.domain.port.out.ClockPort;
import com.factory.events.domain.port.out.EventRepositoryPort;
import com.factory.events.domain.port.out.OutboxPort;
import com.factory.events.application.saga.RegisterEventSaga;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegisterEventUseCaseImplTest {

  EventRepositoryPort events;
  AuditRepositoryPort audits;
  OutboxPort outbox;
  ClockPort clock;
  RegisterEventSaga saga;
  RegisterEventUseCaseImpl service;
  final Instant now = Instant.parse("2026-05-20T13:00:00Z");

  @BeforeEach
  void setup() {
    events = mock(EventRepositoryPort.class);
    audits = mock(AuditRepositoryPort.class);
    outbox = mock(OutboxPort.class);
    clock = mock(ClockPort.class);
    when(clock.now()).thenReturn(now);
    saga = mock(RegisterEventSaga.class);
    service = new RegisterEventUseCaseImpl(events, audits, outbox, clock, saga);
  }

  RegisterEventCommand cmd(EventType type) {
    return new RegisterEventCommand("TEST-1", type, "Teste", now);
  }

  @Test
  void productionRetornaSuccessESemAuditoria() {
    var saved = new FactoryEvent(1L, "TEST-1", EventType.PRODUCTION, "Teste", now);
    when(events.existsByEventId("TEST-1")).thenReturn(false);
    when(events.saveAndFlush(any(FactoryEvent.class))).thenReturn(saved);

    var result = service.register(cmd(EventType.PRODUCTION));

    assertTrue(result instanceof RegisterEventResult.Success);
    var success = (RegisterEventResult.Success) result;
    assertEquals("TEST-1", success.event().eventId());
    verify(events).saveAndFlush(any(FactoryEvent.class));
    verify(outbox).save(
      eq("FactoryEvent"), eq("TEST-1"), eq("EventRegistered"), anyInt(), anyString()
    );
    verifyNoInteractions(audits);
    verifyNoInteractions(saga);
  }

  @Test
  void qualityDelegasSaga() {
    var saved = new FactoryEvent(1L, "TEST-1", EventType.QUALITY, "Teste", now);
    when(events.existsByEventId("TEST-1")).thenReturn(false);
    when(saga.execute(any(RegisterEventCommand.class)))
      .thenReturn(new RegisterEventResult.Success(saved));

    var result = service.register(cmd(EventType.QUALITY));

    assertTrue(result instanceof RegisterEventResult.Success);
    verify(saga).execute(any(RegisterEventCommand.class));
    verifyNoInteractions(audits);
    verify(events, never()).saveAndFlush(any(FactoryEvent.class));
  }

  @Test
  void duplicateRetornaDuplicateSemGravarNada() {
    when(events.existsByEventId("TEST-1")).thenReturn(true);

    var result = service.register(cmd(EventType.QUALITY));

    assertTrue(result instanceof RegisterEventResult.Duplicate);
    var dup = (RegisterEventResult.Duplicate) result;
    assertEquals("TEST-1", dup.eventId());
    verify(events, never()).saveAndFlush(any());
    verifyNoInteractions(audits);
    verifyNoInteractions(saga);
  }
}
