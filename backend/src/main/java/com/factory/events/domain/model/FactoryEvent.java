package com.factory.events.domain.model;

import java.time.Instant;

/**
 * Evento de fábrica. Value object imutável ({@code record}).
 * Invariantes de integridade (eventId não vazio, description não vazia) são
 * garantidas por constraints do banco e validação na borda (Bean Validation).
 */
public record FactoryEvent(
  Long id,
  String eventId,
  EventType eventType,
  String description,
  Instant timestamp
) {}
