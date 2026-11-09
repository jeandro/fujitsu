package com.factory.events.domain.model;

import java.time.Instant;

public record RegisterEventCommand(
  String eventId,
  EventType eventType,
  String description,
  Instant timestamp
) {}