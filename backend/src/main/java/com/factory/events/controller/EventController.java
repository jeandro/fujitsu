package com.factory.events.controller;

import com.factory.events.dto.EventRequestDTO;
import com.factory.events.dto.EventResponseDTO;
import com.factory.events.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@CrossOrigin(origins = "*")
@Tag(name = "Eventos de Fábrica", description = "Endpoints para registro, validação, impedimento de duplicidade e consulta de eventos")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @PostMapping
    @Operation(summary = "Registrar novo evento", description = "Recebe o evento da fábrica, impede cadastros duplicados (409) e gera log de auditoria se for QUALITY.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Evento registrado com sucesso", 
                     content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Dados do evento inválidos ou incompletos"),
        @ApiResponse(responseCode = "409", description = "Conflito: Evento já registrado previamente")
    })
    public ResponseEntity<EventResponseDTO> receiveEvent(@Valid @RequestBody EventRequestDTO requestDTO) {
        EventResponseDTO response = eventService.processEvent(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Operation(summary = "Listar eventos registrados", description = "Retorna todos os eventos persistidos no banco de dados.")
    @ApiResponse(responseCode = "200", description = "Lista de eventos obtida com sucesso")
    public ResponseEntity<List<EventResponseDTO>> getAllEvents() {
        List<EventResponseDTO> events = eventService.findAllEvents();
        return ResponseEntity.ok(events);
    }
}
