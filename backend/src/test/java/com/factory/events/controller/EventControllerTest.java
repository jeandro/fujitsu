package com.factory.events.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.factory.events.dto.EventRequestDTO;
import com.factory.events.dto.EventResponseDTO;
import com.factory.events.model.EventType;
import com.factory.events.service.EventService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventController.class)
class EventControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private EventService eventService;

  @Test
  @DisplayName("Deve retornar 201 Created ao registrar evento com sucesso")
  void testReceiveEventSuccess() throws Exception {
    EventResponseDTO response = new EventResponseDTO(
      "EVT-1001",
      EventType.PRODUCTION,
      "Linha Ativa",
      Instant.now()
    );
    when(eventService.processEvent(any(EventRequestDTO.class))).thenReturn(
      response
    );

    String jsonPayload = """
        {
            "eventId": "EVT-1001",
            "eventType": "PRODUCTION",
            "description": "Linha Ativa",
            "timestamp": "2026-05-20T10:00:00"
        }
    """;

    mockMvc
      .perform(
        post("/api/events")
          .contentType(MediaType.APPLICATION_JSON)
          .content(jsonPayload)
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.eventId").value("EVT-1001"));
  }

  @Test
  @DisplayName("Deve retornar 200 OK e lista de eventos no GET")
  void testGetAllEventsSuccess() throws Exception {
    EventResponseDTO response = new EventResponseDTO(
      "EVT-1001",
      EventType.PRODUCTION,
      "Linha Ativa",
      Instant.now()
    );
    when(eventService.findAllEvents()).thenReturn(List.of(response));

    mockMvc
      .perform(get("/api/events"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].eventId").value("EVT-1001"));
  }

  @Test
  void unexpectedFailureDoesNotLeakInternalDetails() throws Exception {
    when(eventService.findAllEvents()).thenThrow(
      new IllegalStateException("SECRET_DB_PASSWORD")
    );
    mockMvc
      .perform(get("/api/events"))
      .andExpect(status().isInternalServerError())
      .andExpect(jsonPath("$.status").value(500))
      .andExpect(jsonPath("$.requestId").isNotEmpty())
      .andExpect(jsonPath("$.timestamp").isNotEmpty())
      .andExpect(
        content().string(
          org.hamcrest.Matchers.not(
            org.hamcrest.Matchers.containsString("SECRET_DB_PASSWORD")
          )
        )
      );
  }
}
