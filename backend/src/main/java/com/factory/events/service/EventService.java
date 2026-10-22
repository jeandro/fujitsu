package com.factory.events.service;

import com.factory.events.dto.EventRequestDTO;
import com.factory.events.dto.EventResponseDTO;
import com.factory.events.exception.DuplicateEventException;
import com.factory.events.model.AuditLog;
import com.factory.events.model.EventType;
import com.factory.events.model.FactoryEvent;
import com.factory.events.repository.AuditRepository;
import com.factory.events.repository.EventRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventRepository eventRepository;
    private final AuditRepository auditRepository;

    public EventService(EventRepository eventRepository, AuditRepository auditRepository) {
        this.eventRepository = eventRepository;
        this.auditRepository = auditRepository;
    }

    @Transactional
    public EventResponseDTO processEvent(EventRequestDTO requestDTO) {
        String normalizedId = requestDTO.getEventId().trim();

        if (eventRepository.existsByEventId(normalizedId)) {
            throw new DuplicateEventException("Evento duplicado! O evento com ID '" + normalizedId + "' já foi cadastrado.");
        }

        FactoryEvent event = new FactoryEvent(
            normalizedId,
            requestDTO.getEventType(),
            requestDTO.getDescription().trim(),
            requestDTO.getTimestamp()
        );

        FactoryEvent savedEvent;
        try {
            savedEvent = eventRepository.save(event);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateEventException("Evento duplicado! O evento com ID '" + normalizedId + "' já foi cadastrado concorrentemente.");
        }

        if (savedEvent.getEventType() == EventType.QUALITY) {
            AuditLog auditLog = new AuditLog(
                savedEvent.getEventId(),
                "Auditoria de Qualidade registrada em " + LocalDateTime.now() + " - Descrição: " + savedEvent.getDescription(),
                LocalDateTime.now()
            );
            auditRepository.save(auditLog);
        }

        return new EventResponseDTO(
            savedEvent.getEventId(),
            savedEvent.getEventType(),
            savedEvent.getDescription(),
            savedEvent.getTimestamp()
        );
    }

    @Transactional(readOnly = true)
    public List<EventResponseDTO> findAllEvents() {
        return eventRepository.findAll().stream()
            .map(e -> new EventResponseDTO(e.getEventId(), e.getEventType(), e.getDescription(), e.getTimestamp()))
            .collect(Collectors.toList());
    }
}
