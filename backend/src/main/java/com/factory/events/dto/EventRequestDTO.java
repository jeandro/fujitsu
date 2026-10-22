package com.factory.events.dto;

import com.factory.events.model.EventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public class EventRequestDTO {

    @NotBlank(message = "O ID do evento é obrigatório")
    @Size(max = 100, message = "O ID do evento deve ter no máximo 100 caracteres")
    private String eventId;

    @NotNull(message = "O tipo de evento é obrigatório")
    private EventType eventType;

    @NotBlank(message = "A descrição é obrigatória")
    @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres")
    private String description;

    @NotNull(message = "O timestamp é obrigatório")
    private LocalDateTime timestamp;

    public EventRequestDTO() {}

    public EventRequestDTO(String eventId, EventType eventType, String description, LocalDateTime timestamp) {
        this.eventId = eventId;
        this.eventType = eventType;
        this.description = description;
        this.timestamp = timestamp;
    }

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }

    public EventType getEventType() { return eventType; }
    public void setEventType(EventType eventType) { this.eventType = eventType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
