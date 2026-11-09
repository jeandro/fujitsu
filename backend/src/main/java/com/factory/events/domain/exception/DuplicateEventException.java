package com.factory.events.domain.exception;

public class DuplicateEventException extends RuntimeException {

  public DuplicateEventException(String eventId) {
    super(
      "Evento duplicado! O evento com ID '" +
        eventId +
        "' já foi processado anteriormente."
    );
  }
}