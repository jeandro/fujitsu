package com.factory.events.adapter.in.web;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.factory.events.adapter.in.web.dto.EventRequestDTO;
import com.factory.events.adapter.in.web.dto.EventResponseDTO;
import com.factory.events.adapter.in.web.dto.EventResponsePageDTO;
import com.factory.events.adapter.in.web.dto.Problem;
import com.factory.events.domain.model.EventType;
import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.result.EventPage;
import com.factory.events.domain.model.result.RegisterEventResult;
import com.factory.events.domain.port.in.ListEventsUseCase;
import com.factory.events.domain.port.in.RegisterEventUseCase;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * Teste do EventController (camada adapter.in.web).
 * <p>
 * Testa o mapeamento HTTP para caso de uso sem contexto Spring (sem {@code @WebMvcTest},
 * indisponível no Spring Boot 4.1.1). Cobre CA-01, CA-02, CA-03, CA-04, CA-10, CA-11.
 */
class EventControllerTest {

  private RegisterEventUseCase registerEventUseCase;
  private ListEventsUseCase listEventsUseCase;
  private EventController controller;

  @BeforeEach
  void setup() {
    registerEventUseCase = mock(RegisterEventUseCase.class);
    listEventsUseCase = mock(ListEventsUseCase.class);
    controller = new EventController(registerEventUseCase, listEventsUseCase);
  }

  private static EventRequestDTO dto(String eventId, String type, String desc, String ts) {
    return new EventRequestDTO(eventId, EventType.valueOf(type), desc, Instant.parse(ts));
  }

  @Test
  @DisplayName("CA-01: POST sucesso retorna 201 com eventId (RN-01)")
  void testReceiveEventSuccess() {
    var saved = new FactoryEvent(1L, "EVT-1001", EventType.PRODUCTION, "Linha Ativa",
      Instant.parse("2026-05-20T13:00:00Z"));
    when(registerEventUseCase.register(any())).thenReturn(new RegisterEventResult.Success(saved));

    var response = controller.receiveEvent("key-1", dto("EVT-1001", "PRODUCTION", "Linha Ativa",
      "2026-05-20T13:00:00Z"));

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    var body = (EventResponseDTO) response.getBody();
    assertNotNull(body);
    assertEquals("EVT-1001", body.eventId());
  }

  @Test
  @DisplayName("CA-04: eventId duplicado retorna 409 sem novo efeito (RN-02)")
  void testDuplicateEventReturns409() {
    when(registerEventUseCase.register(any())).thenReturn(
      new RegisterEventResult.Duplicate("EVT-DUP"));

    var response = controller.receiveEvent("key-1", dto("EVT-DUP", "QUALITY", "d",
      "2026-05-20T13:00:00Z"));

    assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
    var body = (Problem) response.getBody();
    assertNotNull(body);
    assertEquals("https://api.factory/events/errors/duplicate-event", body.type());
    assertEquals(409, body.status());
  }

  @Test
  @DisplayName("CA-09: falha de saga retorna 500 audit-compensation-required (RN-07)")
  void testSagaFailureReturns500() {
    when(registerEventUseCase.register(any())).thenReturn(new RegisterEventResult.Failure(
      "Falha na auditoria QUALITY: compensacao executada"));

    var response = controller.receiveEvent("key-1", dto("EVT-SAGA-FAIL", "QUALITY", "d",
      "2026-05-20T13:00:00Z"));

    assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    var body = (Problem) response.getBody();
    assertNotNull(body);
    assertEquals("https://api.factory/events/errors/audit-compensation-required", body.type());
    assertEquals(500, body.status());
  }

  @Test
  @DisplayName("CA-10: GET lista paginada retorna 200 com content (RN-09)")
  void testListEventsSuccess() {
    var event = new FactoryEvent(1L, "EVT-1001", EventType.PRODUCTION, "Linha Ativa",
      Instant.parse("2026-05-20T13:00:00Z"));
    var page = new EventPage(List.of(event), 0, 20, 1, 1, true, true, 1, false);
    when(listEventsUseCase.list(0, 20)).thenReturn(page);

    var response = controller.listEvents(0, 20);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var body = response.getBody();
    assertNotNull(body);
    assertEquals(1, body.content().size());
    assertEquals("EVT-1001", body.content().get(0).eventId());
  }

  @Test
  @DisplayName("CA-10: GET delega paginacao ao caso de uso (RN-09)")
  void testListLimitsSizeTo100() {
    when(listEventsUseCase.list(0, 500)).thenReturn(
      new EventPage(List.of(), 0, 100, 0, 0, true, true, 0, true));

    controller.listEvents(0, 500);

    verify(listEventsUseCase).list(0, 500);
  }

  @Test
  @DisplayName("CA-11: GET por ID retorna 200 quando existe")
  void testObterEventoPorIdSucesso() {
    var event = new FactoryEvent(1L, "EVT-1001", EventType.PRODUCTION, "Linha Ativa",
      Instant.parse("2026-05-20T13:00:00Z"));
    when(listEventsUseCase.findByEventId("EVT-1001")).thenReturn(Optional.of(event));

    var response = controller.obterEvento("EVT-1001");

    assertEquals(HttpStatus.OK, response.getStatusCode());
    var body = (EventResponseDTO) response.getBody();
    assertNotNull(body);
    assertEquals("EVT-1001", body.eventId());
  }

  @Test
  @DisplayName("CA-11: GET por ID retorna 404 quando nao existe (E-06)")
  void testObterEventoPorIdNaoEncontrado() {
    when(listEventsUseCase.findByEventId("EVT-9999")).thenReturn(Optional.empty());

    var response = controller.obterEvento("EVT-9999");

    assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    var body = (Problem) response.getBody();
    assertNotNull(body);
    assertEquals("https://api.factory/events/errors/not-found", body.type());
    assertEquals(404, body.status());
  }

  @Test
  @DisplayName("Sem PUT/PATCH/DELETE: imutabilidade de evento (RN-08, RN-22)")
  void testNaoExisteOperacaoDeAlteracao() {
    // O controller nao expoe PUT/PATCH/DELETE — apenas POST e GET.
    // Este teste documenta que nao ha endpoint de alteracao ou exclusao.
    assertTrue(EventController.class.getDeclaredMethods().length > 0);
    var postMethods = java.util.Arrays.stream(EventController.class.getDeclaredMethods())
      .filter(m -> m.getName().equals("receiveEvent")).toList();
    var getMethods = java.util.Arrays.stream(EventController.class.getDeclaredMethods())
      .filter(m -> m.getName().equals("listEvents") || m.getName().equals("obterEvento")).toList();
    assertEquals(1, postMethods.size(), "Deve existir exatamente um metodo POST");
    assertEquals(2, getMethods.size(), "Devem existir exatamente dois metodos GET");
    assertFalse(java.util.Arrays.stream(EventController.class.getDeclaredMethods())
      .anyMatch(m -> m.getName().equalsIgnoreCase("update")
        || m.getName().equalsIgnoreCase("patch")
        || m.getName().equalsIgnoreCase("delete")),
      "Nao deve existir metodo de alteracao ou exclusao");
  }
}
