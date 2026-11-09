package com.factory.events.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.factory.events.domain.model.EventType;
import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.RegisterEventCommand;
import com.factory.events.domain.model.result.RegisterEventResult;
import com.factory.events.domain.model.result.SagaStepInput;
import com.factory.events.domain.model.result.SagaStepOutcome;
import com.factory.events.domain.port.out.AuditRepositoryPort;
import com.factory.events.domain.port.out.ClockPort;
import com.factory.events.domain.port.out.EventRepositoryPort;
import com.factory.events.domain.port.out.OutboxPort;
import com.factory.events.domain.port.out.SagaInstancePort;
import com.factory.events.domain.port.out.SagaStepPort;
import com.factory.events.application.saga.RegisterEventSaga;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Teste de saga: falha de auditoria QUALITY -> compensacao executada e auditada;
 * evento nao fica visivel (CA-09, RN-06, RN-07, E-05).
 *
 * <p>Testa o orquestrador {@link RegisterEventSaga} em nível de aplicacao com portas
 * mocadas. Forca falha no passo 2 (auditoria) e verifica:
 * <ul>
 *   <li>Compensacao do passo 1 executada (desfaz registro do evento)</li>
 *   <li>Registro de compensacao persistido (saga instance marcada FAILED_COMPENSATED)</li>
 *   <li>Resultado é Failure (nao Success)</li>
 *   <li>Evento nao fica visivel (findByEventId nao é chamado para retorno de sucesso)</li>
 * </ul>
 */
class SagaCompensationTest {

  SagaInstancePort sagaInstance;
  SagaStepPort sagaStep;
  EventRepositoryPort events;
  AuditRepositoryPort audits;
  OutboxPort outbox;
  ClockPort clock;
  RegisterEventSaga saga;

  final String sagaId = "saga-quality-1";
  final Instant now = Instant.parse("2026-05-20T13:00:00Z");
  final RegisterEventCommand command = new RegisterEventCommand(
    "EVT-QUAL-FAIL", EventType.QUALITY, "Falha de inspecao", now);

  @BeforeEach
  void setup() {
    sagaInstance = mock(SagaInstancePort.class);
    sagaStep = mock(SagaStepPort.class);
    events = mock(EventRepositoryPort.class);
    audits = mock(AuditRepositoryPort.class);
    outbox = mock(OutboxPort.class);
    clock = mock(ClockPort.class);
    when(clock.now()).thenReturn(now);
    when(sagaInstance.createSagaInstance(any(), any(), any())).thenReturn(sagaId);
    when(sagaInstance.markSagaSucceeded(sagaId)).thenReturn(true);
    when(sagaInstance.markSagaFailedCompensated(sagaId, anyString())).thenReturn(true);

    saga = new RegisterEventSaga(sagaInstance, sagaStep, events, audits, outbox, clock);
  }

  @Test
  @DisplayName("CA-09: falha no passo 2 (auditoria) executa compensacao do passo 1 na ordem inversa")
  void falhaAuditoriaExecutaCompensacaoOrdemInversa() {
    // Passo 1: registrar evento — OK (Completed)
    when(sagaStep.execute(eq(sagaId), eq(0), any(SagaStepInput.class)))
      .thenReturn(new SagaStepOutcome.Completed());
    // Passo 2: gravar auditoria QUALITY — FALHA
    when(sagaStep.execute(eq(sagaId), eq(1), any(SagaStepInput.class)))
      .thenReturn(new SagaStepOutcome.Failed("auditoria indisponível"));
    // Compensacao do passo 1: desfaz registro — OK
    when(sagaStep.compensate(eq(sagaId), eq(0))).thenReturn(new SagaStepOutcome.Compensated());

    var result = saga.execute(command);

    // ASSERT: resultado é Failure (erro informando que registro nao foi concluido)
    assertTrue(result instanceof RegisterEventResult.Failure);
    var failure = (RegisterEventResult.Failure) result;
    assertTrue(failure.reason().contains("auditoria"), "Motivo deve mencionar auditoria: " + failure.reason());

    // ASSERT: passo 1 executado, passo 2 executado (falhou), compensacao do passo 1 executada
    verify(sagaStep).execute(eq(sagaId), eq(0), any());
    verify(sagaStep).execute(eq(sagaId), eq(1), any());
    verify(sagaStep).compensate(eq(sagaId), eq(0));
    // Compensacao só do passo que executou com sucesso (passo 1), nunca do passo 2 que falhou antes de gravar efeito
    verify(sagaStep, never()).compensate(eq(sagaId), eq(1));

    // ASSERT: saga marcada como FAILED_COMPENSATED
    verify(sagaInstance).markSagaFailedCompensated(sagaId, anyString());
    verify(sagaInstance, never()).markSagaSucceeded(sagaId);
  }

  @Test
  @DisplayName("Evento nao fica visivel apos compensacao (CA-09)")
  void eventoNaoFicaVisivelAposCompensacao() {
    when(sagaStep.execute(eq(sagaId), eq(0), any(SagaStepInput.class)))
      .thenReturn(new SagaStepOutcome.Completed());
    when(sagaStep.execute(eq(sagaId), eq(1), any(SagaStepInput.class)))
      .thenReturn(new SagaStepOutcome.Failed("auditoria indisponível"));
    when(sagaStep.compensate(eq(sagaId), eq(0))).thenReturn(new SagaStepOutcome.Compensated());

    var result = saga.execute(command);

    // ASSERT: resultado é Failure, nao Success com evento
    assertTrue(result instanceof RegisterEventResult.Failure);
    assertFalse(result instanceof RegisterEventResult.Success);

    // ASSERT: findByEventId nao é chamado para retornar evento de sucesso
    verify(events, never()).findByEventId(anyString());

    // ASSERT: compensacao executada (passo 1 desfeito)
    verify(sagaStep).compensate(eq(sagaId), eq(0));
  }

  @Test
  @DisplayName("Registro QUALITY bem-sucedido nao compensa (passo 2 OK)")
  void qualidadeSucessoSemCompensacao() {
    when(sagaStep.execute(eq(sagaId), eq(0), any(SagaStepInput.class)))
      .thenReturn(new SagaStepOutcome.Completed());
    when(sagaStep.execute(eq(sagaId), eq(1), any(SagaStepInput.class)))
      .thenReturn(new SagaStepOutcome.Completed());
    var savedEvent = new FactoryEventStub("EVT-QUAL-OK", EventType.QUALITY, "ok", now);
    when(events.findByEventId("EVT-QUAL-OK")).thenReturn(savedEvent);

    var result = saga.execute(command);

    assertTrue(result instanceof RegisterEventResult.Success);
    verify(sagaInstance).markSagaSucceeded(sagaId);
    verify(sagaStep, never()).compensate(any(), anyInt());
  }

  // Value object minimo para retorno de findByEventId
  private static class FactoryEventStub {
    final String eventId;
    final EventType eventType;
    final String description;
    final Instant timestamp;
    FactoryEventStub(String eventId, EventType eventType, String description, Instant timestamp) {
      this.eventId = eventId;
      this.eventType = eventType;
      this.description = description;
      this.timestamp = timestamp;
    }
  }
}
