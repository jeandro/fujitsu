package com.factory.events.service;

import com.factory.events.dto.EventRequestDTO;
import com.factory.events.dto.EventResponseDTO;
import com.factory.events.exception.DuplicateEventException;
import com.factory.events.model.AuditLog;
import com.factory.events.model.EventType;
import com.factory.events.model.FactoryEvent;
import com.factory.events.repository.AuditRepository;
import com.factory.events.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private AuditRepository auditRepository;

    @InjectMocks
    private EventService eventService;

    private EventRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        requestDTO = new EventRequestDTO("EVT-1001", EventType.PRODUCTION, "Início da esteira 01", LocalDateTime.now());
    }

    @Test
    @DisplayName("Deve processar e salvar evento de produção com sucesso")
    void testProcessEventProductionSuccess() {
        when(eventRepository.existsByEventId("EVT-1001")).thenReturn(false);
        FactoryEvent savedEvent = new FactoryEvent("EVT-1001", EventType.PRODUCTION, "Início da esteira 01", LocalDateTime.now());
        when(eventRepository.save(any(FactoryEvent.class))).thenReturn(savedEvent);

        EventResponseDTO response = eventService.processEvent(requestDTO);

        assertNotNull(response);
        assertEquals("EVT-1001", response.getEventId());
        verify(auditRepository, never()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Deve salvar log de auditoria ao processar evento de QUALIDADE")
    void testProcessEventQualityAuditSuccess() {
        requestDTO.setEventType(EventType.QUALITY);
        when(eventRepository.existsByEventId("EVT-1001")).thenReturn(false);
        FactoryEvent savedEvent = new FactoryEvent("EVT-1001", EventType.QUALITY, "Defeito detectado", LocalDateTime.now());
        when(eventRepository.save(any(FactoryEvent.class))).thenReturn(savedEvent);

        EventResponseDTO response = eventService.processEvent(requestDTO);

        assertNotNull(response);
        verify(auditRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Deve lançar DuplicateEventException quando ID do evento já existir")
    void testProcessEventDuplicateThrowsException() {
        when(eventRepository.existsByEventId("EVT-1001")).thenReturn(true);

        assertThrows(DuplicateEventException.class, () -> eventService.processEvent(requestDTO));
        verify(eventRepository, never()).save(any(FactoryEvent.class));
    }

    @Test
    @DisplayName("Deve listar todos os eventos cadastrados com sucesso")
    void testFindAllEventsSuccess() {
        FactoryEvent event = new FactoryEvent("EVT-1001", EventType.PRODUCTION, "Linha ativa", LocalDateTime.now());
        when(eventRepository.findAll()).thenReturn(List.of(event));

        List<EventResponseDTO> events = eventService.findAllEvents();

        assertEquals(1, events.size());
        assertEquals("EVT-1001", events.get(0).getEventId());
    }
}
