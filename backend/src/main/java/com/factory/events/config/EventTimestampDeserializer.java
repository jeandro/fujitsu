package com.factory.events.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeParseException;

/** Preserva o instante e aceita o exemplo legado sem offset do enunciado. */
public class EventTimestampDeserializer extends JsonDeserializer<Instant> {

  @Override
  public Instant deserialize(JsonParser parser, DeserializationContext context)
    throws IOException {
    String value = parser.getValueAsString();
    if (value != null) {
      try {
        return OffsetDateTime.parse(value).toInstant();
      } catch (DateTimeParseException ignored) {
        try {
          return LocalDateTime.parse(value)
            .atZone(ZoneId.of("America/Sao_Paulo"))
            .toInstant();
        } catch (DateTimeParseException invalid) {
          /* resposta pública tratada pelo handler */
        }
      }
    }
    return (Instant) context.handleUnexpectedToken(Instant.class, parser);
  }
}
