package com.factory.events.exception;

/**
 * Exceção lançada quando uma tentativa de cadastrar um eventId duplicado é detectada.
 */
public class DuplicateEventException extends RuntimeException {

  public DuplicateEventException(String eventId) {
    super(
      "Evento duplicado! O evento com ID '" +
        eventId +
        "' já foi processado anteriormente."
    );
  }
}
