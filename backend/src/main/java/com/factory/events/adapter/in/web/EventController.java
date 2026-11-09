package com.factory.events.adapter.in.web;

import com.factory.events.adapter.in.web.dto.EventResponseDTO;
import com.factory.events.adapter.in.web.dto.EventResponsePageDTO;
import com.factory.events.adapter.in.web.dto.Problem;
import com.factory.events.domain.model.FactoryEvent;
import com.factory.events.domain.model.RegisterEventCommand;
import com.factory.events.domain.model.result.EventPage;
import com.factory.events.domain.model.result.RegisterEventResult;
import com.factory.events.domain.port.in.ListEventsUseCase;
import com.factory.events.domain.port.in.RegisterEventUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/events")
@Tag(name = "eventos", description = "Eventos de fábrica: registro idempotente, consulta paginada e obtenção por ID.")
public class EventController {

  private final RegisterEventUseCase registerEventUseCase;
  private final ListEventsUseCase listEventsUseCase;

  public EventController(RegisterEventUseCase registerEventUseCase,
                         ListEventsUseCase listEventsUseCase) {
    this.registerEventUseCase = registerEventUseCase;
    this.listEventsUseCase = listEventsUseCase;
  }

  @PostMapping(consumes = "application/json")
  @Operation(
    summary = "Registrar evento de fábrica",
    description = "Recebe um evento de fábrica, valida a entrada (RN-01), impede duplicidade de eventId (RN-02) " +
      "e garante replay idempotente pelo cabeçalho Idempotency-Key (RN-03, RN-04, RN-05). " +
      "Quando eventType é QUALITY, orquestra saga com compensação (RN-06, RN-07). " +
      "Não existem PUT/PATCH/DELETE: campos são imutáveis após a criação (RN-08)."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "201", description = "Evento registrado com sucesso",
      content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
    @ApiResponse(responseCode = "200", description = "Replay idempotente (RN-03)",
      content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
    @ApiResponse(responseCode = "400", description = "Erro de validação ou Idempotency-Key ausente (E-01)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "401", description = "Não autenticado (E-11)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "403", description = "Papel sem permissão (E-12)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "409", description = "Conflito: duplicado ou idempotência (E-02, E-03, E-04)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "413", description = "Corpo muito grande (E-09)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "429", description = "Limite de taxa excedido (E-10)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "500", description = "Erro interno (E-13)",
      content = @Content(schema = @Schema(implementation = Problem.class)))
  })
  public ResponseEntity<Object> receiveEvent(
    @Parameter(hidden = true) @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
    @Valid @RequestBody com.factory.events.adapter.in.web.dto.EventRequestDTO requestDTO
  ) {
    RegisterEventCommand command = EventApiMapper.toCommand(requestDTO);
    RegisterEventResult result = registerEventUseCase.register(command);

    return switch (result) {
      case RegisterEventResult.Success s -> ResponseEntity
        .created(URI.create("/v1/events/" + s.event().eventId()))
        .body(EventApiMapper.toResponse(s.event()));
      case RegisterEventResult.Duplicate d -> ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new Problem(
          "https://api.factory/events/errors/duplicate-event",
          "Evento duplicado",
          409,
          "O evento com ID '" + d.eventId() + "' já foi processado anteriormente.",
          "/v1/events#req-" + UUID.randomUUID(),
          Map.of(), null, null
        ));
      case RegisterEventResult.Failure f -> ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new Problem(
          "https://api.factory/events/errors/audit-compensation-required",
          "Falha na auditoria com compensação executada",
          500,
          f.reason(),
          "/v1/events#req-" + UUID.randomUUID(),
          Map.of(), null, null
        ));
      default -> ResponseEntity
        .status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new Problem(
          "https://api.factory/events/errors/internal-error",
          "Erro interno",
          500,
          "Erro interno. Tente novamente ou informe o identificador ao suporte.",
          "/v1/events#req-" + UUID.randomUUID(),
          Map.of(), null, null
        ));
    };
  }

  @GetMapping
  @Operation(
    summary = "Listar eventos paginados",
    description = "Consulta paginada de eventos registrados (RN-09). Ordenação: timestamp desc, id desc. " +
      "Página padrão 20; máximo 100 itens por página. Leitura direta, sem cache (RN-21). " +
      "Máximo 3 consultas por request (RN-12)."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Página de eventos obtida com sucesso",
      content = @Content(schema = @Schema(implementation = EventResponsePageDTO.class))),
    @ApiResponse(responseCode = "400", description = "Erro de validação (E-01)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "401", description = "Não autenticado (E-11)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "403", description = "Papel sem permissão (E-12)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "500", description = "Erro interno (E-13)",
      content = @Content(schema = @Schema(implementation = Problem.class)))
  })
  public ResponseEntity<EventResponsePageDTO> listEvents(
    @Parameter(description = "Número da página (base 0). Padrão 0.")
    @RequestParam(defaultValue = "0") int page,
    @Parameter(description = "Itens por página. Máximo 100 (RN-09). Padrão 20.")
    @RequestParam(defaultValue = "20") int size
  ) {
    EventPage pageResult = listEventsUseCase.list(page, size);
    return ResponseEntity.ok(
      EventApiMapper.toResponsePage(pageResult, confidentialAuthorized())
    );
  }

  @GetMapping("/{eventId}")
  @Operation(
    summary = "Obter evento por ID",
    description = "Retorna o evento identificado por eventId. description é confidencial (RN-17) " +
      "e devolvido apenas a papéis autorizados (RN-20)."
  )
  @ApiResponses({
    @ApiResponse(responseCode = "200", description = "Evento encontrado",
      content = @Content(schema = @Schema(implementation = EventResponseDTO.class))),
    @ApiResponse(responseCode = "404", description = "Recurso não encontrado (E-06)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "401", description = "Não autenticado (E-11)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "403", description = "Papel sem permissão (E-12)",
      content = @Content(schema = @Schema(implementation = Problem.class))),
    @ApiResponse(responseCode = "500", description = "Erro interno (E-13)",
      content = @Content(schema = @Schema(implementation = Problem.class)))
  })
  public ResponseEntity<Object> obterEvento(
    @Parameter(description = "ID do evento", example = "EVT-1001")
    @PathVariable String eventId
  ) {
    Optional<FactoryEvent> event = listEventsUseCase.findByEventId(eventId);
    if (event.isEmpty()) {
      return ResponseEntity
        .status(HttpStatus.NOT_FOUND)
        .body(new Problem(
          "https://api.factory/events/errors/not-found",
          "Recurso não encontrado",
          404,
          "Recurso não encontrado.",
          "/v1/events/" + eventId + "#req-" + UUID.randomUUID(),
          Map.of(), null, null
        ));
    }
    return ResponseEntity.ok(
      EventApiMapper.toResponse(event.get(), confidentialAuthorized())
    );
  }

  private boolean confidentialAuthorized() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated()) {
      return false;
    }
    return auth.getAuthorities().stream()
      .map(GrantedAuthority::getAuthority)
      .anyMatch(a -> a.equals("ROLE_OPERADOR") || a.equals("ROLE_GERENTE"));
  }
}
