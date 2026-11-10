package com.factory.events.application.saga;

import com.factory.events.domain.model.AuditLog;
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
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orquestrador de saga para registro de evento QUALITY (evento + auditoria).
 * <p>
 * Cada passo roda em transação curta ({@code REQUIRES_NEW}): nunca segura conexão
 * entre passos. Se um passo falha, compensa os passos anteriores e a saga vai para
 * {@code FAILED_COMPENSATED}. A compensação é um comando novo, auditável e
 * idempotente (RN-07) — nunca rollback físico silencioso.
 */
@Service
public class RegisterEventSaga {

  private static final String SAGA_TYPE = "register-event-quality-audit";

  private final SagaInstancePort sagaInstance;
  private final SagaStepPort sagaStep;
  private final EventRepositoryPort events;
  private final AuditRepositoryPort audits;
  private final OutboxPort outbox;
  private final ClockPort clock;
  private final ObjectMapper objectMapper;

  public RegisterEventSaga(SagaInstancePort sagaInstance,
                           SagaStepPort sagaStep,
                           EventRepositoryPort events,
                           AuditRepositoryPort audits,
                           OutboxPort outbox,
                           ClockPort clock,
                           ObjectMapper objectMapper) {
    this.sagaInstance = sagaInstance;
    this.sagaStep = sagaStep;
    this.events = events;
    this.audits = audits;
    this.outbox = outbox;
    this.clock = clock;
    this.objectMapper = objectMapper;
  }

  /**
   * Executa a saga de registro QUALITY.
   * Nunca {@code null}: {@link RegisterEventResult.Success} ou
   * {@link RegisterEventResult.Failure}.
   */
  public RegisterEventResult execute(@NonNull RegisterEventCommand command) {
    if (!EventType.QUALITY.equals(command.eventType())) {
      throw new IllegalArgumentException("Saga só para eventos QUALITY");
    }

    String sagaId = sagaInstance.createSagaInstance(
      SAGA_TYPE,
      command.eventId(),
      toJson(command)
    );

    // Passo 1: registrar evento (+ outbox)
    try {
      executeStep(sagaId, 0, "registrar-evento", command);
    } catch (Exception ex) {
      compensateStep(sagaId, 0);
      sagaInstance.markSagaFailedCompensated(
        sagaId,
        "Falha ao registrar evento: " + ex.getMessage()
      );
      return new RegisterEventResult.Failure(
        "Falha ao registrar evento: " + ex.getMessage()
      );
    }

    // Passo 2: gravar log de auditoria QUALITY
    Instant now = clock.now();
    AuditLog audit = new AuditLog(
      null,
      command.eventId(),
      "AUDITORIA QUALIDADE: Evento registrado em " + now +
        ". Descrição: " + command.description(),
      now
    );
    try {
      executeStep(sagaId, 1, "gravar-auditoria-qualidade", audit);
    } catch (Exception ex) {
      // Compensação do passo 1: desfaz o registro do evento (exclusão lógica)
      compensateStep(sagaId, 0);
      sagaInstance.markSagaFailedCompensated(
        sagaId,
        "Falha na auditoria QUALITY: " + ex.getMessage()
      );
      return new RegisterEventResult.Failure(
        "Falha na auditoria QUALITY: " + ex.getMessage()
      );
    }

    sagaInstance.markSagaSucceeded(sagaId);
    return events.findByEventId(command.eventId())
      .<RegisterEventResult>map(RegisterEventResult.Success::new)
      .orElseGet(() -> new RegisterEventResult.Failure(
        "Evento não encontrado após registro (eventId=" + command.eventId() + ")"
      ));
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void executeStep(String sagaId, int stepIndex, String stepName, Object payload) {
    SagaStepInput input = new SagaStepInput(stepName, toJson(payload));
    SagaStepOutcome outcome = sagaStep.execute(sagaId, stepIndex, input);
    switch (outcome) {
      case SagaStepOutcome.Failed failed ->
        throw new IllegalStateException("Passo " + stepName + " falhou: " + failed.reason());
      case SagaStepOutcome.CompensationFailed failed ->
        throw new IllegalStateException("Passo " + stepName + " falhou na compensação: " + failed.reason());
      default -> {
      }
    }
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  void compensateStep(String sagaId, int stepIndex) {
    SagaStepOutcome outcome = sagaStep.compensate(sagaId, stepIndex);
    switch (outcome) {
      case SagaStepOutcome.CompensationFailed failed ->
        throw new IllegalStateException("Compensação falhou: " + failed.reason());
      default -> {
      }
    }
  }

  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (Exception ex) {
      throw new IllegalStateException("Falha ao serializar payload da saga", ex);
    }
  }
}
