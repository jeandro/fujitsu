package com.factory.events.application;

import com.factory.events.domain.model.EventType;
import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.RegisterEventCommand;
import com.factory.events.domain.model.result.RegisterEventResult;
import com.factory.events.domain.port.in.RegisterEventUseCase;
import com.factory.events.domain.port.out.AuditRepositoryPort;
import com.factory.events.domain.port.out.EventRepositoryPort;
import com.factory.events.domain.port.out.OutboxPort;
import com.factory.events.application.saga.RegisterEventSaga;
import tools.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Caso de uso de registro de evento.
 * Para QUALITY, delega à saga orquestrada (evento + auditoria em passos com compensação).
 * Para os demais tipos, grava evento + outbox em uma única transação.
 */
@Service
public class RegisterEventUseCaseImpl implements RegisterEventUseCase {

  private final EventRepositoryPort events;
  private final AuditRepositoryPort audits;
  private final OutboxPort outbox;
  private final RegisterEventSaga saga;
  private final ObjectMapper objectMapper;

  public RegisterEventUseCaseImpl(EventRepositoryPort events,
                                  AuditRepositoryPort audits,
                                  OutboxPort outbox,
                                  RegisterEventSaga saga,
                                  ObjectMapper objectMapper) {
    this.events = events;
    this.audits = audits;
    this.outbox = outbox;
    this.saga = saga;
    this.objectMapper = objectMapper;
  }

  @Override
  @Transactional
  public RegisterEventResult register(@NonNull RegisterEventCommand command) {
    if (events.existsByEventId(command.eventId())) {
      return new RegisterEventResult.Duplicate(command.eventId());
    }

    if (EventType.QUALITY.equals(command.eventType())) {
      return saga.execute(command);
    }

    return registerSimple(command);
  }

  private RegisterEventResult registerSimple(RegisterEventCommand command) {
    var event = new FactoryEvent(
      null,
      command.eventId(),
      command.eventType(),
      command.description(),
      command.timestamp()
    );
    var saved = events.saveAndFlush(event);
    outbox.save(
      "FactoryEvent",
      saved.eventId(),
      "EventRegistered",
      1,
      toJson(saved)
    );
    return new RegisterEventResult.Success(saved);
  }

  private String toJson(FactoryEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (Exception ex) {
      throw new IllegalStateException("Falha ao serializar evento para outbox", ex);
    }
  }
}
