package com.factory.events.adapter.out.persistence;

import tools.jackson.databind.ObjectMapper;
import com.factory.events.domain.model.AuditLog;
import com.factory.events.domain.model.EventType;
import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.RegisterEventCommand;
import com.factory.events.domain.model.result.SagaStepInput;
import com.factory.events.domain.model.result.SagaStepOutcome;
import com.factory.events.domain.port.out.AuditRepositoryPort;
import com.factory.events.domain.port.out.EventRepositoryPort;
import com.factory.events.domain.port.out.OutboxPort;
import com.factory.events.domain.port.out.SagaStepPort;
import java.time.Instant;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

/**
 * Adaptador de execução e compensação de passos de saga orquestrada.
 * <p>
 * Não segura conexão entre passos: cada chamada roda na transação curta do
 * orquestrador ({@link com.factory.events.application.saga.RegisterEventSaga}
 * com {@code REQUIRES_NEW}).
 * <p>
 * A compensação do passo {@code registrar-evento} desfaz o registro do evento
 * por exclusão lógica ({@code status = 'CANCELLED'} — o evento some da consulta
 * normal mas permanece para auditoria) usando o {@code eventId} armazenado na
 * linha do passo de saga. Compensação é um comando novo, auditável e
 * idempotente (reexecutar a exclusão lógica sobre evento já cancelado é efeito
 * nulo), nunca rollback físico silencioso.
 */
@Component
public class SagaStepAdapter implements SagaStepPort {

  private static final String PASSO_REGISTRAR_EVENTO = "registrar-evento";
  private static final String PASSO_GRAVAR_AUDITORIA_QUALIDADE = "gravar-auditoria-qualidade";

  private final ObjectMapper objectMapper;
  private final EventRepositoryPort events;
  private final AuditRepositoryPort audits;
  private final OutboxPort outbox;
  private final SagaStepJpaRepository stepRepository;

  public SagaStepAdapter(ObjectMapper objectMapper,
                         EventRepositoryPort events,
                         AuditRepositoryPort audits,
                         OutboxPort outbox,
                         SagaStepJpaRepository stepRepository) {
    this.objectMapper = objectMapper;
    this.events = events;
    this.audits = audits;
    this.outbox = outbox;
    this.stepRepository = stepRepository;
  }

  @Override
  @NonNull
  public SagaStepOutcome execute(@NonNull String sagaId, int stepIndex,
                                 @NonNull SagaStepInput input) {
    long sagaIdLong = sagaIdOf(sagaId);
    try {
      switch (input.stepName()) {
        case PASSO_REGISTRAR_EVENTO -> {
          var cmd = objectMapper.readValue(input.payload(), RegisterEventCommand.class);
          var event = new FactoryEvent(
            null, cmd.eventId(), cmd.eventType(), cmd.description(), cmd.timestamp()
          );
          var saved = events.saveAndFlush(event);
          outbox.save(
            "FactoryEvent", saved.eventId(), "EventRegistered", 1,
            toJson(saved)
          );
          stepRepository.updateStepEventId(sagaIdLong, stepIndex, saved.eventId());
          stepRepository.updateStepState(sagaIdLong, stepIndex, "SUCCEEDED", Instant.now());
          return new SagaStepOutcome.Completed();
        }
        case PASSO_GRAVAR_AUDITORIA_QUALIDADE -> {
          var audit = objectMapper.readValue(input.payload(), AuditLog.class);
          audits.save(audit);
          stepRepository.updateStepState(sagaIdLong, stepIndex, "SUCCEEDED", Instant.now());
          return new SagaStepOutcome.Completed();
        }
        default -> {
          return new SagaStepOutcome.Failed("Passo desconhecido: " + input.stepName());
        }
      }
    } catch (Exception ex) {
      try {
        stepRepository.updateStepState(sagaIdLong, stepIndex, "FAILED", Instant.now());
      } catch (Exception ignored) {
        // não falhar a compensação por erro de persistência de estado
      }
      return new SagaStepOutcome.Failed(ex.getMessage());
    }
  }

  @Override
  @NonNull
  public SagaStepOutcome compensate(@NonNull String sagaId, int stepIndex) {
    long sagaIdLong = sagaIdOf(sagaId);
    try {
      var steps = stepRepository.findBySagaIdOrderBySeq(sagaIdLong);
      var step = steps.stream()
        .filter(s -> s.getSeq() == stepIndex)
        .findFirst()
        .orElse(null);

      if (step == null) {
        return new SagaStepOutcome.Compensated();
      }

      switch (step.getName()) {
        case PASSO_REGISTRAR_EVENTO -> {
          // Exclusão lógica: marca o evento como cancelado (não aparece na
          // consulta normal, permanece para auditoria). Idempotente: evento
          // já cancelado não produz efeito adicional (UPDATE afeta 0 linhas).
          String eventId = step.getEventId();
          if (eventId != null && !eventId.isBlank()) {
            events.cancelEvent(eventId);
          }
          stepRepository.updateCompensationState(sagaIdLong, stepIndex, "DONE", Instant.now());
          return new SagaStepOutcome.Compensated();
        }
        case PASSO_GRAVAR_AUDITORIA_QUALIDADE -> {
          stepRepository.updateCompensationState(sagaIdLong, stepIndex, "DONE", Instant.now());
          return new SagaStepOutcome.Compensated();
        }
        default -> {
          return new SagaStepOutcome.Compensated();
        }
      }
    } catch (Exception ex) {
      return new SagaStepOutcome.CompensationFailed(ex.getMessage());
    }
  }

  private long sagaIdOf(String sagaId) {
    return Long.parseLong(sagaId.replace("saga-", ""));
  }

  private String toJson(FactoryEvent event) {
    try {
      return objectMapper.writeValueAsString(event);
    } catch (Exception ex) {
      throw new IllegalStateException("Falha ao serializar evento para outbox", ex);
    }
  }
}
